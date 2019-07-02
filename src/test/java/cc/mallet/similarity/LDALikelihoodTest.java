package cc.mallet.similarity;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.util.LDAUtils;

public class LDALikelihoodTest {
	
	@Test
	public void testCalcProbWordGivenDoc() {
		int [] document = {2,0,0};
		Map<Integer,Double> probs = LDALikelihoodDistance.calcProbWordGivenDocMLWordEncoding(document);
		assertEquals(2/3.0, probs.get(0), 0.00000001);
		assertEquals(1/3.0, probs.get(2), 0.00000001);
	}
	
	@Test
	public void testCalcProbWordGivenCorpus() {
		int [][] corpus = {
				{0,0,0,0},
				{1,1,1,1},
				{2,2,2,2},
		};
		
		int [] vocab = {0,1,2};
		
		double [] probs = LDALikelihoodDistance.calculateProbWordGivenCorpusMLWordEncoding(corpus,vocab);
		assertEquals(1/3.0, probs[0], 0.00000001);
		assertEquals(1/3.0, probs[1], 0.00000001);
		assertEquals(1/3.0, probs[2], 0.00000001);
	}

	@Test
	public void testCalcProbWordGivenCorpus2() {
		int [][] corpus = {
				{0,0,0,0},
				{0,0,0,0},
				{1,1,1,1},
				{2,2,2,2},
		};
		
		int [] vocab = {0,1,2};
		
		double [] probs = LDALikelihoodDistance.calculateProbWordGivenCorpusMLWordEncoding(corpus,vocab);
		assertEquals(1/2.0, probs[0], 0.00000001);
		assertEquals(1/4.0, probs[1], 0.00000001);
		assertEquals(1/4.0, probs[2], 0.00000001);
		
		LDALikelihoodDistance ld = new LDALikelihoodDistance(3,0.01);
		ld.initModel(corpus, vocab);
		assertEquals(ld.calcProbWordGivenCorpus(0), probs[0], 0.00000001);
		assertEquals(ld.calcProbWordGivenCorpus(1), probs[1], 0.00000001);
		assertEquals(ld.calcProbWordGivenCorpus(2), probs[2], 0.00000001);
	}

	@Test
	public void testCalcProbWordGivenTheta() {
		int [][] corpus = {
				{0,0,0,0},
				{0,0,0,0},
				{1,1,1,1},
				{2,2,2,2},
		};
		
		int [] vocab = {0,1,2};
		
		double [] theta = {1/3.0,1/3.0,1/3.0};
		
		double [][] phi = {
				{0.1,0.2,0.7},
				{0.2,0.1,0.7},
				{0.25,0.5,0.25}
		};
		
		double expected = 1/3.0 * 0.1 + 1/3.0 * 0.2 + 1/3.0 * 0.25; 
		LDALikelihoodDistance ld = new LDALikelihoodDistance(3,0.01);
		ld.initModel(corpus, vocab, phi);
		double ldaProb = ld.calcProbWordGivenTheta(theta, 0, phi);
		assertEquals(expected, ldaProb, 0.00000001);
	}
	
	@Test
	public void testCalcUnlikeliyProbWordGivenTheta() {
		int [][] corpus = {
				{0,0,0,0},
				{0,0,0,0},
				{1,1,1,1},
				{2,2,2,2},
		};
		
		int [] vocab = {0,1,2};
		
		double [] theta = {0.0,1/2.0,1/2.0};
		
		double [][] phi = {
				{0.1,0.2,0.7},
				{0.2,0.1,0.7},
				{0.25,0.5,0.25}
		};
		
		double expected = 0 * 0.1 + 1/2.0 * 0.2 + 1/2.0 * 0.25; 
		LDALikelihoodDistance ld = new LDALikelihoodDistance(3,0.01);
		ld.initModel(corpus, vocab, phi);
		double ldaProb = ld.calcProbWordGivenTheta(theta, 0, phi);
		assertEquals(expected, ldaProb, 0.00000001);
	}

	
	@Test
	public void testCalcWordLikelihood() {
		int [][] corpus = {
				{0,0,0,0},
				{0,0,0,0},
				{1,1,1,1},
				{2,2,2,2},
		};
		
		int [] vocab = {0,1,2};
		
		int [] query = {0};
		
		int [] document = {0};
		
		double [] theta = {1/3.0,1/3.0,1/3.0};
		
		double [][] phi = {
				{0.1,0.2,0.7},
				{0.2,0.1,0.7},
				{0.25,0.5,0.25}
		};
		
		LDALikelihoodDistance ld = new LDALikelihoodDistance(3,0.01);
		ld.initModel(corpus, vocab, phi);
		ld.setMu(1.0);
		
		double ldaProb = ld.calcProbWordGivenTheta(theta, query[0], phi);
		
		double ratio = document.length / (document.length + ld.getMu());
		double expected = 
				Math.log(ld.getLambda() * ((ratio * 1.0) + (1-ratio) * 1/2) + 
				(1-ld.getLambda()) * ldaProb);
		System.out.println("Expected (testCalcWordLikelihood): " + expected + " => "+ Math.exp(expected));
		assertEquals(expected, ld.ldaLoglikelihood(query, document, theta), 0.00000001);
	}

	
	@Test
	public void testCalcHighLDAWordLikelihood() {
		int [][] corpus = {
				{0,0,0,0},
				{2,2,2,2},
				{1,1,1,1},
				{2,2,2,2},
		};
		
		int [] vocab = {0,1,2};
		
		int [] query = {2};
		
		int [] document = {2};
		
		double [] theta = {0.4,0.4,0.2};
		
		double [][] phi = {
				{0.1,0.1,0.8},
				{0.1,0.1,0.8},
				{0.25,0.5,0.25}
		};
		
		LDALikelihoodDistance ld = new LDALikelihoodDistance(3,0.01);
		ld.initModel(corpus, vocab, phi);
		
		double ldaProb = ld.calcProbWordGivenTheta(theta, query[0], phi);
		
		double ratio = document.length / (document.length + ld.getMu());
		double expected = 
				Math.log(ld.getLambda() * ((ratio * 1.0) + (1-ratio) * 1/2) + 
				(1-ld.getLambda()) * ldaProb);
		System.out.println("Expected (testCalcHighLDAWordLikelihood): " + expected + " => "+ Math.exp(expected));
		assertEquals(expected, ld.ldaLoglikelihood(query, document, theta), 0.00000001);
	}
	
