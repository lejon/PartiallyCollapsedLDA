package cc.mallet.configuration;

import cc.mallet.topics.randomscan.document.BatchBuilderFactory;
import cc.mallet.topics.randomscan.topic.TopicBatchBuilderFactory;
import cc.mallet.topics.randomscan.topic.TopicIndexBuilderFactory;
import cc.mallet.util.LoggingUtils;

public interface LDAConfiguration {

	public static final int START_DIAG_DEFAULT = 500;
	public static final int NO_TOPICS_DEFAULT = 10;
	public static final double ALPHA_DEFAULT = 50.0 / NO_TOPICS_DEFAULT;
	public static final double BETA_DEFAULT = 0.01;
	public static final int NO_BATCHES_DEFAULT = 4;
	public static final int NO_TOPIC_BATCHES_DEFAULT = 2;
	public static final int RARE_WORD_THRESHOLD = 0;
	public static final Integer NO_ITER_DEFAULT = 1500;
	public static final int TOPIC_INTER_DEFAULT = 10;
	public static final int SEED_DEFAULT = 0; // Default should be to use clock time
	public static final int RESULTS_SIZE_DEFAULT = 1;
	public static final String BATCH_BUILD_SCHEME_DEFAULT = BatchBuilderFactory.EVEN_SPLIT;
	public static final String TOPIC_BATCH_BUILD_SCHEME_DEFAULT = TopicBatchBuilderFactory.EVEN_SPLIT;
	public static final String TOPIC_INDEX_BUILD_SCHEME_DEFAULT = TopicIndexBuilderFactory.ALL;
	public static final boolean LOG_TYPE_TOPIC_DENSITY_DEFAULT = false;
	public static final boolean LOG_DOCUMENT_DENSITY_DEFAULT = false;
	public static final String LOG_PHI_DENSITY_DEFAULT = null;
	public static final int PHI_BURN_IN_DEFAULT = 0;
	public static final int PHI_THIN_DEFAULT = 1;
	public static final boolean SAVE_PHI_MEAN_DEFAULT = false;
	public static final int TF_IDF_VOCAB_SIZE_DEFAULT = -1;
	public static final int NO_TOP_WORDS_DEFAULT = 20;
	public static final int MAX_DOC_BUFFFER_SIZE_DEFAULT = 10000;
	public static final boolean KEEP_CONNECTING_PUNCTUATION = false;
	public static final double LAMBDA_DEFAULT = 0.6;
	public static final String SPARSE_DIRICHLET_SAMPLER_DEFAULT = "cc.mallet.types.MarsagliaSparseDirichlet";
	public static final String SPARSE_DIRICHLET_SAMPLER_BULDER_DEFAULT = "cc.mallet.types.DefaultSparseDirichletSamplerBuilder";
	public static final int ALIAS_POISSON_DEFAULT_THRESHOLD = 100;
	public static final String FILE_REGEX_DEFAULT = ".*\\.txt$";
	public static final int HYPERPARAM_OPTIM_INTERVAL_DEFAULT = -1;
	public static final boolean SYMMETRIC_ALPHA_DEFAULT = false;
	public static final double HDP_GAMMA_DEFAULT = 1;
	public static final int HDP_START_TOPICS_DEFAULT = 1;
	public static final boolean LOG_TOKENS_PER_TOPIC = false;
	public static final int DOCUMENT_SAMPLER_SPLIT_LIMIT_DEFAULT = 100;
	public static final double HDP_K_PERCENTILE = .8;

	public LoggingUtils getLoggingUtil();

	public void setLoggingUtil(LoggingUtils logger);

	public void activateSubconfig(String subConfName);

	public void forceActivateSubconfig(String subConfName);

	public String getActiveSubConfig();

	public String[] getSubConfigs();

	public String whereAmI();

	public String getDatasetFilename();

	public String getScheme();

	public Integer getNoTopics(int defaultValue);
	
	public void setNoTopics(int newValue);

	public Double getAlpha(double defaultValue);

	public Double getBeta(double defaultValue);

	public Integer getNoIterations(int defaultValue);

