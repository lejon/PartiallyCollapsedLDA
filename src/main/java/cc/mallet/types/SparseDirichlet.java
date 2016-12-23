package cc.mallet.types;

public interface SparseDirichlet {
	
	public double[] nextDistribution();
	public double[] nextDistribution(int [] counts);

}
