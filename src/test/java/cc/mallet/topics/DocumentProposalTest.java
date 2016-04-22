package cc.mallet.topics;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.Test;


public class DocumentProposalTest {
	
	Random rnd = new Random();
	int numTopics = 12;
	int docLength = 11;
	double alphaSum = 0.1;
	int [] oneDocTopics = {11,0,1,3,10,0,6,0,8,9,10,11,1,9,8,7,0,4,11,11};
	
	int loops = 10000;
	int [] stats = new int[2];
	double [] drawStats = new double[numTopics];
	
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
			}
			drawStats[docTopicIndicatorProposal]++;
			System.out.println("drew:" + docTopicIndicatorProposal);
		}
		
		for (int i = 0; i < drawStats.length; i++) {
			drawStats[i] /= loops;
		}
		
		System.out.println("Less or more: "+ Arrays.toString(stats));
		System.out.println("Distr: "+ Arrays.toString(drawStats));
	}


}
