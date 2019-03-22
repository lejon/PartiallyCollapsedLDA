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
import java.util.concurrent.atomic.AtomicInteger;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelSequence;
import cc.mallet.types.VSDirichlet;
import cc.mallet.types.VariableSelectionDirichlet;
import cc.mallet.types.VariableSelectionResult;
import cc.mallet.util.IntArraySortUtils;
import cc.mallet.util.LoggingUtils;
import cc.mallet.util.OptimizedGentleAliasMethod;
import cc.mallet.util.WalkerAliasTable;

public class NZVSSpaliasUncollapsedParallelLDA extends UncollapsedParallelLDA implements LDAGibbsSampler{
	private static final long serialVersionUID = 1L;
	//private static final int NOT_IN_SET = -1;
	WalkerAliasTable [] aliasTables; 
	double [] typeNorm; // Array with doubles with sum of alpha * phi
	private ExecutorService tableBuilderExecutor;
	
	// #### VSSelection
	// Jagged array containing the topics that are non-zero for each type
	int [][] nonZeroTypeTopicIdxs = null;
	// How many indices  are zero for each type, i.e the column count for the zeroTypeTopicIdxs array
	Object [] nonZeroTypeTopicIdxsColLocks = null;
	AtomicInteger [] nonZeroTypeTopicColIdxs = null;
	
	//AtomicInteger toPrior = new AtomicInteger();
	//AtomicInteger usedTypeSparsness = new AtomicInteger();
	private VariableSelectionDirichlet vsDirichlet;
	double vsPriorDefault = 0.5;
	

	public NZVSSpaliasUncollapsedParallelLDA(LDAConfiguration config) {
		super(config);
	}

