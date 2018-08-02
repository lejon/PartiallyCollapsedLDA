package cc.mallet.topics;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.types.InstanceList;

public class ParanoidPoissonPolyaUrnHDP extends PoissonPolyaUrnHLDA {

	private static final long serialVersionUID = 1L;

	public ParanoidPoissonPolyaUrnHDP(LDAConfiguration config) {
		super(config);
	}
	
	@Override
	protected void samplePhi() {
		super.samplePhi();
		ensureConsistentPhi(phi);
		ensureConsistentTopicTypeCounts(topicTypeCountMapping, typeTopicCounts, tokensPerTopic);
		debugPrintMMatrix();
	}

	@Override
	public void addInstances(InstanceList training) {
		super.addInstances(training);
		//ensureConsistentTopicTypeCounts(topicTypeCounts);
		ensureConsistentPhi(phi);
		ensureConsistentTopicTypeCounts(topicTypeCountMapping, typeTopicCounts, tokensPerTopic);
		debugPrintMMatrix();
	}

	@Override
	protected void updateCounts() throws InterruptedException {
		super.updateCounts();
		ensureConsistentPhi(phi);
		ensureConsistentTopicTypeCounts(topicTypeCountMapping, typeTopicCounts, tokensPerTopic);
		debugPrintMMatrix();
	}
		
	@Override
	public void postSample() {
		super.postSample();
		int updateCountSum = 0;
		for (int batch = 0; batch < batchLocalTopicTypeUpdates.length; batch++) {
				for (int topic = 0; topic < numTopics; topic++) {
					for (int type = 0; type < numTypes; type++) {
					//updateCountSum += batchLocalTopicTypeUpdates[batch][topic][type];
					updateCountSum += batchLocalTopicTypeUpdates[topic][type].get();
				}
			}
			if(updateCountSum!=0) throw new IllegalStateException("Update count does not sum to zero: " + updateCountSum); 
			updateCountSum = 0;
		}
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
		
		ensureTTEquals();
	}

}
