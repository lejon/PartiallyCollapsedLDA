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

public class LDALikelihoodDistance implements TrainedDistance, InstanceDistance {
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
	double lambda = 0.7;
	double mu = 1.0;
	double mixtureRatio = -1;
	
	double [][] phi;

	Pipe instancePipe;
	
	String samplerFn = "stored_samplers/saved_similarity_sampler.bin";
	private double[] p_w_coll;

	public LDALikelihoodDistance(int K, double alpha) {
		this.alpha = alpha;
	}
	
	public LDALikelihoodDistance(LDASamplerWithPhi trainedSampler) {
		this(trainedSampler.getConfiguration());
		this.trainedSampler = trainedSampler;
		trainingSetTopicDists = trainedSampler.getThetaEstimate();
		setPhi(trainedSampler.getPhi());
		trainingset = trainedSampler.getDataset();
		p_w_coll = calculateProbWordGivenCorpus(trainingset);
	}

	public LDALikelihoodDistance(LDAConfiguration config) {
		this.config = config;
		alpha = config.getAlpha(0.01);
	}
	
	public double getMixtureRatio() {
		return mixtureRatio;
	}

	public void setMixtureRatio(double mixtureRatio) {
		this.mixtureRatio = mixtureRatio;
	}

	public double[][] getPhi() {
		return phi;
	}

	public void setPhi(double[][] phi) {
		this.phi = phi;
	}
	
	public double getLambda() {
		return lambda;
	}

	public void setLambda(double lambda) {
		this.lambda = lambda;
	}
		
	public double [] getSampledTopics(Instance instance) {
		return sampledTopics.get(instance);
	}

	public double [] distanceToAll(Instance instance) {
		double[] distances = new double[trainingSetTopicDists.length];
		double[] docTheta;
		int[] wordTokens = LDAUtils.getWordTokens(instance);
		int hashCode = Arrays.hashCode(wordTokens);
		if(trainingset.contains(instance)) {
			int idx = trainingset.indexOf(instance);
			docTheta = trainingSetTopicDists[idx];
		} else if(cache.containsKey(hashCode)) {			
			docTheta = cache.get(hashCode);			
		} else {
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
		}
		
		sampledTopics.put(instance, docTheta);
		
		TokenFrequencyVectorizer tv = new TokenFrequencyVectorizer();
		double [] testDoc = tv.instanceToVector(instance);
		
		for (int i = 0 ; i < trainingSetTopicDists.length; i++) {
			Instance trainInst = trainingset.get(i);
			FeatureSequence trainTokenSeq = (FeatureSequence) trainInst.getData();
			FeatureSequence testTokenSeq = (FeatureSequence) instance.getData();
			// If both doc has length 0
			if(trainTokenSeq.getLength()==0 && testTokenSeq.getLength()==0) {
				distances[i] = 0.0;
			} else if(trainTokenSeq.getLength()==0 || testTokenSeq.getLength()==0) {
				distances[i] = Double.POSITIVE_INFINITY;
			} else {
				//System.out.println("Doc-topic: " + arrToStr(docTopicMeans));
				//System.out.println("Test-topic: " + arrToStr(trainingDoc));
				//double klDivergence = calcKLDivergences(classCentroids.get(key), docTheta);
				//double klDivergence = calcKLDivergences(trainingDoc, docTopicMeans);
				double distance = distanceToTrainingSample(testDoc, i);
				//System.out.println("Divergence vs. " + instance +" is:" + klDivergence);
				// We need to transform the kl-divergencies (low is good) to scores (high is good)
				distances[i] = distance;
			}
		}
				
		//System.out.println("["  + instance.getTarget().toString() + "]: Scores are: " + arrToStr(scores));
		
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
		int doc1length = 0;
		for (int i = 0; i < v1.length; i++) {
			doc1length += v1[i];
		}

		int doc2length = 0;
		for (int i = 0; i < v2.length; i++) {
			doc2length += v2[i];
		}

		int [] v1Indices = new int[doc1length];
		int [] v2Indices = new int[doc2length];
		
		int wc = 0;
		for (int i = 0; i < v1.length; i++) {
			int wordFreq = (int) v1[i];
			if(wordFreq>0) {
				for (int j = 0; j < wordFreq; j++) {					
					v1Indices[wc++] = i;
				}
			}
		}

		wc = 0;
		for (int i = 0; i < v2.length; i++) {
			int wordFreq = (int) v2[i];
			if(wordFreq>0) {
				for (int j = 0; j < wordFreq; j++) {					
					v2Indices[wc++] = i;
				}
			}
		}
		
		Alphabet alphabet = trainingset.getAlphabet();
		String s1 = LDAUtils.indicesToString(v1Indices, alphabet);
		String s2 = LDAUtils.indicesToString(v2Indices, alphabet);
		String [] doclines = new String [] {s1,s2};
		StringClassArrayIterator readerTest = new StringClassArrayIterator (
				doclines, "X"); 

		InstanceList testInstances = new InstanceList(trainingset.getPipe());
		testInstances.addThruPipe(readerTest);
		
		Instance instanceQuery = testInstances.get(0);
		int[] wordTokensQuery = LDAUtils.getWordTokens(instanceQuery);
		int hashCodeQuery = Arrays.hashCode(wordTokensQuery);
		double [] thetaQuery;
		if(!cache.containsKey(hashCodeQuery)) {
			thetaQuery = sample(instanceQuery);
			cache.put(hashCodeQuery, thetaQuery);
		} else {
			thetaQuery = cache.get(hashCodeQuery);
		}

		Instance instanceDoc = testInstances.get(1);
		int[] wordTokensDoc = LDAUtils.getWordTokens(instanceDoc);
		int hashCodeDoc = Arrays.hashCode(wordTokensDoc);
		double [] thetaDoc;
		if(!cache.containsKey(hashCodeDoc)) {
			thetaDoc = sample(instanceDoc);
			cache.put(hashCodeDoc, thetaDoc);
		} else {
			thetaDoc = cache.get(hashCodeDoc);
		}

		sampledQueryTopics.put(Arrays.hashCode(v1), thetaQuery);
		sampledQueryTopics.put(Arrays.hashCode(v2), thetaDoc);

		int [] v1FreqDoc = Arrays.stream(v1).mapToInt(x -> (int) x).toArray();
		int [] v2FreqDoc = Arrays.stream(v2).mapToInt(x -> (int) x).toArray();
		return ldaLoglikelihood(v1FreqDoc, v2FreqDoc, thetaDoc);
	}
	
