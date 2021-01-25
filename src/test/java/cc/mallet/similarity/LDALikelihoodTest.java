package cc.mallet.similarity;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.util.Arrays;
import org.junit.Test;

import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.util.LDAUtils;

public class LDALikelihoodTest {
	
	@Test
	public void testCalcProbWordGivenDoc() {
		int [] document = {2,0,0};
		//Map<Integer,Double> probs = LDALikelihoodDistance.calcProbWordGivenDocMLFrequencyEncoding(document);
		double [] probs = LDALikelihoodDistance.calcProbWordGivenDocMLFrequencyEncoding(document);
		assertEquals(2/2.0, probs[0], 0.00000001);
	}
	
	@Test
	public void testCalcProbWordGivenCorpus() {
		int [][] corpus = {
				{0,0,0},
				{1,1,1},
				{2,2,2},
		};
		
		int [] vocab = {0,1,2};
		
		double [] probs = LDALikelihoodDistance.calculateProbWordGivenCorpusMLFrequencyEncoding(corpus,vocab);
		assertEquals(3/9.0, probs[0], 0.00000001);
		assertEquals(3/9.0, probs[1], 0.00000001);
		assertEquals(3/9.0, probs[2], 0.00000001);
	}

	@Test
	public void testCalcProbWordGivenCorpus2() {
		int [][] corpus = {
				{0,0,0},
				{0,0,0},
				{1,1,1},
				{2,2,2},
		};
		
		int [] vocab = {0,1,2};
		
		double [] probs = LDALikelihoodDistance.calculateProbWordGivenCorpusMLFrequencyEncoding(corpus,vocab);
		assertEquals(3/9.0, probs[0], 0.00000001);
		assertEquals(3/9.0, probs[1], 0.00000001);
		assertEquals(3/9.0, probs[2], 0.00000001);
		
		LDALikelihoodDistance ld = new LDALikelihoodDistance(0.01);
		ld.initModel(corpus, vocab);
		assertEquals(ld.calcProbWordGivenCorpus(0), probs[0], 0.00000001);
		assertEquals(ld.calcProbWordGivenCorpus(1), probs[1], 0.00000001);
		assertEquals(ld.calcProbWordGivenCorpus(2), probs[2], 0.00000001);
	}

	@Test
	public void testCalcProbWordGivenTheta() {
		int [][] corpus = {
				{0,0,0},
				{0,0,0},
				{1,1,1},
				{2,2,2},
		};
		
		int [] vocab = {0,1,2};
		
		double [] theta = {1/3.0,1/3.0,1/3.0};
		
		double [][] phi = {
				{0.1,0.2,0.7},
				{0.2,0.1,0.7},
				{0.25,0.5,0.25}
		};
		
		double expected = 1/3.0 * 0.1 + 1/3.0 * 0.2 + 1/3.0 * 0.25; 
		LDALikelihoodDistance ld = new LDALikelihoodDistance(0.01);
		ld.initModel(corpus, vocab, phi);
		double ldaProb = ld.calcProbWordGivenTheta(theta, 0, phi);
		assertEquals(expected, ldaProb, 0.00000001);
	}
	
