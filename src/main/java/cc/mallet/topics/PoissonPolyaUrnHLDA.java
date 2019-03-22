
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

import org.apache.commons.math3.distribution.PoissonDistribution;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.types.BinomialSampler;
import cc.mallet.types.Dirichlet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelSequence;
import cc.mallet.types.PolyaUrnDirichlet;
import cc.mallet.types.SparseDirichlet;
import cc.mallet.types.SparseDirichletSamplerBuilder;
import cc.mallet.types.VariableSelectionResult;
import cc.mallet.util.LoggingUtils;
import cc.mallet.util.OptimizedGentleAliasMethod;
import cc.mallet.util.WalkerAliasTable;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * This is a parallel implementation of the Poisson Polya Urn HDP
 * 
 * What this class adds on top of the PolyaUrn LDA is additional sampling
 * of the number of topics to use in each iteration. Since the number of
 * topics potentially change in each iteration we have to keep track of this.
 * It is not a problem when the number of topics increase, but when they
 * decrease we have to keep track of this and re-map topics with too high
 * topic indicator. This might be solvable in other ways more efficiently, 
 * by only setting the probability of those topics to zero in Phi this might
 * be implemented later.
 * 
 * The number of new topics are sampled just after the Z sampling in the 
 * postZ method. Here the typeTopic matrix is also updated.
 * 
 * In this particular version numTopics (possibly) change every iteration
 * and phi and the typeTopic matrix is changed to reflect numTopics 
 * 
 * @author Leif Jonsson
 *
 */
public class PoissonPolyaUrnHLDA extends UncollapsedParallelLDA implements HDPSamplerWithPhi {

	private static final long serialVersionUID = 1L;

	double gamma;
	double [] psi;
	boolean [] activeTopics;
	double alphaCoef;
	DocTopicTokenFreqTable docTopicTokenFreqTable; 
	int nrStartTopics;
	int maxTopics;
	List<Integer> activeTopicHistory = new ArrayList<Integer>();
	List<Integer> activeTopicInDataHistory = new ArrayList<Integer>();
	int [] topicOcurrenceCount;
	static final int BINOMIAL_TABLE_START_IDX = 50;
	static final int BINOMIAL_TABLE_SIZE = 50;
	static final int BINOMIAL_TABLE_MAXWIDTH = 200;
//	AtomicInteger countBernBin = new AtomicInteger();
//	AtomicInteger countBernSumBin = new AtomicInteger();
//	AtomicInteger countExactBin = new AtomicInteger();
//	AtomicInteger countAliasBin = new AtomicInteger();
//	AtomicInteger countNormalBin = new AtomicInteger();

	WalkerAliasTable [] aliasTables; 
	double [] typeNorm; // Array with doubles with sum of alpha * phi
	private ExecutorService tableBuilderExecutor;

	// #### Sparsity handling
	// Jagged array containing the topics that are non-zero for each type
	int [][] nonZeroTypeTopicIdxs = null;
	// How many indices  are zero for each type, i.e the column count for the zeroTypeTopicIdxs array
	int [] nonZeroTypeTopicColIdxs = null;

	boolean staticPhiAliasTableIsBuild = false;

	Int2IntArrayMap topicMappingTable;

	int poissonNormalApproxThreshold;

	public PoissonPolyaUrnHLDA(LDAConfiguration config) {
		super(config);
		
		gamma = config.getHDPGamma(LDAConfiguration.HDP_GAMMA_DEFAULT);
		nrStartTopics = config.getHDPNrStartTopics(LDAConfiguration.HDP_START_TOPICS_DEFAULT);
		
		System.out.println("HDP Start topics: " + nrStartTopics);
		
		// In the HDP the number of topics we are initialized with is 
		// taken as the maxNumber of topics possible
		maxTopics = numTopics;
		alphaCoef = config.getAlpha(LDAConfiguration.ALPHA_DEFAULT);
		poissonNormalApproxThreshold = config.getAliasPoissonThreshold(LDAConfiguration.ALIAS_POISSON_DEFAULT_THRESHOLD);
		
		// Initialize G to give same effect as symmetic alpha
		psi = new double[numTopics];
		for (int i = 0; i < alpha.length; i++) {
			psi[i] = 1;
		}
		
		// We should NOT do hyperparameter optimization of alpha or beta in the HDP
		hyperparameterOptimizationInterval = -1;
		
		// Initialize the number of active topics to nrStartTopics
		activeTopics = new boolean[numTopics];

		for (int i = 0; i < nrStartTopics; i++) {
			activeTopics[i] = true;
		}
	}
		
