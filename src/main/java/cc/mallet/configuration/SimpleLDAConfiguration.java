package cc.mallet.configuration;

import org.apache.commons.lang.NotImplementedException;

import cc.mallet.util.LoggingUtils;

public class SimpleLDAConfiguration implements LDAConfiguration {
	LoggingUtils logUtil;
	private String scheme;
	private Integer noTopics;
	private Double alpha;
	private Double beta;
	private Integer noIters;
	private Integer noBatches;
	private Integer noTopicBatches;
	private Integer rareThreshold;
	private Integer topicInterval;
	private Integer startDiagnostic;
	private Integer seed = 0;
	private boolean debug = false;
	private String dataset_fn;
	private String building_scheme;
	private double percentage_split_size_doc;
	private double percentage_split_size_topic;
	private int resultSetSize = 1;
	private Integer fullPhiPeriod;
	private Double topTokensToSample;

	// How to build which words in the topics to sample
	private String topic_building_scheme;
	// How to build the topic batches
	private String topic_batch_building_scheme;
	int instability_period;
	private double[] fixed_split_size_doc;
	private int skipStep = 1;

	public SimpleLDAConfiguration(LoggingUtils logUtil, String scheme,
			Integer noTopics, Double alpha, Double beta, Integer noIters,
			Integer noBatches, Integer rareThreshold, Integer topicInterval,
			Integer startDiagnostic, int seed, String datasetFn) {
		super();
		this.logUtil = logUtil;
		this.scheme = scheme;
		this.noTopics = noTopics;
		this.alpha = alpha;
		this.beta = beta;
		this.noIters = noIters;
		this.noBatches = noBatches;
		this.rareThreshold = rareThreshold;
		this.topicInterval = topicInterval;
		this.startDiagnostic = startDiagnostic;
		this.seed = seed;
		this.dataset_fn = datasetFn;
	}

	public SimpleLDAConfiguration() {
	}

	@Override
	public LoggingUtils getLoggingUtil() {
		return logUtil;
	}

	@Override
	public void setLoggingUtil(LoggingUtils logger) {
		logUtil = logger;
	}

	@Override
	public void activateSubconfig(String subConfName) {

	}

	@Override
	public void forceActivateSubconfig(String subConfName) {

	}

	@Override
	public String getActiveSubConfig() {
		return "default";
	}

	@Override
	public String[] getSubConfigs() {
		return new String[] {};
	}

	@Override
	public String whereAmI() {
		return "<SimpleLDAConfiguration>";
	}

	@Override
	public String getDatasetFilename() {
		return dataset_fn;
	}

	@Override
	public String getScheme() {
		return scheme;
	}

	@Override
	public Integer getNoTopics(int defaultValue) {
		return noTopics;
	}

	@Override
	public Double getAlpha(double defaultValue) {
		return alpha;
	}

	@Override
	public Double getBeta(double defaultValue) {
		return beta;
	}

	@Override
	public Integer getNoIterations(int defaultValue) {
		return noIters;
	}

	@Override
	public Integer getNoBatches(int defaultValue) {
		return noBatches;
	}

	@Override
	public Integer getRareThreshold(int defaultValue) {
		return rareThreshold;
	}

	@Override
	public Integer getTopicInterval(int defaultValue) {
		return topicInterval;
	}

	@Override
	public Integer getStartDiagnostic(int defaultValue) {
		return startDiagnostic;
	}
	
	public void setLogUtil(LoggingUtils logUtil) {
		this.logUtil = logUtil;
	}

	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	public void setNoTopics(Integer noTopics) {
		this.noTopics = noTopics;
	}

	public void setAlpha(Double alpha) {
		this.alpha = alpha;
	}

	public void setBeta(Double beta) {
		this.beta = beta;
	}

	public void setNoIters(Integer noIters) {
		this.noIters = noIters;
	}

	public void setNoBatches(Integer noBatches) {
		this.noBatches = noBatches;
	}

	public void setNoTopicBatches(Integer noTopicBatches) {
		this.noTopicBatches = noTopicBatches;
	}

	public void setRareThreshold(Integer rareThreshold) {
		this.rareThreshold = rareThreshold;
	}

	public void setTopicInterval(Integer topicInterval) {
		this.topicInterval = topicInterval;
	}

	public void setStartDiagnostic(Integer startDiagnostic) {
		this.startDiagnostic = startDiagnostic;
	}
	
	@Override
	public int getSeed(int seedDefault) {
		return this.seed;
	}

