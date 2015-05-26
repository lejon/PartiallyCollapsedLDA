package cc.mallet.topics;

import java.util.logging.Logger;

public class LogState {
	public double logLik;
	public int iteration;
	public String wordsPerTopic;
	public String loggingPath;
	public Logger logger;

	public LogState(double logLik, int iteration, String wordsPerTopic, String loggingPath, Logger logger,
			long absoluteTime, long zSamplingTokenUpdateTime, long phiSamplingTime, double density) {
		this( logLik,  iteration,  wordsPerTopic,  loggingPath,  logger);
	}

	public LogState(double logLik, int iteration, String wordsPerTopic, String loggingPath, Logger logger) {
		this.logLik = logLik;
		this.iteration = iteration;
		this.wordsPerTopic = wordsPerTopic;
		this.loggingPath = loggingPath;
		this.logger = logger;
	}
}