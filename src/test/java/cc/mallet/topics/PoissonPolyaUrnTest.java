package cc.mallet.topics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Random;

import org.junit.Test;

import cc.mallet.types.PolyaUrnDirichlet;
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
			poissonSample = (int) PolyaUrnDirichlet.nextPoissonNormalApproximation(50);
			if(poissonSample<0) poissonSample = 0;
			int numTopics = poissonSample;
			if(numTopics<3) numTopics = 3;
			//System.out.println("numTopics = " + numTopics);
			poissonSample = (int) PolyaUrnDirichlet.nextPoissonNormalApproximation(20);
			if(poissonSample<0) poissonSample = 1;
			int newNumTopics = numTopics - poissonSample;
			if(newNumTopics<0) newNumTopics = numTopics-1;
			if(newNumTopics==numTopics) newNumTopics = numTopics-1;
			//System.out.println("newNumTopics = " + newNumTopics);
			poissonSample = (int) PolyaUrnDirichlet.nextPoissonNormalApproximation(10);
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


}