	@Test
	public void testCalcUnlikeliyProbWordGivenTheta() {
		int [][] corpus = {
				{0,0,0},
				{0,0,0},
				{1,1,1},
				{2,2,2},
		};
		
		int [] vocab = {0,1,2};
		
		double [] theta = {0.0,1/2.0,1/2.0};
		
		double [][] phi = {
				{0.1,0.2,0.7},
				{0.2,0.1,0.7},
				{0.25,0.5,0.25}
		};
		
		double expected = 0 * 0.1 + 1/2.0 * 0.2 + 1/2.0 * 0.25; 
		LDALikelihoodDistance ld = new LDALikelihoodDistance(0.01);
		ld.initModel(corpus, vocab, phi);
		double ldaProb = ld.calcProbWordGivenTheta(theta, 0, phi);
		assertEquals(expected, ldaProb, 0.00000001);
	}

	
	@Test
	public void testCalcWordLikelihood() {
		int [][] corpus = {
				{0,0,0},
				{0,0,0},
				{1,1,1},
				{2,2,2},
		};
		
		int [] vocab = {0,1,2};
		
		int [] query = {1,0,0};
		
		int [] document = {1,0,0};
		
		double [] theta = {1/3.0,1/3.0,1/3.0};
		
		double [][] phi = {
				{0.1,0.2,0.7},
				{0.2,0.1,0.7},
				{0.25,0.5,0.25}
		};
		
		LDALikelihoodDistance ld = new LDALikelihoodDistance(0.01);
		ld.initModel(corpus, vocab, phi);
		ld.setMu(1.0);
		
		double ldaProb = ld.calcProbWordGivenTheta(theta, 0, phi);
		
		int doclen = 1;
		double ratio = doclen / (doclen + ld.getMu());
		double expected = 
				Math.log(ld.getLambda() * ((ratio * 1.0) + (1-ratio) * 3/9) + 
				(1-ld.getLambda()) * ldaProb);
		assertEquals(expected, ld.ldaLoglikelihood(query, document, theta), 0.00000001);
	}

	
	@Test
	public void testCalcHighLDAWordLikelihood() {
		int [][] corpus = {
				{0,0,0},
				{2,2,2},
				{1,1,1},
				{2,2,2},
		};
		
		int [] vocab = {0,1,2};
		
		int [] query = {0,0,1};
		
		int [] document = {0,0,1};
		
		double [] theta = {0.4,0.4,0.2};
		
		double [][] phi = {
				{0.1,0.1,0.8},
				{0.1,0.1,0.8},
				{0.25,0.5,0.25}
		};
		
		LDALikelihoodDistance ld = new LDALikelihoodDistance(0.01);
		ld.initModel(corpus, vocab, phi);
		
		double ldaProb = ld.calcProbWordGivenTheta(theta, 2, phi);
		
		int doclen = 1;
		double ratio = doclen / (doclen + ld.getMu());
		double expected = 
				Math.log(ld.getLambda() * ((ratio * 1.0) + (1-ratio) * 5/15.0) + 
				(1-ld.getLambda()) * ldaProb);
		assertEquals(expected, ld.ldaLoglikelihood(query, document, theta), 0.00000001);
	}
	
	@Test
	public void testCalcLambdaOneLDAWordLikelihood() {
		int [][] corpus = {
				{0,0,0},
				{2,2,2},
				{1,1,1},
				{2,2,2},
		};
		
		int [] vocab = {0,1,2};
		
		int [] query = {0,0,1};
		
		int [] document = {0,0,1};
		
		double [] theta = {0.4,0.4,0.2};
		
		double [][] phi = {
				{0.1,0.1,0.8},
				{0.1,0.1,0.8},
				{0.25,0.5,0.25}
		};
		
		LDALikelihoodDistance ld = new LDALikelihoodDistance(0.01);
		ld.initModel(corpus, vocab, phi);
		ld.setLambda(1.0);
		ld.setMu(1.0);
		
		double ldaProb = ld.calcProbWordGivenTheta(theta, query[0], phi);
		
		int doclen = 1;
		double ratio = doclen / (doclen + ld.getMu());
		double expected = 
				Math.log(ld.getLambda() * ((ratio * 1.0) + (1-ratio) * 5/15.0) + 
				(1-ld.getLambda()) * ldaProb);
		assertEquals(expected, ld.ldaLoglikelihood(query, document, theta), 0.00000001);
	}

	@Test
	public void testCalc2WordLikelihood() {
		int [][] corpus = {
				{0,0,0},
				{0,0,0},
				{1,1,1},
				{2,2,2},
		};
		
		int [] vocab = {0,1,2};
		
		int [] query = {1,1,0};
		
		int [] document = {3,3,0};
		
		double [] theta = {1/3.0,1/3.0,1/3.0};
		
		double [][] phi = {
				{0.1,0.2,0.7},
				{0.2,0.1,0.7},
				{0.25,0.5,0.25}
		};
		
		LDALikelihoodDistance ld = new LDALikelihoodDistance(0.01);
		ld.initModel(corpus, vocab, phi);
		ld.setMu(1.0);
		
		double ldaProb0 = ld.calcProbWordGivenTheta(theta, 0, phi);
		double ldaProb1 = ld.calcProbWordGivenTheta(theta, 1, phi);
		
		double corpusSize = 9;
		double doclen = 6;
		double ratio = doclen / (doclen + ld.getMu());
		double expectedw0 = 
				ld.getLambda() * ((ratio * 3/doclen) + (1-ratio) * 3/corpusSize) + 
				(1-ld.getLambda()) * ldaProb0;
		double expectedw1 = 
				ld.getLambda() * ((ratio * 3/doclen) + (1-ratio) * 3/corpusSize) + 
				(1-ld.getLambda()) * ldaProb1;
		
		double expected = Math.log(expectedw0) + Math.log(expectedw1); 
		assertEquals(expected, ld.ldaLoglikelihood(query, document, theta), 0.00000001);
	}
	
