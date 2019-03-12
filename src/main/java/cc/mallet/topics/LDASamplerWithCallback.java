package cc.mallet.topics;

import cc.mallet.topics.tui.IterationListener;

public interface LDASamplerWithCallback extends LDAGibbsSampler {
	void setIterationCallback(IterationListener iterListener);
}
