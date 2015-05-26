package cc.mallet.misc;

import static org.junit.Assert.*;

import org.junit.Test;

import cc.mallet.topics.LDAUtils;

public class LDAUtilsTest {

	@Test
	public void testCalculateDocDensity() {
		double[] kdDensities = {0.2,0.8,0.63,0.26};
		int [] numTopics = {20, 40, 80, 100, 200, 400, 1000, 2000, 5000, 10000}; 
		int [] numDocuments = {500, 1000, 2000, 4000, 80000, 16000, 50000, 100_000, 500_000, 1_000_000, 5_000_000, 8_200_000, 10_000_000, 100_000_000};
		for (int i = 0; i < numTopics.length; i++) {
			for (int j = 0; j < numDocuments.length; j++) {				
				double calculateDocDensity = LDAUtils.calculateDocDensity(kdDensities, numTopics[i], numDocuments[j]);
				//System.out.println(numTopics[i] + " + " + numDocuments[j] + " => " + calculateDocDensity);
				assertTrue(calculateDocDensity>0 && calculateDocDensity < 1.0);
			}
		}
	}

}
