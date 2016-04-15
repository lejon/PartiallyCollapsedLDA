package cc.mallet.topics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class ModifiedSimpleLDATest {

	int numTopics = 5;
	double alpha = 0.01;
	double [] alphas = {0.01, 5.0, 0.04, 0.1, 0.000001};
	int docLength = 10;
	int [] oneDocTopics = {1,2,0,1,0, 1,2,0,1,0};
	//int [] oneDocTopics = {0,0,0,0,0, 0,0,0,0,0};

	
	@Test
	public void testThetaEstimate() {
		double [] thetaEstimate = ModifiedSimpleLDA.calcThetaEstimate(numTopics, alpha, docLength, oneDocTopics);
		System.out.println(Arrays.toString(thetaEstimate));
		double docSum = 0.0;
		for (int i = 0; i < thetaEstimate.length; i++) {
			docSum += thetaEstimate[i];
			assertTrue(thetaEstimate[i]!=0.0);
		}
		assertEquals(1.0,docSum, 0.0000000001);
	}

	@Test
	public void testThetaEstimates() {
		double [] thetaEstimate = ModifiedSimpleLDA.calcThetaEstimate(numTopics, alphas,  docLength, oneDocTopics);
		System.out.println(Arrays.toString(thetaEstimate));
		double docSum = 0.0;
		for (int i = 0; i < thetaEstimate.length; i++) {
			docSum += thetaEstimate[i];
			assertTrue(thetaEstimate[i]!=0.0);
		}
		assertEquals(1.0,docSum, 0.0000000001);
	}

	@Test
	public void testZbar() {
		double [] docTopicMeans = ModifiedSimpleLDA.calcZBar(numTopics, docLength, oneDocTopics);
		System.out.println(Arrays.toString(docTopicMeans));
		double docSum = 0.0;
		for (int i = 0; i < docTopicMeans.length; i++) {
			docSum += docTopicMeans[i];
		}
		assertEquals(1.0,docSum, 0.0000000001);
	}

}
