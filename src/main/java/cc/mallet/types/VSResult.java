package cc.mallet.types;

public class VSResult implements VariableSelectionResult {
	public double [] phiRow;
	public int [] nonZeroIdxs;
	public VSResult(double[] phiRow, int [] nonZeroIdxs) {
		this.phiRow = phiRow;
		this.nonZeroIdxs = nonZeroIdxs;
	}
	@Override
	public double[] getPhi() {
		return phiRow;
	}
	@Override
	public int[] getNonZeroIdxs() {
		int [] res = new int[nonZeroIdxs.length];
		for (int i = 0; i < nonZeroIdxs.length; i++) {
			res[i] = nonZeroIdxs[i];
		}
		return res;
	}
}