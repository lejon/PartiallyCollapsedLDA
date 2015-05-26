package cc.mallet.types;

import cc.mallet.util.ParallelRandoms;

public class SparseDirichlet extends ParallelDirichlet {
	double [] cs;
	double [] ds;

	public SparseDirichlet(double[] prior) {
		super(prior);
		cs = new double[prior.length];
		ds = new double[prior.length];
		for (int idx = 0; idx < prior.length; idx++) {
			double [] params = ParallelRandoms.preCalcParams(partition[idx]*magnitude);
			cs[idx] = params[0];
			ds[idx] = params[1];
		}
	}
	
	public SparseDirichlet(int size, double prior) {
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

		for (int i=0; i<distribution.length; i++) {
			distribution[i] /= sum;
			if (distribution[i] <= 0) {
				distribution[i] = Double.MIN_VALUE;
			}			
		}

		return distribution;
	}

}
