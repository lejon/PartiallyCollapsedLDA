package cc.mallet.types;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.math3.util.CombinatoricsUtils;
import org.apache.commons.math3.util.FastMath;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;

public class PolyaUrnDirichlet extends ParallelDirichlet implements SparseDirichlet {

	public PolyaUrnDirichlet(double[] prior) {
		super(prior);
	}

	public PolyaUrnDirichlet(int size, double prior) {
		super(size, prior);
	}

	@Override
	public VSResult nextDistributionWithSparseness(int [] counts) {
		double distribution[] = new double[partition.length];
		TIntArrayList resultingNonZeroIdxs = new TIntArrayList();
		double sum = 0;

		// implements the Poisson Polya Urn
		for (int i=0; i<distribution.length; i++) {
			// determine whether we land in F_0 or F^_n
			distribution[i] = (double) nextPoisson(partition[i] * magnitude + (double) counts[i]);
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
	public VSResult nextDistributionWithSparseness(double prior) {
		double distribution[] = new double[partition.length];
		int [] nonZero = updateDistributionWithSparseness(distribution, prior);
		return new VSResult(distribution, nonZero);
	}

	@Override
	public int[] updateDistributionWithSparseness(double [] target, double prior) {		
		TIntHashSet resultingNonZeroIdxs = new TIntHashSet();
		Arrays.fill(target, 0.0);
		// (2) Draw \nu_k ~ Poisson(length * prior)
		long nu_k = PolyaUrnDirichlet.nextPoisson(target.length * prior);
		
		for(int i = 0; i < nu_k; i++) {
			int u = ThreadLocalRandom.current().nextInt(target.length);
			//(3) For i=1,..,\nu, choose a column in distribution uniformly at random, and add 1 to it
			target[u]++;
			resultingNonZeroIdxs.add(u);
		}
		
		// Normalize the rows
		int [] nonZero = resultingNonZeroIdxs.toArray();
		if(nu_k>0) {
			for(int i = 0; i < nonZero.length; i++) {
				int idx = nonZero[i];
				target[idx] /= (double) nu_k;
			}
		}
		return nonZero;
	}
	
	@Override
	public double[] nextDistribution(int[] counts) {
		return nextDistributionWithSparseness(counts).phiRow;
	}

	@Override
	public VSResult nextDistributionWithSparseness() {
		throw new java.lang.UnsupportedOperationException();
	}
	
	public static long nextPoisson(double meanPoisson) {
		return nextPoissonCommons(meanPoisson);
	}

	/**
	 * Normal approximation of Poisson draw. Should only be used when meanPoisson is
	 * large enough for the normal approximation to be valid. 
	 * 
	 * @param meanPoisson
	 * @return
	 */
	public static long nextPoissonNormalApproximation(double meanPoisson) {
		long sample = Math.round(Math.sqrt(meanPoisson) * ThreadLocalRandom.current().nextGaussian() + (meanPoisson));
		if(sample<0) 
			System.err.println("WARNING: PolyaUrnDirichlet.nextPoissonNormalApproximation drew negative value! Normal approximation should only be used if mean is large is enough. Mean was: " + meanPoisson);
		return sample;
	}
	
	/**
	 * This is an O(1) algorithm for any lambda. The first half of the code is taken from Breeze, 
	 * which appears to implement the method in Kemp (1991). Second half of the code is the PA 
	 * method in Atkinson (1979) for large lambda, based on pseudo code I found online. The cutoff 
	 * value (500) between the two methods was chosen by trial and error.
	 * 
	 * @autor Alexander Terenin (ported from Scala by Leif Jonsson)
	 * 
	 * @param lambda Rate for the Poisson
	 * @return a Possion covariate
	 */
	
	// #########  Seem to be something wrong with this version, seems to end up in an endless while loop...

	//	public static long nextPoissonO1(double lambda) {
//		if(lambda == 0) return 0;
//		else if(lambda < 10.0) {
//			double t = Math.exp(-lambda);
//			int k = 0;
//			double u = Math.random();
//			double s = t;
//			while(s < u) {
//				k += 1;
//				t *= lambda / k;
//				s += t;
//			}
//			return k;
//		} else if(lambda < 500.0) {
//			int k_start = (int) lambda;
//			double u = Math.random();
//			double t1 = Math.exp(k_start * Math.log(lambda) - lambda - org.apache.commons.math3.special.Gamma.logGamma(lambda+1));
//			if (t1 > u)
//				return k_start;
//			else {
//				int k1 = k_start;
//				int k2 = k_start;
//				double t2 = t1;
//				double s = t1;
//				while(true) {
//					k1 += 1;
//					t1 *= lambda / k1; s += t1;
//					if (s > u) return k1;
//					if (k2 > 0) {
//						t2 *= k2 / lambda;
//						k2 -= 1;
//						s += t2;
//						if (s > u) return k2;
//					}
//				}
//			}
//		} else {
//			double c = 0.767 - 3.36 / lambda;
//			double beta = Math.PI / Math.sqrt(3.0 * lambda);
//			double alpha = beta * lambda;
//			double k = Math.log(c) - lambda - Math.log(beta);
//			while (true) {
//				double u = Math.random();
//				double x = (alpha - Math.log((1.0 - u) / u)) / beta;
//				int n = (int) Math.floor(x + 0.5);
//				if (n > 0) {
//					double v = Math.random();
//					double y = alpha - beta * x;
//					double l = y + Math.log(v) - 2.0 * Math.log(1.0 + Math.exp(y));
//					double r = k + n * Math.log(lambda) - org.apache.commons.math3.special.Gamma.logGamma(n + 1);
//					if (l <= r)
//						return n;
//				}
//			}
//		}
//		// LEIF: When is this exception supposed to be thrown? After certain number of loops?   
//		// throw new Exception("exited infinite loop");
//	}

	
	/**
	 * HACK: copy/pasted to avoid instantiating PoissonDistribution classes that won't be used except for random draws
	 * taken from ApacheCommons Math and modified for ThreadLocalRandom and nextStandardExponential (Apache licensed)
	 * no idea how good or how bad this algorithm is, for mean < 40, it uses IID exponentials, which seems inefficient
	 * 
	 * @param meanPoisson
	 * @return Poisson covariate
	 */
	public static long nextPoissonCommons(double meanPoisson) {
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
			final long y2 = lambdaFractional < Double.MIN_VALUE ? 0 : nextPoissonCommons(lambdaFractional);
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
	public static double nextStandardExponential() {
		for(;;) {
			double u = ThreadLocalRandom.current().nextDouble();
			double e = -FastMath.log(u);
			if(e > 0 && e < Double.MAX_VALUE && e == e) { // check for zero, positive infinity, and NaN
				return e;
			}
		}
	}
}
