package cc.mallet.topics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.ArrayUtils;

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMaps;

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

	public DocTopicTokenFreqTable(int numTopics) {
		this.numTopics = numTopics;
		docTokenFreqMap = new Int2ObjectSortedMap[numTopics]; 
		// Initialize
		for (int topic = 0; topic < numTopics; topic++) {			
			docTokenFreqMap[topic] =  Int2ObjectSortedMaps.synchronize(new Int2ObjectAVLTreeMap<AtomicInteger>());
		}
	}

	public void increment(int topic, int tokenFreq) {
		if(topic<0||topic>=numTopics) {
			throw new IndexOutOfBoundsException("DocTopicTokenFreqTable only contains " + numTopics + " topics. " + topic + " is out of range");
		}
		if(docTokenFreqMap[topic].get(tokenFreq)==null) {
			docTokenFreqMap[topic].put(tokenFreq, new AtomicInteger(0));
		}
		docTokenFreqMap[topic].get(tokenFreq).incrementAndGet();
	}

	/**
	 * Return the Reverse cumulative sum of document topic frequencies
	 * 
	 * This table allows us to answer the question:
	 * "How many documents (Y) have more than X number of topic indicators for topic K"
	 * 
	 * @param topic Which topic frequency table to return
	 * @return Reverse Cumulative Frequency table
	 */
	public int [] getReverseCumulativeSum(int topic) {
		Int2ObjectSortedMap<AtomicInteger> countTable = docTokenFreqMap[topic];

		int maxIdx = 0;
		for (int key : countTable.keySet()) {
			if(key>maxIdx) {
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
		List<Integer> emptyTopics = new ArrayList<Integer>();
		
		for (int topic = 0; topic < numTopics; topic++) {
			if(getReverseCumulativeSum(topic).length==0) {
				emptyTopics.add(topic);
			}
		}
		
		int[] intArray = ArrayUtils.toPrimitive(emptyTopics.toArray(new Integer[0]));
		return intArray;

	}

	public int getNumTopics() {
		return numTopics;
	}

	public void moveTopic(int oldTopicPos, int newTopicPos) {
		Int2ObjectSortedMap<AtomicInteger> tmp = docTokenFreqMap[newTopicPos];
		docTokenFreqMap[newTopicPos] = docTokenFreqMap[oldTopicPos];
		docTokenFreqMap[oldTopicPos] = tmp;
	}

}
