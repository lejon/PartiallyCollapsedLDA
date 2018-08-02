package cc.mallet.topics;

import java.util.List;

public interface HDPSamplerWithPhi extends LDASamplerWithPhi {
	int [] getTopicOcurrenceCount();
	List<Integer> getActiveTopicHistory();
	List<Integer> getActiveTopicInDataHistory();

}
