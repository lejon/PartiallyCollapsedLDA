package cc.mallet.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * This algorithm implements Marsaglias algorithm for very fast drawing of
 * gamma distributed covariates.
 * 
 * @author Leif Jonsson
 *
 */
public class ParallelRandoms extends Randoms {

	private static final long serialVersionUID = 2720201250960273621L;
	
	private static final ThreadLocal<XORShiftRandom> gaussGen = new ThreadLocal<XORShiftRandom>() {
		@Override
		protected XORShiftRandom initialValue() {
			return new XORShiftRandom();
		}
	};
	{
		gaussGen.set(new XORShiftRandom());
	}

	/* (non-Javadoc)
	 * @see cc.mallet.util.Randoms#nextGamma(double, double)
	 */
	@Override
	public double nextGamma(double alpha, double beta) {
		return rgamma(alpha, beta, 0);
	}
	
	/* (non-Javadoc)
	 * @see cc.mallet.util.Randoms#nextGamma(double, double, double)
	 */
	@Override
	public double nextGamma(double alpha, double beta, double lambda) {
		return rgamma(alpha, beta, lambda);
	}
	
	public double nextBeta(double alpha, double beta) {
		return rbeta(alpha, beta);
	}
	
	public static double rbeta(double alpha, double beta) {
		double x = rgamma(alpha, 1.0, 0);
        double y = rgamma(beta, 1.0, 0);
        return x / (x + y);
	}
	
	/**
	 * Draw gamma random covariate using Marsaglias fast algorithm
	 * 
	 * @param alpha
	 * @param beta
	 * @param lambda
	 * @return gamma distributed covariate
	 */
	public static double rgamma(double alpha, double beta, double lambda) {
		if (alpha <= 0 || beta <= 0) {
			throw new IllegalArgumentException ("alpha and beta must be strictly positive: alpha = " + alpha + " beta = " + beta + " lambda = " + lambda);
		}
		if(alpha<1) {
			double u=ThreadLocalRandom.current().nextDouble();
			return ((prgamma(1+alpha) * Math.pow(u, 1.0/alpha)) * beta)+lambda;
		} else {
			return (prgamma(alpha) * beta) + lambda;
		}
	}
		
	/**
	 * Draw <code>noSamples</code> gamma random covariate using Marsaglias fast algorithm
	 * This is faster than calling the 'one sample' method many times since this method 
	 * caches some values used in the drawing.
	 * 
	 * @param alpha
	 * @param beta
	 * @param lambda
	 * @param noSamples
	 * @return an array with <code>noSamples</code> gamma distributed covariates
	 */
	public static double [] rgamma(double alpha, double beta, double lambda, int noSamples) {
		if (alpha <= 0 || beta <= 0) {
			throw new IllegalArgumentException ("alpha and beta must be strictly positive.");
		}
		double [] samples = new double[noSamples];
		if(alpha<1) {
			double d = (1.0+alpha)-(1.0/3.0); 
			double c = 1.0/Math.sqrt(9.0*d);
			for (int i = 0; i < noSamples; i++) {				
				double u=ThreadLocalRandom.current().nextDouble();
				samples[i]  = ((prgamma(d,c) * Math.pow(u, 1.0/alpha)) * beta) + lambda;
			}
		} else {
			double d = alpha-(1.0/3.0); 
			double c = 1.0/Math.sqrt(9.0*d);
			for (int i = 0; i < noSamples; i++) {	
				samples[i] = (prgamma(d,c) * beta) + lambda;
			}
		}
		return samples;
	}
	
	/**
	 * Draws a new gamma with d and c pre-calculated. Assumes beta = 1 and lambda = 0
	 * @param d
	 * @param c
	 * @return new gamma distributed sample
	 */
	public static double nextGammaPreCalc(double alpha, double d, double c) {
		if(alpha<1) {
			double u=ThreadLocalRandom.current().nextDouble();
			return prgamma(d,c) * Math.pow(u, 1.0/alpha);
		} else {			
			return prgamma(d,c);
		}
	}
	
	/**
	 * Pre calculates d and c in Marsaglias algorithm to save computation of these 
	 * constants for given alphas. This can be used in combination with <code>nextGammaPreCalc</code>
	 * @param alpha
	 * @return array of length 2 where the first value is 'c' and the other is 'd'
	 */
	public static double [] preCalcParams(double alpha) {
		if (alpha <= 0) {
			throw new IllegalArgumentException ("alpha must be strictly positive.");
		}
		double d;
		double c;
		if(alpha<1) {
			d = (1.0+alpha)-(1.0/3.0); 
		} else {
			d = alpha-(1.0/3.0); 
		}
		c = 1.0/Math.sqrt(9.0*d);
		double [] params = {c,d};
		return params;
	}
	
	
	/**
	 * The full Marsaglia gamma covariate sampler, for alpha > 1
	 * @param alpha
	 * @return gamma distributed sample
	 */
	protected static double prgamma(double alpha) {
		double x,v,u;
		double d = alpha-(1.0/3.0); 
		double c = 1.0/Math.sqrt(9.0*d);
		while(true) {
			do {x=gaussGen.get().nextGaussian(); v=1.0+c*x;} while(v<=0.0);
			v=v*v*v; 
			u=gaussGen.get().nextDouble();
			if( u<(1.0-0.0331*(x*x)*(x*x)) ) return (d*v);
			if( Math.log(u)<(0.5*x*x+d*(1.0-v+Math.log(v))) ) return (d*v);
		}
	}
	
	/**
	 * The full Marsaglia gamma covariate sampler, for alpha > 1
	 * @param alpha
	 * @return gamma distributed sample
	 */
	protected static double prgammaOrig(double alpha) {
		double x,v,u;
		double d = alpha-(1.0/3.0); 
		double c = 1.0/Math.sqrt(9.0*d);
		while(true) {
			do {x=ThreadLocalRandom.current().nextGaussian(); v=1.0+c*x;} while(v<=0.0);
			v=v*v*v; 
			u=ThreadLocalRandom.current().nextDouble();
			if( u<(1.0-0.0331*(x*x)*(x*x)) ) return (d*v);
			if( Math.log(u)<(0.5*x*x+d*(1.0-v+Math.log(v))) ) return (d*v);
		}
	}
	
	/**
	 * The full Marsaglia gamma covariate sampler, for alpha > 1 with c and d 
	 * pre-calculated
	 * @param d
	 * @param c
	 * @return gamma distributed sample
	 */
	protected static double prgamma(double d, double c) {
		double x,v,u;
		while(true) {
			do {x=ThreadLocalRandom.current().nextGaussian(); v=1.0+c*x;} while(v<=0.0);
			v=v*v*v; 
			u=ThreadLocalRandom.current().nextDouble();
			if( u<(1.0-0.0331*(x*x)*(x*x)) ) return (d*v);
			if( Math.log(u)<(0.5*x*x+d*(1.0-v+Math.log(v))) ) return (d*v);
		}
	}
}
