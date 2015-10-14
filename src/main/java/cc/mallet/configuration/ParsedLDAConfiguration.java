package cc.mallet.configuration;

import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;

import cc.mallet.util.LoggingUtils;

public class ParsedLDAConfiguration extends HierarchicalINIConfiguration implements Configuration, LDAConfiguration {

	private static final long serialVersionUID = 1L;

	String subConfName = null;
	LDACommandLineParser commandlineParser = null;
	String whereAmI;
	LoggingUtils logger;
	int noTopics = -1;

	/* (non-Javadoc)
	 * @see configuration.LDAConfiguration#getLoggingUtil()
	 */
	@Override
	public LoggingUtils getLoggingUtil() {
		if(logger==null) throw new IllegalArgumentException("You havent initialized the Logger before usage");
		return logger;
	}

	/* (non-Javadoc)
	 * @see configuration.LDAConfiguration#setLoggingUtil(utils.LoggingUtils)
	 */
	@Override
	public void setLoggingUtil(LoggingUtils logger) {
		this.logger = logger;
	}

	public ParsedLDAConfiguration() {

	}

	public ParsedLDAConfiguration(LDACommandLineParser cp) throws ConfigurationException {
		super(cp.getConfigFn());
		setDefaultListDelimiter(',');
		commandlineParser = cp;
		whereAmI = cp.getConfigFn();
	}

	public ParsedLDAConfiguration(String path) throws ConfigurationException {
		super(path);
		whereAmI = path;
		setDefaultListDelimiter(',');
	}

	/* (non-Javadoc)
	 * @see configuration.LDAConfiguration#activateSubconfig(java.lang.String)
	 */
	@Override
	public void activateSubconfig(String subConfName) {
		boolean foundIt = false;
		String [] configs = super.getStringArray("configs");
		for( String cfg : configs ) {
			cfg = cfg.trim();
			if( subConfName.equals(cfg) ) {
				foundIt = true;
			}
		}
		if( !foundIt ) {
			throw new IllegalArgumentException("No such configuration: " + subConfName);
		}
		this.subConfName = subConfName; 
	}

	/* (non-Javadoc)
	 * @see configuration.LDAConfiguration#forceActivateSubconfig(java.lang.String)
	 */
	@Override
	public void forceActivateSubconfig(String subConfName) {
		this.subConfName = subConfName; 
	}

	/* (non-Javadoc)
	 * @see configuration.LDAConfiguration#getActiveSubConfig()
	 */
	@Override
	public String getActiveSubConfig() {
		return subConfName;
	}

	private String translateKey(String key) {
		return (subConfName == null ? "" : subConfName + ".") + key ;
	}

	public boolean getBooleanProperty(String key) {
		return (getStringProperty(key)!=null) && 
				(getStringProperty(key).equalsIgnoreCase("true") 
						|| getStringProperty(key).equals("1"));
	}

	public String [] getStringArrayProperty(String key) {
		return trimStringArray(super.getStringArray(translateKey(key)));
	}

	public int [] getIntArrayProperty(String key, int [] defaultValues) {
		String [] ints = super.getStringArray(translateKey(key));
		if(ints==null || ints.length==0) {
			//throw new IllegalArgumentException("Could not find any int array for key:" + translateKey(key));
			return defaultValues;
		}
		int [] result = new int[ints.length];
		for (int i = 0; i < ints.length; i++) {
			result[i] = Integer.parseInt(ints[i].trim());
		}
		return result;
	}
	
	public double [] getDoubleArrayProperty(String key) {
		String [] ints = super.getStringArray(translateKey(key));
		if(ints==null || ints.length==0) { 
			throw new IllegalArgumentException("Could not find any double array for key:" 
					+ translateKey(key)); 
		}
		double [] result = new double[ints.length];
		for (int i = 0; i < ints.length; i++) {
			result[i] = Double.parseDouble(ints[i].trim());
		}
		return result;
	}


	protected String [] trimStringArray(String [] toTrim) {
		for (int i = 0; i < toTrim.length; i++) {
			toTrim[i] = toTrim[i].trim();
		}
		return toTrim;
	}

	@Override
	public String getStringProperty(String key) {
		if(commandlineParser.hasOption(key) && commandlineParser.getOption(key)!=null) {
			return commandlineParser.getOption(key);
		} else {
			// This hack lets us have "," in strings
			String strProp = "";
			Object prop = super.getProperty(translateKey(key));
			if(prop instanceof java.util.List) {
				@SuppressWarnings("unchecked")
				List<String> propParts = (List<String>) prop;
				for (String string : propParts) {
					strProp += string + ",";
				}
				strProp = strProp.substring(0, strProp.length()-1);
			} else {
				strProp = (String) prop;
			}
			return strProp;
		}
	}

