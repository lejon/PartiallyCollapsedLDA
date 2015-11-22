package cc.mallet.topics.randomscan.topic;

import cc.mallet.topics.LDAGibbsSampler;


/**
 * In general batch builders are NOT thread safe, they are intended to be called only from
 * the coordinator thread!
 * 
 *
 */
public interface TopicBatchBuilder {

	/**
	 * Do the calculation of the batch size, this Algorithm can vary depending on scheme
	 */
	void calculateBatch();

	/**
	 * The result is a matrix that contains the topic indices to sample for each worker
	 * @return a matrix A indexed by A[batch][documentIdx]
	 */
	int [][] topicBatches();

	
	/**
	 * @param currentIteration
	 * @return how many topics should be sampled during this iteration
	 */
	int getTopicsInIteration(int currentIteration);
	
	
	/**
	 * Sets the sampler that wants to do random scan, we might need various statistics from 
	 * the sampler to decide which documents, types and topics to sample 
	 * @param sampler
	 */
	void setSampler(LDAGibbsSampler sampler);
}
