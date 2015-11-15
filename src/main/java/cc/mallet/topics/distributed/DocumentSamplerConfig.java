package cc.mallet.topics.distributed;

import java.io.Serializable;

public class DocumentSamplerConfig implements Serializable {

	private static final long serialVersionUID = 1L;

	public int numTopics;
	public int numTypes;
	public double alpha;
	public double beta;
	public int resultSize;
	public int [] docIndices;
	public  int batchId;
	public boolean sendPartials;
	
	public DocumentSamplerConfig(int numTopics, int numTypes, double alpha, 
			double beta, int resultSize, int myBatch, int [] docIndices, boolean sendPartials) {
		super();
		this.numTopics = numTopics;
		this.numTypes = numTypes;
		this.alpha = alpha;
		this.beta = beta;
		this.resultSize = resultSize;
		this.batchId = myBatch;
		this.docIndices = docIndices;
		this.sendPartials = sendPartials;
	}

	
}
