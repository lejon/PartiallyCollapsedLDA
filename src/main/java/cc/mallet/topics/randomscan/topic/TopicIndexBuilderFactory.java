package cc.mallet.topics.randomscan.topic;

import java.lang.reflect.InvocationTargetException;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.topics.LDAGibbsSampler;

public class TopicIndexBuilderFactory {
	
	public static final String ALL                 = "cc.mallet.topics.randomscan.topic.AllWordsTopicIndexBuilder";
	public static final String ADAPTIVE_BETA_MIX   = "cc.mallet.topics.randomscan.topic.TopWordsRandomFractionTopicIndexBuilder";

	public TopicIndexBuilderFactory() {
	}

	@SuppressWarnings("unchecked")
	public static synchronized TopicIndexBuilder get(LDAConfiguration config, LDAGibbsSampler sampler) {
		String building_scheme = config.getTopicIndexBuildingScheme(LDAConfiguration.TOPIC_INDEX_BUILD_SCHEME_DEFAULT);
		
		@SuppressWarnings("rawtypes")
		Class topicIndexBuilderClass = null;
		try {
			topicIndexBuilderClass = Class.forName(building_scheme);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e);
		}

		@SuppressWarnings("rawtypes")
		Class[] argumentTypes = new Class[2];
		argumentTypes[0] = LDAConfiguration.class; 
		argumentTypes[1] = LDAGibbsSampler.class;
				
		try {
			return (TopicIndexBuilder) topicIndexBuilderClass.getDeclaredConstructor(argumentTypes)
					.newInstance(config,sampler);
		} catch (InstantiationException | IllegalAccessException
				| InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e);
		}
	}
}
