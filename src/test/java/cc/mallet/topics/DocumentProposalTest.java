package cc.mallet.topics;

import static org.junit.Assert.assertTrue;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest;
import org.junit.Test;


public class DocumentProposalTest {
	
	Random rnd = new Random();
	int numTopics = 12;
	int docLength = 11;
	double alphaSum = 0.1;
	int [] oneDocTopics = {11,0,1,3,10,0,6,0,8,9,10,11,1,9,8,7,0,4,11,11};
	
	int loops = 1_000_000;
	int [] stats = new int[2];
	double [] drawStats = new double[numTopics];
	double [] priorDrawStats = new double[numTopics];
	KolmogorovSmirnovTest ksTest = new KolmogorovSmirnovTest();
	
	@Test
	public void testDocProposal() {
		for (int i = 0; i < loops; i++) {
			double u_i = ThreadLocalRandom.current().nextDouble() * (oneDocTopics.length + alphaSum); // (n_d + K*alpha) * u where u ~ U(0,1)
			int docTopicIndicatorProposal = -1;
			if(u_i < oneDocTopics.length) {
				stats[0]++;
				docTopicIndicatorProposal = oneDocTopics[(int) u_i];
			} else {
				stats[1]++;
				docTopicIndicatorProposal = (int) (((u_i - oneDocTopics.length) / alphaSum) * numTopics); // assume symmetric alpha, just draws one alpha
				priorDrawStats[docTopicIndicatorProposal]++;
			}
			drawStats[docTopicIndicatorProposal]++;
		}
		
		for (int i = 0; i < drawStats.length; i++) {
			drawStats[i] /= loops;
		}
		for (int i = 0; i < priorDrawStats.length; i++) {
			priorDrawStats[i] /= loops;
		}
		
		//System.out.println("Less or more: "+ Arrays.toString(stats));
		//System.out.println("Distr: "+ Arrays.toString(drawStats));
		//System.out.println("Prior Distr: "+ Arrays.toString(priorDrawStats));
		double [] expectedProportions = new double[priorDrawStats.length];
		for (int i = 0; i < expectedProportions.length; i++) {
			expectedProportions[i] = stats[1] / (double) (priorDrawStats.length*loops);
		}
		//System.out.println("Expected Distr: "+ Arrays.toString(expectedProportions));

		double pval = ksTest.kolmogorovSmirnovTest(priorDrawStats, expectedProportions);

		//System.out.println("Pval is: " + pval);

		assertTrue("Pval is: " + pval, pval > 0.00001);

	}


}
