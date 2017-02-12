package cc.mallet.types;

import java.lang.reflect.InvocationTargetException;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.topics.LDAGibbsSampler;

public abstract class StandardArgsDirichletBuilder implements SparseDirichletSamplerBuilder {

	@Override
	public SparseDirichlet build(LDAGibbsSampler sampler) {
		return instantiateSparseDirichletSampler(getSparseDirichletSamplerClassName(), 
				sampler.getNoTypes(), 
				sampler.getConfiguration().getBeta(LDAConfiguration.BETA_DEFAULT));
	}
	
	@SuppressWarnings("unchecked")
	protected synchronized SparseDirichlet instantiateSparseDirichletSampler(String samplerClassName, int numTypes, double beta) {

		@SuppressWarnings("rawtypes")
		Class modelClass = null;
		try {
			modelClass = Class.forName(samplerClassName);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e);
		}

		@SuppressWarnings("rawtypes")
		Class[] argumentTypes = new Class[2];
		argumentTypes[0] = int.class;
		argumentTypes[1] = double.class; 

		try {
			return (SparseDirichlet) modelClass.getDeclaredConstructor(argumentTypes).newInstance(numTypes,beta);
		} catch (InstantiationException | IllegalAccessException
				| InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e);
		}
	}

	protected abstract String getSparseDirichletSamplerClassName();

}
