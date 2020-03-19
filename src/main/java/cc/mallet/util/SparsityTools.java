package cc.mallet.util;

public class SparsityTools {
	public static int remove(int oldTopic, int[] nonZeroTopics, int[] nonZeroTopicsBackMapping, int nonZeroTopicCnt) {
		if (nonZeroTopicCnt<1) {
			throw new IllegalArgumentException ("Cannot remove, count is less than 1 => " + nonZeroTopicCnt);
		}
		// We have one less non-zero topic, move the last to its place, and decrease the non-zero count
		int nonZeroIdx = nonZeroTopicsBackMapping[oldTopic];
		nonZeroTopics[nonZeroIdx] = nonZeroTopics[--nonZeroTopicCnt];
		nonZeroTopicsBackMapping[nonZeroTopics[nonZeroIdx]] = nonZeroIdx;
		return nonZeroTopicCnt;
	}

	public static int insert(int newTopic, int[] nonZeroTopics, int[] nonZeroTopicsBackMapping, int nonZeroTopicCnt) {
		//// We have a new non-zero topic put it in the last empty slot and increase the count
		nonZeroTopics[nonZeroTopicCnt] = newTopic;
		nonZeroTopicsBackMapping[newTopic] = nonZeroTopicCnt;
		return ++nonZeroTopicCnt;
	}

	public static int removeSorted(int oldTopic, int[] nonZeroTopics, int[] nonZeroTopicsBackMapping, int nonZeroTopicCnt) {
		if (nonZeroTopicCnt<1) {
			throw new IllegalArgumentException ("Cannot remove, count is less than 1");
		}
		//System.out.println("New empty topic. Cnt = " + nonZeroTopicCnt);	
		int nonZeroIdx = nonZeroTopicsBackMapping[oldTopic];
		nonZeroTopicCnt--;
		// Shift the ones above one step to the left
		for(int i=nonZeroIdx; i<nonZeroTopicCnt;i++) {
			// Move the last non-zero topic to this new empty slot 
			nonZeroTopics[i] = nonZeroTopics[i+1];
			// Do the corresponding for the back mapping
			nonZeroTopicsBackMapping[nonZeroTopics[i]] = i;
		}
		return nonZeroTopicCnt;
	}

	public static int insertSorted(int newTopic, int[] nonZeroTopics, int[] nonZeroTopicsBackMapping, int nonZeroTopicCnt) {
		//// We have a new non-zero topic put it in the last empty slot
		int slot = 0;
		while(newTopic > nonZeroTopics[slot] && slot < nonZeroTopicCnt) slot++;

		for(int i=nonZeroTopicCnt; i>slot;i--) {
			// Move the last non-zero topic to this new empty slot 
			nonZeroTopics[i] = nonZeroTopics[i-1];
			// Do the corresponding for the back mapping
			nonZeroTopicsBackMapping[nonZeroTopics[i]] = i;
		}				
		nonZeroTopics[slot] = newTopic;
		nonZeroTopicsBackMapping[newTopic] = slot;
		nonZeroTopicCnt++;
		return nonZeroTopicCnt;
	}

	public static int findIdx(double[] cumsum, double u, int maxIdx) {
		if(cumsum.length<2000) {
			return findIdxLinSentinel(cumsum,u,maxIdx);
		} else {
			return findIdxBin(cumsum,u,maxIdx);
		}
	}
	
	public static int findIdxBin(double[] cumsum, double u, int maxIdx) {
		int slot = java.util.Arrays.binarySearch(cumsum,0,maxIdx,u);

		return slot >= 0 ? slot : -(slot+1); 
	}

	public static int findIdxLinSentinel(double[] cumsum, double u, int maxIdx) {
		cumsum[cumsum.length-1] = Double.MAX_VALUE;
		int i = 0;
		while(true) {
			if(u<=cumsum[i]) return i;
			i++;
		}
	}

	public static int findIdxLin(double[] cumsum, double u, int maxIdx) {
		for (int i = 0; i < maxIdx; i++) {
			if(u<=cumsum[i]) return i;
		}
		return cumsum.length-1;
	}
}
