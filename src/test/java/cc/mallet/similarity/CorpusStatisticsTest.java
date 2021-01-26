package cc.mallet.similarity;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;

import org.junit.Before;
import org.junit.Test;

import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.util.LDADatasetStringLoadingUtils;
import cc.mallet.util.LDAUtils;

public class CorpusStatisticsTest {

	InstanceList instances;
	CorpusStatistics cs; 

	@Before
	public void noSetup() throws FileNotFoundException {
		instances = LDAUtils.loadInstances("src/main/resources/datasets/tfidf-samples.txt", null, 0);
		cs = new CorpusStatistics(instances); 
	}

	@Test
	public void testAvgDocLenStream() throws FileNotFoundException, InterruptedException  {
		//InstanceList ng20 = LDAUtils.loadInstances("src/main/resources/datasets/SmallTexts.txt", null, 0);
		InstanceList ng20 = LDAUtils.loadInstances("src/main/resources/datasets/20newsgroups.txt", null, 0);
		StreamCorpusStatistics cs = new StreamCorpusStatistics(ng20);

		long cc = 0;
		for(Instance instance : ng20) {
			FeatureSequence tokenSequence =
					(FeatureSequence) instance.getData();
			cc += tokenSequence.getFeatures().length;
		}

		//System.out.println("No tokens: " + cc);
		//System.out.println("Num types: " + ng20.getAlphabet().size());

		assertEquals(ng20.size(),cs.corpusSize);
		assertEquals(ng20.getAlphabet().size(),cs.getNumTypes());
		assertEquals(cc,cs.size());
		assertEquals(cc/(double)ng20.size(),cs.getAvgDocLen(),0.0000001);
	}

	@Test
	public void testDocFreqsStream() throws FileNotFoundException, InterruptedException  {
		String [] doclines = {
				"'INSERT DISK THREE' ? But I can only get two in the drive !", 
				"'Intel Inside intel' is a Government Warning required by Law.",
				"'Intel Inside': The world's most widely used warning label.",
				"A Freudian slip is when you say one thing but mean your mother",
				"A backward poet writes inverse."
		};

		String [] classNames = new String [doclines.length];
		for (int i = 0; i < classNames.length; i++) {
			classNames[i] = "X";
		}

		InstanceList small = LDADatasetStringLoadingUtils.loadInstancesStrings(doclines, classNames);

		StreamCorpusStatistics cs = new StreamCorpusStatistics(small);

		int freudianIdx = small.getAlphabet().lookupIndex("freudian");
		//System.out.println("freudian is at:" + freudianIdx);

		assertEquals(1,cs.getDocFreqs()[freudianIdx]);

		int intelIdx = small.getAlphabet().lookupIndex("intel");
		//System.out.println("Intel is at:" + intelIdx);

		assertEquals(2,cs.getDocFreqs()[intelIdx]);

		int aIdx = small.getAlphabet().lookupIndex("a");
		//System.out.println("a is at:" + aIdx);

		assertEquals(3,cs.getDocFreqs()[aIdx]);

		int insideIdx = small.getAlphabet().lookupIndex("inside");
		assertEquals(3,cs.getCoOccurrence(intelIdx, insideIdx));
		assertEquals(3,cs.getCoOccurrence(insideIdx,intelIdx));

		assertEquals(1,cs.getCoOccurrence(aIdx, freudianIdx));
		assertEquals(1,cs.getCoOccurrence(freudianIdx,aIdx));

		assertEquals(0,cs.getCoOccurrence(aIdx, intelIdx));
		assertEquals(0,cs.getCoOccurrence(intelIdx,aIdx));

	}

	@Test
	public void testAvgDocLen()  {
		assertEquals(6,cs.getAvgDocLen(),0.0000001);
	}

	@Test
	public void testCorpusSize() throws FileNotFoundException {
		assertEquals(2,cs.size(),0.0000001);
	}

	@Test
	public void testNumTypes() throws FileNotFoundException {
		assertEquals(6,cs.getNumTypes());
	}

	@Test
	public void testInvertedIndex() throws FileNotFoundException {
		int [][] invertedIndex = cs.getInvertedIndex();
		Alphabet dataAlphabet = instances.getDataAlphabet();
		assertEquals(1,invertedIndex[dataAlphabet.lookupIndex("this")][0]);
		assertEquals(1,invertedIndex[dataAlphabet.lookupIndex("this")][1]);
		assertEquals(1,invertedIndex[dataAlphabet.lookupIndex("is")][0]);
		assertEquals(1,invertedIndex[dataAlphabet.lookupIndex("is")][1]);
		assertEquals(1,invertedIndex[dataAlphabet.lookupIndex("sample")][0]);
		assertEquals(0,invertedIndex[dataAlphabet.lookupIndex("sample")][1]);
		assertEquals(0,invertedIndex[dataAlphabet.lookupIndex("another")][0]);
		assertEquals(2,invertedIndex[dataAlphabet.lookupIndex("another")][1]);
		assertEquals(3,invertedIndex[dataAlphabet.lookupIndex("example")][1]);
	}

	@Test
	public void testDocFreqs() throws FileNotFoundException {
		int [] freqs = cs.getDocFreqs();
		Alphabet dataAlphabet = instances.getDataAlphabet();
		assertEquals(2,freqs[dataAlphabet.lookupIndex("this")]);
		assertEquals(2,freqs[dataAlphabet.lookupIndex("is")]);
		assertEquals(1,freqs[dataAlphabet.lookupIndex("sample")]);
		assertEquals(1,freqs[dataAlphabet.lookupIndex("another")]);
		assertEquals(1,freqs[dataAlphabet.lookupIndex("example")]);
	}
}
