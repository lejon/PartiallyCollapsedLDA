package cc.mallet.topics.randomscan.topic;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.topics.LDAGibbsSampler;

public class MixedMandelbrotDeltaNTopicIndexBuilder implements TopicIndexBuilder {

	LDAConfiguration config;
	LDAGibbsSampler sampler; 
	int instabilityPeriod = 0;
	int fullPhiPeriod;
	MandelbrotTopicIndexBuilder mandelbrot;
	DeltaNTopicIndexBuilder deltaN;
	int cnt = 0;

	public MixedMandelbrotDeltaNTopicIndexBuilder(LDAConfiguration config, LDAGibbsSampler sampler) {
		this.config = config;
		this.sampler = sampler;
		instabilityPeriod = config.getInstabilityPeriod(0); 
		fullPhiPeriod = config.getFullPhiPeriod(-1);
		mandelbrot = new MandelbrotTopicIndexBuilder(config, sampler);
		deltaN = new DeltaNTopicIndexBuilder(config, sampler);
	}

	/**
	 * Samples the top 20% of the most frequent tokens in the corpus every second time and Delta N the other 
	 * second time, respects the <code>full_phi_period</code>
	 * variable
	 */
	@Override
	public int[][] getTopicTypeIndices() {
		// If we are in the instable period, sample everything (null means everything)
		int currentIteration = sampler.getCurrentIteration();
		if(currentIteration<instabilityPeriod) return null;

		if(cnt%2==0) {
			cnt++;
			return mandelbrot.getTopicTypeIndices();
		} else {
			cnt++;
			return deltaN.getTopicTypeIndices();
		}
		
	}
}
