package cc.mallet.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VSDirichletTest {


	@Test
	public void testIndicatorSampling() {
		double beta = 0.1;
		int numTypes = 10;
		int [] zeroCount = new int[numTypes];

		double vsPrior = 0.1;

		double[] dirichletParams = {1, 100, 1, 100, 100, 1200, 100, 100, 1, 1000};		
		Dirichlet dir = new ParallelDirichlet(dirichletParams);

		double[] unifDirichletParams = {1, 1, 1, 1, 1, 1, 1, 1, 1, 1};		
		Dirichlet unifDir = new ParallelDirichlet(unifDirichletParams);

		double [] phiRow = unifDir.nextDistribution();

		SimpleMultinomial mn = new SimpleMultinomial(dir.nextDistribution());
		VariableSelectionDirichlet dist = new VSDirichlet(beta, vsPrior);

		int noLoops = 100;
		for (int loop = 0; loop < noLoops; loop++) {
			int [] relevantTypeTopicCounts = mn.draw(100);

			VariableSelectionResult res = dist.nextDistribution(relevantTypeTopicCounts, phiRow);
			phiRow = res.getPhi();

			int [] zeroIdxs = res.getNonZeroIdxs();

			assertTrue(res.getPhi().length==numTypes);
			assertTrue(zeroIdxs.length<=numTypes);
			double sum = 0.0;
			for (int i = 0; i < res.getPhi().length; i++) {
				assertTrue(res.getPhi()[i]>=0 && res.getPhi()[i]<=1);
				sum += res.getPhi()[i];
				if(res.getPhi()[i]==0.0) {
					zeroCount[i]++;
				}
			}
			assertEquals(1.0,sum,0.0001);
			//System.out.println("Non zero Idxs:" + zeroIdxs.length);
			for (int i = 0; i < zeroIdxs.length; i++) {
				assertTrue(zeroIdxs[i]>=0 && zeroIdxs[i]<=numTypes);
			}
			System.out.println(arrToStr(phiRow, "Phi"));
		}
		System.out.println(arrToStr(zeroCount, "ZeroCount"));
	}

	String arrToStr(double [] arr, String title) {
		String res = ""; 		
		res += title + "[" +  arr.length + "]:";
		for (int j = 0; j < arr.length; j++) { 			
			res += String.format("%.4f",arr[j]) + ", ";
		} 		
		return res;
	}

	String arrToStr(int [] arr, String title) {
		String res = ""; 		
		res += title + "[" +  arr.length + "]:";
		for (int j = 0; j < arr.length; j++) { 			
			res += arr[j] + ", ";
		} 		
		return res;
	}
}
