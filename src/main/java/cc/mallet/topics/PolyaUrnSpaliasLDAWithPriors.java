package cc.mallet.topics;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelSequence;
import cc.mallet.types.VariableSelectionResult;
import cc.mallet.util.WalkerAliasTable;

public class PolyaUrnSpaliasLDAWithPriors extends PolyaUrnSpaliasLDA implements LDAGibbsSampler, LDASamplerWithTopicPriors, LDASamplerWithDocumentPriors {

	private static final long serialVersionUID = 1L;

	public PolyaUrnSpaliasLDAWithPriors(LDAConfiguration config) {
		super(config);
	}

	@Override
	public double[][] getTopicPriors() {
		return topicPriors;
	}

	@Override
	public Map<Integer, int[]> getDocumentPriors() {
		return documentPriors;
	}

	@Override
	public void addInstances(InstanceList training) {
		alphabet = training.getDataAlphabet();
		numTypes = alphabet.size();
		initializePriors(config);
		super.addInstances(training);
	}

	@Override
	protected LDADocSamplingResult sampleTopicAssignmentsParallel(LDADocSamplingContext ctx) {
		FeatureSequence tokens = ctx.getTokens();
		LabelSequence topics = ctx.getTopics();
		int myBatch = ctx.getMyBatch();

		int type, oldTopic, newTopic;

		final int docLength = tokens.getLength();
		if(docLength==0) { 
			return new LDADocSamplingResultSparseSimple(new int [0],0,new int [0]); 
		}

		int [] tokenSequence = tokens.getFeatures();
		int [] oneDocTopics = topics.getFeatures();

		int[] localTopicCounts = new int[numTopics];

		// This vector contains the indices of the topics with non-zero entries.
		// It has to be numTopics long since the non-zero topics come and go...
		int [] nonZeroTopics = new int[numTopics];

		// So we can map back from a topic to where it is in nonZeroTopics vector
		int [] nonZeroTopicsBackMapping = new int[numTopics];

		// Populate topic counts
		int nonZeroTopicCnt = 0;
		for (int position = 0; position < docLength; position++) {
			int topicInd = oneDocTopics[position];
			localTopicCounts[topicInd]++;
			if(localTopicCounts[topicInd]==1) {
				nonZeroTopicCnt = insert(topicInd, nonZeroTopics, nonZeroTopicsBackMapping, nonZeroTopicCnt);
			}
		}

		double[] thisDocumentPriors = null;
		Set<Integer> forbidden = null;
		if(haveDocumentPriors) {
			thisDocumentPriors = initDocumentPriors(ctx);

			forbidden = new HashSet<>();
			for (int i = 0; i < thisDocumentPriors.length; i++) {
				if(thisDocumentPriors[i]==0.0) {
					forbidden.add(i);
				}
			}
		} else {
			thisDocumentPriors = new double[numTopics]; 
			Arrays.fill(thisDocumentPriors, 1);
		}

		kdDensities.addAndGet(nonZeroTopicCnt);

		double[] cumsum = new double[numTopics]; 
		int [] nonZeroTopicsAdjusted;
		int nonZeroTopicCntAdjusted;

		//	Iterate over the words in the document
		for (int position = 0; position < docLength; position++) {
			type = tokenSequence[position];
			oldTopic = oneDocTopics[position]; // z_position
			localTopicCounts[oldTopic]--;

			// Potentially update nonZeroTopics mapping
			if(localTopicCounts[oldTopic]==0) {
				nonZeroTopicCnt = remove(oldTopic, nonZeroTopics, nonZeroTopicsBackMapping, nonZeroTopicCnt);
			}

			if(localTopicCounts[oldTopic]<0) 
				throw new IllegalStateException("Counts cannot be negative! Count for topic:" 
						+ oldTopic + " is: " + localTopicCounts[oldTopic]);

			// Propagates the update to the topic-token assignments
			/**
			 * Used to subtract and add 1 to the local structure containing the number of times
			 * each token is assigned to a certain topic. Called before and after taking a sample
			 * topic assignment z
			 */
			decrement(myBatch, oldTopic, type);

			int nonZeroTypeCnt = nonZeroTypeTopicColIdxs[type];

			if(nonZeroTypeCnt < nonZeroTopicCnt && nonZeroTypeCnt > 0) {
				// INTERSECTION SHOULD IMPROVE perf since we use result both in cumsum and sample topic
				// Intersection needs to b O(k) for it to improve perf, but unless we add more memory 
				// requirements it becomes O(k log(k))
				nonZeroTopicsAdjusted = nonZeroTypeTopicIdxs[type];
				nonZeroTopicCntAdjusted = nonZeroTypeCnt;
				//usedTypeSparsness.incrementAndGet();
			} else {
				nonZeroTopicsAdjusted = nonZeroTopics;
				nonZeroTopicCntAdjusted = nonZeroTopicCnt;
			}

			double u = ThreadLocalRandom.current().nextDouble();

			// Document and type sparsity removed all (but one?) topics, just use the prior contribution
			// This happens when the document has only one word, then we use only the 
			// word probability in phi
			if(nonZeroTopicCntAdjusted==0) {
				double[] topicTermScores = new double[numTopics];

				double score = phi[0][type];
				topicTermScores[0] = score;
				for (int topic = 1; topic < numTopics; topic++) {
					score += phi[topic][type];
					topicTermScores[topic] = score;
				}
				// Choose a random point between 0 and the sum of all topic scores
				double sample = random.nextUniform() * score;

				// In some rare cases (typically in very short documents), all types have
				// 0 likelihood in the PolyaUrn Case, in this case we just randomly pick a 
				// topic (i.e the else branch)
				if(sample > 0.0) {
					// Figure out which topic contains that point
					newTopic = -1;
					while (sample > 0.0) {
						newTopic++;
						sample -= topicTermScores[newTopic];
					} 
				} else {
					newTopic = random.nextInt(numTopics);
				}
			} else { 
				newTopic = calcScoreSampleTopic(type, localTopicCounts, cumsum, nonZeroTopicsAdjusted, nonZeroTopicCntAdjusted, u, thisDocumentPriors);
			}

			// Sometimes when we draw from the prior in the above case, we draw a topic
			// that is forbidden in the document prior spec (because the alias tables are golbal
			// and the document prior spec is document local), in that case we build a new
			// alias table for this document / prior combination (cache it) and draw from it
			if(haveDocumentPriors && forbidden.contains(newTopic)) {
				WalkerAliasTable extraTable = getExtraAliasTable(type,ctx.getDocIdx());
				newTopic = extraTable.generateSample();
			}

			// Make sure we actually sampled a valid topic
			if (newTopic < 0 || newTopic >= numTopics) {
				System.err.println("Didn't manage to sample " + docLength + " long document. Sampling from " + (nonZeroTopicCntAdjusted==0?"prior":"likelihood"));
				throw new IllegalStateException ("PolyaUrnSpaliasLDA: Invalid topic sampled (" + newTopic + ").");
			}

			// Put that new topic into the counts
			oneDocTopics[position] = newTopic;
			localTopicCounts[newTopic]++;

			// Potentially update nonZeroTopics mapping
			if(localTopicCounts[newTopic]==1) {
				nonZeroTopicCnt = insert(newTopic, nonZeroTopics, nonZeroTopicsBackMapping, nonZeroTopicCnt);
			}

			// Propagates the update to the topic-token assignments
			/**
			 * Used to subtract and add 1 to the local structure containing the number of times
			 * each token is assigned to a certain topic. Called before and after taking a sample
			 * topic assignment z
			 */
			increment(myBatch, newTopic, type);
		}

		return new LDADocSamplingResultSparseSimple(localTopicCounts,nonZeroTopicCnt,nonZeroTopics);
	}


