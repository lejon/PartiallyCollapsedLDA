package cc.mallet.configuration;

import java.io.Serializable;
import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cc.mallet.util.LDALoggingUtils;
import cc.mallet.util.LDANullLogger;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
//@JsonIgnoreProperties(ignoreUnknown = true)
public class SimpleLDAConfiguration implements LDAConfiguration, Serializable {
	private static final long serialVersionUID = 1L;
	@JsonIgnore LDALoggingUtils logUtil = new LDANullLogger();
	private String scheme;
	private Integer noTopics       = LDAConfiguration.NO_TOPICS_DEFAULT;
	private Double alpha           = LDAConfiguration.ALPHA_DEFAULT;
	private Double beta            = LDAConfiguration.BETA_DEFAULT;
	private Double lambda          = LDAConfiguration.LAMBDA_DEFAULT;
	private Integer noIters        = LDAConfiguration.NO_ITER_DEFAULT;
	private Integer noBatches      = LDAConfiguration.NO_BATCHES_DEFAULT;
	private Integer noTopicBatches = LDAConfiguration.NO_TOPIC_BATCHES_DEFAULT;
	private Integer rareThreshold  = LDAConfiguration.RARE_WORD_THRESHOLD;
	private Integer tfIdfThreshold = LDAConfiguration.TF_IDF_VOCAB_SIZE_DEFAULT;
	private Integer topicInterval  = LDAConfiguration.TOPIC_INTER_DEFAULT;
	private Integer startDiagnostic = -1;
	private Integer seed = 0;
	private boolean debug = false;
	private String originalDatasetFilename;
	private String datasetFilename;
	private String testDatasetFilename;
	private String buildingScheme;
	private double percentageSplitSizeDoc;
	private double percentageSplitSizeTopic;
	private int resultSetSize = 1;
	private Integer fullPhiPeriod;
	private Double topTokensToSample;
	private String topicPriorFilename;
	// How to build which words in the topics to sample
	private String topic_building_scheme;
	// How to build the topic batches
	private String topic_batch_building_scheme;
	int instability_period;
	private double[] fixed_split_size_doc;
	private int skipStep = 1;
	private boolean savePhi;
	private int phiBurnIn = LDAConfiguration.PHI_BURN_IN_DEFAULT;
	private String docLengthsFilename;
	private boolean saveDocLengths;
	private String termFrequencyFilename;
	private boolean saveTermFrequencies;
	private String vocabularyFilename;
	private boolean saveVocabulary;
	private String corpusFn;
	private boolean saveCorpus;
	private boolean printPhi;
	private boolean measureTiming;
	private boolean logTokensPerTopic = LDAConfiguration.LOG_TOKENS_PER_TOPIC;
	private boolean logTypeTopicDensity;
	private boolean logDocumentDensity;
	private String experimentOutputDirectory;
	private boolean logPhiDensity;
	private boolean keepNumbers;
	private boolean saveDocumentTopicMeans;
	private boolean saveDocumentTopicTheta;
	private String documentTopicMeansOutputFilename;
	private String documentTopicThetaOutputFilename;
	private boolean saveDocumentTopicDiagnostics;
	private String documentTopicDiagnosticsOutputFilename;
	private String phiMeansOutputFilename;
	private boolean keepConnectingPunctuation;
	private String stoplistFilename;
	private int nrTopWords = LDAConfiguration.NO_TOP_WORDS_DEFAULT;
	private int maxDocBufferSize = LDAConfiguration.MAX_DOC_BUFFFER_SIZE_DEFAULT;
	private int phiMeanThinDefault = LDAConfiguration.PHI_THIN_DEFAULT;
	private String dirichletSamplerBuilderClassName = LDAConfiguration.SPARSE_DIRICHLET_SAMPLER_BULDER_DEFAULT;
	private int aliasPoissonThreshold = LDAConfiguration.ALIAS_POISSON_DEFAULT_THRESHOLD;
	private String fileRegex;
	private Integer hyperparamOptimInterval  = LDAConfiguration.HYPERPARAM_OPTIM_INTERVAL_DEFAULT;
	private boolean symmetricAlpha = LDAConfiguration.SYMMETRIC_ALPHA_DEFAULT;
	private double hdpGgamma = LDAConfiguration.HDP_GAMMA_DEFAULT;
	private int hdpNrStartTopics = LDAConfiguration.HDP_START_TOPICS_DEFAULT;
	private int documentSamplerSplitLimit = LDAConfiguration.DOCUMENT_SAMPLER_SPLIT_LIMIT_DEFAULT;
	private double hdpKPercentile = LDAConfiguration.HDP_K_PERCENTILE;
	private boolean logTopicIndicators;
	private String samplerClass = LDAConfiguration.MODEL_DEFAULT;
	private boolean noPreprocess;
	private boolean saveSampler;
	private String savedSamplerDir = LDAConfiguration.STORED_SAMPLER_DIR_DEFAULT;
	private String iterationCallbackClass = LDAConfiguration.MODEL_CALLBACK_DEFAULT;
	private String subConfig = "default";
	private String documentPriorFilename;

	public SimpleLDAConfiguration(LDALoggingUtils logUtil, String scheme,
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
		this.datasetFilename = datasetFn;
	}

	public SimpleLDAConfiguration() {
	}

