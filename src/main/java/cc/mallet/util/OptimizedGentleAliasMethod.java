package cc.mallet.util;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class OptimizedGentleAliasMethod implements WalkerAliasTable {
	Random random = new Random();
	
	int k;
	double [] ps;
	int [] a;
	double [] bs;
	int [] lows;
	int [] highs;
	
	public OptimizedGentleAliasMethod() {
		
	}
	public OptimizedGentleAliasMethod(double [] pis, double normalizer) {
		generateAliasTable(pis,normalizer);
	}
	
	public OptimizedGentleAliasMethod(double [] pis) {
		generateAliasTable(pis);
	}

	@Override
	public void initTableNormalizedProbabilities(double[] probabilities) {
		generateAliasTable(probabilities);
	}
	
	@Override
	public void initTable(double[] probabilities, double normalizer) {
		generateAliasTable(probabilities,normalizer);
		
	}
	
	public void generateAliasTable(double [] pi, double normalizer) {
		k = pi.length;
		lows  = new int[k];
		highs = new int[k];
		ps    = new double[k];
		bs    = new double[k];
		a     = new int [k];
		
		reGenerateAliasTable(pi, normalizer);
	}
	
	public void reGenerateAliasTable(double[] pi, double normalizer) {
		if(pi.length!=k) throw new IllegalArgumentException("Cannot call reGenerate with different length probabilities!");
		int lowCnt = 0;
		int highCnt = 0;
		double k1 = 1.0/k;
		for (int i = 0; i < k; i++) {
			a[i] = i;
			bs[i] = (pi[i]/normalizer) - k1;
			if(bs[i]<0.0) {
				lows[lowCnt++] = i;
			} else {
				highs[highCnt++] = i;
			}
		}
		int steps = 0;
		while(steps<=k&&lowCnt>0&&highCnt>0) {
			int l = lows[--lowCnt];
			int h = highs[highCnt-1];
			double c=bs[l];
			double d=bs[h];
			bs[l] = 0;
			bs[h] = c + d;
			if(bs[h]<=0) {highCnt--;}
			if(bs[h]<0) {lows[lowCnt++] = h;}
			a[l] = h;
			ps[l] = 1.0 + ((double) k) * c;
		}
	}
	
	public void generateAliasTable(double [] pi) {
		generateAliasTable(pi,1.0);
	}
	
	public String toString() {
		String res = "PSes: ";
		for (int psi = 0; psi < ps.length; psi++) {
			res += ps[psi] + ", ";
		}
		return res;
	}
	
	@Override
	public int generateSample() {
		double u = ThreadLocalRandom.current().nextDouble();
		return generateSample(u);
	}
	
	@Override
	public int generateSample(double u) {
//		int i=random.nextInt(k);
//		if (u>ps[i]) i=a[i]; return i; // ups-i follows a U ~ (0,1)
		 // Also correct... 
		double ups = (u*k); // WARNING: The code MUST be exactly like this
		int i = (int) ups;  // including keeping the double and casting it to an int
		if ((ups-i)>ps[i]) i=a[i]; return i; // ups-i follows a U ~ (0,1)
	}
	
	public static void main(String [] args) {
		OptimizedGentleAliasMethod ga = new OptimizedGentleAliasMethod();
		double [] pi = {0.3, 0.05, 0.2, 0.4, 0.05};
		ga.generateAliasTable(pi);
		int [] counts = new int[pi.length];
		int noSamples = 10_000_000;
		for(int i = 0; i<noSamples; i++) {
			counts[ga.generateSample()]++;
		}
		System.out.println("Ratios are:");
		for (int i = 0; i < counts.length; i++) {
			System.out.println(((double)counts[i])/((double)noSamples));
		}
	}
	
}
