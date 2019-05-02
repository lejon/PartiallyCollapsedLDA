package cc.mallet.types;

import cc.mallet.util.ParallelRandoms;

public class MarsagliaSparseDirichlet extends ParallelDirichlet implements SparseDirichlet {
	double [] cs;
	double [] ds;

	public MarsagliaSparseDirichlet(double[] prior) {
		super(prior);
		cs = new double[prior.length];
		ds = new double[prior.length];
		for (int idx = 0; idx < prior.length; idx++) {
			double [] params = ParallelRandoms.preCalcParams(partition[idx]*magnitude);
			cs[idx] = params[0];
			ds[idx] = params[1];
		}
	}
	
	public MarsagliaSparseDirichlet(int size, double prior) {
		super(size, prior);
		cs = new double[size];
		ds = new double[size];
		double [] params = ParallelRandoms.preCalcParams(partition[0]*magnitude);
		for (int idx = 0; idx < size; idx++) {
			cs[idx] = params[0];
			ds[idx] = params[1];
		}
	}

	public double[] nextDistribution(int [] counts) {
		double distribution[] = new double[partition.length];

		double sum = 0;
		for (int i=0; i<distribution.length; i++) {
			// If the count is 0 use the precalculated version of Marsaglias gamma sampler
			if(counts[i]==0) {
				distribution[i] = ParallelRandoms.nextGammaPreCalc((partition[i] * magnitude), ds[i], cs[i]);
			} else {
				distribution[i] = ParallelRandoms.rgamma((partition[i] * magnitude) + counts[i], 1, 0);
			}
			sum += distribution[i];
		}

		if(sum!=0) {
			for (int i=0; i<distribution.length; i++) {
				distribution[i] /= sum;
				if (distribution[i] <= 0) {
					distribution[i] = Double.MIN_VALUE;
				}			
			}
		}

		return distribution;
	}

	@Override
	public VSResult nextDistributionWithSparseness() {
		int [] nonZero = new int[0]; 
		return new VSResult(nextDistribution(), nonZero);
	}

	@Override
	public VSResult nextDistributionWithSparseness(int[] counts) {
		int [] nonZero = new int[0]; 
		return new VSResult(nextDistribution(counts), nonZero);
	}

	@Override
	public VSResult nextDistributionWithSparseness(double prior) {
		double distribution[] = new double[partition.length];
		int [] nonZero = updateDistributionWithSparseness(distribution, prior);
		return new VSResult(distribution, nonZero);
	}

	@Override
	public int [] updateDistributionWithSparseness(double[] previousDistribution, double prior) {
		int [] nonZero = new int[previousDistribution.length];  
		double sum = 0;
		for (int i=0; i<previousDistribution.length; i++) {
			previousDistribution[i] = ParallelRandoms.rgamma(prior + previousDistribution[i], 1, 0);
			sum += previousDistribution[i];
			nonZero[i] = i;
		}

		if(sum!=0) {
			for (int i=0; i<previousDistribution.length; i++) {
				previousDistribution[i] /= sum;
				if (previousDistribution[i] <= 0) {
					previousDistribution[i] = Double.MIN_VALUE;
				}			
			}
		}
		return nonZero;
	}

	
}
