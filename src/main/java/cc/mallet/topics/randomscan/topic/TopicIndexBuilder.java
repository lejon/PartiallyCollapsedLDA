package cc.mallet.topics.randomscan.topic;

public interface TopicIndexBuilder {

	/**
	 * A matrix that contains the types to sample for each topic
	 * @return an matrix A indexed by A[topic][typeIdx], null means sample ALL types for all topics 
	 */
	int[][] getTopicTypeIndices();

}
