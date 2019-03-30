package cc.mallet.topics.randomscan.topic;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.topics.LDAGibbsSampler;
import cc.mallet.types.InstanceList;

public class EvenSplitTopicBatchBuilder implements TopicBatchBuilder {

	int[] topicBatchSizeArray;
	int[] topicBatchStartArray;
	int [][] topicBatches;
	LDAGibbsSampler sampler;
	LDAConfiguration config;
	InstanceList data;
	int numTopics = -1; // Make sure things crash if the default is used, fail fast!

	public EvenSplitTopicBatchBuilder(LDAConfiguration config, LDAGibbsSampler sampler) {
		this.config = config;
		this.data = sampler.getDataset();
	}
	
	@Override
	public void setSampler(LDAGibbsSampler sampler) {
		this.sampler = sampler;
	}
	
	@Override
	public void calculateBatch() {
		int numBatches = config.getNoTopicBatches(LDAConfiguration.NO_TOPIC_BATCHES_DEFAULT);
		numTopics = config.getNoTopics(LDAConfiguration.NO_TOPICS_DEFAULT);
		topicBatchSizeArray = new int[numBatches];
		topicBatchStartArray = new int[numBatches];
		int topicRemainder = numTopics % numBatches;
		int topicBatchSize = numTopics / numBatches;
		for (int b = 0; b < numBatches; b++) {
			topicBatchSizeArray[b] = topicBatchSize + (topicRemainder > b ? 1 : 0);
			if(b > 0) topicBatchStartArray[b] = topicBatchStartArray[b-1] + topicBatchSizeArray[b-1];
		}
	}
	
	@Override
	public synchronized int[][] topicBatches() {
		if(topicBatches==null) {
			topicBatches = new int[topicBatchSizeArray.length][];
			for (int i = 0; i < topicBatches.length; i++) {
				topicBatches[i] = new int[topicBatchSizeArray[i]];
				int idx = topicBatchStartArray[i];
				for (int j = 0; j < topicBatches[i].length; j++) {
					topicBatches[i][j] = idx++;
				}
			}
			return topicBatches;
		} else {
			return topicBatches;
		}
	}

	/**
	 * This builder samples all topics every iteration, this can be changed in subclasses
	 * @see cc.mallet.topics.randomscan.topic.TopicBatchBuilder#getTopicsInIteration(int)
	 */
	@Override
	public int getTopicsInIteration(int currentIteration) {
		return numTopics;
	}
}
