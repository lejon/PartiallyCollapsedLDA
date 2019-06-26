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
import java.util.Map;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.configuration.ModelFactory;
import cc.mallet.configuration.ParsedLDAConfiguration;
import cc.mallet.configuration.SimpleLDAConfiguration;
import cc.mallet.pipe.Pipe;
import cc.mallet.topics.LDASamplerWithPhi;
import cc.mallet.topics.PolyaUrnSpaliasLDA;
import cc.mallet.topics.SpaliasUncollapsedParallelLDA;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.util.ArrayStringUtils;
import cc.mallet.util.LDAUtils;
import cc.mallet.util.StringClassArrayIterator; 

public class LDADistancer implements TrainedDistance, InstanceDistance {
	static DecimalFormat mydecimalFormat = new DecimalFormat("00.###E0");
	public static int noDigits = 4;

	public boolean abort = false;
	LDAConfiguration config;
	
	InstanceList trainingset;
	LDASamplerWithPhi trainedSampler;
	double alpha;
	String [] testRowIds;
	Map<Instance, double []> sampledTopics = new HashMap<>();
	Map<Integer, double []> sampledQueryTopics = new HashMap<>();
	double [][] trainingSetTopicDists;
	Map<Integer,double []> cache = new HashMap<>();
	
	int testSampleIter = 500;
	
	Distance dist = new KLDistance();

	Pipe instancePipe;
	
	String samplerFn = "stored_samplers/saved_similarity_sampler.bin";
	
	public LDADistancer(LDAConfiguration config) {
		this.config = config;
		alpha = config.getAlpha(0.01);
	}

	public LDADistancer(LDAConfiguration config, Distance d) {
		this(config);
		this.dist = d;
	}

	public LDADistancer(LDASamplerWithPhi ldaModel) {
		this.config = ldaModel.getConfiguration();
		alpha = config.getAlpha(0.01);
		trainedSampler = ldaModel;
		trainingset = ldaModel.getDataset();
		trainingSetTopicDists = ldaModel.getThetaEstimate();
	}

	public LDADistancer(LDASamplerWithPhi ldaModel, Distance d) {
		this(ldaModel);
		dist = d;
	}

	public Distance getDist() {
		return dist;
	}

	public void setDist(Distance dist) {
		this.dist = dist;
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
		
		if(trainingset.contains(instance)) {
			int idx = trainingset.indexOf(instance);
			docTheta = trainingSetTopicDists[idx]; 
		}
		else if(cache.containsKey(hashCode)) {			
			docTheta = cache.get(hashCode);
		} else {
			InstanceList currentTestset = new InstanceList(trainingset.getDataAlphabet(), trainingset.getTargetAlphabet());
			currentTestset.add(instance);
			SimpleLDAConfiguration sc;
			if(config instanceof SimpleLDAConfiguration) {
				sc = (SimpleLDAConfiguration) config;
			} else {
				sc = ((ParsedLDAConfiguration) config).simpleLDAConfiguration();
			}
			sc.setTopicInterval(100);
			LDASamplerWithPhi spalias = (LDASamplerWithPhi) ModelFactory.get(sc);
			spalias.addInstances(currentTestset);
			spalias.setPhi(trainedSampler.getPhi(), instance.getAlphabet(), instance.getTargetAlphabet());
			spalias.sampleZGivenPhi(testSampleIter);
			double [][] thetaEstimate = spalias.getThetaEstimate();
			docTheta = thetaEstimate[0];
			cache.put(hashCode, docTheta);
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
				double distance = dist.calculate(docTheta, trainingDoc);
				//System.out.println("Divergence vs. " + instance +" is:" + klDivergence);
				// We need to transform the kl-divergencies (low is good) to scores (high is good)
				distances[i] = distance;
			}
		}
				
		//System.out.println("["  + instance.getTarget().toString() + "]: Scores are: " + arrToStr(scores));
		
