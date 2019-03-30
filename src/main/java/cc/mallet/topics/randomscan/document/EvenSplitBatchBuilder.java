package cc.mallet.topics.randomscan.document;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.topics.LDAGibbsSampler;
import cc.mallet.types.InstanceList;

public class EvenSplitBatchBuilder implements DocumentBatchBuilder {

	LDAGibbsSampler sampler;
	LDAConfiguration config;
	InstanceList data;

	int[] batchSizeArray;
	int[] batchStartArray;
	int [][] documentBatches;
	int documentsPerIter;
	

	public EvenSplitBatchBuilder(LDAConfiguration config, LDAGibbsSampler sampler) {
		this.config = config;
		this.data = sampler.getDataset();
	}
	
	@Override
	public void setSampler(LDAGibbsSampler sampler) {
		this.sampler = sampler;
	}

	@Override
	public synchronized void calculateBatch() {
		documentsPerIter = 0;
		int corpusSize = data.size();
		// Initializes the batch sizes to be as even as possible
		int numBatches = config.getNoBatches(LDAConfiguration.NO_BATCHES_DEFAULT);
		int remainder = (corpusSize % numBatches);
		int batchSize = (corpusSize / numBatches);
		batchSizeArray = new int[numBatches];
		batchStartArray = new int[numBatches];
		for (int b = 0; b < numBatches; b++) {
			batchSizeArray[b] = batchSize + (remainder > b ? 1 : 0);
			documentsPerIter += batchSizeArray[b]; 
			if(b > 0) batchStartArray[b] = batchStartArray[b-1] + batchSizeArray[b-1];
		}
	}

	@Override
	public synchronized int[][] documentBatches() {
		if(documentBatches==null) {
			documentBatches = new int[batchSizeArray.length][];
			for (int i = 0; i < documentBatches.length; i++) {
				documentBatches[i] = new int[batchSizeArray[i]];
				int idx = batchStartArray[i];
				for (int j = 0; j < documentBatches[i].length; j++) {
					documentBatches[i][j] = idx++;
				}
			}
			return documentBatches;
		} else {
			return documentBatches;
		}
	}

	@Override
	public int getDocResultsSize() {
		return config.getResultSize(LDAConfiguration.RESULTS_SIZE_DEFAULT);
	}

	/* This implementation have the same number of documents in each iteration
	 * @see utils.BatchBuilder#getDocumentsInIteration(int)
	 */
	@Override
	public int getDocumentsInIteration(int currentIteration) {
		return documentsPerIter;
	}
}
