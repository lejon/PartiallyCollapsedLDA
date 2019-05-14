package cc.mallet.topics;


import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.NotImplementedException;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Dirichlet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.IDSorter;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.LabelSequence;
import cc.mallet.types.SparseDirichlet;
import cc.mallet.types.SparseDirichletSamplerBuilder;
import cc.mallet.util.LDAUtils;
import cc.mallet.util.MalletLogger;
import cc.mallet.util.Randoms;

/**
 * Derivative of SimpleLDA, but with support for non-uniform alpha and beta
 * 
 * @author Leif Jonsson
 */
public class ModifiedSimpleLDA implements LDAGibbsSampler, AbortableSampler, Serializable {

	protected static Logger logger = MalletLogger.getLogger(ModifiedSimpleLDA.class.getName());

	// the training instances and their topic assignments
	protected ArrayList<TopicAssignment> data;  
	
	// The original training data
	InstanceList trainingData;

	// the alphabet for the input data
	protected Alphabet alphabet; 

	// the alphabet for the topics
	protected LabelAlphabet topicAlphabet; 

	// The number of topics requested
	protected int numTopics;

	// The size of the vocabulary
	protected int numTypes;

	// Prior parameters
	//protected double alpha;	 // Dirichlet(alpha,alpha,...) is the distribution over topics
	protected double [] alpha;	 // Dirichlet(alpha,alpha,...) is the distribution over topics
	protected double alphaSum;
	protected double beta;   // Prior on per-topic multinomial distribution over words
	protected double betaSum;
	public static final double DEFAULT_BETA = 0.01;

	// An array to put the topic counts for the current document. 
	// Initialized locally below.  Defined here to avoid
	// garbage collection overhead.
	protected int[] oneDocTopicCounts; // indexed by <document index, topic index>

	// Statistics needed for sampling.
	protected int[][] typeTopicCounts; // indexed by <feature index, topic index>
	protected int[] tokensPerTopic; // indexed by <topic index>

	public int showTopicsInterval = 50;
	public int wordsPerTopic = 10;

	protected Randoms random;
	protected NumberFormat formatter;
	protected boolean printLogLikelihood = false;
	
	// Structures for hyperparameter optimization 
	protected AtomicInteger [][] documentTopicHistogram;
	protected int longestDocLength = -1;
	protected boolean saveHistStats = false;

	protected static volatile boolean abort = false;
	private static final long serialVersionUID = 1L;
	int startSeed;

	protected Alphabet targetAlphabet;

	protected LDAConfiguration config;

	protected InstanceList testSet = null; 
	protected int currentIteration = 0;

	// Used for random scan in subclass, number of occurences of each type in the corpus
	protected int [] typeCounts;
	// A vector of type indices sorted so the first element contains the index of the
	// type that occurs most in the corpus and so on in descending order
	protected int [] typeFrequencyIndex;
	// The cumulative sum of the token mass, (0-1)
	protected double [] typeFrequencyCumSum;

	// for dirichlet estimation
	public int[] docLengthCounts; // histogram of document sizes
	public int[][] topicDocCounts; // histogram of document/topic counts, indexed by <topic index, sequence position index>
	boolean usingSymmetricAlpha = true;
	// The max over typeTotals, used for beta optimization
	int maxTypeCount;
	
	List<Double> loglikelihood = new ArrayList<>();
	List<Double> heldOutLoglikelihood = new ArrayList<>();
	
	public static LabelAlphabet newTopicLabelAlphabet (int numTopics) {
		LabelAlphabet ret = new LabelAlphabet();
		for (int i = 0; i < numTopics; i++)
			ret.lookupIndex("topic"+i);
		return ret;
	}

