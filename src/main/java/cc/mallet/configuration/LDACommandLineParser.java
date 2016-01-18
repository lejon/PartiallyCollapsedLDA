package cc.mallet.configuration;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

public class LDACommandLineParser {
	
	protected String comment  = null;
	protected String configFn = null;
	protected String fullPath = null;
	protected Options options = new Options();
	protected CommandLineParser parser = new PosixParser(); 
	protected CommandLine parsedCommandLine;

	public LDACommandLineParser() throws ParseException {
		addOptions();
	}
	
	public LDACommandLineParser(String [] args) throws ParseException {
		addOptions();

		parsedCommandLine = parseCommandLine(args);

		if( parsedCommandLine.hasOption( "cm" ) ) {
			comment = parsedCommandLine.getOptionValue( "comment" );
		}
		if( parsedCommandLine.hasOption( "cf" ) ) {
			configFn = parsedCommandLine.getOptionValue( "run_cfg" );
		}
	}
	
	public Options getOptions() {
		return options;
	}

	protected CommandLine parseCommandLine(String [] args) throws ParseException {
		return parser.parse( options, args );
	}

	@SuppressWarnings("static-access")
	protected void addOptions() {
		options.addOption( "dbg", "debug", true, "use debugging " );
		options.addOption( "cm", "comment", true, "a comment ot be added to the logfile " );
		options.addOption( "ds", "dataset", true, "filename of dataset file" );
		options.addOption( "ts", "topics", true, "number of topics" );
		options.addOption( "a", "alpha", true, "uniform alpha prior" );
		options.addOption( "b", "beta", true, "uniform beta prior" );
		options.addOption( "i", "iterations", true, "number of sample iterations" );
		options.addOption( "batch", "batches", true, "the number of batches to split the data in" );
		options.addOption( "r", "rare_threshold", true, "the number of batches to split the data in" );
		options.addOption( "ti", "topic_interval", true, "topic interval" );
		options.addOption( "sd", "start_diagnostic", true, "start diagnostic" );
		options.addOption( "sch", "scheme", true, "sampling scheme " );
		options.addOption( "cf", "run_cfg", true, "full path to the RunConfiguration file " );
		options.addOption( OptionBuilder.withLongOpt( "block-size" )
				.withDescription( "use SIZE-byte blocks" )
				.hasArg()
				.withArgName("SIZE")
				.create() );
	}
	
	public boolean hasOption(String key) {
		return options.hasOption(key);
	}
	
	public String getOption(String key) {
		return parsedCommandLine.getOptionValue(key);
	}

	public String getConfigFn() {
		return configFn;
	}

	public void setConfigFn(String configFn) {
		this.configFn = configFn;
	}

	public String getFullPath() {
		return fullPath;
	}

	public void setFullPath(String fullPath) {
		this.fullPath = fullPath;
	}
	
	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}
	
	public String toString() {
		return "--comment=" + getComment() + " --run_config=" + getConfigFn(); 
	}
}
