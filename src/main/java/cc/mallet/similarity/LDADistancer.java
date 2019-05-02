package cc.mallet.similarity;

import java.io.IOException;
import java.text.DecimalFormat;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.pipe.Pipe;
import cc.mallet.topics.LDASamplerWithPhi;
import cc.mallet.topics.SpaliasUncollapsedParallelLDA;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.util.LDAUtils;

public class LDADistancer  {
	static DecimalFormat mydecimalFormat = new DecimalFormat("00.###E0");
	public static int noDigits = 4;

	public boolean abort = false;
	LDAConfiguration config;
	
	InstanceList trainingset;
	LDASamplerWithPhi trainedSampler;
	int noClassified = 0;
	double alpha;
	String [] testRowIds;
	double [][] sampledTestTopics;
	double [][] trainingSetTopicDists;
	
	Distance dist = new KLDistance();

	Pipe instancePipe;
	
	public LDADistancer(LDAConfiguration config) {
		this.config = config;
		alpha = config.getAlpha(0.01);
	}
	
	public Distance getDist() {
		return dist;
	}

	public void setDist(Distance dist) {
		this.dist = dist;
	}
	
	public void setNoTRainingSamples(int samples) {
		sampledTestTopics = new double[samples][config.getNoTopics(LDAConfiguration.NO_TOPICS_DEFAULT)];
	}
	
	public double [][] getSampledTestTopics() {
		return sampledTestTopics;
	}

	public double [] distance(Instance instance) {
		double[] distances = new double[trainingSetTopicDists.length];
		InstanceList currentTestset = new InstanceList(trainingset.getDataAlphabet(), trainingset.getTargetAlphabet());
		currentTestset.add(instance);
		SpaliasUncollapsedParallelLDA spalias = new SpaliasUncollapsedParallelLDA(config);
		spalias.addInstances(currentTestset);
		spalias.setPhi(trainedSampler.getPhi(), instance.getAlphabet(), instance.getTargetAlphabet());
		spalias.sampleZGivenPhi(2000);
		

//		double [][] sampledTestZBar = spalias.getZbar();
//		double[] docTopicMeans = sampledTestZBar[0];
//		sampledTestTopics[noClassified] = docTopicMeans;
//		// Normalize 
//		double sum = MatrixOps.sum(docTopicMeans);
//		for (int i = 0; i < docTopicMeans.length; i++) {
//			docTopicMeans[i] = (docTopicMeans[i]+alpha) / sum;
//		}

		
		double [][] thetaEstimate = spalias.getThetaEstimate();
		double[] docTheta = thetaEstimate[0];
		sampledTestTopics[noClassified] = docTheta;
			
		for (int i = 0 ; i < trainingSetTopicDists.length; i++) {
			Instance trainInst = trainingset.get(i);
			FeatureSequence trainTokenSeq = (FeatureSequence) trainInst.getData();
			FeatureSequence testTokenSeq = (FeatureSequence) instance.getData();
			// If either doc has length 0
			if(trainTokenSeq.getLength()==0 && testTokenSeq.getLength()==0) {
				distances[i] = 0.0;
			} else if(trainTokenSeq.getLength()==0 || testTokenSeq.getLength()==0) {
				distances[i] = Double.POSITIVE_INFINITY;
			} else {
				double [] trainingDoc = trainingSetTopicDists[i];
				//System.out.println("Doc-topic: " + arrToStr(docTopicMeans));
				//System.out.println("Test-topic: " + arrToStr(trainingDoc));
				//double klDivergence = calcKLDivergences(classCentroids.get(key), docTheta);
				//double klDivergence = calcKLDivergences(trainingDoc, docTopicMeans);
				double distance = dist.calculate(trainingDoc, docTheta);
				//System.out.println("Divergence vs. " + instance +" is:" + klDivergence);
				// We need to transform the kl-divergencies (low is good) to scores (high is good)
				distances[i] = distance;
			}
		}
				
		//System.out.println("["  + instance.getTarget().toString() + "]: Scores are: " + arrToStr(scores));
		
		noClassified++;
		if(noClassified%50==0) System.out.println("# Classified: " + noClassified );
		return distances;
	}

	public LDASamplerWithPhi train(InstanceList trainingset) throws IOException {
		instancePipe = trainingset.getPipe();
		
		this.trainingset = trainingset;
				
		trainedSampler = new SpaliasUncollapsedParallelLDA(config);
		trainedSampler.addInstances(trainingset);
		trainedSampler.sample(config.getNoIterations(3000));
		
		trainingSetTopicDists = trainedSampler.getThetaEstimate();
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
		return arrToStr(arr, "DoubleArray", Integer.MAX_VALUE, false);
	}

	public static String arrToStr(double [] arr, String title) {
		return arrToStr(arr, title, Integer.MAX_VALUE, false);
	}

	public static String arrToStr(double [] arr, String title, boolean mark) {
		return arrToStr(arr, title, Integer.MAX_VALUE, mark);
	}
	
	public static String arrToStr(double [] arr, String title, int maxLen, boolean mark) {
		String res = "";
		res += title + "[" +  arr.length + "]:";
		for (int j = 0; j < arr.length && j < maxLen; j++) {
			if(arr[j]<0.05) {
				res += "     *, ";
			} else {				
				res += formatDouble(arr[j]) + ", ";
			}
		}
		return res;
	}
	
	public static String [] extractRowIds(InstanceList trainingSet) {
		String [] result = new String[trainingSet.size()];
		int copied = 0;
		for(Instance instance : trainingSet) {
			result[copied++] = instance.getName().toString();
		}
		return result;
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
	
	public String [][] getTopWords(int wordsPerTopic) {
		return LDAUtils.getTopWords(wordsPerTopic, 
				trainedSampler.getAlphabet().size(), 
				trainedSampler.getNoTopics(), 
				trainedSampler.getTypeTopicMatrix(), 
				trainedSampler.getAlphabet());
	}

}
