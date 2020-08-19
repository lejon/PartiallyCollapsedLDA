package cc.mallet.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;

import cc.mallet.configuration.Configuration;
import cc.mallet.configuration.LDACommandLineParser;

public class LoggingUtils implements Serializable, LDALoggingUtils {

	private static final long serialVersionUID = 1L;
	String baseDir = ".";
	File currentLogDirf = null;
	transient List<Timing> timings = new ArrayList<Timing>();

	public LoggingUtils() {	}

	public LoggingUtils(String baseDir, File currentLogDirf, List<Timing> timings) {
		super();
		this.baseDir = baseDir;
		this.currentLogDirf = currentLogDirf;
		this.timings.addAll(timings);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((baseDir == null) ? 0 : baseDir.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LoggingUtils other = (LoggingUtils) obj;
		if (baseDir == null) {
			if (other.baseDir != null)
				return false;
		} else if (!baseDir.equals(other.baseDir))
			return false;
		return true;
	}

	@Override
	public synchronized File getLogDir() {
		if(currentLogDirf==null) throw new IllegalArgumentException("You havent initialized the Logger before usage");
		return currentLogDirf;
	}

	@Override
	public synchronized File checkAndCreateCurrentLogDir(String dirPrefix) {
		if(currentLogDirf==null) {
			currentLogDirf = FileLoggingUtils.createLogDir(dirPrefix);
			baseDir = currentLogDirf.getAbsolutePath();
			return currentLogDirf;
		} else {
			return currentLogDirf;
		}
	}

	public static synchronized File checkCreateAndCreateDir(String string) {
		return FileLoggingUtils.checkAndCreateDir(string);
	}

	@Override
	public synchronized File checkCreateAndSetSubLogDir(String string) {
		if(!string.trim().equalsIgnoreCase("") && !string.trim().startsWith("/")) {
			string = "/" + string;
		}
		File sublogdirf = new File(baseDir + string);
		currentLogDirf = sublogdirf;
		if(!sublogdirf.exists()) {
			sublogdirf.mkdirs();
			return sublogdirf;
		} else {
			return sublogdirf;
		}
	}

	@Override
	public synchronized void logTiming(Timing timeing) {
		timings.add(timeing);
	}

	@Override
	public synchronized void clearTimings() {
		timings.clear();
	}

	@Override
	public String dynamicLogRun(String pathPrefix, Timer t, 
			LDACommandLineParser cp,Configuration config, 
			List<Long> memusage, String runner, 
			String logfilename, String heading, String iterationType, int iterations,
			List<String> metadata) throws Exception {

		File logdirf = checkAndCreateCurrentLogDir(pathPrefix);
		String logdir = logdirf.getAbsolutePath();
		// Fix this tomorrow... ;)
		//logTimings("Timings",logdir);

		return FileLoggingUtils.doLogging(t, cp, config, memusage, runner,
				logfilename, heading, iterationType, iterations, metadata,
				logdir);
	}


	@Override
	public void logTimings(String logfilename,String logdir) throws FileNotFoundException {
		String separator = ",";
		if(timings.size()>0) {
			PrintWriter out = new PrintWriter(logdir + logfilename + ".txt");
			for(Timing timing : timings) {
				out.println("" + timing.getStart() + separator + timing.getStop() + separator + timing.getLabel());
			}
			out.close();
		}
	}

	public static String logRun(String pathPrefix, Timer t, 
			LDACommandLineParser cp,Configuration config, 
			List<Long> memusage, String runner, 
			String logfilename, String heading, String iterationType, int iterations,
			List<String> metadata) throws Exception {

		File logdirf = FileLoggingUtils.createLogDir(pathPrefix);
		String logdir = logdirf.getAbsolutePath();

		return FileLoggingUtils.doLogging(t, cp, config, memusage, runner,
				logfilename, heading, iterationType, iterations, metadata,
				logdir);
	}

	public static String doInitialLogging(LDACommandLineParser cp, Configuration config,
			String runner, String logfilename,  List<String> metadata, String logdir)
					throws FileNotFoundException, Exception, IOException {

		RuntimeMXBean RuntimemxBean = ManagementFactory.getRuntimeMXBean();
		List<String> arguments = RuntimemxBean.getInputArguments();

		if(!logfilename.startsWith("/") || !logdir.endsWith("/")) {
			logfilename = "/" + logfilename;
		}

		PrintWriter out = new PrintWriter(logdir + logfilename + ".txt");
		out.println("=== Setup ===");
		out.print("Java Args:");
		for( String arg : arguments ) {
			out.print(arg + " ");
		}
		out.println();
		if( cp != null) {
			out.println("CommandLine: " + cp);
			if(cp.getComment()!=null) out.println("Comment: " + cp.getComment());
		}
		if( config != null) { 
			out.println("Active subconfig: " +config.getActiveSubConfig());
			out.println("Config:Title: " + config.getStringProperty("title").trim());
			out.println("Config:Description: " + config.getStringProperty("description").trim());
		}
		//out.println(iterationType + iterations);
		out.println("Running class: " + runner);
		out.println("=== Metadata ===");

		String implVer = FileLoggingUtils.getManifestInfo("Implementation-Version", "PCPLDA");
		String buildVer = FileLoggingUtils.getManifestInfo("Implementation-Build","PCPLDA");

		if(implVer==null||buildVer==null) {
			out.println("Latest Commit Info:" + FileLoggingUtils.getLatestCommit());						
		} else {
			out.println("Implementation-Version:" + implVer);
			out.println("Implementation-Build:" + buildVer);			
		}

		if(metadata!=null) {
			for( String meta : metadata ) {
				out.println(meta);
			}
		}

		if( config != null) { 
			File confsrc = new File(config.whereAmI());
			MoreFileUtils.copyFile(new File(config.whereAmI()), new File(logdir + "/" + confsrc.getName()));
		}

		out.flush();
		out.close();

		return logdir + logfilename;
	}

	@Override
	public synchronized PrintWriter checkCreateAndCreateLogPrinter(String dir, String filename) {
		return FileLoggingUtils.checkCreateAndCreateLogPrinter(dir, filename, true);
	}

	@Override
	public PrintWriter getLogPrinter(String filename) {
		return FileLoggingUtils.checkCreateAndCreateLogPrinter(baseDir, filename, false);
	}

	@Override
	public PrintWriter getAppendingLogPrinter(String filename) {
		return FileLoggingUtils.checkCreateAndCreateLogPrinter(baseDir, filename, true);
	}

	@Override
	public PrintStream getLogPrintStream(String filename, boolean append) {
		try {
			return new PrintStream(new FileOutputStream(baseDir + File.pathSeparator + filename, true));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new IllegalAccessError("Could not create stream from: " + baseDir + File.pathSeparator + filename);
		}
	}

	@Override
	public boolean isFileLogger() {
		return true;
	}

}
