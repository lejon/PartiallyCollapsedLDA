package cc.mallet.topics;

import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.LabelSequence;
import cc.mallet.util.OptimizedGentleAliasMethod;

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
		double[] localTopicCounts_i = new double[numTopics];
		
		// Populate topic counts
		int nonZeroTopicCnt = 0;
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
			//System.out.println("Word topic indicator: " + wordTopicIndicatorProposal);
			
			double n_d_zi_i = localTopicCounts_i[oldTopic];
			double n_d_zstar_i = localTopicCounts_i[wordTopicIndicatorProposal];
			double pi_w = Math.min(1, (alpha + n_d_zstar_i) / (alpha + n_d_zi_i));
			
			double u_pi_w = ThreadLocalRandom.current().nextDouble();
			//System.out.println("Word Acceptance ratio : " + pi_w);
			boolean accept_pi_w = u_pi_w < pi_w;
			
			if(accept_pi_w) {				
				localTopicCounts[oldTopic]--;
				localTopicCounts[wordTopicIndicatorProposal]++;
				
				// Set oldTopic to the new wordTopicIndicatorProposal just accepted.
				// By doing this the below document proposal will be relative to the 
				// new best proposal
				oldTopic = wordTopicIndicatorProposal;
				//wordAccepts.incrementAndGet();
				//System.out.println("Accepted word!");
			} 
			
			// #####################################
			// Document Topic Distribution 
			// #####################################
			int docTopicIndicatorProposal = oneDocTopics[rnd.nextInt(oneDocTopics.length)];
			//System.out.println("Doc topic indicator: " + docTopicIndicatorProposal + " (old=" + oldTopic +")");
			n_d_zi_i = localTopicCounts_i[oldTopic];
			n_d_zstar_i = localTopicCounts_i[docTopicIndicatorProposal];
			double n_d_zi = localTopicCounts[oldTopic];
			double n_d_zstar = localTopicCounts[docTopicIndicatorProposal];
			
			double nom = phi[docTopicIndicatorProposal][type] * (alpha + n_d_zstar_i) * n_d_zi;
			double denom = phi[oldTopic][type] * (alpha + n_d_zi) * n_d_zstar;
			double ratio = nom / denom;
			//System.out.println("n_s_i = " + n_s_i + " n_t_i = " + n_t_i + " n_d_s = " + n_d_s + " n_d_t = " + n_d_t + "   " + nom + " / " + denom + " ======> " + ratio);
			double pi_d = Math.min(1, ratio);
			
			//System.out.println("Doc  Acceptance ratio : " + pi_d);

			double u_pi_d = ThreadLocalRandom.current().nextDouble();
			boolean accept_pi_d = u_pi_d < pi_d;
			
			if (accept_pi_d) {
				newTopic = docTopicIndicatorProposal;
				//docAccepts.incrementAndGet();
				//System.out.println("Accepted Doc!");
			} else {
				// We did not accept either word or document proposal 
				// so oldTopic is still the best indicator
				newTopic = oldTopic;
				//oldAccepts.incrementAndGet();
				//System.out.println("Kept OLD!");
			}

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
			// Make sure the "_i" version is also up to date!
			localTopicCounts_i[newTopic]++;
		}
		//System.out.println("Ratio: " + ((double)numPrior/(double)numLikelihood));
	}	
}
