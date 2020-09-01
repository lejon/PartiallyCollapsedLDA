package cc.mallet.topics.tui;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class LoglikelihoodCalculatorTest {

	@Test
	public void test() {
		int M = 2;
		int V = 3;
		int numTopics; 

		int[][] zs = {{0,2,2,3}, {3,1,1,1}};
		int[][] w  = {{1,1,0,2}, {2,2,0,0}};
		Map<Integer,String> vocab = new HashMap<>();
		vocab.put(0, "a");
		vocab.put(1, "b");
		vocab.put(2, "c");
		numTopics = LoglikelihoodCalculator.findNumTopics(zs);
		
		assertEquals(4,numTopics);
		
		LoglikelihoodCalculator llc = new LoglikelihoodCalculator(numTopics, w, vocab, zs);
		
		int [][] nmk = new int[M][numTopics];
		int [][] nkt = new int[numTopics][V];
		int [] nk = new int[numTopics];
		
		for( int i = 0; i < zs.length; i++) {
			int [] row = zs[i];
			llc.updateLocalTopicCounts(nmk[i],row);
			llc.updateTypeTopicMatrix(nkt, w[i], row);
			llc.updateTopicCounts(nk,row);
		}

		int [] nke = {1,3,2,2};
		assertArrayEquals(nk, nke);

		int [] nmk0 = {1,0,2,1};
		assertArrayEquals(nmk0, nmk[0]);
		int [] nmk1 = {0,3,0,1};
		assertArrayEquals(nmk1, nmk[1]);
		
		int [] nk0t = {0,1,0};
		assertArrayEquals(nk0t, nkt[0]);
		int [] nk1t = {2,0,1};
		assertArrayEquals(nk1t, nkt[1]);
		
		System.out.println(llc.calcLL(0.1, 0.01));
	}
	
}
