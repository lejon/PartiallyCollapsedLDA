package cc.mallet.topics;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.types.BinomialSampler;
import cc.mallet.types.InstanceList;
import cc.mallet.types.ParallelDirichlet;
import cc.mallet.types.VariableSelectionResult;
import cc.mallet.util.IndexSorter;
import cc.mallet.util.LoggingUtils;
import cc.mallet.util.OptimizedGentleAliasMethod;
import cc.mallet.util.ParallelRandoms;

/**
 * This is a parallel implementation of the Poisson Polya Urn HDP LDA
 * 
 * What this class adds on top of the PolyaUrn LDA is additional sampling
 * of the number of topics to use in each iteration. 
 * 
 * This version has no notion of active topics which it can sample from, 
 * rather it can sample from all topics (up to k (Aka k_max)). So in this
 * implementation numTopics should be set high (say 500)
 * 
 * @author Leif Jonsson
 *
 */
public class PoissonPolyaUrnHDPLDAInfiniteTopics extends PolyaUrnSpaliasLDA implements HDPSamplerWithPhi {

	private static final long serialVersionUID = 1L;

	int poissonNormalApproxThreshold;
	PsiSampler psiSampler;
	double gamma;
	
	// activeTopics stores the k_95's for post analysis  
	List<Integer> activeTopics = new ArrayList<>();
	
	// This is \alpha in paper, i.e. the concentration parameter within documents.
	double alphaCoef; 
	
	DocTopicTokenFreqTable docTopicTokenFreqTable; 
	int nrStartTopics;
	
	// numTopics is the same as K_max in paper, the maximum number of topics.
	
	// activeTopicHistory, activeTopicInDataHistory,topicOcurrenceCount is only used for post analysis, not used in algorithm.
	List<Integer> activeTopicHistory = new ArrayList<Integer>(); 
	List<Integer> activeTopicInDataHistory = new ArrayList<Integer>();
	// topicOcurrenceCount stores how many times the topic has been active?
	int [] topicOcurrenceCount;
	
	ParallelDirichlet phiDirichletPrior;

	double k_percentile;

	boolean[] deceasedTopics;

	public PoissonPolyaUrnHDPLDAInfiniteTopics(LDAConfiguration config) {
		super(config);
		
		gamma = config.getHDPGamma(LDAConfiguration.HDP_GAMMA_DEFAULT);
		k_percentile = config.getHDPKPercentile(LDAConfiguration.HDP_K_PERCENTILE);
		nrStartTopics = config.getHDPNrStartTopics(LDAConfiguration.HDP_START_TOPICS_DEFAULT);
		
		System.out.println("HDP gamma: " + gamma);
		System.out.println("HDP start topics: " + nrStartTopics);
		
		alphaCoef = config.getAlpha(LDAConfiguration.ALPHA_DEFAULT); 
		poissonNormalApproxThreshold = config.getAliasPoissonThreshold(LDAConfiguration.ALIAS_POISSON_DEFAULT_THRESHOLD); 
				
		// We should NOT do hyperparameter optimization of alpha or beta in the HDP
		// Let it be controlled by config setting
		//hyperparameterOptimizationInterval = -1;
		
		topicOcurrenceCount = new int[numTopics];
		deceasedTopics = new boolean[numTopics];
				
		psiSampler = new GEMBasedPsiSampler(gamma);
	}
		
	@Override
	public void addInstances(InstanceList training) {
		super.addInstances(training);
		phiDirichletPrior = new ParallelDirichlet(numTypes, beta);
		docTopicTokenFreqTable = new DocTopicTokenFreqTable(numTopics,longestDocLength);
	}
	
	/* When we initialize Z we have to limit the topic indicators
	 * to nrStartTopics
	 * @see cc.mallet.topics.UncollapsedParallelLDA#initialDrawTopicIndicator()
	 */
	@Override
	int initialDrawTopicIndicator() {
		return random.nextInt(nrStartTopics);
	}

