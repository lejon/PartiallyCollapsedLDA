
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

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.distribution.PoissonDistribution;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelSequence;
import cc.mallet.types.SparseDirichlet;
import cc.mallet.types.SparseDirichletSamplerBuilder;
import cc.mallet.types.VariableSelectionResult;
import cc.mallet.util.LDAUtils;
import cc.mallet.util.LoggingUtils;
import cc.mallet.util.OptimizedGentleAliasMethod;
import cc.mallet.util.WalkerAliasTable;

/**
 * @author Leif Jonsson
 *
 */
public class PoissonPolyaUrnHLDA extends UncollapsedParallelLDA implements LDASamplerWithPhi{

	private static final long serialVersionUID = 1L;

	double gamma;
	double [] alphaG;
	int [][] document_freq_counts;
	boolean [] active_topics;
	double alphaCoef;
	DocTopicTokenFreqTable docTopicTokenFreqTable; 
	int nrStartTopics;
	int maxTopics;
	List<Integer> activeTopicHistory = new ArrayList<Integer>();
	
	protected double[][] phitrans;

	WalkerAliasTable [] aliasTables; 
	double [] typeNorm; // Array with doubles with sum of alpha * phi
	private ExecutorService tableBuilderExecutor;

	// #### Sparsity handling
	// Jagged array containing the topics that are non-zero for each type
	int [][] nonZeroTypeTopicIdxs = null;
	// How many indices  are zero for each type, i.e the column count for the zeroTypeTopicIdxs array
	int [] nonZeroTypeTopicColIdxs = null;

	boolean staticPhiAliasTableIsBuild = false;

	public PoissonPolyaUrnHLDA(LDAConfiguration config) {
		super(config);
		
		gamma = config.getHDPGamma(LDAConfiguration.HDP_GAMMA_DEFAULT);
		nrStartTopics = config.getHDPNrStartTopics(LDAConfiguration.HDP_START_TOPICS_DEFAULT);
		
		System.out.println("HDP Start topics: " + nrStartTopics);
		
		// In the HDP the number of topics we are initialized with is 
		// taken as the maxNumber of topics possible
		maxTopics = numTopics;
		alphaCoef = config.getAlpha(LDAConfiguration.ALPHA_DEFAULT);
		
		// Initialize G to give same effect as symmetic alpha
		alphaG = new double[numTopics];
		for (int i = 0; i < alpha.length; i++) {
			alphaG[i] = 1;
		}
		
		// We should NOT do hyperparameter optimization of alpha or beta in the HDP
		hyperparameterOptimizationInterval = -1;
		
		// Initialize the number of active topics to nrStartTopics
		active_topics = new boolean[numTopics];

		for (int i = 0; i < nrStartTopics; i++) {
			active_topics[i] = true;
		}
		docTopicTokenFreqTable = new DocTopicTokenFreqTable(numTopics);
	}
	
