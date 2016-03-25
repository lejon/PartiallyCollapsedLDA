package cc.mallet.util;

import java.util.concurrent.ThreadLocalRandom;

public class OptimizedGentleAliasMethodConstantMemory extends OptimizedGentleAliasMethod {
	/*
	Random random = new Random();
	int k;
	double [] ps;
	int [] a;
	double [] bs;
	int [] lows;
	int [] highs;
	*/
	
	int tableSize;
	
	public OptimizedGentleAliasMethodConstantMemory() {
		
	}
	public OptimizedGentleAliasMethodConstantMemory(double [] pis, double normalizer, int maxSize) {
		generateAliasTable(pis, normalizer, maxSize);
	}
	
	public OptimizedGentleAliasMethodConstantMemory(double [] pis, int maxSize) {
		generateAliasTable(pis, 1.0, maxSize);
	}

	@Override
	public void initTableNormalizedProbabilities(double[] probabilities) {
		generateAliasTable(probabilities);
	}
	
	@Override
	public void initTable(double[] probabilities, double normalizer) {
		generateAliasTable(probabilities,normalizer);
		
	}
	
	public void generateAliasTable(double [] pi, double normalizer, int maxSize) {
		lows  = new int[maxSize];
		highs = new int[maxSize];
		ps    = new double[maxSize];
		bs    = new double[maxSize];
		a     = new int [maxSize];
		tableSize = maxSize;
		
		reGenerateAliasTable(pi, normalizer);
	}
	
	public void reGenerateAliasTable(double[] pi, double normalizer) {
		k = pi.length;
		if(k > tableSize) throw new IllegalArgumentException("Cannot call reGenerate with more prob than table size!");
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
		generateAliasTable(pi, 1.0, pi.length);
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
		OptimizedGentleAliasMethodConstantMemory ga = new OptimizedGentleAliasMethodConstantMemory();
		double [] pi = {0.3, 0.05, 0.2, 0.4, 0.05};
		int arraySize = 20;
		ga.generateAliasTable(pi, arraySize);
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
