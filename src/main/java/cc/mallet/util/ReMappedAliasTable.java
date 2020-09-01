package cc.mallet.util;

import java.io.Serializable;
import java.util.concurrent.ThreadLocalRandom;

public class ReMappedAliasTable extends OptimizedGentleAliasMethod implements WalkerAliasTable, Serializable {
	private static final long serialVersionUID = 1L;
	
	int [] mapping;
	
	public ReMappedAliasTable(int [] mapping) {
		this.mapping = mapping;
	}
	
	public ReMappedAliasTable(double [] pis, double normalizer, int [] mapping) {
		this.mapping = mapping;
		generateAliasTable(pis,normalizer);
	}
	
	public ReMappedAliasTable(double [] pis, int [] mapping) {
		this.mapping = mapping;
		generateAliasTable(pis);
	}
	
	@Override
	public int generateSample() {
		double u = ThreadLocalRandom.current().nextDouble();
		return mapping[generateSample(u)];
	}	
}
