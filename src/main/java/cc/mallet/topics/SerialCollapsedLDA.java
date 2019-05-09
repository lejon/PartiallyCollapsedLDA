package cc.mallet.topics;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

import org.apache.commons.lang.NotImplementedException;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.topics.SimpleLDA;
import cc.mallet.topics.TopicAssignment;
import cc.mallet.types.Dirichlet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelSequence;
import cc.mallet.util.LDAUtils;
import cc.mallet.util.LoggingUtils;
import cc.mallet.util.MalletLogger;
import cc.mallet.util.Randoms;
import cc.mallet.util.Timing;

public class SerialCollapsedLDA extends SimpleLDA implements LDAGibbsSampler {

	private static final long serialVersionUID = 7533987649605469394L;
	private static Logger logger = MalletLogger.getLogger(SerialCollapsedLDA.class.getName());
	LDAConfiguration config;
	int currentIteration = 0 ;
	private int startSeed;
	boolean abort = false;

	// Used for inefficiency calculations
	int [][] topIndices = null;

	public SerialCollapsedLDA(LDAConfiguration config) {
		super(config.getNoTopics(LDAConfiguration.NO_TOPICS_DEFAULT),
				config.getAlpha(LDAConfiguration.ALPHA_DEFAULT)*config.getNoTopics(LDAConfiguration.NO_TOPICS_DEFAULT),
				config.getBeta(LDAConfiguration.BETA_DEFAULT),
				new Randoms(config.getSeed(LDAConfiguration.SEED_DEFAULT))
				);
		setConfiguration(config);
		printLogLikelihood = false;
		showTopicsInterval = config.getTopicInterval(LDAConfiguration.TOPIC_INTER_DEFAULT);
	}
	
	@Override
	public void setRandomSeed(int seed) {
		super.setRandomSeed(seed);
		startSeed = seed;
	}

	public int getStartSeed() {
		return startSeed;
	}

	public SerialCollapsedLDA(int numberOfTopics) {
		super(numberOfTopics);
	}

	public SerialCollapsedLDA(int numberOfTopics, double alpha, double beta) {
		super(numberOfTopics, alpha*numberOfTopics, beta);
	}

	public SerialCollapsedLDA(int numberOfTopics, double alpha, double beta,
			Randoms random) {
		super(numberOfTopics, alpha*numberOfTopics, beta, random);
	}

