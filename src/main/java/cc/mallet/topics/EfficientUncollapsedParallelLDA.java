package cc.mallet.topics;

import java.util.concurrent.ThreadLocalRandom;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.LabelSequence;


public class EfficientUncollapsedParallelLDA extends UncollapsedParallelLDA implements LDAGibbsSampler{

	private static final long serialVersionUID = 1L;

	public EfficientUncollapsedParallelLDA(LDAConfiguration config) {
		super(config);
	}

	@Override
	protected LDADocSamplingResult sampleTopicAssignmentsParallel(LDADocSamplingContext ctx) {
		FeatureSequence tokens = ctx.getTokens();
		LabelSequence topics = ctx.getTopics();
		int myBatch = ctx.getMyBatch();
		
		int type, oldTopic, newTopic;

		final int docLength = tokens.getLength();
		if(docLength==0) return null;
		
		int [] tokenSequence = tokens.getFeatures();
		int [] oneDocTopics = topics.getFeatures();
		
		int[] localTopicCounts = new int[numTopics];
		
		// TODO: This must be wrong 
//		// With a uniform alpha, 'fill' can be used
//		//Arrays.fill(localTopicCounts, alpha);
//		// With non uniform alpha, it should be filled accordingly
//		for (int i = 0; i < localTopicCounts.length; i++) {
//			localTopicCounts[i] = alpha[i];
//		}
		
		// Find the non-zero words and topic counts that we have in this document
		for (int position = 0; position < docLength; position++) {
			int topicInd = oneDocTopics[position];
			localTopicCounts[topicInd]++;
		}

		double score, sum;
		double[] topicTermScores = new double[numTopics];

		//	Iterate over the words in the document
		for (int position = 0; position < docLength; position++) {
			type = tokenSequence[position];
			oldTopic = oneDocTopics[position];
			localTopicCounts[oldTopic]--;
			if(localTopicCounts[oldTopic]<0) throw new IllegalStateException("Counts cannot be negative! Count for topic:" + oldTopic + " is: " + localTopicCounts[oldTopic]);

			// Propagates the update to the topic-token assignments
			/**
			 * Used to subtract and add 1 to the local structure containing the number of times
			 * each token is assigned to a certain topic. Called before and after taking a sample
			 * topic assignment z
			 */
			decrement(myBatch, oldTopic, type);
			//System.out.println("(Batch=" + myBatch + ") Decremented: topic=" + oldTopic + " type=" + type + " => " + batchLocalTopicUpdates[myBatch][oldTopic][type]);

			// Now calculate and add up the scores for each topic for this word
			sum = 0.0;

			for (int topic = 0; topic < numTopics; topic++) {
				//score = (localTopicCounts[topic] + alpha) *
				// The "+ alpha" is already done by the "Arrays.fill" 
				score = localTopicCounts[topic] * phi[topic][type];
				topicTermScores[topic] = score;
				sum += score;
			}

			// Choose a random point between 0 and the sum of all topic scores
			// The thread local random performs better in concurrent situations 
			// than the standard random which is thread safe and incurs lock 
			// contention
			double U = ThreadLocalRandom.current().nextDouble();
			double sample = U * sum;

			// Figure out which topic contains that point
			if(U<0.5) {
				newTopic = -1;
				while (sample > 0.0) {
					newTopic++;
					sample -= topicTermScores[newTopic];
				} 
			} else {
				sample = sum - sample;
				newTopic = localTopicCounts.length;
				while (sample > 0.0) {
					newTopic--;
					sample -= topicTermScores[newTopic];
				} 				
			}

			// Make sure we actually sampled a valid topic
			if (newTopic < 0 || newTopic > numTopics) {
				throw new IllegalStateException ("EfficientUncollapsedParallelLDA: New valid topic not sampled.");
			}

			// Put that new topic into the counts
			oneDocTopics[position] = newTopic;
			localTopicCounts[newTopic]++;
			// Propagates the update to the topic-token assignments
			/**
			 * Used to subtract and add 1 to the local structure containing the number of times
			 * each token is assigned to a certain topic. Called before and after taking a sample
			 * topic assignment z
			 */
			increment(myBatch, newTopic, type);
			//System.out.println("(Batch=" + myBatch + ") Incremented: topic=" + newTopic + " type=" + type + " => " + batchLocalTopicUpdates[myBatch][newTopic][type]);		
		}
		return new LDADocSamplingResultDense(localTopicCounts);
	}

}
