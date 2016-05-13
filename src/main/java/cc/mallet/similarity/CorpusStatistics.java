package cc.mallet.similarity;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicInteger;

import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.util.IndexSorter;

public class CorpusStatistics {

	protected Alphabet alphabet; 
	protected Alphabet targetAlphabet;
	protected int numTypes;
	protected AtomicInteger [] typeCounts;
	protected AtomicInteger [] docFreqs;
	protected AtomicInteger [][] invertedIndex;
	protected AtomicInteger corpusWordCount = new AtomicInteger();
	protected int corpusSize;
	protected int [] typeFrequencyIndex;
	protected double [] typeFrequencyCumSum;
	protected double avgDocLen = 0.0;
	private ForkJoinPool statisticsCollectionPool;
	
	protected int [][] finalInvertedIndex = null;  
	
	class RecursiveStatisticsCollector extends RecursiveAction {
		final static long serialVersionUID = 1L;
		InstanceList dataset;
		int startDoc = -1;
		int endDoc = -1;
		int limit = 100;
		int myBatch = -1;

		public RecursiveStatisticsCollector(int startDoc, int endDoc, int ll, InstanceList instances) {
			this.limit = ll;
			this.startDoc = startDoc;
			this.endDoc = endDoc;
			this.dataset = instances;
		}

		@Override
		protected void compute() {
			if ( (endDoc-startDoc) <= limit ) {
				for (int docIdx = startDoc; docIdx < endDoc; docIdx++) {
					collect(dataset.get(docIdx),docIdx);
				}
			}
			else {
				int range = (endDoc-startDoc);
				int startDoc1 = startDoc;
				int endDoc1 = startDoc + (range / 2);
				int startDoc2 = endDoc1;
				int endDoc2 = endDoc;
				invokeAll(new RecursiveStatisticsCollector(startDoc1,endDoc1,limit,dataset),
						new RecursiveStatisticsCollector(startDoc2,endDoc2,limit,dataset));
			}
		}
	}
		
	public CorpusStatistics(InstanceList training) {
		corpusSize = training.size();
		alphabet = training.getDataAlphabet();
		targetAlphabet = training.getTargetAlphabet();
		numTypes = alphabet.size();
		typeCounts = new AtomicInteger[numTypes];
		docFreqs = new AtomicInteger[numTypes];
		for (int i = 0; i < docFreqs.length; i++) {
			docFreqs[i] = new AtomicInteger();
			typeCounts[i] = new AtomicInteger();
		}
		invertedIndex = new AtomicInteger[numTypes][];
		for (int i = 0; i < invertedIndex.length; i++) {
			invertedIndex[i] = new AtomicInteger[corpusSize];
			for (int j = 0; j < invertedIndex[i].length; j++) {
				invertedIndex[i][j] = new AtomicInteger();
			}
		}

		statisticsCollectionPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
		RecursiveStatisticsCollector dslr = new RecursiveStatisticsCollector(0,training.size(),200,training);                
		statisticsCollectionPool.invoke(dslr);
		
		finalInvertedIndex = new int[numTypes][];
		for (int i = 0; i < finalInvertedIndex.length; i++) {
			finalInvertedIndex[i] = atomicIntArrayToIntArray(invertedIndex[i]);
		}

		avgDocLen = corpusWordCount.get() / ((double) corpusSize);

		typeFrequencyIndex = IndexSorter.getSortedIndices(atomicIntArrayToIntArray(typeCounts));
		typeFrequencyCumSum = calcTypeFrequencyCumSum(typeFrequencyIndex,atomicIntArrayToIntArray(typeCounts));
	}

	private int[] atomicIntArrayToIntArray(AtomicInteger[] atomicIntArray) {
		int [] result  = new int[atomicIntArray.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = atomicIntArray[i].get();
		}
		return result;
	}

	void collect(Instance instance, int docNo) {
		boolean [] reportedType = new boolean[numTypes];
		FeatureSequence tokens = (FeatureSequence) instance.getData();
		int docLength = tokens.size();
		corpusWordCount.addAndGet(docLength);

		for (int position = 0; position < docLength; position++) {
			int type = tokens.getIndexAtPosition(position);
			typeCounts[type].incrementAndGet();
			if(!reportedType[type]) {
				docFreqs[type].incrementAndGet();
				reportedType[type] = true;
			}
			invertedIndex[type][docNo].incrementAndGet();
		}
	}
	
	private double[] calcTypeFrequencyCumSum(int[] typeFrequencyIndex,int[] typeCounts) {
		double [] result = new double[typeCounts.length];
		int wc = corpusWordCount.get();
		result[0] = ((double)typeCounts[typeFrequencyIndex[0]]) / wc;
		for (int i = 1; i < typeFrequencyIndex.length; i++) {
			result[i] = (((double)typeCounts[typeFrequencyIndex[i]]) / wc) + result[i-1];
		}
		return result;
	}
	
	/**
	 * Calculates the query term frequencies, i.e how many times the words in the query occurs in 
	 * the document
	 * @param cs
	 * @param query
	 * @return
	 */
	public static double[] calcQueryTf(CorpusStatistics cs, Instance query, int docIdx) {
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

	public static double [] calcTf(CorpusStatistics cs, Instance instance) {
		FeatureSequence tokens = (FeatureSequence) instance.getData();
		int docLength = tokens.size();
		double [] tf = new double[cs.getNumTypes()];
		for (int position = 0; position < docLength; position++) {
			int type = tokens.getIndexAtPosition(position);
			tf[type] += 1;
		}
		return tf;
	}

	public double size() {
		return corpusSize;
	}

	public double getAvgDocLen() {
		return avgDocLen;
	}

	public int getNumTypes() {
		return numTypes;
	}

	public int [] getDocFreqs() {
		return atomicIntArrayToIntArray(docFreqs);
	}
	
	public int [][] getInvertedIndex() {
		return finalInvertedIndex;
	}

}