	/* (non-Javadoc)
	 * @see configuration.LDAConfiguration#getConfProperty(java.lang.String)
	 */
	@Override
	public Object getConfProperty(String key) {
		return super.getProperty(translateKey(key));
	}
	
	/* (non-Javadoc)
	 * @see org.apache.commons.configuration.AbstractHierarchicalFileConfiguration#setProperty(java.lang.String, java.lang.Object)
	 */
	@Override
	public void setProperty(String key, Object value) {
		super.setProperty(translateKey(key), value);
	}

	/* (non-Javadoc)
	 * @see configuration.LDAConfiguration#getSubConfigs()
	 */
	@Override
	public String[] getSubConfigs() {
		return trimStringArray(super.getStringArray("configs"));
	}

	@Override
	public Integer getInteger(String key, Integer defaultValue) {
		if(commandlineParser.hasOption(key) && commandlineParser.getOption(key)!=null) {
			return Integer.parseInt(commandlineParser.getOption(key.trim()));
		} else {
			return super.getInteger(translateKey(key),defaultValue);
		}
	}

	@Override
	public Double getDouble(String key, Double defaultValue) {
		if(commandlineParser.hasOption(key) && commandlineParser.getOption(key)!=null) {
			return Double.parseDouble(commandlineParser.getOption(key.trim()));
		} else {
			return super.getDouble(translateKey(key),defaultValue);
		}
	}

	/* (non-Javadoc)
	 * @see configuration.LDAConfiguration#whereAmI()
	 */
	@Override
	public String whereAmI() {
		return whereAmI;
	}
	
	/* (non-Javadoc)
	 * @see configuration.LDAConfiguration#getDatasetFilename()
	 */
	@Override
	public String getDatasetFilename() {
		return getStringProperty("dataset");
	}

	/* (non-Javadoc)
	 * @see configuration.LDAConfiguration#getScheme()
	 */
	@Override
	public String getScheme() {
		return getStringProperty("scheme");
	}
	
	/* (non-Javadoc)
	 * @see configuration.LDAConfiguration#getNoTopics(int)
	 */
	@Override
	public Integer getNoTopics(int defaultValue) {
		return noTopics < 0 ? getInteger("topics",defaultValue) : noTopics;
	}
	
	/* (non-Javadoc)
	 * @see configuration.LDAConfiguration#getAlpha(double)
	 */
	@Override
	public Double getAlpha(double defaultValue) {
		return getDouble("alpha",defaultValue);
	}
	
	/* (non-Javadoc)
	 * @see configuration.LDAConfiguration#getBeta(double)
	 */
	@Override
	public Double getBeta(double defaultValue) {
		return getDouble("beta",defaultValue);
	}
	
	/* (non-Javadoc)
	 * @see configuration.LDAConfiguration#getNoIterations(int)
	 */
	@Override
	public Integer getNoIterations(int defaultValue) {
		return getInteger("iterations",defaultValue);
	}
	
	/* (non-Javadoc)
	 * @see configuration.LDAConfiguration#getNoBatches(int)
	 */
	@Override
	public Integer getNoBatches(int defaultValue) {
		return getInteger("batches",defaultValue);
	}
	
	/* (non-Javadoc)
	 * @see configuration.LDAConfiguration#getRareThreshold(int)
	 */
	@Override
	public Integer getRareThreshold(int defaultValue) {
		return getInteger("rare_threshold",defaultValue);
	}
	
	/* (non-Javadoc)
	 * @see configuration.LDAConfiguration#getTopicInterval(int)
	 */
	@Override
	public Integer getTopicInterval(int defaultValue) {
		return getInteger("topic_interval",defaultValue);
	}
	
	/* (non-Javadoc)
	 * @see configuration.LDAConfiguration#getStartDiagnostic(int)
	 */
	@Override
	public Integer getStartDiagnostic(int defaultValue) {
		return getInteger("start_diagnostic",defaultValue);
	}

	/**
	 * @param seedDefault
	 * @return the seed set in the config file or the LSB of the current time if set to -1
	 */
	@Override
	public int getSeed(int seedDefault) {
		int seed = getInteger("seed",seedDefault);
		if(seed==0) {seed=(int)System.currentTimeMillis();};
		return seed;
	}

	@Override
	public boolean getDebug() {
		String key = "debug";
		return getBooleanProperty(key);
	}

	@Override
	public boolean getPrintPhi() {
		String key = "print_phi";
		return getBooleanProperty(key);
	}

