package cc.mallet.topics;

import java.io.IOException;

public interface LDASamplerContinuable extends LDAGibbsSampler {
	void continueSampling(int iterations) throws IOException;
	void preContinuedSampling();
	void postContinuedSampling();
}