	@Test
	public void testCalcNonWordLikelihood() {
		int [][] corpus = {
				{0,0,0},
				{0,0,0},
				{1,1,1},
				{2,2,2},
		};
		
		int [] vocab = {0,1,2};
		
		int [] query = {0,0,1};
		
		int [] document = {3,3,0};
		
		double [] theta = {1/3.0,1/3.0,1/3.0};
		
		double [][] phi = {
				{0.1,0.2,0.7},
				{0.2,0.1,0.7},
				{0.25,0.5,0.25}
		};
		
		LDALikelihoodDistance ld = new LDALikelihoodDistance(0.01);
		ld.initModel(corpus, vocab, phi);
		ld.setMu(1.0);
		
		double ldaProb0 = ld.calcProbWordGivenTheta(theta, 2, phi);
		
		double corpusSize = 9;
		double doclen = 6;
		double ratio = doclen / (doclen + ld.getMu());
		double expected = 
				Math.log(ld.getLambda() * ((ratio * 0.) + (1-ratio) * 3/corpusSize) + 
				(1-ld.getLambda()) * ldaProb0);
		assertEquals(expected, ld.ldaLoglikelihood(query, document, theta), 0.00000001);
	}
	
//	@Test
//	public void test() {
//		double [] theta1 = {0.1,0.2,0.7};
//		double [] theta2 = {1/3.0,1/3.0,1/3.0};
//		double [] theta3 = {0.25,0.25,0.5};
//		double [] theta4 = {0.1,0.25,0.65};
//		int [] v1 = {3,0,0};
//		int [] v2 = {3,0,0};
//		int [] v3 = {0,3,0};
//		int [] v4 = {1,1,1};
//		int [] v5 = {1,1,1};
//		
//		int [][] trainingset = {
//				{0,0,0},
//				{1,1,1},
//				{2,2,2},
//				{0,2,2},
//				{1,2,2},
//				{0,1,1}
//		};
//		
//		LDALikelihoodDistance ld = new LDALikelihoodDistance(3,0.01);
//		
//		double [][] phi = {
//				{0.1,0.2,0.7},
//				{0.2,0.1,0.7},
//				{0.25,0.5,0.25}
//		};
//		
//		int [] vocab = {0,1,2};
//		ld.setPhi(phi);
//		ld.initModel(trainingset, vocab);
//		double dist = ld.ldaLoglikelihood(v1, v2, theta1);
//		System.out.println("Dist: " + dist);
//		dist = ld.ldaLoglikelihood(v1, v2, theta2);
//		System.out.println("Dist: " + dist);
//		dist = ld.ldaLoglikelihood(v1, v2, theta3);
//		System.out.println("Dist: " + dist);
//		dist = ld.ldaLoglikelihood(v1, v3, theta4);
//		System.out.println("Dist: " + dist);
//		dist = ld.ldaLoglikelihood(v1, v4, theta4);
//		System.out.println("Dist: " + dist);
//		dist = ld.ldaLoglikelihood(v1, v5, theta4);
//		System.out.println("Dist: " + dist);
//		dist = ld.ldaLoglikelihood(v1, v5, theta2);
//		System.out.println("Dist: " + dist);
//	}
	