	/**
	 * Calculate p(query|document) 
	 * @param query Frequency encoded query (query.length == vocabulary.length)
	 * @param document Frequency encoded document (document.length == vocabulary.length)
	 * @param theta
	 * @return logLikelihood of document generating query
	 */
	public double ldaLoglikelihood(int[] query, int[] document, double[] theta) {
		Map<Integer, Double> p_w_d = calcProbWordGivenDocMLWordEncoding(document);

		double querylength = getDocLength(query);
		double doclength = getDocLength(document);
		
		// Some sanity check first
		if(querylength == 0 && doclength == 0) return 0;
		if(querylength == 0 && doclength != 0) return Double.POSITIVE_INFINITY;
		if(querylength != 0 && doclength == 0) return Double.POSITIVE_INFINITY;

		if(mixtureRatio<0) {
			mixtureRatio = (doclength / (doclength + mu));
		}

		double p_q_d = 0.0;		
		for (int i = 0; i < query.length; i++) {
			double wordProb = 0.0;
			int wordFreq = (int)query[i];
			if(wordFreq > 0) {
				int word = i;
				double wordTopicProb = calcProbWordGivenTheta(theta, word, phi);
				double wordCorpusProb = calcProbWordGivenCorpus(word);
				
				if(p_w_d.get(word) != null) {
					wordProb = p_w_d.get(word);
				}
				p_q_d += Math.log(lambda * (mixtureRatio * wordProb + 
						(1-mixtureRatio) * wordCorpusProb) + 
						(1-lambda) * wordTopicProb);	
			}
		}
				
		return p_q_d;
	}

	int getDocLength(int[] document) {
		int doclength = 0;
		for (int i = 0; i < document.length; i++) {
			doclength += document[i];
		}
		return doclength;
	}

	int getDocLength(double[] document) {
		int doclength = 0;
		for (int i = 0; i < document.length; i++) {
			doclength += document[i];
		}
		return doclength;
	}

	double calcProbWordGivenCorpus(int word) {
		return p_w_coll[word];
	}

	/**
	 * Calculate the maximum likelihood estimate of a word given a document
	 * 
	 * @param document
	 * @return
	 */
	public static Map<Integer, Double> calcProbWordGivenDocMLWordEncoding(int[] document) {
		Set<Integer> uniqueDocumentWords = new HashSet<>();
		Map<Integer,Double> p_w_d = new HashMap<>();

		double d2length = 0;
		// Find the number of unique words in v2
		// Find the number of times each unique word occurs in v2
		for (int i = 0; i < document.length; i++) {
			int wordFreq = (int)document[i];
			if(wordFreq>0) {
				d2length += wordFreq;
				int word = i;
				uniqueDocumentWords.add(word);
				if(p_w_d.get(word) == null) {
					p_w_d.put(word,0.0);
				}
				p_w_d.put(word,p_w_d.get(word) + wordFreq);
			}
		}
		
		// Normalize
		if(d2length!=0) {
			for (Integer word : p_w_d.keySet()) {
				p_w_d.put(word,p_w_d.get(word) / d2length);
			}
		}

		return p_w_d;
	}
	
	double calcProbWordGivenTheta(double[] theta2, int word, double [][] phi) {
		int K = phi.length;
		double p_w_lda = 1.0;
		for (int k = 0; k < K; k++) {
			p_w_lda *= theta2[k] * phi[k][word];
		}
		return p_w_lda;
	}

