package cc.mallet.topics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.junit.Assert;
import org.junit.Test;

public class DocTopicTokenFreqTableTest {

	@Test
	public void testEmptyTable() {
		int numTopics = 3;
		DocTopicTokenFreqTable docTopicTokenFreqTable = new DocTopicTokenFreqTable(numTopics,0);
		
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
		DocTopicTokenFreqTable docTopicTokenFreqTable = new DocTopicTokenFreqTable(numTopics,2);
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

		//System.out.println("Table:" + docTopicTokenFreqTable);
		//System.out.println("Cumsum: " + Arrays.toString(docTopicTokenFreqTable.getReverseCumulativeSum(0)));
		// Three documents have more than 1 (-1 = 0) topic indicators for the first topic (0) 
		assertEquals(3,docTopicTokenFreqTable.getReverseCumulativeSum(0)[0]);
		// Two documents have more than 2 (-1 = 1) topic indicators for the first topic (0)
		assertEquals(2,docTopicTokenFreqTable.getReverseCumulativeSum(0)[1]);
	}

	
	@Test
	public void test3Docs() {
		int numTopics = 3;
		DocTopicTokenFreqTable docTopicTokenFreqTable = new DocTopicTokenFreqTable(numTopics,6);
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
		DocTopicTokenFreqTable docTopicTokenFreqTable = new DocTopicTokenFreqTable(numTopics,6);
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
		DocTopicTokenFreqTable docTopicTokenFreqTable = new DocTopicTokenFreqTable(numTopics,6);
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
	
	@Test
	public void testReset() {
		int numTopics = 5;
		DocTopicTokenFreqTable docTopicTokenFreqTable = new DocTopicTokenFreqTable(numTopics,6);
		int numDocs = 3;
		
		{
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
			
			
			int [][] expectedReverseHist = {{2,1,1}, {}, {3,1,1,1,1}, {2}, {}};
			
			for (int topic = 0; topic < numTopics; topic++) {
				Assert.assertArrayEquals(expectedReverseHist[topic], docTopicTokenFreqTable.getReverseCumulativeSum(topic));
			}

		}
		docTopicTokenFreqTable.reset();
		
		{
			int [][] documentLocalTopicCounts2 = {{0,5,1,0,0}, {0,1,0,0,1},{0,1,1,0,3}};

			for (int docNo = 0; docNo < numDocs; docNo++) {
				int [] localTopicCounts = documentLocalTopicCounts2[docNo];
				for (int i = 0; i < numTopics; i++) {
					if(localTopicCounts[i]!=0) {
						docTopicTokenFreqTable.increment(i,(int)localTopicCounts[i]);
					}
				}
			}

			int [] expectedEmptyTopics = {0,3};

			for (int topic = 0; topic < numTopics; topic++) {
				Assert.assertArrayEquals(expectedEmptyTopics, docTopicTokenFreqTable.getEmptyTopics());
			}
			
			int [][] expectedReverseHist = {{}, {3,1,1,1,1}, {2}, {}, {2,1,1}};
			
			for (int topic = 0; topic < numTopics; topic++) {
				Assert.assertArrayEquals(expectedReverseHist[topic], docTopicTokenFreqTable.getReverseCumulativeSum(topic));
			}
		}

	}
	
	
	@Test
	public void testFreqChange() {
		int numTopics = 5;
		DocTopicTokenFreqTable docTopicTokenFreqTable = new DocTopicTokenFreqTable(numTopics,6);
		int numDocs = 1;
		
		int [][] documentLocalTopicCounts2 = {{7,0,0,0,3}};

		for (int docNo = 0; docNo < numDocs; docNo++) {
			int [] localTopicCounts = documentLocalTopicCounts2[docNo];
			for (int i = 0; i < numTopics; i++) {
				if(localTopicCounts[i]!=0) {
					try {
						docTopicTokenFreqTable.increment(i,(int)localTopicCounts[i]);
					} catch(Exception e) {
						return;
					}
					fail("Should throw exception since frequency of doc (7) exceeds max in table (6)");
				}
			}
		}
	}

	@Test
	public void testSampleLOneDocAnalytic() {
		int numTopics = 5;
		DocTopicTokenFreqTable docTopicTokenFreqTable = new DocTopicTokenFreqTable(numTopics,12);
		int numDocs = 1;
		int [][] documentLocalTopicCounts = {{3,3,3,2,1}};
		int maxDocLen = 0;
		for (int i = 0; i < documentLocalTopicCounts.length; i++) {
			maxDocLen += documentLocalTopicCounts[0][i];
		}

		for (int docNo = 0; docNo < numDocs; docNo++) {
			int [] localTopicCounts = documentLocalTopicCounts[docNo];
			for (int i = 0; i < numTopics; i++) {
				if(localTopicCounts[i]!=0) {
					docTopicTokenFreqTable.increment(i,(int)localTopicCounts[i]);
				}
			}
		}

		double [] psi = {0.5,0.2,0.1,0.1,0.1};
		double alpha = 1;	
		// 		
		// True values computed using the Anoniak distribution (9) in paper
		// Proportion (given local topic counts and psi) of the number of times
		// we have 1, 2, 3 tables
		double [][] trueProp = {
				{0.53333333, 0.40000000, 0.06666667}, 
				{0.75757576, 0.22727273, 0.01515152}, 
				{0.865800866, 0.129870130, 0.0043290041}, 
				{0.90909091, 0.09090909}, 
				{1}};
		int sampleSize = 10000;
		long [][] lSamples = new long[sampleSize][numTopics];
		for (int i = 0; i < sampleSize; i++) {
			for (int topic = 0; topic < numTopics; topic++) {
				// l_k is the number of tables
				int l_k = PoissonPolyaUrnHDPLDA.sampleL(topic, maxDocLen, docTopicTokenFreqTable, alpha, psi[topic]);
				lSamples[i][topic] = l_k;
			}
		}

		for (int topic = 0; topic < numTopics; topic++) {
			long [] props = new long[documentLocalTopicCounts[0][topic]];
			for (int sampleNr = 0; sampleNr < sampleSize; sampleNr++) {
				// Sampled l_k is the number of tables (1-3 in this case), so we have to subtract 1
				// when we use l_l as an index...
				props[(int)lSamples[sampleNr][topic]-1]++;
			}

			if(trueProp[topic].length==1) {
				assertEquals(sampleSize, props[0]);
			} else {
				ChiSquareTest cs = new ChiSquareTest();
				double test1 = cs.chiSquareTest(trueProp[topic], props);
				if(!(test1 > 0.01)) {
					System.out.println("Test:" + test1);
					System.err.println("TEST FAILS");
				} 
				assertTrue(test1 > 0.01);
			}
		}
	}

	@Test
	public void testSampleLSimR() {
		int numTopics = 5;
		DocTopicTokenFreqTable docTopicTokenFreqTable = new DocTopicTokenFreqTable(numTopics,7);
		int [][] documentLocalTopicCounts = {{2,1,3,1,0}, {3,0,1,0,0}, {0,0,1,1,0}};
		int numDocs = documentLocalTopicCounts.length;
		int maxDocLen = 0;
		for (int i = 0; i < documentLocalTopicCounts.length; i++) {
			int tmpLen = 0;
			for (int j = 0; j < documentLocalTopicCounts[i].length; j++) {				
				tmpLen += documentLocalTopicCounts[i][j];
			}
			if(tmpLen>maxDocLen) maxDocLen = tmpLen;
		}

		for (int docNo = 0; docNo < numDocs; docNo++) {
			int [] localTopicCounts = documentLocalTopicCounts[docNo];
			for (int i = 0; i < numTopics; i++) {
				if(localTopicCounts[i]!=0) {
					docTopicTokenFreqTable.increment(i,(int)localTopicCounts[i]);
				}
			}
		}

//		System.out.println("Freq table: \n" + docTopicTokenFreqTable);
//		System.out.println("Reverse cumsum 0: " + Arrays.toString(docTopicTokenFreqTable.getReverseCumulativeSum(0)));
		double [] psi = {0.1,0.1,0.1,0.1,0.1};
		double alpha = 1;

		int sampleSize = 10000;
		long [][] lSamples = new long[sampleSize][numTopics];
		for (int i = 0; i < sampleSize; i++) {
			for (int topic = 0; topic < numTopics; topic++) {
				// l_k is the number of tables
				int l_k = PoissonPolyaUrnHDPLDA.sampleL(topic, maxDocLen, docTopicTokenFreqTable, alpha, psi[topic]);
				lSamples[i][topic] = l_k;
			}
		}

		double [][] trueProp = {{0.787784, 0.196259, 0.015583, 0.000374},
				{1}, 
				{0.865271, 0.130358, 0.004371},
				{0, 1},
				{0}
		}; // Based on simulations so accept larger deviations
		
		// ### Topic 0
		long [] props = new long[maxDocLen];
		for (int sampleNr = 0; sampleNr < sampleSize; sampleNr++) {
			// Sampled l_k is the number of tables (1-3 in this case), so we have to subtract 1
			// when we use l_l as an index...
			props[(int)lSamples[sampleNr][0]-1]++;
		}
		long [] nonZeroCounts = new long[4];
		for (int i = 0; i < nonZeroCounts.length; i++) {
			// The first slot will be 0 so add +1
			nonZeroCounts[i] = props[i+1];
		} 

//		System.out.println("Props: " + Arrays.toString(nonZeroCounts));
		ChiSquareTest cs = new ChiSquareTest();
		double test1 = cs.chiSquareTest(trueProp[0], nonZeroCounts);
		if(!(test1 > 0.01)) {
			System.out.println("Test:" + test1);
			System.err.println("TEST FAILS");
		} 
		assertTrue(test1 > 0.01);
		
		// ### Topic 1
		props = new long[maxDocLen];
		for (int sampleNr = 0; sampleNr < sampleSize; sampleNr++) {
			// Sampled l_k is the number of tables (1-3 in this case), so we have to subtract 1
			// when we use l_l as an index...
			props[(int)lSamples[sampleNr][1]-1]++;
		}
//		System.out.println("Props: " + Arrays.toString(props));
		assertEquals(sampleSize, props[0]);
		
		// ### Topic 2
		props = new long[maxDocLen];
		for (int sampleNr = 0; sampleNr < sampleSize; sampleNr++) {
			// Sampled l_k is the number of tables (1-3 in this case), so we have to subtract 1
			// when we use l_l as an index...
			props[(int)lSamples[sampleNr][2]-1]++;
		}
		nonZeroCounts = new long[3];
		for (int i = 0; i < nonZeroCounts.length; i++) {
			// The two first slots will be 0 so add +2
			nonZeroCounts[i] = props[i+2];
		} 

//		System.out.println("Props: " + Arrays.toString(nonZeroCounts));
		cs = new ChiSquareTest();
		test1 = cs.chiSquareTest(trueProp[2], nonZeroCounts);
		if(!(test1 > 0.01)) {
			System.out.println("Test:" + test1);
			System.err.println("TEST FAILS");
		} 
		assertTrue(test1 > 0.01);
		
		// ### Topic 3
		props = new long[maxDocLen];
		for (int sampleNr = 0; sampleNr < sampleSize; sampleNr++) {
			// Sampled l_k is the number of tables (1-3 in this case), so we have to subtract 1
			// when we use l_l as an index...
			props[(int)lSamples[sampleNr][3]-1]++;
		}
//		System.out.println("Props: " + Arrays.toString(props));
		assertEquals(0, props[0]);
		assertEquals(sampleSize, props[1]);
		
		
		// ### Topic 4
		long sum = 0;
		for (int sampleNr = 0; sampleNr < sampleSize; sampleNr++) {
			// Sampled l_k is the number of tables (1-3 in this case), so we have to subtract 1
			// when we use l_l as an index...
			sum += lSamples[sampleNr][4];
		}
//		System.out.println("Sum: " + sum);
		assertEquals(0, sum);
	}

	@Test
	public void testSampleL() {
		int numTopics = 5;
		DocTopicTokenFreqTable docTopicTokenFreqTable = new DocTopicTokenFreqTable(numTopics,6);
		int numDocs = 3;
		int [][] documentLocalTopicCounts = {{0,0,5,1,0}, {1,0,1,0,0},{3,0,1,1,0}};
		int maxDocLen = 0;
		for (int i = 0; i < documentLocalTopicCounts.length; i++) {
			int tmpLen = 0;
			for (int j = 0; j < documentLocalTopicCounts[i].length; j++) {				
				tmpLen += documentLocalTopicCounts[i][j];
			}
			if(tmpLen>maxDocLen) maxDocLen = tmpLen;
		}

		for (int docNo = 0; docNo < numDocs; docNo++) {
			int [] localTopicCounts = documentLocalTopicCounts[docNo];
			for (int i = 0; i < numTopics; i++) {
				if(localTopicCounts[i]!=0) {
					docTopicTokenFreqTable.increment(i,(int)localTopicCounts[i]);
				}
			}
		}

		int topic = 0;
//		System.out.println("Freq table: \n" + docTopicTokenFreqTable);
//		System.out.println("Reverse cumsum 0: " + Arrays.toString(docTopicTokenFreqTable.getReverseCumulativeSum(topic)));
		double psi = 0.1;
		double alpha = 1;

		int [] lSamples = new int[200];
		for (int i = 0; i < 2000; i++) {
			int l_k = PoissonPolyaUrnHDPLDA.sampleL(topic, maxDocLen, docTopicTokenFreqTable, alpha, psi);
			lSamples[l_k]++;
		}

//		System.out.println("Samples: " + Arrays.toString(lSamples));
	}

}