	class ParallelTableBuilder implements Callable<WalkerAliasTableBuildResult> {
		int type;
		public ParallelTableBuilder(int type) {
			this.type = type;
		}
		@Override
		public WalkerAliasTableBuildResult call() {
			double [] probs = new double[numTopics];
			double typeMass = 0; // Type prior mass
			for (int topic = 0; topic < numTopics; topic++) {
				// In the HDP the sampled psi takes the place of the alpha vector in LDA but
				// it is still multiplied with the LDA alpha scalar (alphaCoef)
				typeMass += probs[topic] = phi[topic][type] * alphaCoef * psiSampler.getPsi()[topic];
				if(phi[topic][type]!=0) {
					int newSize = nonZeroTypeTopicColIdxs[type]++;
					nonZeroTypeTopicIdxs[type][newSize] = topic;
				}
			}

			if(aliasTables[type]==null) {
				aliasTables[type] = new OptimizedGentleAliasMethod(probs,typeMass);
			} else {
				aliasTables[type].reGenerateAliasTable(probs, typeMass);
			}

			return new WalkerAliasTableBuildResult(type, aliasTables[type], typeMass);
		}   
	}
	
	/**
	 * This method can only be called from threads working
	 * on separate topics. It is not thread safe if several threads
	 * work on the same topic
	 * 
	 * This is overridden from Uncollapsed since we need to book keep
	 * when a topic (possibly temporarily) dies (i.e goes from having
	 * tokens assigned to it to not having any). In this case
	 * we sample Phi for that topic from the prior in loopOverTopics.
	 * 
	 * @param type
	 * @param topic
	 * @param count
	 */
	@Override
	protected void updateTypeTopicCount(int type, int topic, int count) {
		// Topic resurrected... yay! (not really)
		if(tokensPerTopic[topic]==0 && deceasedTopics[topic]) {
			// I.e if a topic has 0 tokens and it is changed it MUST increase
			// since it cannot be negative, and if it was then previously deceased
			// it is now resurrected
			deceasedTopics[topic] = false;
		}
		super.updateTypeTopicCount(type, topic, count);
		// Topic died... whimp...
		if(tokensPerTopic[topic]==0) {
			deceasedTopics[topic] = true;
		}
	}

	@Override
	public void postPhi() {
		super.postPhi();

		// This is a global parameter so normalization need to be done after psi_k has been sampled in parallel.
		psiSampler.finalizeSampling();
	}
	
	Callable<WalkerAliasTableBuildResult> getAliasTableBuilder(int type) {
		return new ParallelTableBuilder(type);
	}
	
	/**
	 * Re-arranges the topics in the typeTopic matrix based
	 * on tokensPerTopic
	 * 
	 * @param tokensPerTopic
	 */
	protected void reArrangeTopics(int [] tokensPerTopic) {
		int [] sortedIndices =  IndexSorter.getSortedIndices(tokensPerTopic);
				
		for (int i = 0; i < sortedIndices.length; i++) {
			if(i != sortedIndices[i]) {
				moveTopic(sortedIndices[i], i);					
				docTopicTokenFreqTable.moveTopic(sortedIndices[i],i);
				sortedIndices = IndexSorter.getSortedIndices(this.tokensPerTopic);
			}
		}
	}
	
	@Override
	public void postSample() {
		super.postSample();
		tableBuilderExecutor.shutdown();
		reArrangeTopics(tokensPerTopic);
	}
	
	interface PsiSampler {
		/**
		 * Updates one topic given its sufficient statistic
		 * @param topic
		 * @param sufficientStatistic
		 */
		void updateTopic(int topic, int sufficientStatistic);
		void reset();
		void finalizeSampling();
		double [] getPsi();
		void incrementTopic(int i);
	}
	
	class GEMBasedPsiSampler implements PsiSampler {
		double [] psi; // This is capital Psi in paper.
		int [] l;
		long lSum = -1;
		double gamma;
		
		public GEMBasedPsiSampler(double gamma) {
			this.gamma = gamma;
			l = new int[numTopics];
			
			psi = new double[numTopics];
			
			// Init psi by drawing from prior
			finalizeSampling();
//			for (int i = 0; i < nrStartTopics; i++) {
//				psi[i] = 1.0 / nrStartTopics;
//			}
		}
		
