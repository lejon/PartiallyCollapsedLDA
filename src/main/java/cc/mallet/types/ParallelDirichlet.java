package cc.mallet.types;

import cc.mallet.util.ParallelRandoms;

public class ParallelDirichlet extends Dirichlet {

	public ParallelDirichlet(Alphabet dict, double alpha) {
		super(dict, alpha);
		random = new ParallelRandoms();
	}

	public ParallelDirichlet(Alphabet dict) {
		super(dict);
		random = new ParallelRandoms();
	}

	public ParallelDirichlet(double m, double[] p) {
		super(m, p);
		random = new ParallelRandoms();
	}

	public ParallelDirichlet(double[] alphas, Alphabet dict) {
		super(alphas, dict);
		random = new ParallelRandoms();
	}

	public ParallelDirichlet(double[] p) {
		super(p);
		random = new ParallelRandoms();
	}

	public ParallelDirichlet(int size, double alpha) {
		super(size, alpha);
		random = new ParallelRandoms();
	}

	public ParallelDirichlet(int size) {
		super(size);
		random = new ParallelRandoms();
	}
	
	// We override this method since we don't want to set 
	// distribution[i] = 0.0001 if the gamma draw == 0
	// We instead use Double.MIN_VALUE
	@Override
	public double[] nextDistribution() {
		double distribution[] = new double[partition.length];
		if(random==null) {
			random = new ParallelRandoms();
		}

//		For each dimension, draw a sample from Gamma(mp_i, 1)
		double sum = 0;
		for (int i=0; i<distribution.length; i++) {
			distribution[i] = random.nextGamma(partition[i] * magnitude, 1);
			sum += distribution[i];
		}

//		Normalize
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

}
