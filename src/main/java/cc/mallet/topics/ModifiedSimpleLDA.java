package cc.mallet.topics;


import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.NotImplementedException;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.topics.SimpleLDA;
import cc.mallet.topics.TopicAssignment;
import cc.mallet.types.FeatureSequence;
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
		logger.setLevel(Level.FINE);
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


}
