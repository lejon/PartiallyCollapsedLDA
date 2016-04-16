package cc.mallet.topics;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.NotImplementedException;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Dirichlet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelSequence;
import cc.mallet.util.MalletLogger;
import cc.mallet.util.Randoms;

public class ModifiedSimpleLDA extends SimpleLDA implements LDAGibbsSampler, AbortableSampler {

	protected static volatile boolean abort = false;
	/**
	 * Base class for all modifications of SimpleLDA done to test the Partially Collapsed Gibbs Sampler.
	 * I had to extend SimpleLDA to instantiate a new logger to manage input, as the original logger is
	 * private in the library implementation.
	 * 
	 * @author Paolo Elena, Leif Jonsson
	 */
	private static final long serialVersionUID = 1L;
	int startSeed;
	
	protected Alphabet targetAlphabet;

	protected LDAConfiguration config;

	protected InstanceList testSet = null; 
	protected int currentIteration = 0;
	
	// Used for random scan in subclass
	protected int [] typeCounts;
	// A vector of type indices sorted so the first element contains the index of the
	// type that occurs most in the corpus and so on in descending order
	protected int [] typeFrequencyIndex;
	// The cumulative sum of the token mass, (0-1)
	protected double [] typeFrequencyCumSum;
	
	// for dirichlet estimation
	public int[] docLengthCounts; // histogram of document sizes
	public int[][] topicDocCounts; // histogram of document/topic counts, indexed by <topic index, sequence position index>
	public int totalTokens;
	boolean usingSymmetricAlpha = true;
	// The number of times each type appears in the corpus
	int[] typeTotals;
	// The max over typeTotals, used for beta optimization
	int maxTypeCount;
	private boolean histogramsInitialized; 
	
	public ModifiedSimpleLDA(LDAConfiguration conf) {
		super(conf.getNoTopics(LDAConfiguration.NO_TOPICS_DEFAULT), 
				conf.getAlpha(LDAConfiguration.ALPHA_DEFAULT)*conf.getNoTopics(LDAConfiguration.NO_TOPICS_DEFAULT), 
				conf.getBeta(LDAConfiguration.BETA_DEFAULT), 
				new Randoms(conf.getSeed(LDAConfiguration.SEED_DEFAULT)));
		this.config = conf;
		setRandomSeed(conf.getSeed(LDAConfiguration.SEED_DEFAULT));
		printLogLikelihood = false;
		showTopicsInterval = conf.getTopicInterval(LDAConfiguration.TOPIC_INTER_DEFAULT);
		logger.info("Simple LDA: " + numTopics + " topics");
		logger.setLevel(Level.INFO);
	}

	@Override
	public void setRandomSeed(int seed) {
		super.setRandomSeed(seed);
		startSeed = seed;
	}

	public int getStartSeed() {
		return startSeed;
	}

	public InstanceList getTestSet() {
		return testSet;
	}

	public void setTestSet(InstanceList testSet) {
		this.testSet = testSet;
	}

	protected static Logger logger = MalletLogger.getLogger(SimpleLDA.class.getName());

	public void setShowTopicsInterval(int interval) {
		showTopicsInterval = interval;
	}

	@Override
	/**
	 * Serial collapsed Gibbs sampler for training the SimpleLDA model.
	 * The code is copied from cc.mallet.topics.SimpleLDA.java, with the sole purpose
	 * of using the new logger (the original one was set on too low a level, I needed Level.FINE)
	 */
	public void sample (int iterations) throws IOException {
		String loggingPath = config.getLoggingUtil().getLogDir().getAbsolutePath();
		double logLik = modelLogLikelihood();
		String tw = topWords (wordsPerTopic);
		LDAUtils.logLikelihoodToFile(logLik,0,tw,loggingPath,logger);

		for (int iteration = 1; iteration <= iterations && !abort; iteration++) {
			currentIteration = iteration;

			long iterationStart = System.currentTimeMillis();

			// Loop over every document in the corpus
			for (int doc = 0; doc < data.size(); doc++) {
				FeatureSequence tokenSequence =
						(FeatureSequence) data.get(doc).instance.getData();
				LabelSequence topicSequence =
						(LabelSequence) data.get(doc).topicSequence;

				sampleTopicsForOneDoc (tokenSequence, topicSequence);
			} 

			long elapsedMillis = System.currentTimeMillis() - iterationStart;
			logger.fine(iteration + "\t" + elapsedMillis + "ms\t");

			if (showTopicsInterval > 0 && iteration % showTopicsInterval == 0) {
				logLik = modelLogLikelihood();
				tw = topWords (wordsPerTopic);
				LDAUtils.logLikelihoodToFile(logLik,iteration,tw,loggingPath,logger);
			}
		}
	}

