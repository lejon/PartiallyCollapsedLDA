package cc.mallet.topics;

import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.LabelSequence;
import cc.mallet.util.OptimizedGentleAliasMethod;

public class LightPCLDA extends SpaliasUncollapsedParallelLDA {

	private static final long serialVersionUID = 1L;
	Random rnd = new Random();
	
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

	public LightPCLDA(LDAConfiguration config) {
		super(config);
		tbFactory = new PhiTableBuilderFactory();
	}
	
	class PhiTableBuilderFactory implements TableBuilderFactory {
		public Callable<TableBuildResult> instance(int type) {
			return new PhiParallelTableBuilder(type);
		}
	}

	class PhiParallelTableBuilder implements Callable<TableBuildResult> {
		int type;
		public PhiParallelTableBuilder(int type) {
			this.type = type;
		}
		@Override
		public TableBuildResult call() {
			double [] probs = new double[numTopics];
			double typeMass = 0; // Type prior mass
			double [] phiType =  phitrans[type]; 
			for (int topic = 0; topic < numTopics; topic++) {
				typeMass += probs[topic] = phiType[topic];
			}
			
			if(aliasTables[type]==null) {
				aliasTables[type] = new OptimizedGentleAliasMethod(probs,typeMass);
			} else {
				aliasTables[type].reGenerateAliasTable(probs, typeMass);
			}
				
			return new TableBuildResult(type, aliasTables[type], typeMass);
		}   
	}

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
		
		// Populate topic counts
		int nonZeroTopicCnt = 0;
		for (int position = 0; position < docLength; position++) {
			int topicInd = oneDocTopics[position];
			localTopicCounts[topicInd]++;
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

			// Propagates the update to the topic-token assignments
			/**
			 * Used to subtract and add 1 to the local structure containing the number of times
			 * each token is assigned to a certain topic. Called before and after taking a sample
			 * topic assignment z
			 */
			decrement(myBatch, oldTopic, type);

			// #####################################
			// Word Topic Distribution 
			// #####################################
			double u = ThreadLocalRandom.current().nextDouble();
			int wordTopicIndicatorProposal = aliasTables[type].generateSample(u);
			//System.out.println("Word topic indicator: " + wordTopicIndicatorProposal);
			
			double n_d_s_i = Math.max(localTopicCounts[oldTopic] - 1,0);
			double n_d_t_i = Math.max(localTopicCounts[wordTopicIndicatorProposal] - 1,0);
			double pi_w = Math.min(1, (alpha + n_d_s_i) / (alpha + n_d_t_i));
			
			double u_pi_w = ThreadLocalRandom.current().nextDouble();
			//System.out.println("Word Acceptance ratio : " + pi_w);
			boolean accept_pi_w = u_pi_w < pi_w;
			
			if(accept_pi_w) {				
				newTopic = wordTopicIndicatorProposal;
				wordAccepts.incrementAndGet();
				increment(myBatch, newTopic, type);
				//System.out.println("Accepted word!");
			} 
			
			// #####################################
			// Document Topic Distribution 
			// #####################################
			int docTopicIndicatorProposal = oneDocTopics[rnd.nextInt(oneDocTopics.length)];
			//System.out.println("Doc topic indicator: " + docTopicIndicatorProposal + " (old=" + oldTopic +")");
			double n_s_i = Math.max(tokensPerTopic[oldTopic]-1,0);
			double n_t_i = Math.max(tokensPerTopic[docTopicIndicatorProposal]-1,0);
			double n_d_s = localTopicCounts[oldTopic];
			double n_d_t = localTopicCounts[docTopicIndicatorProposal];
			
			double nom = phi[oldTopic][type] * (alpha + n_s_i) * n_d_t;
			double denom = phi[docTopicIndicatorProposal][type] * (alpha + n_t_i) * n_d_s;
			double ratio = nom / denom;
			//System.out.println("n_s_i = " + n_s_i + " n_t_i = " + n_t_i + " n_d_s = " + n_d_s + " n_d_t = " + n_d_t + "   " + nom + " / " + denom + " ======> " + ratio);
			double pi_d = Math.min(1, 
					ratio
					);
			
			//System.out.println("Doc  Acceptance ratio : " + pi_d);

			double u_pi_d = ThreadLocalRandom.current().nextDouble();
			boolean accept_pi_d = u_pi_d < pi_d;
			
			if (accept_pi_d) {
				newTopic = docTopicIndicatorProposal;
				docAccepts.incrementAndGet();
				//System.out.println("Accepted Doc!");
			} 
			
			if(!accept_pi_d && !accept_pi_w){
				newTopic = oldTopic;
				oldAccepts.incrementAndGet();
				//System.out.println("Kept OLD!");
			}
			// Propagates the update to the topic-token assignments
			/**
			 * Used to subtract and add 1 to the local structure containing the number of times
			 * each token is assigned to a certain topic. Called before and after taking a sample
			 * topic assignment z
			 */
			increment(myBatch, newTopic, type);

			// Make sure we actually sampled a valid topic
			if (newTopic < 0 || newTopic > numTopics) {
				throw new IllegalStateException ("LightPC-LDA: New valid topic not sampled (" + newTopic + ").");
			}

			// Remove one count from old topic
			localTopicCounts[oldTopic]--;
			// Update the word indicator
			oneDocTopics[position] = newTopic;
			// Put that new topic into the counts
			localTopicCounts[newTopic]++;
		}
		//System.out.println("Ratio: " + ((double)numPrior/(double)numLikelihood));
	}	
}
