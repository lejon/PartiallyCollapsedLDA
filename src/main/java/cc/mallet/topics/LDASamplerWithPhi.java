package cc.mallet.topics;

public interface LDASamplerWithPhi extends LDAGibbsSampler {
	double [][] getPhi();
	double [][] getPhiMeans();
	double [][] getTopicPriors();
	public void prePhi();
	public void postPhi();
}
