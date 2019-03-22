package cc.mallet.topics;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

public class LightXLDATest {
	
	@Test
	public void testEnsureSameAcceptanceProbabilityDocument() {
		System.out.println("\ntestEnsureSameAcceptanceProbabilityDocument\n");
		
		int numTopics = 2;
		int numTypes = 3;
		int[] documentTopics = {0,1,0,1,1,1,1,0};
		int[] documentTypes = {2,1,2,1,0,1,1,0};
		
		double alpha = 0.1;
		double beta = 0.01;
		double betaSum = beta * numTypes;
		System.out.println("alpha: " + alpha + " beta: " + beta + " betaSum: " + betaSum);		
		
		int[] localTopicCounts = new int[numTopics];
		for (int j = 0; j < documentTopics.length; j++) {	
			localTopicCounts[documentTopics[j]] += 1;
		}
		System.out.println("localTopicCounts: " + Arrays.toString(localTopicCounts));
		
		int [][] globalTypeTopicCounts = 
			{
				{10,20},
				{4,6},
				{9,3},
			};
		int [][] globalTypeTopicCounts_not_i = new int[numTypes][numTopics];
		for (int i = 0; i < numTypes; i++) {	
			for (int j = 0; j < numTopics; j++) {	
				globalTypeTopicCounts_not_i[i][j] = globalTypeTopicCounts[i][j];
			}
		}
		for (int j = 0; j < globalTypeTopicCounts.length; j++) {			
			System.out.println("globalTypeTopicCounts: " + Arrays.toString(globalTypeTopicCounts[j]));
		}

		// Assert topicCountBetaHat is correct
		double [] topicCountBetaHat = new double[numTopics];
		LightPCLDAtypeTopicProposal.initTopicCountBetaHat(topicCountBetaHat, numTopics, numTypes, globalTypeTopicCounts, betaSum);
		double [] topicCountBetaHatManual = {23.03,29.03};
		System.out.println("topicCountBetaHat: " + Arrays.toString(topicCountBetaHat));
		System.out.println("topicCountBetaHatManual: " + Arrays.toString(topicCountBetaHatManual));
		assertEquals("topicCountBetaHat[0] is not calculated correctly", topicCountBetaHat[0], topicCountBetaHatManual[0], 0.00001);
		assertEquals("topicCountBetaHat[1] is not calculated correctly", topicCountBetaHat[1], topicCountBetaHatManual[1], 0.00001);
		double [] topicCountBetaHat_not_i = Arrays.copyOf(topicCountBetaHat, topicCountBetaHat.length);
		
		// Calculate and assert phi
		double [][] phi = new double[numTopics][numTypes];
		for (int i = 0; i < numTypes; i++) {	
			for (int j = 0; j < numTopics; j++) {	
				phi[j][i] = (globalTypeTopicCounts[i][j] + beta) / topicCountBetaHat[j]; 
			}
		}
		double phiMan00 = (10 + 0.01) / 23.03;
		double phiMan11 = (6 + 0.01) / 29.03;
		assertEquals("phi[0][0] is not calculated correctly", phi[0][0], phiMan00, 0.00001);
		assertEquals("phi[1][1] is not calculated correctly", phi[1][1], phiMan11, 0.00001);
		for (int j = 0; j < phi.length; j++) {
			System.out.println("Phi: " + Arrays.toString(phi[j]));
		}
		
		// Calculate and assert globalTokensPerTopic
		int [] globalTokensPerTopic = new int[numTopics];
		int [] globalTokensPerTopic_not_i = new int[numTopics];
		for (int i = 0; i < numTypes; i++) {	
			for (int j = 0; j < numTopics; j++) {	
				globalTokensPerTopic[j] += globalTypeTopicCounts[i][j]; 
				globalTokensPerTopic_not_i[j] += globalTypeTopicCounts[i][j]; 
			}
		}
		assertEquals("globalTokensPerTopic[0] is not calculated correctly", globalTokensPerTopic[0], 23.0 , 0.00001);
		assertEquals("globalTokensPerTopic[1] is not calculated correctly", globalTokensPerTopic[1], 29.0, 0.00001);		
		System.out.println("globalTokensPerTopic: " + Arrays.toString(globalTokensPerTopic));
		

		// Calculate and assert accept-reject
		System.out.println("\nTest accept reject:\n");
		
		// The proposed new topic by the word proposal
		int[] wordTopicIndicatorProposal = {1, 0};
		// The proposed new topic by the document proposal
		int[] docTopicIndicatorProposal = {1, 0};
		
		double [] cLightWordProbManual = {2.6131462, 0.8757712};
		double [] cLightDocProbManual = {0.4209629, 1.2117754};
		double [] pw2LightWordProbManual = {2.4285714, 0.7560976};
		double [] pw2LightDocProbManual = {0.391229, 1.046187};
		// If phi_i is used: pw2LightWordProbManual = 2.6131462 0.8757712 = cLightWordProbManual
		// If phi_i is used: pw2LightWordProbManual = 0.4209629 1.2117754 = cLightDocProbManual
		
		int [] localTopicCounts_i = Arrays.copyOf(localTopicCounts, localTopicCounts.length);
		System.out.println("documentTopics: " + Arrays.toString(documentTopics));
		System.out.println("documentTypes: " + Arrays.toString(documentTypes));
		System.out.println("wordTopicIndicatorProposal: " + Arrays.toString(wordTopicIndicatorProposal));
		System.out.println("docTopicIndicatorProposal: " + Arrays.toString(docTopicIndicatorProposal));
		for (int j = 0; j < 2; j++) {
			int type = documentTypes[j];
			int oldTopic = documentTopics[j];
			
			localTopicCounts_i[documentTopics[j]] -= 1;
			// System.out.println("localTopicCounts_i: " + Arrays.toString(localTopicCounts_i));
			globalTypeTopicCounts_not_i[type][documentTopics[j]] -= 1;
			for (int i = 0; i < globalTypeTopicCounts.length; i++) {
				// System.out.println("globalTypeTopicCounts_not_i: " + Arrays.toString(globalTypeTopicCounts_not_i[i]));
			}
			globalTokensPerTopic_not_i[documentTopics[j]] -= 1;
			// System.out.println("globalTokensPerTopic_not_i: " + Arrays.toString(globalTokensPerTopic_not_i));

			topicCountBetaHat_not_i[documentTopics[j]] -= 1;
			// System.out.println("globalTokensPerTopic_not_i: " + Arrays.toString(topicCountBetaHat_not_i));
			
			double [][] phi_i = new double[numTopics][numTypes];
			for (int i = 0; i < numTypes; i++) {	
				for (int k = 0; k < numTopics; k++) {	
					phi_i[k][i] = (globalTypeTopicCounts_not_i[i][k] + beta) / topicCountBetaHat_not_i[k]; 
					// System.out.println((globalTypeTopicCounts_not_i[i][k] + beta) + "/" + topicCountBetaHat_not_i[k] + " = " + phi_i[k][i]);
				}
			}
			for (int k = 0; k < phi.length; k++) {
				// System.out.println("Phi_i: " + Arrays.toString(phi_i[k]));
			}
			// 																		int[][] globalTypeTopicCounts, int[] globalTokensPerTopic, double[] localTopicCounts_i, int type, int oldTopic, int wordTopicIndicatorProposal, double alpha, double beta, double betaSum
			double cLightWordProb = CollapsedLightLDA.calculateWordAcceptanceProbability(globalTypeTopicCounts, globalTokensPerTopic, localTopicCounts_i, type, oldTopic, wordTopicIndicatorProposal[j], alpha, beta, betaSum);
			System.out.println("cL pi_w:" + cLightWordProb + " cLightWordProb (manual):" + cLightWordProbManual[j]);
			assertEquals("CollapsedLightLDA WordAcceptanceProbability is not correct", cLightWordProb, cLightWordProbManual[j], 0.00001);
			
			// System.out.println("localTopicCounts: " + Arrays.toString(localTopicCounts));
			// System.out.println("localTopicCounts_i: " + Arrays.toString(localTopicCounts_i));
			//                                                                      int[][] globalTypeTopicCounts, int[] globalTokensPerTopic, double[] localTopicCounts, double[] localTopicCounts_i, int type, int oldTopic, int docTopicIndicatorProposal, double alpha, double beta, double betaSum
			double cLightDocProb = CollapsedLightLDA.calculateDocumentAcceptanceProbability(globalTypeTopicCounts, globalTokensPerTopic, localTopicCounts, localTopicCounts_i, type, oldTopic, docTopicIndicatorProposal[j], alpha, beta, betaSum);			
			System.out.println("cL pi_d:" + cLightDocProb + " cLightDocProb (manual):" + cLightDocProbManual[j]);
			assertEquals("CollapsedLightLDA DocAcceptanceProbability is not correct", cLightDocProb, cLightDocProbManual[j], 0.00001);
			
			//                                                                              double[] localTopicCounts_not_i, int type, int oldTopic, int wordTopicIndicatorProposal, double[] topicCountBetaHat, int[][] typeTopicCounts, double[][] phi, double alpha, double beta
			double pw2LightWordProb = LightPCLDAtypeTopicProposal.calculateWordAcceptanceProbability(localTopicCounts_i, type, oldTopic, wordTopicIndicatorProposal[j], topicCountBetaHat, globalTypeTopicCounts, phi, alpha, beta);
			System.out.println("pw2 pi_w:" + pw2LightWordProb + " pw2LightWordProbManual (manual):" + pw2LightWordProbManual[j]);
			assertEquals("PCLightLDAw2 WordAcceptanceProbability is not correct", pw2LightWordProb, pw2LightWordProbManual[j], 0.00001);			
			
			//                                                                                 double[] localTopicCounts, double[] localTopicCounts_not_i, int type, int oldTopic, int docTopicIndicatorProposal, double[][] phi, double alpha
			double pw2LightDocProb = LightPCLDAtypeTopicProposal.calculateDocumentAcceptanceProbability(localTopicCounts, localTopicCounts_i, type, oldTopic, docTopicIndicatorProposal[j], phi, alpha);
			System.out.println("pw2 pi_w:" + pw2LightDocProb + " pw2LightWordProbManual (manual):" + pw2LightDocProbManual[j]);
			assertEquals("PCLightLDAw2 DocAcceptanceProbability is not correct", pw2LightDocProb, pw2LightDocProbManual[j], 0.00001);			
			
			double pw2LightWordProb_phi_i = LightPCLDAtypeTopicProposal.calculateWordAcceptanceProbability(localTopicCounts_i, type, oldTopic, wordTopicIndicatorProposal[j], topicCountBetaHat, globalTypeTopicCounts, phi_i, alpha, beta);
			System.out.println("pw2LightWordProb_phi_i:" + pw2LightWordProb_phi_i + " cLightWordProb:" + cLightWordProb);
			assertEquals("WordAcceptanceProbability CollapsedLightLDA != PCLightLDAw2 with Phi_i", pw2LightWordProb_phi_i, cLightWordProb, 0.00001);			
			
			double pw2LightDocProb_phi_i = LightPCLDAtypeTopicProposal.calculateDocumentAcceptanceProbability(localTopicCounts, localTopicCounts_i, type, oldTopic, docTopicIndicatorProposal[j], phi_i, alpha);
			System.out.println("pw2LightWordProb_phi_i:" + pw2LightDocProb_phi_i + " cLightWordProb:" + cLightDocProb);
			assertEquals("DocAcceptanceProbability CollapsedLightLDA != PCLightLDAw2 with Phi_i", pw2LightDocProb_phi_i, cLightDocProb, 0.00001);			
						
			/*
			TODO: Refactor out this
			double pw1LightWordProb = LightPCLDA.calculateWordAcceptanceProbability(localTopicCounts_i, type, oldTopic, wordTopicIndicatorProposal, topicCountBetaHat, globalTypeTopicCounts, phi, alpha, betaSum);
			double pw1LightDocProb  = LightPCLDA.calculateDocumentAcceptanceProbability(localTopicCounts_i, localTopicCounts_not_i, type, oldTopic, docTopicIndicatorProposal, phi, alpha);
			*/
			
			System.out.println();
			topicCountBetaHat_not_i[documentTopics[j]] += 1;
			globalTypeTopicCounts_not_i[type][documentTopics[j]] += 1;
			globalTokensPerTopic_not_i[documentTopics[j]] += 1;
			localTopicCounts_i[documentTopics[j]] += 1;
		}

	}
	/* R code for manual calculations
#pi_w =[(n^−di_td + alpha_t)(n^−di_tw + beta_w)(n^−di_s + beta_hat)(n_sw + beta_w)(nt + beta_hat)]/
#  [(n^−di_sd + alpha_s)(n−di_sw + beta_w)(n^−di_t + beta_hat)(ntw + beta_w)(ns + beta_hat)]
t <- c(1,0); s<-c(0,1); a <- 0.1; b <- 0.01; type <- c(2,1), oldtopic <- 0; bhat <- b * 3 # Types = 3
pi_wcl <- 
  ((c(5,3) + a) * (c(3, 4) + b) * (c(22, 28) + bhat) * (c(9,6) + b) * (c(29, 23) + bhat)) /
  ((c(2,4) + a) * (c(8, 5) + b) * (c(29, 23) + bhat) * (c(3,4) + b) * (c(23, 29) + bhat))


#pi_d =[(n−di_td + alpha_t)(n−di_tw + beta_w)(n−di_s + beta_hat)(nsd + alpha_s)]/
#  [(n−di_sd + alpha_s)(n−di_sw + beta_w)(n−di_t + beta_hat)(ntd + alpha_t)]
t <- c(1,0); s<-c(0,1); a <- 0.1; b <- 0.01; type <- c(2,1); bhat <- b * 3 # Types = 3
pi_d <- 
  ((c(5,3) + a) * (c(3, 4) + b) * (c(22, 28) + bhat) * (c(3,5) + a)) / 
  ((c(2,4) + a) * (c(8, 5) + b) * (c(29, 23) + bhat) * (c(5,3) + a))

# pi_w = phi_tw * (a + n−di_td) * (n_sw + beta_w) * (nt + beta_hat) /
#  phi_sw * (a + n−di_sd) * (n_tw + beta_w) * (ns + beta_hat)
t <- c(1,0); s<-c(0,1); a <- 0.1; b <- 0.01; type <- c(2,1); bhat <- b * 3 # Types = 3
pi_w2 <- (c(0.1036858422321736, 0.17412071211463306) * (a + c(5,3)) * (c(9,6) + b) * (c(29,23) + bhat)) /
  (c(0.3912288319583152, 0.20702721322769546) * (a + c(2,4)) * (c(3,4) + b) * (c(23,29) + bhat))

# pi_d = phi_tw * (a + n−di_td) * (a + n_sd) /
#  phi_sw * (a + n−di_sd) * (a + n_td)
t <- c(1,0); s<-c(0,1); a <- 0.1; b <- 0.01; type <- c(2,1); bhat <- b * 3 # Types = 3
pi_d <- (c(0.1036858422321736, 0.17412071211463306) * (a + c(5,3)) * (a + c(3,5))) /
  (c(0.3912288319583152, 0.20702721322769546) * (a + c(2,4)) * (a + c(5,3)))

# Calculations with Phi_i
#pi_w =[(n^−di_tw + beta_w)(n^−di_s + beta_hat)]/
#  [(n−di_sw + beta_w)(n^−di_t + beta_hat)]
#pi_w =[(n^−di_tw + beta_w)/(n^−di_t + beta_hat)]/
#  [(n−di_sw + beta_w)/(n^−di_s + beta_hat)]
# pi_w = phi_tw  / phi_sw 

#pi_w =[(n^−di_tw + beta_w)(n^−di_s + beta_hat)/
#  [(n−di_sw + beta_w)(n^−di_t + beta_hat)
pi_wcl <- 
  ((c(3, 4) + b) * (c(22, 28) + bhat)) /
  ((c(8, 5) + b) * (c(29, 23) + bhat))

#pi_w =[(n^−di_tw + beta_w)/(n^−di_t + beta_hat)]/
#  [(n−di_sw + beta_w)/(n^−di_s + beta_hat)]
pi_wcl <- 
  ((c(3, 4) + b) / (c(29, 23) + bhat)) /
  ((c(8, 5) + b) / (c(22, 28) + bhat))
# pi_w = phi_tw  / phi_sw 

# pi_w = phi_tw * (a + n−di_td) * (n_sw + beta_w) * (nt + beta_hat) /
#  phi_sw * (a + n−di_sd) * (n_tw + beta_w) * (ns + beta_hat)
t <- c(1,0); s<-c(0,1); a <- 0.1; b <- 0.01; type <- c(2,1); bhat <- b * 3 # Types = 3
pi_phi_i_w2 <- (c(0.1036858422321736, 0.17412071211463306) * (a + c(5,3)) * (c(9,6) + b) * (c(29,23) + bhat)) /
  (c(0.36359509759418973, 0.17873706742775597) * (a + c(2,4)) * (c(3,4) + b) * (c(23,29) + bhat))

# pi_d = phi_tw * (a + n−di_td) * (a + n_sd) /
#  phi_sw * (a + n−di_sd) * (a + n_td)

pi_phi_i_d <- (c(0.1036858422321736, 0.17412071211463306) * (a + c(5,3)) * (a + c(3,5))) /
  (c(0.36359509759418973, 0.17873706742775597) * (a + c(2,4)) * (a + c(5,3)))
  
	 */


}