	int calcScoreSampleTopic(int type, int[] localTopicCounts, double[] cumsum, int[] nonZeroTopicsAdjusted,
			int nonZeroTopicCntAdjusted, double u, double [] thisDocumentPriors) {
		int newTopic;
		double sum;
		int topic = nonZeroTopicsAdjusted[0];
		double score = localTopicCounts[topic] * phi[topic][type] * topicPriors[topic][type] * thisDocumentPriors[topic];
		cumsum[0] = score;
		// Now calculate and add up the scores for each topic for this word
		// We build a cumsum indexed by topicIndex
		int topicIdx = 1;
		while ( topicIdx < nonZeroTopicCntAdjusted ) {
			topic = nonZeroTopicsAdjusted[topicIdx];
			score = localTopicCounts[topic] * phi[topic][type] * topicPriors[topic][type] * thisDocumentPriors[topic];
			cumsum[topicIdx] = score + cumsum[topicIdx-1];
			topicIdx++;
		}
		sum = cumsum[topicIdx-1]; // sigma_likelihood

		// Choose a random point between 0 and the sum of all topic scores
		// The thread local random performs better in concurrent situations 
		// than the standard random which is thread safe and incurs lock 
		// contention
		double u_sigma = u * (typeNorm[type] + sum);
		// u ~ U(0,1)  
		// u [0,1]
		// u_sigma = u * (typeNorm[type] + sum)
		// if u_sigma < typeNorm[type] -> prior
		// u * (typeNorm[type] + sum) < typeNorm[type] => u < typeNorm[type] / (typeNorm[type] + sum)
		// else -> likelihood
		// u_prior = u_sigma / typeNorm[type] -> u_prior (0,1)
		// u_likelihood = (u_sigma - typeNorm[type]) / sum  -> u_likelihood (0,1)

		newTopic = sampleNewTopic(type, nonZeroTopicsAdjusted, nonZeroTopicCntAdjusted, sum, cumsum, u, u_sigma);
		return newTopic;
	}

	@Override
	double [] samplePhiTopic(int [] relevantTypeTopicCounts, int topic) {
		VariableSelectionResult res = dirichletSampler.nextDistributionWithSparseness(relevantTypeTopicCounts);
		double [] phi = res.getPhi();
		for (int i = 0; i < phi.length; i++) {
			phi[i] = phi[i] * topicPriors[topic][i];
		}
		return phi;
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeObject(topicPriors);
		out.writeObject(documentPriors);
	}

	@SuppressWarnings("unchecked")
	private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
		topicPriors = (double[][]) in.readObject();
		documentPriors = (Map<Integer, int[]>) in.readObject();
	}

	@Override
	public void initFrom(LDAGibbsSampler source) {
		super.initFrom(source);
		LDASamplerWithTopicPriors phiSampler = (LDASamplerWithTopicPriors) source;
		topicPriors = phiSampler.getTopicPriors();
		LDASamplerWithDocumentPriors phiSamplerWithDocPriors = (LDASamplerWithDocumentPriors) source;
		documentPriors =  phiSamplerWithDocPriors.getDocumentPriors();
	}

}