	public SimpleLDAConfiguration(LDALoggingUtils logUtil, String scheme, Integer noTopics, Double alpha, Double beta,
			Double lambda, Integer noIters, Integer noBatches, Integer noTopicBatches, Integer rareThreshold,
			Integer tfIdfThreshold, Integer topicInterval, Integer startDiagnostic, Integer seed, boolean debug,
			String dataset_fn, String test_dataset_fn, String building_scheme, double percentage_split_size_doc,
			double percentage_split_size_topic, int resultSetSize, Integer fullPhiPeriod, Double topTokensToSample,
			String topicPriorFilename, String documentPriorFilename, String topic_building_scheme, String topic_batch_building_scheme,
			int instability_period, double[] fixed_split_size_doc, int skipStep, boolean savePhi, int phiBurnIn,
			String docLengthsFilename, boolean saveDocLengths, String termFrequencyFilename,
			boolean saveTermFrequencies, String vocabularyFn, boolean saveVocabulary, String corpusFn,
			boolean saveCorpus, boolean printPhi, boolean measureTiming, boolean logTokensPerTopic,
			boolean logTypeTopicDensity, boolean logDocumentDensity, String experimentOutputDirectory,
			boolean logPhiDensity, boolean keepNumbers, boolean saveDocumentTopicMeans, boolean saveDocumentTopicTheta,
			String documentTopicMeansOutputFilename, String documentTopicThetaOutputFilename,
			boolean saveDocumentTopicDiagnostics, String documentTopicDiagnosticsOutputFilename,
			String phiMeansOutputFilename, boolean keepConnectingPunctuation, String stoplistFilename, int nrTopWords,
			int maxDocBufferSize, int phiMeanThinDefault, String dirichletSamplerBuilderClassName,
			int aliasPoissonThreshold, String fileRegex, Integer hyperparamOptimInterval, boolean symmetricAlpha,
			double hdpGgamma, int hdpNrStartTopics, int documentSamplerSplitLimit, double hdpKPercentile,
			boolean logTopicIndicators, String samplerClass, String original_dataset_fn, boolean noPreprocess,
			boolean saveSampler, String savedSamplerDir, String iterationCallbackClass) {
		super();
		this.logUtil = logUtil;
		this.scheme = scheme;
		this.noTopics = noTopics;
		this.alpha = alpha;
		this.beta = beta;
		this.lambda = lambda;
		this.noIters = noIters;
		this.noBatches = noBatches;
		this.noTopicBatches = noTopicBatches;
		this.rareThreshold = rareThreshold;
		this.tfIdfThreshold = tfIdfThreshold;
		this.topicInterval = topicInterval;
		this.startDiagnostic = startDiagnostic;
		this.seed = seed;
		this.debug = debug;
		this.datasetFilename = dataset_fn;
		this.originalDatasetFilename = original_dataset_fn;
		this.testDatasetFilename = test_dataset_fn;
		this.buildingScheme = building_scheme;
		this.percentageSplitSizeDoc = percentage_split_size_doc;
		this.percentageSplitSizeTopic = percentage_split_size_topic;
		this.resultSetSize = resultSetSize;
		this.fullPhiPeriod = fullPhiPeriod;
		this.topTokensToSample = topTokensToSample;
		this.topicPriorFilename = topicPriorFilename;
		this.documentPriorFilename = documentPriorFilename;
		this.topic_building_scheme = topic_building_scheme;
		this.topic_batch_building_scheme = topic_batch_building_scheme;
		this.instability_period = instability_period;
		this.fixed_split_size_doc = fixed_split_size_doc;
		this.skipStep = skipStep;
		this.savePhi = savePhi;
		this.phiBurnIn = phiBurnIn;
		this.docLengthsFilename = docLengthsFilename;
		this.saveDocLengths = saveDocLengths;
		this.termFrequencyFilename = termFrequencyFilename;
		this.saveTermFrequencies = saveTermFrequencies;
		this.vocabularyFilename = vocabularyFn;
		this.saveVocabulary = saveVocabulary;
		this.corpusFn = corpusFn;
		this.saveCorpus = saveCorpus;
		this.printPhi = printPhi;
		this.measureTiming = measureTiming;
		this.logTokensPerTopic = logTokensPerTopic;
		this.logTypeTopicDensity = logTypeTopicDensity;
		this.logDocumentDensity = logDocumentDensity;
		this.experimentOutputDirectory = experimentOutputDirectory;
		this.logPhiDensity = logPhiDensity;
		this.keepNumbers = keepNumbers;
		this.saveDocumentTopicMeans = saveDocumentTopicMeans;
		this.saveDocumentTopicTheta = saveDocumentTopicTheta;
		this.documentTopicMeansOutputFilename = documentTopicMeansOutputFilename;
		this.documentTopicThetaOutputFilename = documentTopicThetaOutputFilename;
		this.saveDocumentTopicDiagnostics = saveDocumentTopicDiagnostics;
		this.documentTopicDiagnosticsOutputFilename = documentTopicDiagnosticsOutputFilename;
		this.phiMeansOutputFilename = phiMeansOutputFilename;
		this.keepConnectingPunctuation = keepConnectingPunctuation;
		this.stoplistFilename = stoplistFilename;
		this.nrTopWords = nrTopWords;
		this.maxDocBufferSize = maxDocBufferSize;
		this.phiMeanThinDefault = phiMeanThinDefault;
		this.dirichletSamplerBuilderClassName = dirichletSamplerBuilderClassName;
		this.aliasPoissonThreshold = aliasPoissonThreshold;
		this.fileRegex = fileRegex;
		this.hyperparamOptimInterval = hyperparamOptimInterval;
		this.symmetricAlpha = symmetricAlpha;
		this.hdpGgamma = hdpGgamma;
		this.hdpNrStartTopics = hdpNrStartTopics;
		this.documentSamplerSplitLimit = documentSamplerSplitLimit;
		this.hdpKPercentile = hdpKPercentile;
		this.logTopicIndicators = logTopicIndicators;
		this.samplerClass = samplerClass;
		this.noPreprocess = noPreprocess;
		this.saveSampler = saveSampler;
		this.savedSamplerDir = savedSamplerDir;
		this.iterationCallbackClass = iterationCallbackClass;
	}

