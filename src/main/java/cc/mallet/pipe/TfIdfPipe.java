package cc.mallet.pipe;

import gnu.trove.TIntDoubleHashMap;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureCounter;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.IDSorter;
import cc.mallet.types.Instance;

public class TfIdfPipe extends Pipe {

	// Counter for IDF
	FeatureCounter counter;
	// Counter for TF per document
	FeatureCounter tfs;

	int corpusSize;

	static final long serialVersionUID = 1;

	public TfIdfPipe() {
		super(new Alphabet(), null);

		counter = new FeatureCounter(this.getDataAlphabet());
		tfs = new FeatureCounter(this.getDataAlphabet());
	}

	public TfIdfPipe(Alphabet dataDict, Alphabet targetDict) {
		super(dataDict, targetDict);

		counter = new FeatureCounter(dataAlphabet);
		tfs = new FeatureCounter(dataAlphabet);
	}

	public Instance pipe(Instance instance) {
		gnu.trove.TIntIntHashMap idfIncrementedForIstance = new gnu.trove.TIntIntHashMap();

		if (instance.getData() instanceof FeatureSequence) {
			FeatureSequence features = (FeatureSequence) instance.getData();
			for (int position = 0; position < features.size(); position++) {
				int featureIndex = features.getIndexAtPosition(position);
				// If we have not already incremented this feature for this document...
				if(idfIncrementedForIstance.get(featureIndex)==0) {
					// ...then do so, i.e one more document contained this feature
					counter.increment(featureIndex);
					idfIncrementedForIstance.put(featureIndex,1);
				}
				// Increment total feature count
				tfs.increment(featureIndex);
			}
		}
		else {
			throw new IllegalArgumentException("Looking for a FeatureSequence, found a " + 
					instance.getData().getClass());
		}

		corpusSize++;
		return instance;
	}

	public int getTf(int featureIdx) {
		return tfs.get(featureIdx);
	}

	public int getIdf(int featureIdx) {
		return counter.get(featureIdx);
	}

	public TIntDoubleHashMap getTfIdf() {
		TIntDoubleHashMap tfidfs = new TIntDoubleHashMap();

		for(int featureIdx = 0; featureIdx <  getDataAlphabet().size(); featureIdx++ ) {
			int tf = getTf(featureIdx);
			int idf = getIdf(featureIdx);

			double tfIdf = (tf == 0 || idf == 0) ? 0.0 : (double) tf * Math.log(corpusSize / (double) idf);
			tfidfs.put(featureIdx, tfIdf);
		}

		return tfidfs;
	}

	public int [] freqSortWords(TIntDoubleHashMap tfIdfs, Alphabet dataAlphabet) {
		IDSorter[] sortedWords = new IDSorter[dataAlphabet.size()];

		for (int type = 0; type < dataAlphabet.size(); type++) {
			sortedWords[type] = new IDSorter(type, tfIdfs.get(type));
		}

		java.util.Arrays.sort(sortedWords);

		int [] ranks = new int[sortedWords.length];

		for (int i = 0; i < ranks.length; i++) {
			ranks[i] = sortedWords[i].getID();
		}

		return ranks;
	}

	public int getFreqPos(int feature, int [] ranks) {
		for (int rank = 0; rank < ranks.length; rank++) {
			if(feature == ranks[rank]) {
				return rank;
			}
		}
		throw new IllegalArgumentException("Could not find feature index: " + feature + " in sorted words...");
	}

	/**
	 * Returns a new alphabet that contains minimumCount number of words 
	 */
	public Alphabet getPrunedAlphabet(int minimumCount) {

		Alphabet currentAlphabet = getDataAlphabet();
		Alphabet prunedAlphabet = new Alphabet();

		TIntDoubleHashMap tfidfs = getTfIdf();
		int [] ranks = freqSortWords(tfidfs, currentAlphabet);

		for (int i = 0; i < minimumCount && i < ranks.length; i++) {
			int feature = ranks[i];
			prunedAlphabet.lookupIndex(currentAlphabet.lookupObject(feature));
		}

		prunedAlphabet.stopGrowth();
		return prunedAlphabet;

	}

	/** 
	 *  Writes a list of features that do not occur at or 
	 *  above the specified cutoff to the pruned file, one per line.
	 *  This file can then be passed to a stopword filter as 
	 *  "additional stopwords".
	 */
	public void writePrunedWords(File prunedFile, int minimumCount) throws IOException {

		PrintWriter out = new PrintWriter(prunedFile);

		Alphabet currentAlphabet = getDataAlphabet();

		TIntDoubleHashMap tfidfs = getTfIdf();
		int [] ranks = freqSortWords(tfidfs, currentAlphabet);

		for (int i = minimumCount; i < ranks.length; i++) {
			int feature = ranks[i];
			out.println(currentAlphabet.lookupObject(feature));
		}

		out.close();
	}

	/** 
	 *  Add all pruned words to the internal stoplist of a SimpleTokenizer.
	 */
	public void addPrunedWordsToStoplist(SimpleTokenizer tokenizer, int minimumCount) {
		Alphabet currentAlphabet = getDataAlphabet();

		TIntDoubleHashMap tfidfs = getTfIdf();
		int [] ranks = freqSortWords(tfidfs, currentAlphabet);

		for (int i = minimumCount; i < ranks.length; i++) {
			int feature = ranks[i];
			tokenizer.stop((String) currentAlphabet.lookupObject(feature));
		}
	}
}
