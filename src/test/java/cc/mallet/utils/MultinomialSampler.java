package cc.mallet.utils;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.math3.stat.inference.ChiSquareTest;

public class MultinomialSampler {
	
	double [] probs;
	
	public MultinomialSampler(double[] probs) {
		super();
		this.probs = probs;
	}
	
	public int generateSample() {
		return generateSample(probs);
	}
	
	public static int generateSample(double [] probs) {
		double U = ThreadLocalRandom.current().nextDouble();
		int theSample = -1;
		while (U > 0.0) {
			theSample++;
			U -= probs[theSample];
		} 
		return theSample;
	}

	public static int [] multinomialSampler(double[] probs, int noSamples) {
		int[] multinomialSamples = new int[noSamples];
		for (int i = 0; i < noSamples; i++) {
			multinomialSamples[i] = generateSample(probs);
		}
		return multinomialSamples;
	}
	
	public static void main(String [] args) {
		double [] pi = {2.0/15.0,7.0/15.0,6.0/15.0};
		MultinomialSampler ga = new MultinomialSampler(pi);	
		
		int noSamples = 20;
		int [] samples = new int[noSamples];
		for (int i = 0; i < samples.length; i++) {
			samples[i] = ga.generateSample();
		}
		long [] cnts = new long[pi.length];
		for (int i = 0; i < samples.length; i++) {
			cnts[samples[i]]++;
		}
		
		double [] obsFreq = new double[cnts.length];
		for (int i = 0; i < obsFreq.length; i++) {
			obsFreq[i] = cnts[i] / (double) noSamples;
		}

		ChiSquareTest cs = new ChiSquareTest();
		if(cs.chiSquareTest(pi, cnts, 0.01)) {
			System.out.println("Probs: " + Arrays.toString(pi) + " are NOT equal to " +  Arrays.toString(obsFreq));
		} else {
			System.out.println("Probs: " + Arrays.toString(pi) + " ARE equal to " +  Arrays.toString(obsFreq));
		}
	}
}
