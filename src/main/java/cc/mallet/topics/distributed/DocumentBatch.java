package cc.mallet.topics.distributed;
import java.io.Serializable;

public class DocumentBatch implements Serializable {

	private static final long serialVersionUID = 1L;
	
	public int [][] docTopics; 

	public DocumentBatch(int [][] docTopics) {
		this.docTopics = docTopics;
	}

}
