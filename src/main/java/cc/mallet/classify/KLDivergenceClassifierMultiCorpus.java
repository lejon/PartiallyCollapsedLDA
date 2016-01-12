package cc.mallet.classify;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import cc.mallet.classify.evaluate.ConfusionMatrix;
import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.topics.LDASamplerWithPhi;
import cc.mallet.topics.SpaliasUncollapsedParallelLDA;
import cc.mallet.types.Alphabet;
import cc.mallet.types.CrossValidationIterator;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelVector;
import cc.mallet.types.MatrixOps;

public class KLDivergenceClassifierMultiCorpus extends Classifier {
	static DecimalFormat mydecimalFormat = new DecimalFormat("00.###E0");
	public static int noDigits = 4;

	public boolean abort = false;
	private static final long serialVersionUID = 1L;
	LDAConfiguration config;
	
	InstanceList trainingset;
	Map<String,LDASamplerWithPhi> trainedSamplers;
	Map<String,double []> classCentroids;
	int noClassified = 0;
	double alpha;
	
	public KLDivergenceClassifierMultiCorpus(LDAConfiguration config) {
		this.config = config;
		alpha = config.getAlpha(0.01);
	}

	@Override
	public Classification classify(Instance instance) {
		
		Alphabet targetAlphabet = instance.getTargetAlphabet();
		double[] scores = new double[targetAlphabet.size()];
		for (String key : trainedSamplers.keySet()) {
			LDASamplerWithPhi ldaGibbsSampler = trainedSamplers.get(key);
			InstanceList currentTestset = new InstanceList(trainingset.getDataAlphabet(), trainingset.getTargetAlphabet());
			currentTestset.add(instance);
			SpaliasUncollapsedParallelLDA spalias = new SpaliasUncollapsedParallelLDA(config);
			spalias.addInstances(currentTestset);
			spalias.setPhi(ldaGibbsSampler.getPhi(), getAlphabet(), getLabelAlphabet());
			spalias.sampleZGivenPhi(300);
			double [][] sampledTestZBar = spalias.getZbar();
			double[] docTopicMeans = sampledTestZBar[0];
			// Normalize 
			double sum = MatrixOps.sum(docTopicMeans);
			for (int i = 0; i < docTopicMeans.length; i++) {
				docTopicMeans[i] = (docTopicMeans[i]+alpha) / sum;
			}
			//System.out.println("Doc-topic: " + arrToStr(docTopicMeans));
			double klDivergence = calcKLDivergences(classCentroids.get(key), docTopicMeans);
			//System.out.println("Divergence vs. " + key + "(" + targetAlphabet.lookupIndex(key) + ") is:" + klDivergence);
			scores[targetAlphabet.lookupIndex(key)] = klDivergence;
		}
		
		// We need to transform the kl-divergencies (low is good) to scores (high is good)
		for (int i = 0; i < scores.length; i++) {
			scores[i] = 1.0 / scores[i];
		}
		
		
		System.out.println("["  + instance.getTarget().toString() + "]: Scores are: " + arrToStr(scores));
		
		noClassified++;
		if(noClassified%50==0) System.out.println("# Classified: " + noClassified );
		return new Classification(instance, this, new LabelVector (getLabelAlphabet(), scores));
	}
	
	protected double calcKLDivergences(double[] ds, double[] ds2) {
		// Use symmetrisized KL divergence
		double u1 = cc.mallet.util.Maths.klDivergence(ds, ds2);
		double u2 = cc.mallet.util.Maths.klDivergence(ds2, ds);
		return (u1 + u2) / 2;
	}

	public Map<String,LDASamplerWithPhi> train(InstanceList trainingset) throws IOException {
		instancePipe = trainingset.getPipe();
		
		this.trainingset = trainingset;
		
		Map<String,InstanceList> classMap = new HashMap<>();
		
		// Create an instance list per class
		for (int i = 0; i < trainingset.size(); i++) {
			Instance instance = trainingset.get(i);
			String classLabel = instance.getTarget().toString();
			if(classMap.get(classLabel)==null) {
				InstanceList newList = new InstanceList(trainingset.getDataAlphabet(), trainingset.getTargetAlphabet());
				classMap.put(classLabel, newList);
				newList.add(instance);
			} else {
				classMap.get(classLabel).add(instance);
			}
		}
		
		// Train one LDA model per class
		trainedSamplers = new HashMap<>();
		for (String key : classMap.keySet()) {
			InstanceList classTrainingInstances = classMap.get(key);
			System.out.println("Training on " + classTrainingInstances.size() + " documents in class: " + key);
			SpaliasUncollapsedParallelLDA spalias = new SpaliasUncollapsedParallelLDA(config);
			spalias.addInstances(classTrainingInstances);
			spalias.sample(config.getNoIterations(3000));
			trainedSamplers.put(key,spalias);
		}
		
		// Calculate class centroids
		classCentroids = calculateCentroids(trainedSamplers);
		System.out.println("Centroids are: ");
		
		for (String key : classCentroids.keySet()) {
			double [] centroid = classCentroids.get(key);
			System.out.println("Centroid for: "+ key + " is: " + arrToStr(centroid));
		}
		
		return trainedSamplers;
	}
		
