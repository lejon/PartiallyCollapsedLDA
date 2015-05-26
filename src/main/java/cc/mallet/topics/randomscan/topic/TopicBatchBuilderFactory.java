package cc.mallet.topics.randomscan.topic;

import java.lang.reflect.InvocationTargetException;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.topics.LDAGibbsSampler;

public class TopicBatchBuilderFactory {
	
	public static final String EVEN_SPLIT       = "cc.mallet.topics.randomscan.topic.EvenSplitTopicBatchBuilder";

	public TopicBatchBuilderFactory() {
	}

	@SuppressWarnings("unchecked")
	public static synchronized TopicBatchBuilder get(LDAConfiguration config, LDAGibbsSampler sampler) {
		String building_scheme = config.getTopicBatchBuildingScheme(LDAConfiguration.TOPIC_BATCH_BUILD_SCHEME_DEFAULT);
		
		@SuppressWarnings("rawtypes")
		Class batchBuilderClass = null;
		try {
			batchBuilderClass = Class.forName(building_scheme);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e);
		}

		@SuppressWarnings("rawtypes")
		Class[] argumentTypes = new Class[2];
		argumentTypes[0] = LDAConfiguration.class; 
		argumentTypes[1] = LDAGibbsSampler.class;
				
		try {
			return (TopicBatchBuilder) batchBuilderClass.getDeclaredConstructor(argumentTypes)
					.newInstance(config,sampler);
		} catch (InstantiationException | IllegalAccessException
				| InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e);
		}
	}
}