	@Override
	public boolean getDebug() {
		return debug;
	}

	@Override
	public boolean getPrintPhi() {
		return false;
	}

	@Override
	public boolean getMeasureTiming() {
		return false;
	}

	@Override
	public void setNoTopics(int newValue) {
		noTopics = newValue;
	}

	@Override
	public int[] getIntArrayProperty(String key, int [] defaultValues) {
		switch(key) {
		case "dn_diagnostic_interval": {
			return new int [0];
		}
		case "diagnostic_interval": {
			return new int [0];
		}
		default : {
			return defaultValues;
		}
		}
	}

	@Override
	public int getResultSize(int resultsSizeDefault) {
		return resultSetSize;
	}

	public void setResultSize(int resultsSize) {
		this.resultSetSize = resultsSize;
	}
	
	public void setBatchBuildingScheme(String string) {
		this.building_scheme = string;
	}

	@Override
	public String getDocumentBatchBuildingScheme(String batchBuildSchemeDefault) {
		return building_scheme == null ? batchBuildSchemeDefault : building_scheme;
	}

	@Override
	public String getTopicBatchBuildingScheme(String batchBuildSchemeDefault) {
		return topic_batch_building_scheme == null ? batchBuildSchemeDefault : null;
	}

	@Override
	public double getDocPercentageSplitSize() {
		return percentage_split_size_doc;
	}
	
	public void setDocPercentageSplitSize(double splitSize) {
		this.percentage_split_size_doc = splitSize;
	}

	@Override
	public double getTopicPercentageSplitSize() {
		return percentage_split_size_topic;
	}
	
	public void setTopicPercentageSplitSize(double splitSize) {
		this.percentage_split_size_topic = splitSize;
	}

	@Override
	public Integer getNoTopicBatches(int defaultValue) {
		return noTopicBatches == null ? defaultValue : noTopicBatches;
	}
	
	public void setTopicBuildingScheme(String topic_building_scheme) {
		this.topic_building_scheme = topic_building_scheme;
	}

	@Override
	public String getTopicIndexBuildingScheme(String topicIndexBuildSchemeDefault) {
		return topic_building_scheme == null ? topicIndexBuildSchemeDefault : topic_building_scheme;
	}

	public void setInstabilityPeriod(int instability_period) {
		this.instability_period = instability_period;
	}

	@Override
	public int getInstabilityPeriod(int defaultValue) {
		return instability_period;
	}

	public void setFixedSplitSizeDoc(double[] fixed_split_size_doc) {
		this.fixed_split_size_doc = fixed_split_size_doc;
	}

	@Override
	public double[] getFixedSplitSizeDoc() {
		return fixed_split_size_doc;
	}

	public void setFullPhiPeriod(Integer value) {
		fullPhiPeriod = value;
	}

	@Override
	public int getFullPhiPeriod(int defaultValue) {
		return fullPhiPeriod == null ? defaultValue : fullPhiPeriod;
	}

	@Override
	public String[] getSubTopicIndexBuilders(int i) {
		return null;
	}
	
	@Override
	public double topTokensToSample(double defaultValue) {
		return topTokensToSample == null ? defaultValue : null;
	}

	@Override
	public void setProperty(String key, Object value) {
		throw new NotImplementedException("This method is not implemented for SimpleLDAConfiguration, use type safe deticated methods instead (you might need to implement the one you need! :))");
	}

	@Override
	public int[] getPrintNDocsInterval() {
		return new int [0];
	}

	@Override
	public int getPrintNDocs() {
		return 0;
	}

	@Override
	public int[] getPrintNTopWordsInterval() {
		return new int [0];
	}

	@Override
	public int getPrintNTopWords() {
		return 0;
	}

	public void setProportionalTopicIndexBuilderSkipStep(int stepSize) {
		skipStep = stepSize;
	}

	@Override
	public int getProportionalTopicIndexBuilderSkipStep() {
		return skipStep;
	}

	@Override
	public boolean logTypeTopicDensity(boolean logTypeTopicDensityDefault) {
		return false;
	}

	@Override
	public boolean logDocumentDensity(boolean logDocumentDensityDefault) {
		return false;
	}

	@Override
	public String getExperimentOutputDirectory(String defaultDir) {
		return null;
	}
	
	@Override
	public double getVariableSelectionPrior(double vsPriorDefault) {
		return vsPriorDefault;
	}

	@Override
	public boolean logPhiDensity(String logPhiDensityDefault) {
		return false;
	}
}
