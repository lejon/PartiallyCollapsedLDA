package cc.mallet.topics;

import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.LabelSequence;
import cc.mallet.util.OptimizedGentleAliasMethod;

/**
 * @author Leif Jonsson
 * 
 * Implementation of the Light-PC-LDA algorithm which uses Metropolis-Hastings to 
 * sample the topic indicators. This algorithm has O(1) to sample one topic
 * indicator no matter how many topics
 * 
 */
public class LightPCLDAw2 extends LightPCLDA {

	// TODO: Leif, should this be here?
	double [] topicCountBetaHat = new double[numTopics];
	
	public LightPCLDAw2(LDAConfiguration config) {
		super(config);
		tbFactory = new TTTableBuilderFactory();
		
	}
	
	class TTTableBuilderFactory implements TableBuilderFactory {
		public Callable<TableBuildResult> instance(int type) {
			return new TTParallelTableBuilder(type);
		}
	}

	class TTParallelTableBuilder implements Callable<TableBuildResult> {
		int type;
		public TTParallelTableBuilder(int type) {
			this.type = type;
		}
		@Override
		public TableBuildResult call() {
			double [] probs = new double[numTopics];
			double typeMass = 0; // Type prior mass
			for (int topic = 0; topic < numTopics; topic++) {
				// TODO: Leif, can we read typeTopicCounts like this?
				// TODO: If this works we can use a sparse version instead
				typeMass += probs[topic] = (typeTopicCounts[type][topic] + beta) / topicCountBetaHat[topic];
			}
			
			if(aliasTables[type]==null) {
				aliasTables[type] = new OptimizedGentleAliasMethod(probs, typeMass);
			} else {
				aliasTables[type].reGenerateAliasTable(probs, typeMass);
			}
				
			return new TableBuildResult(type, aliasTables[type], typeMass);
		}   
	}
	
	@Override
	public void preIteration() {
		super.preIteration();
		// TODO: Leif, should this be here?
		for (int topic = 0; topic < numTopics; topic++) {
			for (int type = 0; type < numTypes; type++) {
				topicCountBetaHat[topic] += typeTopicCounts[type][topic] + beta;
			}
		}
		
	};

	@Override
	protected void sampleTopicAssignmentsParallel(LDADocSamplingContext ctx) {
		FeatureSequence tokens = ctx.getTokens();
		LabelSequence topics = ctx.getTopics();
		int myBatch = ctx.getMyBatch();

		final int docLength = tokens.getLength();
		if(docLength==0) return;
		
		int [] tokenSequence = tokens.getFeatures();
		int [] oneDocTopics = topics.getFeatures();

		double[] localTopicCounts = new double[numTopics];
		double[] localTopicCounts_not_i = new double[numTopics];
		
		// Populate topic counts
		int nonZeroTopicCnt = 0; // Only needed for statistics
		for (int position = 0; position < docLength; position++) {
			int topicInd = oneDocTopics[position];
			localTopicCounts[topicInd]++;
			localTopicCounts_not_i[topicInd]++;
			if(localTopicCounts[topicInd]==1) nonZeroTopicCnt++;
		}

		kdDensities.addAndGet(nonZeroTopicCnt);
		
		//	Iterate over the words in the document
		for (int position = 0; position < docLength; position++) {
			int type = tokenSequence[position];
			int oldTopic = oneDocTopics[position]; // z_position
			int newTopic = oldTopic;
			
			if(localTopicCounts[oldTopic]<0) 
				throw new IllegalStateException("LightPC-LDA: Counts cannot be negative! Count for topic:" 
						+ oldTopic + " is: " + localTopicCounts[oldTopic]);

			decrement(myBatch, oldTopic, type);

			// #####################################
			// Word Topic Distribution 
			// #####################################
			
			// N_{d,Z_i} => # counts of topic Z_i in document d
			// Create n_d^{-i}, decrease document topic count with z_i
			localTopicCounts_not_i[oldTopic]--;
			
			double u = ThreadLocalRandom.current().nextDouble();
			// TODO: Leif, do this works?
			int wordTopicIndicatorProposal = aliasTables[type].generateSample(u);
			
			// If we drew a new topic indicator, do MH step for Word proposal
			if(wordTopicIndicatorProposal!=oldTopic) {
				double n_d_zi_not_i = localTopicCounts_not_i[oldTopic];
				double n_d_zstar_not_i = localTopicCounts_not_i[wordTopicIndicatorProposal];
				double n_zstar_beta_hat = topicCountBetaHat[wordTopicIndicatorProposal];
				double n_zi_beta_hat = topicCountBetaHat[oldTopic];
				double n_w_zi = typeTopicCounts[type][oldTopic];
				double n_w_zstar = typeTopicCounts[type][wordTopicIndicatorProposal];
								
				double nom = phi[wordTopicIndicatorProposal][type] * (alpha + n_d_zstar_not_i) * (beta + n_w_zi) * n_zstar_beta_hat;
				double denom = phi[oldTopic][type] * (alpha + n_d_zi_not_i)  * (beta + n_w_zstar) * n_zi_beta_hat;
				double pi_w =  nom / denom;
				
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
						// wordAccepts.incrementAndGet();
						oldTopic = wordTopicIndicatorProposal;
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
			
			// If we drew a new topic indicator, do MH step for Document proposal
			if(docTopicIndicatorProposal!=oldTopic) {
				double n_d_zstar_not_i = localTopicCounts_not_i[docTopicIndicatorProposal];
				double n_d_zi_not_i = localTopicCounts_not_i[oldTopic];
				double n_d_zi = localTopicCounts[oldTopic];
				double n_d_zstar = localTopicCounts[docTopicIndicatorProposal];

				double nom = phi[docTopicIndicatorProposal][type] * (alpha + n_d_zstar_not_i) * (alpha + n_d_zi);
				double denom = phi[oldTopic][type] * (alpha + n_d_zi_not_i) * (alpha + n_d_zstar);
				double pi_d = nom / denom;
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
			increment(myBatch, newTopic, type);

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
			localTopicCounts_not_i[newTopic]++;
		}
	}	
}
