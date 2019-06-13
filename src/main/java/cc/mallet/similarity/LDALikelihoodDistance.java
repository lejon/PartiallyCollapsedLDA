package cc.mallet.similarity;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.configuration.ParsedLDAConfiguration;
import cc.mallet.configuration.SimpleLDAConfiguration;
import cc.mallet.pipe.Pipe;
import cc.mallet.topics.LDASamplerWithPhi;
import cc.mallet.topics.PolyaUrnSpaliasLDA;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.util.ArrayStringUtils;
import cc.mallet.util.LDAUtils;
import cc.mallet.util.StringClassArrayIterator; 

public class LDALikelihoodDistance implements TrainedDistance {
	static DecimalFormat mydecimalFormat = new DecimalFormat("00.###E0");
	public static int noDigits = 4;

	public boolean abort = false;
	LDAConfiguration config;
	
	InstanceList trainingset;
	LDASamplerWithPhi trainedSampler;
	int noClassified = 0;
	double alpha;
	String [] testRowIds;
	Map<Instance, double []> sampledTopics = new HashMap<>();
	Map<Integer, double []> sampledQueryTopics = new HashMap<>();
	double [][] trainingSetTopicDists;
	Map<Integer,double []> cache = new HashMap<>();
	
	int testSampleIter = 500;
	double epsilon = 0.0;
	double lambda = 0.4;
	double mu = 1000.0;
	
	double [][] phi;

	Pipe instancePipe;
	
	String samplerFn = "stored_samplers/saved_similarity_sampler.bin";
	private int[] N_d;
	private double[] p_w_coll;
	
	public LDALikelihoodDistance(LDAConfiguration config) {
		this.config = config;
		alpha = config.getAlpha(0.01);
	}
	
	public void setNoTRainingSamples(int samples) {
		
	}
	
	public double [] getSampledTopics(Instance instance) {
		return sampledTopics.get(instance);
	}

