package cc.mallet.topics;

public class LDADocSamplingResultSparseSimple extends LDADocSamplingResultDense implements LDADocSamplingResultSparse {

	int nonZeroTopicCnt;
	int [] nonZeroIndices;

	public LDADocSamplingResultSparseSimple(int[] localTopicCounts, int nonZeroTopicCnt, int[] nonZeroIndices) {
		super(localTopicCounts);
		this.nonZeroTopicCnt = nonZeroTopicCnt;
		this.nonZeroIndices = nonZeroIndices;
	}

	@Override
	public int getNonZeroTopicCounts() {
		return nonZeroTopicCnt;
	}

	@Override
	public int[] getNonZeroIndices() {
		return nonZeroIndices;
	}

}