	@Override
	public void addInstances(InstanceList training) {
		alphabet = training.getDataAlphabet();
		numTypes = alphabet.size();
		nonZeroTypeTopicIdxs = new int[numTypes][numTopics];
		nonZeroTypeTopicColIdxs = new int[numTypes];

		aliasTables = new WalkerAliasTable[numTypes];
		typeNorm    = new double[numTypes];
		phitrans    = new double[numTypes][numTopics];

		super.addInstances(training);
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
			double [] phiType =  phitrans[type]; 
			for (int topic = 0; topic < numTopics; topic++) {
				// In the HDP the sampled G takes the place of the alpha vector in LDA but
				// it is still multiplied with the LDA alpha scalar
				typeMass += probs[topic] = phiType[topic] * alphaCoef * alphaG[topic];
				if(phiType[topic]!=0) {
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
		System.out.println("Freq table: \n" + docTopicTokenFreqTable);
		
		// Finish G sampling, i.e normalize G
		double sumG = 0.0;
		for (int i = 0; i < alphaG.length; i++) {			
			sumG += alphaG[i];
		}
		for (int i = 0; i < alphaG.length; i++) {			
			alphaG[i] /= sumG;
		}
		System.out.println("Alpha G: " + Arrays.toString(alphaG));
		
		// Reset frequency table
		docTopicTokenFreqTable = new DocTopicTokenFreqTable(numTopics);
	}
	
	protected void doPreIterationTableBuilding() {
		LDAUtils.transpose(phi, phitrans);

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
		super.postSample();
		tableBuilderExecutor.shutdown();
	}
	
	@Override
	public void postZ() {
		super.postZ();
		
		// Resample the number of topics to use 
		activeTopicHistory.add(numTopics);
		int activeInData = updateNrActiveTopics(docTopicTokenFreqTable.getEmptyTopics(), active_topics, numTopics);
		System.out.println("Active topics: " + Arrays.toString(active_topics));
		System.out.println("Nr Topics in data: " + activeInData);
		
		int newNumTopics = activeInData+ sampleNrTopics(gamma); 
		if(newNumTopics>maxTopics) 
			throw new IndexOutOfBoundsException("New sampled number of topics (" 
					+ newNumTopics 
					+ ") iexceeds maxTopics (" + maxTopics + ")");
		
		
		setNumTopics(newNumTopics);
		System.out.println("Nr active Topics: " + numTopics);
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
				double [] phiType =  phitrans[type]; 
				int topic = nonZeroTopicsAdjusted[0];
				double score = localTopicCounts[topic] * phiType[topic];
				cumsum[0] = score;
				// Now calculate and add up the scores for each topic for this word
				// We build a cumsum indexed by topicIndex
				int topicIdx = 1;
				while ( topicIdx < nonZeroTopicCntAdjusted ) {
					topic = nonZeroTopicsAdjusted[topicIdx];
					score = localTopicCounts[topic] * phiType[topic];
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
			if (newTopic < 0 || newTopic > numTopics) {
				throw new IllegalStateException ("SpaliasUncollapsedParallelLDA: New valid topic not sampled (" + newTopic + ").");
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
			
			// Update the document topic count table
			for (int topic = 0; topic < numTopics; topic++) {
				if(localTopicCounts[topic]!=0) {
					docTopicTokenFreqTable.increment(topic,(int)localTopicCounts[topic]);
				}
			}
		}

		return localTopicCounts;
	}

	double calcCumSum(int type, double[] localTopicCounts, int[] nonZeroTopics, int nonZeroTopicCnt, double[] cumsum) {
		double [] phiType =  phitrans[type]; 
		int topic = nonZeroTopics[0];
		double score = localTopicCounts[topic] * phiType[topic];
		cumsum[0] = score;
		// Now calculate and add up the scores for each topic for this word
		// We build a cumsum indexed by topicIndex
		int topicIdx = 1;
		while ( topicIdx < nonZeroTopicCnt ) {
			topic = nonZeroTopics[topicIdx];
			score = localTopicCounts[topic] * phiType[topic];
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
			if(!active_topics[topic]) {
				for (int i = 0; i < phiMatrix[topic].length; i++) {
					phiMatrix[topic] = new double[numTypes];
				}
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
			if(l_k == 0.0) {
				System.out.println("Freq table: \n" + docTopicTokenFreqTable);
				System.err.println("Zero sampled for topic " + topic);
				System.err.println("Rev hist: " + Arrays.toString(docTopicTokenFreqTable.getReverseCumulativeSum(topic)));
				System.err.println("Active topics: " + Arrays.toString(active_topics));
				System.err.println("Active indices: " + Arrays.toString(activeIndices));
				System.err.println("Empty topics: \n" + Arrays.toString(docTopicTokenFreqTable.getEmptyTopics()));
			}
			System.out.println("[" + topic + "] Sampled l_k: " + l_k);
			PoissonDistribution pois_gamma = new PoissonDistribution(l_k);
			int eta_k = pois_gamma.sample(); 
			alphaG[topic] = eta_k;
			
			int [] relevantTypeTopicCounts = topicTypeCountMapping[topic];
			VariableSelectionResult res = dirichletSampler.nextDistributionWithSparseness(relevantTypeTopicCounts);
			phiMatrix[topic] = res.getPhi();
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
		for(int j = 0; j < maxDocLen; j++) {
			int trials = 0;
			if( freqHist.length > j ) {				
				trials = freqHist[j];
			}
			double p = gamma / (gamma + j);
			BinomialDistribution c_j_k = new BinomialDistribution(trials, p);
			lSum += c_j_k.sample();
		}
		return lSum;
	}	
	
	protected int sampleNrTopics(double gamma) {
		PoissonDistribution pois_gamma = new PoissonDistribution(gamma);
		int sample = pois_gamma.sample();
		
		System.out.println("Sampled: " + sample + " additional topics...");
		
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

	protected int updateNrActiveTopics(int[] emptyTopics, boolean [] active_topics, int numTopics) {
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
			}
		}
		
		// Rest is inactive
		for (int i = numTopics; i < active_topics.length; i++) {
			active_topics[i] = false;			
		}

		return nrActiveTopics;
	}
}
