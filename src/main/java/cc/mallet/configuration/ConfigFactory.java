package cc.mallet.configuration;

import org.apache.commons.configuration.ConfigurationException;

public class ConfigFactory {
	protected static Configuration mainConfig = null;
	public static Configuration getMainConfiguration(LDACommandLineParser cp) throws ConfigurationException {
		if( mainConfig == null ) {
			mainConfig = new ParsedLDAConfiguration(cp);
		}
		
		return mainConfig;
	}

	public static Configuration getMainRemoteConfiguration(LDACommandLineParser cp) throws ConfigurationException {
		if( mainConfig == null ) {
			mainConfig = new ParsedRemoteLDAConfiguration(cp);
		}
		
		return mainConfig;
	}
	
	public static Configuration getTrainTestConfiguration(LDACommandLineParser cp) throws ConfigurationException {
		if( mainConfig == null ) {
			mainConfig = new ParsedLDATrainTestConfiguration(cp);
		}
		
		return mainConfig;
	}

	public static Configuration getMainConfiguration() {
		return mainConfig;
	}
	
	public static Configuration setMainConfiguration(Configuration conf) {
		return mainConfig = conf;
	}

}
