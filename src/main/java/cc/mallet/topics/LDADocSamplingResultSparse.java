package cc.mallet.topics;

public interface LDADocSamplingResultSparse extends LDADocSamplingResult {

	int getNonZeroTopicCounts();
	int [] getNonZeroIndices();

}