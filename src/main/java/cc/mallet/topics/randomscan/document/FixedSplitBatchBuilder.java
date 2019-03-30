package cc.mallet.topics.randomscan.document;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.topics.LDAGibbsSampler;
import cc.mallet.types.InstanceList;

/**
 * 
 * This document batch builder builds batches per iteration that are a fixed %'age of the
 * total corpus. The percentage is read from the <code>fixed_split_size_doc</code> config
 * parameter. This can be an array of percentages (0.01, in which case it will it will take
 * these in order, the first percentage the first iteration, the next the next iteration
 * and so on. If it is only one value this will be used for every iteration. The documents
 * are NOT randomly drawn! It takes X% from the beginning of the corpus and then the next
 * X% of the corpus and so on. 
 *
 */
public class FixedSplitBatchBuilder implements DocumentBatchBuilder {

	LDAGibbsSampler sampler;
	LDAConfiguration config;
	InstanceList data;

	int[] batchSizeArray;
	int [][] documentBatches;
	int numBatches;

	int documentsInIter;
	double [] percentages;
	// We are looping over the different percentages
	// percentagePointer index is the current position
	int percentagePointer = 0;
	// Points to the next document to be added to a batch
	int globalDocPointer = 1;

	public FixedSplitBatchBuilder(LDAConfiguration config, LDAGibbsSampler sampler) {
		this.config = config;
		this.data = sampler.getDataset();
		percentages = config.getFixedSplitSizeDoc();
		numBatches = config.getNoBatches(LDAConfiguration.NO_BATCHES_DEFAULT);
		if(percentages==null||percentages.length==0) {
			throw new IllegalArgumentException("Using 'fixed_split_size_doc' but did not find valid config for it.");
		}
	}

	@Override
	public void setSampler(LDAGibbsSampler sampler) {
		this.sampler = sampler;
	}

	@Override
	public synchronized void calculateBatch() {
		int corpusSize = data.size();
		documentsInIter = (int) Math.ceil(percentages[percentagePointer]*corpusSize);
		percentagePointer = (percentagePointer+1) % percentages.length;

		// Distribute the documents evenly over the processors
		int remainder = (documentsInIter % numBatches);
		int batchSize = (documentsInIter / numBatches);
		batchSizeArray = new int[numBatches];
		for (int b = 0; b < numBatches; b++) {
			batchSizeArray[b] = batchSize + (remainder > b ? 1 : 0);
		}
		
		documentBatches = new int[numBatches][];
		for (int i = 0; i < documentBatches.length; i++) {
			documentBatches[i] = new int[batchSizeArray[i]];
			for (int j = 0; j < documentBatches[i].length; j++) {
				documentBatches[i][j] = globalDocPointer++;
				// If we have reached the end of the corpus, start over
				if(globalDocPointer>=corpusSize) {
					globalDocPointer = 0;
				}
			}
		}
	}

	@Override
	public synchronized int[][] documentBatches() {
		return documentBatches;
	}

	@Override
	public int getDocResultsSize() {
		return config.getResultSize(LDAConfiguration.RESULTS_SIZE_DEFAULT);
	}

	@Override
	public int getDocumentsInIteration(int currentIteration) {
		return documentsInIter;
	}
}
