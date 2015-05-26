package cc.mallet.topics;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelSequence;

public class ParanoidVSSpaliasUncollapsedLDA extends NZVSSpaliasUncollapsedParallelLDA {

	private static final long	serialVersionUID	= 6948198361119397002L;

	boolean silent = false;
	
	public ParanoidVSSpaliasUncollapsedLDA(LDAConfiguration config) {
		super(config);
	}
	
	@Override
	protected void samplePhi() {
		super.samplePhi();
		ensureConsistentPhi(phi);
		ensureConsistentTopicTypeCounts(topicTypeCountMapping);
		if(!silent) System.out.println("Phi is consistent after sampling!");
		if(!silent) System.out.println("Topic count is consistent after sampling!");
		debugPrintMMatrix();
	}

	@Override
	public void addInstances(InstanceList training) {
		super.addInstances(training);
		//ensureConsistentTopicTypeCounts(topicTypeCounts);
		ensureConsistentPhi(phi);
		ensureConsistentTopicTypeCounts(topicTypeCountMapping);
		if(!silent) System.out.println("Phi is consistent after add instances!");
		if(!silent) System.out.println("Topic count is consistent after add instances!");
		debugPrintMMatrix();
	}

	@Override
	protected void updateCounts() throws InterruptedException {
		super.updateCounts();
		ensureConsistentPhi(phi);
		if(!silent) System.out.println("Phi is consistent after count update!");
		ensureConsistentTopicTypeCounts(topicTypeCountMapping);
		if(!silent) System.out.println("Topic count is consistent after count update!");
		debugPrintMMatrix();
	}
	
	

	@Override
	protected void postSample() {
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
	void sampleTopicAssignmentsParallel(FeatureSequence tokens, LabelSequence topics, int myBatch) {		//SamplingResult res = super.sampleTopicAssignmentsParallel(tokenSequence, oneDocTopics, myBatch);
		super.sampleTopicAssignmentsParallel(tokens, topics, myBatch);
		// THIS CANNOT BE ENSURED with a job stealing implementation
		//ensureConsistentTopicTypeCountDelta(batchLocalTopicTypeUpdates, myBatch);
		//return res;
	}


}