	public double [] distanceToAll(Instance instance) {
		double[] distances = new double[trainingSetTopicDists.length];
		double[] docTheta;
		int[] wordTokens = LDAUtils.getWordTokens(instance);
		int hashCode = Arrays.hashCode(wordTokens);
		if(!cache.containsKey(hashCode)) {			
			InstanceList currentTestset = new InstanceList(trainingset.getDataAlphabet(), trainingset.getTargetAlphabet());
			currentTestset.add(instance);
			SimpleLDAConfiguration sc = ((ParsedLDAConfiguration) config).simpleLDAConfiguration();
			sc.setTopicInterval(10);
			PolyaUrnSpaliasLDA spalias = new PolyaUrnSpaliasLDA(sc);
			spalias.addInstances(currentTestset);
			spalias.setPhi(trainedSampler.getPhi(), instance.getAlphabet(), instance.getTargetAlphabet());
			spalias.sampleZGivenPhi(testSampleIter);
			double [][] thetaEstimate = spalias.getThetaEstimate();
			docTheta = thetaEstimate[0];
			cache.put(hashCode, docTheta);
			
		} else {
			docTheta = cache.get(hashCode);
		}
		
		sampledTopics.put(instance, docTheta);
			
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
				double distance = calculate(trainingDoc, docTheta);
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
				
		trainedSampler = new PolyaUrnSpaliasLDA(config);
		trainedSampler.addInstances(trainingset);
		trainedSampler.sample(config.getNoIterations(3000));
		
		trainingSetTopicDists = trainedSampler.getThetaEstimate();
		
		for (int i = 0; i < trainingset.size(); i++) {
			Instance instance = trainingset.get(i);
			sampledTopics.put(instance, trainingSetTopicDists[i]);
		}
		
		return trainedSampler;
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
				res += ArrayStringUtils.formatDouble(arr[j]) + ", ";
			}
		}
		return res;
	}

	public static String arrToIdxStr(double [] arr, String title, int maxLen, Instance instance) {
		Alphabet alphabet = instance.getAlphabet();
		
		String res = "";
		res += title + "[" +  arr.length + "]:";
		for (int j = 0; j < arr.length && j < maxLen; j++) {
			if(arr[j]<0.00005) {
				res += "";
			} else {				
				String word = (String) alphabet.lookupObject(j);
				res += "(" + word + "):" + j + ":" + ArrayStringUtils.formatDouble(arr[j]) + ", ";
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

	@Override
	public double calculate(double[] v1, double[] v2) {
		System.out.println("Calculating...");
		int [] v1Indices = new int[v1.length];
		int [] v2Indices = new int[v2.length];
		
		for (int i = 0; i < v1Indices.length; i++) {
			v1Indices[i] = (int) v1[i];
		}

		for (int i = 0; i < v2Indices.length; i++) {
			v2Indices[i] = (int) v2[i];
		}
		
		Alphabet alphabet = trainingset.getAlphabet();
		String s1 = LDAUtils.indicesToString(v1Indices, alphabet);
		String s2 = LDAUtils.indicesToString(v2Indices, alphabet);
		String [] doclines = new String [] {s1,s2};
		StringClassArrayIterator readerTest = new StringClassArrayIterator (
				doclines, "X"); 

		InstanceList testInstances = new InstanceList(trainingset.getPipe());
		testInstances.addThruPipe(readerTest);
		double [] theta1;
		
		Instance instance = testInstances.get(0);
		int[] wordTokens = LDAUtils.getWordTokens(instance);
		int hashCode = Arrays.hashCode(wordTokens);
		if(!cache.containsKey(hashCode)) {
			theta1 = sample(instance);
			cache.put(hashCode, theta1);
		} else {
			theta1 = cache.get(hashCode);
		}

		double [] theta2 = sample(testInstances.get(1));
		sampledQueryTopics.put(Arrays.hashCode(v1), theta1);
		sampledQueryTopics.put(Arrays.hashCode(v2), theta2);

		return 0.0;
	}
	
	private double ldaLoglikelihood(int[] v1, int[] v2, double[] theta1, double[] theta2) {
		double p_w = 0.0;
		double N_d = v2.length;
		
		//System.out.println("v1: \n\t" + Arrays.toString(v1) + "\n\t" + Arrays.toString(theta1));
		//System.out.println("v2: \n\t" + Arrays.toString(v2) + "\n\t" + Arrays.toString(theta2));

		Set<Integer> uniqueDocumentWords = new HashSet<>();
		Map<Integer,Double> p_w_d = new HashMap<>();

		// Find the number of unique words in v2
		// Find the number of times each word occurs in v2
		for (int i = 0; i < v2.length; i++) {
			int wordFreq = (int)v2[i];
			if(wordFreq>0) {
				int word = i;
				uniqueDocumentWords.add(word);
				if(p_w_d.get(word) == null) {
					p_w_d.put(word,0.0);
				}
				p_w_d.put(word,p_w_d.get(word) + wordFreq);
			}
		}

		// Normalize
		for (Integer word : p_w_d.keySet()) {
			p_w_d.put(word,p_w_d.get(word) /  N_d);
		}

		int K = config.getNoTopics(LDAConfiguration.NO_TOPICS_DEFAULT);
		double mixtureRatio = (N_d / (N_d + mu));
		for (int i = 0; i < v1.length; i++) {
			double wordProb = epsilon;
			int wordFreq = (int)v1[i];
			if(wordFreq > 0) {
				int word = i;
				if(p_w_d.get(word) != null) {
					wordProb = p_w_d.get(word);
				}
				
				double p_w_lda = 0.0;
				for (int k = 0; k < K; k++) {
					p_w_lda += theta2[k] * phi[k][word];
				}
				//System.out.println("p_w_lda: " + p_w_lda);
				
				p_w += lambda *
						// Ordinary likelihood
						(mixtureRatio * wordProb + (1-mixtureRatio) * p_w_coll[word]) +
						// LDA likelihood
						(1-lambda) * p_w_lda;						
			}
		}
		
		return 1-p_w;
	}

	double [] sample(Instance instance) {
		InstanceList currentTestset = new InstanceList(trainingset.getDataAlphabet(), trainingset.getTargetAlphabet());
		currentTestset.add(instance);
		SimpleLDAConfiguration sc = ((ParsedLDAConfiguration) config).simpleLDAConfiguration();
		// Hide printouts for LL for sampling docs
		sc.setTopicInterval(testSampleIter + 10);
		PolyaUrnSpaliasLDA spalias = new PolyaUrnSpaliasLDA(sc);
		spalias.addInstances(currentTestset);
		spalias.setPhi(trainedSampler.getPhi(), instance.getAlphabet(), instance.getTargetAlphabet());
		spalias.sampleZGivenPhi(testSampleIter);
		
		double [][] thetaEstimate = spalias.getThetaEstimate();
		
		sampledTopics.put(instance, thetaEstimate[0]);
		return thetaEstimate[0];
	}

	@Override
	public void init(InstanceList trainingset) {
		String trainingsetHash = getTrainingSetHash();
		String storedHash = readStoredTrainingsetHash(samplerFn + "-training_hash-" + trainingsetHash);
		File storedSampler = new File(samplerFn + "-sampler-" + trainingsetHash);
		this.trainingset = trainingset;
		
		
		initLikelihood(trainingset);
		
		
		if(storedSampler.exists() && trainingsetHash.equals(storedHash)) {
			try {
				System.out.println("Using pretrained sampler @:" + storedSampler.getAbsolutePath());
				trainedSampler = PolyaUrnSpaliasLDA.read(storedSampler);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			trainingset.getAlphabet().stopGrowth();
			trainedSampler = new PolyaUrnSpaliasLDA(config);
			trainedSampler.addInstances(trainingset);
			try {
				trainedSampler.sample(config.getNoIterations(3000));
				File tmpDir = new File("stored_samplers");
				if(!tmpDir.exists()) {
					tmpDir.mkdir();
				}
				((PolyaUrnSpaliasLDA) trainedSampler).write(storedSampler);
				writeTrainingsetHash(trainingsetHash,samplerFn + "-training_hash-" + trainingsetHash);
			} catch (IOException e) {
				e.printStackTrace();
				throw new IllegalStateException(e);
			}
		}
		trainingSetTopicDists = trainedSampler.getThetaEstimate();
		phi = trainedSampler.getPhi();
		
		for (int i = 0; i < trainingset.size(); i++) {
			Instance instance = trainingset.get(i);
			sampledTopics.put(instance, trainingSetTopicDists[i]);
		}
		
		System.out.println("Top words of the traines sampler are: \n" + 
				LDAUtils.formatTopWords(LDAUtils.getTopWords(10, 
						trainedSampler.getAlphabet().size(), 
						trainedSampler.getNoTopics(), 
						trainedSampler.getTypeTopicMatrix(), 
						trainedSampler.getAlphabet())));
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}

	 void initLikelihood(InstanceList trainingset2) {
		 N_d = new int[trainingset.size()];
			p_w_coll = new double[trainingset.getAlphabet().size()];
			long corpusSize = 0;
			for (int i = 0; i < trainingset.size(); i++) {
				FeatureSequence words = (FeatureSequence)trainingset.get(i).getData();
				N_d[i] = words.size();
				for (int j = 0; j < words.size(); j++) {
					p_w_coll[words.getIndexAtPosition(j)]++;
					corpusSize++;
				}
			}	
			
			//normalize
			for (int i = 0; i < p_w_coll.length; i++) {
				p_w_coll[i] /= corpusSize;
			}
	}

	private String getTrainingSetHash() {
		
		String configFn = config.whereAmI();

		byte[] bytes = null;
		try {
			bytes = Files.readAllBytes(Paths.get(configFn));
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Static getInstance method is called with hashing MD5 
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} 

		// digest() method is called to calculate message digest 
		// of an input digest() return array of byte 
		byte[] messageDigest = md.digest(bytes); 

		// Convert byte array into signum representation 
		BigInteger no = new BigInteger(1, messageDigest);

		// Convert message digest into hex value 
		String hashtext = no.toString(16); 
		while (hashtext.length() < 32) { 
			hashtext = "0" + hashtext; 
		} 
		return hashtext; 
	} 

	private void writeTrainingsetHash(String trainingsetHash, String string) {
		try {
			Files.write(Paths.get(string), trainingsetHash.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String readStoredTrainingsetHash(String filePath) {
		String hash = null;
		try {
			hash = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
		} catch (IOException e) {
		}

		return hash;
	}

	@Override
	public double distanceToTrainingSample(double[] query, int sampleId) {
		int [] v1Indices = new int[query.length];
		
		for (int i = 0; i < v1Indices.length; i++) {
			v1Indices[i] = (int) query[i];
		}
		
		Alphabet alphabet = trainingset.getAlphabet();
		String s1 = LDAUtils.indicesToString(v1Indices, alphabet);
		String [] doclines = new String [] {s1};
		StringClassArrayIterator readerTest = new StringClassArrayIterator (
				doclines, "X"); 

		InstanceList testInstances = new InstanceList(trainingset.getPipe());
		testInstances.addThruPipe(readerTest);
		
		double [] theta1;
		int hashCode = Arrays.hashCode(LDAUtils.getWordTokens(testInstances.get(0)));
		if(!cache.containsKey(hashCode)) {	
			theta1 = sample(testInstances.get(0));
			cache.put(hashCode, theta1);
		} else {
			theta1 = cache.get(hashCode);
		}
		
		double [] theta2 = trainingSetTopicDists[sampleId];
		sampledTopics.put(testInstances.get(0), theta1);
		sampledQueryTopics.put(Arrays.hashCode(query), theta1);

		
		FeatureSequence features = (FeatureSequence) trainingset.get(sampleId).getData();
		int [] v1 = new int[query.length];
		for (int i = 0; i < v1.length; i++) {
			v1[i] = (int)query[i];
		}

		int [] v2 = new int[trainingset.get(sampleId).getAlphabet().size()];
		for (int i = 0; i < features.size(); i++) {
			v2[features.getIndexAtPosition(i)]++;
		}
		
		double dd = ldaLoglikelihood(v1, v2, theta1, theta2);
		//System.out.println("Comparing: \n\t" + s1 + "\n\t" + Arrays.toString(theta1) + "\n\t" + Arrays.toString(theta2) + "\n\t" + LDAUtils.instanceToString(trainingset.get(sampleId)));
		//System.out.println("Distance was: " + dd);
		return dd;
	}

	public double[] getSampledQueryTopics(double[] instanceVector) {
		return sampledQueryTopics.get(Arrays.hashCode(instanceVector));
	}

}
