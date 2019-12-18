package cc.mallet.topics;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.types.InstanceList;
import cc.mallet.types.VariableSelectionResult;

public class PolyaUrnSpaliasLDAWithPriors extends PolyaUrnSpaliasLDA implements LDAGibbsSampler, LDASamplerWithPriors {
	
	private static final long serialVersionUID = 1L;

	public PolyaUrnSpaliasLDAWithPriors(LDAConfiguration config) {
		super(config);
	}

	@Override
	public double[][] getTopicPriors() {
		return topicPriors;
	}

	@Override
	public void addInstances(InstanceList training) {
		alphabet = training.getDataAlphabet();
		numTypes = alphabet.size();
		initializePriors(config);
		super.addInstances(training);
	}
	
	int calcScoreSampleTopic(int type, int[] localTopicCounts, double[] cumsum, int[] nonZeroTopicsAdjusted,
			int nonZeroTopicCntAdjusted, double u) {
		int newTopic;
		double sum;
		int topic = nonZeroTopicsAdjusted[0];
		double score = localTopicCounts[topic] * phi[topic][type] * topicPriors[topic][type];
		cumsum[0] = score;
		// Now calculate and add up the scores for each topic for this word
		// We build a cumsum indexed by topicIndex
		int topicIdx = 1;
		while ( topicIdx < nonZeroTopicCntAdjusted ) {
			topic = nonZeroTopicsAdjusted[topicIdx];
			score = localTopicCounts[topic] * phi[topic][type] * topicPriors[topic][type];
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
}
