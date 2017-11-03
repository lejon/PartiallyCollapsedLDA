package cc.mallet.topics;

import cc.mallet.types.Alphabet;

public interface LDASamplerWithPhi extends LDAGibbsSampler {
	double [][] getPhi();
	void setPhi(double [][] phi, Alphabet dataAlphabet, Alphabet targetAlphabet);
	double [][] getPhiMeans();
	public void prePhi();
	public void postPhi();
	void sampleZGivenPhi(int iterations);
}
