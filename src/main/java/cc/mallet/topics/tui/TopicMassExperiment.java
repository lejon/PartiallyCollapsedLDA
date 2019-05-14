package cc.mallet.topics.tui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.FileSystem;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.HierarchicalConfiguration.Node;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.event.ConfigurationErrorEvent;
import org.apache.commons.configuration.event.ConfigurationErrorListener;
import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.apache.commons.configuration.interpol.ConfigurationInterpolator;
import org.apache.commons.configuration.reloading.ReloadingStrategy;
import org.apache.commons.configuration.tree.ConfigurationNode;
import org.apache.commons.configuration.tree.ExpressionEngine;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.commons.logging.Log;

import cc.mallet.configuration.ConfigFactory;
import cc.mallet.configuration.LDACommandLineParser;
import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.configuration.ParsedLDAConfiguration;
import cc.mallet.topics.LDAGibbsSampler;
import cc.mallet.topics.UncollapsedParallelLDA;
import cc.mallet.types.InstanceList;
import cc.mallet.util.LDAUtils;
import cc.mallet.util.LoggingUtils;

public class TopicMassExperiment {

	private static PrintWriter pw;

	public static void main(String[] args) throws Exception {
		
		Thread.setDefaultUncaughtExceptionHandler(new Thread.
				UncaughtExceptionHandler() {
			public void uncaughtException(Thread t, Throwable e) {
				System.out.println(t + " throws exception: " + e);
				e.printStackTrace();
				System.err.println("Main thread Exiting.");
				System.exit(-1);
			}
		});
		
		topicMassExperiment(args);
		
		
		int [] rwds = {0,10,100,200,500,1000,2000};
		pw = new PrintWriter(new FileWriter(new File("typeMass.csv")));
		pw.println("RareWords, Dataset, VocabSize, CorpusSize, Instances");
		for (int i = 0; i < rwds.length; i++) {
			rareWordsExperiment(args, rwds[i]);
			pw.println();
		}
		pw.flush();
		pw.close();
	}

	static void topicMassExperiment(String[] args) throws ParseException, ConfigurationException, FileNotFoundException {
		LDACommandLineParser cp = new LDACommandLineParser(args);
		String logSuitePath = "Runs/RunSuite" + LoggingUtils.getDateStamp();
		LDAConfiguration config = (LDAConfiguration) ConfigFactory.getMainConfiguration(cp);
		LoggingUtils lu = new LoggingUtils();
		lu.checkAndCreateCurrentLogDir(logSuitePath);
		config.setLoggingUtil(lu);

		int commonSeed = config.getSeed(LDAConfiguration.SEED_DEFAULT);
		String [] configs = config.getSubConfigs();
		for(String conf : configs) {
			lu.checkCreateAndSetSubLogDir(conf);
			config.activateSubconfig(conf);

			System.out.println("Using Config: " + config.whereAmI());
			String dataset_fn = config.getDatasetFilename();
			System.out.println("Using dataset: " + dataset_fn);
			String whichModel = config.getScheme();
			System.out.println("Scheme: " + whichModel);

			InstanceList instances = LDAUtils.loadInstances(dataset_fn, 
					"stoplist.txt", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));

			LDAGibbsSampler model = new UncollapsedParallelLDA(config);
			System.out.println(
					String.format("Uncollapsed LDA (%d batches).", 
							config.getNoBatches(LDAConfiguration.NO_BATCHES_DEFAULT)));
			System.out.println(String.format("Rare word threshold: %d", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD)));

			InstanceList trainingSet = instances;
			model.setRandomSeed(commonSeed);
			System.out.println("Using seed " + config.getSeed(LDAConfiguration.SEED_DEFAULT));
			// Imports the data into the model

			System.out.println("Vocabulary size: " + trainingSet.getDataAlphabet().size() + "\n");
			System.out.println("Loading " + trainingSet.size() + " instances...\n");

			model.addInstances(trainingSet);
			
