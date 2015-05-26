package cc.mallet.types;

public interface VariableSelectionDirichlet {
	public VariableSelectionResult nextDistribution(int[] counts, double [] previousPhi);
}
