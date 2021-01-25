package cc.mallet.similarity;

import static java.lang.Math.max;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import lombok.ToString;

@ToString
public class StatObj {
	public int [] typeCounts;
	public int [] docFreqs;
	public long corpusWordCount; 
	public int corpusSize;
	Set<Integer> uniqueTypes;
	Map<Integer,Map<Integer,Integer>> coOccurrences;
	

	public static StatObj add(StatObj a, StatObj b) {
		StatObj result = new StatObj();
		Set<Integer>  tmpUniqueTypes = new HashSet<>();
		
		result.corpusWordCount = a.corpusWordCount + b.corpusWordCount;

		result.typeCounts = addCounts(a.typeCounts, b.typeCounts);
		result.docFreqs = addCounts(a.docFreqs, b.docFreqs);

		result.corpusSize = a.corpusSize + b.corpusSize;
		if(a.uniqueTypes!=null)
			tmpUniqueTypes.addAll(a.uniqueTypes);
		if(b.uniqueTypes!=null)
			tmpUniqueTypes.addAll(b.uniqueTypes);
		result.uniqueTypes = tmpUniqueTypes;
		
		Map<Integer,Map<Integer,Integer>> tmpCoOccurrence = new HashMap<>();
		if(a.coOccurrences!=null) 
			mergeCoOccurrences(tmpCoOccurrence, a.coOccurrences);
		if(b.coOccurrences!=null) 
			mergeCoOccurrences(tmpCoOccurrence, b.coOccurrences);
		result.coOccurrences = tmpCoOccurrence;

		return result;
	}

	static int [] addCounts(int [] aOrigCounts, int [] bOrigCounts) {
		int [] aCounts = aOrigCounts != null ? aOrigCounts : new int [0];
		int [] bCounts = bOrigCounts != null ? bOrigCounts : new int [0];
		int [] resultCounts = new int[max(aCounts.length,bCounts.length)];
		if(aCounts.length == bCounts.length) {
			for (int i = 0; i < resultCounts.length; i++) {
				resultCounts[i] = aCounts[i] + bCounts[i];
			}
		} else {
			int [] longer = aCounts.length > bCounts.length ? aCounts : bCounts;
			int [] shorter = aCounts.length <= bCounts.length ? aCounts : bCounts;
			int i = 0; 
			for (; i < shorter.length; i++) {
				resultCounts[i] = longer[i] + shorter[i];
			}			
			for (; i < longer.length; i++) {
				resultCounts[i] = longer[i];
			}
		}
		return resultCounts;
	}

	public static StatObj addInstance(StatObj inputResult, Instance instance) {
		StatObj newRes = new StatObj();
		newRes.typeCounts = new int [instance.getAlphabet().size()];
		newRes.docFreqs = new int [instance.getAlphabet().size()];
		boolean [] reportedType = new boolean[instance.getAlphabet().size()];
		FeatureSequence tokens = (FeatureSequence) instance.getData();
		int docLength = tokens.size();
		newRes.corpusWordCount = docLength + inputResult.corpusWordCount;
		newRes.corpusSize = 1 + inputResult.corpusSize;
		Set<Integer>  tmpUniqueTypes = new HashSet<>();
		Map<Integer,Map<Integer,Integer>> tmpCoOccurrence = new HashMap<>();

		for (int position = 0; position < docLength; position++) {
			int type = tokens.getIndexAtPosition(position);
			tmpUniqueTypes.add(type);
			newRes.typeCounts[type]++;
			if(!reportedType[type]) {
				newRes.docFreqs[type]++;
				reportedType[type] = true;
			}
			
			if(position+1 < docLength) {
				int coOccurring = tokens.getIndexAtPosition(position+1);
				condIncrement(tmpCoOccurrence,type,coOccurring);
			}
		}
		
		if(inputResult.coOccurrences!=null)
			mergeCoOccurrences(tmpCoOccurrence,inputResult.coOccurrences);
		newRes.coOccurrences = tmpCoOccurrence;
		
		if(inputResult.uniqueTypes!=null)
			tmpUniqueTypes.addAll(inputResult.uniqueTypes);
		newRes.uniqueTypes = tmpUniqueTypes;
		
		for (int i = 0; inputResult.typeCounts != null && i < inputResult.typeCounts.length; i++) {
			newRes.typeCounts[i] += inputResult.typeCounts[i];
		}

		for (int i = 0; inputResult.docFreqs != null && i < inputResult.docFreqs.length; i++) {
			newRes.docFreqs[i] += inputResult.docFreqs[i];
		}
		
		return newRes;
	}

	static void mergeCoOccurrences(Map<Integer, Map<Integer, Integer>> tmpCoOccurrence,
			Map<Integer, Map<Integer, Integer>> coOccurrence2) {
		for(int type : coOccurrence2.keySet()) {
			for(int coOccurringType : coOccurrence2.get(type).keySet()) {
				condIncrement(tmpCoOccurrence, type, coOccurringType, coOccurrence2.get(type).get(coOccurringType));
			}
		}
	}
	
	static void condIncrement(Map<Integer,Map<Integer,Integer>> tmpCoOccurrence, int type, int coOccurringType) {
		condIncrement(tmpCoOccurrence, type, coOccurringType, 1);
	}

	static void condIncrement(Map<Integer,Map<Integer,Integer>> tmpCoOccurrence, int type, int coOccurringType, 
			int increment) {
		if(!tmpCoOccurrence.containsKey(type)) {
			tmpCoOccurrence.put(type, new HashMap<>());
		}
		if(!tmpCoOccurrence.get(type).containsKey(coOccurringType)) {
			tmpCoOccurrence.get(type).put(coOccurringType,0);
		}
		tmpCoOccurrence.get(type).put(coOccurringType,tmpCoOccurrence.get(type).get(coOccurringType)+increment);
	}

	public double getAvgDocLen() {
		return corpusWordCount / (double) corpusSize;
	}

	public int getUniqueTypes() {
		return uniqueTypes.size();
	}
	
	public Map<Integer,Map<Integer,Integer>> getCoOccurrences() {
		return coOccurrences;
	}

}
