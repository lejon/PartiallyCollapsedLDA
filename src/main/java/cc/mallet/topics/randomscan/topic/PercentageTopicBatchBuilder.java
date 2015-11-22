package cc.mallet.topics.randomscan.topic;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.topics.LDAGibbsSampler;
import cc.mallet.util.IndexSampler;
import cc.mallet.util.WithoutReplacementSampler;

/**
 * 
 * Samples X% of the ROWS (which topics) in Phi controlled by the <code>percentage_split_size_topic</code>
 * config parameter. 
 * 
 *  Config Example:
 *  	percentage_split_size_topic = 0.01 # Samples 1 % of (the topics) rows of Phi
 *
 */
public class PercentageTopicBatchBuilder implements TopicBatchBuilder {

	LDAConfiguration config;;
	int numTopicBatches;
	int numTopics;
	int topicsToSamplePerIteration;
	int remainder;
	int topicRemainder;
	int topicsPerBatch;
	double phiPercentage = 1.0;

	// The topic batches
	int [][] topicBatches;
	LDAGibbsSampler sampler; 

	public PercentageTopicBatchBuilder(LDAConfiguration config, LDAGibbsSampler sampler) {
		this.config = config;
		this.sampler = sampler;
		this.phiPercentage = config.getTopicPercentageSplitSize();
		this.numTopicBatches = config.getNoTopicBatches(LDAConfiguration.NO_TOPIC_BATCHES_DEFAULT);
		this.numTopics = config.getNoTopics(LDAConfiguration.NO_TOPICS_DEFAULT);

		// Calculate the number of topics to sample per batch
		this.topicsToSamplePerIteration = (int) Math.ceil(phiPercentage * numTopics);
		this.topicsPerBatch = topicsToSamplePerIteration / numTopicBatches;
		this.topicRemainder = topicsToSamplePerIteration % numTopicBatches;
		this.topicBatches = new int[numTopicBatches][];
	}
	
	@Override
	public void setSampler(LDAGibbsSampler sampler) {
		this.sampler = sampler;
	}

	@Override
	public void calculateBatch() {
		IndexSampler is = new WithoutReplacementSampler(0, numTopics);
		
		for (int b = 0; b < numTopicBatches; b++) {
			int topicsInBatch = topicsPerBatch + (topicRemainder > b ? 1 : 0);
			int [] topicIndices = new int[topicsInBatch];
			for (int j = 0; j < topicsInBatch; j++) {
				topicIndices[j] = is.nextSample();
			}
			topicBatches[b] = topicIndices;
		}
	}

	@Override
	public int[][] topicBatches() {
		return topicBatches;
	}

	@Override
	public int getTopicsInIteration(int currentIteration) {
		return topicsToSamplePerIteration;
	}
}
