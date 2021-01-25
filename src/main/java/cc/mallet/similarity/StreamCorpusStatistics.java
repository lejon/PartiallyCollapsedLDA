package cc.mallet.similarity;

import java.util.Map;

import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;

public class StreamCorpusStatistics {

	protected Alphabet alphabet; 
	protected Alphabet targetAlphabet;
	protected int numTypes;
	protected int [] typeCounts;
	protected int [] docFreqs;
	protected int [][] invertedIndex;
	protected long corpusWordCount;
	protected int corpusSize;
	protected int [] typeFrequencyIndex;
	protected double [] typeFrequencyCumSum;
	protected double avgDocLen = 0.0;

	protected int [][] finalInvertedIndex = null;  

	StatObj result;

	public StreamCorpusStatistics(InstanceList training) {
		corpusSize = training.size();
		alphabet = training.getDataAlphabet();
		targetAlphabet = training.getTargetAlphabet();
		numTypes = alphabet.size();

		result = training.stream()
				.parallel()
				.reduce(new StatObj(), 
						(statObj, instance) -> StatObj.addInstance(statObj, instance), 
						StatObj::add);

		//		typeFrequencyIndex = IndexSorter.getSortedIndices(atomicIntArrayToIntArray(typeCounts));
		//		typeFrequencyCumSum = calcTypeFrequencyCumSum(typeFrequencyIndex,atomicIntArrayToIntArray(typeCounts));
	}



	//	private double[] calcTypeFrequencyCumSum(int[] typeFrequencyIndex,int[] typeCounts) {
	//		double [] result = new double[typeCounts.length];
	//		int wc = corpusWordCount.get();
	//		result[0] = ((double)typeCounts[typeFrequencyIndex[0]]) / wc;
	//		for (int i = 1; i < typeFrequencyIndex.length; i++) {
	//			result[i] = (((double)typeCounts[typeFrequencyIndex[i]]) / wc) + result[i-1];
	//		}
	//		return result;$
	//	}

	/**
	 * Calculates the query term frequencies, i.e how many times the words in the query occurs in 
	 * the document
	 * @param cs
	 * @param query
	 * @return
	 */
	public static double[] calcQueryTf(StreamCorpusStatistics cs, Instance query, int docIdx) {
		int [][] invertedIndex = cs.getInvertedIndex();
		FeatureSequence tokens = (FeatureSequence) query.getData();
		int queryLength = tokens.size();
		double [] tf = new double[cs.getNumTypes()];
		for (int position = 0; position < queryLength; position++) {
			int type = tokens.getIndexAtPosition(position);
			tf[type] = invertedIndex[type][docIdx];
		}
		return tf;
	}

	public static double [] calcTf(StreamCorpusStatistics cs, Instance instance) {
		FeatureSequence tokens = (FeatureSequence) instance.getData();
		int docLength = tokens.size();
		double [] tf = new double[cs.getNumTypes()];
		for (int position = 0; position < docLength; position++) {
			int type = tokens.getIndexAtPosition(position);
			tf[type] += 1;
		}
		return tf;
	}

	public long size() {
		return result.corpusWordCount;
	}

	public double getAvgDocLen() {
		return result.getAvgDocLen();
	}

	public int getNumTypes() {
		return result.getUniqueTypes();
	}

	/**
	 * Returns an array which contains the number of documents each unique
	 * word type occurs in
	 * 
	 * @return array of word type occurrences 
	 */
	public int [] getDocFreqs() {
		return result.docFreqs;
	}

	/**
	 * Returns an array which contains the number of documents each unique
	 * word type occurs in
	 * 
	 * @return array of word type occurrences 
	 */
	public int [] getTypeCounts() {
		return result.typeCounts;
	}
	
	public int [][] getInvertedIndex() {
		return finalInvertedIndex;
	}

	public Map<Integer,Map<Integer,Integer>> getCoOccurrence() {
		return result.getCoOccurrences();
	}

	public double getNormalizedCoOccurrence(int type1, int type2) {
		int fOccur = getForwardCoOccurrence(type1, type2);
		if(fOccur==0) {
			// Switch the words around and calculate occurrence
			int tmp = type1;
			type1 = type2;
			type2 = tmp;
			fOccur = getForwardCoOccurrence(type1,type2);
		}
		// Type1 can now be switched, but that's ok, and how we want it...
		return fOccur / (double) getTypeCounts()[type1];
	}

	public int getCoOccurrence(int type1, int type2) {
		return getForwardCoOccurrence(type1, type2) + getForwardCoOccurrence(type2,type1);
	}

	public int getForwardCoOccurrence(int type1, int type2) {
		if(!result.getCoOccurrences().containsKey(type1)) {
			return 0;
		} else {
			if(!result.getCoOccurrences().get(type1).containsKey(type2)) {
				return 0;
			} else {
				return result.getCoOccurrences().get(type1).get(type2);
			}
		}
	}



}
