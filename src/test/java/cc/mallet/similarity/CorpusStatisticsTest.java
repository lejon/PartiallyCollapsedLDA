package cc.mallet.similarity;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;

import org.junit.Before;
import org.junit.Test;

import cc.mallet.types.Alphabet;
import cc.mallet.types.InstanceList;
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
