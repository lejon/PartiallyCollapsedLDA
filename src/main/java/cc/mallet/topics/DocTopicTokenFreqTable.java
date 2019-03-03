package cc.mallet.topics;

import java.util.concurrent.atomic.AtomicInteger;

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;

/**
 * A frequency table that holds a frequency table per topic.
 * The frequency table tells how many documents that contain a
 * set of frequencies per topic
 * 
 * I.e for topic 3, the map might be
 * 
 *  4 docs have 1 topic indicators of topic 3
 * 10 docs have 2 topic indicators of topic 3
 * 50 docs have 3 topic indicators of topic 3
 * 12 docs have 4 topic indicators of topic 3 
 *  7 docs have 5 topic indicators of topic 3 
 *  3 docs have 6 topic indicators of topic 3  
 *  1 docs have 7 topic indicators of topic 3   
 * 
 *  A matrix Q_{kp} of size K \times \max{n_d}, wher k is topic and p is the m_dk frequency.
 *     [ 0  0  0  0 0 0 0 ]
 * Q = [ 0  0  0  0 0 0 0 ]
 *     [ 4 10 50 12 7 3 1 ]
 * 
 * I.e we have a histogram for topic 3
 * 
 * Y: Nr. documents that has X topic indicators:	 4	10	50	12	 7	3	1
 * X: Nr. topic indicators of topic 3:			 	 1	 2	 3	 4	5	6
 * 
 * 
 * @author Leif Jonsson
 *
 */
public class DocTopicTokenFreqTable {

	Int2ObjectSortedMap<AtomicInteger> [] docTokenFreqMap;
	int numTopics;
	int maxFreq = -1;
	boolean [] nonEmptyTopics;

	@SuppressWarnings("unchecked")
	public DocTopicTokenFreqTable(int numTopics, int maxFreq) {
		this.numTopics = numTopics;
		this.maxFreq = maxFreq;
		docTokenFreqMap = new Int2ObjectSortedMap[numTopics];
		nonEmptyTopics = new boolean[numTopics];
		// Initialize 
		for (int topic = 0; topic < numTopics; topic++) {			
			//docTokenFreqMap[topic] =  Int2ObjectSortedMaps.synchronize(new Int2ObjectAVLTreeMap<AtomicInteger>());
			docTokenFreqMap[topic] =  new Int2ObjectAVLTreeMap<AtomicInteger>();
			for(int tokenFreq = 0; tokenFreq <= maxFreq; tokenFreq++) {
				docTokenFreqMap[topic].put(tokenFreq, new AtomicInteger(0));
			}
		}
	}
	
	public void reset() {
		for (int topic = 0; topic < numTopics; topic++) {			
			for(int tokenFreq = 0; tokenFreq <= maxFreq; tokenFreq++) {
				docTokenFreqMap[topic].get(tokenFreq).set(0);
			}
		}		
	}

	public void increment(int topic, int tokenFreq) {
		if(topic<0||topic>=numTopics) {
			throw new IndexOutOfBoundsException("DocTopicTokenFreqTable only contains " + numTopics + " topics. " + topic + " is out of range");
		}
		docTokenFreqMap[topic].get(tokenFreq).incrementAndGet();
		nonEmptyTopics[topic] = tokenFreq > 0;
	}

	/**
	 * Return the Reverse cumulative sum of document topic frequencies
	 * 
	 * This table allows us to answer the question:
	 * "How many documents (Y) have more than X number of topic indicators for topic K"
	 * 
	 *  A matrix D(k,j) of size K \times \max{n_d}, where k is topic and j is the number of document with at least j topic indicators.
	 *  Where \max{n_d} is the length of the longest document in the corpus
	 * 
     *          [ 0   0  0  0  0 0 0 ]
     * D(k,j) = [ 0   0  0  0  0 0 0 ]
     *          [ 87 83 73 23 11 4 1 ]
	 * @param topic Which topic frequency table to return
	 * @return Reverse Cumulative Frequency table
	 */
	// TODO: Fix test suite with example
	public int [] getReverseCumulativeSum(int topic) {
		Int2ObjectSortedMap<AtomicInteger> countTable = docTokenFreqMap[topic];

		int maxIdx = 0;
		for (int key : countTable.keySet()) {
			if(key>maxIdx && countTable.get(key).get()!=0) {
				maxIdx = key; 
			}
		}

		int[] intArray =  new int[maxIdx];

		int cumsum = 0;
		for (int key = maxIdx; key > 0; key--) {
			if(countTable.get(key)!=null) {
				cumsum += countTable.get(key).get();
				intArray[key-1] = cumsum;
			} else {
				intArray[key-1] = cumsum;
			}
		}

		return intArray;
	}

	public String toString() {
		String str = "";
		for (int topic = 0; topic < docTokenFreqMap.length; topic++) {	
			Int2ObjectSortedMap<AtomicInteger> countTable = docTokenFreqMap[topic];
			str += "\t[" + topic + "]: ";
			for (int key : countTable.keySet()) {
				str += "(" + key + "=>" + countTable.get(key).get() + "),"; 
			}
			str += "\n";
		}
		return str;
	}

	public int[] getEmptyTopics() {
		int emptyCnt = 0;
		for(int i = 0; i < nonEmptyTopics.length; i++) {
			if(!nonEmptyTopics[i]) {
				emptyCnt++;
			}
		}

		int [] emptyTopics = new int[emptyCnt];
		int cnt = 0;
		for(int i = 0; i < nonEmptyTopics.length; i++) {
			if(!nonEmptyTopics[i]) {
				emptyTopics[cnt++] = i;
			}
		}
		return emptyTopics;
	}
	
	public int getNumTopics() {
		return numTopics;
	}

	// TODO: This should be thread safe
	// But it is hard to make it thread safe without introducing a global
	// lock on the class, and we don't want to do that since that would
	// degrade performance on increment and we don't want that
	// WARNING: Not thread safe, should not be called from multiple threads
	public void moveTopic(int oldTopicPos, int newTopicPos) {
		Int2ObjectSortedMap<AtomicInteger> tmp = docTokenFreqMap[newTopicPos];
		docTokenFreqMap[newTopicPos] = docTokenFreqMap[oldTopicPos];
		docTokenFreqMap[oldTopicPos] = tmp;
	}

}
