package cc.mallet.configuration;

import java.lang.reflect.InvocationTargetException;

import cc.mallet.topics.LDAGibbsSampler;
import cc.mallet.topics.LDASamplerWithCallback;
import cc.mallet.topics.tui.IterationListener;

public class ModelFactory {
	@SuppressWarnings("unchecked")
	public static synchronized LDAGibbsSampler get(LDAConfiguration config, double[][] xs, int[] ys) {
		String model_name = config.getSamplerClass(LDAConfiguration.MODEL_DEFAULT);

		@SuppressWarnings("rawtypes")
		Class modelClass = null;
		try {
			modelClass = Class.forName(model_name);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e);
		}

		@SuppressWarnings("rawtypes")
		Class[] argumentTypes = new Class[3];
		argumentTypes[0] = LDAConfiguration.class; 
		argumentTypes[1] = double [][].class; 
		argumentTypes[2] = int [].class;

		try {
			return (LDAGibbsSampler) modelClass.getDeclaredConstructor(argumentTypes)
					.newInstance(config,xs,ys);
		} catch (InstantiationException | IllegalAccessException
				| InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static synchronized LDASamplerWithCallback get(LDAConfiguration config, double[][] xs, 
			int[] ys, IterationListener callback) {
		String model_name = config.getSamplerClass(LDAConfiguration.MODEL_DEFAULT);

		@SuppressWarnings("rawtypes")
		Class modelClass = null;
		try {
			modelClass = Class.forName(model_name);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e);
		}

		@SuppressWarnings("rawtypes")
		Class[] argumentTypes = new Class[3];
		argumentTypes[0] = LDAConfiguration.class; 
		argumentTypes[1] = double [][].class; 
		argumentTypes[2] = int [].class;

		try {
			LDASamplerWithCallback dwc = (LDASamplerWithCallback) modelClass.getDeclaredConstructor(argumentTypes)
					.newInstance(config,xs,ys);
			dwc.setIterationCallback(callback);
			return dwc;
		} catch (InstantiationException | IllegalAccessException
				| InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e);
		}
	}
}
