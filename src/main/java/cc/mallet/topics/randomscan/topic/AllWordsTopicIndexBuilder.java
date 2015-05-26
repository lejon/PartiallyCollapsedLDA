package cc.mallet.topics.randomscan.topic;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.topics.LDAGibbsSampler;

public class AllWordsTopicIndexBuilder implements TopicIndexBuilder {
	
	LDAConfiguration config;
	LDAGibbsSampler sampler;
	int[][] topicTypeIndices;
	
	public AllWordsTopicIndexBuilder(LDAConfiguration config, LDAGibbsSampler sampler) {
		this.config = config;
		this.sampler = sampler;
		//topicTypeIndices = AllWordsTopicIndexBuilder.getAllIndicesMatrix(sampler.getAlphabet().size(), sampler.getNoTopics());
	}
	
	/**
	 * Sample all words
	 */
	@Override
	public synchronized int[][] getTopicTypeIndices() {
		//return topicTypeIndices;
		// null means sample all
		return null;
	}

	public static int[][] getAllIndicesMatrix(int vocabSize, int noTopics) {
		int [] indicesToSample = new int[vocabSize];
		for (int i = 0; i < indicesToSample.length; i++) {
			indicesToSample[i] = i;
		}
		int [][] topicTypeIndices = new int [noTopics][];
		// In the basic version we sample the same tokens (words) in all the topics
		for (int i = 0; i < topicTypeIndices.length; i++) {
			topicTypeIndices[i] = indicesToSample;
		}
		return topicTypeIndices;
	}

}
