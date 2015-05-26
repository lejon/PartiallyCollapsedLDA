package cc.mallet.topics.randomscan.topic;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.topics.LDAGibbsSampler;

public class MandelbrotTopicIndexBuilder implements TopicIndexBuilder {

	LDAConfiguration config;
	LDAGibbsSampler sampler; 
	int instabilityPeriod = 0;
	int fullPhiPeriod;
	AllWordsTopicIndexBuilder allWords;
	double percentToSample;

	public MandelbrotTopicIndexBuilder(LDAConfiguration config, LDAGibbsSampler sampler) {
		this.config = config;
		this.sampler = sampler;
		instabilityPeriod = config.getInstabilityPeriod(0); 
		fullPhiPeriod = config.getFullPhiPeriod(-1);
		percentToSample = config.topTokensToSample(0.2);
		allWords = new AllWordsTopicIndexBuilder(config,sampler);
	}

	/**
	 * Samples the top X% ('percent_top_tokens' from config) of the most frequent tokens in the corpus, 
	 * respects the <code>full_phi_period</code> variable
	 */
	@Override
	public int[][] getTopicTypeIndices() {
		// If we are in the instable period, sample everything (null means everything)
		int currentIteration = sampler.getCurrentIteration();
		if(currentIteration<instabilityPeriod) return null;

		// Every fullPhiPeriod we sample the whole Phi
		if(fullPhiPeriod>0 && ((currentIteration % fullPhiPeriod) == 0)) {
			return allWords.getTopicTypeIndices();			
		} else {
			int [] topIndices = sampler.getTopTypeFrequencyIndices();
			
			int noToSample = (int) Math.ceil(percentToSample*topIndices.length);
			int [] indicesToSample = new int[noToSample];
			for (int i = 0; i < noToSample; i++) {
				indicesToSample[i] = topIndices[i];
			}
			int [][] topicTypeIndices = new int [sampler.getNoTopics()][];
			// In the basic version we sample the same tokens (words) in all the topics
			for (int i = 0; i < topicTypeIndices.length; i++) {
				topicTypeIndices[i] = indicesToSample;
			}
			return topicTypeIndices;
		}
	}
}