	public ModifiedSimpleLDA(LDAConfiguration conf) {		
		this.data = new ArrayList<TopicAssignment>();
		this.topicAlphabet = newTopicLabelAlphabet (conf.getNoTopics(LDAConfiguration.NO_TOPICS_DEFAULT));
		this.numTopics = topicAlphabet.size();

		double alphaConf = conf.getAlpha(LDAConfiguration.ALPHA_DEFAULT);
		this.alphaSum = alphaConf*conf.getNoTopics(LDAConfiguration.NO_TOPICS_DEFAULT);

		this.alpha = new double[numTopics];
		for (int i = 0; i < alpha.length; i++) {
			alpha[i] = alphaConf;
		}
		this.beta = conf.getBeta(LDAConfiguration.BETA_DEFAULT);
		this.random = new Randoms(conf.getSeed(LDAConfiguration.SEED_DEFAULT));
		
		oneDocTopicCounts = new int[numTopics];
		tokensPerTopic = new int[numTopics];
		
		formatter = NumberFormat.getInstance();
		formatter.setMaximumFractionDigits(5);
		
		this.config = conf;
		setRandomSeed(conf.getSeed(LDAConfiguration.SEED_DEFAULT));
		printLogLikelihood = false;
		showTopicsInterval = conf.getTopicInterval(LDAConfiguration.TOPIC_INTER_DEFAULT);
		logger.setLevel(Level.INFO);
	}

	@Override
	public void setRandomSeed(int seed) {
		random = new Randoms(seed);
		startSeed = seed;
	}

	protected void sampleTopicsForOneDoc (FeatureSequence tokenSequence,
			FeatureSequence topicSequence) {

		int[] oneDocTopics = topicSequence.getFeatures();

		int[] currentTypeTopicCounts;
		int type, oldTopic, newTopic;
		int docLength = tokenSequence.getLength();

		int[] localTopicCounts = new int[numTopics];

		//		populate topic counts
		for (int position = 0; position < docLength; position++) {
			localTopicCounts[oneDocTopics[position]]++;
		}

		double score, sum;
		double[] topicTermScores = new double[numTopics];

		//	Iterate over the positions (words) in the document 
		for (int position = 0; position < docLength; position++) {
			type = tokenSequence.getIndexAtPosition(position);
			oldTopic = oneDocTopics[position];

			// Grab the relevant row from our two-dimensional array
			currentTypeTopicCounts = typeTopicCounts[type];

			//	Remove this token from all counts. 
			localTopicCounts[oldTopic]--;
			tokensPerTopic[oldTopic]--;
			assert(tokensPerTopic[oldTopic] >= 0) : "old Topic " + oldTopic + " below 0";
			currentTypeTopicCounts[oldTopic]--;

			// Now calculate and add up the scores for each topic for this word
			sum = 0.0;

			// Here's where the math happens! Note that overall performance is 
			//  dominated by what you do in this loop.
			for (int topic = 0; topic < numTopics; topic++) {
				score =
						(alpha[topic] + localTopicCounts[topic]) *
						((beta + currentTypeTopicCounts[topic]) /
								(betaSum + tokensPerTopic[topic]));
				sum += score;
				topicTermScores[topic] = score;
			}

			// Choose a random point between 0 and the sum of all topic scores
			double sample = random.nextUniform() * sum;

			// Figure out which topic contains that point
			newTopic = -1;
			while (sample > 0.0) {
				newTopic++;
				sample -= topicTermScores[newTopic];
			}

			// Make sure we actually sampled a topic
			if (newTopic == -1) {
				throw new IllegalStateException ("SimpleLDA: New topic not sampled.");
			}

			// Put that new topic into the counts
			oneDocTopics[position] = newTopic;
			localTopicCounts[newTopic]++;
			tokensPerTopic[newTopic]++;
			currentTypeTopicCounts[newTopic]++;
		}
	}

