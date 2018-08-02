package cc.mallet.topics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.ArrayUtils;

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMaps;

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
