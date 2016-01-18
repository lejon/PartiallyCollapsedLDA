package cc.mallet.configuration;

import org.apache.commons.cli.ParseException;

public class LDATrainTestCommandLineParser extends LDACommandLineParser {
	
	public LDATrainTestCommandLineParser(String [] args) throws ParseException {
		super.addOptions();
		addOptions();
		parsedCommandLine = parseCommandLine(args);

		if( parsedCommandLine.hasOption( "cm" ) ) {
			comment = parsedCommandLine.getOptionValue( "comment" );
		}
		if( parsedCommandLine.hasOption( "cf" ) ) {
			configFn = parsedCommandLine.getOptionValue( "run_cfg" );
		}
	}
	
	protected void addOptions() {
		options.addOption( "ts", "testset", true, "a filename to a file containing which ids in the dataset to act as test ids " );
	}

}
