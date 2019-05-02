package cc.mallet.topics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Random;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.junit.Ignore;
import org.junit.Test;

import cc.mallet.types.PolyaUrnDirichlet;
import cc.mallet.util.OptimizedGentleAliasMethod;
import cc.mallet.util.WalkerAliasTable;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;

public class PoissonPolyaUrnTest {

	@Test
	public void testCreateTopicTranslationTable() {
		int numTopics = 23;
		int newNumTopics = 18;
		int activeInData = 12; 
		boolean[] activeTopics = {true,true,true,true,true,true,false,true,false,true,true,false,true,false,false,true,false,false,false,false,false,true,false};
		Int2IntArrayMap tt = PoissonPolyaUrnHLDA.createTopicTranslationTable(numTopics, newNumTopics, activeInData, activeTopics);
		assertEquals(1, tt.size());
		assertTrue(tt.containsKey(21));
		assertEquals(6,tt.get(21));
	}
	
	@Test
	public void testRandomConfigs() {
		//int nrLoops = 100;
		int nrLoops = 100_000;
		//int nrLoops = 10_000_000;
		for (int i = 0; i < nrLoops; i++) {
			int poissonSample; 
			poissonSample = (int) PolyaUrnDirichlet.nextPoisson(50);
			if(poissonSample<0) poissonSample = 0;
			int numTopics = poissonSample;
			if(numTopics<3) numTopics = 3;
			//System.out.println("numTopics = " + numTopics);
			poissonSample = (int) PolyaUrnDirichlet.nextPoisson(20);
			if(poissonSample<0) poissonSample = 1;
			int newNumTopics = numTopics - poissonSample;
			if(newNumTopics<0) newNumTopics = numTopics-1;
			if(newNumTopics==numTopics) newNumTopics = numTopics-1;
			//System.out.println("newNumTopics = " + newNumTopics);
			poissonSample = (int) PolyaUrnDirichlet.nextPoisson(10);
			if(poissonSample<0) poissonSample = 0;
			int activeInData = newNumTopics - poissonSample;
			if(activeInData<0) activeInData = newNumTopics-1;
			//System.out.println("activeInData = " + activeInData);
			int topicMax = numTopics + 100;
			
			boolean[] activeTopics = new boolean[topicMax]; 
			for(int active = 0; active < topicMax; active++) {
				activeTopics[active] = active<activeInData;
			}
			
			shuffle(activeTopics);
			
			//System.out.println(Arrays.toString(activeTopics));
			//System.out.println();
			int numAbove = 0;
			for(int topic = 0; topic < topicMax; topic++) {
				if(activeTopics[topic] && topic>=newNumTopics) numAbove++; 
			}
			
			try {
				Int2IntArrayMap tt = PoissonPolyaUrnHLDA.createTopicTranslationTable(numTopics, newNumTopics, activeInData, activeTopics);
				assertEquals(numAbove, tt.size());
				for(int topic = 0; topic < topicMax; topic++) {
					if(activeTopics[topic] && topic>=newNumTopics) {
						assertTrue(tt.containsKey(topic));
						assertTrue(tt.get(topic)<newNumTopics);
					}
				}
				//System.out.println(tt);
			} catch (Exception e) {
				System.out.println("numTopics = " + numTopics);
				System.out.println("newNumTopics = " + newNumTopics);
				System.out.println("activeInData = " + activeInData);
				e.printStackTrace();
				fail();
			}
		}
	}
	
	private static Random random;

    public static void shuffle(boolean[] array) {
        if (random == null) random = new Random();
        int count = array.length;
        for (int i = count; i > 1; i--) {
            swap(array, i - 1, random.nextInt(i));
        }
    }

    private static void swap(boolean[] array, int i, int j) {
        boolean temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }
	
	@Test
	public void testNoLessTopics() {
		int numTopics = 23;
		int newNumTopics = 25;
		int activeInData = 12; 
		boolean[] activeTopics = {true,true,true,true,true,true,false,true,false,true,true,false,true,false,false,true,false,false,false,false,false,true,false};
		try {
			PoissonPolyaUrnHLDA.createTopicTranslationTable(numTopics, newNumTopics, activeInData, activeTopics);
			fail("Creation should throw exception if new num topics is bigger than numTopics");
		} catch (Exception e) {
		}
	}
	
