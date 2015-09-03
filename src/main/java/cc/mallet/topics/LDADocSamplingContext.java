package cc.mallet.topics;

import cc.mallet.types.FeatureSequence;
import cc.mallet.types.LabelSequence;

public interface LDADocSamplingContext {

	FeatureSequence getTokens();

	void setTokens(FeatureSequence tokens);

	LabelSequence getTopics();

	void setTopics(LabelSequence topics);

	int getMyBatch();

	void setMyBatch(int myBatch);

	int getDocId();

	void setDocId(int docId);

}