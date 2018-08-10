package cc.mallet.topics;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

public class DocTopicTokenFreqTableTest {

	@Test
	public void testEmptyTable() {
		int numTopics = 3;
		DocTopicTokenFreqTable docTopicTokenFreqTable = new DocTopicTokenFreqTable(numTopics);
		
		int [][] expectedReverseHist = {{}, {},{}};
		
		for (int topic = 0; topic < numTopics; topic++) {
//			System.out.println("Expected: " 
//					+ Arrays.toString(expectedReverseHist[topic]) 
//					+ " Actual: " 
//					+ Arrays.toString(docTopicTokenFreqTable.getReverseCumulativeSum(topic)));
			Assert.assertArrayEquals(expectedReverseHist[topic], docTopicTokenFreqTable.getReverseCumulativeSum(topic));
		}
	}

	@Test
	public void testApi() {
		int numTopics = 3;
		DocTopicTokenFreqTable docTopicTokenFreqTable = new DocTopicTokenFreqTable(numTopics);
		int [][] documentLocalTopicCounts = {{1,0,0}, {2,0,0}, {2,0,0}};
		int numDocs = documentLocalTopicCounts.length;

		for (int docNo = 0; docNo < numDocs; docNo++) {
			int [] localTopicCounts = documentLocalTopicCounts[docNo];
			for (int i = 0; i < numTopics; i++) {
				if(localTopicCounts[i]!=0) {
					docTopicTokenFreqTable.increment(i,(int)localTopicCounts[i]);
				}
			}
		}

		System.out.println("Table:" + docTopicTokenFreqTable);
		System.out.println("Cumsum: " + Arrays.toString(docTopicTokenFreqTable.getReverseCumulativeSum(0)));
		// Three documents have more than 1 (-1 = 0) topic indicators for the first topic (0) 
		assertEquals(3,docTopicTokenFreqTable.getReverseCumulativeSum(0)[0]);
		// Two documents have more than 2 (-1 = 1) topic indicators for the first topic (0)
		assertEquals(2,docTopicTokenFreqTable.getReverseCumulativeSum(0)[1]);
	}

	
	@Test
	public void test3Docs() {
		int numTopics = 3;
		DocTopicTokenFreqTable docTopicTokenFreqTable = new DocTopicTokenFreqTable(numTopics);
		int numDocs = 3;
		int [][] documentLocalTopicCounts = {{0,5,1}, {1,1,0},{3,1,1}};

		for (int docNo = 0; docNo < numDocs; docNo++) {
			int [] localTopicCounts = documentLocalTopicCounts[docNo];
			for (int i = 0; i < numTopics; i++) {
				if(localTopicCounts[i]!=0) {
					docTopicTokenFreqTable.increment(i,(int)localTopicCounts[i]);
				}
			}
		}
				
		int [][] expectedReverseHist = {{2,1,1}, {3,1,1,1,1},{2}};
		
		for (int topic = 0; topic < numTopics; topic++) {
//			System.out.println("Expected: " 
//					+ Arrays.toString(expectedReverseHist[topic]) 
//					+ " Actual: " 
//					+ Arrays.toString(docTopicTokenFreqTable.getReverseCumulativeSum(topic)));
			Assert.assertArrayEquals(expectedReverseHist[topic], docTopicTokenFreqTable.getReverseCumulativeSum(topic));
		}
	}
	
	@Test
	public void testEmptyTopics() {
		int numTopics = 5;
		DocTopicTokenFreqTable docTopicTokenFreqTable = new DocTopicTokenFreqTable(numTopics);
		int numDocs = 3;
		int [][] documentLocalTopicCounts = {{0,0,5,1,0}, {1,0,1,0,0},{3,0,1,1,0}};

		for (int docNo = 0; docNo < numDocs; docNo++) {
			int [] localTopicCounts = documentLocalTopicCounts[docNo];
			for (int i = 0; i < numTopics; i++) {
				if(localTopicCounts[i]!=0) {
					docTopicTokenFreqTable.increment(i,(int)localTopicCounts[i]);
				}
			}
		}
				
		int [][] expectedReverseHist = {{2,1,1}, {}, {3,1,1,1,1},{2}, {}};
		
		for (int topic = 0; topic < numTopics; topic++) {
//			System.out.println("Expected: " 
//					+ Arrays.toString(expectedReverseHist[topic]) 
//					+ " Actual: " 
//					+ Arrays.toString(docTopicTokenFreqTable.getReverseCumulativeSum(topic)));
			Assert.assertArrayEquals(expectedReverseHist[topic], docTopicTokenFreqTable.getReverseCumulativeSum(topic));
		}
	}
	
	@Test
	public void testGetEmptyTopics() {
		int numTopics = 5;
		DocTopicTokenFreqTable docTopicTokenFreqTable = new DocTopicTokenFreqTable(numTopics);
		int numDocs = 3;
		int [][] documentLocalTopicCounts = {{0,0,5,1,0}, {1,0,1,0,0},{3,0,1,1,0}};

		for (int docNo = 0; docNo < numDocs; docNo++) {
			int [] localTopicCounts = documentLocalTopicCounts[docNo];
			for (int i = 0; i < numTopics; i++) {
				if(localTopicCounts[i]!=0) {
					docTopicTokenFreqTable.increment(i,(int)localTopicCounts[i]);
				}
			}
		}
				
		int [] expectedEmptyTopics = {1,4};
		
		for (int topic = 0; topic < numTopics; topic++) {
//			System.out.println("Expected: " 
//					+ Arrays.toString(expectedReverseHist[topic]) 
//					+ " Actual: " 
//					+ Arrays.toString(docTopicTokenFreqTable.getReverseCumulativeSum(topic)));
			Assert.assertArrayEquals(expectedEmptyTopics, docTopicTokenFreqTable.getEmptyTopics());
		}
	}

}