		@Override
		public void reset() {
			l = new int[numTopics];
			lSum = 0;
		}
		
		/* (non-Javadoc)
		 * @see cc.mallet.topics.PoissonPolyaUrnHDPLDA.PsiSampler#updateTopic(int, double)
		 */
		@Override
		public void updateTopic(int topic, int l_k) {
			// \Psi ~ GD(1,1,..,1, \gamma, \gamma, ..,\gamma)		
			// alphaPrime_j = alpha_j (always 1) + y_j (i.e. l_k)
			l[topic] = l_k;
			lSum += l_k;
		}

		@Override
		public void finalizeSampling() {

			// We want to sample from the Psi prior to init psi
			// at that point we have to allow that lSum == -1
			if(lSum > 0 && lSum < data.size()) {
				throw new ArrayIndexOutOfBoundsException("l ("+ lSum + ") is smaller than the number of documents in the corpus " 
						+ "("+ data.size() + ")");
			}
			
			// We want to sample from the Psi prior to init psi
			// at that point we have to allow that lSum == -1
			if(lSum > 0 && lSum > getCorpusSize()) {
				throw new ArrayIndexOutOfBoundsException("l ("+ lSum + ") is bigger than the number of tokens in the corpus " 
						+ "("+ getCorpusSize() + ")");
			}

			// n = sum over l_k
			
			// \Psi ~ GD(1,1,..,1, \gamma, \gamma, ..,\gamma)
			// betaPrime = b_j (always gamma gamma) + sum_{j+1}{k+1} y_i (i.e l_k)
			// This must be wrong on the Wikipedia page since "k+1" does not make sense...
			long [] betaPrime = new long[numTopics-1];
			for (int topic = 0; topic < (numTopics-1); topic++) {
				for (int topic_l = (topic+1); topic_l < numTopics; topic_l++) {
					betaPrime[topic] += l[topic_l];
				}
			}

			// Draw \nu_i ~ Beta(c_i,d_i) for all i...
			double [] nu = new double[numTopics];
			for (int topic = 0; topic < (numTopics-1); topic++) {
				nu[topic] = ParallelRandoms.rbeta(l[topic] + 1, gamma + (double) betaPrime[topic]);
			}
			// ...except the final one, which is set to 1
			nu[numTopics-1] = 1;
						
			// Finish psi sampling
			double psiSum = 0;
			double oneMinusPsiProd = 1;
			for (int topic = 0; topic < (numTopics-1); topic++) {
				psi[topic] = nu[topic] * oneMinusPsiProd;
				oneMinusPsiProd *= (1-nu[topic]); 
				psiSum += psi[topic];
			}
			
			//System.out.println("Psi=" + Arrays.toString(psi));
			
			// Take care of rounding errors
			if(psiSum>1.0) {
				psi[numTopics-1] = Double.MIN_VALUE;
			} else {				
				psi[numTopics-1] = 1-psiSum;			
			}
		}
		
		@Override
		public double [] getPsi() {
			return psi;
		}

		@Override
		public void incrementTopic(int topic) {

		}
	}

	@Override
	public void postZ() {
		super.postZ();
				
		// activeInData are topics that were non-zero in the last iteration.
		int[] emptyTopics = docTopicTokenFreqTable.getEmptyTopics();
		int activeInData = numTopics - emptyTopics.length;
		activeTopicInDataHistory.add(activeInData); // This is only used for post statistics not for inference
		int calcK = calcK(k_percentile, tokensPerTopic);
		activeTopicHistory.add(calcK); // This is only used for post statistics not for inference
				
								
		if (showTopicsInterval > 0 && currentIteration % showTopicsInterval == 0) {
			System.err.println("Active in data: " + activeInData 
					+ "\tK = " + calcK 
					+ "\tEmpty topics (" + emptyTopics.length + ")");
		}

		psiSampler.reset();
	}
	
