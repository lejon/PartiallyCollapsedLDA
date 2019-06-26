package cc.mallet.similarity;

import cc.mallet.types.Instance;

public interface InstanceDistance {
	double distance(Instance v1, Instance v2);
}
