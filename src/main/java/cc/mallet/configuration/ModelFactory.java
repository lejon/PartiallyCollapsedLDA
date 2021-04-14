package cc.mallet.configuration;

import java.lang.reflect.InvocationTargetException;

import cc.mallet.topics.LDAGibbsSampler;
import cc.mallet.topics.LDASamplerWithCallback;
import cc.mallet.topics.tui.IterationListener;

public class ModelFactory {
	
	public static final String ADLDA_MODEL = "adlda";
	public static final String UNCOLLAPSED_MODEL = "uncollapsed";
	public static final String COLLAPSED_MODEL = "collapsed";
	public static final String LIGHT_COLLAPSED_MODEL = "lightcollapsed";
	public static final String EFFICIENT_UNCOLLAPSED_MODEL = "efficient_uncollapsed";
	public static final String SPALIAS_MODEL = "spalias";
	public static final String POLYAURN_MODEL =  "polyaurn";
	public static final String POLYAURN_PRIORS_MODEL =  "polyaurn_priors";
	public static final String PPU_HLDA_MODEL =  "ppu_hlda";
	public static final String PPU_HDPLDA_MODEL =  "ppu_hdplda";
	public static final String PPU_HDP_ALL_TOPICS_MODEL =  "ppu_hdplda_all_topics";
	public static final String SPALIAS_PRIORS_MODEL =  "spalias_priors";
	public static final String LIGHTPCLDA_MODEL =  "lightpclda";
	public static final String LIGHTPCLDA_PROPOSAL_MODEL =  "lightpclda_proposal";
	public static final String NZVSSPALIAS_MODEL =  "nzvsspalias";
	public static final String DEFAULT_MODEL =  POLYAURN_MODEL;
	
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
	public static synchronized IterationListener getIterationCallback(LDAConfiguration config) {
		String model_name = config.getIterationCallbackClass(LDAConfiguration.MODEL_CALLBACK_DEFAULT);
		if(model_name == null) return null;
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
			return (IterationListener) modelClass.getDeclaredConstructor(argumentTypes)
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
	
	public static LDAGibbsSampler createModel(LDAConfiguration config, String whichModel) {
		LDAGibbsSampler model;
		switch(whichModel) {
		case ADLDA_MODEL: {
			model = ModelFactory.get(config, "cc.mallet.topics.ADLDA");
			System.out.println("ADLDA.");
			break;
		}
		case UNCOLLAPSED_MODEL: {
			model = ModelFactory.get(config, "cc.mallet.topics.UncollapsedParallelLDA");
			System.out.println("Uncollapsed Parallell LDA.");
			break;
		}
		case COLLAPSED_MODEL: {
			model = ModelFactory.get(config, "cc.mallet.topics.SerialCollapsedLDA");
			System.out.println("Uncollapsed Parallell LDA.");
			break;
		}
		case LIGHT_COLLAPSED_MODEL: {
			model = ModelFactory.get(config, "cc.mallet.topics.CollapsedLightLDA");
			System.out.println("CollapsedLightLDA Parallell LDA.");
			break;
		}
		case EFFICIENT_UNCOLLAPSED_MODEL: {
			model = ModelFactory.get(config, "cc.mallet.topics.EfficientUncollapsedParallelLDA");
			System.out.println("EfficientUncollapsedParallelLDA Parallell LDA.");
			break;
		}
		case SPALIAS_MODEL: {
			model = ModelFactory.get(config, "cc.mallet.topics.SpaliasUncollapsedParallelLDA");
			System.out.println("SpaliasUncollapsed Parallell LDA.");
			break;
		}
		case POLYAURN_MODEL: {
			model = ModelFactory.get(config, "cc.mallet.topics.PolyaUrnSpaliasLDA");
			System.out.println("PolyaUrnSpaliasLDA Parallell LDA.");
			break;
		}
		case POLYAURN_PRIORS_MODEL: {
			model = ModelFactory.get(config, "cc.mallet.topics.PolyaUrnSpaliasLDAWithPriors");
			System.out.println("PolyaUrnSpaliasLDA Parallell LDA With Priors.");
			break;
		}
		case PPU_HLDA_MODEL: {
			throw new IllegalStateException("ppu_hlda: using PoissonPolyaUrnHLDA is not verified to be working, won't run");
			// 			model = new PoissonPolyaUrnHLDA(config);
			// 			model = ModelFactory.get(config, "cc.mallet.topics.PolyaUrnSpaliasLDA");
			//			System.out.println("PoissonPolyaUrnHLDA Parallell HDP.");
			// 			break;
		}
		case PPU_HDPLDA_MODEL: {
			throw new IllegalStateException("ppu_hdplda: using PoissonPolyaUrnHDPLDA is not verified to be working, won't run");
			// 			model = new PoissonPolyaUrnHDPLDA(config);
			//			System.out.println("PoissonPolyaUrnHDPLDA Parallell HDP.");
			// 			break;
		}
		case PPU_HDP_ALL_TOPICS_MODEL: {
			model = ModelFactory.get(config, "cc.mallet.topics.PoissonPolyaUrnHDPLDAInfiniteTopics");
			System.out.println("PoissonPolyaUrnHDPLDAInfiniteTopics Parallell HDP.");
			break;
		}
		case SPALIAS_PRIORS_MODEL: {
			model = ModelFactory.get(config, "cc.mallet.topics.SpaliasUncollapsedParallelWithPriors");
			System.out.println("SpaliasUncollapsed Parallell LDA with Priors.");
			break;
		}
		case LIGHTPCLDA_MODEL: {
			model = ModelFactory.get(config, "cc.mallet.topics.LightPCLDA");
			System.out.println("Light PC LDA.");
			break;
		}
		case LIGHTPCLDA_PROPOSAL_MODEL: {
			model = ModelFactory.get(config, "cc.mallet.topics.LightPCLDAtypeTopicProposal");
			System.out.println("Light PC LDA with proposal 2.");
			break;
		}
		case NZVSSPALIAS_MODEL: {
			model = ModelFactory.get(config, "cc.mallet.topics.NZVSSpaliasUncollapsedParallelLDA");
			System.out.println("NZVSSpaliasUncollapsedParallelLDA Parallell LDA.");
			break;
		}
		default : {
			System.out.println("Invalid model type. Aborting");
			return null;
		}
		}
		return model;
	}
}
