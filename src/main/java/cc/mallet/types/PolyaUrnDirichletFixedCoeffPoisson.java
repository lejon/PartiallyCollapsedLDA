package cc.mallet.types;

import gnu.trove.TIntArrayList;
import scala.NotImplementedError;

public class PolyaUrnDirichletFixedCoeffPoisson extends PolyaUrnDirichlet implements SparseDirichlet {

	PoissonFixedCoeffSampler fep;
	
	public PolyaUrnDirichletFixedCoeffPoisson(int size, double prior, PoissonFixedCoeffSampler fep) {
		super(size, prior);
		this.fep = fep;
	}

	public VSResult nextDistributionWithSparseness(int [] counts) {
		double distribution[] = new double[partition.length];
		TIntArrayList resultingNonZeroIdxs = new TIntArrayList();
		double sum = 0;

		// implements the Poisson Polya Urn
		for (int i=0; i<distribution.length; i++) {
			distribution[i] = fep.nextPoisson(counts[i]);
			sum += distribution[i];
			if(distribution[i]!=0) {
				resultingNonZeroIdxs.add(i);
			}
		}

		for (int i=0; i<distribution.length; i++) {
			distribution[i] /= sum;
		}

		return new VSResult(distribution, resultingNonZeroIdxs.toNativeArray());
	}
	
	@Override
	public double[] nextDistribution(int[] counts) {
		return nextDistributionWithSparseness(counts).phiRow;
	}

	@Override
	public VSResult nextDistributionWithSparseness() {
		throw new NotImplementedError();
	}
}