	public static int calcK(double percentile, int [] tokensPerTopic) {
		int [] sortedAllocation = Arrays.copyOf(tokensPerTopic, tokensPerTopic.length);
		Arrays.sort(sortedAllocation);
		int [] ecdf = calcEcdf(sortedAllocation);  
		int k95 = findPercentile(ecdf,percentile);
		return k95;
	}

	public static int findPercentile(int[] ecdf, double percentile) {
		double total = ecdf[ecdf.length-1];
		for (int j = 0; j < ecdf.length; j++) {
			if(ecdf[j]/total > percentile) {
				return j;
			}
		}
		return ecdf.length;
	}

	public static int[] calcEcdf(int[] sortedAllocation) {
		int [] ecdf = new int[sortedAllocation.length]; 
		ecdf[0] = sortedAllocation[sortedAllocation.length-1];
		for(int i = 1; i < sortedAllocation.length; i++) {
			ecdf[i] = sortedAllocation[sortedAllocation.length - i - 1] + ecdf[i-1]; 
		}

		return ecdf;
	}
	
	@Override
	protected LDADocSamplingResult sampleTopicAssignmentsParallel(LDADocSamplingContext ctx) {
		LDADocSamplingResultSparse res = (LDADocSamplingResultSparse) super.sampleTopicAssignmentsParallel(ctx);

		int [] nonZeroTopics = res.getNonZeroIndices();
		int [] localTopicCounts = res.getLocalTopicCounts();
		// Update the document topic frequency count table
		for (int topic = 0; topic < res.getNonZeroTopicCounts(); topic++) {
			docTopicTokenFreqTable.increment(nonZeroTopics[topic],(int)localTopicCounts[nonZeroTopics[topic]]);
		}

		return res;
	}
	
	/**
	 * Samples new Phi's.
	 * 
	 * 
	 * @param indices the topics the thread should sample
	 * @param topicTypeIndices Choose which elements in Phi top sample (random scan Gibbs sampling)
	 * @param phiMatrix
	 */
	@Override
	public void loopOverTopics(int [] indices, int[][] topicTypeIndices, double[][] phiMatrix) {
		long beforeSamplePhi = System.currentTimeMillis();
		int topicsToSample = indices.length;
		for (int topicIdx = 0; topicIdx < topicsToSample; topicIdx++) {
			int topic = indices[topicIdx];

			VariableSelectionResult res;
			if(tokensPerTopic[topic]>0) {
				// First part of Psi sampling. Normalization must be done 
				// in postIteration when all Psi_k has been sampled
				int l_k = sampleL(topic, longestDocLength, docTopicTokenFreqTable, alphaCoef, psiSampler.getPsi()[topic]);
				psiSampler.updateTopic(topic, l_k);
				
				int [] relevantTypeTopicCounts = topicTypeCountMapping[topic];
				res = dirichletSampler.nextDistributionWithSparseness(relevantTypeTopicCounts);
				phiMatrix[topic] = res.getPhi();
			} else {
				res = dirichletSampler.nextDistributionWithSparseness(beta);
				phiMatrix[topic] = res.getPhi();
				//phiMatrix[topic] = dirichletSampler.nextDistribution();
			}
			
			// We won't use the table any more, so we reset the topic here
			// so we don't have to loop over all in serial in postPhi
			docTopicTokenFreqTable.reset(topic);
		}
		long elapsedMillis = System.currentTimeMillis();
		long threadId = Thread.currentThread().getId();

		if(measureTimings) {
			PrintWriter pw = LoggingUtils.checkCreateAndCreateLogPrinter(
					config.getLoggingUtil().getLogDir() + "/timing_data",
					"thr_" + threadId + "_Phi_sampling.txt");
			pw.println(beforeSamplePhi + "," + elapsedMillis);
			pw.flush();
			pw.close();
		}
	}

