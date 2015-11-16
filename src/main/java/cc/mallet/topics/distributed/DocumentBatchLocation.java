package cc.mallet.topics.distributed;
import java.io.Serializable;

public class DocumentBatchLocation implements Serializable {

	private static final long serialVersionUID = 1L;
	
	public String filename; 

	public DocumentBatchLocation(String filename) {
		this.filename = filename;
	}

}
