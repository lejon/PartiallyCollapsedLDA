package cc.mallet.util;


public interface WalkerAliasTable {
	public int generateSample();
	public int generateSample(double u);
	public void initTable(double [] probabilities, double normalizer);
	public void reGenerateAliasTable(double[] pi, double normalizer);
	public void initTableNormalizedProbabilities(double [] probabilities);
}