	@Test
	public void testIR() {
		String [] doclines = {
				"Xyzzy reports a profit but revenue is down", 
				"Quorus narrows quarter loss but revenue decreases further"};
		
		String [] classNames = new String [doclines.length];
		for (int i = 0; i < classNames.length; i++) {
			classNames[i] = "X";
		}
		
		InstanceList train = LDAUtils.loadInstancesStrings(doclines, classNames);
		//System.out.println(train.getAlphabet());
		assertEquals(14,train.getAlphabet().size());
		
		LDALikelihoodDistance cd = new LDALikelihoodDistance(0.01);
		cd.init(train);
		cd.setLambda(1.0);
		cd.setMixtureRatio(1/2.0);
		
		String [] doclinesTest = {"revenue down"};
		InstanceList test = LDAUtils.loadInstancesStrings(doclinesTest,train.getPipe());
		assertEquals("revenue, down", LDAUtils.instanceToString(test.get(0)));

		TokenFrequencyVectorizer tv = new TokenFrequencyVectorizer(); 
		double [] v1 = tv.instanceToVector(test.get(0));
		//System.out.println("v1=" + Arrays.toString(v1));
		double [] v2 = tv.instanceToVector(train.get(0));
		//System.out.println("v2=" + Arrays.toString(v2));
		double result1 = cd.calculate(v1,v2);
		
		double expected = 1 - (3/256.0);
		assertEquals(expected, 1-Math.exp(result1), 0.0001);

		double [] v3 = tv.instanceToVector(train.get(1));
		double result2 = cd.calculate(v1,v3);
		
		double expected2 = 1-(1/256.0);
		assertEquals(expected2, 1-Math.exp(result2), 0.0001);
	}
	
	
	@Test
	public void testCoOccurrenceProb() {
		String [] doclines = {
				"Xyzzy reports a profit but revenue is down", 
				"Quorus narrows quarter loss but revenue decreases further"
				};
		
		String [] classNames = new String [doclines.length];
		for (int i = 0; i < classNames.length; i++) {
			classNames[i] = "X";
		}
		
		InstanceList train = LDAUtils.loadInstancesStrings(doclines, classNames);
		//System.out.println(train.getAlphabet());
		assertEquals(14,train.getAlphabet().size());

		StreamCorpusStatistics cs = new StreamCorpusStatistics(train);
						
				
		int decreasesIdx = train.getAlphabet().lookupIndex("decreases");
		int revenueIdx = train.getAlphabet().lookupIndex("revenue");
		assertEquals(2,cs.getDocFreqs()[revenueIdx]);
		assertEquals(1,cs.getCoOccurrence(decreasesIdx, revenueIdx));
		assertEquals(0.5,cs.getNormalizedCoOccurrence(decreasesIdx, revenueIdx),0.00001);
		
		//TODO: Fix this
		//TokenFrequencyVectorizer tv = new TokenFrequencyVectorizer(); 
		//int [] document = Arrays.stream(tv.instanceToVector(train.get(0))).mapToInt(val -> ((int) val)).toArray();
		//double coOccurProb = CoOccurrenceLDALikelihoodDistance.coOccurrenceScore(decreasesIdx, document, cs);
		//assertEquals(1.0,coOccurProb,0.00001);
	}
	
	@Test
	public void testCoOccurrenceProbLikelihood() throws FileNotFoundException {
		InstanceList train = LDAUtils.loadInstances("src/main/resources/datasets/20newsgroups.txt", null, 0);		//System.out.println(train.getAlphabet());

		StreamCorpusStatistics cs = new StreamCorpusStatistics(train);
		int whiteIdx = train.getAlphabet().lookupIndex("white");
		int houseIdx = train.getAlphabet().lookupIndex("house");
		
		long revenueFreq = 0;
		for(Instance instance : train) {
			FeatureSequence tokens =
					(FeatureSequence) instance.getData();
			int [] tokenSequence = tokens.getFeatures();
			for (int position = 0; position < tokens.getLength(); position++) {
				int type = tokenSequence[position];
				if(type==houseIdx) {
					revenueFreq++;
				}
			}
		}
			
		TokenFrequencyVectorizer tv = new TokenFrequencyVectorizer(); 
		int [] document = Arrays.stream(tv.instanceToVector(train.get(0))).mapToInt(val -> ((int) val)).toArray();
				
		assertEquals(revenueFreq,cs.getTypeCounts()[houseIdx]);
		assertEquals(307,cs.getCoOccurrence(whiteIdx, houseIdx));
		assertEquals(0.2845,cs.getNormalizedCoOccurrence(whiteIdx, houseIdx),0.0001);
		double coOccurProb = CoOccurrenceLDALikelihoodDistance.coOccurrenceScore(whiteIdx, document, cs);
		assertEquals(0.04334,coOccurProb,0.0001);
	}

}
