package cc.mallet.topics;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

public class LightXLDATest {

	@Test
	public void testEnsureSameAcceptanceProbability() {
		int numTopics = 2;
		int numTypes = 2;
		int type = 0;
		
		// The old topic
		int oldTopic = 0;
		// The proposed new topic by the word proposal
		int wordTopicIndicatorProposal = 1;
		// The proposed new topic by the document proposal
		int docTopicIndicatorProposal = 1;
		double alpha = 0.1;
		double beta = 0.01;
		double betaSum = beta * numTypes;
		
		double [] localTopicCounts_i = {3,5};
		double [] localTopicCounts_not_i = Arrays.copyOf(localTopicCounts_i, localTopicCounts_i.length);
		// Index of "oldTopic" 
		int sIdx = 0;
		localTopicCounts_not_i[sIdx] -= 1;
				
		int [][] globalTypeTopicCounts = 
			{
				{10,20},
				{4,6},
			};
		
		double [][] phi = {
				{globalTypeTopicCounts[0][0] / (double) (globalTypeTopicCounts[0][0]+globalTypeTopicCounts[0][1]), globalTypeTopicCounts[0][1] / (double) (globalTypeTopicCounts[0][0]+globalTypeTopicCounts[0][1])},
				{globalTypeTopicCounts[1][0] / (double) (globalTypeTopicCounts[1][0]+globalTypeTopicCounts[1][1]), globalTypeTopicCounts[1][1] / (double) (globalTypeTopicCounts[1][0]+globalTypeTopicCounts[1][1])}
		};
		for (int j = 0; j < phi.length; j++) {			
			System.out.println("Phi: " + Arrays.toString(phi[j]));
		}
		
		double [] topicCountBetaHat = new double[numTopics];
		
		int [] globalTokensPerTopic = {globalTypeTopicCounts[0][0]+globalTypeTopicCounts[1][0], globalTypeTopicCounts[0][1]+globalTypeTopicCounts[1][1]};
		System.out.println("globalTokensPerTopic: " + Arrays.toString(globalTokensPerTopic));

		LightPCLDAtypeTopicProposal.initTopicCountBetaHat(topicCountBetaHat, numTopics, numTypes, globalTypeTopicCounts, betaSum);
		
		double cLightWordProb = CollapsedLightLDA.calculateWordAcceptanceProbability(globalTypeTopicCounts, globalTokensPerTopic, localTopicCounts_i, type, oldTopic, wordTopicIndicatorProposal, alpha, beta, betaSum);
		double cLightDocProb  = CollapsedLightLDA.calculateDocumentAcceptanceProbability(globalTypeTopicCounts, globalTokensPerTopic, localTopicCounts_i, localTopicCounts_i, type, oldTopic, docTopicIndicatorProposal, alpha, beta, betaSum);
		double pLightWordProb = LightPCLDAtypeTopicProposal.calculateWordAcceptanceProbability(localTopicCounts_i, type, oldTopic, wordTopicIndicatorProposal, topicCountBetaHat, globalTypeTopicCounts, phi, alpha, betaSum);
		double pLightDocProb  = LightPCLDAtypeTopicProposal.calculateDocumentAcceptanceProbability(localTopicCounts_i, localTopicCounts_not_i, type, oldTopic, docTopicIndicatorProposal, phi, alpha);
		assertEquals("CollapsedLightLDA WordAcceptanceProbability does not equal that of LightPCLDA", cLightWordProb, pLightWordProb, 0.00001);
		assertEquals("CollapsedLightLDA DocumentAcceptanceProbability does not equal that of LightPCLDA",cLightDocProb, pLightDocProb, 0.00001);
	}

}
