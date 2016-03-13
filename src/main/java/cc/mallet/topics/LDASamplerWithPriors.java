package cc.mallet.topics;

public interface LDASamplerWithPriors extends LDAGibbsSampler {
	double [][] getTopicPriors();
}
