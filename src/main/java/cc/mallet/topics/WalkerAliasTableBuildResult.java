package cc.mallet.topics;

import cc.mallet.util.WalkerAliasTable;

public class WalkerAliasTableBuildResult {
	public int type;
	public WalkerAliasTable table;
	public double typeNorm;
	
	public WalkerAliasTableBuildResult(int type, WalkerAliasTable table, double typeNorm) {
		this.type = type;
		this.table = table;
		this.typeNorm = typeNorm;
	}
}