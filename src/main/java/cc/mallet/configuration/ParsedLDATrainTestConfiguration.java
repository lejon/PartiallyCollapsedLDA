package cc.mallet.configuration;

import org.apache.commons.configuration.ConfigurationException;


public class ParsedLDATrainTestConfiguration extends ParsedLDAConfiguration implements Configuration, LDATrainTestConfiguration {

	private static final long serialVersionUID = 1L;
	
	public ParsedLDATrainTestConfiguration(LDACommandLineParser cp) throws ConfigurationException {
		super(cp);
	}

	public ParsedLDATrainTestConfiguration(String path) throws ConfigurationException {
		super(path);
	}

	/* (non-Javadoc)
	 * @see cc.mallet.configuration.LDATrainTestConfiguration#getTextDatasetTestFilename()
	 */
	@Override
	public String getTextDatasetTestIdsFilename() {
		return getStringProperty("textdataset_testids");
	}
}
