package cc.mallet.topics;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

public class ModifiedSimpleLDATest {

	int numTopics = 5;
	double alpha = 0.01;
	double [] alphas = {0.01, 5.0, 0.04, 0.1, 0.000001};
	double [][] thetaEstimate = new double[1][numTopics];
	int docIdx = 0;
	int docLength = 10;
	int [] oneDocTopics = {1,2,0,1,0, 1,2,0,1,0};
	//int [] oneDocTopics = {0,0,0,0,0, 0,0,0,0,0};

	
	@Test
	public void testThetaEstimate() {
		ModifiedSimpleLDA.calcThetaEstimate(numTopics, alpha, thetaEstimate, docIdx, docLength, oneDocTopics);
		System.out.println(Arrays.toString(thetaEstimate[0]));
		double docSum = 0.0;
		for (int i = 0; i < thetaEstimate[docIdx].length; i++) {
			docSum += thetaEstimate[docIdx][i];
		}
		assertEquals(1.0,docSum, 0.0000000001);
	}

	@Test
	public void testThetaEstimates() {
		ModifiedSimpleLDA.calcThetaEstimate(numTopics, alphas, thetaEstimate, docIdx, docLength, oneDocTopics);
		System.out.println(Arrays.toString(thetaEstimate[0]));
		double docSum = 0.0;
		for (int i = 0; i < thetaEstimate[docIdx].length; i++) {
			docSum += thetaEstimate[docIdx][i];
		}
		assertEquals(1.0,docSum, 0.0000000001);
	}

	@Test
	public void testZbar() {
		ModifiedSimpleLDA.calcZBar(numTopics, thetaEstimate, docIdx, docLength, oneDocTopics);
		System.out.println(Arrays.toString(thetaEstimate[0]));
		double docSum = 0.0;
		for (int i = 0; i < thetaEstimate[docIdx].length; i++) {
			docSum += thetaEstimate[docIdx][i];
		}
		assertEquals(1.0,docSum, 0.0000000001);
	}

}
