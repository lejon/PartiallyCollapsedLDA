package cc.mallet.topics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.util.SparsityTools;
import cc.mallet.util.WalkerAliasTable;

public abstract class SparseUncollapsedSampler extends UncollapsedParallelLDA {

	private static final long serialVersionUID = 1L;

	WalkerAliasTable [] aliasTables; 
	double [] typeNorm; // Array with doubles with sum of alpha * phi
	transient ExecutorService tableBuilderExecutor;

	boolean staticPhiAliasTableIsBuild = false;


	public SparseUncollapsedSampler(LDAConfiguration config) {
		super(config);
	}

	@Override
	public void preSample() {
		super.preSample();
		int poolSize = 2; // Parallel alias table pool (why 2?)
		tableBuilderExecutor = Executors.newFixedThreadPool(Math.max(1, poolSize));
	}

	@Override
	public void preContinuedSampling() {
		super.preContinuedSampling();
		int poolSize = 2; // Parallel alias table pool (why 2?)
		tableBuilderExecutor = Executors.newFixedThreadPool(Math.max(1, poolSize));
	}

	@Override
	public void preIteration() {
		doPreIterationTableBuilding();
		super.preIteration();
	}

	public void preIterationGivenPhi() {
		if(!staticPhiAliasTableIsBuild) {
			doPreIterationTableBuilding();
			super.preIterationGivenPhi();
			staticPhiAliasTableIsBuild = true;
		}
	}

	@Override
	public void postSample() {
		super.postSample();
		tableBuilderExecutor.shutdown();
	}

	@Override
	public void postContinuedSampling() {
		super.postContinuedSampling();
		tableBuilderExecutor.shutdown();
	}

	protected abstract Callable<WalkerAliasTableBuildResult> getAliasTableBuilder(int type);

	protected void doPreIterationTableBuilding() {
		List<Callable<WalkerAliasTableBuildResult>> builders = new ArrayList<>();
		final int [][] topicTypeIndices = topicIndexBuilder.getTopicTypeIndices();
		if(topicTypeIndices!=null) {
			// The topicIndexBuilder supports having different types per topic,
			// this is currently not used, so we can just pick the first topic
			// since it will be the same for all topics
			int [] typesToSample = topicTypeIndices[0];
			for (int typeIdx = 0; typeIdx < typesToSample.length; typeIdx++) {
				builders.add(getAliasTableBuilder(typesToSample[typeIdx]));
			}
			// if the topicIndexBuilder returns null it means sample ALL types
		} else {
			for (int type = 0; type < numTypes; type++) {
				builders.add(getAliasTableBuilder(type));
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

	int remove(int oldTopic, int[] nonZeroTopics, int[] nonZeroTopicsBackMapping, int nonZeroTopicCnt) {
		return SparsityTools.remove(oldTopic, nonZeroTopics, nonZeroTopicsBackMapping, nonZeroTopicCnt);
	}

	int insert(int newTopic, int[] nonZeroTopics, int[] nonZeroTopicsBackMapping, int nonZeroTopicCnt) {
		return SparsityTools.insert(newTopic, nonZeroTopics, nonZeroTopicsBackMapping, nonZeroTopicCnt);
	}

	int removeSorted(int oldTopic, int[] nonZeroTopics, int[] nonZeroTopicsBackMapping, int nonZeroTopicCnt) {
		return SparsityTools.removeSorted(oldTopic, nonZeroTopics, nonZeroTopicsBackMapping, nonZeroTopicCnt);
	}

	int insertSorted(int newTopic, int[] nonZeroTopics, int[] nonZeroTopicsBackMapping, int nonZeroTopicCnt) {
		return SparsityTools.insertSorted(newTopic, nonZeroTopics, nonZeroTopicsBackMapping, nonZeroTopicCnt);
	}

	int findIdx(double[] cumsum, double u, int maxIdx) {
		return SparsityTools.findIdx(cumsum, u, maxIdx);
	}

	int findIdxBin(double[] cumsum, double u, int maxIdx) {
		return SparsityTools.findIdxBin(cumsum, u, maxIdx);
	}

	int findIdxLinSentinel(double[] cumsum, double u, int maxIdx) {
		return SparsityTools.findIdxLinSentinel(cumsum, u, maxIdx);
	}

	int findIdxLin(double[] cumsum, double u, int maxIdx) {
		return SparsityTools.findIdxLin(cumsum, u, maxIdx);
	}

}
