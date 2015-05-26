package cc.mallet.topics.randomscan.topic;

import java.lang.reflect.InvocationTargetException;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.topics.LDAGibbsSampler;

public class MetaTopicIndexBuilder implements TopicIndexBuilder {

	LDAConfiguration config;
	LDAGibbsSampler sampler; 
	int instabilityPeriod = 0;
	int fullPhiPeriod;
	int builderIdx = 0;
	TopicIndexBuilder [] builders;

	public MetaTopicIndexBuilder(LDAConfiguration config, LDAGibbsSampler sampler) {
		this.config = config;
		this.sampler = sampler;
		instabilityPeriod = config.getInstabilityPeriod(0); 
		fullPhiPeriod = config.getFullPhiPeriod(-1);
		String [] subbuilders = config.getSubTopicIndexBuilders(-1);
		initBuilders(subbuilders);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void initBuilders(String[] subbuilders) {
		builders = new TopicIndexBuilder[subbuilders.length];
		int cnt = 0;
		for (String builderName : subbuilders) {
			
		Class topicIndexBuilderClass = null;
		try {
			topicIndexBuilderClass = Class.forName(builderName);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e);
		}

		Class[] argumentTypes = new Class[2];
		argumentTypes[0] = LDAConfiguration.class; 
		argumentTypes[1] = LDAGibbsSampler.class;
				
		try {
			builders[cnt++] = (TopicIndexBuilder) topicIndexBuilderClass.getDeclaredConstructor(argumentTypes)
					.newInstance(config,sampler);
		} catch (InstantiationException | IllegalAccessException
				| InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e);
		}
		}
	}

	/**
	 * Loop over the different builders and call them one by one
	 */
	@Override
	public int[][] getTopicTypeIndices() {
		// If we are in the instable period, sample everything (null means everything)
		int currentIteration = sampler.getCurrentIteration();
		if(currentIteration<instabilityPeriod) return null;

		int[][] topicTypeIndices = builders[builderIdx].getTopicTypeIndices();
		builderIdx = (builderIdx+1) % builders.length;
		return topicTypeIndices;
	}
}
