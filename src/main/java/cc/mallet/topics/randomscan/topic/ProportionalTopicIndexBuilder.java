package cc.mallet.topics.randomscan.topic;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.topics.LDAGibbsSampler;
import cc.mallet.util.SystematicSampling;

public class ProportionalTopicIndexBuilder implements TopicIndexBuilder {

	LDAConfiguration config;
	LDAGibbsSampler sampler; 
	int instabilityPeriod = 0;
	int fullPhiPeriod;
	AllWordsTopicIndexBuilder allWords;
	int skipStep=1;

	public ProportionalTopicIndexBuilder(LDAConfiguration config, LDAGibbsSampler sampler) {
		this.config = config;
		this.sampler = sampler;
		instabilityPeriod = config.getInstabilityPeriod(0); 
		fullPhiPeriod = config.getFullPhiPeriod(-1);
		allWords = new AllWordsTopicIndexBuilder(config,sampler);
		skipStep = config.getProportionalTopicIndexBuilderSkipStep();
	}

	/**
	 * Samples the types in the corpus proportional to their frequency in the corpus
	 * using systematic sampling. 
	 * Respects the <code>full_phi_period</code>
	 * Respects the <code>instabilityPeriod</code>
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
			//typeCounts
			int [] typeFreqs = sampler.getTypeFrequencies();
			int [] indicesToSample = SystematicSampling.sample(typeFreqs, skipStep);
			
			int [][] topicTypeIndices = new int [sampler.getNoTopics()][];
			for (int i = 0; i < topicTypeIndices.length; i++) {
				topicTypeIndices[i] = indicesToSample;
			}
			return topicTypeIndices;
		}
	}
}