	@Override
	public boolean getMeasureTiming() {
		String key = "measure_timing";
		return getBooleanProperty(key);
	}

	@Override
	public void setNoTopics(int newValue) {
		noTopics = newValue;
	}

	@Override
	public int getResultSize(int resultsSizeDefault) {
		return getInteger("results_size",resultsSizeDefault);
	}

	@Override
	public String getDocumentBatchBuildingScheme(String batchBuildSchemeDefault) {
		String configProperty = getStringProperty("batch_building_scheme");
		return (configProperty == null) ? batchBuildSchemeDefault : configProperty;
	}

	@Override
	public String getTopicBatchBuildingScheme(String batchBuildSchemeDefault) {
		String configProperty = getStringProperty("topic_batch_building_scheme");
		return (configProperty == null) ? batchBuildSchemeDefault : configProperty;
	}

	@Override
	public double getDocPercentageSplitSize() {
		return getDouble("percentage_split_size_doc",1.0);
	}
	
	@Override
	public double getTopicPercentageSplitSize() {
		return getDouble("percentage_split_size_topic",1.0);
	}

	@Override
	public Integer getNoTopicBatches(int defaultValue) {
		return getInteger("topic_batches",defaultValue);
	}

	@Override
	public String getTopicIndexBuildingScheme(String topicIndexBuildSchemeDefault) {
		String configProperty = getStringProperty("topic_index_building_scheme");
		return (configProperty == null) ? topicIndexBuildSchemeDefault : configProperty;

	}

	@Override
	public int getInstabilityPeriod(int defaultValue) {
		return getInteger("instability_period",defaultValue);
	}

	@Override
	public double[] getFixedSplitSizeDoc() {
		return getDoubleArrayProperty("fixed_split_size_doc");
	}

	@Override
	public int getFullPhiPeriod(int defaultValue) {
		return getInteger("full_phi_period",defaultValue);	}

	@Override
	public String[] getSubTopicIndexBuilders(int i) {
		return getStringArrayProperty("sub_topic_index_builders");
	}

	@Override
	public double topTokensToSample(double defaultValue) {
		return getDouble("percent_top_tokens",defaultValue);
	}

	@Override
	public int[] getPrintNDocsInterval() {
		int [] defaultVal = {-1};
		return getIntArrayProperty("print_ndocs_interval", defaultVal);
	}

	@Override
	public int getPrintNDocs() {
		return getInteger("print_ndocs_cnt",0);
	}

	@Override
	public int[] getPrintNTopWordsInterval() {
		int [] defaultVal = {-1};
		return getIntArrayProperty("print_ntopwords_interval", defaultVal);
	}

	@Override
	public int getPrintNTopWords() {
		return getInteger("print_ntopwords_cnt",0);
	}

	@Override
	public int getProportionalTopicIndexBuilderSkipStep() {
		return getInteger("proportional_ib_skip_step",1);
	}

	@Override
	public boolean logTypeTopicDensity(boolean logTypeTopicDensityDefault) {
		String key = "log_type_topic_density";
		return getBooleanProperty(key);
	}

	@Override
	public boolean logDocumentDensity(boolean logDocumentDensityDefault) {
		String key = "log_document_density";
		return getBooleanProperty(key);
	}

	@Override
	public String getExperimentOutputDirectory(String defaultDir) {
		String dir = getStringProperty("experiment_out_dir");
		if(dir != null && dir.endsWith("/")) dir = dir.substring(0,dir.length()-1);
		return (dir == null) ? defaultDir : dir;
	}

	@Override
	public double getVariableSelectionPrior(double vsPriorDefault) {
		return getDouble("variable_selection_prior",vsPriorDefault);
	}

	@Override
	public boolean logPhiDensity(String logPhiDensityDefault) {
		String key = "log_phi_density";
		return getBooleanProperty(key);
	}

	@Override
	public String getTopicPriorFilename() {
		return getStringProperty("topic_prior_filename");
	}
	
	@Override
	public String getStoplistFilename(String defaultStoplist) {
		String stoplistFn = getStringProperty("stoplist");
		if(stoplistFn==null || stoplistFn.length()==0) {
			stoplistFn = defaultStoplist;
		}
		return stoplistFn;
	}

	@Override
	public boolean keepNumbers() {
		String key = "keep_numbers";
		return getBooleanProperty(key);
	}

	@Override
	public boolean saveDocumentTopicMeans() {
		String key = "save_doc_topic_means";
		return getBooleanProperty(key);
	}

	@Override
	public String getDocumentTopicMeansOutputFilename() {
		return getStringProperty("doc_topic_mean_filename");
	}
}