	double [] sample(Instance instance) {
		InstanceList currentTestset = new InstanceList(trainingset.getDataAlphabet(), trainingset.getTargetAlphabet());
		currentTestset.add(instance);
		SimpleLDAConfiguration sc;
		if(config instanceof ParsedLDAConfiguration) {
			sc = ((ParsedLDAConfiguration) config).simpleLDAConfiguration();
		} else {
			sc = (SimpleLDAConfiguration) config;
		}
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
		
		p_w_coll = calculateProbWordGivenCorpus(trainingset);
		
		if(storedSampler.exists() 
				&& trainingsetHash != null 
				&& trainingsetHash.equals(storedHash)) {
			try {
				System.out.println("Using pretrained sampler @:" + storedSampler.getAbsolutePath());
				trainedSampler = PolyaUrnSpaliasLDA.read(storedSampler);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			trainingset.getAlphabet().stopGrowth();
			if(config==null) {
				config = new SimpleLDAConfiguration();
			}
			
			trainedSampler = new PolyaUrnSpaliasLDA(config);
			trainedSampler.addInstances(trainingset);
			try {
				trainedSampler.sample(config.getNoIterations(3000));
				File tmpDir = new File("stored_samplers");
				if(!tmpDir.exists()) {
					tmpDir.mkdir();
				}
				((PolyaUrnSpaliasLDA) trainedSampler).write(storedSampler);
				if(trainingsetHash!=null) {
					writeTrainingsetHash(trainingsetHash,samplerFn + "-training_hash-" + trainingsetHash);
				}
			} catch (IOException e) {
				e.printStackTrace();
				throw new IllegalStateException(e);
			}
		}
		trainingSetTopicDists = trainedSampler.getThetaEstimate();
		phi = trainedSampler.getPhi();

		for (int i = 0; i < trainingset.size(); i++) {
			Instance instance = trainingset.get(i);
			int[] wordTokensQuery = LDAUtils.getWordTokens(instance);
			int hashCodeQuery = Arrays.hashCode(wordTokensQuery);
			cache.put(hashCodeQuery, trainingSetTopicDists[i]);
			sampledTopics.put(instance, trainingSetTopicDists[i]);
		}
		
		System.out.println("Top words of the trained sampler are: \n" + 
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

	 double [] calculateProbWordGivenCorpus(InstanceList trainingset) {
		 double [] p_w_coll = new double[trainingset.getAlphabet().size()];
		 long corpusSize = 0;
		 for (int i = 0; i < trainingset.size(); i++) {
			 FeatureSequence words = (FeatureSequence)trainingset.get(i).getData();
			 for (int j = 0; j < words.size(); j++) {
				 p_w_coll[words.getIndexAtPosition(j)]++;
				 corpusSize++;
			 }
		 }	

		 //normalize
		 for (int i = 0; i < p_w_coll.length; i++) {
			 p_w_coll[i] /= corpusSize;
		 }
		 
		 return p_w_coll;
	}

	 static double [] calculateProbWordGivenCorpusMLWordEncoding(int [][] trainingset, int [] vocabulary) {
		 double [] p_w_coll = new double[vocabulary.length];
		 long corpusSize = 0;
		 for (int i = 0; i < trainingset.length; i++) {
			 int [] words = trainingset[i];
			 for (int j = 0; j < words.length; j++) {
				 p_w_coll[words[j]]++;
				 corpusSize++;
			 }
		 }	

		 //normalize
		 for (int i = 0; i < p_w_coll.length; i++) {
			 p_w_coll[i] /= corpusSize;
		 }
		 
		 return p_w_coll;
	}

	private String getTrainingSetHash() {
		if(config==null) return null;
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
		double [] theta = trainingSetTopicDists[sampleId];
		
		int [] v1 = new int[query.length];
		for (int i = 0; i < v1.length; i++) {
			v1[i] = (int)query[i];
		}

		TokenFrequencyVectorizer tv = new TokenFrequencyVectorizer();
		int [] v2 = Arrays
				.stream(tv.instanceToVector(trainingset.get(sampleId)))
				.mapToInt(x -> (int)x)
				.toArray();
		
		return -ldaLoglikelihood(v1, v2, theta);
	}

	public double[] getSampledQueryTopics(double[] instanceVector) {
		return sampledQueryTopics.get(Arrays.hashCode(instanceVector));
	}

	public void initModel(int[][] trainingset, int[] vocab) {
		p_w_coll = calculateProbWordGivenCorpusMLWordEncoding(trainingset,vocab);
		
	}

	public void initModel(int[][] trainingset, int[] vocab, double [][] phi) {
		p_w_coll = calculateProbWordGivenCorpusMLWordEncoding(trainingset,vocab);
		setPhi(phi);
		
	}

	public double getMu() {
		return mu;
	}

	public void setMu(double mu) {
		this.mu = mu;
	}

	@Override
	public double distance(Instance instance1, Instance instance2) {
		TokenFrequencyVectorizer tv = new TokenFrequencyVectorizer();
		int [] query = tv.instanceToIntVector(instance1);
		int [] document = tv.instanceToIntVector(instance2);
		
		double [] docTheta;
		if(sampledTopics.get(instance2) == null) {
			docTheta = sample(instance2);
			sampledTopics.put(instance2, docTheta);
		} else {
			docTheta = sampledTopics.get(instance2);
		}
		
		return -ldaLoglikelihood(query, document, docTheta);
	}
}