			int [] wordIdxs = model.getTopTypeFrequencyIndices(); 
			int [] counts   = model.getTypeFrequencies();
			int totSum = 0;
			//System.out.println("Word order:");
			for (int i = wordIdxs.length-1; i >= 0; i--) {
				totSum += counts[wordIdxs[i]];
				//System.out.println("Word ["+wordIdxs[i]+", " +i+"] " + model.getAlphabet().lookupObject(wordIdxs[i]) + " occurs: " + counts[wordIdxs[i]] + " times");
			}
			System.out.println("Tot sum:" + totSum + " Alphabet size: " + model.getAlphabet().size());
			double [] cumsum = model.getTypeMassCumSum();
			//System.out.println("Cumsum:" + model.get);
			for (int i = 0; i < cumsum.length; i++) {
				double mass = (((double)i)/cumsum.length);
				//System.out.println("CumSum[" + mass + ", " + i + "]: " + cumsum[i]);
				//if( i > 100) Thread.sleep(10000);
				if(i%50==0) {
					System.out.printf("CumSum[%.4f]:", mass);
					System.out.println(cumsum[i]);
				}
			}
			System.out.println("Dataset:" + dataset_fn);
		}
	}
	
	static void rareWordsExperiment(String[] args, int rareWordTh) throws ParseException, 
	ConfigurationException, IOException {
		LDACommandLineParser cp = new LDACommandLineParser(args);
		String logSuitePath = "Runs/RunSuite" + LoggingUtils.getDateStamp();
		LDAConfiguration origConfig = (LDAConfiguration) ConfigFactory.getMainConfiguration(cp);
		
		RWConfig config = new RWConfig(origConfig);
		
		config.setRareThreshold(rareWordTh);
		
		LoggingUtils lu = new LoggingUtils();
		lu.checkAndCreateCurrentLogDir(logSuitePath);
		config.setLoggingUtil(lu);

		int commonSeed = config.getSeed(LDAConfiguration.SEED_DEFAULT);
		String [] configs = config.getSubConfigs();
		for(String conf : configs) {
			lu.checkCreateAndSetSubLogDir(conf);
			config.activateSubconfig(conf);

			String dataset_fn = config.getDatasetFilename();

			InstanceList instances = LDAUtils.loadInstances(dataset_fn, 
					"stoplist.txt", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));

			LDAGibbsSampler model = new UncollapsedParallelLDA(config);
			System.out.println(
					String.format("Uncollapsed LDA (%d batches).", 
							config.getNoBatches(LDAConfiguration.NO_BATCHES_DEFAULT)));

			InstanceList trainingSet = instances;
			model.setRandomSeed(commonSeed);
			model.addInstances(trainingSet);
			
			pw.print(rareWordTh 
					+ ", " + dataset_fn
					+ ", " + trainingSet.getDataAlphabet().size()
					+ ", " + model.getCorpusSize()
					+ ", " + trainingSet.size()
					);
			pw.println();
			
			System.out.println("Rare word threshold: " + rareWordTh);
			System.out.println("Dataset            : " + dataset_fn);
			System.out.println("Vocabulary size    : " + trainingSet.getDataAlphabet().size());
			System.out.println("Instances          : " + trainingSet.size());
			System.out.println("Coprus size      : " + model.getCorpusSize());
		}
	}
	
	public static class RWConfig implements LDAConfiguration {
		ParsedLDAConfiguration pc;
		private Integer rareWordTh;
		
		public RWConfig(LDAConfiguration origConfig) {
			this.pc = (ParsedLDAConfiguration) origConfig;
		}

		public LoggingUtils getLoggingUtil() {
			return pc.getLoggingUtil();
		}

		public void setLoggingUtil(LoggingUtils logger) {
			pc.setLoggingUtil(logger);
		}

		public void activateSubconfig(String subConfName) {
			pc.activateSubconfig(subConfName);
		}

		public int hashCode() {
			return pc.hashCode();
		}

		public void forceActivateSubconfig(String subConfName) {
			pc.forceActivateSubconfig(subConfName);
		}

		public String getActiveSubConfig() {
			return pc.getActiveSubConfig();
		}

		public String[] getStringArrayProperty(String key) {
			return pc.getStringArrayProperty(key);
		}

		public int[] getIntArrayProperty(String key, int[] defaultValues) {
			return pc.getIntArrayProperty(key, defaultValues);
		}

		public double[] getDoubleArrayProperty(String key) {
			return pc.getDoubleArrayProperty(key);
		}

		public String getStringProperty(String key) {
			return pc.getStringProperty(key);
		}

		public void addConfigurationListener(ConfigurationListener l) {
			pc.addConfigurationListener(l);
		}

		public Object getConfProperty(String key) {
			return pc.getConfProperty(key);
		}

		public boolean removeConfigurationListener(ConfigurationListener l) {
			return pc.removeConfigurationListener(l);
		}

		public void setProperty(String key, Object value) {
			pc.setProperty(key, value);
		}

		public String[] getSubConfigs() {
			return pc.getSubConfigs();
		}

		public Collection<ConfigurationListener> getConfigurationListeners() {
			return pc.getConfigurationListeners();
		}

		public Integer getInteger(String key, Integer defaultValue) {
			return pc.getInteger(key, defaultValue);
		}

		public void clearProperty(String key) {
			pc.clearProperty(key);
		}

		public Double getDouble(String key, Double defaultValue) {
			return pc.getDouble(key, defaultValue);
		}

		public void clearConfigurationListeners() {
			pc.clearConfigurationListeners();
		}

		public void clearTree(String key) {
			pc.clearTree(key);
		}

		public boolean isDetailEvents() {
			return pc.isDetailEvents();
		}

		public String whereAmI() {
			return pc.whereAmI();
		}

		public String getDatasetFilename() {
			return pc.getDatasetFilename();
		}

		public String getTestDatasetFilename() {
			return pc.getTestDatasetFilename();
		}

		public void setDetailEvents(boolean enable) {
			pc.setDetailEvents(enable);
		}

		public void load() throws ConfigurationException {
			pc.load();
		}

		public String getScheme() {
			return pc.getScheme();
		}

		public void load(String fileName) throws ConfigurationException {
			pc.load(fileName);
		}

		public Integer getNoTopics(int defaultValue) {
			return pc.getNoTopics(defaultValue);
		}

		public void load(File file) throws ConfigurationException {
			pc.load(file);
		}

		public void load(URL url) throws ConfigurationException {
			pc.load(url);
		}

		public Double getAlpha(double defaultValue) {
			return pc.getAlpha(defaultValue);
		}

		public void load(InputStream in) throws ConfigurationException {
			pc.load(in);
		}

		public void load(InputStream in, String encoding) throws ConfigurationException {
			pc.load(in, encoding);
		}

		public Double getBeta(double defaultValue) {
			return pc.getBeta(defaultValue);
		}

		public void addErrorListener(ConfigurationErrorListener l) {
			pc.addErrorListener(l);
		}

		public void save() throws ConfigurationException {
			pc.save();
		}

		public Integer getNoIterations(int defaultValue) {
			return pc.getNoIterations(defaultValue);
		}

		public void save(String fileName) throws ConfigurationException {
			pc.save(fileName);
		}

		public void save(File file) throws ConfigurationException {
			pc.save(file);
		}

		public Integer getNoBatches(int defaultValue) {
			return pc.getNoBatches(defaultValue);
		}

		public void save(URL url) throws ConfigurationException {
			pc.save(url);
		}

		public boolean removeErrorListener(ConfigurationErrorListener l) {
			return pc.removeErrorListener(l);
		}

		public void save(OutputStream out) throws ConfigurationException {
			pc.save(out);
		}

		public Integer getRareThreshold(int defaultValue) {
			return rareWordTh;
		}

		public void setRareThreshold(int defaultValue) {
			rareWordTh = defaultValue;
		}

		public void save(OutputStream out, String encoding) throws ConfigurationException {
			pc.save(out, encoding);
		}

		public Integer getTopicInterval(int defaultValue) {
			return pc.getTopicInterval(defaultValue);
		}

		public String getFileName() {
			return pc.getFileName();
		}

		public void setFileName(String fileName) {
			pc.setFileName(fileName);
		}

		public void clearErrorListeners() {
			pc.clearErrorListeners();
		}

		public Integer getStartDiagnostic(int defaultValue) {
			return pc.getStartDiagnostic(defaultValue);
		}

		public String getBasePath() {
			return pc.getBasePath();
		}

		public Collection<ConfigurationErrorListener> getErrorListeners() {
			return pc.getErrorListeners();
		}

		public void setBasePath(String basePath) {
			pc.setBasePath(basePath);
		}

		public File getFile() {
			return pc.getFile();
		}

		public int getSeed(int seedDefault) {
			return pc.getSeed(seedDefault);
		}

		public void setFile(File file) {
			pc.setFile(file);
		}

		public URL getURL() {
			return pc.getURL();
		}

		public void setURL(URL url) {
			pc.setURL(url);
		}

		public void setListDelimiter(char listDelimiter) {
			pc.setListDelimiter(listDelimiter);
		}

		public boolean getDebug() {
			return pc.getDebug();
		}

		public void setAutoSave(boolean autoSave) {
			pc.setAutoSave(autoSave);
		}

		public boolean isAutoSave() {
			return pc.isAutoSave();
		}

		public boolean getPrintPhi() {
			return pc.getPrintPhi();
		}

		public ReloadingStrategy getReloadingStrategy() {
			return pc.getReloadingStrategy();
		}

		public void setReloadingStrategy(ReloadingStrategy strategy) {
			pc.setReloadingStrategy(strategy);
		}

		public boolean getMeasureTiming() {
			return pc.getMeasureTiming();
		}

		public char getListDelimiter() {
			return pc.getListDelimiter();
		}

		public void reload() {
			pc.reload();
		}

		public void setNoTopics(int newValue) {
			pc.setNoTopics(newValue);
		}

		public boolean isDelimiterParsingDisabled() {
			return pc.isDelimiterParsingDisabled();
		}

		public int getResultSize(int resultsSizeDefault) {
			return pc.getResultSize(resultsSizeDefault);
		}

		public Node getRoot() {
			return pc.getRoot();
		}

		public String getDocumentBatchBuildingScheme(String batchBuildSchemeDefault) {
			return pc.getDocumentBatchBuildingScheme(batchBuildSchemeDefault);
		}

		public void refresh() throws ConfigurationException {
			pc.refresh();
		}

		public void setDelimiterParsingDisabled(boolean delimiterParsingDisabled) {
			pc.setDelimiterParsingDisabled(delimiterParsingDisabled);
		}

		public String getTopicBatchBuildingScheme(String batchBuildSchemeDefault) {
			return pc.getTopicBatchBuildingScheme(batchBuildSchemeDefault);
		}

		public void setRoot(Node node) {
			pc.setRoot(node);
		}

		public double getDocPercentageSplitSize() {
			return pc.getDocPercentageSplitSize();
		}

		public double getTopicPercentageSplitSize() {
			return pc.getTopicPercentageSplitSize();
		}

		public String toString() {
			return pc.toString();
		}

		public String getEncoding() {
			return pc.getEncoding();
		}

		public void setEncoding(String encoding) {
			pc.setEncoding(encoding);
		}

		public Integer getNoTopicBatches(int defaultValue) {
			return pc.getNoTopicBatches(defaultValue);
		}

		public Object getReloadLock() {
			return pc.getReloadLock();
		}

		public String getTopicIndexBuildingScheme(String topicIndexBuildSchemeDefault) {
			return pc.getTopicIndexBuildingScheme(topicIndexBuildSchemeDefault);
		}

		public void setThrowExceptionOnMissing(boolean throwExceptionOnMissing) {
			pc.setThrowExceptionOnMissing(throwExceptionOnMissing);
		}

		public boolean containsKey(String key) {
			return pc.containsKey(key);
		}

		public ConfigurationNode getRootNode() {
			return pc.getRootNode();
		}

		public int getInstabilityPeriod(int defaultValue) {
			return pc.getInstabilityPeriod(defaultValue);
		}

		public Iterator<String> getKeys() {
			return pc.getKeys();
		}

		public double[] getFixedSplitSizeDoc() {
			return pc.getFixedSplitSizeDoc();
		}

		public void setRootNode(ConfigurationNode rootNode) {
			pc.setRootNode(rootNode);
		}

		public Iterator<String> getKeys(String prefix) {
			return pc.getKeys(prefix);
		}

		public int getFullPhiPeriod(int defaultValue) {
			return pc.getFullPhiPeriod(defaultValue);
		}

		public String[] getSubTopicIndexBuilders(int i) {
			return pc.getSubTopicIndexBuilders(i);
		}

		public Object getProperty(String key) {
			return pc.getProperty(key);
		}

		public double topTokensToSample(double defaultValue) {
			return pc.topTokensToSample(defaultValue);
		}

		public boolean isThrowExceptionOnMissing() {
			return pc.isThrowExceptionOnMissing();
		}

		public int[] getPrintNDocsInterval() {
			return pc.getPrintNDocsInterval();
		}

		public int getPrintNDocs() {
			return pc.getPrintNDocs();
		}

		public void save(Writer writer) throws ConfigurationException {
			pc.save(writer);
		}

		public StrSubstitutor getSubstitutor() {
			return pc.getSubstitutor();
		}

		public boolean isEmpty() {
			return pc.isEmpty();
		}

		public int[] getPrintNTopWordsInterval() {
			return pc.getPrintNTopWordsInterval();
		}

		public void addNodes(String key, Collection<? extends ConfigurationNode> nodes) {
			pc.addNodes(key, nodes);
		}

		public int getPrintNTopWords() {
			return pc.getPrintNTopWords();
		}

		public int getProportionalTopicIndexBuilderSkipStep() {
			return pc.getProportionalTopicIndexBuilderSkipStep();
		}

		public ConfigurationInterpolator getInterpolator() {
			return pc.getInterpolator();
		}

		public ExpressionEngine getExpressionEngine() {
			return pc.getExpressionEngine();
		}

		public void setExpressionEngine(ExpressionEngine expressionEngine) {
			pc.setExpressionEngine(expressionEngine);
		}

		public void load(Reader reader) throws ConfigurationException {
			pc.load(reader);
		}

		public Log getLogger() {
			return pc.getLogger();
		}

		public void setLogger(Log log) {
			pc.setLogger(log);
		}

		public void configurationChanged(ConfigurationEvent event) {
			pc.configurationChanged(event);
		}

		public void addErrorLogListener() {
			pc.addErrorLogListener();
		}

		public void configurationError(ConfigurationErrorEvent event) {
			pc.configurationError(event);
		}

		public void addProperty(String key, Object value) {
			pc.addProperty(key, value);
		}

		public void setFileSystem(FileSystem fileSystem) {
			pc.setFileSystem(fileSystem);
		}

		public void resetFileSystem() {
			pc.resetFileSystem();
		}

		public FileSystem getFileSystem() {
			return pc.getFileSystem();
		}

		public Configuration subset(String prefix) {
			return pc.subset(prefix);
		}

		public SubnodeConfiguration configurationAt(String key, boolean supportUpdates) {
			return pc.configurationAt(key, supportUpdates);
		}

		public Properties getProperties(String key) {
			return pc.getProperties(key);
		}

		public Properties getProperties(String key, Properties defaults) {
			return pc.getProperties(key, defaults);
		}

		public boolean getBoolean(String key) {
			return pc.getBoolean(key);
		}

		public boolean getBoolean(String key, boolean defaultValue) {
			return pc.getBoolean(key, defaultValue);
		}

		public Boolean getBoolean(String key, Boolean defaultValue) {
			return pc.getBoolean(key, defaultValue);
		}

		public SubnodeConfiguration configurationAt(String key) {
			return pc.configurationAt(key);
		}

		public List<HierarchicalConfiguration> configurationsAt(String key) {
			return pc.configurationsAt(key);
		}

		public byte getByte(String key) {
			return pc.getByte(key);
		}

		public byte getByte(String key, byte defaultValue) {
			return pc.getByte(key, defaultValue);
		}

		public Byte getByte(String key, Byte defaultValue) {
			return pc.getByte(key, defaultValue);
		}

		public double getDouble(String key) {
			return pc.getDouble(key);
		}

		public Set<String> getSections() {
			return pc.getSections();
		}

		public double getDouble(String key, double defaultValue) {
			return pc.getDouble(key, defaultValue);
		}

		public SubnodeConfiguration getSection(String name) {
			return pc.getSection(name);
		}

		public float getFloat(String key) {
			return pc.getFloat(key);
		}

		public float getFloat(String key, float defaultValue) {
			return pc.getFloat(key, defaultValue);
		}

		public Float getFloat(String key, Float defaultValue) {
			return pc.getFloat(key, defaultValue);
		}

		public int getInt(String key) {
			return pc.getInt(key);
		}

		public int getInt(String key, int defaultValue) {
			return pc.getInt(key, defaultValue);
		}

		public long getLong(String key) {
			return pc.getLong(key);
		}

		public long getLong(String key, long defaultValue) {
			return pc.getLong(key, defaultValue);
		}

		public Long getLong(String key, Long defaultValue) {
			return pc.getLong(key, defaultValue);
		}

		public short getShort(String key) {
			return pc.getShort(key);
		}

		public short getShort(String key, short defaultValue) {
			return pc.getShort(key, defaultValue);
		}

		public Short getShort(String key, Short defaultValue) {
			return pc.getShort(key, defaultValue);
		}

		public void clear() {
			pc.clear();
		}

		public BigDecimal getBigDecimal(String key) {
			return pc.getBigDecimal(key);
		}

		public BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
			return pc.getBigDecimal(key, defaultValue);
		}

		public BigInteger getBigInteger(String key) {
			return pc.getBigInteger(key);
		}

		public BigInteger getBigInteger(String key, BigInteger defaultValue) {
			return pc.getBigInteger(key, defaultValue);
		}

		public String getString(String key) {
			return pc.getString(key);
		}

		public String getString(String key, String defaultValue) {
			return pc.getString(key, defaultValue);
		}

		public String[] getStringArray(String key) {
			return pc.getStringArray(key);
		}

		public int getMaxIndex(String key) {
			return pc.getMaxIndex(key);
		}

		public Configuration interpolatedConfiguration() {
			return pc.interpolatedConfiguration();
		}

		public List<Object> getList(String key) {
			return pc.getList(key);
		}

		public List<Object> getList(String key, List<Object> defaultValue) {
			return pc.getList(key, defaultValue);
		}

		public void copy(Configuration c) {
			pc.copy(c);
		}

		public void append(Configuration c) {
			pc.append(c);
		}

		@Override
		public boolean logTypeTopicDensity(boolean logTypeTopicDensityDefault) {
			return logTypeTopicDensityDefault;
		}

		@Override
		public boolean logDocumentDensity(boolean logDocumentDensityDefault) {
			return logDocumentDensityDefault;
		}

		@Override
		public String getExperimentOutputDirectory(String defaultDir) {
			return defaultDir;
		}
		
		@Override
		public double getVariableSelectionPrior(double vsPriorDefault) {
			return vsPriorDefault;
		}

		@Override
		public boolean logPhiDensity(String logPhiDensityDefault) {
			return false;
		}

		@Override
		public String getTopicPriorFilename() {
			return pc.getTopicPriorFilename();
		}

		@Override
		public String getStoplistFilename(String string) {
			return pc.getStoplistFilename(string);
		}

		@Override
		public boolean keepNumbers() {
			return pc.keepNumbers();
		}

		@Override
		public boolean saveDocumentTopicMeans() {
			return pc.saveDocumentTopicMeans();
		}

		@Override
		public String getDocumentTopicMeansOutputFilename() {
			return pc.getDocumentTopicMeansOutputFilename();
		}

		@Override
		public String getDocumentTopicThetaOutputFilename() {
			return pc.getDocumentTopicThetaOutputFilename();
		}

		@Override
		public String getPhiMeansOutputFilename() {
			return pc.getPhiMeansOutputFilename();
		}

		@Override
		public boolean savePhiMeans(boolean defaultValue) {
			return false;
		}

		@Override
		public int getPhiBurnInPercent(int phiBurnInDefault) {
			return 0;
		}

		@Override
		public int getPhiMeanThin(int phiMeanThinDefault) {
			return 0;
		}

		@Override
		public Integer getTfIdfVocabSize(int defaultValue) {
			return pc.getTfIdfVocabSize(defaultValue);
		}

		@Override
		public int getNrTopWords(int defaltNr) {
			return pc.getNrTopWords(defaltNr);
		}

		@Override
		public int getMaxDocumentBufferSize(int defaltSize) {
			return pc.getMaxDocumentBufferSize(defaltSize);
		}

		@Override
		public boolean getKeepConnectingPunctuation(boolean defaultKeepConnectingPunctuation) {
			return false;
		}

		@Override
		public boolean saveVocabulary(boolean defaultVal) {
			return false;
		}

		@Override
		public String getVocabularyFilename() {
			return pc.getVocabularyFilename();
		}

		@Override
		public boolean saveTermFrequencies(boolean defaultValue) {
			return defaultValue;
		}

		@Override
		public String getTermFrequencyFilename() {
			return pc.getTermFrequencyFilename();
		}

		@Override
		public boolean saveDocLengths(boolean defaultValue) {
			return defaultValue;
		}

		@Override
		public String getDocLengthsFilename() {
			return pc.getDocLengthsFilename();
		}

		@Override
		public double getLambda(double lambdaDefault) {
			return 0.6;
		}

		@Override
		public boolean saveDocumentThetaEstimate() {
			return pc.saveDocumentThetaEstimate();
		}

		@Override
		public String getDirichletSamplerBuilderClass(String defaultName) {
			return pc.getDirichletSamplerBuilderClass(defaultName);
		}

		@Override
		public int getAliasPoissonThreshold(int aliasPoissonDefaultThreshold) {
			return pc.getAliasPoissonThreshold(aliasPoissonDefaultThreshold);
		}

		@Override
		public String getFileRegex(String string) {
			return pc.getFileRegex(string);
		}

		@Override
		public boolean saveDocumentTopicDiagnostics() {
			return pc.saveDocumentTopicDiagnostics();
		}

		@Override
		public String getDocumentTopicDiagnosticsOutputFilename() {
			return pc.getDocumentTopicDiagnosticsOutputFilename();
		}
		
		public Integer getHyperparamOptimInterval(int defaultValue) {
			return pc.getHyperparamOptimInterval(defaultValue);
		}

		@Override
		public boolean useSymmetricAlpha(boolean symmetricAlpha) {
			return pc.useSymmetricAlpha(symmetricAlpha);
		}

		@Override
		public double getHDPGamma(double gammaDefault) {
			return pc.getHDPGamma(gammaDefault);
		}

		@Override
		public int getHDPNrStartTopics(int defaultValue) {
			return pc.getHDPNrStartTopics(defaultValue);
		}

		@Override
		public boolean logTokensPerTopic(boolean logTokensPerTopic) {
			return pc.logTokensPerTopic(logTokensPerTopic);
		}

		@Override
		public int getDocumentSamplerSplitLimit(int documentSamplerSplitLimitDefault) {
			return pc.getDocumentSamplerSplitLimit(documentSamplerSplitLimitDefault);
		}

		@Override
		public double getHDPKPercentile(double hdpKPercentile) {
			return pc.getHDPKPercentile(hdpKPercentile);
		}

		@Override
		public boolean saveCorpus(boolean b) {
			return pc.saveCorpus(b);
		}

		@Override
		public String getCorpusFilename() {
			return pc.getCorpusFilename();
		}

		@Override
		public boolean logTopicIndicators(boolean b) {
			return pc.logTopicIndicators(b);
		}

	}
}
