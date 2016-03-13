package cc.mallet.topics;

public interface LDASamplerWithPhi extends LDAGibbsSampler {
	double [][] getPhi();
	double [][] getPhiMeans();
	public void prePhi();
	public void postPhi();
}