	// See. PoissonPolyaUrnHDPLDATest.testSampleLOneDocAnalytic and testSampleLSimR: 
	// SampleL for one document should have the antoniak distribution (see pdf in eq (9) in paper)
	// Very detailed code review (Måns and Leif) 2019-03-12, cross verified with unit tests 
	protected int sampleL(int topic, int maxDocLen, 
			DocTopicTokenFreqTable docTopicTokenFreqTable, 
			double alpha, double psi_k) {
		if(psi_k<0) throw new IllegalArgumentException("Non positive psi_k: " + psi_k);
		// freqHist is D(j, k = topic)
		int [] freqHist = docTopicTokenFreqTable.getReverseCumulativeSum(topic);
		
		// Sum over c_j_k
		
		// We skip the step where nrTopicIndicators==1, since we know that for this
		// case p==1 and rbinom(1,X,1) == X i.e freqHist[0] 
		// (i.e the number of documents in the corpus)
		int lSum = freqHist[0];
		// nrTopicIndicators is j in paper
		// nrDocsWithMoreTopicIndicators is D(j,k = topic) in paper	
		for(int nrTopicIndicators = 2; nrTopicIndicators <= maxDocLen; nrTopicIndicators++) {
			int nrDocsWithMoreTopicIndicators = 0;
			if( freqHist.length >= nrTopicIndicators ) {				
				nrDocsWithMoreTopicIndicators = freqHist[nrTopicIndicators-1];
			}

			// As soon as we see zero, i.e 
			// "how many documents have more than nrTopicIndicators allocated to topic == 0"
			// we know the rest will be zero also since it is a reverse cumulative sum, and 
			// thus not contribute to the sum, so we can exit.
			if(nrDocsWithMoreTopicIndicators==0) break;

			//double p = gamma / (gamma + nrTopicIndicators - 1);
			
			// Calculate on log scale to reduce risk of underflow
			//double nom = Math.exp(Math.log(alpha) + Math.log(psi_k));
			double nom = alpha * psi_k;
			double denom = (nom + nrTopicIndicators - 1);
			double p;
			// 0 / 0 should => 1
			if(nom==0.0 && denom == 0.0) {
				p = 1;
			} else {
				//p = Math.exp(Math.log(nom) - Math.log(denom));
				p = nom / denom;
			}

			// Handling roundoff errors
			if(p<= 0.0) {
				continue;
			}
			if(p >= 1) { 
				p = 1;
				lSum += nrDocsWithMoreTopicIndicators;
				continue;
			}

			lSum += BinomialSampler.rbinom(nrDocsWithMoreTopicIndicators, p);
		}
		if(lSum > tokensPerTopic[topic]) {
			throw new ArrayIndexOutOfBoundsException("l_" + topic 
					+ " ("+ lSum + ") is bigger than # tokens assigned to topic " 
					+ topic + "("+ tokensPerTopic[topic] + ") Iter: " + getCurrentIteration());
		}
		return lSum;
	}	
	
//	/**
//	 * Adapted for HDP LDA
//	 *
//	 */
//	@Override
//	public double modelLogLikelihood() {
//		double logLikelihood = 0.0;
//		//int nonZeroTopics;
//
//		// The likelihood of the model is a combination of a 
//		// Dirichlet-multinomial for the words in each topic
//		// and a Dirichlet-multinomial for the topics in each
//		// document.
//
//		// The likelihood function of a dirichlet multinomial is
//		//	 Gamma( sum_i alpha_i )	 prod_i Gamma( alpha_i + N_i )
//		//	prod_i Gamma( alpha_i )	  Gamma( sum_i (alpha_i + N_i) )
//
//		// So the log likelihood is 
//		//	logGamma ( sum_i alpha_i ) - logGamma ( sum_i (alpha_i + N_i) ) + 
//		//	 sum_i [ logGamma( alpha_i + N_i) - logGamma( alpha_i ) ]
//
//		// Do the documents first
//
//		int[] topicCounts = new int[numTopics];
//		double[] topicLogGammas = new double[numTopics];
//		int[] docTopics;
//
//		for (int topic=0; topic < numTopics; topic++) {
//			topicLogGammas[ topic ] = Dirichlet.logGammaStirling( alpha[topic] * psiSampler.getPsi()[topic] );
//		}
//
//		for (int doc=0; doc < data.size(); doc++) {
//			LabelSequence topicSequence =	(LabelSequence) data.get(doc).topicSequence;
//
//			docTopics = topicSequence.getFeatures();
//
//			for (int token=0; token < docTopics.length; token++) {
//				topicCounts[ docTopics[token] ]++;
//			}
//
//			for (int topic=0; topic < numTopics; topic++) {
//				if (topicCounts[topic] > 0) {
//					logLikelihood += (Dirichlet.logGammaStirling(alpha[topic] * psiSampler.getPsi()[topic] + topicCounts[topic]) -
//							topicLogGammas[ topic ]);
//				}
//			}
//
//			// subtract the (count + parameter) sum term
//			logLikelihood -= Dirichlet.logGammaStirling(alphaSum + docTopics.length);
//
//			Arrays.fill(topicCounts, 0);
//		}
//
//		// add the parameter sum term
//		logLikelihood += data.size() * Dirichlet.logGammaStirling(alphaSum);
//
//		// And the topics
//
//		// Count the number of type-topic pairs that are not just (logGamma(beta) - logGamma(beta))
//		int nonZeroTypeTopics = 0;
//
//		for (int type=0; type < numTypes; type++) {
//			// reuse this array as a pointer
//
//			topicCounts = typeTopicCounts[type];
//
//			for (int topic = 0; topic < numTopics; topic++) {
//				if (topicCounts[topic] == 0) { continue; }
//
//				nonZeroTypeTopics++;
//				logLikelihood += Dirichlet.logGammaStirling(beta + topicCounts[topic]);
//
//				if (Double.isNaN(logLikelihood)) {
//					System.err.println("NaN in log likelihood calculation: " + topicCounts[topic]);
//					System.exit(1);
//				} 
//				else if (Double.isInfinite(logLikelihood)) {
//					logger.warning("infinite log likelihood");
//					System.exit(1);
//				}
//			}
//		}
//
//		for (int topic=0; topic < numTopics; topic++) {
//			logLikelihood -= 
//					Dirichlet.logGammaStirling( (beta * numTypes) +
//							tokensPerTopic[ topic ] );
//
//			if (Double.isNaN(logLikelihood)) {
//				logger.info("NaN after topic " + topic + " " + tokensPerTopic[ topic ]);
//				return 0;
//			}
//			else if (Double.isInfinite(logLikelihood)) {
//				logger.info("Infinite value after topic " + topic + " " + tokensPerTopic[ topic ]);
//				return 0;
//			}
//
//		}
//
//		// logGamma(|V|*beta) for every topic
//		logLikelihood += 
//				Dirichlet.logGammaStirling(beta * numTypes) * numTopics;
//
//		// logGamma(beta) for all type/topic pairs with non-zero count
//		logLikelihood -=
//				Dirichlet.logGammaStirling(beta) * nonZeroTypeTopics;
//
//		if (Double.isNaN(logLikelihood)) {
//			logger.info("at the end");
//		}
//		else if (Double.isInfinite(logLikelihood)) {
//			logger.info("Infinite value beta " + beta + " * " + numTypes);
//			return 0;
//		}
//
//		return logLikelihood;
//	}
		
	public int[] getTopicOcurrenceCount() {
		return topicOcurrenceCount;
	}

	public void setTopicOcurrenceCount(int[] topicOcurrenceCount) {
		this.topicOcurrenceCount = topicOcurrenceCount;
	}

	public List<Integer> getActiveTopicHistory() {
		return activeTopicHistory;
	}

	public void setActiveTopicHistory(List<Integer> activeTopicHistory) {
		this.activeTopicHistory = activeTopicHistory;
	}

	public List<Integer> getActiveTopicInDataHistory() {
		return activeTopicInDataHistory;
	}

	public void setActiveTopicInDataHistory(List<Integer> activeInDataTopicHistory) {
		this.activeTopicInDataHistory = activeInDataTopicHistory;
	}
}
