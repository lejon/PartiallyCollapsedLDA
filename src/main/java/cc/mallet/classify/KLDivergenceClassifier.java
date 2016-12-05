package cc.mallet.classify;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import cc.mallet.classify.evaluate.EnhancedConfusionMatrix;
import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.topics.LDASamplerWithPhi;
import cc.mallet.topics.SpaliasUncollapsedParallelLDA;
import cc.mallet.types.Alphabet;
import cc.mallet.types.CrossValidationIterator;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelVector;
import cc.mallet.types.MatrixOps;
import cc.mallet.util.LDAUtils;

public class KLDivergenceClassifier extends Classifier {
	static DecimalFormat mydecimalFormat = new DecimalFormat("00.###E0");
	public static int noDigits = 4;

	public boolean abort = false;
	private static final long serialVersionUID = 1L;
	LDAConfiguration config;
	
	InstanceList trainingset;
	LDASamplerWithPhi trainedSampler;
	Map<String,double []> classCentroids;
	int noClassified = 0;
	double alpha;
	String [] testRowIds;
	double [][] sampledTestTopics;
	
	public KLDivergenceClassifier(LDAConfiguration config) {
		this.config = config;
		alpha = config.getAlpha(0.01);
	}

	@Override
	public Classification classify(Instance instance) {
		
		Alphabet targetAlphabet = instance.getTargetAlphabet();
		double[] scores = new double[targetAlphabet.size()];
		InstanceList currentTestset = new InstanceList(trainingset.getDataAlphabet(), trainingset.getTargetAlphabet());
		currentTestset.add(instance);
		SpaliasUncollapsedParallelLDA spalias = new SpaliasUncollapsedParallelLDA(config);
		spalias.addInstances(currentTestset);
		spalias.setPhi(trainedSampler.getPhi(), getAlphabet(), getLabelAlphabet());
		spalias.sampleZGivenPhi(300);
		
		double [][] sampledTestZBar = spalias.getZbar();
		double[] docTopicMeans = sampledTestZBar[0];
		sampledTestTopics[noClassified] = docTopicMeans;
		// Normalize 
		double sum = MatrixOps.sum(docTopicMeans);
		for (int i = 0; i < docTopicMeans.length; i++) {
			docTopicMeans[i] = (docTopicMeans[i]+alpha) / sum;
		}

		
//		double [][] thetaEstimate = spalias.getThetaEstimate();
//		double[] docTheta = thetaEstimate[0];
//		sampledTestTopics[noClassified] = docTheta;
			
		for (String key : classCentroids.keySet()) {
			//System.out.println("Doc-topic: " + arrToStr(docTopicMeans));
			//double klDivergence = calcKLDivergences(classCentroids.get(key), docTheta);
			double klDivergence = calcKLDivergences(classCentroids.get(key), docTopicMeans);
			//System.out.println("Divergence vs. " + key + "(" + targetAlphabet.lookupIndex(key) + ") is:" + klDivergence);
			// We need to transform the kl-divergencies (low is good) to scores (high is good)
			scores[targetAlphabet.lookupIndex(key)] = 1.0 / klDivergence;
		}
				
		//System.out.println("["  + instance.getTarget().toString() + "]: Scores are: " + arrToStr(scores));
		
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

	public LDASamplerWithPhi train(InstanceList trainingset) throws IOException {
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
		
		trainedSampler = new SpaliasUncollapsedParallelLDA(config);
		trainedSampler.addInstances(trainingset);
		trainedSampler.sample(config.getNoIterations(3000));
		
		// Calculate class centroids
		//classCentroids = calculateCentroids(trainedSampler.getThetaEstimate(), trainingset);
		classCentroids = calculateCentroids(trainedSampler.getZbar(), trainingset);
		System.out.println("Centroids are: ");

		for (String key : classCentroids.keySet()) {
			double [] centroid = classCentroids.get(key);
			System.out.println("Centroid for: "+ key + " is: " + arrToStr(centroid));
		}
		
		return trainedSampler;
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

	private Map<String, double[]> calculateCentroids(double [][] docTopics, InstanceList trainingset) {
		Map<String, double[]> classCentroids = new HashMap<>();
		Map<String, Integer> classCentroidCnts = new HashMap<>();
		
		for (int docIdx = 0; docIdx < trainingset.size(); docIdx++) {
			Instance instance = trainingset.get(docIdx);
			String className = instance.getTarget().toString();
			if(classCentroids.get(className)==null) {
				classCentroids.put(className, new double[config.getNoTopics(-1)]);
				classCentroidCnts.put(className, 0);
			}
			
			double [] classCentroid = classCentroids.get(className);
			classCentroidCnts.put(className, classCentroidCnts.get(className) + 1);
			
			double [] zBars = docTopics[docIdx];
			for (int topic = 0; topic < zBars.length; topic++) {
				classCentroid[topic] += zBars[topic];
			}
		}
		
		// Normalize
		for (String key : classCentroids.keySet()) {
			double [] classCentroid = classCentroids.get(key);
			// The centroid of a class is defined as the average doc-topics of the documents in that class
			for (int topic = 0; topic < classCentroid.length; topic++) {
				classCentroid[topic] = (classCentroid[topic] + alpha) / (double) classCentroidCnts.get(key);
			}
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
			
			sampledTestTopics = new double[cvSplit[TESTING].size()][];

			System.out.println("\nTesting on: " + cvSplit[TESTING].size() + " documents...");
			
			testRowIds = extractRowIds(cvSplit[TESTING]);
			trials[fold] = new Trial(this, cvSplit[TESTING]);
			System.out.println("Trial accuracy: "  + trials[fold].getAccuracy());
			EnhancedConfusionMatrix enhancedConfusionMatrix = new EnhancedConfusionMatrix(trials[fold]);
			System.out.println("Trial confusion matrix: \n"  + enhancedConfusionMatrix);
			
			saveFoldData(fold, enhancedConfusionMatrix, trials[fold]);
		}
		
		return trials;
	}
	
	public static String [] extractRowIds(InstanceList trainingSet) {
		String [] result = new String[trainingSet.size()];
		int copied = 0;
		for(Instance instance : trainingSet) {
			result[copied++] = instance.getName().toString();
		}
		return result;
	}

	private void saveFoldData(int fold, EnhancedConfusionMatrix enhancedConfusionMatrix, Trial trial) throws FileNotFoundException, IOException {
		String [] allColnames = new String[config.getNoTopics(0)];
		for (int i = 0; i < allColnames.length; i++) {
			allColnames[i] = "Z" + i;
		}
		File lgDir = config.getLoggingUtil().getLogDir();
		
		// Save example betas
		String foldPrefix = "fold-";
		
		// Save example doc-topic means if that is turned on in config
		if(config.saveDocumentTopicMeans() && config.getDatasetFilename()!=null) {
			String dtFn = config.getDocumentTopicMeansOutputFilename();
			LDAUtils.writeASCIIDoubleMatrix(trainedSampler.getZbar(), lgDir.getAbsolutePath() + "/" + foldPrefix + fold + "-" + dtFn, ",");
			LDAUtils.writeASCIIDoubleMatrix(sampledTestTopics,lgDir.getAbsolutePath() + "/" + foldPrefix + fold + "-TESTSET-" + dtFn, ",");
		}
		
		PrintWriter idsOut = new PrintWriter(config.getLoggingUtil().getLogDir().getAbsolutePath() + "/test-ids-fold-" + fold + ".txt");
		for (String id : testRowIds) {				
			idsOut.println(id);
		}
		idsOut.flush();
		idsOut.close();
		
		PrintWriter out = new PrintWriter(config.getLoggingUtil().getLogDir().getAbsolutePath() + "/confusion-matrix-fold-" + fold + ".txt");
		out.println(enhancedConfusionMatrix);
		out.flush();
		out.close();
		
		PrintWriter pw = new PrintWriter(config.getLoggingUtil().getLogDir().getAbsolutePath() + "/confusion-matrix-fold-" + fold + ".csv");
		pw.println(enhancedConfusionMatrix.toCsv(","));
		pw.flush();
		pw.close();

		PrintWriter clssFs = new PrintWriter(config.getLoggingUtil().getLogDir().getAbsolutePath() + "/classifications-fold-" + fold + ".txt");
		for (int j = 0; j < trial.size(); j++) {
			Classification cl = trial.get(j);
			cl.print(clssFs);
		}
		clssFs.flush();
		clssFs.close();
		
		PrintWriter topOut = new PrintWriter(lgDir.getAbsolutePath() + "/fold-" + fold + "-TopWords.txt");
		
		String topWords = LDAUtils.formatTopWordsAsCsv(LDAUtils.getTopWords(config.getNrTopWords(LDAConfiguration.NO_TOP_WORDS_DEFAULT), trainedSampler.getAlphabet().size(), 
				trainedSampler.getNoTopics(), trainedSampler.getTypeTopicMatrix(), trainedSampler.getAlphabet()));
		topOut.println(topWords);
		System.out.println("Top words are: \n" + topWords);
		topOut.flush();
		topOut.close();
	}

	public boolean getAbort() {
		return abort;
	}

	public void setAbort(boolean abort) {
		this.abort = abort;
	}

	public LDASamplerWithPhi getTrainedSampler() {
		return trainedSampler;
	}

}
