package cc.mallet.topics;

import java.util.List;
import java.util.concurrent.Future;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.types.InstanceList;

public class ParanoidCollapsedLightLDA extends CollapsedLightLDA {

	private static final long	serialVersionUID	= 6948198361119397002L;
	
	public ParanoidCollapsedLightLDA(LDAConfiguration config) {
		super(config);
	}
	
	@Override
	public void addInstances(InstanceList training) {
		super.addInstances(training);
		ensureConsistentTopicTypeCounts(typeTopicCounts);
		debugPrintMMatrix();
	}

	@Override
	protected void updateCounts(List<Future<BatchDocumentSamplerResult>> futureResults) throws InterruptedException {
		super.updateCounts(futureResults);
		ensureConsistentTopicTypeCounts(typeTopicCounts);
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
	protected void sampleTopicAssignmentsParallel(LDADocSamplingContext ctxIn)  {
		//SamplingResult res = super.sampleTopicAssignmentsParallel(tokenSequence, oneDocTopics, myBatch);
		LightLDADocSamplingContext ctx = (LightLDADocSamplingContext) ctxIn;
		int [][] globalTypeTopicCounts = ctx.getMyTypeTopicCounts();
		super.sampleTopicAssignmentsParallel(ctx);
		ensureConsistentTopicTypeCounts(globalTypeTopicCounts);
		//System.out.println("Glboal type topic count is consistent!");
		//ensureConsistentTopicTypeCountDelta(batchLocalTopicTypeUpdates, ctx.getMyBatch());
	}


}

