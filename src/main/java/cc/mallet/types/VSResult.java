package cc.mallet.types;

public class VSResult implements VariableSelectionResult {
	public double [] phiRow;
	public int [] zeroIdxs;
	public VSResult(double[] phiRow, int [] nonZeroIdxs) {
		this.phiRow = phiRow;
		this.zeroIdxs = nonZeroIdxs;
	}
	@Override
	public double[] getPhi() {
		return phiRow;
	}
	@Override
	public int[] getZeroIdxs() {
		int [] res = new int[zeroIdxs.length];
		for (int i = 0; i < zeroIdxs.length; i++) {
			res[i] = zeroIdxs[i];
		}
		return res;
	}
}