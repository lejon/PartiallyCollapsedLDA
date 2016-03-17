package cc.mallet.topics;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import cc.mallet.types.FeatureSequence;
import cc.mallet.types.LabelSequence;
import cc.mallet.util.Randoms;

public class LightLDAWorkerRunnable extends WorkerRunnable {
	
	double kdDensity  = 0.0;

	public double getKdDensity() {
		return kdDensity;
	}

	public void setKdDensity(double kdDensity) {
		this.kdDensity = kdDensity;
	}

	public LightLDAWorkerRunnable(int numTopics, double[] alpha, double alphaSum,
			double beta, Randoms random, ArrayList<TopicAssignment> data,
			int[][] typeTopicCounts, int[] tokensPerTopic, int startDoc,
			int numDocs) {
		super(numTopics, alpha, alphaSum, beta, random, data, typeTopicCounts,
				tokensPerTopic, startDoc, numDocs);
	}
	
	public boolean isFinished() {
		return isFinished;
	}
	
	protected void sampleTopicsForOneDoc (FeatureSequence tokenSequence,
			  FeatureSequence topicSequence,
			  boolean readjustTopicsAndStats /* currently ignored */) {
		int[] oneDocTopics = topicSequence.getFeatures();
		
		int[] currentTypeTopicCounts;
		int type, oldTopic, newTopic;
		double topicWeightsSum;
		int docLength = tokenSequence.getLength();
		
		int[] localTopicCounts = new int[numTopics];
		int[] localTopicIndex = new int[numTopics];
		
		//		populate topic counts
		for (int position = 0; position < docLength; position++) {
			if (oneDocTopics[position] == ParallelTopicModel.UNASSIGNED_TOPIC) { continue; }
			localTopicCounts[oneDocTopics[position]]++;
		}
		
		// Build an array that densely lists the topics that
		//  have non-zero counts.
		int denseIndex = 0;
		for (int topic = 0; topic < numTopics; topic++) {
			if (localTopicCounts[topic] != 0) {
				localTopicIndex[denseIndex] = topic;
				denseIndex++;
			}
		}
		
		// Record the total number of non-zero topics
		int nonZeroTopics = denseIndex;
		kdDensity += nonZeroTopics;
		
		//		Initialize the topic count/beta sampling bucket
		double topicBetaMass = 0.0;
		
		// Initialize cached coefficients and the topic/beta 
		//  normalizing constant.
		
		for (denseIndex = 0; denseIndex < nonZeroTopics; denseIndex++) {
			int topic = localTopicIndex[denseIndex];
			int n = localTopicCounts[topic];
		
			//	initialize the normalization constant for the (B * n_{t|d}) term
			topicBetaMass += beta * n /	(tokensPerTopic[topic] + betaSum);	
		
			//	update the coefficients for the non-zero topics
			cachedCoefficients[topic] =	(alpha[topic] + n) / (tokensPerTopic[topic] + betaSum);
		}
		
		double topicTermMass = 0.0;
		
		double[] topicTermScores = new double[numTopics];
		int[] topicTermIndices;
		int[] topicTermValues;
		int i;
		double score;
		
		//	Iterate over the positions (words) in the document 
		for (int position = 0; position < docLength; position++) {
			type = tokenSequence.getIndexAtPosition(position);
			oldTopic = oneDocTopics[position];
		
			currentTypeTopicCounts = typeTopicCounts[type];
		
			if (oldTopic != ParallelTopicModel.UNASSIGNED_TOPIC) {
				//	Remove this token from all counts. 
				
				// Remove this topic's contribution to the 
				//  normalizing constants
				smoothingOnlyMass -= alpha[oldTopic] * beta / 
					(tokensPerTopic[oldTopic] + betaSum);
				topicBetaMass -= beta * localTopicCounts[oldTopic] /
					(tokensPerTopic[oldTopic] + betaSum);
				
				// Decrement the local doc/topic counts
				
				localTopicCounts[oldTopic]--;
				
				// Maintain the dense index, if we are deleting
				//  the old topic
				if (localTopicCounts[oldTopic] == 0) {
					
					// First get to the dense location associated with
					//  the old topic.
					
					denseIndex = 0;
					
					// We know it's in there somewhere, so we don't 
					//  need bounds checking.
					while (localTopicIndex[denseIndex] != oldTopic) {
						denseIndex++;
					}
				
					// shift all remaining dense indices to the left.
					while (denseIndex < nonZeroTopics) {
						if (denseIndex < localTopicIndex.length - 1) {
							localTopicIndex[denseIndex] = 
								localTopicIndex[denseIndex + 1];
						}
						denseIndex++;
					}
					
					nonZeroTopics --;
				}
		
				// Decrement the global topic count totals
				tokensPerTopic[oldTopic]--;
				assert(tokensPerTopic[oldTopic] >= 0) : "old Topic " + oldTopic + " below 0";
			
		
				// Add the old topic's contribution back into the
				//  normalizing constants.
				smoothingOnlyMass += alpha[oldTopic] * beta / 
					(tokensPerTopic[oldTopic] + betaSum);
				topicBetaMass += beta * localTopicCounts[oldTopic] /
					(tokensPerTopic[oldTopic] + betaSum);
		
				// Reset the cached coefficient for this topic
				cachedCoefficients[oldTopic] = 
					(alpha[oldTopic] + localTopicCounts[oldTopic]) /
					(tokensPerTopic[oldTopic] + betaSum);
			}
		
		
			// Now go over the type/topic counts, decrementing
			//  where appropriate, and calculating the score
			//  for each topic at the same time.
		
			int index = 0;
			int currentTopic, currentValue;
		
			boolean alreadyDecremented = (oldTopic == ParallelTopicModel.UNASSIGNED_TOPIC);
		
			topicTermMass = 0.0;
		
			while (index < currentTypeTopicCounts.length && 
				   currentTypeTopicCounts[index] > 0) {
				currentTopic = currentTypeTopicCounts[index] & topicMask;
				currentValue = currentTypeTopicCounts[index] >> topicBits;
		
				if (! alreadyDecremented && 
					currentTopic == oldTopic) {
		
					// We're decrementing and adding up the 
					//  sampling weights at the same time, but
					//  decrementing may require us to reorder
					//  the topics, so after we're done here,
					//  look at this cell in the array again.
		
					currentValue --;
					if (currentValue == 0) {
						currentTypeTopicCounts[index] = 0;
					}
					else {
						currentTypeTopicCounts[index] =
							(currentValue << topicBits) + oldTopic;
					}
					
					// Shift the reduced value to the right, if necessary.
		
					int subIndex = index;
					while (subIndex < currentTypeTopicCounts.length - 1 && 
						   currentTypeTopicCounts[subIndex] < currentTypeTopicCounts[subIndex + 1]) {
						int temp = currentTypeTopicCounts[subIndex];
						currentTypeTopicCounts[subIndex] = currentTypeTopicCounts[subIndex + 1];
						currentTypeTopicCounts[subIndex + 1] = temp;
						
						subIndex++;
					}
		
					alreadyDecremented = true;
				}
				else {
					score = 
						cachedCoefficients[currentTopic] * currentValue;
					topicTermMass += score;
					topicTermScores[index] = score;
		
					index++;
				}
			}
			
			double sample = random.nextUniform() * (smoothingOnlyMass + topicBetaMass + topicTermMass);
			double origSample = sample;
		
			//	Make sure it actually gets set
			newTopic = -1;
		
			if (sample < topicTermMass) {
				//topicTermCount++;
		
				i = -1;
				while (sample > 0) {
					i++;
					sample -= topicTermScores[i];
				}
		
				newTopic = currentTypeTopicCounts[i] & topicMask;
				currentValue = currentTypeTopicCounts[i] >> topicBits;
				
				currentTypeTopicCounts[i] = ((currentValue + 1) << topicBits) + newTopic;
		
				// Bubble the new value up, if necessary
				
				while (i > 0 &&
					   currentTypeTopicCounts[i] > currentTypeTopicCounts[i - 1]) {
					int temp = currentTypeTopicCounts[i];
					currentTypeTopicCounts[i] = currentTypeTopicCounts[i - 1];
					currentTypeTopicCounts[i - 1] = temp;
		
					i--;
				}
		
			}
			else {
				sample -= topicTermMass;
		
				if (sample < topicBetaMass) {
					//betaTopicCount++;
		
					sample /= beta;
		
					for (denseIndex = 0; denseIndex < nonZeroTopics; denseIndex++) {
						int topic = localTopicIndex[denseIndex];
		
						sample -= localTopicCounts[topic] /
							(tokensPerTopic[topic] + betaSum);
		
						if (sample <= 0.0) {
							newTopic = topic;
							break;
						}
					}
		
				}
				else {
					//smoothingOnlyCount++;
		
					sample -= topicBetaMass;
		
					sample /= beta;
		
					newTopic = 0;
					sample -= alpha[newTopic] /
						(tokensPerTopic[newTopic] + betaSum);
		
					while (sample > 0.0) {
						newTopic++;
						sample -= alpha[newTopic] / 
							(tokensPerTopic[newTopic] + betaSum);
					}
					
				}
		
				// Move to the position for the new topic,
				//  which may be the first empty position if this
				//  is a new topic for this word.
				
				index = 0;
				while (currentTypeTopicCounts[index] > 0 &&
					   (currentTypeTopicCounts[index] & topicMask) != newTopic) {
					index++;
					if (index == currentTypeTopicCounts.length) {
						System.err.println("type: " + type + " new topic: " + newTopic);
						for (int k=0; k<currentTypeTopicCounts.length; k++) {
							System.err.print((currentTypeTopicCounts[k] & topicMask) + ":" + 
											 (currentTypeTopicCounts[k] >> topicBits) + " ");
						}
						System.err.println();
		
					}
				}
		
		
				// index should now be set to the position of the new topic,
				//  which may be an empty cell at the end of the list.
		
				if (currentTypeTopicCounts[index] == 0) {
					// inserting a new topic, guaranteed to be in
					//  order w.r.t. count, if not topic.
					currentTypeTopicCounts[index] = (1 << topicBits) + newTopic;
				}
				else {
					currentValue = currentTypeTopicCounts[index] >> topicBits;
					currentTypeTopicCounts[index] = ((currentValue + 1) << topicBits) + newTopic;
		
					// Bubble the increased value left, if necessary
					while (index > 0 &&
						   currentTypeTopicCounts[index] > currentTypeTopicCounts[index - 1]) {
						int temp = currentTypeTopicCounts[index];
						currentTypeTopicCounts[index] = currentTypeTopicCounts[index - 1];
						currentTypeTopicCounts[index - 1] = temp;
		
						index--;
					}
				}
		
			}
		
			if (newTopic == -1) {
				System.err.println("WorkerRunnable sampling error: "+ origSample + " " + sample + " " + smoothingOnlyMass + " " + 
						topicBetaMass + " " + topicTermMass);
				newTopic = numTopics-1; // TODO is this appropriate
				//throw new IllegalStateException ("WorkerRunnable: New topic not sampled.");
			}
			//assert(newTopic != -1);
		
			//			Put that new topic into the counts
			oneDocTopics[position] = newTopic;
		
			smoothingOnlyMass -= alpha[newTopic] * beta / 
				(tokensPerTopic[newTopic] + betaSum);
			topicBetaMass -= beta * localTopicCounts[newTopic] /
				(tokensPerTopic[newTopic] + betaSum);
		
			localTopicCounts[newTopic]++;
		
			// If this is a new topic for this document,
			//  add the topic to the dense index.
			if (localTopicCounts[newTopic] == 1) {
				
				// First find the point where we 
				//  should insert the new topic by going to
				//  the end (which is the only reason we're keeping
				//  track of the number of non-zero
				//  topics) and working backwards
		
				denseIndex = nonZeroTopics;
		
				while (denseIndex > 0 &&
					   localTopicIndex[denseIndex - 1] > newTopic) {
		
					localTopicIndex[denseIndex] =
						localTopicIndex[denseIndex - 1];
					denseIndex--;
				}
				
				localTopicIndex[denseIndex] = newTopic;
				nonZeroTopics++;
			}
		
			tokensPerTopic[newTopic]++;
		
			//	update the coefficients for the non-zero topics
			cachedCoefficients[newTopic] =
				(alpha[newTopic] + localTopicCounts[newTopic]) /
				(tokensPerTopic[newTopic] + betaSum);
		
			smoothingOnlyMass += alpha[newTopic] * beta / 
				(tokensPerTopic[newTopic] + betaSum);
			topicBetaMass += beta * localTopicCounts[newTopic] /
				(tokensPerTopic[newTopic] + betaSum);
		
		}
		
		if (shouldSaveState) {
			// Update the document-topic count histogram,
			//  for dirichlet estimation
			docLengthCounts[ docLength ]++;
		
			for (denseIndex = 0; denseIndex < nonZeroTopics; denseIndex++) {
				int topic = localTopicIndex[denseIndex];
				
				topicDocCounts[topic][ localTopicCounts[topic] ]++;
			}
		}
		
		//	Clean up our mess: reset the coefficients to values with only
		//	smoothing. The next doc will update its own non-zero topics...
		
		for (denseIndex = 0; denseIndex < nonZeroTopics; denseIndex++) {
			int topic = localTopicIndex[denseIndex];
		
			cachedCoefficients[topic] =
				alpha[topic] / (tokensPerTopic[topic] + betaSum);
		}
	}
	