	static WalkerAliasTable constructBinomialAliasTable(int trials, double prob) {
		int tableLength = Math.max(100, 2*trials);
		WalkerAliasTable table = null;
		for (int i = 1; i < tableLength; i++) {
			BinomialDistribution binDistCalc = new BinomialDistribution(trials, prob);
			double [] trialProbabilities = new double [tableLength];
			for (int j = 0; j < tableLength; j++) {
				trialProbabilities[j] =  binDistCalc.probability(j);
			}
			table = new OptimizedGentleAliasMethod(trialProbabilities); 
		}
		return table;
	}

		
	@Test
	@Ignore("not robust enough yet") public void testBinomialAliasUsingCounts() {
		// Setup draws
		int noDraws = 500_000;
		int [] trialss = {2, 10, 20, 50, 100};
		double [] probs = {0.01, 0.1, 0.5};
		
		int samplesLen = 1000;
		for (int trials : trialss) {	
			for (double prob : probs) {
				long [] samplesAlias = new long[samplesLen]; 
				long [] samplesB = new long[samplesLen]; 
				WalkerAliasTable table = constructBinomialAliasTable(trials, prob);

				BinomialDistribution binDist = new BinomialDistribution(trials, prob);
				for (int i = 0; i < noDraws; i++) {
					samplesAlias[table.generateSample()]++;
					int binSample = binDist.sample(); 
					samplesB[binSample]++;
				}
				
				int [] rg = findSeqRange(samplesB);

				int smallestIdx = rg[0];
				int largestIdx = rg[1];
				
//				System.out.println("Smallest: "+ smallestIdx);
//				System.out.println("Largest: "+ largestIdx);
//				System.out.println("Value at Largest: " + samplesB[largestIdx]);
//				
//				System.out.println("Alias:" + Arrays.toString(samplesAlias));
//				System.out.println("Binom:" + Arrays.toString(samplesB));

				int obsLen = largestIdx - smallestIdx;
//				System.out.println("Obs. Len.: " + obsLen);
				// Adapt to the test preconditions
				long [] obsAlias = new long[obsLen];
				long [] obsBin = new long[obsLen];
				for (int i = smallestIdx; i < largestIdx; i++) {
					obsAlias[i-smallestIdx] = samplesAlias[i];
					obsBin[i-smallestIdx] = samplesB[i];
				}
				
//				System.out.println("Alias:" + Arrays.toString(obsAlias));
//				System.out.println("Binom:" + Arrays.toString(obsBin));
				
				ChiSquareTest cs = new ChiSquareTest();
				double test1 = cs.chiSquareTestDataSetsComparison(obsBin, obsAlias);
//				System.out.println(test1);
				assertTrue(test1 > 0.01);
//				System.out.println();
			}
		}
	}
	
	/* Find the longest sequence of consecutive non-zero values in an array */
	public static int [] findSeqRange(long [] array) {
		int maxSequenceStartIndex = 0;
        int maxSequenceLength = 0;
        int currentSequenceStartIndex = 0;
        int currentSequenceLength = 0;
        for (int i = 0; i < array.length; i++)
        {
            if(array[i] == 0)
            {
                if(currentSequenceLength > maxSequenceLength)
                {
                    maxSequenceLength = currentSequenceLength;
                    maxSequenceStartIndex = currentSequenceStartIndex;
                }
                currentSequenceStartIndex = i + 1;
                currentSequenceLength = 0;
            }
            else
            {
                currentSequenceLength++;
            }
        }
		
        if(currentSequenceLength > maxSequenceLength)
        {
            maxSequenceStartIndex = currentSequenceStartIndex;
            maxSequenceLength = currentSequenceLength;
        }
        int maxSequenceEndIndex = maxSequenceStartIndex + maxSequenceLength;

		return new int [] {maxSequenceStartIndex, maxSequenceEndIndex};
	}

	@Test
	@Ignore("not robust enough yet") public void testBinomialAliasUsingProbs() {
		// Setup draws
		int noDraws = 500_000;
		int tableLength = 200;
		int [] trialss = {15, 20, 50, 100};
		double [] probs = {0.01, 0.1, 0.5, 0.75};
		
		for (int trials : trialss) {	
			for (double prob : probs) {
				long [] samplesAlias = new long[tableLength]; 
				int maxIdx = 0;
				WalkerAliasTable table = constructBinomialAliasTable(trials, prob);

				double [] trialProbabilities = new double [tableLength];
				for (int i = 1; i < tableLength; i++) {
					BinomialDistribution binDistCalc = new BinomialDistribution(trials, prob);
					for (int j = 0; j < tableLength; j++) {
						trialProbabilities[j] =  binDistCalc.probability(j);
						if(trialProbabilities[j]>0.00000000001) {
							maxIdx = j+1;
						}
					}
				}

				for (int i = 0; i < noDraws; i++) {
					samplesAlias[table.generateSample()]++; 
				}
								
				// Adapt to the test preconditions
				long [] obsAlias = new long[maxIdx];
				double [] possibleProbs = new double[maxIdx];
				for (int i = 0; i < maxIdx; i++) {
					obsAlias[i] = samplesAlias[i];
					possibleProbs[i] = trialProbabilities[i];
				}
				
				ChiSquareTest cs = new ChiSquareTest();
				double test1 = cs.chiSquareTest(possibleProbs,obsAlias);
				double alpha = 0.01;
				if(test1 <= alpha) {
					System.out.println("Trials: " + trials + " prob: " + prob);
					System.out.println("Probs:" + Arrays.toString(possibleProbs));
					System.out.println("Alias:" + Arrays.toString(obsAlias));
					System.out.println(test1);
					System.out.println();
				}
				assertTrue(test1 > alpha);
			}
		}
	}
}
