package cc.mallet.configuration;

import java.lang.reflect.InvocationTargetException;

import cc.mallet.topics.LDAGibbsSampler;
import cc.mallet.topics.LDASamplerWithCallback;
import cc.mallet.topics.tui.IterationListener;

public class ModelFactory {
	@SuppressWarnings("unchecked")
	public static synchronized LDAGibbsSampler get(LDAConfiguration config) {
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
		Class[] argumentTypes = new Class[1];
		argumentTypes[0] = LDAConfiguration.class; 
		
		try {
			return (LDAGibbsSampler) modelClass.getDeclaredConstructor(argumentTypes)
					.newInstance(config);
		} catch (InstantiationException | IllegalAccessException
				| InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static synchronized LDAGibbsSampler get(LDAConfiguration config, String model_name) {
		@SuppressWarnings("rawtypes")
		Class modelClass = null;
		try {
			modelClass = Class.forName(model_name);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e);
		}

		@SuppressWarnings("rawtypes")
		Class[] argumentTypes = new Class[1];
		argumentTypes[0] = LDAConfiguration.class; 

		try {
			return (LDAGibbsSampler) modelClass.getDeclaredConstructor(argumentTypes)
					.newInstance(config);
		} catch (InstantiationException | IllegalAccessException
				| InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static synchronized LDASamplerWithCallback get(LDAConfiguration config, IterationListener callback) {
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
		Class[] argumentTypes = new Class[2];
		argumentTypes[0] = LDAConfiguration.class;
		argumentTypes[1] = IterationListener.class;

		try {
			LDASamplerWithCallback dwc = (LDASamplerWithCallback) modelClass.getDeclaredConstructor(argumentTypes)
					.newInstance(config);
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
