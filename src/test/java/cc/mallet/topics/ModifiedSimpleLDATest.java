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
	double normalizer;
	double alphaSum;
	int [] localTopicCounts = new int[numTopics];
	
	{
		for (int i = 0; i < oneDocTopics.length; i++) {
			localTopicCounts[oneDocTopics[i]]++;
		}
		
		for (int i = 0; i < numTopics; i++) {
			normalizer += localTopicCounts[i] + alphas[i];
			alphaSum += alphas[i];
		}
	}

	
	@Test
	public void testThetaEstimate() {
		double [] thetaEstimate = ModifiedSimpleLDA.calcThetaEstimate(numTopics, alpha, docLength, oneDocTopics);
		double docSum = 0.0;
		double normalizer = 0;
		for (int k = 0; k < numTopics; k++) {
			normalizer += localTopicCounts[k] + alpha;
		}
		for (int i = 0; i < thetaEstimate.length; i++) {
			docSum += thetaEstimate[i];
			assertTrue(thetaEstimate[i]!=0.0);
			assertEquals("Expected " + thetaEstimate[i] + " but got " + ((localTopicCounts[i] + alpha) / normalizer),
					thetaEstimate[i],(localTopicCounts[i] + alpha) / normalizer, 
					0.000000000000001);
		}

		assertEquals(1.0, docSum, 0.0000000001);
	}
	
	@Test
	public void testThetaEstimateNonSymAlpha() {
		double [] thetaEstimate = ModifiedSimpleLDA.calcThetaEstimate(numTopics, alphas, alphaSum, docLength, oneDocTopics);
		double docSum = 0.0;
		for (int i = 0; i < thetaEstimate.length; i++) {
			docSum += thetaEstimate[i];
			assertTrue(thetaEstimate[i]!=0.0);
			assertEquals("Expected " + thetaEstimate[i] + " but got " + ((localTopicCounts[i] + alpha) / normalizer),
					thetaEstimate[i],(localTopicCounts[i] + alphas[i]) / normalizer,0.000000000000001);
		}
		assertEquals(1.0, docSum, 0.0000000001);
	}
	
	@Test
	public void testThetaEstimateNonSymAlphaEmptyDoc() {
		int [] localTopicCounts = new int[numTopics];
		int [] oneDocTopics = {0,0,0,0,0,0,0,0,0,0};
		double normalizer = 0;
		for (int i = 0; i < alphas.length; i++) {
			normalizer += localTopicCounts[i] + alphas[i];
		}
		int docLength = 0;
		double [] thetaEstimate = ModifiedSimpleLDA.calcThetaEstimate(numTopics, alphas, alphaSum, docLength, oneDocTopics);
		double docSum = 0.0;
		for (int i = 0; i < thetaEstimate.length; i++) {
			docSum += thetaEstimate[i];
			assertTrue(thetaEstimate[i]!=0.0);
			assertEquals("Expected " + thetaEstimate[i] + " but got " + ((localTopicCounts[i] + alphas[i]) / normalizer),
					thetaEstimate[i],(localTopicCounts[i] + alphas[i]) / normalizer,0.000000000000001);
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
			assertTrue(thetaEstimate[i]!=0.0);
			assertEquals("Expected " + thetaEstimate[i] + " but got " + (alpha/(numTopics*alpha)),
					thetaEstimate[i],alpha/(numTopics*alpha),0.000000000000001);

		}
		assertEquals(1.0, docSum, 0.0000000001);
	}

	@Test
	public void testThetaEstimates() {
		double [] thetaEstimate = ModifiedSimpleLDA.calcThetaEstimate(numTopics, alphas, alphaSum, docLength, oneDocTopics);
		double docSum = 0.0;
		for (int i = 0; i < thetaEstimate.length; i++) {
			docSum += thetaEstimate[i];
			assertTrue(thetaEstimate[i]!=0.0);
			assertEquals("Expected " + thetaEstimate[i] + " but got " + ((localTopicCounts[i] + alphas[i]) / normalizer),
					thetaEstimate[i],(localTopicCounts[i] + alphas[i]) / normalizer,0.000000000000001);

			
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