	@Override
	public void addInstances(InstanceList training) {
		alphabet = training.getDataAlphabet();
		numTypes = alphabet.size();
		nonZeroTypeTopicIdxs = new int[numTypes][numTopics];
		nonZeroTypeTopicColIdxs = new int[numTypes];
		topicOcurrenceCount = new int[numTopics];

		aliasTables = new WalkerAliasTable[numTypes];
		typeNorm    = new double[numTypes];

		super.addInstances(training);
		
		docTopicTokenFreqTable = new DocTopicTokenFreqTable(numTopics,longestDocLength);
	}

	
	/* When we initialize Z we have to limit the topic indicators
	 * to nrStartTopics
	 * 
	 * @see cc.mallet.topics.UncollapsedParallelLDA#initialDrawTopicIndicator()
	 */
	@Override
	int initialDrawTopicIndicator() {
		return random.nextInt(nrStartTopics);
	}

	/* When we initialize phi we have to limit the topic indicators
	 * to nrStartTopics
	 */
	@Override
	public void initialSamplePhi(int [] topicIndices, double[][] phiMatrix) {
		int [] hdpStartTopicIndices = new int[nrStartTopics];
		for (int i = 0; i < nrStartTopics; i++) {
			topicIndices[i] = i;
		}
		super.initialSamplePhi(hdpStartTopicIndices, phi);
	}

	@Override
	public void preSample() {
		super.preSample();
		int poolSize = 2;
		tableBuilderExecutor = Executors.newFixedThreadPool(Math.max(1, poolSize));
		// Now all structures should be initialized with numTopics
		// now set numTopics to the number of topics we want to start with
		setNumTopics(nrStartTopics);
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
				// In the HDP the sampled G takes the place of the alpha vector in LDA but
				// it is still multiplied with the LDA alpha scalar
				typeMass += probs[topic] = phi[topic][type] * alphaCoef * psi[topic];
				if(phi[topic][type]!=0) {
					int newSize = nonZeroTypeTopicColIdxs[type]++;
					nonZeroTypeTopicIdxs[type][newSize] = topic;
				}
			}

			// In HDP num topics keep changing, so so does probs, so
			// we have to completely re-build them... for now...
			//if(aliasTables[type]==null) {
				aliasTables[type] = new OptimizedGentleAliasMethod(probs,typeMass);
			//} else {
			//	aliasTables[type].reGenerateAliasTable(probs, typeMass);
			//}

