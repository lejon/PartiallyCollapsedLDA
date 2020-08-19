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

public class FileLoggingUtils {
	public static synchronized PrintWriter checkCreateAndCreateLogPrinter(String dir,String filename, boolean append) {
		File sublogdirf = new File(dir);
		if(!sublogdirf.exists()) {
			sublogdirf.mkdirs();
		} 
		File logfile = new File(sublogdirf.getAbsolutePath() + "/" + filename);
		try {
			return new PrintWriter(new FileWriter(logfile,append));
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e);
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
	
	public static synchronized File checkAndCreateDir(String string) {
		File sublogdirf = new File(string);
		if(!sublogdirf.exists()) {
			sublogdirf.mkdirs();
			return sublogdirf;
		} else {
			return sublogdirf;
		}
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

	public static String doLogging(Timer t,
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
