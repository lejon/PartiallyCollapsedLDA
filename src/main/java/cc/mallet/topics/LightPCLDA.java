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
 * Implementation of the Light-LDA algorithm which uses Metropolis-Hastings to 
 * sample the topic indicators. This algorithm has O(1) to sample one topic
 * indicator no matter how many topics
 * 
 * From the article: "LightLDA: Big Topic Models on Modest Compute Clusters" 
 * by Jinhui Yuan1, Fei Gao1,2, Qirong Ho3, Wei Dai4, Jinliang Wei4, Xun Zheng4,
 *   Eric P. Xing4, Tie-Yan Liu1, Wei-Ying Ma1
 */
public class LightPCLDA extends SpaliasUncollapsedParallelLDA {

	private static final long serialVersionUID = 1L;
	Random rnd = new Random();
	
	/* This part can be added if measuring statistics over types of accept..
	AtomicInteger wordAccepts = new AtomicInteger();
	AtomicInteger docAccepts = new AtomicInteger();
	AtomicInteger oldAccepts = new AtomicInteger();
	
	public int getWordAccepts() {
		return wordAccepts.intValue();
	}

	public int getDocAccepts() {
		return docAccepts.intValue();
	}

	public int getOldAccepts() {
		return oldAccepts.intValue();
	}
	*/

	public LightPCLDA(LDAConfiguration config) {
		super(config);
	}
	
	class PhiTableBuilderFactory implements TableBuilderFactory {
		public Callable<WalkerAliasTableBuildResult> instance(int type) {
			return new PhiParallelTableBuilder(type);
		}
	}

	/**
	 * @author Leif Jonsson
	 * 
	 * The alias table is build only from Phi in LightPCLDA vs. phi*alpha in ordinary PCLDA
	 *
	 */
	class PhiParallelTableBuilder implements Callable<WalkerAliasTableBuildResult> {
		int type;
		public PhiParallelTableBuilder(int type) {
			this.type = type;
		}
		@Override
		public WalkerAliasTableBuildResult call() {
			double [] probs = new double[numTopics];
			double typeMass = 0; // Type prior mass
			for (int topic = 0; topic < numTopics; topic++) {
				typeMass += probs[topic] = phi[topic][type];
			}
			
			if(aliasTables[type]==null) {
				aliasTables[type] = new OptimizedGentleAliasMethod(probs,typeMass);
			} else {
				aliasTables[type].reGenerateAliasTable(probs, typeMass);
			}
				
			return new WalkerAliasTableBuildResult(type, aliasTables[type], typeMass);
		}   
	}

	@Override
	protected LDADocSamplingResult sampleTopicAssignmentsParallel(LDADocSamplingContext ctx) {
		FeatureSequence tokens = ctx.getTokens();
		LabelSequence topics = ctx.getTopics();
		int myBatch = ctx.getMyBatch();

		final int docLength = tokens.getLength();
		if(docLength==0) return null;
		
		int [] tokenSequence = tokens.getFeatures();
		int [] oneDocTopics = topics.getFeatures();

		int[] localTopicCounts = new int[numTopics];
		double[] localTopicCounts_i = new double[numTopics];
		
		// Populate topic counts
		int nonZeroTopicCnt = 0; // Only needed for statistics
		for (int position = 0; position < docLength; position++) {
			int topicInd = oneDocTopics[position];
			localTopicCounts[topicInd]++;
			localTopicCounts_i[topicInd]++;
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
			
			localTopicCounts_i[oldTopic]--;
			
			double u = ThreadLocalRandom.current().nextDouble();
			int wordTopicIndicatorProposal = aliasTables[type].generateSample(u);
			
			// If we drew a new topic indicator, do MH step for Word proposal
			if(wordTopicIndicatorProposal!=oldTopic) {
				double n_d_zi_i = localTopicCounts_i[oldTopic];
				double n_d_zstar_i = localTopicCounts_i[wordTopicIndicatorProposal];
				double pi_w = (alpha[oldTopic] + n_d_zstar_i) / (alpha[oldTopic] + n_d_zi_i);
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
			 
			double u_i = ThreadLocalRandom.current().nextDouble() * (oneDocTopics.length + alphaSum);
			
			int docTopicIndicatorProposal = -1;
			if(u_i < oneDocTopics.length) {
				docTopicIndicatorProposal = oneDocTopics[(int) u_i];
			} else {
				docTopicIndicatorProposal = (int) (((u_i - oneDocTopics.length) / alphaSum) * numTopics);
			}
			
			// If we drew a new topic indicator, do MH step for Document proposal
			if(docTopicIndicatorProposal!=oldTopic) {
				double n_d_zstar_i = localTopicCounts_i[docTopicIndicatorProposal];
				double n_d_zi_i = localTopicCounts_i[oldTopic];
				double n_d_zi = localTopicCounts[oldTopic];
				double n_d_zstar = localTopicCounts[docTopicIndicatorProposal];

				double nom = phi[docTopicIndicatorProposal][type] * (alpha[oldTopic] + n_d_zstar_i) * (alpha[oldTopic] + n_d_zi);
				double denom = phi[oldTopic][type] * (alpha[oldTopic] + n_d_zi_i) * (alpha[oldTopic] + n_d_zstar);
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
			localTopicCounts_i[newTopic]++;
		}
		return new LDADocSamplingResultDense(localTopicCounts);
	}	
}
