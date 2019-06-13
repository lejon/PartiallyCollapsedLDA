package cc.mallet.similarity;

import cc.mallet.types.Instance;

public interface InstanceDistance {
	double calculate(Instance v1, Instance v2);
}
