package cc.mallet.topics;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.math3.distribution.BetaDistribution;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.types.BinomialSampler;
import cc.mallet.types.Dirichlet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelSequence;
import cc.mallet.types.ParallelDirichlet;
import cc.mallet.types.SparseDirichlet;
import cc.mallet.types.SparseDirichletSamplerBuilder;
import cc.mallet.types.VariableSelectionResult;
import cc.mallet.util.IndexSorter;
import cc.mallet.util.LoggingUtils;
import cc.mallet.util.OptimizedGentleAliasMethod;
import cc.mallet.util.WalkerAliasTable;

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
public class PoissonPolyaUrnHDPLDAInfiniteTopics extends UncollapsedParallelLDA implements HDPSamplerWithPhi {

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
	
	// The prior Gamma distribution
	//GammaDist gd;
//	AtomicInteger countBernBin = new AtomicInteger();
//	AtomicInteger countBernSumBin = new AtomicInteger();
//	AtomicInteger countExactBin = new AtomicInteger();
//	AtomicInteger countAliasBin = new AtomicInteger();
//	AtomicInteger countNormalBin = new AtomicInteger();
//	AtomicInteger revCumSum = new AtomicInteger();

	WalkerAliasTable [] aliasTables; 
	double [] typeNorm; // Array with doubles with sum of alpha * phi
	private ExecutorService tableBuilderExecutor;

	// #### Sparsity handling
	// Jagged array containing the topics that are non-zero for each type
	int [][] nonZeroTypeTopicIdxs = null;
	
	// How many indices  are zero for each type, i.e the column count for the zeroTypeTopicIdxs array
	int [] nonZeroTypeTopicColIdxs = null;

	boolean staticPhiAliasTableIsBuild = false;

	private ParallelDirichlet phiDirichletPrior;

	private double k_percentile;

	public PoissonPolyaUrnHDPLDAInfiniteTopics(LDAConfiguration config) {
		super(config);
		
		gamma = config.getHDPGamma(LDAConfiguration.HDP_GAMMA_DEFAULT);
		k_percentile = config.getHDPKPercentile(LDAConfiguration.HDP_K_PERCENTILE);
		nrStartTopics = config.getHDPNrStartTopics(LDAConfiguration.HDP_START_TOPICS_DEFAULT);
		
		System.out.println("HDP gamma: " + gamma);
		
		alphaCoef = config.getAlpha(LDAConfiguration.ALPHA_DEFAULT); 
		poissonNormalApproxThreshold = config.getAliasPoissonThreshold(LDAConfiguration.ALIAS_POISSON_DEFAULT_THRESHOLD); 
				
		// We should NOT do hyperparameter optimization of alpha or beta in the HDP
		hyperparameterOptimizationInterval = -1;
		
		topicOcurrenceCount = new int[numTopics];
				
		// Here we set Gamma, the prior base measure
		//gd = new UniformGamma();
		//gd = new GeometricGamma(1.0 / (1+gamma));
		//GammaDist gd = new GeometricGamma(1.0 / (1+gamma));
		//GammaDist gd = new GeometricGamma(0.05);
		
		//psiSampler = new PoissonBasedPsiSampler();
		psiSampler = new GEMBasedPsiSampler(gamma);
	}
		
