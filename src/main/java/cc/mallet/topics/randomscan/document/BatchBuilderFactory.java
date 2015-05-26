package cc.mallet.topics.randomscan.document;

import java.lang.reflect.InvocationTargetException;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.topics.LDAGibbsSampler;

public class BatchBuilderFactory {
	
	public static final String EVEN_SPLIT       = "cc.mallet.topics.randomscan.document.EvenSplitBatchBuilder";
	public static final String PERCENTAGE_SPLIT = "cc.mallet.topics.randomscan.document.PercentageBatchBuilder";
	public static final String ADAPTIVE_SPLIT   = "cc.mallet.topics.randomscan.document.AdaptiveBatchBuilder";
	public static final String FIXED_SPLIT      = "cc.mallet.topics.randomscan.document.FixedSplitBatchBuilder";

	public BatchBuilderFactory() {
	}

	@SuppressWarnings("unchecked")
	public static synchronized DocumentBatchBuilder get(LDAConfiguration config, LDAGibbsSampler sampler) {
		String topic_building_scheme = config.getDocumentBatchBuildingScheme(LDAConfiguration.BATCH_BUILD_SCHEME_DEFAULT);
		
		@SuppressWarnings("rawtypes")
		Class batchBuilderClass = null;
		try {
			batchBuilderClass = Class.forName(topic_building_scheme);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e);
		}

		@SuppressWarnings("rawtypes")
		Class[] argumentTypes = new Class[2];
		argumentTypes[0] = LDAConfiguration.class; 
		argumentTypes[1] = LDAGibbsSampler.class;
				
		try {
			return (DocumentBatchBuilder) batchBuilderClass.getDeclaredConstructor(argumentTypes)
					.newInstance(config,sampler);
		} catch (InstantiationException | IllegalAccessException
				| InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e);
		}
	}
}
