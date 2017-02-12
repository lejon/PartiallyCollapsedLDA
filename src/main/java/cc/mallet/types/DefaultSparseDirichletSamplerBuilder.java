package cc.mallet.types;

import cc.mallet.configuration.LDAConfiguration;

public class DefaultSparseDirichletSamplerBuilder extends StandardArgsDirichletBuilder {

	@Override
	protected String getSparseDirichletSamplerClassName() {
		return LDAConfiguration.SPARSE_DIRICHLET_SAMPLER_DEFAULT;
	}

}
