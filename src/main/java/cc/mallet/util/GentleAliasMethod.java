package cc.mallet.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class GentleAliasMethod implements WalkerAliasTable {
	Random random = new Random();
	
	int k;
	double [] ps;
	int [] a;
	
	public GentleAliasMethod() {
		
	}
	public GentleAliasMethod(double [] pis, double normalizer) {
		generateAliasTable(pis,normalizer);
	}
	
	public GentleAliasMethod(double [] pis) {
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
		ps = new double[k];
		double [] b = new double[k];
		List<Integer> low = new ArrayList<>();
		List<Integer> high = new ArrayList<>();
		double k1 = 1.0/k;
		a = new int [k];
		for (int i = 0; i < k; i++) {
			a[i] = i;
			b[i] = (pi[i]/normalizer) - k1;
			if(b[i]<0.0) {
				low.add(i);
			} else {
				high.add(i);
			}
		}
		int steps = 0;
		while(steps<=k&&low.size()>0&&high.size()>0) {
			int l = low.remove(0);
			int h = high.get(0);
			double c=b[l];
			double d=b[h];
			b[l] = 0;
			b[h] = c + d;
			if(b[h]<=0) {high.remove(0);}
			if(b[h]<0) {low.add(h);}
			a[l] = h;
			ps[l] = 1.0 + ((double) k) * c;
		}
	}
	
	@Override
	public void reGenerateAliasTable(double[] pi, double normalizer) {
		generateAliasTable(pi,normalizer);
	}
	
	public void generateAliasTable(double [] pi) {
		generateAliasTable(pi,1.0);
	}
	
	@Override
	public int generateSample() {
		int i=random.nextInt(k); if (ThreadLocalRandom.current().nextDouble()>ps[i]) i=a[i]; return i;
	}

	@Override
	public int generateSample(double u) {
		int i=random.nextInt(k); if (u>ps[i]) i=a[i]; return i;
	}
	
	public static void main(String [] args) {
		GentleAliasMethod ga = new GentleAliasMethod();
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
