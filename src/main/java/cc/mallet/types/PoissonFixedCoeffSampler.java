package cc.mallet.types;

import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.math3.util.CombinatoricsUtils;
import org.apache.commons.math3.util.FastMath;

import cc.mallet.util.OptimizedGentleAliasMethod;
import cc.mallet.util.WalkerAliasTable;

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
			return nextStandardPoisson(beta + betaAdd);
		}
	}

	// HACK: copy/pasted to avoid instantiating PoissonDistribution classes that won't be used except for random draws
	// taken from ApacheCommons Math and modified for ThreadLocalRandom and nextStandardExponential (Apache licensed)
	// no idea how good or how bad this algorithm is, for mean < 40, it uses IID exponentials, which seems inefficient
	protected long nextStandardPoisson(double meanPoisson) {
		final double pivot = 40.0d;
		if (meanPoisson < pivot) {
			double p = FastMath.exp(-meanPoisson);
			long n = 0;
			double r = 1.0d;
			double rnd = 1.0d;

			while (n < 1000 * meanPoisson) {
				rnd = ThreadLocalRandom.current().nextDouble();
				r *= rnd;
				if (r >= p) {
					n++;
				} else {
					return n;
				}
			}
			return n;
		} else {
			final double lambda = FastMath.floor(meanPoisson);
			final double lambdaFractional = meanPoisson - lambda;
			final double logLambda = FastMath.log(lambda);
			final double logLambdaFactorial = CombinatoricsUtils.factorialLog((int) lambda);
			final long y2 = lambdaFractional < Double.MIN_VALUE ? 0 : nextStandardPoisson(lambdaFractional);
			final double delta = FastMath.sqrt(lambda * FastMath.log(32 * lambda / FastMath.PI + 1));
			final double halfDelta = delta / 2;
			final double twolpd = 2 * lambda + delta;
			final double a1 = FastMath.sqrt(FastMath.PI * twolpd) * FastMath.exp(1 / (8 * lambda));
			final double a2 = (twolpd / delta) * FastMath.exp(-delta * (1 + delta) / twolpd);
			final double aSum = a1 + a2 + 1;
			final double p1 = a1 / aSum;
			final double p2 = a2 / aSum;
			final double c1 = 1 / (8 * lambda);

			double x = 0;
			double y = 0;
			double v = 0;
			int a = 0;
			double t = 0;
			double qr = 0;
			double qa = 0;
			for (;;) {
				final double u = ThreadLocalRandom.current().nextDouble();
				if (u <= p1) {
					final double n = ThreadLocalRandom.current().nextGaussian();
					x = n * FastMath.sqrt(lambda + halfDelta) - 0.5d;
					if (x > delta || x < -lambda) {
						continue;
					}
					y = x < 0 ? FastMath.floor(x) : FastMath.ceil(x);
					final double e = nextStandardExponential();
					v = -e - (n * n / 2) + c1;
				} else {
					if (u > p1 + p2) {
						y = lambda;
						break;
					} else {
						x = delta + (twolpd / delta) * nextStandardExponential();
						y = FastMath.ceil(x);
						v = -nextStandardExponential() - delta * (x + 1) / twolpd;
					}
				}
				a = x < 0 ? 1 : 0;
				t = y * (y + 1) / (2 * lambda);
				if (v < -t && a == 0) {
					y = lambda + y;
					break;
				}
				qr = t * ((2 * y + 1) / (6 * lambda) - 1);
				qa = qr - (t * t) / (3 * (lambda + a * (y + 1)));
				if (v < qa) {
					y = lambda + y;
					break;
				}
				if (v > qr) {
					continue;
				}
				if (v < y * logLambda - CombinatoricsUtils.factorialLog((int) (y + lambda)) + logLambdaFactorial) {
					y = lambda + y;
					break;
				}
			}
			return y2 + (long) y;
		}
	}

	// exponential RV sampler via inversion method
	protected double nextStandardExponential() {
		for(;;) {
			double u = ThreadLocalRandom.current().nextDouble();
			double e = -FastMath.log(u);
			if(e > 0 && e < Double.MAX_VALUE && e == e) { // check for zero, positive infinity, and NaN
				return e;
			}
		}
	}
}
