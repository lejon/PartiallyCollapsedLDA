package cc.mallet.topics;

import java.util.ArrayList;
import java.util.HashMap;

public interface HashLDA {
	int [] getMapType();
	public ArrayList<HashMap<Integer, Integer>> getHashTopicTypeCounts();
}