	@Override
	public void addInstances(InstanceList training) {
		vsDirichlet = new VSDirichlet(beta, config.getVariableSelectionPrior(vsPriorDefault));
		alphabet = training.getDataAlphabet();
		numTypes = alphabet.size();
		nonZeroTypeTopicIdxs = new int[numTypes][numTopics];
		nonZeroTypeTopicIdxsColLocks = new Object[numTypes];
		nonZeroTypeTopicColIdxs = new AtomicInteger[numTypes];
		for (int i = 0; i < numTypes; i++) {
			nonZeroTypeTopicIdxsColLocks[i] = new Object();
			nonZeroTypeTopicColIdxs[i] = new AtomicInteger();
		}
		aliasTables = new WalkerAliasTable[numTypes];
		typeNorm    = new double[numTypes];
		super.addInstances(training);
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
				typeMass += probs[topic] = phi[topic][type] * alpha[topic];
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
	public void preSample() {
		super.preSample();
		int poolSize = 2; // Parallel alias table pool (why 2?)
		tableBuilderExecutor = Executors.newFixedThreadPool(Math.max(1, poolSize));
	}

	@Override
	public void preIteration() {
		final int [][] topicTypeIndices = topicIndexBuilder.getTopicTypeIndices();
		List<ParallelTableBuilder> builders = new ArrayList<>();
		if(topicTypeIndices!=null) {
			// The topicIndexBuilder supports having different types per topic,
			// this is currently not used, so we can just pick the first topic
			// since it will be the same for all topics
			int [] typesToSample = topicTypeIndices[0];
			for (int typeIdx = 0; typeIdx < typesToSample.length; typeIdx++) {
				builders.add(new ParallelTableBuilder(typesToSample[typeIdx]));
			}
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
		super.preIteration();
	}

	@Override
	public void prePhi() {
		super.prePhi();
		for (int type = 0; type < numTypes; type++) {
			nonZeroTypeTopicColIdxs[type].set(0);
			Arrays.fill(nonZeroTypeTopicIdxs[type],0);
		}
	}

	@Override
	public void postIteration() {
		//System.out.println("Used prior: " + toPrior.get() + " / " + corpusWordCount);
		//toPrior.set(0);
		//System.out.println("Used typeSparseness (" + config.getVariableSelectionPrior(vsPriorDefault) + "): " + usedTypeSparsness.get());
		//usedTypeSparsness.set(0);
		super.postIteration();
	}

	@Override
	public void postSample() {
		tableBuilderExecutor.shutdown();
		super.postSample();
	}
	
	@Override
	protected LDADocSamplingResultSparseSimple sampleTopicAssignmentsParallel(LDADocSamplingContext ctx) {
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
		//Arrays.fill(nonZeroTopicsBackMapping, NOT_IN_SET);
		
		// Populate topic counts
		int nonZeroTopicCnt = 0;
		for (int position = 0; position < docLength; position++) {
			int topicInd = oneDocTopics[position];
			localTopicCounts[topicInd]++;
			if(localTopicCounts[topicInd]==1) {
				//nonZeroTopics.add(topicInd);
				//nonZeroTopicCnt++;
				nonZeroTopicCnt = insert(topicInd, nonZeroTopics, nonZeroTopicsBackMapping, nonZeroTopicCnt);
			}
		}
		//kdDensities[myBatch] += nonZeroTopicCnt;
		kdDensities.addAndGet(nonZeroTopicCnt);
		double sum; // sigma_likelihood
		double[] cumsum = new double[numTopics]; 
		int [] nonZeroTopicsAdjusted = new int[numTopics];
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
			
			int nonZeroTypeCnt = nonZeroTypeTopicColIdxs[type].get();
			
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
				// INTERSECTION SHOULD IMPROVE perf since we use result both in cunmsum and sample topic
				// Intersection needs to b O(k) for it to improve perf, but unless we add more memory 
				// requirements it becomes O(k log(k))
				nonZeroTopicsAdjusted = nonZeroTypeTopicIdxs[type]; // Is it zero or nonzero, its confusing?
				nonZeroTopicCntAdjusted = nonZeroTypeCnt;
				//usedTypeSparsness.incrementAndGet();
			} else {
				nonZeroTopicsAdjusted = nonZeroTopics;
				nonZeroTopicCntAdjusted = nonZeroTopicCnt;
			}
			
			double u = ThreadLocalRandom.current().nextDouble();
			
			// Document and type sparsity removed all (but one?) topics, just use the prior contribution
			if(nonZeroTopicCntAdjusted==0) {
				//toPrior.incrementAndGet();
				newTopic = aliasTables[type].generateSample(u); // uniform (0,1)
			} else {
				double score;
				int topic = nonZeroTopicsAdjusted[0];
				score = localTopicCounts[topic] * phi[topic][type];
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
			if (newTopic < 0 || newTopic > numTopics) {
				throw new IllegalStateException ("SpaliasUncollapsedParallelLDA: New valid topic not sampled (" + newTopic + ").");
			}

			// Put that new topic into the counts
			oneDocTopics[position] = newTopic;
			localTopicCounts[newTopic]++;

			// Potentially update nonZeroTopics mapping
			if(localTopicCounts[newTopic]==1) {
				//nonZeroTopics.add(newTopic);
				//nonZeroTopicCnt++;
				nonZeroTopicCnt = insert(newTopic, nonZeroTopics, nonZeroTopicsBackMapping, nonZeroTopicCnt);
			}

			// Propagates the update to the topic-token assignments
			/**
			 * Used to subtract and add 1 to the local structure containing the number of times
			 * each token is assigned to a certain topic. Called before and after taking a sample
			 * topic assignment z
			 */
			increment(myBatch, newTopic, type);
			//System.out.println("(Batch=" + myBatch + ") Incremented: topic=" + newTopic + " type=" + type + " => " + batchLocalTopicUpdates[myBatch][newTopic][type]);		
		}
		//System.out.println("Ratio: " + ((double)numPrior/(double)numLikelihood));
		return new LDADocSamplingResultSparseSimple(localTopicCounts,nonZeroTopicCnt,nonZeroTopics);
	}

	/*
	protected int intersection(int [] nonZeroTypeTopicIdxs, int nonZeroTypeTopicCnt, int[] nonZeroDocumentTopics, int [] nonZeroDocumentTopicsBackMapping,
			int[] nonZeroTopicsAdjusted, int nonZeroDocumentTopicCnt) {
		int nonZeroCnt = 0;
		// If we have more type sparsity loop over nonZeroTypeTopicIdxs, else loop over nonZeroTypeTopicCnt
		if(nonZeroTypeTopicCnt < nonZeroDocumentTopicCnt) {
			usedTypeSparsness.incrementAndGet();
			for (int i = 0; i < nonZeroTypeTopicCnt; i++) {
				if(nonZeroDocumentTopicsBackMapping[nonZeroTypeTopicIdxs[i]]!=NOT_IN_SET)
					nonZeroTopicsAdjusted[nonZeroCnt++] = nonZeroTypeTopicIdxs[i];
			}
		} else {
			for (int i = 0; i < nonZeroDocumentTopicCnt; i++) {
				if(findInTypeSet(nonZeroDocumentTopics[i],nonZeroTypeTopicIdxs, nonZeroTypeTopicCnt)!=NOT_IN_SET)
					nonZeroTopicsAdjusted[nonZeroCnt++] = nonZeroDocumentTopics[i];
			}
			
		}
		return nonZeroCnt;
	}

	private int findInTypeSet(int value, int[] nonZeroTypeTopicIdxs, int numNonZeroForType) {
		// int currSize = size.get();
		// Find the place to insert
		int i = 0;
		while (i < numNonZeroForType && nonZeroTypeTopicIdxs[i]<value) i++;
		// topic is already inserted
		if(nonZeroTypeTopicIdxs[i]==value) {
			return i;
		};
		return NOT_IN_SET;
	}*/

	double calcCumSum(int type, double[] localTopicCounts, int[] nonZeroTopics, int nonZeroTopicCnt, double[] cumsum) {
		double score;
		double sum;
		int topic = nonZeroTopics[0];
		score = localTopicCounts[topic] * phi[topic][type];
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
		sum = cumsum[topicIdx-1]; // sigma_likelihood
		return sum;
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
			//toPrior.incrementAndGet();
			newTopic = aliasTables[type].generateSample(u+((sum*u)/typeNorm[type])); // uniform (0,1)
			//System.out.println("Prior Sampled topic: " + newTopic);
		} else {
			//numLikelihood++;
			//double u_lik = (u_sigma - typeNorm[type]) / sum; // Cumsum is not normalized so don't divide by sum 
			double u_lik = (u_sigma - typeNorm[type]);
			int slot = findIdx(cumsum,u_lik,nonZeroTopicCnt);
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
			throw new IllegalArgumentException ("SpaliasUncollapsedParallelLDA: Cannot remove, count is less than 1");
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

	protected static int findIdx(double[] cumsum, double u, int maxIdx) {
		return findIdxBin(cumsum,u,maxIdx);
	}
	
	protected static int findIdxBin(double[] cumsum, double u, int maxIdx) {
		int slot = java.util.Arrays.binarySearch(cumsum,0,maxIdx,u);

		return slot >= 0 ? slot : -(slot+1); 
	}

	public static int findIdxLinSentinel(double[] cumsum, double u, int maxIdx) {
		cumsum[cumsum.length-1] = Double.MAX_VALUE;
		int i = 0;
		while(true) {
			if(u<=cumsum[i]) return i;
			i++;
		}
	}

	public static int findIdxLin(double[] cumsum, double u, int maxIdx) {
		for (int i = 0; i < maxIdx; i++) {
			if(u<=cumsum[i]) return i;
		}
		return cumsum.length-1;
	}
	
	
	@Override
	protected void samplePhi() {
		super.samplePhi();
	}

	/**
	 * Samples new Phi's using variable selection. 
	 * 
	 * @param indices
	 * @param topicTypeIndices
	 * @param phiMatrix
	 */
	@Override
	public void loopOverTopics(int [] indices, int[][] topicTypeIndices, double[][] phiMatrix) {
		long beforeSamplePhi = System.currentTimeMillis();		
		for (int topic : indices) {
			int [] relevantTypeTopicCounts = topicTypeCountMapping[topic];
			VariableSelectionResult res = vsDirichlet.nextDistribution(relevantTypeTopicCounts, phiMatrix[topic]);
			phiMatrix[topic] = res.getPhi();
			int [] nonZeroIdxs = res.getNonZeroIdxs();
			//if(topic==0) {System.out.println("Non Zero in topic: " + zeroIdxs.length + " / " + phi[0].length + " = " + ((double)zeroIdxs.length)/phi[0].length);}
			for (int i = 0; i < nonZeroIdxs.length; i++) {
				int type = nonZeroIdxs[i];
				synchronized (nonZeroTypeTopicIdxsColLocks[type]) {					
					IntArraySortUtils.arrayIntSetAddSorted(nonZeroTypeTopicIdxs[type], topic, nonZeroTypeTopicColIdxs[type]);
				}
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
}
