package cc.mallet.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.mallet.configuration.Configuration;
import cc.mallet.configuration.LDACommandLineParser;

public class LoggingUtils {

	String baseDir = "";
	File currentLogDirf = null;
	List<Timing> timings = new ArrayList<Timing>();

	public LoggingUtils() {	}
	
	public LoggingUtils(String baseDir, File currentLogDirf, List<Timing> timings) {
		super();
		this.baseDir = baseDir;
		this.currentLogDirf = currentLogDirf;
		this.timings.addAll(timings);
	}

	public synchronized File getLogDir() {
		if(currentLogDirf==null) throw new IllegalArgumentException("You havent initialized the Logger before usage");
		return currentLogDirf;
	}

	public synchronized File checkAndCreateCurrentLogDir(String dirPrefix) {
		if(currentLogDirf==null) {
			currentLogDirf = LoggingUtils.createLogDir(dirPrefix);
			baseDir = currentLogDirf.getAbsolutePath();
			return currentLogDirf;
		} else {
			return currentLogDirf;
		}
	}
	
	public static synchronized File checkCreateAndCreateDir(String string) {
		File sublogdirf = new File(string);
		if(!sublogdirf.exists()) {
			sublogdirf.mkdirs();
			return sublogdirf;
		} else {
			return sublogdirf;
		}
	}

	public static synchronized PrintWriter checkCreateAndCreateLogPrinter(String dir,String filename) {
		File sublogdirf = new File(dir);
		if(!sublogdirf.exists()) {
			sublogdirf.mkdirs();
		} 
		File logfile = new File(sublogdirf.getAbsolutePath() + "/" + filename);
		try {
			return new PrintWriter(new FileWriter(logfile,true));
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e);
		}
	}

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

	public synchronized void logTiming(Timing timeing) {
		timings.add(timeing);
	}

	public synchronized void clearTimings() {
		timings.clear();
	}

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

	public static String getCommitHash() throws IOException {
		ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "--verify", "HEAD");
		/*Map<String, String> env = pb.environment();
		 env.put("VAR1", "myValue");
		 env.remove("OTHERVAR");
		 env.put("VAR2", env.get("VAR1") + "suffix");*/
		pb.directory(new File("."));
		Process proc = pb.start();

		BufferedReader stdInput = new BufferedReader(new 
				InputStreamReader(proc.getInputStream()));

		BufferedReader stdError = new BufferedReader(new 
				InputStreamReader(proc.getErrorStream()));

		String result = "";
		String s = null;
		while ((s = stdInput.readLine()) != null) {
			result += s;
		}

		String err = "";
		while ((s = stdError.readLine()) != null) {
			err+=s;
		}
		
		if(!err.equals("")) {
			System.out.println("Here is the standard error of the command (if any):\n");
			System.out.println(err);
		}
		return result;
	}
	
	public static String getCommitComment(String hash) throws IOException {
		ProcessBuilder pb = new ProcessBuilder("git", "log", "--max-count=1", "--pretty=oneline", hash);
		/*Map<String, String> env = pb.environment();
		 env.put("VAR1", "myValue");
		 env.remove("OTHERVAR");
		 env.put("VAR2", env.get("VAR1") + "suffix");*/
		pb.directory(new File("."));
		Process proc = pb.start();

		BufferedReader stdInput = new BufferedReader(new 
				InputStreamReader(proc.getInputStream()));

		BufferedReader stdError = new BufferedReader(new 
				InputStreamReader(proc.getErrorStream()));

		String result = "";
		String s = null;
		while ((s = stdInput.readLine()) != null) {
			result += s;
		}

		String err = "";
		while ((s = stdError.readLine()) != null) {
			err+=s;
		}
		
		if(!err.equals("")) {
			System.out.println("Here is the standard error of the command (if any):\n");
			System.out.println(err);
		}
		return result;
	}
	
	public static String getLatestCommit() throws IOException {
		ProcessBuilder pb = new ProcessBuilder("git", "log", "--max-count=1", "--pretty=oneline");
		/*Map<String, String> env = pb.environment();
		 env.put("VAR1", "myValue");
		 env.remove("OTHERVAR");
		 env.put("VAR2", env.get("VAR1") + "suffix");*/
		pb.directory(new File("."));
		Process proc = pb.start();

		BufferedReader stdInput = new BufferedReader(new 
				InputStreamReader(proc.getInputStream()));

		BufferedReader stdError = new BufferedReader(new 
				InputStreamReader(proc.getErrorStream()));

		String result = "";
		String s = null;
		while ((s = stdInput.readLine()) != null) {
			result += s;
		}

		String err = "";
		while ((s = stdError.readLine()) != null) {
			err+=s;
		}
		
		if(!err.equals("")) {
			System.out.println("Here is the standard error of the command (if any):\n");
			System.out.println(err);
		}
		return result;
	}

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
		
		String implVer = getManifestInfo("Implementation-Version", "PCPLDA");
		String buildVer = getManifestInfo("Implementation-Build","PCPLDA");
		
		if(implVer==null||buildVer==null) {
			out.println("Latest Commit Info:" + getLatestCommit());						
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
			FileUtils.copyFile(new File(config.whereAmI()), new File(logdir + "/" + confsrc.getName()));
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
			out.println("Config:Title: " + config.getStringProperty("title").trim());
			out.println("Config:Description: " + config.getStringProperty("description").trim());
		}
		//out.println(iterationType + iterations);
		out.println("Running class: " + runner);
		out.println("=== Metadata ===");
		
		String implVer = getManifestInfo("Implementation-Version", "PCPLDA");
		String buildVer = getManifestInfo("Implementation-Build","PCPLDA");
		
		if(implVer==null||buildVer==null) {
			out.println("Latest Commit Info:" + getLatestCommit());						
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
			FileUtils.copyFile(configFile, destFile);
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

	public static String getManifestInfo(String value, String jarfilePrefix) {
		@SuppressWarnings("rawtypes")
		Enumeration resEnum;
		try {
			resEnum = Thread.currentThread().getContextClassLoader().getResources(JarFile.MANIFEST_NAME);
			while (resEnum.hasMoreElements()) {
				try {
					URL url = (URL)resEnum.nextElement();
					Pattern p = Pattern.compile(jarfilePrefix + ".+\\.jar");
					Matcher m = p.matcher(url.toExternalForm());	
					if( m.find() ) {
						InputStream is = url.openStream();
						if (is != null) {
							Manifest manifest = new Manifest(is);
							Attributes mainAttribs = manifest.getMainAttributes();
							String version = mainAttribs.getValue(value);
							if(version != null) {
								return version;
							}
						}
					}
				}
				catch (Exception e) {
				}
			}
		} catch (IOException e1) {
		}
		return null;
	}

}