			return new WalkerAliasTableBuildResult(type, aliasTables[type], typeMass);
		}   
	}

	@Override
	public void preIteration() {	
		doPreIterationTableBuilding();
		super.preIteration();
	}

	@Override
	public void postIteration() {
		//System.out.println("Freq table: \n" + docTopicTokenFreqTable);
		
		// Finish G sampling, i.e normalize G
		double sumG = 0.0;
		for (int i = 0; i < psi.length; i++) {			
			sumG += psi[i];
		}
		for (int i = 0; i < psi.length; i++) {			
			psi[i] /= sumG;
		}
		//System.out.println("Alpha G: " + Arrays.toString(alphaG));
		
		// Reset frequency table
		docTopicTokenFreqTable.reset();
//		System.out.println("Exact: " + countExactBin.get() + " Normal: " + countNormalBin.get() + " Table: " + countAliasBin.get() + " Bern: " + countBernBin.get() + " BernSum: " + countBernSumBin.get());
	}
	
	protected void doPreIterationTableBuilding() {
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

	@Override
	public void prePhi() {
		super.prePhi();
		Arrays.fill(nonZeroTypeTopicColIdxs,0);
	}

	@Override
	public void postSample() {
		setNumTopics(activeTopicHistory.get(activeTopicHistory.size()-1));
		super.postSample();
		tableBuilderExecutor.shutdown();
	}
	
	@Override
	public void postZ() {
		super.postZ();
		
		// Resample the number of topics to use 
		activeTopicHistory.add(numTopics);
		int activeInData = updateNrActiveTopics(docTopicTokenFreqTable.getEmptyTopics(), activeTopics, topicOcurrenceCount, numTopics);
		activeTopicInDataHistory.add(activeInData);
		//System.out.println("Active topics: " + Arrays.toString(activeTopics));
		//System.out.println("Nr Topics in data: " + activeInData);
		
		int newNumTopics = activeInData + sampleNrTopics(gamma);
		
		if(newNumTopics < 1 || newNumTopics>maxTopics) 
			throw new IndexOutOfBoundsException("New sampled number of topics (" 
					+ newNumTopics 
					+ ") less than 1 or exceeds maxTopics (" + maxTopics + ") exiting");
		
		// If the new number of topics is smaller than the previous we may need to re-map some
		// active topic indicators with values higher than the new numTopics down to lower values
		// This table is needed when we sample Z, to re-map topics
		if(newNumTopics<numTopics) {
			topicMappingTable = createTopicTranslationTable(numTopics, newNumTopics, activeInData, activeTopics);
			reArrangeTopics(topicMappingTable, activeTopics, docTopicTokenFreqTable);
		} else {
			topicMappingTable = null;
		}
		
		if (showTopicsInterval > 0 && currentIteration % showTopicsInterval == 0) {
			System.err.println("Topic stats: Nr Topics:" + numTopics + "\t New topics: " + newNumTopics + "\t Active in data: " + activeInData + "\t Topic diff: " + (newNumTopics - numTopics));
		}

		setNumTopics(newNumTopics);
	}
	
	/* 
	 * Uses AD-LDA logLikelihood calculation
	 *  
	 * Here we override SimpleLDA's original likelihood calculation and use the
	 * AD-LDA logLikelihood calculation. 
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
			topicLogGammas[ topic ] = Dirichlet.logGammaStirling( alpha[topic] );
		}

		for (int doc=0; doc < data.size(); doc++) {
			LabelSequence topicSequence =	(LabelSequence) data.get(doc).topicSequence;

			docTopics = topicSequence.getFeatures();
			
			// Since in the HDP the numTopics can change between iterations, here we may need 
			// to re-map topic indicators with too high values from the previous iteration
			// the same way that is done in the Z sampling
			for (int topicInd = 0; topicInd < docTopics.length; topicInd++) {
				int topic = docTopics[topicInd];
				if(topic>=numTopics) {
					docTopics[topicInd] = topicMappingTable.get(topic);
				}
			}
			
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

	
	/**
	 * Re-arranges the topics in the typeTopic matrix (and its transpose) according
	 * to the topicMappingTable
	 * 
	 * @param topicMappingTable
	 */
	protected void reArrangeTopics(Int2IntArrayMap topicMappingTable, boolean [] activeTopics, DocTopicTokenFreqTable docTopicTokenFreqTable) {
		IntSet keys = topicMappingTable.keySet();
		for (int oldTopicPos : keys) {
			int newTopicPos = topicMappingTable.get(oldTopicPos);
			moveTopic(oldTopicPos, newTopicPos, 0);
			
			// Update topic occurrence 
			topicOcurrenceCount[newTopicPos] = topicOcurrenceCount[oldTopicPos];
			topicOcurrenceCount[oldTopicPos] = 0;
			
			// Update active topics
			activeTopics[newTopicPos] = true;
			activeTopics[oldTopicPos] = false;
			
			// Update doc freq table
			docTopicTokenFreqTable.moveTopic(oldTopicPos,newTopicPos);
		}
	}

	/**
	 * Creates a table that maps the positions of old topics with topic indicators higher than
	 * numTopics down on the range [0,numTopics]
	 * 
	 * This table is used in the Z sampling during the first pass when we create the local 
	 * topic counts
	 * 
	 * @param numTopics
	 * @param newNumTopics
	 * @param activeTopics
	 * @return
	 */
	static protected Int2IntArrayMap createTopicTranslationTable(int numTopics, int newNumTopics, int activeInData, boolean[] activeTopics) {
		int diff = newNumTopics - activeInData;
		if(!(newNumTopics<numTopics)) throw new IndexOutOfBoundsException("New number of topics is not smaller than numTopics, no need to create mapping table.");
		int [] freeSpacesInRange = new int[newNumTopics];
		int nrFreeAssigned = 0;
		for (int i = 0; i < newNumTopics; i++) {
			if(!activeTopics[i]) {
				freeSpacesInRange[nrFreeAssigned++] = i;
			}
		}
		
		Int2IntArrayMap translationTable = new Int2IntArrayMap(diff);
		
		nrFreeAssigned = 0;
		for (int i = newNumTopics; i < activeTopics.length; i++) {
			if(activeTopics[i]) {
				translationTable.put(i, freeSpacesInRange[nrFreeAssigned++]);
			}
		}
		
		return translationTable;
	}

	@Override
	protected LDADocSamplingResult sampleTopicAssignmentsParallel(LDADocSamplingContext ctx) {
		FeatureSequence tokens = ctx.getTokens();
		LabelSequence topics = ctx.getTopics();
		int myBatch = ctx.getMyBatch();

		int type, oldTopic, newTopic;

		final int docLength = tokens.getLength();
		if(docLength==0) return null;

		int [] tokenSequence = tokens.getFeatures();
		int [] oneDocTopics = topics.getFeatures();

		int[] localTopicCounts = new int[numTopics];

		// This vector contains the indices of the topics with non-zero entries.
		// It has to be numTopics long since the non-zero topics come and go...
		int [] nonZeroTopics = new int[numTopics];

		// So we can map back from a topic to where it is in nonZeroTopics vector
		int [] nonZeroTopicsBackMapping = new int[numTopics];

		// Populate topic counts
		int nonZeroTopicCnt = 0;
		for (int position = 0; position < docLength; position++) {
			int topicInd = oneDocTopics[position];
			// If this is an old topicInd with topic >= numTopics we re-map it
			// to its current position
			if(topicInd>=numTopics) {
				topicInd = topicMappingTable.get(topicInd);
				oneDocTopics[position] = topicInd;
			}
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
				newTopic = (int) Math.floor(u * numTopics); // uniform (0,1)
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

		// Update the document topic count table
		for (int topic = 0; topic < numTopics; topic++) {
			if(localTopicCounts[topic]!=0) {
				docTopicTokenFreqTable.increment(topic,(int)localTopicCounts[topic]);
			}
		}

		return new LDADocSamplingResultSparseSimple(localTopicCounts,nonZeroTopicCnt,nonZeroTopics);
	}

	double calcCumSum(int type, double[] localTopicCounts, int[] nonZeroTopics, int nonZeroTopicCnt, double[] cumsum) { 
		int topic = nonZeroTopics[0];
		double score = localTopicCounts[topic] * phi[topic][type];
		cumsum[0] = score;
		// Now calculate and add up the scores for each topic for this word
		// We build a cumsum indexed by topicIndex
		int topicIdx = 1;
		while ( topicIdx < nonZeroTopicCnt ) {
			topic = nonZeroTopics[topicIdx];
			score = localTopicCounts[topic] * phi[topic][type];
			cumsum[topicIdx] = score + cumsum[topicIdx-1];
			topicIdx++;
		}
		return cumsum[topicIdx-1];
	}

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
			//numPrior++;
			newTopic = aliasTables[type].generateSample(u+((sum*u)/typeNorm[type])); // uniform (0,1)
			//System.out.println("Prior Sampled topic: " + newTopic);
		} else {
			//numLikelihood++;
			//double u_lik = (u_sigma - typeNorm[type]) / sum; // Cumsum is not normalized so don't divide by sum 
			double u_lik = (u_sigma - typeNorm[type]);
			int slot = SpaliasUncollapsedParallelLDA.findIdx(cumsum,u_lik,nonZeroTopicCnt);
			newTopic = nonZeroTopics[slot];
			// Make sure we actually sampled a valid topic
		}
		return newTopic;
	}

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

	protected static int insert(int newTopic, int[] nonZeroTopics, int[] nonZeroTopicsBackMapping, int nonZeroTopicCnt) {
		//// We have a new non-zero topic put it in the last empty slot and increase the count
		nonZeroTopics[nonZeroTopicCnt] = newTopic;
		nonZeroTopicsBackMapping[newTopic] = nonZeroTopicCnt;
		return ++nonZeroTopicCnt;
	}

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
	 * @param indices
	 * @param topicTypeIndices
	 * @param phiMatrix
	 */
	@Override
	public void loopOverTopics(int [] indices, int[][] topicTypeIndices, double[][] phiMatrix) {
		int [] activeIndices = new int[indices.length];
		int numActive = 0;
		for (int topic : indices) {
			// Set this topic to zero if it is inactive
			if(!activeTopics[topic]) {
				phiMatrix[topic] = new double[numTypes];
			} else {
				activeIndices[numActive++] = topic;
			}
		}
		
		long beforeSamplePhi = System.currentTimeMillis();		
		for (int topicIdx = 0; topicIdx < numActive; topicIdx++) {
			int topic = activeIndices[topicIdx];
			// First part of G sampling, rest (normalization) must be done 
			// in postIteration when all G_k has been sampled
			double l_k = sampleL(topic, gamma, longestDocLength, docTopicTokenFreqTable);
			System.out.println("l_" + topic + " = " + l_k);
			if(l_k == 0.0) {
				System.err.println("Freq table: \n" + docTopicTokenFreqTable);
				System.err.println("Zero sampled for topic " + topic);
				System.err.println("Rev hist: " + Arrays.toString(docTopicTokenFreqTable.getReverseCumulativeSum(topic)));
				System.err.println("Active topics: " + Arrays.toString(activeTopics));
				System.err.println("Active indices: " + Arrays.toString(activeIndices));
				System.err.println("Empty topics: \n" + Arrays.toString(docTopicTokenFreqTable.getEmptyTopics()));
			}
			int eta_k; 
			if(l_k>poissonNormalApproxThreshold) {
				eta_k = (int) PolyaUrnDirichlet.nextPoissonNormalApproximation(l_k);
			} else {				
				PoissonDistribution pois_gamma = new PoissonDistribution(l_k);
				eta_k = pois_gamma.sample();
			}
			
			psi[topic] = eta_k;
			
			int [] relevantTypeTopicCounts = topicTypeCountMapping[topic];
			VariableSelectionResult res = dirichletSampler.nextDistributionWithSparseness(relevantTypeTopicCounts);
			
			// If we have to remap this topic, use the mapping table
			if(topicMappingTable!=null && topicMappingTable.containsKey(topic)) {
				phiMatrix[topicMappingTable.get(topic)] = res.getPhi();
			} else {
				phiMatrix[topic] = res.getPhi();
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

	protected int sampleL(int topic, double gamma, int maxDocLen, DocTopicTokenFreqTable docTopicTokenFreqTable) {
		int [] freqHist = docTopicTokenFreqTable.getReverseCumulativeSum(topic);
		
		// Sum over c_j_k
		int lSum = 0;
		for(int docLength = 0; docLength < maxDocLen; docLength++) {
			int trials = 0;
			if( freqHist.length > docLength ) {				
				trials = freqHist[docLength];
			}
			// As soon as we see zero, we know the rest will be 
			// zero and not contribute to the sum, so we can exit.
			if(trials==0) break;
			int bsample = 0;
			// Only sample if trials != 0, otherwise sample = 0;
			if(trials != 0) {
				double p = gamma / (gamma + docLength);
				
				bsample = BinomialSampler.rbinom(trials, p);
			}
			//System.err.println("Binomial sample: Trials: " + trials + " probability: " + p + " => " + bsample);
			lSum += bsample;
		}
		return lSum;
	}	
	
	protected int sampleNrTopics(double gamma) {
		int sample = -1;
		if(gamma<poissonNormalApproxThreshold) {
			PoissonDistribution pois_gamma = new PoissonDistribution(gamma);
			sample = pois_gamma.sample();
		} else {
			long lsample = PolyaUrnDirichlet.nextPoissonNormalApproximation(gamma);
			if(lsample>Integer.MAX_VALUE) throw new IllegalArgumentException("Nr topics sampled is TOOO large");
			sample = (int) lsample;
		}
		//System.out.println("Sampled: " + sample + " additional topics...");
		return sample; 
	}
	
	@Override
	public LDAConfiguration getConfiguration() {
		return config;
	}

	@Override
	public int getNoTypes() {
		return numTypes;
	}

	protected int updateNrActiveTopics(int[] emptyTopics, boolean [] active_topics, int[] topicOcurrenceCount, int numTopics) {
		int nrActiveTopics = 0;
		
		int eIdx = 0;
		// Update up to numTopics
		for (int i = 0; i < numTopics; i++) {
			if(eIdx < emptyTopics.length && i==emptyTopics[eIdx]) {
				active_topics[i] = false;
				eIdx++;
			} else {
				nrActiveTopics++;
				active_topics[i] = true;
				topicOcurrenceCount[i]++;
			}
		}
		
		// Rest is inactive
		for (int i = numTopics; i < active_topics.length; i++) {
			active_topics[i] = false;			
		}

		return nrActiveTopics;
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