	public void setPrintPhi(boolean printPhi) {
		this.printPhi = printPhi;
	}

	public void setMeasureTiming(boolean measureTiming) {
		this.measureTiming = measureTiming;
	}

	public void setLogTypeTopicDensity(boolean logTypeTopicDensity) {
		this.logTypeTopicDensity = logTypeTopicDensity;
	}

	public void setLogDocumentDensity(boolean logDocumentDensity) {
		this.logDocumentDensity = logDocumentDensity;
	}

	public void setExperimentOutputDirectory(String experimentOutputDirectory) {
		this.experimentOutputDirectory = experimentOutputDirectory;
	}

	public void setLogPhiDensity(boolean logPhiDensity) {
		this.logPhiDensity = logPhiDensity;
	}

	public void setKeepNumbers(boolean keepNumbers) {
		this.keepNumbers = keepNumbers;
	}

	public void setSaveDocumentTopicMeans(boolean saveDocumentTopicMeans) {
		this.saveDocumentTopicMeans = saveDocumentTopicMeans;
	}

	public void setDocumentTopicMeansOutputFilename(String documentTopicMeansOutputFilename) {
		this.documentTopicMeansOutputFilename = documentTopicMeansOutputFilename;
	}

	public void setPhiMeansOutputFilename(String phiMeansOutputFilename) {
		this.phiMeansOutputFilename = phiMeansOutputFilename;
	}

	public void setKeepConnectingPunctuation(boolean keepConnectingPunctuation) {
		this.keepConnectingPunctuation = keepConnectingPunctuation;
	}

	public Integer getNoTopics() {
		return noTopics;
	}

	public Integer getRareThreshold() {
		return rareThreshold;
	}

	public int getPhiBurnIn() {
		return phiBurnIn;
	}

	public void setDocLengthsFilename(String docLengthsFilename) {
		this.docLengthsFilename = docLengthsFilename;
	}

	public void setSaveDocLengths(boolean saveDocLengths) {
		this.saveDocLengths = saveDocLengths;
	}

	public void setSaveTermFrequencies(boolean saveTermFrequencies) {
		this.saveTermFrequencies = saveTermFrequencies;
	}

	public void setVocabularyFn(String vocabularyFn) {
		this.vocabularyFilename = vocabularyFn;
	}

	public void setSaveVocabulary(boolean saveVocabulary) {
		this.saveVocabulary = saveVocabulary;
	}

	public void setSaveCorpus(boolean saveCorpus) {
		this.saveCorpus = saveCorpus;
	}

	@Override
	@JsonIgnore public LDALoggingUtils getLoggingUtil() {
		return logUtil;
	}

	@Override
	public void setLoggingUtil(LDALoggingUtils logger) {
		logUtil = logger;
	}

	@Override
	@JsonIgnore public void activateSubconfig(String subConfName) {

	}

	@Override
	@JsonIgnore public void forceActivateSubconfig(String subConfName) {

	}

	@Override
	@JsonIgnore public String getActiveSubConfig() {
		return subConfig;
	}

	@Override
	@JsonIgnore public String[] getSubConfigs() {
		return new String[] {};
	}

	@Override
	public String whereAmI() {
		return "<SimpleLDAConfiguration>";
	}

	@Override
	public String getDatasetFilename() {
		return datasetFilename;
	}

	@Override
	public String getOriginalDatasetFilename() {
		return originalDatasetFilename;
	}

	@Override
	public String getTestDatasetFilename() {
		return testDatasetFilename;
	}


	public void setDatasetFilename(String fn) {
		datasetFilename = fn;
	}

