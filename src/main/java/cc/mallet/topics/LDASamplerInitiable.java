package cc.mallet.topics;

public interface LDASamplerInitiable extends LDAGibbsSampler {
	void initFrom(LDAGibbsSampler source);
}
