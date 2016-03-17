package cc.mallet.topics;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import cc.mallet.types.FeatureSequence;
import cc.mallet.types.LabelSequence;
import cc.mallet.util.Randoms;
import cc.mallet.util.WalkerAliasTable;

public class LightLDAWorkerRunnable extends MyWorkerRunnable {
	
	WalkerAliasTable [] aliasTables; 

	public LightLDAWorkerRunnable(int numTopics, double[] alpha, double alphaSum,
			double beta, Randoms random, ArrayList<TopicAssignment> data,
			int[][] typeTopicCounts, int[] tokensPerTopic, int startDoc,
			int numDocs, WalkerAliasTable [] aliasTables) {
		super(numTopics, alpha, alphaSum, beta, random, data, typeTopicCounts,
				tokensPerTopic, startDoc, numDocs);
		this.aliasTables = aliasTables;
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
		
		// Cashed values
		// TODO: Is this the correct way?
		double beta_bar = beta * typeTopicCounts.length; // beta * V (beta_bar in article)
		
		//	Iterate over the words in the document
		for (int position = 0; position < docLength; position++) {
			int type = tokenSequence[position];
			int oldTopic = oneDocTopics[position]; // z_position
			int newTopic = oldTopic;
			
			if(localTopicCounts[oldTopic]<0) 
				throw new IllegalStateException("LightPC-LDA: Counts cannot be negative! Count for topic:" 
						+ oldTopic + " is: " + localTopicCounts[oldTopic]);

			// #####################################
			// Word-Topic Proposal 
			// #####################################
			
			// N_{d,Z_i} => # counts of topic Z_i in document d
			// s = old state
			// t = proposed state
			
			localTopicCounts_i[oldTopic]--;
			
			// Draw topic proposal t
			// TODO: I cant find an array that contain the number of tokens per word type? I set this to be called typeTokens, need to be fixed.
			double u_w = ThreadLocalRandom.current().nextDouble() * (typeTokens[type] + (numTopics*beta)); // (n_w + K*beta) * u where u ~ U(0,1)

			int wordTopicIndicatorProposal = -1;
			if(u_w < typeTokens[type]) {
				// TODO: Assuming that the Alias table is returning an index to use in backmapping
				wordTopicIndicatorProposal = nonZeroTypeTopicsBackMapping[type][aliasTables[type].generateSample(u_w)];
			} else {
				wordTopicIndicatorProposal = (int) (((u_w - typeTokens[type]) / (numTopics*beta)) * numTopics); // assume symmetric beta, just draws one topic
			}
						
			// Make sure we actually sampled a valid topic
			if (wordTopicIndicatorProposal < 0 || wordTopicIndicatorProposal > numTopics) {
				throw new IllegalStateException ("Collapsed Light-LDA: New valid topic not sampled (" + newTopic + ").");
			}
			
			
			if(wordTopicIndicatorProposal!=oldTopic) {
				// If we drew a new topic indicator, do MH step for Word proposal
				
				double n_d_s_i = localTopicCounts_i[oldTopic];
				double n_d_t_i = localTopicCounts_i[wordTopicIndicatorProposal];
				double n_w_s = typeTopicCounts[type][oldTopic];
				double n_w_t = typeTopicCounts[type][wordTopicIndicatorProposal];					
				double n_w_s_i = typeTopicCounts[type][oldTopic] - 1.0;
				double n_w_t_i = n_w_t; // Since wordTopicIndicatorProposal!=oldTopic above promise that s!=t and hence n_tw=n_t_i
				double n_t = tokensPerTopic[wordTopicIndicatorProposal]; // Global counts of the number of topic indicators in each topic
				double n_s = tokensPerTopic[oldTopic]; 
				double n_t_i = n_t; 
				double n_s_i = n_s - 1.0; 
				
				// Calculate rejection rate
				// TODO: Is this a correct way of calculate a large product?
				double pi_w = ((alpha + n_d_t_i) / (alpha + n_d_s_i));
				pi_w *= ((beta + n_w_t_i) / (beta + n_w_s_i));
				pi_w *= ((beta_bar + n_s_i) / (beta_bar + n_t_i));
				pi_w *= ((beta + n_w_s) / (beta + n_w_t));
				pi_w *= ((beta + n_t) / (beta + n_s));
				
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
			// Document-Topic Proposal  
			// #####################################
			 
			double u_i = ThreadLocalRandom.current().nextDouble() * (oneDocTopics.length + (numTopics*alpha)); // (n_d + K*alpha) * u where u ~ U(0,1)

			int docTopicIndicatorProposal = -1;
			if(u_i < oneDocTopics.length) {
				docTopicIndicatorProposal = oneDocTopics[(int) u_i];
			} else {
				docTopicIndicatorProposal = (int) (((u_i - oneDocTopics.length) / (numTopics*alpha)) * numTopics); // assume symmetric alpha, just draws one alpha
			}
			
			// Make sure we actually sampled a valid topic
			if (docTopicIndicatorProposal < 0 || docTopicIndicatorProposal > numTopics) {
				throw new IllegalStateException ("Collapsed Light-LDA: New valid topic not sampled (" + newTopic + ").");
			}

			// If we drew a new topic indicator, do MH step for Document proposal
			if(docTopicIndicatorProposal!=oldTopic) {
				double n_d_s = localTopicCounts[oldTopic];
				double n_d_t = localTopicCounts[wordTopicIndicatorProposal];
				double n_d_s_i = localTopicCounts_i[oldTopic];
				double n_d_t_i = localTopicCounts_i[wordTopicIndicatorProposal];
				double n_w_s_i = typeTopicCounts[type][oldTopic] - 1.0;
				double n_w_t_i = typeTopicCounts[type][wordTopicIndicatorProposal]; // Since wordTopicIndicatorProposal!=oldTopic above promise that s!=t and hence n_tw=n_t_i
				double n_t_i = tokensPerTopic[wordTopicIndicatorProposal]; // Global counts of the number of topic indicators in each topic
				double n_s_i = tokensPerTopic[oldTopic] - 1.0; 
				
				// Calculate rejection rate
				// TODO: Is this a correct way of calculate a large product?
				double pi_d = ((alpha + n_d_t_i) / (alpha + n_d_s_i));
				pi_d *= ((beta + n_w_t_i) / (beta + n_w_s_i));
				pi_d *= ((beta_bar + n_s_i) / (beta_bar + n_t_i));
				pi_d *= ((alpha + n_d_s) / (alpha + n_d_t));
				
				// Calculate MH acceptance Min.(1,ratio) but as an if else
				if (pi_d > 1){
					newTopic = docTopicIndicatorProposal;
				} else {
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
				throw new IllegalStateException ("Collapsed Light-LDA: New valid topic not sampled (" + newTopic + ").");
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
