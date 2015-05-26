package cc.mallet.topics.randomscan.document;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.topics.LDAGibbsSampler;
import cc.mallet.util.IndexSampler;
import cc.mallet.util.WithoutReplacementSampler;

public class PercentageBatchBuilder implements DocumentBatchBuilder {

	LDAConfiguration config;
	int numBatches;
	int docsToSamplePerIteration;
	int docsPerBatch;
	int remainder;
	int totalDocsAvailable;
	double docsPercentage = 1.0;

	// The document batches
	int [][] docBatches; 
	LDAGibbsSampler sampler; 

	public PercentageBatchBuilder(LDAConfiguration config, LDAGibbsSampler sampler) {
		this.config = config;
		this.sampler = sampler;

		// Document part
		this.totalDocsAvailable = sampler.getDataset().size();
		
		this.docsPercentage = config.getDocPercentageSplitSize();
		// Calculate the number of docs to sample per batch
		calculateDocumentsToSample(config, docsPercentage);
	}

	protected void calculateDocumentsToSample(LDAConfiguration config, double docsPercentage) {
		this.docsToSamplePerIteration = (int) Math.ceil(docsPercentage * totalDocsAvailable);
		this.numBatches = config.getNoBatches(LDAConfiguration.NO_BATCHES_DEFAULT);
		this.docBatches = new int[numBatches][];
		// The number of docs should be evenly split over the batches
		this.docsPerBatch = docsToSamplePerIteration / numBatches;
		// We split the remainder evenly over the batches
		this.remainder = docsToSamplePerIteration % numBatches;
	}
	
	@Override
	public void setSampler(LDAGibbsSampler sampler) {
		this.sampler = sampler;
	}

	@Override
	public void calculateBatch() {
		IndexSampler is = new WithoutReplacementSampler(0, totalDocsAvailable);
		
		for(int i = 0; i < numBatches; i++) {
			// This is needed since we split the remainder evenly over the batches
			int docsInBatch = docsPerBatch + (remainder > i ? 1 : 0);
			int [] indices = new int[docsInBatch];
		
			// Sample w/o replacement
			for (int j = 0; j < docsInBatch; j++) {
				indices[j] = is.nextSample();
			}
			docBatches[i] = indices;
		}
	}

	int calcDocsToSample() {
		return docsToSamplePerIteration;
	}

	@Override
	public int[][] documentBatches() {
		return docBatches;
	}

	@Override
	public int getDocResultsSize() {
		return config.getResultSize(LDAConfiguration.RESULTS_SIZE_DEFAULT);
	}

	@Override
	public int getDocumentsInIteration(int currentIteration) {
		return calcDocsToSample();
	}
}
