package cc.mallet.types;

import org.apache.commons.math3.distribution.PoissonDistribution;

import cc.mallet.util.OptimizedGentleAliasMethod;
import cc.mallet.util.WalkerAliasTable;

/**
 * Implementation of an efficient sampler for the Poisson distribution

 * This sampler is efficient for the special case when we know that we will draw many values from a Poisson with mean 'beta+count'.
 * I.e some fixed 'beta' value plus low counts. This is typically the case in LDA (Latent Dirichlet Allocation).
 * When this is the case, we can pre-generate WalkerAlias tables for counts less than some value (say 100). This means that
 * draws from the Poisson for count values < 100 will be very fast, where the standard Poisson draw is quite expensive.
 * For draws when lambda is bigger than 100, the Poisson draw can be well approximated with a normal distribution and will also be fast.
 * 
 * @author Leif Jonsson
 *
 */
public class PoissonFixedCoeffSampler {

	int L;
	double beta;
	WalkerAliasTable [] aliasTables;

	public PoissonFixedCoeffSampler(double beta, int L) {
		this.L = L;
		this.beta = beta;
		aliasTables = new WalkerAliasTable[L];
		for (int i = 0; i < aliasTables.length; i++) {
			double [] pis = new double[L*2];
			double lambda = beta+i;
			PoissonDistribution pois = new PoissonDistribution(lambda);
			for (int j = 0; j < pis.length; j++) {
				pis[j] = pois.probability(j);
			}
			aliasTables[i] = createAliasTable(pis);			
		}
	}

	WalkerAliasTable createAliasTable(double [] pis) {
		return new OptimizedGentleAliasMethod(pis);
	}

	public long nextPoisson(int betaAdd) {
		if(betaAdd<L) {
			return aliasTables[betaAdd].generateSample();
		} else {
			return PolyaUrnDirichlet.nextPoissonNormalApproximation(beta + betaAdd);
		}
	}
}
