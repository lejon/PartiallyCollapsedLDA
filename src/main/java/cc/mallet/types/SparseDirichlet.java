package cc.mallet.types;

public interface SparseDirichlet {
	
	public double[] nextDistribution();
	public double[] nextDistribution(int [] counts);
	public VSResult nextDistributionWithSparseness();
	public VSResult nextDistributionWithSparseness(int [] counts);
	public VSResult nextDistributionWithSparseness(double [] previousDistribution, double prior);
	public int[] updateDistributionWithSparseness(double[] previousDistribution, double prior);

}