	@Test
	public void testCalcLambdaOneLDAWordLikelihood() {
		int [][] corpus = {
				{0,0,0,0},
				{2,2,2,2},
				{1,1,1,1},
				{2,2,2,2},
		};
		
		int [] vocab = {0,1,2};
		
		int [] query = {2};
		
		int [] document = {2};
		
		double [] theta = {0.4,0.4,0.2};
		
		double [][] phi = {
				{0.1,0.1,0.8},
				{0.1,0.1,0.8},
				{0.25,0.5,0.25}
		};
		
		LDALikelihoodDistance ld = new LDALikelihoodDistance(3,0.01);
		ld.initModel(corpus, vocab, phi);
		ld.setLambda(1.0);
		ld.setMu(1.0);
		
		double ldaProb = ld.calcProbWordGivenTheta(theta, query[0], phi);
		
		double ratio = document.length / (document.length + ld.getMu());
		double expected = 
				Math.log(ld.getLambda() * ((ratio * 1.0) + (1-ratio) * 1/2) + 
				(1-ld.getLambda()) * ldaProb);
		System.out.println("Expected (testCalcLambdaOneLDAWordLikelihood): " + expected + " => "+ Math.exp(expected));
		assertEquals(expected, ld.ldaLoglikelihood(query, document, theta), 0.00000001);
	}

	@Test
	public void testCalc2WordLikelihood() {
		int [][] corpus = {
				{0,0,0,0},
				{0,0,0,0},
				{1,1,1,1},
				{2,2,2,2},
		};
		
		int [] vocab = {0,1,2};
		
		int [] query = {0,1};
		
		int [] document = {0,0,0,1,1,1};
		
		double [] theta = {1/3.0,1/3.0,1/3.0};
		
		double [][] phi = {
				{0.1,0.2,0.7},
				{0.2,0.1,0.7},
				{0.25,0.5,0.25}
		};
		
		LDALikelihoodDistance ld = new LDALikelihoodDistance(3,0.01);
		ld.initModel(corpus, vocab, phi);
		ld.setMu(1.0);
		
		double ldaProb0 = ld.calcProbWordGivenTheta(theta, query[0], phi);
		double ldaProb1 = ld.calcProbWordGivenTheta(theta, query[1], phi);
		
		double ratio = document.length / (document.length + ld.getMu());
		double expectedw0 = 
				ld.getLambda() * ((ratio * 0.5) + (1-ratio) * 1/2) + 
				(1-ld.getLambda()) * ldaProb0;
		double expectedw1 = 
				ld.getLambda() * ((ratio * 0.5) + (1-ratio) * 1/4) + 
				(1-ld.getLambda()) * ldaProb1;
		
		double expected = Math.log(expectedw0) + Math.log(expectedw1); 
		System.out.println("Expected (testCalc2WordLikelihood): " + expected + " => "+ Math.exp(expected));
		assertEquals(expected, ld.ldaLoglikelihood(query, document, theta), 0.00000001);
	}
	
