package cc.mallet.topics.tui;

import cc.mallet.topics.LDAGibbsSampler;

public interface IterationListener {
	void iterationCallback(LDAGibbsSampler model);
}
