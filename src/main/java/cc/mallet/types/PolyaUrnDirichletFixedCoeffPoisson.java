package cc.mallet.types;

import java.util.Arrays;

import gnu.trove.TIntArrayList;

public class PolyaUrnDirichletFixedCoeffPoisson extends PolyaUrnDirichlet implements SparseDirichlet {

	PoissonFixedCoeffSampler fep;
	
	public PolyaUrnDirichletFixedCoeffPoisson(int size, double prior, PoissonFixedCoeffSampler fep) {
		super(size, prior);
		this.fep = fep;
	}
	
	@Override
	public VSResult nextDistributionWithSparseness(int [] counts) {
		double distribution[] = new double[partition.length];
		int [] resultingNonZeroIdxs = new int[distribution.length];
		double sum = 0;

		int cnt = 0;
		// implements the Poisson Polya Urn
		for (int i=0; i<distribution.length; i++) {
			distribution[i] = fep.nextPoisson(counts[i]);
			
			sum += distribution[i];
			if(distribution[i]!=0) {
				resultingNonZeroIdxs[cnt++] = i;
			}
		}

		if(sum>0) {
			for (int i=0; i<distribution.length; i++) {
				distribution[i] /= sum;

				// With the Poisson it is allowed to have 0's
				//			if (distribution[i] <= 0) {
				//				distribution[i] = Double.MIN_VALUE;
				//			}			
			}
		}

		return new VSResult(distribution, Arrays.copyOf(resultingNonZeroIdxs,cnt));
	}
	
	public VSResult nextDistributionWithSparsenessOrig(int [] counts) {
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
// With the Poisson it is allowed to have 0's
//			if (distribution[i] <= 0) {
//				distribution[i] = Double.MIN_VALUE;
//			}			
		}

		return new VSResult(distribution, resultingNonZeroIdxs.toNativeArray());
	}
	
	@Override
	public double[] nextDistribution(int[] counts) {
		return nextDistributionWithSparseness(counts).phiRow;
	}

	@Override
	public VSResult nextDistributionWithSparseness() {
		throw new java.lang.UnsupportedOperationException();
	}
}
