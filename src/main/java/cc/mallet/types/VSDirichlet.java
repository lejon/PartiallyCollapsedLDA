package cc.mallet.types;

import gnu.trove.TIntArrayList;
import cc.mallet.util.ParallelRandoms;
import cc.mallet.util.Randoms;
import static java.lang.Math.exp;

public class VSDirichlet implements VariableSelectionDirichlet {
	double beta = 0;
	Randoms random = new ParallelRandoms();
	double vsPrior;
	boolean useNonZero = true;

	public VSDirichlet(double beta, double vsPrior) {
		this(beta, vsPrior, true);
	}

	public VSDirichlet(double beta, double vsPrior, boolean useNonZero) {
		this.beta = beta;
		this.vsPrior = vsPrior;
		this.useNonZero = useNonZero;
	}

	/*
	 * Draw a Dirichlet distribution using variable selection 
	 * 
	 * @param counts Array int[numTypes] the type counts for the topic we are sampling 
	 * @param previousPhi Previous Dirichlet draw
	 * 
	 * @return VSResult containing phiRow, the Dirichlet draw, zeroIdxs a vector containing the types  
	 *  with Phi == 0, each index is less than numTypes
	 * 
	 */
	public VariableSelectionResult nextDistribution(int[] counts, double [] previousPhi) {
		int n_k = 0; // Finally, let n_k be the total number of tokens associated with the kth topic. 
		int zeroPhi = 0; // Number of 0 entries in this topic
		double [] phi = new double[previousPhi.length];
		TIntArrayList resultingZeroIdxs = new TIntArrayList();
		for (int i = 0; i < counts.length; i++) {
			n_k += counts[i];
			if(previousPhi[i]==0.0) zeroPhi++;
		}
		
		double sum_phi = 0;

		for (int i = 0; i < previousPhi.length; i++) {
			// If counts != 0, we draw Phi as usual
			if(counts[i]!=0) {
				phi[i] = random.nextGamma(counts[i]+beta, 1);
				if(useNonZero) resultingZeroIdxs.add(i);
			} else {
				//System.out.println("Count was zero...");
				double U = random.nextDouble();
				double indicatorProb = drawIndicatorProb(zeroPhi, n_k, beta);
				if(indicatorProb>1 && indicatorProb < 0) throw new IllegalStateException("Inconsistent Indicator probability");
				// Else, if we drew an indicator I == 0, and set Phi == 0
				if( U < indicatorProb )	{
					// It this slot was previously non-zero we now have another Zero 
					// phi for this topic, increase zeroPhi
					if(previousPhi[i]!=0.0) {
						zeroPhi++;
						if(zeroPhi>counts.length) throw new IllegalStateException("Cannot have more zeros than length of row");
					}
					phi[i] = 0.0;
					if(!useNonZero) resultingZeroIdxs.add(i);
				} else {
					// Else if we drew an indicator I == 1 then we sample Phi
					// If this previously was zero we have one less zero
					if(previousPhi[i]==0.0) {
						zeroPhi--;
						if(zeroPhi<0) throw new IllegalStateException("Cannot have less than 0 zeros");
					}
					phi[i] = random.nextGamma(counts[i]+beta, 1);
					if (phi[i] < 0) {
						throw new IllegalStateException("Drew negative gamma");
					} 
					if(useNonZero) resultingZeroIdxs.add(i);
				}
			}	
			sum_phi += phi[i];
		}

		// Normalize Dirichlet
		for (int i = 0; i < phi.length; i++) {
			phi[i] = phi[i] / sum_phi;
		}
		
		return new VSResult(phi, resultingZeroIdxs.toNativeArray());
	}
	
	protected double drawIndicatorProb(int zeroPhi, int n_k, double beta) {
		// Use log and subtract instead...
		double piReciprocal = 1.0-vsPrior;  
		double gammaBeta = Dirichlet.logGammaStirling( beta );
		double denom1 = Dirichlet.logGammaStirling( n_k + (zeroPhi * beta) + beta );
		double denom2 = Dirichlet.logGammaStirling( n_k + (zeroPhi * beta));
		double quot = piReciprocal / ( piReciprocal + (exp(gammaBeta - denom1 + denom2 ) * vsPrior));
		//System.out.println("Di:" + m_k + " Nz: " + zeroPhi  + " alpha: " + alpha + " ga: " + gammaAlpha + " denom: " + denom1 + " quot: " + quot);
		return quot;
	}
}
