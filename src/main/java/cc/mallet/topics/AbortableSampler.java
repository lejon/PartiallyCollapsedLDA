package cc.mallet.topics;

public interface AbortableSampler {
	void abort();
	boolean getAbort();
}
