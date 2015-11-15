package cc.mallet.topics.distributed;
import java.io.Serializable;


public class TypeTopicUpdates implements Serializable {

	private static final long serialVersionUID = 1L;
	
	public int [] updates; 
	public boolean isFinal = true;

	public TypeTopicUpdates(int [] updates) {
		this.updates = updates;
	}

	public TypeTopicUpdates(int [] updates, boolean isFinal) {
		this.updates = updates;
		this.isFinal = isFinal;
	}

}