	public double modelLogLikelihood() {
		double logLikelihood = 0.0;

		// The likelihood of the model is a combination of a 
		// Dirichlet-multinomial for the words in each topic
		// and a Dirichlet-multinomial for the topics in each
		// document.

		// The likelihood function of a dirichlet multinomial is
		//	 Gamma( sum_i alpha_i )	 prod_i Gamma( alpha_i + N_i )
		//	prod_i Gamma( alpha_i )	  Gamma( sum_i (alpha_i + N_i) )

		// So the log likelihood is 
		//	logGamma ( sum_i alpha_i ) - logGamma ( sum_i (alpha_i + N_i) ) + 
		//	 sum_i [ logGamma( alpha_i + N_i) - logGamma( alpha_i ) ]

		// Do the documents first

		int[] topicCounts = new int[numTopics];
		double[] topicLogGammas = new double[numTopics];
		int[] docTopics;

		for (int topic=0; topic < numTopics; topic++) {
			topicLogGammas[ topic ] = Dirichlet.logGamma( alpha[topic] );
		}

		for (int doc=0; doc < data.size(); doc++) {
			LabelSequence topicSequence = (LabelSequence) data.get(doc).topicSequence;

			docTopics = topicSequence.getFeatures();

			for (int token=0; token < docTopics.length; token++) {
				topicCounts[ docTopics[token] ]++;
			}

			for (int topic=0; topic < numTopics; topic++) {
				if (topicCounts[topic] > 0) {
					logLikelihood += (Dirichlet.logGamma(alpha[topic] + topicCounts[topic]) -
							topicLogGammas[ topic ]);
				}
			}

			// subtract the (count + parameter) sum term
			logLikelihood -= Dirichlet.logGamma(alphaSum + docTopics.length);

			Arrays.fill(topicCounts, 0);
		}

		// add the parameter sum term
		logLikelihood += data.size() * Dirichlet.logGamma(alphaSum);

		// And the topics

		// Count the number of type-topic pairs
		int nonZeroTypeTopics = 0;

		for (int type=0; type < numTypes; type++) {
			// reuse this array as a pointer

			topicCounts = typeTopicCounts[type];

			for (int topic = 0; topic < numTopics; topic++) {
				if (topicCounts[topic] == 0) { continue; }

				nonZeroTypeTopics++;
				logLikelihood += Dirichlet.logGamma(beta + topicCounts[topic]);

				if (Double.isNaN(logLikelihood)) {
					System.out.println(topicCounts[topic]);
					System.exit(1);
				}
			}
		}

		for (int topic=0; topic < numTopics; topic++) {
			logLikelihood -= 
					Dirichlet.logGamma( (beta * numTopics) +
							tokensPerTopic[ topic ] );
			if (Double.isNaN(logLikelihood)) {
				System.out.println("after topic " + topic + " " + tokensPerTopic[ topic ]);
				System.exit(1);
			}

		}

		logLikelihood += 
				(Dirichlet.logGamma(beta * numTopics)) -
				(Dirichlet.logGamma(beta) * nonZeroTypeTopics);

		if (Double.isNaN(logLikelihood)) {
			System.out.println("at the end");
			System.exit(1);
		}


		return logLikelihood;
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

	protected SparseDirichlet createDirichletSampler() {
		SparseDirichletSamplerBuilder db = instantiateSparseDirichletSamplerBuilder(config.getDirichletSamplerBuilderClass(LDAConfiguration.SPARSE_DIRICHLET_SAMPLER_BULDER_DEFAULT));
		return db.build(this);
	}

	@SuppressWarnings("unchecked")
	protected SparseDirichletSamplerBuilder instantiateSparseDirichletSamplerBuilder(String samplerBuilderClassName) {
		@SuppressWarnings("rawtypes")
		Class modelClass = null;
		try {
			modelClass = Class.forName(samplerBuilderClassName);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e);
		}

		@SuppressWarnings("rawtypes")
		Class[] argumentTypes = new Class[0];

		try {
			return (SparseDirichletSamplerBuilder) modelClass.getDeclaredConstructor(argumentTypes).newInstance();
		} catch (InstantiationException | IllegalAccessException
				| InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e);
		}
	}

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

	// 
	// Methods for displaying and saving results
	//

	public String topWords (int numWords) {

		StringBuilder output = new StringBuilder();

		IDSorter[] sortedWords = new IDSorter[numTypes];

		for (int topic = 0; topic < numTopics; topic++) {
			for (int type = 0; type < numTypes; type++) {
				sortedWords[type] = new IDSorter(type, typeTopicCounts[type][topic]);
			}

			Arrays.sort(sortedWords);

			output.append(topic + "\t" + tokensPerTopic[topic] + "\t");
			for (int i=0; i < numWords; i++) {
				output.append(alphabet.lookupObject(sortedWords[i].getID()) + " ");
			}
			output.append("\n");
		}

		return output.toString();
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

	public void setNumTopics(int newNumTopics) {
		numTopics = newNumTopics;
	}
	
	@Override
	public int getCurrentIteration() {
		return currentIteration;
	}

	@Override
	public InstanceList getDataset() {
		return trainingData;
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

	@Override
	public double[] getLogLikelihood() {
		return loglikelihood.stream().mapToDouble(d -> d).toArray();
	}

	@Override
	public double[] getHeldOutLogLikelihood() {
		return heldOutLoglikelihood.stream().mapToDouble(d -> d).toArray();
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
			thetaEstimate[k] = (localTopicCounts[k] + alpha[k]) / normalizer;
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
			thetaEstimate[k] = (localTopicCounts[k] + alpha) / normalizer;
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

	public void optimizeAlpha() {
		// First clear the sufficient statistic histograms

		for (int topic = 0; topic < topicDocCounts.length; topic++) {
			Arrays.fill(topicDocCounts[topic], 0);
		}

		for (int topic=0; topic < numTopics; topic++) {
			if (! usingSymmetricAlpha) {
				for (int count=0; count < documentTopicHistogram[topic].length; count++) {
					if (documentTopicHistogram[topic][count].get() > 0) {
						topicDocCounts[topic][count] += documentTopicHistogram[topic][count].get();
						documentTopicHistogram[topic][count].set(0);
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
				for (int count=0; count < documentTopicHistogram[topic].length; count++) {
					if (documentTopicHistogram[topic][count].get() > 0) {
						topicDocCounts[0][count] += documentTopicHistogram[topic][count].get();
						//			 ^ the only change
						documentTopicHistogram[topic][count].set(0);
					}
				}
			}
		}

		if (usingSymmetricAlpha) {
			alphaSum = Dirichlet.learnSymmetricConcentration(topicDocCounts[0],
					docLengthCounts,
					numTopics,
					alphaSum);
			for (int topic = 0; topic < numTopics; topic++) {
				alpha[topic] = alphaSum / numTopics;
			}
		}
		else {
			alphaSum = Dirichlet.learnParameters(alpha, topicDocCounts, docLengthCounts, 1.001, 1.0, 1);
		}
		
		logger.fine("[alpha: " + Arrays.toString(alpha) + "] ");	
	}

	public void optimizeBeta() {
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


		logger.fine("[beta: " + formatter.format(beta) + "] ");		
	}

	@Override
	public LDAConfiguration getConfiguration() {
		return config;
	}

	@Override
	public int getNoTypes() {
		return numTypes;
	}

	@Override
	public void addTestInstances(InstanceList testSet) {
		if(!testSet.getAlphabet().equals(alphabet)) {
			throw new IllegalStateException("Alphabets on training and test sets do not match!");
		}
		this.testSet = testSet;
	}

	@Override
	public double getBeta() {
		return beta;
	}

	@Override
	public double[] getAlpha() {
		double [] alphaVect = new double[numTopics];
		for (int i = 0; i < alphaVect.length; i++) {
			alphaVect[i] = alpha[i];
		}
		return alphaVect;
	}

	public void addInstances (InstanceList training) {
		trainingData = training;
		alphabet = training.getDataAlphabet();
		numTypes = alphabet.size();

		betaSum = beta * numTypes;

		typeTopicCounts = new int[numTypes][numTopics];

		for (Instance instance : training) {

			FeatureSequence tokens = (FeatureSequence) instance.getData();
			LabelSequence topicSequence =
					new LabelSequence(topicAlphabet, new int[ tokens.size() ]);

			int[] topics = topicSequence.getFeatures();
			for (int position = 0; position < tokens.size(); position++) {

				int topic = random.nextInt(numTopics);
				topics[position] = topic;
				tokensPerTopic[topic]++;

				int type = tokens.getIndexAtPosition(position);
				typeTopicCounts[type][topic]++;
			}

			TopicAssignment t = new TopicAssignment (instance, topicSequence);
			data.add (t);
		}

	}

	public Alphabet getAlphabet() { return alphabet; }
	public LabelAlphabet getTopicAlphabet() { return topicAlphabet; }
	public int getNumTopics() { return numTopics; }
	public ArrayList<TopicAssignment> getData() { return data; }
	public int[][] getTypeTopicCounts() { return typeTopicCounts; }
	public int[] getTopicTotals() { return tokensPerTopic; }


}