	/**
	 * Returns an array, indexed by the integer value of each token, containing
	 * how many times the token appeared in the original dataset. As the subclass
	 * saves this computation, it is not necessary to recompute it as in the overridden
	 * method.
	 */
	public int[] countOccurrencesOfEachToken() {
		int alphabetSize = alphabet.size();
		System.out.println("Alphabet size = " + alphabetSize);
		int[] countsForEachToken = new int[alphabetSize];
		for (int doc = 0; doc < data.size(); doc++) {
			FeatureSequence tokenSequence =
					(FeatureSequence) data.get(doc).instance.getData();
			int docLength = tokenSequence.getLength();
			for (int position = 0; position < docLength; position++) {
				int type = tokenSequence.getIndexAtPosition(position);
				countsForEachToken[type]++;
			}
		}
		return countsForEachToken;
	} 

	public String [][] getTopWords(int noWords) {
		String[][] topTopicWords = LDAUtils.getTopWords(noWords, numTypes, numTopics, typeTopicCounts, alphabet);
		return topTopicWords;
	}

	@Override
	public void setConfiguration(LDAConfiguration config) {
		this.config = config;
	}

	@Override
	public int[][] getZIndicators() {
		int [][] indicators = new int[data.size()][];
		for (int doc = 0; doc < data.size(); doc++) {
			FeatureSequence tokenSequence =	(FeatureSequence) data.get(doc).instance.getData();
			LabelSequence topicSequence = (LabelSequence) data.get(doc).topicSequence;
			int[] oneDocTopics = topicSequence.getFeatures();
			int docLength = tokenSequence.getLength();
			indicators[doc] = new int [docLength];
			for (int position = 0; position < docLength; position++) {
				indicators[doc][position] = oneDocTopics[position];
			}
		}
		return indicators;
	}
	
	@Override
	public void setZIndicators(int[][] zIndicators) {
		throw new NotImplementedException("Setting start values is not implemented yet! :(");
	}

	@Override
	public int getNoTopics() {
		return numTopics;
	}

	@Override
	public int getCurrentIteration() {
		return currentIteration;
	}

	@Override
	public ArrayList<TopicAssignment> getDataset() {
		return super.getData();
	}
	
	@Override
	public int[][] getDeltaStatistics() {
		throw new NotImplementedException("Delta statistics is not implemented yet! :(");	
	}
	
	@Override
	public int[] getTopTypeFrequencyIndices() {
		return typeFrequencyIndex;
	}

	@Override
	public double[] getTypeMassCumSum() {
		return typeFrequencyCumSum;
	}

	@Override
	public int[] getTypeFrequencies() {
		return typeCounts;
	}
	
	@Override
	public int getCorpusSize() {
		int corpusWordCount = 0;
		for (int doc = 0; doc < data.size(); doc++) {
			FeatureSequence tokens =
					(FeatureSequence) data.get(doc).instance.getData();
			int docLength = tokens.size();
			corpusWordCount += docLength;
		}
		return corpusWordCount;
	}
	
	@Override
	public int[][] getDocumentTopicMatrix() {
		int [][] res = new int[data.size()][];
		for (int docIdx = 0; docIdx < data.size(); docIdx++) {
			int[] topicSequence = data.get(docIdx).topicSequence.getFeatures();
			res[docIdx] = new int[numTopics];
			for (int position = 0; position < topicSequence.length; position++) {
				int topicInd = topicSequence[position];
				res[docIdx][topicInd]++;
			}
		}
		return res;
	}

	@Override
	public int[][] getTypeTopicMatrix() {
		int [][] res = new int[typeTopicCounts.length][typeTopicCounts[0].length];
		for (int i = 0; i < res.length; i++) {
			for (int j = 0; j < res[i].length; j++) {
				res[i][j] = typeTopicCounts[i][j];
			}
		}
		return res;
	}
	
	protected int sum(int[] vector) {
		int sum = 0;
		for (int i = 0; i < vector.length; i++) {
			sum += vector[i];
		}
		return sum;
	}

	protected int sum(int[][] matrix) {
		int sum = 0;
		for (int row = 0; row < matrix.length; row++) {
			for (int col = 0; col < matrix[row].length; col++) {
				sum += matrix[row][col];
			}
		}
		return sum;
	}
	
