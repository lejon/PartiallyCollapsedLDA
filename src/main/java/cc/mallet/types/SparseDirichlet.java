package cc.mallet.types;

public interface SparseDirichlet {
	
	public double[] nextDistribution();
	public double[] nextDistribution(int [] counts);
	public VSResult nextDistributionWithSparseness();
	public VSResult nextDistributionWithSparseness(int [] counts);
	public VSResult nextDistributionWithSparseness(double prior);
	public int[] updateDistributionWithSparseness(double [] target, double prior);

}
