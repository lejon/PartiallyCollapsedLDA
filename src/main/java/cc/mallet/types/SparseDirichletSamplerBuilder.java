package cc.mallet.types;

import cc.mallet.topics.LDAGibbsSampler;

public interface SparseDirichletSamplerBuilder {
	SparseDirichlet build(LDAGibbsSampler sampler);
}