	/* 
	 * Updates the vector tokensPerTopic used in modelLogLikelihood
	 */
	protected void updateTokensPerTopicSerial() {
		tokensPerTopic = new int[numTopics];
		for ( int type = 0; type < typeTopicCounts.length; type++ ) {
			for( int topic = 0; topic < typeTopicCounts[type].length; topic++) {
				tokensPerTopic[topic] += typeTopicCounts[type][topic];
			}
		}
	}

	@Override
	public void abort() {
		abort = true; 
	}

	@Override
	public boolean getAbort() {
		return abort;
	}


	/**
	 * Returns the document topic means. Observe that a document can
	 * have a zero mean for one topic. If you want the theta estimate use
	 * <code>getThetaEstimate</code> instead
	 * @return document topic means 
	 */
	public double [][] getZbar() {
		return ModifiedSimpleLDA.getZbar(data,numTopics);
	}

	/**
	 * Returns the document topic means. Observe that a document can
	 * have a zero mean for one topic. If you want the theta estimate use
	 * <code>getThetaEstimate</code> instead
	 * 
	 * @param data
	 * @param numTopics
	 * @return document topic means 
	 */
	public static double [][] getZbar(ArrayList<TopicAssignment> data, int numTopics) {
		double [][] docTopicMeans = new double [data.size()][];
		for (int docIdx = 0; docIdx < data.size(); docIdx++) {
			FeatureSequence tokenSequence =
					(FeatureSequence) data.get(docIdx).instance.getData();
			LabelSequence topicSequence =
					(LabelSequence) data.get(docIdx).topicSequence;
	
			int docLength = tokenSequence.getLength();
			int [] oneDocTopics = topicSequence.getFeatures();
	
			docTopicMeans[docIdx] = calcZBar(numTopics, docLength, oneDocTopics);
		}
		return docTopicMeans;
	}


	static double[] calcZBar(int numTopics, int docLength, int[] oneDocTopics) {
		double[] docTopicMeans = new double[numTopics];
		double[] localTopicCounts = new double[numTopics];

		for (int position = 0; position < docLength; position++) {
			int topicInd = oneDocTopics[position];
			localTopicCounts[topicInd]++;
		}

		for (int k = 0; k < numTopics; k++) {
			if(docLength==0) {
				docTopicMeans[k] = 0.0;
			} else {					
				docTopicMeans[k] = localTopicCounts[k] / docLength;
			}
			if(Double.isInfinite(docTopicMeans[k]) || Double.isNaN(docTopicMeans[k]) || docTopicMeans[k] < 0) { 
				throw new IllegalStateException("docTopicMeans is broken: " +  " Topic=" + k + " mean=" + docTopicMeans[k]);  
			}
		}
		return docTopicMeans;
	}
	
	/**
	 * Returns an estimate of theta
	 * @return estimate of theta
	 */
	public double [][] getThetaEstimate() {
		return ModifiedSimpleLDA.getThetaEstimate(data,numTopics,alpha);
	}

	/**
	 * Returns an estimate of theta, this differs from <code>getZBar</code> in that it will never
	 * contain zeros for any topic (unless it gets an abnormal alpha = 0)
	 * 
	 * @param data
	 * @param numTopics
	 * @param alpha
	 * @return estimate of theta
	 */
	public static double [][] getThetaEstimate(ArrayList<TopicAssignment> data, int numTopics, double alpha) {
		double [][] thetaEstimate = new double[data.size()][];
		for (int docIdx = 0; docIdx < data.size(); docIdx++) {
			FeatureSequence tokenSequence =
					(FeatureSequence) data.get(docIdx).instance.getData();
			LabelSequence topicSequence =
					(LabelSequence) data.get(docIdx).topicSequence;
	
			int docLength = tokenSequence.getLength();
			int [] oneDocTopics = topicSequence.getFeatures();
	
			thetaEstimate[docIdx] = calcThetaEstimate(numTopics, alpha, docLength, oneDocTopics);
		}
		return thetaEstimate;
	}

	public static double [] getThetaEstimate(Instance instance, int numTopics, double alpha, int[] oneDocTopics) {
		FeatureSequence tokenSequence =	(FeatureSequence) instance.getData();
		int docLength = tokenSequence.getLength();

		return calcThetaEstimate(numTopics, alpha, docLength, oneDocTopics);
	}

