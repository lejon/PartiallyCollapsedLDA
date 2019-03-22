package cc.mallet.topics;

public class LDADocSamplingResultDense implements LDADocSamplingResult {

	int[] localTopicCounts;
	
	public LDADocSamplingResultDense(int[] localTopicCounts) {
		super();
		this.localTopicCounts = localTopicCounts;
	}

	@Override
	public int[] getLocalTopicCounts() {
		return localTopicCounts;
	}

}
