package cc.mallet.topics;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.types.InstanceList;

public class ParanoidSpaliasUncollapsedLDA extends SpaliasUncollapsedParallelLDA {

	private static final long	serialVersionUID	= 6948198361119397002L;
	
	public ParanoidSpaliasUncollapsedLDA(LDAConfiguration config) {
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
	public void postIteration() {
		super.postIteration();
		ensureTTEquals();
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
	protected LDADocSamplingResult sampleTopicAssignmentsParallel(LDADocSamplingContext ctx)  {
		//SamplingResult res = super.sampleTopicAssignmentsParallel(tokenSequence, oneDocTopics, myBatch);
		return super.sampleTopicAssignmentsParallel(ctx);
		// THIS CANNOT BE ENSURED with a job stealing implementation
		//ensureConsistentTopicTypeCountDelta(batchLocalTopicTypeUpdates, myBatch);
		//return res;
	}


}

