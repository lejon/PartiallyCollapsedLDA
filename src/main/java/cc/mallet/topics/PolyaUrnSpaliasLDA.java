package cc.mallet.topics;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelSequence;
import cc.mallet.types.PoissonFixedCoeffSampler;
import cc.mallet.types.SparseDirichlet;
import cc.mallet.types.VariableSelectionResult;
import cc.mallet.util.LoggingUtils;
import cc.mallet.util.OptimizedGentleAliasMethod;
import cc.mallet.util.WalkerAliasTable;

public class PolyaUrnSpaliasLDA extends UncollapsedParallelLDA implements LDAGibbsSampler{
	
	protected double[][] phitrans;

	private static final long serialVersionUID = 1L;
	WalkerAliasTable [] aliasTables; 
	double [] typeNorm; // Array with doubles with sum of alpha * phi
	private ExecutorService tableBuilderExecutor;
	
	
	// #### Sparsity handling
	// Jagged array containing the topics that are non-zero for each type
	int [][] nonZeroTypeTopicIdxs = null;
	// How many indices  are zero for each type, i.e the column count for the zeroTypeTopicIdxs array
	int [] nonZeroTypeTopicColIdxs = null;	

	public PolyaUrnSpaliasLDA(LDAConfiguration config) {
		super(config);
	}

	@Override
	public void addInstances(InstanceList training) {
		alphabet = training.getDataAlphabet();
		numTypes = alphabet.size();
		nonZeroTypeTopicIdxs = new int[numTypes][numTopics];
		phitrans    = new double[numTypes][numTopics];
		nonZeroTypeTopicColIdxs = new int[numTypes];
		aliasTables = new WalkerAliasTable[numTypes];
		typeNorm    = new double[numTypes];
		super.addInstances(training);
	}
	
	

	@Override
	protected SparseDirichlet createDirichletSampler() {
		PoissonFixedCoeffSampler fep = new PoissonFixedCoeffSampler(beta, 100);
		return instantiateSparseDirichletSampler("cc.mallet.types.PolyaUrnDirichletFixedCoeffPoisson",numTypes,beta,fep);
		//return instantiateSparseDirichletSampler("cc.mallet.types.PolyaUrnDirichlet",numTypes,beta);
	}

	@SuppressWarnings("unchecked")
	private SparseDirichlet instantiateSparseDirichletSampler(String samplerClassName, int numTypes, double beta, PoissonFixedCoeffSampler fep) {
		String model_name = config.getSparseDirichletSamplerClass(samplerClassName);

		@SuppressWarnings("rawtypes")
		Class modelClass = null;
		try {
			modelClass = Class.forName(model_name);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e);
		}

		@SuppressWarnings("rawtypes")
		Class[] argumentTypes = new Class[3];
		argumentTypes[0] = int.class;
		argumentTypes[1] = double.class; 
		argumentTypes[2] = PoissonFixedCoeffSampler.class; 

		try {
			return (SparseDirichlet) modelClass.getDeclaredConstructor(argumentTypes).newInstance(numTypes,beta,fep);
		} catch (InstantiationException | IllegalAccessException
				| InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e);
		}
	}

	class ParallelTableBuilder implements Callable<TableBuildResult> {
		int type;
		public ParallelTableBuilder(int type) {
			this.type = type;
		}
		@Override
		public TableBuildResult call() {
			double [] probs = new double[numTopics];
			double typeMass = 0; // Type prior mass
			double [] phiType =  phitrans[type]; 
			for (int topic = 0; topic < numTopics; topic++) {
				typeMass += probs[topic] = phiType[topic] * alpha; // alpha[topic]
				if(phiType[topic]!=0) {
					int newSize = nonZeroTypeTopicColIdxs[type]++;
					nonZeroTypeTopicIdxs[type][newSize] = topic;
				}
			}
			
			if(aliasTables[type]==null) {
				aliasTables[type] = new OptimizedGentleAliasMethod(probs,typeMass);
			} else {
				aliasTables[type].reGenerateAliasTable(probs, typeMass);
			}
				
			return new TableBuildResult(type, aliasTables[type], typeMass);
		}   
	}

	static class TableBuildResult {
		public int type;
		public WalkerAliasTable table;
		public double typeNorm;
		public TableBuildResult(int type, WalkerAliasTable table, double typeNorm) {
			super();
			this.type = type;
			this.table = table;
			this.typeNorm = typeNorm;
		}
	}

	@Override
	public void preSample() {
		super.preSample();
		int poolSize = 2; // Parallel alias table pool (why 2?)
		tableBuilderExecutor = Executors.newFixedThreadPool(Math.max(1, poolSize));
	}

	public static void transpose(double[][] matrix, double [][] transpose) {
		int rows = matrix.length;
		int cols = matrix[0].length;
		for (int row = 0; row < rows; row++)
			for (int col = 0; col < cols; col++)
				transpose[col][row] = matrix[row][col];
	}

	@Override
	public void preIteration() {
		
		transpose(phi, phitrans);
		
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
		
		List<Future<TableBuildResult>> results;
		try {
			results = tableBuilderExecutor.invokeAll(builders);
			for (Future<TableBuildResult> result : results) {
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
		Arrays.fill(nonZeroTypeTopicColIdxs,0);
		//for (int type = 0; type < numTypes; type++) {
			// Should not be needed, since we just overwrite previous iteration result
			//Arrays.fill(nonZeroTypeTopicIdxs[type],0);
		//}
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
		super.postSample();
		tableBuilderExecutor.shutdown();
	}

	@Override
	protected void sampleTopicAssignmentsParallel(LDADocSamplingContext ctx) {
		FeatureSequence tokens = ctx.getTokens();
		LabelSequence topics = ctx.getTopics();
		int myBatch = ctx.getMyBatch();
		
		int type, oldTopic, newTopic;

		final int docLength = tokens.getLength();
		if(docLength==0) return;
		
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
			//System.out.println("(Batch=" + myBatch + ") Incremented: topic=" + newTopic + " type=" + type + " => " + batchLocalTopicUpdates[myBatch][newTopic][type]);		
		}
		//System.out.println("Ratio: " + ((double)numPrior/(double)numLikelihood));
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

}