	public void setOriginalDatasetFilename(String fn) {
		originalDatasetFilename = fn;
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

	public void setLogUtil(LDALoggingUtils logUtil) {
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

	public void setSeed(int seed) {
		this.seed = seed;
	}

	@Override
	public boolean getDebug() {
		return debug;
	}

	@Override
	public boolean getPrintPhi() {
		return printPhi;
	}

	@Override
	public boolean getMeasureTiming() {
		return measureTiming;
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
		this.buildingScheme = string;
	}

	@Override
	public String getDocumentBatchBuildingScheme(String batchBuildSchemeDefault) {
		return buildingScheme == null ? batchBuildSchemeDefault : buildingScheme;
	}

	@Override
	public String getTopicBatchBuildingScheme(String batchBuildSchemeDefault) {
		return topic_batch_building_scheme == null ? batchBuildSchemeDefault : topic_batch_building_scheme;
	}

	@Override
	public double getDocPercentageSplitSize() {
		return percentageSplitSizeDoc;
	}

	public void setDocPercentageSplitSize(double splitSize) {
		this.percentageSplitSizeDoc = splitSize;
	}

	@Override
	public double getTopicPercentageSplitSize() {
		return percentageSplitSizeTopic;
	}

	public void setTopicPercentageSplitSize(double splitSize) {
		this.percentageSplitSizeTopic = splitSize;
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
		return topTokensToSample == null ? defaultValue : topTokensToSample.doubleValue();
	}

	@Override
	public void setProperty(String key, Object value) {
		throw new RuntimeException("This method is not implemented for SimpleLDAConfiguration, use type safe deticated methods instead (you might need to implement the one you need! :))");
	}

	@Override
	@JsonIgnore public int[] getPrintNDocsInterval() {
		return new int [0];
	}

	@Override
	@JsonIgnore public int getPrintNDocs() {
		return 0;
	}

	@Override
	@JsonIgnore public int[] getPrintNTopWordsInterval() {
		return new int [0];
	}

	@Override
	@JsonIgnore public int getPrintNTopWords() {
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
		return logTypeTopicDensity;
	}

	@Override
	public boolean logDocumentDensity(boolean logDocumentDensityDefault) {
		return logDocumentDensity;
	}

	@Override
	public String getExperimentOutputDirectory(String defaultDir) {
		return experimentOutputDirectory;
	}

	@Override
	public double getVariableSelectionPrior(double vsPriorDefault) {
		return vsPriorDefault;
	}

	@Override
	public boolean logPhiDensity(String logPhiDensityDefault) {
		return logPhiDensity;
	}

	public String getTopicPriorFilename() {
		return topicPriorFilename;
	}

	@Override
	public String getDocumentPriorFilename() {
		return documentPriorFilename;
	}

	public void setTopicPriorFilename(String topicPriorFilename) {
		this.topicPriorFilename = topicPriorFilename;
	}

	@Override
	public String getStoplistFilename(String string) {
		return stoplistFilename;
	}

	public void setStoplistFilename(String string) {
		this.stoplistFilename = string;
	}

	@Override
	public boolean keepNumbers() {
		return keepNumbers;
	}

	@Override
	public boolean saveDocumentTopicMeans() {
		return saveDocumentTopicMeans;
	}

	@Override
	public String getDocumentTopicMeansOutputFilename() {
		return documentTopicMeansOutputFilename;
	}

	@Override
	public boolean saveDocumentTopicDiagnostics() {
		return saveDocumentTopicDiagnostics;
	}

	@Override
	public String getDocumentTopicDiagnosticsOutputFilename() {
		return documentTopicDiagnosticsOutputFilename;
	}

	@Override
	public String getPhiMeansOutputFilename() {
		return phiMeansOutputFilename;
	}

	@Override
	public boolean savePhiMeans(boolean defaultVal) {
		return savePhi;
	}

	public void setPhiBurnIn(int phiBurnIn) {
		this.phiBurnIn = phiBurnIn;
	}

	@Override
	public int getPhiBurnInPercent(int phiBurnInDefault) {
		return phiBurnIn;
	}

	@Override
	public int getPhiMeanThin(int phiMeanThinDefault) {
		return this.phiMeanThinDefault < 0 ? phiMeanThinDefault : this.phiMeanThinDefault;
	}

	public void setSavePhi(boolean savePhi) {
		this.savePhi = savePhi;
	}

	@Override
	public Integer getTfIdfVocabSize(int defaultValue) {
		return tfIdfThreshold == null ? defaultValue : tfIdfThreshold;
	}

	@Override
	public int getNrTopWords(int defaltNr) {
		if(nrTopWords < 0 )
			return defaltNr;
		else 
			return nrTopWords;
	}

	@Override
	public int getMaxDocumentBufferSize(int defaltSize) {
		if(maxDocBufferSize  < 0 )
			return defaltSize;
		else 
			return maxDocBufferSize;
	}

	@Override
	public boolean getKeepConnectingPunctuation(boolean defaultKeepConnectingPunctuation) {
		return keepConnectingPunctuation;
	}

	@Override
	public boolean saveVocabulary(boolean defaultVal) {
		return saveVocabulary;
	}

	@Override
	public String getVocabularyFilename() {
		return vocabularyFilename;
	}

	@Override
	public boolean saveTermFrequencies(boolean defaultValue) {
		return saveTermFrequencies;
	}

	@Override
	public String getTermFrequencyFilename() {
		return termFrequencyFilename;
	}

	@Override
	public boolean saveDocLengths(boolean defaultValue) {
		return saveDocLengths;
	}

	@Override
	public String getDocLengthsFilename() {
		return docLengthsFilename;
	}

	@Override
	public double getLambda(double lambdaDefault) {
		return lambda;
	}

	public void setSaveDocumentTopicTheta(boolean saveDocumentTopicTheta) {
		this.saveDocumentTopicTheta = saveDocumentTopicTheta;
	}

	public String getDocumentTopicThetaOutputFilename() {
		return documentTopicThetaOutputFilename;
	}

	public void setDocumentTopicThetaOutputFilename(String documentTopicThetaOutputFilename) {
		this.documentTopicThetaOutputFilename = documentTopicThetaOutputFilename;
	}

	@Override
	public boolean saveDocumentThetaEstimate() {
		return saveDocumentTopicTheta;
	}

	public String getDirichletSamplerBuilderClassName() {
		return dirichletSamplerBuilderClassName;
	}

	public void setDirichletSamplerBuilderClassName(String dirichletSamplerBuilderClassName) {
		this.dirichletSamplerBuilderClassName = dirichletSamplerBuilderClassName;
	}

	@Override
	public String getDirichletSamplerBuilderClass(String defaultName) {
		return getDirichletSamplerBuilderClassName();
	}

	public int getAliasPoissonThreshold() {
		return aliasPoissonThreshold;
	}

	public void setAliasPoissonThreshold(int aliasPoissonThreshold) {
		this.aliasPoissonThreshold = aliasPoissonThreshold;
	}

	@Override
	public int getAliasPoissonThreshold(int aliasPoissonDefaultThreshold) {
		return aliasPoissonThreshold;
	}

	public void setFileRegex(String string) {
		fileRegex = string;
	}

	@Override
	public String getFileRegex(String string) {
		return fileRegex == null ? string : fileRegex;
	}

	public void setSaveDocumentTopicDiagnostics(boolean saveDocumentTopicDiagnostics) {
		this.saveDocumentTopicDiagnostics = saveDocumentTopicDiagnostics;
	}

	public void setDocumentTopicDiagnosticsOutputFilename(String documentTopicDiagnosticsOutputFilename) {
		this.documentTopicDiagnosticsOutputFilename = documentTopicDiagnosticsOutputFilename;
	}

	public Integer getHyperparamOptimInterval(int defaultValue) {
		return hyperparamOptimInterval == null ? defaultValue : hyperparamOptimInterval;
	}

	public void setHyperparamOptimInterval(Integer hyperparamOptimInterval) {
		this.hyperparamOptimInterval = hyperparamOptimInterval;
	}

	@Override
	public boolean useSymmetricAlpha(boolean defaultAlpha) {
		return symmetricAlpha;
	}

	public void setUseSymmetricAlpha(boolean symmetricAlpha) {
		this.symmetricAlpha = symmetricAlpha;
	}

	@Override
	public double getHDPGamma(double gammaDefault) {
		return hdpGgamma;
	}

	public void setHdpGgamma(double hdpGgamma) {
		this.hdpGgamma = hdpGgamma;
	}

	@Override
	public int getHDPNrStartTopics(int defaultValue) {
		return hdpNrStartTopics;
	}

	public void setHDPNrStartTopics(int defaultValue) {
		this.hdpNrStartTopics = defaultValue;
	}

	@Override
	public boolean logTokensPerTopic(boolean logTokensPerTopic) {
		return this.logTokensPerTopic;
	}

	@Override
	public int getDocumentSamplerSplitLimit(int documentSamplerSplitLimitDefault) {
		return documentSamplerSplitLimit;
	}

	public void setDocumentSamplerSplitLimit(int documentSamplerSplitLimitDefault) {
		this.documentSamplerSplitLimit = documentSamplerSplitLimitDefault;
	}

	@Override
	public double getHDPKPercentile(double hdpKPercentile) {
		return this.hdpKPercentile ;
	}

	@Override
	public boolean saveCorpus(boolean b) {
		return saveCorpus;
	}

	@Override
	public String getCorpusFilename() {
		return corpusFn;
	}

	public void setCorpusFilename(String corpusFn) {
		this.corpusFn = corpusFn;
	}

	@Override
	public boolean logTopicIndicators(boolean b) {
		return logTopicIndicators;
	}

	public void setLogTopicIndicators(boolean b) {
		logTopicIndicators = b;
	}

	public String getSamplerClass() {
		return samplerClass;
	}

	public void setSamplerClass(String samplerClass) {
		this.samplerClass = samplerClass;
	}

	@Override
	public String getSamplerClass(String modelDefault) {
		return samplerClass;
	}

	public void setNoPreprocess(boolean val) {
		noPreprocess = val;
	}

	@Override
	public boolean noPreprocess() {
		return noPreprocess;
	}

	@Override
	public boolean saveSampler(boolean b) {
		return saveSampler;
	}

	public void setSaveSampler(boolean b) {
		this.saveSampler = b;
	}

	@Override
	public String getSavedSamplerDirectory(String string) {
		return savedSamplerDir;
	}

	public void setSavedSamplerDirectory(String savedSamplerDir) {
		this.savedSamplerDir = savedSamplerDir;
	}


	// Eclipse generated equals and hashCode
	// Needs to be re-generated if adding fields to class (that we care about)
	// LoggingUtils needs to be excluded since it generates temporary dirnames 
	// we do not want to affect the hashCode
	// Consider if it is worth pulling in Lombok for this... 

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + aliasPoissonThreshold;
		result = prime * result + ((alpha == null) ? 0 : alpha.hashCode());
		result = prime * result + ((beta == null) ? 0 : beta.hashCode());
		result = prime * result + ((buildingScheme == null) ? 0 : buildingScheme.hashCode());
		result = prime * result + ((corpusFn == null) ? 0 : corpusFn.hashCode());
		result = prime * result + ((datasetFilename == null) ? 0 : datasetFilename.hashCode());
		result = prime * result + (debug ? 1231 : 1237);
		result = prime * result
				+ ((dirichletSamplerBuilderClassName == null) ? 0 : dirichletSamplerBuilderClassName.hashCode());
		result = prime * result + ((docLengthsFilename == null) ? 0 : docLengthsFilename.hashCode());
		result = prime * result + documentSamplerSplitLimit;
		result = prime * result + ((documentTopicDiagnosticsOutputFilename == null) ? 0
				: documentTopicDiagnosticsOutputFilename.hashCode());
		result = prime * result
				+ ((documentTopicMeansOutputFilename == null) ? 0 : documentTopicMeansOutputFilename.hashCode());
		result = prime * result
				+ ((documentTopicThetaOutputFilename == null) ? 0 : documentTopicThetaOutputFilename.hashCode());
		result = prime * result + ((experimentOutputDirectory == null) ? 0 : experimentOutputDirectory.hashCode());
		result = prime * result + ((fileRegex == null) ? 0 : fileRegex.hashCode());
		result = prime * result + Arrays.hashCode(fixed_split_size_doc);
		result = prime * result + ((fullPhiPeriod == null) ? 0 : fullPhiPeriod.hashCode());
		long temp;
		temp = Double.doubleToLongBits(hdpGgamma);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(hdpKPercentile);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + hdpNrStartTopics;
		result = prime * result + ((hyperparamOptimInterval == null) ? 0 : hyperparamOptimInterval.hashCode());
		result = prime * result + instability_period;
		result = prime * result + (keepConnectingPunctuation ? 1231 : 1237);
		result = prime * result + (keepNumbers ? 1231 : 1237);
		result = prime * result + ((lambda == null) ? 0 : lambda.hashCode());
		result = prime * result + (logDocumentDensity ? 1231 : 1237);
		result = prime * result + (logPhiDensity ? 1231 : 1237);
		result = prime * result + (logTokensPerTopic ? 1231 : 1237);
		result = prime * result + (logTopicIndicators ? 1231 : 1237);
		result = prime * result + (logTypeTopicDensity ? 1231 : 1237);
		result = prime * result + maxDocBufferSize;
		result = prime * result + (measureTiming ? 1231 : 1237);
		result = prime * result + ((noBatches == null) ? 0 : noBatches.hashCode());
		result = prime * result + ((noIters == null) ? 0 : noIters.hashCode());
		result = prime * result + (noPreprocess ? 1231 : 1237);
		result = prime * result + ((noTopicBatches == null) ? 0 : noTopicBatches.hashCode());
		result = prime * result + ((noTopics == null) ? 0 : noTopics.hashCode());
		result = prime * result + nrTopWords;
		result = prime * result + ((originalDatasetFilename == null) ? 0 : originalDatasetFilename.hashCode());
		temp = Double.doubleToLongBits(percentageSplitSizeDoc);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(percentageSplitSizeTopic);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + phiBurnIn;
		result = prime * result + phiMeanThinDefault;
		result = prime * result + ((phiMeansOutputFilename == null) ? 0 : phiMeansOutputFilename.hashCode());
		result = prime * result + (printPhi ? 1231 : 1237);
		result = prime * result + ((rareThreshold == null) ? 0 : rareThreshold.hashCode());
		result = prime * result + resultSetSize;
		result = prime * result + ((samplerClass == null) ? 0 : samplerClass.hashCode());
		result = prime * result + (saveCorpus ? 1231 : 1237);
		result = prime * result + (saveDocLengths ? 1231 : 1237);
		result = prime * result + (saveDocumentTopicDiagnostics ? 1231 : 1237);
		result = prime * result + (saveDocumentTopicMeans ? 1231 : 1237);
		result = prime * result + (saveDocumentTopicTheta ? 1231 : 1237);
		result = prime * result + (savePhi ? 1231 : 1237);
		result = prime * result + (saveSampler ? 1231 : 1237);
		result = prime * result + (saveTermFrequencies ? 1231 : 1237);
		result = prime * result + (saveVocabulary ? 1231 : 1237);
		result = prime * result + ((savedSamplerDir == null) ? 0 : savedSamplerDir.hashCode());
		result = prime * result + ((scheme == null) ? 0 : scheme.hashCode());
		result = prime * result + ((seed == null) ? 0 : seed.hashCode());
		result = prime * result + skipStep;
		result = prime * result + ((startDiagnostic == null) ? 0 : startDiagnostic.hashCode());
		result = prime * result + ((stoplistFilename == null) ? 0 : stoplistFilename.hashCode());
		result = prime * result + (symmetricAlpha ? 1231 : 1237);
		result = prime * result + ((termFrequencyFilename == null) ? 0 : termFrequencyFilename.hashCode());
		result = prime * result + ((testDatasetFilename == null) ? 0 : testDatasetFilename.hashCode());
		result = prime * result + ((tfIdfThreshold == null) ? 0 : tfIdfThreshold.hashCode());
		result = prime * result + ((topTokensToSample == null) ? 0 : topTokensToSample.hashCode());
		result = prime * result + ((topicInterval == null) ? 0 : topicInterval.hashCode());
		result = prime * result + ((topicPriorFilename == null) ? 0 : topicPriorFilename.hashCode());
		result = prime * result + ((documentPriorFilename == null) ? 0 : documentPriorFilename.hashCode());
		result = prime * result + ((topic_batch_building_scheme == null) ? 0 : topic_batch_building_scheme.hashCode());
		result = prime * result + ((topic_building_scheme == null) ? 0 : topic_building_scheme.hashCode());
		result = prime * result + ((vocabularyFilename == null) ? 0 : vocabularyFilename.hashCode());
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
		SimpleLDAConfiguration other = (SimpleLDAConfiguration) obj;
		if (aliasPoissonThreshold != other.aliasPoissonThreshold)
			return false;
		if (alpha == null) {
			if (other.alpha != null)
				return false;
		} else if (!alpha.equals(other.alpha))
			return false;
		if (beta == null) {
			if (other.beta != null)
				return false;
		} else if (!beta.equals(other.beta))
			return false;
		if (buildingScheme == null) {
			if (other.buildingScheme != null)
				return false;
		} else if (!buildingScheme.equals(other.buildingScheme))
			return false;
		if (corpusFn == null) {
			if (other.corpusFn != null)
				return false;
		} else if (!corpusFn.equals(other.corpusFn))
			return false;
		if (datasetFilename == null) {
			if (other.datasetFilename != null)
				return false;
		} else if (!datasetFilename.equals(other.datasetFilename))
			return false;
		if (debug != other.debug)
			return false;
		if (dirichletSamplerBuilderClassName == null) {
			if (other.dirichletSamplerBuilderClassName != null)
				return false;
		} else if (!dirichletSamplerBuilderClassName.equals(other.dirichletSamplerBuilderClassName))
			return false;
		if (docLengthsFilename == null) {
			if (other.docLengthsFilename != null)
				return false;
		} else if (!docLengthsFilename.equals(other.docLengthsFilename))
			return false;
		if (documentSamplerSplitLimit != other.documentSamplerSplitLimit)
			return false;
		if (documentTopicDiagnosticsOutputFilename == null) {
			if (other.documentTopicDiagnosticsOutputFilename != null)
				return false;
		} else if (!documentTopicDiagnosticsOutputFilename.equals(other.documentTopicDiagnosticsOutputFilename))
			return false;
		if (documentTopicMeansOutputFilename == null) {
			if (other.documentTopicMeansOutputFilename != null)
				return false;
		} else if (!documentTopicMeansOutputFilename.equals(other.documentTopicMeansOutputFilename))
			return false;
		if (documentTopicThetaOutputFilename == null) {
			if (other.documentTopicThetaOutputFilename != null)
				return false;
		} else if (!documentTopicThetaOutputFilename.equals(other.documentTopicThetaOutputFilename))
			return false;
		if (experimentOutputDirectory == null) {
			if (other.experimentOutputDirectory != null)
				return false;
		} else if (!experimentOutputDirectory.equals(other.experimentOutputDirectory))
			return false;
		if (fileRegex == null) {
			if (other.fileRegex != null)
				return false;
		} else if (!fileRegex.equals(other.fileRegex))
			return false;
		if (!Arrays.equals(fixed_split_size_doc, other.fixed_split_size_doc))
			return false;
		if (fullPhiPeriod == null) {
			if (other.fullPhiPeriod != null)
				return false;
		} else if (!fullPhiPeriod.equals(other.fullPhiPeriod))
			return false;
		if (Double.doubleToLongBits(hdpGgamma) != Double.doubleToLongBits(other.hdpGgamma))
			return false;
		if (Double.doubleToLongBits(hdpKPercentile) != Double.doubleToLongBits(other.hdpKPercentile))
			return false;
		if (hdpNrStartTopics != other.hdpNrStartTopics)
			return false;
		if (hyperparamOptimInterval == null) {
			if (other.hyperparamOptimInterval != null)
				return false;
		} else if (!hyperparamOptimInterval.equals(other.hyperparamOptimInterval))
			return false;
		if (instability_period != other.instability_period)
			return false;
		if (keepConnectingPunctuation != other.keepConnectingPunctuation)
			return false;
		if (keepNumbers != other.keepNumbers)
			return false;
		if (lambda == null) {
			if (other.lambda != null)
				return false;
		} else if (!lambda.equals(other.lambda))
			return false;
		if (logDocumentDensity != other.logDocumentDensity)
			return false;
		if (logPhiDensity != other.logPhiDensity)
			return false;
		if (logTokensPerTopic != other.logTokensPerTopic)
			return false;
		if (logTopicIndicators != other.logTopicIndicators)
			return false;
		if (logTypeTopicDensity != other.logTypeTopicDensity)
			return false;
		if (maxDocBufferSize != other.maxDocBufferSize)
			return false;
		if (measureTiming != other.measureTiming)
			return false;
		if (noBatches == null) {
			if (other.noBatches != null)
				return false;
		} else if (!noBatches.equals(other.noBatches))
			return false;
		if (noIters == null) {
			if (other.noIters != null)
				return false;
		} else if (!noIters.equals(other.noIters))
			return false;
		if (noPreprocess != other.noPreprocess)
			return false;
		if (noTopicBatches == null) {
			if (other.noTopicBatches != null)
				return false;
		} else if (!noTopicBatches.equals(other.noTopicBatches))
			return false;
		if (noTopics == null) {
			if (other.noTopics != null)
				return false;
		} else if (!noTopics.equals(other.noTopics))
			return false;
		if (nrTopWords != other.nrTopWords)
			return false;
		if (originalDatasetFilename == null) {
			if (other.originalDatasetFilename != null)
				return false;
		} else if (!originalDatasetFilename.equals(other.originalDatasetFilename))
			return false;
		if (Double.doubleToLongBits(percentageSplitSizeDoc) != Double
				.doubleToLongBits(other.percentageSplitSizeDoc))
			return false;
		if (Double.doubleToLongBits(percentageSplitSizeTopic) != Double
				.doubleToLongBits(other.percentageSplitSizeTopic))
			return false;
		if (phiBurnIn != other.phiBurnIn)
			return false;
		if (phiMeanThinDefault != other.phiMeanThinDefault)
			return false;
		if (phiMeansOutputFilename == null) {
			if (other.phiMeansOutputFilename != null)
				return false;
		} else if (!phiMeansOutputFilename.equals(other.phiMeansOutputFilename))
			return false;
		if (printPhi != other.printPhi)
			return false;
		if (rareThreshold == null) {
			if (other.rareThreshold != null)
				return false;
		} else if (!rareThreshold.equals(other.rareThreshold))
			return false;
		if (resultSetSize != other.resultSetSize)
			return false;
		if (samplerClass == null) {
			if (other.samplerClass != null)
				return false;
		} else if (!samplerClass.equals(other.samplerClass))
			return false;
		if (saveCorpus != other.saveCorpus)
			return false;
		if (saveDocLengths != other.saveDocLengths)
			return false;
		if (saveDocumentTopicDiagnostics != other.saveDocumentTopicDiagnostics)
			return false;
		if (saveDocumentTopicMeans != other.saveDocumentTopicMeans)
			return false;
		if (saveDocumentTopicTheta != other.saveDocumentTopicTheta)
			return false;
		if (savePhi != other.savePhi)
			return false;
		if (saveSampler != other.saveSampler)
			return false;
		if (saveTermFrequencies != other.saveTermFrequencies)
			return false;
		if (saveVocabulary != other.saveVocabulary)
			return false;
		if (savedSamplerDir == null) {
			if (other.savedSamplerDir != null)
				return false;
		} else if (!savedSamplerDir.equals(other.savedSamplerDir))
			return false;
		if (scheme == null) {
			if (other.scheme != null)
				return false;
		} else if (!scheme.equals(other.scheme))
			return false;
		if (seed == null) {
			if (other.seed != null)
				return false;
		} else if (!seed.equals(other.seed))
			return false;
		if (skipStep != other.skipStep)
			return false;
		if (startDiagnostic == null) {
			if (other.startDiagnostic != null)
				return false;
		} else if (!startDiagnostic.equals(other.startDiagnostic))
			return false;
		if (stoplistFilename == null) {
			if (other.stoplistFilename != null)
				return false;
		} else if (!stoplistFilename.equals(other.stoplistFilename))
			return false;
		if (symmetricAlpha != other.symmetricAlpha)
			return false;
		if (termFrequencyFilename == null) {
			if (other.termFrequencyFilename != null)
				return false;
		} else if (!termFrequencyFilename.equals(other.termFrequencyFilename))
			return false;
		if (testDatasetFilename == null) {
			if (other.testDatasetFilename != null)
				return false;
		} else if (!testDatasetFilename.equals(other.testDatasetFilename))
			return false;
		if (tfIdfThreshold == null) {
			if (other.tfIdfThreshold != null)
				return false;
		} else if (!tfIdfThreshold.equals(other.tfIdfThreshold))
			return false;
		if (topTokensToSample == null) {
			if (other.topTokensToSample != null)
				return false;
		} else if (!topTokensToSample.equals(other.topTokensToSample))
			return false;
		if (topicInterval == null) {
			if (other.topicInterval != null)
				return false;
		} else if (!topicInterval.equals(other.topicInterval))
			return false;
		if (topicPriorFilename == null) {
			if (other.topicPriorFilename != null)
				return false;
		} else if (!topicPriorFilename.equals(other.topicPriorFilename))
			return false;
		if (documentPriorFilename == null) {
			if (other.documentPriorFilename != null)
				return false;
		} else if (!documentPriorFilename.equals(other.documentPriorFilename))
			return false;
		if (topic_batch_building_scheme == null) {
			if (other.topic_batch_building_scheme != null)
				return false;
		} else if (!topic_batch_building_scheme.equals(other.topic_batch_building_scheme))
			return false;
		if (topic_building_scheme == null) {
			if (other.topic_building_scheme != null)
				return false;
		} else if (!topic_building_scheme.equals(other.topic_building_scheme))
			return false;
		if (vocabularyFilename == null) {
			if (other.vocabularyFilename != null)
				return false;
		} else if (!vocabularyFilename.equals(other.vocabularyFilename))
			return false;
		return true;
	}

	@Override
	public String getIterationCallbackClass(String modelCallbackDefault) {
		return iterationCallbackClass;
	}

	public void setIterationCallbackClass(String modelCallback) {
		this.iterationCallbackClass = modelCallback;
	}

	@Override
	public String toString() {
		ObjectMapper mapper  = new ObjectMapper();
		mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
				.withFieldVisibility(JsonAutoDetect.Visibility.ANY));
		//		                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
		//		                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
		//		                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));

		try {
			return mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			System.err.println("Could not serialize config to JSON:" + e.toString());
			return hashCode() + "";
		}
	}
}