		return distances;
	}

	public LDASamplerWithPhi train(InstanceList trainingset) throws IOException {
		String trainingsetHash = getConfigSetHash();
		String storedHash = readStoredTrainingsetHash(samplerFn + "-training_hash-" + trainingsetHash);
		File storedSampler = new File(samplerFn + "-sampler-" + trainingsetHash);
		this.trainingset = trainingset;
		if(storedSampler.exists() && trainingsetHash.equals(storedHash)) {
			try {
				System.out.println("Using pretrained sampler @:" + storedSampler.getAbsolutePath());
				LDASamplerWithPhi tmp = (LDASamplerWithPhi) ModelFactory.get(config);
				if(tmp instanceof PolyaUrnSpaliasLDA) {
					System.out.println("Loading PolyaUrn sampler...");
					trainedSampler = PolyaUrnSpaliasLDA.read(storedSampler);
				} else if (tmp instanceof SpaliasUncollapsedParallelLDA) {
					System.out.println("Loading Spalias sampler...");
					trainedSampler = SpaliasUncollapsedParallelLDA.read(storedSampler);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			trainingset.getAlphabet().stopGrowth();
			trainedSampler = new PolyaUrnSpaliasLDA(config);
			trainedSampler.addInstances(trainingset);
			try {
				System.out.println("Using config: " + config.getActiveSubConfig());
				trainedSampler.sample(config.getNoIterations(3000));
				File tmpDir = new File("stored_samplers");
				if(!tmpDir.exists()) {
					tmpDir.mkdir();
				}
				if(trainedSampler instanceof PolyaUrnSpaliasLDA) {
					System.out.println("Storing PolyaUrn sampler...");
					((PolyaUrnSpaliasLDA) trainedSampler).write(storedSampler);
				} else if (trainedSampler instanceof SpaliasUncollapsedParallelLDA) {
					System.out.println("Storing SpaliasUncollapsedParallelLDA sampler...");
					((SpaliasUncollapsedParallelLDA) trainedSampler).write(storedSampler);
				}
				writeTrainingsetHash(trainingsetHash,samplerFn + "-training_hash-" + trainingsetHash);
			} catch (IOException e) {
				e.printStackTrace();
				throw new IllegalStateException(e);
			}
		}
		
		trainingSetTopicDists = trainedSampler.getThetaEstimate();
		
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
			if(arr[j]<0.05 && mark) {
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

		return dist.calculate(theta1, theta2);
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
		try {
			train(trainingset);
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
		
		for (int i = 0; i < trainingset.size(); i++) {
			Instance instance = trainingset.get(i);
			sampledTopics.put(instance, trainingSetTopicDists[i]);
		}
		
		int numWords = Math.min(10, trainedSampler.getAlphabet().size());
		System.out.println("Top words of the traines sampler are: \n" + 
				LDAUtils.formatTopWords(LDAUtils.getTopWords(numWords, 
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

	private String getConfigSetHash() {
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
	public double distance(Instance instance1, Instance instance2) {
		int [] v1Indices = LDAUtils.instanceToTokenIndices(instance1);
		
		double [] theta1;
		int hashCode = Arrays.hashCode(LDAUtils.getWordTokens(instance1));
		if(!cache.containsKey(hashCode)) {	
			theta1 = sample(instance1);
			cache.put(hashCode, theta1);
		} else {
			theta1 = cache.get(hashCode);
		}
		
		sampledTopics.put(instance1, theta1);
		sampledQueryTopics.put(Arrays.hashCode(v1Indices), theta1);
		

		int [] v2Indices = LDAUtils.instanceToTokenIndices(instance2);
		double [] theta2;
		hashCode = Arrays.hashCode(LDAUtils.getWordTokens(instance2));
		if(!cache.containsKey(hashCode)) {	
			theta2 = sample(instance2);
			cache.put(hashCode, theta2);
		} else {
			theta2 = cache.get(hashCode);
		}
		
		sampledTopics.put(instance2, theta2);
		sampledQueryTopics.put(Arrays.hashCode(v2Indices), theta2);
		
		double dd = dist.calculate(theta1, theta2);
		return dd;
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

		double dd = dist.calculate(theta1, theta2);
//		System.out.println("Comparing: \n\t" 
//				+ s1 + "\n\t" 
//				+ Arrays.toString(theta1) + "\n\t" 
//				+ Arrays.toString(theta2) + "\n\t"  
//				+ LDAUtils.instanceToString(trainingset.get(sampleId)));
//		System.out.println("Distance was: " + dd);
		return dd;
	}

	public double[] getSampledQueryTopics(double[] instanceVector) {
		return sampledQueryTopics.get(Arrays.hashCode(instanceVector));
	}
}