	static double [] calcThetaEstimate(int numTopics, double [] alpha, int docLength, 	int[] oneDocTopics) {
		double [] thetaEstimate = new double[numTopics];
		double[] localTopicCounts = new double[numTopics];

		for (int position = 0; position < docLength; position++) {
			int topicInd = oneDocTopics[position];
			localTopicCounts[topicInd]++;
		}

		double normalizer = 0.0;
		for (int k = 0; k < numTopics; k++) {
			normalizer += localTopicCounts[k] + alpha[k];
		}

		for (int k = 0; k < numTopics; k++) {
			if(docLength==0) {
				thetaEstimate[k] = 0.0;
			} else {					
				thetaEstimate[k] = (localTopicCounts[k] + alpha[k]) / normalizer;
			}
			if(Double.isInfinite(thetaEstimate[k]) || Double.isNaN(thetaEstimate[k]) || thetaEstimate[k] < 0) { 
				throw new IllegalStateException("theta estimate is broken: " + " Topic=" + k + " theta est=" + thetaEstimate[k]);  
			}
		}
		return thetaEstimate;
	}
	
	static double [] calcThetaEstimate(int numTopics, double alpha, int docLength, int[] oneDocTopics) {
		double [] thetaEstimate = new double[numTopics];
		double[] localTopicCounts = new double[numTopics];

		for (int position = 0; position < docLength; position++) {
			int topicInd = oneDocTopics[position];
			localTopicCounts[topicInd]++;
		}
		
		double normalizer = 0.0;
		for (int k = 0; k < numTopics; k++) {
			normalizer += localTopicCounts[k] + alpha;
		}

		for (int k = 0; k < numTopics; k++) {
			if(docLength==0) {
				thetaEstimate[k] = 0.0;
			} else {					
				thetaEstimate[k] = (localTopicCounts[k] + alpha) / normalizer;
			}
			if(Double.isInfinite(thetaEstimate[k]) || Double.isNaN(thetaEstimate[k]) || thetaEstimate[k] < 0) { 
				throw new IllegalStateException("theta estimate is broken:" + " Topic=" + k + " theta est=" + thetaEstimate[k]);  
			}
		}
		return thetaEstimate;
	}
	
	/**
	 * Returns an estimate of theta, this differs from <code>getZBar</code> in that it will never
	 * contain zeros for any topic (unless it gets an abnormal alpha = 0)
	 * 
	 * @param data
	 * @param numTopics
	 * @param alpha
	 * @return estimate of theta
	 */
	public static double [][] getThetaEstimate(ArrayList<TopicAssignment> data, int numTopics, double [] alpha) {
		double [][] thetaEstimate = new double [data.size()][numTopics];
		for (int docIdx = 0; docIdx < data.size(); docIdx++) {
			FeatureSequence tokenSequence =
					(FeatureSequence) data.get(docIdx).instance.getData();
			LabelSequence topicSequence =
					(LabelSequence) data.get(docIdx).topicSequence;
	
			int docLength = tokenSequence.getLength();
			int [] oneDocTopics = topicSequence.getFeatures();
	
			thetaEstimate[docIdx] = calcThetaEstimate(numTopics, alpha, docLength, oneDocTopics);
		}
		return thetaEstimate;
	}



	@Override
	public void preIteration() {
		
	}

	@Override
	public void postIteration() {

	}

	@Override
	public void preSample() {
		
	}

	@Override
	public void postSample() {
		
	}

	@Override
	public void postZ() {
		
	}

	@Override
	public void preZ() {
		
	}
	
	/** 
	 *  Gather statistics on the size of documents 
	 *  and create histograms for use in Dirichlet hyperparameter
	 *  optimization.
	 */
	protected void initializeHistograms() {
		int maxTokens = 0;
		totalTokens = 0;
		int seqLen;

		Map<Integer,Integer> docLenCnts = new java.util.HashMap<Integer, Integer>();
		for (int doc = 0; doc < data.size(); doc++) {
			FeatureSequence tokens = (FeatureSequence) data.get(doc).instance.getData();
			seqLen = tokens.getLength();
			if (seqLen > maxTokens)
				maxTokens = seqLen;
			totalTokens += seqLen;
			
			if(docLenCnts.get(seqLen) == null) {
				docLenCnts.put(seqLen,0); 
			}
			docLenCnts.put(seqLen,docLenCnts.get(seqLen) + 1);
			
			for (int position = 0; position < tokens.getLength(); position++) {
				int type = tokens.getIndexAtPosition(position);
				typeTotals[ type ]++;
			}
		}
		
		for (int type = 0; type < numTypes; type++) {
			if (typeTotals[type] > maxTypeCount) { maxTypeCount = typeTotals[type]; }
		}

		logger.info("max tokens: " + maxTokens);
		logger.info("total tokens: " + totalTokens);

		docLengthCounts = new int[maxTokens + 1];
		for (int i = 0; i < docLengthCounts.length; i++) {
			if(docLenCnts.get(i)!=null)
				docLengthCounts[i] = docLenCnts.get(i);
		}
		topicDocCounts = new int[numTopics][maxTokens + 1];
		
		betaSum = beta * numTypes;
		histogramsInitialized = true;
	}
	
