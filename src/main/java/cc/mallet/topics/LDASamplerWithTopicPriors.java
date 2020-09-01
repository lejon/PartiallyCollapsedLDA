package cc.mallet.topics;

public interface LDASamplerWithTopicPriors extends LDAGibbsSampler {
	double [][] getTopicPriors();
}
