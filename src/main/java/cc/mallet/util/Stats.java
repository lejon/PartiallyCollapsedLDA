package cc.mallet.util;


public class Stats {
	public int iteration;
	public String loggingPath;
	public long absoluteTime;
	public long zSamplingTokenUpdateTime;
	public long phiSamplingTime;
	public double density;
	public double docDensity;
	public double phiDensity;
	public long [] zTimings;
	public long [] countTimings;
	public Double heldOutLL;

	public Stats(int iteration, String loggingPath,	long absoluteTime,	
			long zSamplingTokenUpdateTime, long phiSamplingTime, double density,
			double docDensity, long [] zTimings, long [] countTimings, double phiDensity) {
		this.iteration = iteration;
		this.loggingPath = loggingPath;
		this.absoluteTime = absoluteTime;
		this.zSamplingTokenUpdateTime = zSamplingTokenUpdateTime;
		this.phiSamplingTime = phiSamplingTime;
		this.density = density;
		this.docDensity = docDensity;
		this.phiDensity = phiDensity;
		this.zTimings = zTimings;
		this.countTimings = countTimings;
	}

	public Stats(int iteration, String loggingPath, long elapsedMillis, long zSamplingTokenUpdateTime,
			long phiSamplingTime, double density, double docDensity, long[] zTimings, long[] countTimings,
			double phiDensity, Double heldOutLL) {
		this(iteration, loggingPath,elapsedMillis,	
				zSamplingTokenUpdateTime,  phiSamplingTime, density,
				docDensity, zTimings, countTimings,  phiDensity);
		this.heldOutLL = heldOutLL;
	}

}