	public static String formatDouble(double d) {
		if ( d == 0.0 ) return "<0.0>";
		if ( d<0.0001 && d>0 || d > -0.0001 && d < 0) {
			return mydecimalFormat.format(d);
		} else {
			String formatString = "%." + noDigits + "f";
			return String.format(formatString, d);
		}
	}

	public static String arrToStr(double [] arr) {
		return arrToStr(arr, "DoubleArray", Integer.MAX_VALUE);
	}
	
	public static String arrToStr(double [] arr, String title, int maxLen) {
		String res = "";
		res += title + "[" +  arr.length + "]:";
		for (int j = 0; j < arr.length && j < maxLen; j++) {
			res += formatDouble(arr[j]) + ", ";
		}
		return res;
	}

	private Map<String, double[]> calculateCentroids(Map<String,LDASamplerWithPhi> trainedSamplers) {
		Map<String, double[]> classCentroids = new HashMap<>();
		for (String key : trainedSamplers.keySet()) {
			LDASamplerWithPhi ldaGibbsSampler = trainedSamplers.get(key);
			double [] classCentroid = new double[config.getNoTopics(-1)];
			double [][] docTopics = ldaGibbsSampler.getZbar();
			for (int doc = 0; doc < docTopics.length; doc++) {
				double [] zBars = docTopics[doc];
				for (int topic = 0; topic < zBars.length; topic++) {
					classCentroid[topic] += zBars[topic];
				}
			}
			// The centroid of a class is defined as the average doc-topics of the documents in that class
			for (int topic = 0; topic < classCentroid.length; topic++) {
				classCentroid[topic] = (classCentroid[topic] + alpha) / docTopics.length;
			}
			classCentroids.put(key, classCentroid);
		}
		return classCentroids;
	}
	
	public Trial [] crossValidate(InstanceList instances, int folds) throws Exception {
		Trial [] trials = new Trial[folds];
		Random r = new Random ();
		int TRAINING = 0;
		int TESTING = 1;
		CrossValidationIterator cvIter = new CrossValidationIterator(instances, folds, r);
		InstanceList[] cvSplit = null;

		for (int fold = 0; fold < folds && !abort; fold++) {
			noClassified = 0;
			cvSplit = cvIter.next();

			int tries = 0;
			int maxTries = 3;
			boolean success = false;
			Exception trainingException = null;
			// Sometimes the gamma or lambda sampling returns NaN and the beta sampling aborts
			// let's try a couple of times before giving up so perhaps we can save 
			// a couple of cross validations
			while(!success && tries < maxTries && !abort) {
				try {
					train( cvSplit[TRAINING] );
					success = true;	    			
				} catch (Exception e1) {
					System.err.println("Training failed: " + e1);
					System.err.println("Retrying (" + tries + "/" + maxTries + ")...");
					trainingException = e1;
					tries++;
				}
			}
			if(!success) {
				System.err.println("Training failed, giving up after " + tries + " tries...");
				throw trainingException;
			}

			System.out.println("\nTesting on: " + cvSplit[TESTING].size() + " documents...");
			trials[fold] = new Trial(this, cvSplit[TESTING]);
			System.out.println("Trial accuracy: "  + trials[fold].getAccuracy());
			ConfusionMatrix enhancedConfusionMatrix = new ConfusionMatrix(trials[fold]);
			System.out.println("Trial confusion matrix: \n"  + enhancedConfusionMatrix);
			
			saveFoldData(fold, enhancedConfusionMatrix);
		}
		
		return trials;
	}

	private void saveFoldData(int fold, ConfusionMatrix enhancedConfusionMatrix) {
		
	}

	public boolean getAbort() {
		return abort;
	}

	public void setAbort(boolean abort) {
		this.abort = abort;
	}

	public Map<String, LDASamplerWithPhi> getTrainedSamplers() {
		return trainedSamplers;
	}

}