	@Override
	public void addInstances(InstanceList training) {
		alphabet = training.getDataAlphabet();
		numTypes = alphabet.size();
		nonZeroTypeTopicIdxs = new int[numTypes][numTopics];
		nonZeroTypeTopicColIdxs = new int[numTypes];

		aliasTables = new WalkerAliasTable[numTypes];
		typeNorm    = new double[numTypes];
		phiDirichletPrior = new ParallelDirichlet(numTypes, beta);

		super.addInstances(training);
		
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

	/* When we initialize phi only for the nrStartTopics
	 * @param topicIndices a vector of length numTopics
	 */
	@Override
	public void initialSamplePhi(int [] topicIndices, double[][] phiMatrix) {
		int [] hdpStartTopicIndices = new int[nrStartTopics];
		for (int i = 0; i < nrStartTopics; i++) {
			hdpStartTopicIndices[i] = i;
		}
		super.initialSamplePhi(hdpStartTopicIndices, phi);
	}

	@Override
	public void preSample() {
		super.preSample();
		int poolSize = 2;
		tableBuilderExecutor = Executors.newFixedThreadPool(Math.max(1, poolSize));
	}

	protected SparseDirichlet createDirichletSampler() {
		SparseDirichletSamplerBuilder db = instantiateSparseDirichletSamplerBuilder(config.getDirichletSamplerBuilderClass("cc.mallet.types.PolyaUrnFixedCoeffPoissonDirichletSamplerBuilder"));
		return db.build(this);
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

	@Override
	public void preIteration() {
		doPreIterationTableBuilding();
		super.preIteration();
	}

	@Override
	public void postPhi() {
		super.postPhi();
						
		// This is a global parameter so normalization need to be done after psi_k has been sampled in parallel.
		psiSampler.finalizeSampling();
		
		// Reset frequency table between iterations
		docTopicTokenFreqTable.reset();
	}
	
	// TODO: Could be removed if inherits from PolyaUrn LDA
	protected void doPreIterationTableBuilding() {
		//LDAUtils.transpose(phi, phitrans);

		List<ParallelTableBuilder> builders = new ArrayList<>();
		final int [][] topicTypeIndices = topicIndexBuilder.getTopicTypeIndices();
		if(topicTypeIndices!=null) {
			// The topicIndexBuilder supports having different types per topic,
			// this is currently not used, so we can just pick the first topic
			// since it will be the same for all topics
			int [] typesToSample = topicTypeIndices[0];
			for (int typeIdx = 0; typeIdx < typesToSample.length; typeIdx++) {
				builders.add(new ParallelTableBuilder(typesToSample[typeIdx]));
			}
			// if the topicIndexBuilder returns null it means sample ALL types
		} else {
			for (int type = 0; type < numTypes; type++) {
				builders.add(new ParallelTableBuilder(type));
			}			
		}

		List<Future<WalkerAliasTableBuildResult>> results;
		try {
			results = tableBuilderExecutor.invokeAll(builders);
			for (Future<WalkerAliasTableBuildResult> result : results) {
				aliasTables[result.get().type] = result.get().table;
				typeNorm[result.get().type] = result.get().typeNorm; // typeNorm is sigma_prior
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (ExecutionException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public void preIterationGivenPhi() {
		if(!staticPhiAliasTableIsBuild) {
			doPreIterationTableBuilding();
			super.preIterationGivenPhi();
			staticPhiAliasTableIsBuild = true;
		}
	}

	// TODO: Inherits from PolyaUrnLDA
	@Override
	public void prePhi() {
		super.prePhi();
		Arrays.fill(nonZeroTypeTopicColIdxs,0);
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
		double gamma;
		
		public GEMBasedPsiSampler(double gamma) {
			this.gamma = gamma;
			// TODO: numTopics - 1
			l = new int[numTopics];
			
			psi = new double[numTopics];
			for (int i = 0; i < nrStartTopics; i++) {
				psi[i] = 1.0 / nrStartTopics;
			}
		}
		
		@Override
		public void reset() {
			l = new int[numTopics];
		}
		
		/* (non-Javadoc)
		 * @see cc.mallet.topics.PoissonPolyaUrnHDPLDA.PsiSampler#updateTopic(int, double)
		 */
		@Override
		public void updateTopic(int topic, int l_k) {
			// \Psi ~ GD(1,1,..,1, \gamma, \gamma, ..,\gamma)		
			// alphaPrime_j = alpha_j (always 1) + y_j (i.e. l_k)
			l[topic] = l_k;
		}

		@Override
		public void finalizeSampling() {
			// n = sum over l_k
			
//			System.out.println("Alpha: " + Arrays.toString(alpha));
			// \Psi ~ GD(1,1,..,1, \gamma, \gamma, ..,\gamma)
			// betaPrime = b_j (always gamma gamma) + sum_{j+1}{k+1} y_i (i.e l_k)
			// This must be wrong on the Wikipedia page since "k+1" does not make sense...
			double [] betaPrime = new double[numTopics];
			for (int topic = 0; topic < numTopics; topic++) {
				betaPrime[topic] = gamma;
				for (int topic_l = (topic+1); topic_l < numTopics; topic_l++) {
					betaPrime[topic] += l[topic_l];
				}
			}
//			System.out.println("Beta: " + Arrays.toString(beta));
			// Draw \nu_i ~ Beta(c_i,d_i) for all i...
			double [] nu = new double[numTopics];
			for (int topic = 0; topic < (numTopics-1); topic++) {
				// TODO: Make sure that creating a new BetaDist will still give a proper Beta dist
				BetaDistribution betaDist = new BetaDistribution(l[topic] + 1, betaPrime[topic]);
				nu[topic] = betaDist.sample();
			}
			// ...except the final one, which is set to 1
			nu[numTopics-1] = 1;
						
			// We don't need to reset psi, since we init it to nu[i]
//			psi = new double[numTopics];
			// Finish psi sampling
			double psiSum = 0;
			for (int topic = 0; topic < (numTopics-1); topic++) {
				psi[topic] = nu[topic];
				// This loop should be able to be optimized away by caching 
				// already calculated nu[i] (1-nu[i-1]) (1-nu[i-2])... results
				// If topic == 0, the psi[topic] should be nu[topic] 
				for (int j = topic-1; j >= 0; j--) {
					psi[topic] *= (1-nu[j]);
				}
				psiSum += psi[topic];
			} 
			// TODO: test case psi sum == 1, always
			
			for (int i = 0; i < numTopics; i++) {
				if(psi[i] < 0) {
					System.out.println("Nu: " + Arrays.toString(nu));			
					System.out.println("Psi sum: " + psiSum);
					System.out.println("Psi: " + Arrays.toString(psi));
				}
			}

//			psiSum = 0;
//			for (int i = 0; i < numTopics; i++) {
//				psiSum += psi[i];
//			}			
//			System.out.println(psiSum + " => Psi: " + Arrays.toString(psi));
		}
		
		@Override
		public double [] getPsi() {
			return psi;
		}

		@Override
		public void incrementTopic(int topic) {

		}
	}

	
	interface GammaDist {
		int [] drawNewTopics(int nrSamples, int range);
	}
	
	class UniformGamma implements GammaDist {
		/**
		 * Draw new topic numbers from \Gamma
		 * 
		 * @param nrAddedTopics
		 * @param numTopics
		 * @return
		 */
		@Override
		public int[] drawNewTopics(int nrSamples, int range) {
			int [] newTopics = new int[nrSamples];
			for (int i = 0; i < newTopics.length; i++) {
				// nextInt draws a uniform from (0, range-1)
				newTopics[i] = ThreadLocalRandom.current().nextInt(range); 
			}

			return newTopics;
		}		
	}

	@Override
	public void postZ() {
		super.postZ();
				
		// activeInData are topics that were non-zero in the last iteration.
		int[] emptyTopics = docTopicTokenFreqTable.getEmptyTopics();
		int activeInData = numTopics - emptyTopics.length;
		activeTopicInDataHistory.add(activeInData); // This is only used for post statistics not for inference
		int calcK = calcK(k_percentile);
		activeTopicHistory.add(calcK); // This is only used for post statistics not for inference
				
								
		if (showTopicsInterval > 0 && currentIteration % showTopicsInterval == 0) {
			System.err.println("Active in data: " + activeInData 
					+ "\tK = " + calcK 
					+ "\tEmpty topics (" + emptyTopics.length + "): " + Arrays.toString(emptyTopics));
		}

		psiSampler.reset();
	}
	
	private int calcK(double percentile) {
		int [] sortedAllocation = Arrays.copyOf(tokensPerTopic, tokensPerTopic.length);
		Arrays.sort(sortedAllocation);
		int [] ecdf = calcEcdf(sortedAllocation);  
		int k95 = findPercentile(ecdf,percentile);
		return k95;
	}

	private int findPercentile(int[] ecdf, double percentile) {
		double total = ecdf[ecdf.length-1];
		for (int j = 0; j < ecdf.length; j++) {
			if(ecdf[j]/total > percentile) {
				return j;
			}
		}
		return ecdf.length;
	}

	private int[] calcEcdf(int[] sortedAllocation) {
		int [] ecdf = new int[sortedAllocation.length]; 
		ecdf[0] = sortedAllocation[sortedAllocation.length-1];
		for(int i = 1; i < sortedAllocation.length; i++) {
			ecdf[i] = sortedAllocation[sortedAllocation.length - i - 1] + ecdf[i-1]; 
		}
		
		return ecdf;
	}
	
	/* 
	 * Uses AD-LDA logLikelihood calculation
	 *  
	 * Here we override SimpleLDA's original likelihood calculation and use the
	 * AD-LDA logLikelihood calculation. 
	 * With this approach all models likelihoods are calculated the same way
	 */
	// TODO: Check if needed.
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
			topicLogGammas[ topic ] = Dirichlet.logGammaStirling( alpha[topic] );
		}

		for (int doc=0; doc < data.size(); doc++) {
			LabelSequence topicSequence =	(LabelSequence) data.get(doc).topicSequence;

			docTopics = topicSequence.getFeatures();
						
			for (int token=0; token < docTopics.length; token++) {
				topicCounts[ docTopics[token] ]++;
			}

			for (int topic=0; topic < numTopics; topic++) {
				if (topicCounts[topic] > 0) {
					logLikelihood += (Dirichlet.logGammaStirling(alpha[topic] + topicCounts[topic]) -
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
					System.err.println("NaN in log likelihood calculation: " + topicCounts[topic]);
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
	protected double [] sampleTopicAssignmentsParallel(LDADocSamplingContext ctx) {
		FeatureSequence tokens = ctx.getTokens();
		LabelSequence topics = ctx.getTopics();
		int myBatch = ctx.getMyBatch();

		int type, oldTopic, newTopic;

		final int docLength = tokens.getLength();
		if(docLength==0) return null;

		int [] tokenSequence = tokens.getFeatures();
		int [] oneDocTopics = topics.getFeatures();

		double[] localTopicCounts = new double[numTopics];

		// This vector contains the indices of the topics with non-zero entries.
		// It has to be numTopics long since the non-zero topics come and go...
		int [] nonZeroTopics = new int[numTopics];

		// So we can map back from a topic to where it is in nonZeroTopics vector
		int [] nonZeroTopicsBackMapping = new int[numTopics];

		// Populate topic counts
		int nonZeroTopicCnt = 0;
		for (int position = 0; position < docLength; position++) {
			int topicInd = oneDocTopics[position];
			localTopicCounts[topicInd]++;
			if(localTopicCounts[topicInd]==1) {
				nonZeroTopicCnt = insert(topicInd, nonZeroTopics, nonZeroTopicsBackMapping, nonZeroTopicCnt);
			}
		}

		//kdDensities[myBatch] += nonZeroTopicCnt;
		kdDensities.addAndGet(nonZeroTopicCnt);

		double sum; // sigma_likelihood
		double[] cumsum = new double[numTopics]; 
		int [] nonZeroTopicsAdjusted;
		int nonZeroTopicCntAdjusted;

		//	Iterate over the words in the document
		for (int position = 0; position < docLength; position++) {
			type = tokenSequence[position];
			oldTopic = oneDocTopics[position]; // z_position
			localTopicCounts[oldTopic]--;

			// Potentially update nonZeroTopics mapping
			if(localTopicCounts[oldTopic]==0) {
				nonZeroTopicCnt = remove(oldTopic, nonZeroTopics, nonZeroTopicsBackMapping, nonZeroTopicCnt);
			}

			if(localTopicCounts[oldTopic]<0) 
				throw new IllegalStateException("Counts cannot be negative! Count for topic:" 
						+ oldTopic + " is: " + localTopicCounts[oldTopic]);

			// Propagates the update to the topic-token assignments
			/**
			 * Used to subtract and add 1 to the local structure containing the number of times
			 * each token is assigned to a certain topic. Called before and after taking a sample
			 * topic assignment z
			 */
			decrement(myBatch, oldTopic, type);
			//System.out.println("(Batch=" + myBatch + ") Decremented: topic=" + oldTopic + " type=" + type + " => " + batchLocalTopicUpdates[myBatch][oldTopic][type]);

			int nonZeroTypeCnt = nonZeroTypeTopicColIdxs[type];

			/*nonZeroTopicCntAdjusted = intersection(zeroTypeTopicIdxs[type], nonZeroTypeCnt, 
						nonZeroTopics, nonZeroTopicsBackMapping, nonZeroTopicsAdjusted, nonZeroTopicCnt);	

				String logstr = "Type NZ    : " + intVectorToString(zeroTypeTopicIdxs[type], fillCnt) 
						+ "\nDoc NZ     : " + intVectorToString(nonZeroTopics, nonZeroTopicCnt) 
						+ "\nAdjusted NZ: " + intVectorToString(nonZeroTopicsAdjusted, nonZeroTopicCntAdjusted);
				System.out.println(logstr);

				System.out.println("Type: " + fillCnt + " Topic: " + nonZeroTopicCnt + " Adjusted: " + nonZeroTopicCntAdjusted);
				if(nonZeroTopicCntAdjusted < Math.min(fillCnt, nonZeroTopicCnt)) {
					System.out.println("################### YAY!");
				}*/

			if(nonZeroTypeCnt < nonZeroTopicCnt) {
				// INTERSECTION SHOULD IMPROVE perf since we use result both in cumsum and sample topic
				// Intersection needs to b O(k) for it to improve perf, but unless we add more memory 
				// requirements it becomes O(k log(k))
				nonZeroTopicsAdjusted = nonZeroTypeTopicIdxs[type];
				nonZeroTopicCntAdjusted = nonZeroTypeCnt;
				//usedTypeSparsness.incrementAndGet();
			} else {
				nonZeroTopicsAdjusted = nonZeroTopics;
				nonZeroTopicCntAdjusted = nonZeroTopicCnt;
			}

			double u = ThreadLocalRandom.current().nextDouble();

			// Document and type sparsity removed all (but one?) topics, just use the prior contribution
			if(nonZeroTopicCntAdjusted==0) {
				newTopic = (int) Math.floor(u * this.numTopics); // uniform (0,1)
			} else {
				int topic = nonZeroTopicsAdjusted[0];
				double score = localTopicCounts[topic] * phi[topic][type];
				cumsum[0] = score;
				// Now calculate and add up the scores for each topic for this word
				// We build a cumsum indexed by topicIndex
				int topicIdx = 1;
				while ( topicIdx < nonZeroTopicCntAdjusted ) {
					topic = nonZeroTopicsAdjusted[topicIdx];
					score = localTopicCounts[topic] * phi[topic][type];
					cumsum[topicIdx] = score + cumsum[topicIdx-1];
					topicIdx++;
				}
				sum = cumsum[topicIdx-1]; // sigma_likelihood

				// Choose a random point between 0 and the sum of all topic scores
				// The thread local random performs better in concurrent situations 
				// than the standard random which is thread safe and incurs lock 
				// contention
				double u_sigma = u * (typeNorm[type] + sum);
				// u ~ U(0,1)  
				// u [0,1]
				// u_sigma = u * (typeNorm[type] + sum)
				// if u_sigma < typeNorm[type] -> prior
				// u * (typeNorm[type] + sum) < typeNorm[type] => u < typeNorm[type] / (typeNorm[type] + sum)
				// else -> likelihood
				// u_prior = u_sigma / typeNorm[type] -> u_prior (0,1)
				// u_likelihood = (u_sigma - typeNorm[type]) / sum  -> u_likelihood (0,1)

				newTopic = sampleNewTopic(type, nonZeroTopicsAdjusted, nonZeroTopicCntAdjusted, sum, cumsum, u, u_sigma);
			}

			// Make sure we actually sampled a valid topic
			if (newTopic < 0 || newTopic >= numTopics) {
				throw new IllegalStateException ("Poisson Polya Urn HDP: New valid topic not sampled (" + newTopic + ").");
			}

			// Put that new topic into the counts
			oneDocTopics[position] = newTopic;
			localTopicCounts[newTopic]++;

			// Potentially update nonZeroTopics mapping
			if(localTopicCounts[newTopic]==1) {
				nonZeroTopicCnt = insert(newTopic, nonZeroTopics, nonZeroTopicsBackMapping, nonZeroTopicCnt);
			}

			// Propagates the update to the topic-token assignments
			/**
			 * Used to subtract and add 1 to the local structure containing the number of times
			 * each token is assigned to a certain topic. Called before and after taking a sample
			 * topic assignment z
			 */
			increment(myBatch, newTopic, type);
			
		}

		// Update the document topic frequency count table
		for (int topic = 0; topic < nonZeroTopicCnt; topic++) {
			docTopicTokenFreqTable.increment(nonZeroTopics[topic],(int)localTopicCounts[nonZeroTopics[topic]]);
		}

		return localTopicCounts;
	}
	
	// TODO: Can be removed if inherited from other class.
	/*
	 * Sample a topic indicator
	 * 
	 * @param type Type of the current token to sample
	 * @param nonZeroTopics Indices of the topics with p(z=k|.) > 0
	 * @param nonZeroTopicCnt Number of indicies in nonZeroTopics
	 * @param sum The sum of Sum_{nonzero_topic} localTopicCounts[topic] * phiType[topic] (also cumsum[nonZeroTopicCnt-1])
	 * @param cumsum The cumulative sum over Sum_{nonzero_topic} localTopicCounts[topic] * phiType[topic]
	 * @param u Uniform value within (0,1)
	 * @param u_sigma Same uniform value within (0,(typeNorm[type] + sum))
	 * 
	 * @return 
	 * 
	 */
	int sampleNewTopic(int type, int[] nonZeroTopics, int nonZeroTopicCnt, double sum, double[] cumsum, double u,
			double u_sigma) {
		int newTopic;
		if(u < (typeNorm[type]/(typeNorm[type] + sum))) {
			newTopic = aliasTables[type].generateSample(u+((sum*u)/typeNorm[type])); // uniform (0,1)
			//System.out.println("Prior Sampled topic: " + newTopic);
		} else {
			//double u_lik = (u_sigma - typeNorm[type]) / sum; // Cumsum is not normalized so don't divide by sum 
			double u_lik = (u_sigma - typeNorm[type]);
			int slot = SpaliasUncollapsedParallelLDA.findIdx(cumsum,u_lik,nonZeroTopicCnt);
			newTopic = nonZeroTopics[slot];
		}
		return newTopic;
	}
	// TODO: Can be removed if inherited from other class.
	protected static int removeIfIn(int oldTopic, int[] nonZeroTopics, int[] nonZeroTopicsBackMapping, int nonZeroTopicCnt) {
		if (nonZeroTopicCnt<1) {
			return nonZeroTopicCnt;
		}
		// We have one less non-zero topic, move the last to its place, and decrease the non-zero count
		int nonZeroIdx = nonZeroTopicsBackMapping[oldTopic];
		if( nonZeroIdx == 0 &&  nonZeroTopics[nonZeroIdx] != oldTopic) {
			return nonZeroTopicCnt; 
		} else {
			nonZeroTopics[nonZeroIdx] = nonZeroTopics[--nonZeroTopicCnt];
			nonZeroTopicsBackMapping[nonZeroTopics[nonZeroIdx]] = nonZeroIdx;
			return nonZeroTopicCnt;
		}
	}

	// TODO: Can be removed if inherited from other class.
	protected static int remove(int oldTopic, int[] nonZeroTopics, int[] nonZeroTopicsBackMapping, int nonZeroTopicCnt) {
		if (nonZeroTopicCnt<1) {
			throw new IllegalArgumentException ("SpaliasUncollapsedParallelLDA: Cannot remove, count is less than 1 => " + nonZeroTopicCnt);
		}
		// We have one less non-zero topic, move the last to its place, and decrease the non-zero count
		int nonZeroIdx = nonZeroTopicsBackMapping[oldTopic];
		//nonZeroTopicsBackMapping[oldTopic] = NOT_IN_SET;
		nonZeroTopics[nonZeroIdx] = nonZeroTopics[--nonZeroTopicCnt];
		nonZeroTopicsBackMapping[nonZeroTopics[nonZeroIdx]] = nonZeroIdx;
		return nonZeroTopicCnt;
	}
	// TODO: Can be removed if inherited from other class.
	protected static int insert(int newTopic, int[] nonZeroTopics, int[] nonZeroTopicsBackMapping, int nonZeroTopicCnt) {
		//// We have a new non-zero topic put it in the last empty slot and increase the count
		nonZeroTopics[nonZeroTopicCnt] = newTopic;
		nonZeroTopicsBackMapping[newTopic] = nonZeroTopicCnt;
		return ++nonZeroTopicCnt;
	}
	// TODO: Can be removed if inherited from other class.
	protected static int removeSorted(int oldTopic, int[] nonZeroTopics, int[] nonZeroTopicsBackMapping, int nonZeroTopicCnt) {
		if (nonZeroTopicCnt<1) {
			throw new IllegalArgumentException ("PolyaUrnLDA: Cannot remove, count is less than 1");
		}
		//System.out.println("New empty topic. Cnt = " + nonZeroTopicCnt);	
		int nonZeroIdx = nonZeroTopicsBackMapping[oldTopic];
		nonZeroTopicCnt--;
		// Shift the ones above one step to the left
		for(int i=nonZeroIdx; i<nonZeroTopicCnt;i++) {
			// Move the last non-zero topic to this new empty slot 
			nonZeroTopics[i] = nonZeroTopics[i+1];
			// Do the corresponding for the back mapping
			nonZeroTopicsBackMapping[nonZeroTopics[i]] = i;
		}
		return nonZeroTopicCnt;
	}
	// TODO: Can be removed if inherited from other class.
	protected static int insertSorted(int newTopic, int[] nonZeroTopics, int[] nonZeroTopicsBackMapping, int nonZeroTopicCnt) {
		//// We have a new non-zero topic put it in the last empty slot
		int slot = 0;
		while(newTopic > nonZeroTopics[slot] && slot < nonZeroTopicCnt) slot++;

		for(int i=nonZeroTopicCnt; i>slot;i--) {
			// Move the last non-zero topic to this new empty slot 
			nonZeroTopics[i] = nonZeroTopics[i-1];
			// Do the corresponding for the back mapping
			nonZeroTopicsBackMapping[nonZeroTopics[i]] = i;
		}				
		nonZeroTopics[slot] = newTopic;
		nonZeroTopicsBackMapping[newTopic] = slot;
		nonZeroTopicCnt++;
		return nonZeroTopicCnt;
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
			//System.out.println("Sampling # topics: " + indices.length);
			//System.out.println("Sampling topic: " + topic);
			
			// FIXEDTODO: Check if tokens per topic == 0 the l_l for that topic => 0
			// Yep!
			if(tokensPerTopic[topic]>0) {
				// First part of Psi sampling. Normalization must be done 
				// in postIteration when all Psi_k has been sampled
				int l_k = sampleL(topic, longestDocLength, docTopicTokenFreqTable, alphaCoef, psiSampler.getPsi()[topic]);
				// System.out.println("l_" + topic + " = " + l_k + " Topic Occurence:" + topicOcurrenceCount[topic]);
				
				psiSampler.updateTopic(topic, l_k);
				int [] relevantTypeTopicCounts = topicTypeCountMapping[topic];
				VariableSelectionResult res = dirichletSampler.nextDistributionWithSparseness(relevantTypeTopicCounts);
				phiMatrix[topic] = res.getPhi();
			// If we have a newly sampled active topic, it won't have any type topic
			// counts (tokensPerTopic[topic]<1), so we draw from the prior
			} else {
				phiMatrix[topic] = phiDirichletPrior.nextDistribution();
			}
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
	static protected int sampleL(int topic, int maxDocLen, 
			DocTopicTokenFreqTable docTopicTokenFreqTable, double alpha, double psi_k) {
		if(psi_k<0) System.out.println("psi_k = " + psi_k);
		// freqHist is D(j, k = topic)
		int [] freqHist = docTopicTokenFreqTable.getReverseCumulativeSum(topic);
		
		// Sum over c_j_k
		int lSum = 0;
		// nrTopicIndicators is j in paper
		// nrDocsWithMoreTopicIndicators is D(j,k = topic) in paper	
		for(int nrTopicIndicators = 1; nrTopicIndicators <= maxDocLen; nrTopicIndicators++) {
			int nrDocsWithMoreTopicIndicators = 0;
			if( freqHist.length >= nrTopicIndicators ) {				
				nrDocsWithMoreTopicIndicators = freqHist[nrTopicIndicators-1];
			}
			//System.out.println("nrDocsWithMoreThanDoclengthTopicIndicators: " + nrDocsWithMoreThanDoclengthTopicIndicators  + " docLength: " + docLength);
			// As soon as we see zero, we know the rest will be 
			// zero and not contribute to the sum, so we can exit.
			if(nrDocsWithMoreTopicIndicators==0) break;
			int bsample = 0;
			// Only sample if trials != 0, otherwise sample = 0;
			//double p = gamma / (gamma + nrTopicIndicators - 1);
			double nom = (alpha  * psi_k);
			double denom = ((alpha  * psi_k) + nrTopicIndicators - 1);
			double p;
			// 0 / 0 should => 1
			if(nom==0.0 && denom == 0.0) {
				p = 1;
			} else {
				p = nom / denom;
			}
			// Smooth out rounding errors...
			if(p > 1) p = 1.0;				
			bsample = BinomialSampler.rbinom(nrDocsWithMoreTopicIndicators, p);
			//System.err.println("Binomial sample: Trials: " + trials + " probability: " + p + " => " + bsample);
			lSum += bsample;
		}
		return lSum;
	}	
		
	@Override
	public LDAConfiguration getConfiguration() {
		return config;
	}

	@Override
	public int getNoTypes() {
		return numTypes;
	}

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
