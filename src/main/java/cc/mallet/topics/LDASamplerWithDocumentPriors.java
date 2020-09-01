package cc.mallet.topics;

import java.util.Map;

public interface LDASamplerWithDocumentPriors extends LDAGibbsSampler {
	Map<Integer, int[]> getDocumentPriors();
}
