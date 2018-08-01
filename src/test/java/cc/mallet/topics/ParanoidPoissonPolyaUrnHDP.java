package cc.mallet.topics;

import cc.mallet.configuration.LDAConfiguration;

public class ParanoidPoissonPolyaUrnHDP extends PoissonPolyaUrnHLDA {

	private static final long serialVersionUID = 1L;

	public ParanoidPoissonPolyaUrnHDP(LDAConfiguration config) {
		super(config);
	}

	@Override
	public void postIteration() {
		super.postIteration();
		for (int topic = numTopics; topic < maxTopics; topic++) {
			if(tokensPerTopic[topic]>0) {
				throw new IllegalArgumentException("Topic count: " + topic + " has value > 0 for " + tokensPerTopic[topic] + ". numTopics:" + numTopics);
			}
			for(int type = 0; type < numTypes; type++) {
				if(topicTypeCountMapping[topic][type]>0) {
					throw new IllegalArgumentException("Topic: " + topic + " has value > 0 for " + topicTypeCountMapping[topic][type] + ". numTopics:" + numTopics);
				}
			}
		}

		
	}

}