	@Override
	public void sample (int iterations) throws IOException {
		String loggingPath = config.getLoggingUtil().getLogDir().getAbsolutePath();
		double logLik = modelLogLikelihood();
		String tw = topWords (wordsPerTopic);
		LDAUtils.logLikelihoodToFile(logLik,0,tw,loggingPath,logger);
		
		int [] printFirstNDocs = config.getPrintNDocsInterval();
		int nDocs = config.getPrintNDocs();
		int [] printFirstNTopWords = config.getPrintNTopWordsInterval();
		int nWords = config.getPrintNTopWords();
		
		int [] defaultVal = {-1};
		int [] output_interval = config.getIntArrayProperty("diagnostic_interval",defaultVal);
		File binOutput = null;
		if(output_interval.length>1||printFirstNDocs.length>1||printFirstNTopWords.length>1) {
			binOutput = LoggingUtils.checkCreateAndCreateDir(config.getLoggingUtil().getLogDir().getAbsolutePath() + "/binaries");
		}

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

			long elapsedMillis = System.currentTimeMillis();
			logger.fine(iteration + "\t" + (elapsedMillis - iterationStart) + "ms\t");

			if(config!= null) { 
				config.getLoggingUtil().logTiming(new Timing(iterationStart,elapsedMillis,"CollapsedSample_Z"));
			}

			if(output_interval.length == 2 && iteration >= output_interval[0] && iteration <= output_interval[1]) {
				LDAUtils.writeBinaryIntMatrix(typeTopicCounts, iteration, numTypes, numTopics, binOutput.getAbsolutePath() + "/Serial_N");
				LDAUtils.writeBinaryIntMatrix(LDAUtils.getDocumentTopicCounts(data, numTopics), iteration, data.size(), numTopics, binOutput.getAbsolutePath() + "/Serial_M");
			}

			if (showTopicsInterval > 0 && iteration % showTopicsInterval == 0) {
				if(config!= null) { 
					logLik = modelLogLikelihood();
					tw = topWords (wordsPerTopic);
					loggingPath = config.getLoggingUtil().getLogDir().getAbsolutePath();
					LDAUtils.logLikelihoodToFile(logLik,iteration,tw,loggingPath,logger);
				}
			}
			if( printFirstNDocs.length > 1 && LDAUtils.inRangeInterval(iteration, printFirstNDocs)) {
				int [][] docTopicCounts = LDAUtils.getDocumentTopicCounts(data, numTopics, nDocs);
				double [][] theta = LDAUtils.drawDirichlets(docTopicCounts);
				LDAUtils.writeBinaryDoubleMatrix(theta, iteration, binOutput.getAbsolutePath() + "/Theta_DxK");				
			}
			if( printFirstNTopWords.length > 1 && LDAUtils.inRangeInterval(iteration, printFirstNTopWords)) {
				// Assign these once
				if(topIndices==null) {
					topIndices = LDAUtils.getTopWordIndices(nWords, numTypes, numTopics, typeTopicCounts, alphabet);
				}
				double [][] phi = LDAUtils.drawDirichlets(typeTopicCounts);
				LDAUtils.writeBinaryDoubleMatrixIndices(LDAUtils.transpose(phi), iteration, 
						binOutput.getAbsolutePath() + "/Phi_KxV", topIndices);
			}
		}
	}

	/**
	 * @return the config
	 */
	public LDAConfiguration getConfig() {
		return config;
	}

	@Override
	/**
	 * @param config the config to set
	 */
	public void setConfiguration(LDAConfiguration config) {
		this.config = config;
	}

	/* 
	 * Uses AD-LDA logLikelihood calculation
	 *  
	 * Here we override SimpleLDA's original likelihood calculation and use the
	 * AD-LDA logLikelihood calculation. It's unclear to me which is "correct"
	 * With this approach all models likelihoods are calculated the same way
	 */
	@Override
	public double modelLogLikelihood() {
		double logLikelihood = 0.0;
		//int nonZeroTopics;

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
			topicLogGammas[ topic ] = Dirichlet.logGammaStirling( alpha );
		}

		for (int doc=0; doc < data.size(); doc++) {
			LabelSequence topicSequence =	(LabelSequence) data.get(doc).topicSequence;

			docTopics = topicSequence.getFeatures();

			for (int token=0; token < docTopics.length; token++) {
				topicCounts[ docTopics[token] ]++;
			}

			for (int topic=0; topic < numTopics; topic++) {
				if (topicCounts[topic] > 0) {
					logLikelihood += (Dirichlet.logGammaStirling(alpha + topicCounts[topic]) -
							topicLogGammas[ topic ]);
				}
			}

			// subtract the (count + parameter) sum term
			logLikelihood -= Dirichlet.logGammaStirling(alphaSum + docTopics.length);

			Arrays.fill(topicCounts, 0);
		}

		// add the parameter sum term
		logLikelihood += data.size() * Dirichlet.logGammaStirling(alphaSum);

		// And the topics

		// Count the number of type-topic pairs that are not just (logGamma(beta) - logGamma(beta))
		int nonZeroTypeTopics = 0;

		for (int type=0; type < numTypes; type++) {
			// reuse this array as a pointer

			topicCounts = typeTopicCounts[type];

			for (int topic = 0; topic < numTopics; topic++) {
				if (topicCounts[topic] == 0) { continue; }

				nonZeroTypeTopics++;
				logLikelihood += Dirichlet.logGammaStirling(beta + topicCounts[topic]);

				if (Double.isNaN(logLikelihood)) {
					System.out.println("NaN in log likelihood calculation: " + topicCounts[topic]);
					System.exit(1);
				} 
				else if (Double.isInfinite(logLikelihood)) {
					logger.warning("infinite log likelihood");
					System.exit(1);
				}
			}
		}

		for (int topic=0; topic < numTopics; topic++) {
			logLikelihood -= 
					Dirichlet.logGammaStirling( (beta * numTypes) +
							tokensPerTopic[ topic ] );

			if (Double.isNaN(logLikelihood)) {
				logger.info("NaN after topic " + topic + " " + tokensPerTopic[ topic ]);
				return 0;
			}
			else if (Double.isInfinite(logLikelihood)) {
				logger.info("Infinite value after topic " + topic + " " + tokensPerTopic[ topic ]);
				return 0;
			}

		}

		// logGamma(|V|*beta) for every topic
		logLikelihood += 
				Dirichlet.logGammaStirling(beta * numTypes) * numTopics;

		// logGamma(beta) for all type/topic pairs with non-zero count
		logLikelihood -=
				Dirichlet.logGammaStirling(beta) * nonZeroTypeTopics;

		if (Double.isNaN(logLikelihood)) {
			logger.info("at the end");
		}
		else if (Double.isInfinite(logLikelihood)) {
			logger.info("Infinite value beta " + beta + " * " + numTypes);
			return 0;
		}

		return logLikelihood;
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
	
	
	/* 
	 * Set the topic indicators in the object
	 *  
	 * This is of interest to compare starting values. 
	 */
	@Override
	public void setZIndicators(int[][] zIndicators) {
		// Set full N matrix to 0
		for( int topic = 0; topic < numTopics; topic++) {
			for ( int type = 0; type < typeTopicCounts.length; type++ ) {
				typeTopicCounts[type][topic] = 0;
			}
			tokensPerTopic[topic] = 0;
		}
		for (int docCnt = 0; docCnt < data.size(); docCnt++) {
			data.get(docCnt).topicSequence = 
					new LabelSequence(topicAlphabet, zIndicators[docCnt]);
			FeatureSequence tokenSequence =
					(FeatureSequence) data.get(docCnt).instance.getData();
			int [] tokens = tokenSequence.getFeatures();
			for (int pos = 0; pos < zIndicators[docCnt].length; pos++) {
				int type = tokens[pos];
				int topic = zIndicators[docCnt][pos];
				typeTopicCounts[type][topic] += 1;
				tokensPerTopic[topic]++;
			}
		}
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
	public ArrayList<TopicAssignment> getData() {
		return data;
	}
	
	@Override
	public int[][] getDeltaStatistics() {
		throw new NotImplementedException("Delta statistics is not implemented yet! :(");	
	}
	
	@Override
	public int[] getTopTypeFrequencyIndices() {
		throw new NotImplementedException("Type Frequency Indices is not implemented yet! :(");
	}

	@Override
	public int[] getTypeFrequencies() {
		throw new NotImplementedException("Type Frequencies is not implemented yet! :(");
	}

	@Override
	public double[] getTypeMassCumSum() {
		throw new NotImplementedException("Type Frequency Cum Sum is not implemented yet! :(");
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
	
	public double [][] getZbar() {
		return ModifiedSimpleLDA.getZbar(data,numTopics);
	}

	public double [][] getThetaEstimate() {
		return ModifiedSimpleLDA.getThetaEstimate(data,numTopics,alpha);
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
		throw new NotImplementedException();
	}

	@Override
	public double getBeta() {
		return beta;
	}
	
	@Override
	public double[] getAlpha() {
		double [] alphaVect = new double[numTopics];
		for (int i = 0; i < alphaVect.length; i++) {
			alphaVect[i] = alpha / numTopics;
		}
		return alphaVect;
	}

	@Override
	public void abort() {
		abort = true;
	}

	@Override
	public boolean getAbort() {
		return abort;
	}

	@Override
	public InstanceList getDataset() {
		throw new NotImplementedException();
	}
	
	@Override
	public double[] getLogLikelihood() {
		return null;
	}

	@Override
	public double[] getHeldOutLogLikelihood() {
		return null;
	}
}
