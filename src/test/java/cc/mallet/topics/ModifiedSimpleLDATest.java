package cc.mallet.topics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ModifiedSimpleLDATest {

	int numTopics = 5;
	double alpha = 0.01;
	double [] alphas = {0.01, 5.0, 0.04, 0.1, 0.000001};
	int docLength = 10;
	int [] oneDocTopics = {1,2,0,1,0,1,2,0,1,0};
	double alphaSum;
	int [] localTopicCounts = new int[numTopics];
	
	{
		for (int i = 0; i < oneDocTopics.length; i++) {
			localTopicCounts[oneDocTopics[i]]++;
		}
		
		for (int i = 0; i < numTopics; i++) {
			alphaSum += localTopicCounts[i] + alphas[i];
		}
	}

	
	@Test
	public void testThetaEstimate() {
		double [] thetaEstimate = ModifiedSimpleLDA.calcThetaEstimate(numTopics, alpha, docLength, oneDocTopics);
		double docSum = 0.0;
		double alphaSum = 0;
		for (int k = 0; k < numTopics; k++) {
			alphaSum += localTopicCounts[k] + alpha;
		}
		for (int i = 0; i < thetaEstimate.length; i++) {
			docSum += thetaEstimate[i];
			assertTrue(thetaEstimate[i]==(localTopicCounts[i] + alpha) / alphaSum);
		}
		assertEquals(1.0, docSum, 0.0000000001);
	}
	
	@Test
	public void testThetaEstimateNonSymAlpha() {
		double [] thetaEstimate = ModifiedSimpleLDA.calcThetaEstimate(numTopics, alphas, docLength, oneDocTopics);
		double docSum = 0.0;
		for (int i = 0; i < thetaEstimate.length; i++) {
			docSum += thetaEstimate[i];
			assertTrue(thetaEstimate[i]==(localTopicCounts[i] + alphas[i]) / alphaSum);
		}
		assertEquals(1.0, docSum, 0.0000000001);
	}
	
	@Test
	public void testThetaEstimateNonSymAlphaEmptyDoc() {
		int [] localTopicCounts = new int[numTopics];
		int [] oneDocTopics = {0,0,0,0,0,0,0,0,0,0};
		double alphaSum = 0;
		for (int i = 0; i < alphas.length; i++) {
			alphaSum += localTopicCounts[i] + alphas[i];
		}
		int docLength = 0;
		double [] thetaEstimate = ModifiedSimpleLDA.calcThetaEstimate(numTopics, alphas, docLength, oneDocTopics);
		double docSum = 0.0;
		for (int i = 0; i < thetaEstimate.length; i++) {
			docSum += thetaEstimate[i];
			assertTrue(thetaEstimate[i]==(localTopicCounts[i] + alphas[i]) / alphaSum);
		}
		assertEquals(1.0, docSum, 0.0000000001);
	}
	
	@Test
	public void testThetaEstimateEmptyDoc() {
		int [] oneDocTopics = {0,0,0,0,0,0,0,0,0,0};
		int docLength = 0;
		double [] thetaEstimate = ModifiedSimpleLDA.calcThetaEstimate(numTopics, alpha, docLength, oneDocTopics);
		double docSum = 0.0;
		for (int i = 0; i < thetaEstimate.length; i++) {
			docSum += thetaEstimate[i];
			assertTrue(thetaEstimate[i]==alpha/(numTopics*alpha));
		}
		assertEquals(1.0, docSum, 0.0000000001);
	}

	@Test
	public void testThetaEstimates() {
		double [] thetaEstimate = ModifiedSimpleLDA.calcThetaEstimate(numTopics, alphas,  docLength, oneDocTopics);
		double docSum = 0.0;
		for (int i = 0; i < thetaEstimate.length; i++) {
			docSum += thetaEstimate[i];
			assertTrue(thetaEstimate[i]==(localTopicCounts[i] + alphas[i]) / alphaSum);
		}
		assertEquals(1.0,docSum, 0.0000000001);
	}

	@Test
	public void testZbar() {
		double [] docTopicMeans = ModifiedSimpleLDA.calcZBar(numTopics, docLength, oneDocTopics);
		double docSum = 0.0;
		for (int i = 0; i < docTopicMeans.length; i++) {
			docSum += docTopicMeans[i];
		}
		assertEquals(1.0,docSum, 0.0000000001);
	}

}
