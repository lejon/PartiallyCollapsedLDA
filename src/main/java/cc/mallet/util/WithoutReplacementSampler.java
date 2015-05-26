package cc.mallet.util;

import gnu.trove.TIntArrayList;

import java.util.concurrent.ThreadLocalRandom;

public class WithoutReplacementSampler implements IndexSampler {

	TIntArrayList available;
	public WithoutReplacementSampler(int startRange, int endRange) {
		available = new TIntArrayList();
		for (int i = startRange; i < endRange; i++) {
			available.add(i);
		}
	}

	@Override
	public int nextSample() {
		if(available.size()==0) {
			throw new IllegalStateException("Sampler is exausted, there are no more to sample");
		}
		int idx = (int) (ThreadLocalRandom.current().nextDouble() * available.size());
		int val = available.get(idx);
		available.remove(idx);
		return val;
	}

}
