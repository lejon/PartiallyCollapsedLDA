package cc.mallet.types;

import java.lang.reflect.InvocationTargetException;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.topics.LDAGibbsSampler;

public class PolyaUrnFixedCoeffPoissonDirichletSamplerBuilder extends StandardArgsDirichletBuilder {
	
	String samplerClassName = "cc.mallet.types.PolyaUrnDirichletFixedCoeffPoisson";

	@Override
	public SparseDirichlet build(LDAGibbsSampler sampler) {
		PoissonFixedCoeffSampler fep = new PoissonFixedCoeffSampler(
				sampler.getConfiguration().getBeta(LDAConfiguration.BETA_DEFAULT), 
				sampler.getConfiguration().getAliasPoissonThreshold(LDAConfiguration.ALIAS_POISSON_DEFAULT_THRESHOLD));
		return instantiateSparseDirichletSampler(samplerClassName, 
				sampler.getNoTypes(), 
				sampler.getConfiguration().getBeta(LDAConfiguration.BETA_DEFAULT), fep);
	}
	
	@SuppressWarnings("unchecked")
	private SparseDirichlet instantiateSparseDirichletSampler(String samplerClassName, int numTypes, double beta, PoissonFixedCoeffSampler fep) {

		@SuppressWarnings("rawtypes")
		Class modelClass = null;
		try {
			modelClass = Class.forName(samplerClassName);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e);
		}

		@SuppressWarnings("rawtypes")
		Class[] argumentTypes = new Class[3];
		argumentTypes[0] = int.class;
		argumentTypes[1] = double.class; 
		argumentTypes[2] = PoissonFixedCoeffSampler.class; 

		try {
			return (SparseDirichlet) modelClass.getDeclaredConstructor(argumentTypes).newInstance(numTypes,beta,fep);
		} catch (InstantiationException | IllegalAccessException
				| InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e);
		}
	}


	@Override
	protected String getSparseDirichletSamplerClassName() {
		return samplerClassName;
	}

}
