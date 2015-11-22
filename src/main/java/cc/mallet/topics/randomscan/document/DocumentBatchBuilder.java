package cc.mallet.topics.randomscan.document;

import cc.mallet.topics.LDAGibbsSampler;


/**
 * In general batch builders are NOT thread safe, they are intended to be called only from
 * the coordinator thread!
 * 
 *
 */
public interface DocumentBatchBuilder {

	/**
	 * Do the calculation of the batch size, this Algorithm can vary depending on scheme
	 */
	void calculateBatch();

	/**
	 * The result is a matrix that contains the document indices to sample for each worker
	 * @return a matrix A indexed by A[batch][documentIdx]
	 */
	public int[][] documentBatches();
	
	/**
	 * @return how many documents the workers should buffer before sending the resulting
	 * samples
	 */
	int getDocResultsSize();

	/**
	 * @param currentIteration
	 * @return how many documenst should be sampled during this iteration
	 */
	int getDocumentsInIteration(int currentIteration);
	
	
	/**
	 * Sets the sampler that wants to do random scan, we might need various statistics from 
	 * the sampler to decide which documents, types and topics to sample 
	 * @param sampler
	 */
	void setSampler(LDAGibbsSampler sampler);
}