	public void optimizeAlpha(WorkerRunnable[] runnables) {
		if(!histogramsInitialized) throw new IllegalStateException("initializeHistograms has not been called beefore calling optimizeAlpha!");
		// First clear the sufficient statistic histograms

		Arrays.fill(docLengthCounts, 0);
		for (int topic = 0; topic < topicDocCounts.length; topic++) {
			Arrays.fill(topicDocCounts[topic], 0);
		}

		
		for (int thread = 0; thread < runnables.length; thread++) {
			int[][] sourceTopicCounts = runnables[thread].getTopicDocCounts();

			for (int topic=0; topic < numTopics; topic++) {

				if (! usingSymmetricAlpha) {
					for (int count=0; count < sourceTopicCounts[topic].length; count++) {
						if (sourceTopicCounts[topic][count] > 0) {
							topicDocCounts[topic][count] += sourceTopicCounts[topic][count];
							sourceTopicCounts[topic][count] = 0;
						}
					}
				}
				else {
					// For the symmetric version, we only need one 
					//  count array, which I'm putting in the same 
					//  data structure, but for topic 0. All other
					//  topic histograms will be empty.
					// I'm duplicating this for loop, which 
					//  isn't the best thing, but it means only checking
					//  whether we are symmetric or not numTopics times, 
					//  instead of numTopics * longest document length.
					for (int count=0; count < sourceTopicCounts[topic].length; count++) {
						if (sourceTopicCounts[topic][count] > 0) {
							topicDocCounts[0][count] += sourceTopicCounts[topic][count];
							//			 ^ the only change
							sourceTopicCounts[topic][count] = 0;
						}
					}
				}
			}
		}

		if (usingSymmetricAlpha) {
			alphaSum = Dirichlet.learnSymmetricConcentration(topicDocCounts[0],
															 docLengthCounts,
															 numTopics,
															 alphaSum);
			//for (int topic = 0; topic < numTopics; topic++) {
				//alpha[topic] = alphaSum / numTopics;
				alpha = alphaSum / numTopics;
			//}
		}
		else {
			throw new UnsupportedOperationException("Assymetric alpha not implemented yet!");
			//alphaSum = Dirichlet.learnParameters(alpha, topicDocCounts, docLengthCounts, 1.001, 1.0, 1);
		}
	}

	public void optimizeBeta(WorkerRunnable[] runnables) {
		if(!histogramsInitialized) throw new IllegalStateException("initializeHistograms has not been called beefore calling optimizeBeta!");
		// The histogram starts at count 0, so if all of the
		//  tokens of the most frequent type were assigned to one topic,
		//  we would need to store a maxTypeCount + 1 count.
		int[] countHistogram = new int[maxTypeCount + 1];
		
		// Now count the number of type/topic pairs that have
		//  each number of tokens.

		int index;
		for (int type = 0; type < numTypes; type++) {
			int[] counts = typeTopicCounts[type];
			index = 0;
			while (index < counts.length) {
				countHistogram[counts[index]]++;
				index++;
			}
		}
			
		// Figure out how large we need to make the "observation lengths"
		//  histogram.
		int maxTopicSize = 0;
		for (int topic = 0; topic < numTopics; topic++) {
			if (tokensPerTopic[topic] > maxTopicSize) {
				maxTopicSize = tokensPerTopic[topic];
			}
		}

		// Now allocate it and populate it.
		int[] topicSizeHistogram = new int[maxTopicSize + 1];
		for (int topic = 0; topic < numTopics; topic++) {
			topicSizeHistogram[ tokensPerTopic[topic] ]++;
		}

		betaSum = Dirichlet.learnSymmetricConcentration(countHistogram,
														topicSizeHistogram,
														numTypes,
														betaSum);
		beta = betaSum / numTypes;
		

		logger.info("[beta: " + formatter.format(beta) + "] ");		
	}
}
