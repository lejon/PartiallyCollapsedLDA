package cc.mallet.types;

import java.util.Arrays;

import cc.mallet.util.ParallelRandoms;

public class ConditionalDirichlet extends ParallelDirichlet{
	
	public ConditionalDirichlet(Alphabet dict, double alpha) {
		super(dict, alpha);
		// TODO Auto-generated constructor stub
	}

	public ConditionalDirichlet(Alphabet dict) {
		super(dict);
		// TODO Auto-generated constructor stub
	}

	public ConditionalDirichlet(double m, double[] p) {
		super(m, p);
		// TODO Auto-generated constructor stub
	}

	public ConditionalDirichlet(double[] alphas, Alphabet dict) {
		super(alphas, dict);
		// TODO Auto-generated constructor stub
	}

	public ConditionalDirichlet(double[] p) {
		super(p);
		// TODO Auto-generated constructor stub
	}

	public ConditionalDirichlet(int size, double alpha) {
		super(size, alpha);
		// TODO Auto-generated constructor stub
	}

	public ConditionalDirichlet(int size) {
		super(size);
		// TODO Auto-generated constructor stub
	}

	
	/** 
	 * Draw a conditional Dirichlet distribution. This version MODIFIES the input
	 * argument Phi and updates the indices in phi_index with new draws
	 * 
	 * @param phi Previous Dirichlet draw
	 * @param phi_index Part of phi to produce new draw conditional draw for.
	 * 
	 */
	public void setNextConditionalDistribution(double[] phi, int[] phi_index) {
		// For each dimension in phi_index, draw a sample from Gamma(mp_i, 1)
		double sum_gamma = 0;
		double sum_phi = 0;
		for (int i = 0; i < phi_index.length; i++) {
			sum_phi += phi[phi_index[i]];
			// Now phi in phi_index contain gammas.
			phi[phi_index[i]] = ParallelRandoms.rgamma(partition[phi_index[i]] * magnitude, 1, 0);
			if (phi[phi_index[i]] <= 0) {
				phi[phi_index[i]] = 0.0001;
			}
			sum_gamma += phi[phi_index[i]]; 
		}

//		Normalize part of dirichlet
		for (int i = 0; i < phi_index.length; i++) {
			phi[phi_index[i]] = (phi[phi_index[i]] / sum_gamma) * sum_phi;
		}
	}

	/** 
	 * Draw a conditional Dirichlet distribution. 
	 * 
	 * @param phi Previous Dirichlet draw
	 * @param phi_index Part of phi to produce new draw conditional draw for.
	 * 
	 */
	public double [] nextConditionalDistribution(double[] phiArg, int[] phi_index) {
		// Create the resulting Phi
		double [] phi = Arrays.copyOf(phiArg, phiArg.length);
		// For each dimension in phi_index, draw a sample from Gamma(mp_i, 1)
		double sum_gamma = 0;
		double sum_phi = 0;
		for (int i = 0; i < phi_index.length; i++) {
			sum_phi += phi[phi_index[i]];
			// Now phi in phi_index contain gammas.
			phi[phi_index[i]] = ParallelRandoms.rgamma(partition[phi_index[i]] * magnitude, 1, 0);
			if (phi[phi_index[i]] <= 0) {
				phi[phi_index[i]] = 0.0001;
			}
			sum_gamma += phi[phi_index[i]]; 
		}

//		Normalize part of dirichlet
		for (int i = 0; i < phi_index.length; i++) {
			phi[phi_index[i]] = (phi[phi_index[i]] / sum_gamma) * sum_phi;
		}
		return phi;
	}
}