	@Test
	public void testCalcNonWordLikelihood() {
		int [][] corpus = {
				{0,0,0,0},
				{0,0,0,0},
				{1,1,1,1},
				{2,2,2,2},
		};
		
		int [] vocab = {0,1,2};
		
		int [] query = {2};
		
		int [] document = {0,0,0,1,1,1};
		
		double [] theta = {1/3.0,1/3.0,1/3.0};
		
		double [][] phi = {
				{0.1,0.2,0.7},
				{0.2,0.1,0.7},
				{0.25,0.5,0.25}
		};
		
		LDALikelihoodDistance ld = new LDALikelihoodDistance(3,0.01);
		ld.initModel(corpus, vocab, phi);
		ld.setMu(1.0);
		
		double ldaProb0 = ld.calcProbWordGivenTheta(theta, 2, phi);
		
		double ratio = document.length / (document.length + ld.getMu());
		double expected = 
				Math.log(ld.getLambda() * ((ratio * 0.) + (1-ratio) * 1/4) + 
				(1-ld.getLambda()) * ldaProb0);
		System.out.println("Expected (testCalcNonWordLikelihood): " + expected + " => "+ Math.exp(expected));
		assertEquals(expected, ld.ldaLoglikelihood(query, document, theta), 0.00000001);
	}
	
	@Test
	public void test() {
		double [] theta1 = {0.1,0.2,0.7};
		double [] theta2 = {1/3.0,1/3.0,1/3.0};
		double [] theta3 = {0.25,0.25,0.5};
		double [] theta4 = {0.1,0.25,0.65};
		int [] v1 = {0,0,0};
		int [] v2 = {0,0,0};
		int [] v3 = {1,1,1};
		int [] v4 = {0,1,2};
		int [] v5 = {2,1,0};
		
		int [][] trainingset = {
				{0,0,0,0},
				{1,1,1,1},
				{2,2,2,2},
				{0,2,2,0},
				{1,2,2,1},
				{0,1,1,0}
		};
		
		LDALikelihoodDistance ld = new LDALikelihoodDistance(3,0.01);
		
		double [][] phi = {
				{0.1,0.2,0.7},
				{0.2,0.1,0.7},
				{0.25,0.5,0.25}
		};
		
		int [] vocab = {0,1,2};
		ld.setPhi(phi);
		ld.initModel(trainingset, vocab);
		double dist = ld.ldaLoglikelihood(v1, v2, theta1);
		System.out.println("Dist: " + dist);
		dist = ld.ldaLoglikelihood(v1, v2, theta2);
		System.out.println("Dist: " + dist);
		dist = ld.ldaLoglikelihood(v1, v2, theta3);
		System.out.println("Dist: " + dist);
		dist = ld.ldaLoglikelihood(v1, v3, theta4);
		System.out.println("Dist: " + dist);
		dist = ld.ldaLoglikelihood(v1, v4, theta4);
		System.out.println("Dist: " + dist);
		dist = ld.ldaLoglikelihood(v1, v5, theta4);
		System.out.println("Dist: " + dist);
		dist = ld.ldaLoglikelihood(v1, v5, theta2);
		System.out.println("Dist: " + dist);
	}
	
	@Test
	public void testIR() {
		String [] doclines = {
				"Xyzzy reports a profit but revenue is down", 
				"Quorus narrows quarter loss but revenue decreases further"};
		InstanceList train = LDAUtils.loadInstancesStrings(doclines, "X", null, null);
		//System.out.println(train.getAlphabet());
		assertEquals(14,train.getAlphabet().size());
		
		LDALikelihoodDistance cd = new LDALikelihoodDistance(3,0.01);
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
		assertEquals(expected, 1-Math.exp(-result1), 0.0001);

		double [] v3 = tv.instanceToVector(train.get(1));
		double result2 = cd.calculate(v1,v3);
		
		double expected2 = 1-(1/256.0);
		assertEquals(expected2, 1-Math.exp(-result2), 0.0001);
	}
	
	@Test
	public void testPatents() throws FileNotFoundException {
		List<String> doclines = LDAUtils.loadDatasetAsString("/Users/eralljn/Documents/Datasets/Patents/csv2019-05-20-16-21-29.lda-processed.lda-tsne_labeled.lda");
		InstanceList train = LDAUtils.loadInstancesStrings(doclines.toArray(new String[0]));
		
		LDALikelihoodDistance cd = new LDALikelihoodDistance(3,0.01);
		cd.init(train);
		cd.setLambda(1.0);
		cd.setMixtureRatio(1/2.0);
		
		LikelihoodDistance ld = new LikelihoodDistance();
		ld.init(train);
		ld.setMixtureRatio(1/2.0);
		
		String [] doclinesTest = {"circuit transistor"};
		
		InstanceList test = LDAUtils.loadInstancesStrings(doclinesTest,train.getPipe());
		assertEquals("circuit, transistor", LDAUtils.instanceToString(test.get(0)));

		TokenFrequencyVectorizer tv = new TokenFrequencyVectorizer(); 

		for(Instance instance : train) {
			double [] query = tv.instanceToVector(test.get(0));
			double [] document = tv.instanceToVector(instance);
			double ldadist = cd.calculate(query, document);
			double likdist = ld.calculate(query, document);
			assertEquals(ldadist, likdist, 0.0001);
		}
		
	}

}
