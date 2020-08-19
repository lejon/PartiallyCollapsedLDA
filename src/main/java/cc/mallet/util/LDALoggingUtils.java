package cc.mallet.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;

import cc.mallet.configuration.Configuration;
import cc.mallet.configuration.LDACommandLineParser;

public interface LDALoggingUtils {

	int hashCode();

	boolean equals(Object obj);

	File getLogDir();

	File checkAndCreateCurrentLogDir(String dirPrefix);

	File checkCreateAndSetSubLogDir(String string);

	void logTiming(Timing timeing);

	void clearTimings();

	String dynamicLogRun(String pathPrefix, Timer t, LDACommandLineParser cp, Configuration config, List<Long> memusage,
			String runner, String logfilename, String heading, String iterationType, int iterations,
			List<String> metadata) throws Exception;

	void logTimings(String logfilename, String logdir) throws FileNotFoundException;
	
	PrintWriter checkCreateAndCreateLogPrinter(String dir,String filename);
	PrintWriter getLogPrinter(String filename);
	PrintWriter getAppendingLogPrinter(String filename);
	PrintStream getLogPrintStream(String filename,boolean append);
	boolean isFileLogger();
}