package cc.mallet.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import cc.mallet.configuration.Configuration;
import cc.mallet.configuration.LDACommandLineParser;

public class LDANullLogger implements LDALoggingUtils, Serializable {

	private static final long serialVersionUID = 1L;
	private File currentLogDirf;
	private String baseDir;

	public LDANullLogger() {	}

	public LDANullLogger(String baseDir) {
		this.baseDir = baseDir;
	}

	public LDANullLogger(String baseDir, File currentLogDirf, List<Timing> timings) {
		this.baseDir = baseDir;
		this.currentLogDirf = currentLogDirf;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		return true;
	}

	@Override
	public synchronized File getLogDir() {
		if(currentLogDirf==null) {
			try {
				currentLogDirf = File.createTempFile("LDA_", "_NUllLogger");
				return currentLogDirf;
			} catch (IOException e) {
				System.err.println("Could not create temp file: " + e);
			}
			return null;
		}
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

	static File createLogDir(String pathPrefix) {
		String dateFormatted = getDateStamp();
		String logdir_base = pathPrefix + "/Run" + dateFormatted;
		String logdir = logdir_base;

		File logdirf = new File(logdir);
		int trycnt = 1;
		while( logdirf.exists() ) {
			logdir = logdir_base + "_" + trycnt++;
			logdirf = new File(logdir);
		}
		logdirf.mkdirs();
		return logdirf;
	}

	public static String getDateStamp() {
		Date logdate = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd--HH_mm_ss");
		return formatter.format(logdate);
	}

	public static String makeRunSuiteDir(String pathPrefix) {
		String dateFormatted = getDateStamp();
		String suiteNameBase = "RunSuite" + dateFormatted;
		String logdir = pathPrefix + "/" + suiteNameBase;
		File logdirf = new File(logdir);
		int trycnt = 1;
		while( logdirf.exists() ) {
			logdir = pathPrefix + "/" + suiteNameBase + "_" + trycnt++;
			logdirf = new File(logdir);
		}
		logdirf.mkdirs();

		return suiteNameBase;
	}

	public static String makeRunSuiteDir(String pathPrefix, String suitePrefix) {
		String dateFormatted = getDateStamp();
		String suiteNameBase = suitePrefix + dateFormatted;
		String logdir = pathPrefix + "/" + suiteNameBase;
		File logdirf = new File(logdir);
		int trycnt = 1;
		while( logdirf.exists() ) {
			logdir = pathPrefix + "/" + suiteNameBase + "_" + trycnt++;
			logdirf = new File(logdir);
		}
		logdirf.mkdirs();

		return suiteNameBase;
	}

	@Override
	public void logTiming(Timing timeing) {

	}

	@Override
	public void clearTimings() {
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

		return doLogging(t, cp, config, memusage, runner,
				logfilename, heading, iterationType, iterations, metadata,
				logdir);
	}

	@Override
	public void logTimings(String logfilename,String logdir) throws FileNotFoundException {}

	public static String logRun(String pathPrefix, Timer t, 
			LDACommandLineParser cp,Configuration config, 
			List<Long> memusage, String runner, 
			String logfilename, String heading, String iterationType, int iterations,
			List<String> metadata) throws Exception {

		File logdirf = createLogDir(pathPrefix);
		String logdir = logdirf.getAbsolutePath();

		return doLogging(t, cp, config, memusage, runner,
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

	private static String doLogging(Timer t,
			LDACommandLineParser cp, Configuration config,
			List<Long> memusage, String runner,
			String logfilename, String heading, String iterationType,
			int iterations, List<String> metadata, String logdir)
					throws FileNotFoundException, Exception, IOException {
		RuntimeMXBean RuntimemxBean = ManagementFactory.getRuntimeMXBean();
		List<String> arguments = RuntimemxBean.getInputArguments();

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
			String title = config.getStringProperty("title");
			if(title==null) {
				title = "<no_title_set>";
			}
			title=title.trim();
			out.println("Config:Title: " + title);
			String desc = config.getStringProperty("description");
			if(desc==null) {
				desc = "";
			}
			desc=desc.trim();
			out.println("Config:Description: " + desc);
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
			File configFile = new File(config.whereAmI());
			File confsrc = configFile;
			File destFile = new File(logdir + "/" + confsrc.getName());
			MoreFileUtils.copyFile(configFile, destFile);
		}

		if(t!=null) {
			t.stop();
			out.println("Start: " + t.getStartdate());
			out.println("Stop : " + t.getEnddate());
		}
		out.print("Memory Usage:");
		if( memusage != null) {
			for( Long mem : memusage ) {
				out.print(mem/1000000000 + "Gb, ");
			}
		}
		out.flush();
		out.close();

		return logdir + logfilename;
	}

	@Override
	public PrintWriter checkCreateAndCreateLogPrinter(String dir, String filename) {
		return getAppendingLogPrinter(dir + File.pathSeparator + filename);
	}

	@Override
	public PrintWriter getAppendingLogPrinter(String filename) {
		return getLogPrinter(filename);
	}

	@Override
	public PrintStream getLogPrintStream(String filename, boolean append) {
		return new PrintStream(new NullOutputStream());
	}

	@Override
	public PrintWriter getLogPrinter(String filename) {
		try {
			return new NullPrintWriter();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean isFileLogger() {
		return false;
	}
}