	public Integer getNoBatches(int defaultValue);

	public Integer getNoTopicBatches(int defaultValue);

	public Integer getRareThreshold(int defaultValue);
	
	public Integer getTfIdfVocabSize(int defaultValue);

	public Integer getTopicInterval(int defaultValue);

	public Integer getStartDiagnostic(int defaultValue);

	public int getSeed(int seedDefault);
	
	public boolean getDebug();

	public boolean getPrintPhi();
	
	public int [] getIntArrayProperty(String key, int [] defaultValues);
	
	public boolean getMeasureTiming();

	public int getResultSize(int resultsSizeDefault);

	public String getDocumentBatchBuildingScheme(String batchBuildSchemeDefault);

	// How to build the topic batches
	public String getTopicBatchBuildingScheme(String batchBuildSchemeDefault);
	
	// How to build which words in the topics to sample
	public String getTopicIndexBuildingScheme(String topicIndexBuildSchemeDefault);

	public double getDocPercentageSplitSize();
	
	public double getTopicPercentageSplitSize();

	public int getInstabilityPeriod(int defaultValue);

	public double[] getFixedSplitSizeDoc();

	public int getFullPhiPeriod(int defaultValue);

	public String[] getSubTopicIndexBuilders(int i);

	public double topTokensToSample(double defaultValue);
	
	void setProperty(String key, Object value);

	public int[] getPrintNDocsInterval();

	public int getPrintNDocs();

	public int[] getPrintNTopWordsInterval();

	public int getPrintNTopWords();

	public int getProportionalTopicIndexBuilderSkipStep();

	public boolean logTypeTopicDensity(boolean logTypeTopicDensityDefault);

	public boolean logDocumentDensity(boolean logDocumentDensityDefault);
	
	public String getExperimentOutputDirectory(String defaultDir);

	public double getVariableSelectionPrior(double vsPriorDefault);

	public boolean logPhiDensity(String logPhiDensityDefault);

	public String getTopicPriorFilename();

	public String getStoplistFilename(String string);

	public boolean keepNumbers();

	public boolean saveDocumentTopicMeans();

	public String getDocumentTopicMeansOutputFilename();

	public String getPhiMeansOutputFilename();

	public boolean savePhiMeans(boolean defaultVal);

	public int getPhiBurnInPercent(int phiBurnInDefault);

	int getPhiMeanThin(int phiMeanThinDefault);

	public int getNrTopWords(int defaltNr);
	
	public int getMaxDocumentBufferSize(int defaltSize);

	public boolean getKeepConnectingPunctuation(boolean defaultKeepConnectingPunctuation);
	
	public boolean saveVocabulary(boolean defaultVal);

	public String getVocabularyFilename();

	public boolean saveTermFrequencies(boolean defaultValue);

	public String getTermFrequencyFilename();

	public boolean saveDocLengths(boolean defaultValue);

	public String getDocLengthsFilename();

	public double getLambda(double lambdaDefault);

	public String getDocumentTopicThetaOutputFilename();

	public boolean saveDocumentThetaEstimate();

	public String getDirichletSamplerBuilderClass(String samplerBuilderClassName);

	public int getAliasPoissonThreshold(int aliasPoissonDefaultThreshold);

	public String getFileRegex(String string);

	public String getTestDatasetFilename();

	public boolean saveDocumentTopicDiagnostics();

	public String getDocumentTopicDiagnosticsOutputFilename();

	public Integer getHyperparamOptimInterval(int defaultValue);

	public boolean useSymmetricAlpha(boolean symmetricAlpha);

	public double getHDPGamma(double gammaDefault);

	public int getHDPNrStartTopics(int hdpStartTopicsDefault);

	public boolean logTokensPerTopic(boolean logTokensPerTopic);

	public int getDocumentSamplerSplitLimit(int documentSamplerSplitLimitDefault);

	public double getHDPKPercentile(double hdpKPercentile);

	public boolean saveCorpus(boolean b);

	public String getCorpusFilename();

	public boolean logTopicIndicators(boolean b);
}