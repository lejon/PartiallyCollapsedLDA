package cc.mallet.topics;

import cc.mallet.types.FeatureSequence;
import cc.mallet.types.LabelSequence;

public class UncollapsedLDADocSamplingContext implements LDADocSamplingContext {
	FeatureSequence tokens;
	LabelSequence topics;
	int myBatch;
	int docId = -1;
	
	public UncollapsedLDADocSamplingContext(FeatureSequence tokens, LabelSequence topics, int myBatch, int docId) {
		super();
		this.tokens = tokens;
		this.topics = topics;
		this.myBatch = myBatch;
		this.docId = docId;
	}
	/* (non-Javadoc)
	 * @see cc.mallet.topics.LDADocSamplingContext#getTokens()
	 */
	@Override
	public FeatureSequence getTokens() {
		return tokens;
	}
	/* (non-Javadoc)
	 * @see cc.mallet.topics.LDADocSamplingContext#setTokens(cc.mallet.types.FeatureSequence)
	 */
	@Override
	public void setTokens(FeatureSequence tokens) {
		this.tokens = tokens;
	}
	/* (non-Javadoc)
	 * @see cc.mallet.topics.LDADocSamplingContext#getTopics()
	 */
	@Override
	public LabelSequence getTopics() {
		return topics;
	}
	/* (non-Javadoc)
	 * @see cc.mallet.topics.LDADocSamplingContext#setTopics(cc.mallet.types.LabelSequence)
	 */
	@Override
	public void setTopics(LabelSequence topics) {
		this.topics = topics;
	}
	/* (non-Javadoc)
	 * @see cc.mallet.topics.LDADocSamplingContext#getMyBatch()
	 */
	@Override
	public int getMyBatch() {
		return myBatch;
	}
	/* (non-Javadoc)
	 * @see cc.mallet.topics.LDADocSamplingContext#setMyBatch(int)
	 */
	@Override
	public void setMyBatch(int myBatch) {
		this.myBatch = myBatch;
	}		
	/* (non-Javadoc)
	 * @see cc.mallet.topics.LDADocSamplingContext#getDocId()
	 */
	@Override
	public int getDocId() {
		return docId;
	}
	/* (non-Javadoc)
	 * @see cc.mallet.topics.LDADocSamplingContext#setDocId(int)
	 */
	@Override
	public void setDocId(int docId) {
		this.docId = docId;
	}
}