	protected void sampleTopicAssignmentsParallel(LDADocSamplingContext ctx) {
		FeatureSequence tokens = ctx.getTokens();
		LabelSequence topics = ctx.getTopics();
		int myBatch = ctx.getMyBatch();

		final int docLength = tokens.getLength();
		if(docLength==0) return;
		
		int [] tokenSequence = tokens.getFeatures();
		int [] oneDocTopics = topics.getFeatures();

		double[] localTopicCounts = new double[numTopics];
		double[] localTopicCounts_i = new double[numTopics];
		
		// Populate topic counts
		int nonZeroTopicCnt = 0; // Only needed for statistics
		for (int position = 0; position < docLength; position++) {
			int topicInd = oneDocTopics[position];
			localTopicCounts[topicInd]++;
			localTopicCounts_i[topicInd]++;
			if(localTopicCounts[topicInd]==1) nonZeroTopicCnt++;
		}
		
		//	Iterate over the words in the document
		for (int position = 0; position < docLength; position++) {
			int type = tokenSequence[position];
			int oldTopic = oneDocTopics[position]; // z_position
			int newTopic = oldTopic;
			
			if(localTopicCounts[oldTopic]<0) 
				throw new IllegalStateException("LightPC-LDA: Counts cannot be negative! Count for topic:" 
						+ oldTopic + " is: " + localTopicCounts[oldTopic]);

			// #####################################
			// Word Topic Distribution 
			// #####################################
			
			// N_{d,Z_i} => # counts of topic Z_i in document d
			
			localTopicCounts_i[oldTopic]--;
			
			double u = ThreadLocalRandom.current().nextDouble();
			int wordTopicIndicatorProposal = aliasTables[type].generateSample(u);
			
			// If we drew a new topic indicator, do MH step for Word proposal
			if(wordTopicIndicatorProposal!=oldTopic) {
				double n_d_zi_i = localTopicCounts_i[oldTopic];
				double n_d_zstar_i = localTopicCounts_i[wordTopicIndicatorProposal];
				double pi_w = (alpha + n_d_zstar_i) / (alpha + n_d_zi_i);
				if(pi_w > 1){
					localTopicCounts[oldTopic]--;
					localTopicCounts[wordTopicIndicatorProposal]++;
					oldTopic = wordTopicIndicatorProposal;
				} else {
					double u_pi_w = ThreadLocalRandom.current().nextDouble();
					boolean accept_pi_w = u_pi_w < pi_w;

					if(accept_pi_w) {				
						localTopicCounts[oldTopic]--;
						localTopicCounts[wordTopicIndicatorProposal]++;
	
						// Set oldTopic to the new wordTopicIndicatorProposal just accepted.
						// By doing this the below document proposal will be relative to the 
						// new best proposal so far
						oldTopic = wordTopicIndicatorProposal;
						//wordAccepts.incrementAndGet();
					} 
				}

			}
			
			// #####################################
			// Document Topic Distribution 
			// #####################################
			 
			double u_i = ThreadLocalRandom.current().nextDouble() * (oneDocTopics.length + (numTopics*alpha));
			
			int docTopicIndicatorProposal = -1;
			if(u_i < oneDocTopics.length) {
				docTopicIndicatorProposal = oneDocTopics[(int) u_i];
			} else {
				docTopicIndicatorProposal = (int) (((u_i - oneDocTopics.length) / (numTopics*alpha)) * numTopics);
			}
			
			// Make sure we actually sampled a valid topic
			if (docTopicIndicatorProposal < 0 || docTopicIndicatorProposal > numTopics) {
				throw new IllegalStateException ("Collapsed LightPC-LDA: New valid topic not sampled (" + newTopic + ").");
			}

			// If we drew a new topic indicator, do MH step for Document proposal
			if(docTopicIndicatorProposal!=oldTopic) {
				double n_d_zstar_i = localTopicCounts_i[docTopicIndicatorProposal];
				double n_d_zi_i = localTopicCounts_i[oldTopic];
				double n_d_zi = localTopicCounts[oldTopic];
				double n_d_zstar = localTopicCounts[docTopicIndicatorProposal];

				double nom = typeTopicCounts[type][docTopicIndicatorProposal] * (alpha + n_d_zstar_i) * (alpha + n_d_zi);
				double denom = typeTopicCounts[type][oldTopic] * (alpha + n_d_zi_i) * (alpha + n_d_zstar);
				double ratio = nom / denom;
				// Calculate MH acceptance Min.(1,ratio) but as an if else
				if (ratio > 1){
					newTopic = docTopicIndicatorProposal;
				} else {
					double pi_d = ratio;
					double u_pi_d = ThreadLocalRandom.current().nextDouble();
					boolean accept_pi_d = u_pi_d < pi_d;
	
					if (accept_pi_d) {
						newTopic = docTopicIndicatorProposal;
						//docAccepts.incrementAndGet();
					} else {
						// We did not accept either word or document proposal 
						// so oldTopic is still the best indicator
						newTopic = oldTopic;
						//oldAccepts.incrementAndGet();
					}	
				}
			}

			// Make sure we actually sampled a valid topic
			if (newTopic < 0 || newTopic > numTopics) {
				throw new IllegalStateException ("LightPC-LDA: New valid topic not sampled (" + newTopic + ").");
			}

			// Remove one count from old topic
			localTopicCounts[oldTopic]--;
			// Update the word topic indicator
			oneDocTopics[position] = newTopic;
			// Put that new topic into the counts
			localTopicCounts[newTopic]++;
			// Make sure the "_i" version is also up to date!
			localTopicCounts_i[newTopic]++;
		}
	}

}
