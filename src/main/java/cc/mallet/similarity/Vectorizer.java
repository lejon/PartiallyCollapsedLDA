package cc.mallet.similarity;

import cc.mallet.types.Instance;

public interface Vectorizer {
	double[] instanceToVector(Instance instance);
	int[] instanceToIntVector(Instance instance);
	String toAnnotatedString(Instance instance);
}
