package cc.mallet.topics;

import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntIntProcedure;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.topics.SpaliasUncollapsedParallelLDA.TableBuildResult;
import cc.mallet.topics.randomscan.document.BatchBuilderFactory;
import cc.mallet.topics.randomscan.document.DocumentBatchBuilder;
import cc.mallet.topics.randomscan.topic.TopicIndexBuilder;
import cc.mallet.topics.randomscan.topic.TopicIndexBuilderFactory;
import cc.mallet.types.Dirichlet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.IDSorter;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelSequence;
import cc.mallet.types.SparseDirichlet;
import cc.mallet.util.IndexSorter;
import cc.mallet.util.LDAThreadFactory;
import cc.mallet.util.LoggingUtils;
import cc.mallet.util.OptimizedGentleAliasMethod;
import cc.mallet.util.WalkerAliasTable;


public class CollapsedLightLDA extends ModifiedSimpleLDA implements LDAGibbsSampler {

	private static final long serialVersionUID = 1L;
	
	DocumentBatchBuilder bb;

	protected int[] batchIndexes;

	boolean measureTimings = false;

	int [] deltaNInterval;
	String dNOutputFn;
	DataOutputStream deltaOutput;

	BlockingQueue<Integer> samplingResults = new LinkedBlockingQueue<Integer>();
	TIntIntHashMap [] globalDeltaNUpdates;

	AtomicInteger [][] batchLocalTopicTypeUpdates;

	int corpusWordCount = 0;

	// Matrix M of topic-token assignments
	// We keep this since we often want fast access to a whole topic
	protected int [][] topicTypeCountMapping;
	protected boolean	debug;
	private ForkJoinPool documentSamplerPool;
	private ExecutorService	topicUpdaters;

	protected TopicIndexBuilder topicIndexBuilder;

	// Used for inefficiency calculations
	protected int [][] topIndices = null;

	AtomicInteger kdDensities = new AtomicInteger();
	long [] zTimings;
	long [] countTimings;

	SparseDirichlet dirichletSampler;
	
	private ExecutorService tableBuilderExecutor;
	protected TableBuilderFactory tbFactory = new TypeTopicTableBuilderFactory();
	
	WalkerAliasTable [] aliasTables; 
	double [] typeNorm; 

	// Sparse matrix structure (Global)
	// Only used in generating sparse Alias tables 
	// TODO: Is nonZeroTypeTopics really needed?
	int [][] nonZeroTypeTopics = new int[numTypes][numTopics];
	// So we can map back from a topic to where it is in nonZeroTopics vector
	int [][] sparseAliasBackMapping = new int[numTypes][numTopics];
	// Sparse global topic counts used to identify positions in nonZeroTypeTopics
	int[] nonZeroTypeTopicCnt = new int[numTypes];
	
	public CollapsedLightLDA(LDAConfiguration config) {
		super(config);

		documentSamplerPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
		
		// With job stealing we can only have one global z / counts timing
		zTimings = new long[1];
		countTimings = new long[1];

		debug = config.getDebug();
		this.batchIndexes = new int[config.getNoBatches(LDAConfiguration.NO_BATCHES_DEFAULT)];
		for (int bb = 0; bb < config.getNoBatches(LDAConfiguration.NO_BATCHES_DEFAULT); bb++) batchIndexes[bb] = bb;

		startupThreadPools();

		measureTimings = config.getMeasureTiming();

		int  [] defaultVal = {-1};
		deltaNInterval = config.getIntArrayProperty("dn_diagnostic_interval",defaultVal);
		if(deltaNInterval.length > 1) {
			dNOutputFn = LoggingUtils.checkCreateAndCreateDir(config.getLoggingUtil().getLogDir().getAbsolutePath() 
					+ "/delta_n").getAbsolutePath();
			dNOutputFn += "/DeltaNs" + "_noDocs_" + data.size() + "_vocab_" 
					+ numTypes + "_iter_" + currentIteration + ".BINARY";
			try {
				deltaOutput = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(dNOutputFn)));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				throw new IllegalArgumentException(e);
			}
		}

		globalDeltaNUpdates = new TIntIntHashMap[numTopics];
		for (int i = 0; i < globalDeltaNUpdates.length; i++) {
			globalDeltaNUpdates[i] = new TIntIntHashMap();
		}
	}

	public int[][] getTopIndices() {
		return topIndices;
	}

	@Override	
	public int[] getTopicTotals() { return tokensPerTopic; }

	@Override 
	public int getCorpusSize() { return corpusWordCount;	}


	@Override
	public int[][] getTypeTopicCounts() { 
		int [][] tTCounts = new int[numTypes][numTopics];
		for (int topic = 0; topic < topicTypeCountMapping.length; topic++) {
			for (int type = 0; type < topicTypeCountMapping[topic].length; type++) {
				tTCounts[type][topic] = topicTypeCountMapping[topic][type];
			}
		}
		return tTCounts;	
	}

	public void debugPrintMMatrix() {
		if(debug) {
			printMMatrix();
		}
	}

	public void printMMatrix() {
		int [][] ttCounts = getTypeTopicCounts();
		String res = "Type Topic Counts:\n";
		res += "Topic:   ";
		for (int topic = 0; topic < ttCounts[0].length; topic++) {
			res += String.format("%02d, ",topic);
		}
		res += "\n";
		res += "-----------------------------------------------------------\n";

		for (int i = 0; i < ttCounts.length; i++) {
			res += "[" + String.format("%02d",i) + "=" + alphabet.lookupObject(i) + "]: ";
			for (int j = 0; j < ttCounts[i].length; j++) {
				if(ttCounts[i][j]==0) {
					res += "    ";
				} else {
					res += String.format("%02d, ", ttCounts[i][j]);
				}
			}
			res += "\n";
		}
		System.out.println(res);
	}

	public void printMMatrix(int [][] matrix, String heading) {
		String res = heading + ":\n";
		res += "Topic:   ";
		for (int topic = 0; topic < matrix[0].length; topic++) {
			res += String.format("%02d, ",topic);
		}
		res += "\n";
		res += "-----------------------------------------------------------\n";
		for (int topic = 0; topic < matrix.length; topic++) {
			res += "[" + String.format("%02d",topic) + "=" + alphabet.lookupObject(topic) + "]: ";
			for (int type = 0; type < matrix[topic].length; type++) {
				if(matrix[topic][type]==0) {
					res += "    ";
				} else {
					res += String.format("%02d, ", matrix[topic][type]);
				}
			}
			res += "\n";
		}
		System.out.println(res);
	}


	public void ensureConsistentTopicTypeCountDelta(AtomicInteger [][] counts, int batch) {
		int sumtotal = 0;
		//int deltacount = 0;
		for (AtomicInteger [] topic : counts ) {
			for (int type = 0; type < topic.length; type++ ) { 
				sumtotal += topic[type].get();
				//if(topic[type]!=0) deltacount++;
			}
		}
		if(sumtotal != 0) {
			//printMMatrix(counts, "Broken Batch:");
			throw new IllegalArgumentException("(Iteration = " + currentIteration + ", Batch = " + batch + ") Delta does not sum to Zero! Sumtotal: " + sumtotal);
		}
	}

	public void printTopicTypeCountDelta(int [][] counts, int batch) {
		int sumtotal = 0;

		for (int [] topic : counts ) {
			for (int type = 0; type < topic.length; type++ ) { 
				sumtotal += topic[type];
			}
		}
		if(sumtotal!=0)
			System.out.println("Batch: " + batch + " Sumtotal:" + sumtotal);
	}


	public void ensureConsistentTopicTypeCounts(int [][] counts) {
		int sumtotal = 0;
		int topicIdx = 0;
		for (int [] topic : counts ) {
			for (int type = 0; type < topic.length; type++ ) { 
				int count = topic[type];
				if(count<0) throw new IllegalArgumentException("Negative topic count! Topic: " 
						+ topicIdx + " has negative count for type: " + type + " count=" + count);
				sumtotal += count;
			}
			topicIdx++;
		}
		if(sumtotal != corpusWordCount) {
			throw new IllegalArgumentException("Count does not sum to nr. types! Sumtotal: " + sumtotal + " no.types: " + corpusWordCount);
		}
		System.out.println("Type Topic count is consistent...");
	}

	public void ensureTTEquals() {
		int sumTTCounts1 = sum(getTypeTopicCounts());
		int sumTTCounts2 = sum(typeTopicCounts);
		int sumTopicTotalCounts = sum(getTopicTotals());

		if(sumTTCounts1 != sumTTCounts2)
			throw new IllegalStateException(currentIteration + ": Type-topic counts does not equals Type-topic count mapping: " + sumTTCounts1 + " != "+ sumTTCounts1);

		if(sumTTCounts1 != sumTopicTotalCounts)
			throw new IllegalStateException(currentIteration + ": Type-topic counts does not equals Topic-total counts: " + sumTTCounts1 + ", "+ sumTTCounts2 + " != " + sumTopicTotalCounts);

	}

	/**
	 * Imports the training instances and initializes the LDA model internals.
	 */
	@Override
	public void addInstances (InstanceList training) {		
		alphabet = training.getDataAlphabet();
		targetAlphabet = training.getTargetAlphabet();
		numTypes = alphabet.size();
		aliasTables = new WalkerAliasTable[numTypes];
		typeNorm    = new double[numTypes];
		typeCounts = new int[numTypes];
		batchLocalTopicTypeUpdates = new AtomicInteger[numTopics][numTypes];
		for (int i = 0; i < batchLocalTopicTypeUpdates.length; i++) {
			for (int j = 0; j < batchLocalTopicTypeUpdates[i].length; j++) {
				batchLocalTopicTypeUpdates[i][j] = new AtomicInteger();
			}
		}
		dirichletSampler = new SparseDirichlet(numTypes,beta);

		betaSum = beta * numTypes;
		topicTypeCountMapping = new int [numTopics][numTypes];
		// Transpose of the above
		typeTopicCounts       = new int [numTypes][numTopics];

		// Looping over the new instances to initialize the topic assignment randomly
		for (Instance instance : training) {
			FeatureSequence tokens = (FeatureSequence) instance.getData();
			int docLength = tokens.size();
			corpusWordCount += docLength;
			LabelSequence topicSequence =
					new LabelSequence(topicAlphabet, new int[ docLength ]);

			int[] topics = topicSequence.getFeatures();
			for (int position = 0; position < docLength; position++) {
				// Sampling a random topic assignment
				int topic = random.nextInt(numTopics);
				topics[position] = topic;

				int type = tokens.getIndexAtPosition(position);
				typeCounts[type] += 1;
				updateTypeTopicCount(type, topic, 1);
			}

			//debugPrintDoc(data.size(),tokens.getFeatures(),topicSequence.getFeatures());
			TopicAssignment t = new TopicAssignment (instance, topicSequence);
			data.add (t);
		}

		typeFrequencyIndex = IndexSorter.getSortedIndices(typeCounts);
		typeFrequencyCumSum = calcTypeFrequencyCumSum(typeFrequencyIndex,typeCounts);

		int [] topicIndices = new int[numTopics];
		for (int i = 0; i < numTopics; i++) {
			topicIndices[i] = i;
		}

		bb = BatchBuilderFactory.get(config, this);
		bb.calculateBatch();
		topicIndexBuilder = TopicIndexBuilderFactory.get(config,this);

		if(logger.getLevel()==Level.INFO) {
			System.out.println("Loaded " + data.size() + " documents, with " + corpusWordCount + " words in total.");
		}
	}

	protected void updateTypeTopicCount(int type, int topic, int count) {
		topicTypeCountMapping[topic][type] += count;
		typeTopicCounts[type][topic] += count;
		tokensPerTopic[topic] += count;
		if(topicTypeCountMapping[topic][type]<0) {
			System.err.println("Emergency print!");
			debugPrintMMatrix();
			throw new IllegalArgumentException("Negative count for topic: " + topic 
					+ "! Count: " + topicTypeCountMapping[topic][type] + " type:" 
					+ alphabet.lookupObject(type) + "(" + type + ") update:" + count);
		}
	}


	private double[] calcTypeFrequencyCumSum(int[] typeFrequencyIndex,int[] typeCounts) {
		double [] result = new double[typeCounts.length];
		result[0] = ((double)typeCounts[typeFrequencyIndex[0]]) / corpusWordCount;
		for (int i = 1; i < typeFrequencyIndex.length; i++) {
			result[i] = (((double)typeCounts[typeFrequencyIndex[i]]) / corpusWordCount) + result[i-1];
		}
		return result;
	}

	/** Trains the LDA model for the given number of iterations. The training is done by sampling z in parallel, updating
	 * the topic-token counts matrix centrally and building alias tables and iterating
	 * 
	 * @see cc.mallet.topics.ModifiedSimpleLDA#sample(int)
	 */
	@Override
	public void sample (int iterations) throws IOException {
		preSample();

		int [] printFirstNDocs = config.getPrintNDocsInterval();
		int nDocs = config.getPrintNDocs();
		int [] printFirstNTopWords = config.getPrintNTopWordsInterval();
		int nWords = config.getPrintNTopWords();

		int [] defaultVal = {-1};
		int [] output_interval = config.getIntArrayProperty("diagnostic_interval",defaultVal);
		File binOutput = null;
		if(output_interval.length>1||printFirstNDocs.length>1||printFirstNTopWords.length>1) {
			binOutput = LoggingUtils.checkCreateAndCreateDir(config.getLoggingUtil().getLogDir().getAbsolutePath() + "/binaries");
		}

		String loggingPath = config.getLoggingUtil().getLogDir().getAbsolutePath();

		double logLik = modelLogLikelihood();	
		String tw = topWords (wordsPerTopic);
		LogState logState = new LogState(logLik, 0, tw, loggingPath, logger);
		LDAUtils.logLikelihoodToFile(logState);

		boolean logTypeTopicDensity = config.logTypeTopicDensity(LDAConfiguration.LOG_TYPE_TOPIC_DENSITY_DEFAULT);
		boolean logDocumentDensity = config.logDocumentDensity(LDAConfiguration.LOG_DOCUMENT_DENSITY_DEFAULT);
		
		double density;
		double docDensity = -1;
		Stats stats;
		if(logTypeTopicDensity || logDocumentDensity) {
			density = logTypeTopicDensity ? LDAUtils.calculateMatrixDensity(typeTopicCounts) : -1;
			docDensity = kdDensities.get() / (double) numTopics / numTypes;
			stats = new Stats(0, loggingPath, System.currentTimeMillis(), 0, 0, density, docDensity, zTimings, countTimings, -1);
			LDAUtils.logStatstHeaderToFile(stats);
			LDAUtils.logStatsToFile(stats);
		}

		for (int iteration = 1; iteration <= iterations && !abort; iteration++) {
			preIteration();
			currentIteration = iteration;
			//if((iteration%100)==0) System.out.println("Iteration: " + iteration);
			// Saves timestamp
			long iterationStart = System.currentTimeMillis();
			for (int i = 0; i < zTimings.length; i++) {
				zTimings[i] = iterationStart;
			}

			// Sample z by dividing the corpus in batches
			preZ();
			loopOverBatches();

			long beforeSync = System.currentTimeMillis();
			try {
				updateCounts();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			postZ();
			long endTypeTopicUpdate = System.currentTimeMillis();
			long zSamplingTokenUpdateTime = endTypeTopicUpdate - iterationStart;
			logger.fine("Time for updating type-topic counts: " + 
					(endTypeTopicUpdate - beforeSync) + "ms\t");

			long elapsedMillis = System.currentTimeMillis();

			if(output_interval.length == 2 && iteration >= output_interval[0] && iteration <= output_interval[1]) {
				LDAUtils.writeBinaryIntMatrix(typeTopicCounts, iteration, numTypes, numTopics, binOutput.getAbsolutePath() + "/N");
				LDAUtils.writeBinaryIntMatrix(LDAUtils.getDocumentTopicCounts(data, numTopics), iteration, data.size(), numTopics, binOutput.getAbsolutePath() + "/M");
			}

			logger.fine("\nIteration " + iteration + "\tTotal time: " + elapsedMillis + "ms\t");
			logger.fine("--------------------");

			// Occasionally print more information
			if (showTopicsInterval > 0 && iteration % showTopicsInterval == 0) {
				if(testSet != null) {
					System.err.println("SHOULD PRINT PERPLEXITY!!!");
				}

				logLik = modelLogLikelihood();	
				tw = topWords (wordsPerTopic);
				logState = new LogState(logLik, iteration, tw, loggingPath, logger);
				LDAUtils.logLikelihoodToFile(logState);
				System.err.println("<" + iteration + "> Log Likelihood: " + logLik);
				logger.info(tw);
				if(logTypeTopicDensity || logDocumentDensity) {
					density = logTypeTopicDensity ? LDAUtils.calculateMatrixDensity(typeTopicCounts) : -1;
					docDensity = kdDensities.get() / (double) numTopics / numTypes;
					stats = new Stats(iteration, loggingPath, elapsedMillis, zSamplingTokenUpdateTime, -1, 
							density, docDensity, zTimings, countTimings,-1);
					LDAUtils.logStatsToFile(stats);
				}
			}

			if( printFirstNDocs.length > 1 && LDAUtils.inRangeInterval(iteration, printFirstNDocs)) {
				int [][] docTopicCounts = LDAUtils.getDocumentTopicCounts(data, numTopics, nDocs);
				double [][] theta = LDAUtils.drawDirichlets(docTopicCounts);
				LDAUtils.writeBinaryDoubleMatrix(theta, iteration, binOutput.getAbsolutePath() + "/Theta_DxK");				
			}
			if( printFirstNTopWords.length > 1 && LDAUtils.inRangeInterval(iteration, printFirstNTopWords)) {
				// Assign these once
				if(topIndices==null) {
					topIndices = LDAUtils.getTopWordIndices(nWords, numTypes, numTopics, typeTopicCounts, alphabet);
				}
			}

			kdDensities.set(0);

			postIteration();
			//long iterEnd = System.currentTimeMillis();
			//System.out.println("Iteration "+ currentIteration + " took: " + (iterEnd-iterStart) + " milliseconds...");
		}

		postSample();

	}

	class TypeTopicTableBuilderFactory implements TableBuilderFactory {
		public Callable<TableBuildResult> instance(int type) {
			return new TypeTopicParallelTableBuilder(type);
		}
	}

	class TypeTopicParallelTableBuilder implements Callable<TableBuildResult> {
		int type;
		public TypeTopicParallelTableBuilder(int type) {
			this.type = type;
		}
		@Override
		public TableBuildResult call() {
			// TODO: I dont know to fix this, nonZeroTypeTopicCnt should be a variable that is reached.
			double [] probs = new double[nonZeroTypeTopicCnt[type]];
			// int [] myTypeTopicCounts = typeTopicCounts[type];
			int [] myNonZeroTypeTopics = nonZeroTypeTopics[type];
			
			// for (int i = 0; i < myTypeTopicCounts.length; i++) {
			// 	  topicMass += myTypeTopicCounts[i];
			//}
			
			// Iterate over nonzero topic indicators
			int topicMass = 0;
			for (int i = 0; i < myNonZeroTypeTopics.length; i++) {
			 	  topicMass += typeTopicCounts[type][myNonZeroTypeTopics[i]];
			}

			// for (int i = 0; i < myTypeTopicCounts.length; i++) {
			// 	typeMass += probs[i] = myTypeTopicCounts[i] / (double) topicMass;
			// }
			
			double typeMass = 0; // Type prior mass
			for (int i = 0; i < myNonZeroTypeTopics.length; i++) {
				typeMass += probs[i] = typeTopicCounts[type][myNonZeroTypeTopics[i]] / (double) topicMass;
			}

			/*
			if(aliasTables[type]==null) {
				aliasTables[type] = new OptimizedGentleAliasMethod(probs,typeMass);
			} else {
				aliasTables[type].reGenerateAliasTable(probs, typeMass);
			}
			*/
			
			// TODO: New tables in all iterations (different sizes)?
			// TODO: I now assume that the aliasTable return the position in prob.
			aliasTables[type] = new OptimizedGentleAliasMethod(probs,typeMass);
			
			return new TableBuildResult(type, aliasTables[type], typeMass);
		}   
	}
	
	public void preIteration() {
		
		List<Callable<TableBuildResult>> builders = new ArrayList<>();
		final int [][] topicTypeIndices = topicIndexBuilder.getTopicTypeIndices();
		if(topicTypeIndices!=null) {
			// The topicIndexBuilder supports having different types per topic,
			// this is currently not used, so we can just pick the first topic
			// since it will be the same for all topics
			int [] typesToSample = topicTypeIndices[0];
			for (int typeIdx = 0; typeIdx < typesToSample.length; typeIdx++) {
				builders.add(tbFactory.instance(typesToSample[typeIdx]));
			}
			// if the topicIndexBuilder returns null it means sample ALL types
		} else {
			for (int type = 0; type < numTypes; type++) {
				builders.add(tbFactory.instance(type));
			}
		}
		
		List<Future<TableBuildResult>> results;
		try {
			results = tableBuilderExecutor.invokeAll(builders);
			for (Future<TableBuildResult> result : results) {
				aliasTables[result.get().type] = result.get().table;
				typeNorm[result.get().type] = result.get().typeNorm; // typeNorm is sigma_prior
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (ExecutionException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	@Override
	public void postSample() {
		// By now we don't need the thread pools any more
		shutdownThreadPools();
		tableBuilderExecutor.shutdown();
		flushDeltaOut();
	}

	void shutdownThreadPools() {
		documentSamplerPool.shutdown();
		try {
			documentSamplerPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		}
		catch (InterruptedException ex) {}

		topicUpdaters.shutdown();
		try {
			topicUpdaters.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		}
		catch (InterruptedException ex) {}
	}

	@Override
	public void preSample() {
		int  [] defaultVal = {-1};
		deltaNInterval = config.getIntArrayProperty("dn_diagnostic_interval", defaultVal);
		if(deltaNInterval.length > 1) {
			dNOutputFn = LoggingUtils.checkCreateAndCreateDir(config.getLoggingUtil().getLogDir().getAbsolutePath() 
					+ "/delta_n").getAbsolutePath();
			dNOutputFn += "/DeltaNs" + "_noDocs_" + data.size() + "_vocab_" 
					+ numTypes + "_iter_" + currentIteration + ".BINARY";
			try {
				deltaOutput = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(dNOutputFn)));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				throw new IllegalArgumentException(e);
			}
		}
		startupThreadPools();
		int poolSize = 2;
		tableBuilderExecutor = Executors.newFixedThreadPool(Math.max(1, poolSize));
	}

	void startupThreadPools() {
		// If we call sample again the thread pool have been shutdown so we create a new one
		if(documentSamplerPool == null || documentSamplerPool.isShutdown()) {
			//documentSamplerPool = Executors.newFixedThreadPool(noBatches, new LDAThreadFactory("DocumentSampler"));
			documentSamplerPool = new ForkJoinPool();
		}
		if(topicUpdaters == null || topicUpdaters.isShutdown()) {
			topicUpdaters = Executors.newFixedThreadPool(2,new LDAThreadFactory("TopicUpdater"));
		}
	}

	/**
	 * Returns if 'iter' is in any of the intervals specified by intervals
	 *
	 * @param iter The iteration to check if interval.
	 * @param intervals An integer array of even length.
	 */
	public boolean iterationInInterval(int iter, int[] intervals) {		
		if(intervals.length == 1) return false;
		if(intervals.length % 2 != 0) throw new IllegalArgumentException();

		for(int i = 0; i < intervals.length / 2; i++){
			if(iter >= intervals[2 * i] && iter <= intervals[2 * i + 1]){
				return true;
			}
		}
		return false;
	}	

	protected void updateCounts() throws InterruptedException {
		// Puts together changes to the type-topic counts matrix, by looping over the collection
		// of updates data structures

		// First empty the updates from previous run
		for (int i = 0; i < globalDeltaNUpdates.length; i++) {
			globalDeltaNUpdates[i].clear();
		}

		if(iterationInInterval(currentIteration, deltaNInterval)) {
			flushDeltaOut();
			dNOutputFn = LoggingUtils.checkCreateAndCreateDir(config.getLoggingUtil().getLogDir().getAbsolutePath() 
					+ "/delta_n").getAbsolutePath();
			dNOutputFn += "/DeltaNs" + "_noDocs_" + data.size() + "_vocab_" 
					+ numTypes + "_iter_" + currentIteration + ".BINARY";
			try {
				deltaOutput = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(dNOutputFn)));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				throw new IllegalArgumentException(e);
			}
		}

		long zFin = System.currentTimeMillis();
		zTimings[0] =  zFin - zTimings[0];
		updateTopics();
		countTimings[0] = System.currentTimeMillis() - zFin;

		if(iterationInInterval(currentIteration, deltaNInterval)) {
			flushDeltaOut();
		}
	}

	void flushDeltaOut() {
		if(deltaOutput!=null) {
			try {
				deltaOutput.flush();
				deltaOutput.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}

	class ParallelTopicUpdater implements Callable<Long> {
		int topic;
		public ParallelTopicUpdater(int topic) {
			this.topic = topic;
		}
		@Override
		public Long call() {
			long updates = 0;
			for (int type = 0; type < numTypes; type++) {	
				if(batchLocalTopicTypeUpdates[topic][type].get()!=0) {
					updateTypeTopicCount(type, topic, batchLocalTopicTypeUpdates[topic][type].get());
					// Now reset the count
					batchLocalTopicTypeUpdates[topic][type].set(0);		

					// Update delta statistics
					boolean success = globalDeltaNUpdates[topic].increment(type);
					// We need Trove 3.0!!
					if(!success) {
						globalDeltaNUpdates[topic].put(type, 1);
					}

					updates++;
				}
			}
			return updates;
		}   
	}
	
	void updateTopics() {
		List<ParallelTopicUpdater> builders = new ArrayList<>();
		for (int topic = 0; topic < numTopics; topic++) {
			builders.add(new ParallelTopicUpdater(topic));
		}
		List<Future<Long>> results;
		try {
			results = topicUpdaters.invokeAll(builders);
			for (Future<Long> result : results) {
				result.get();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (ExecutionException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	class RecursiveDocumentSampler extends RecursiveAction {
		final static long serialVersionUID = 1L;
		double [][] matrix1;
		double [][] matrix2;
		double [][] resultMatrix;
		int startDoc = -1;
		int endDoc = -1;
		int limit = 100;
		int myBatch = -1;

		public RecursiveDocumentSampler(int startDoc, int endDoc, int batchId, int ll) {
			this.limit = ll;
			this.startDoc = startDoc;
			this.endDoc = endDoc;
			this.myBatch = batchId;
		}

		@Override
		protected void compute() {
			if ( (endDoc-startDoc) <= limit ) {
				for (int docIdx = startDoc; docIdx < endDoc; docIdx++) {
					FeatureSequence tokenSequence =
							(FeatureSequence) data.get(docIdx).instance.getData();
					LabelSequence topicSequence =
							(LabelSequence) data.get(docIdx).topicSequence;
					sampleTopicAssignmentsParallel (new UncollapsedLDADocSamplingContext(tokenSequence, topicSequence, myBatch, docIdx));
				}
			}
			else {
				int range = (endDoc-startDoc);
				int startDoc1 = startDoc;
				int endDoc1 = startDoc + (range / 2);
				int startDoc2 = endDoc1+1;
				int endDoc2 = endDoc;
				invokeAll(new RecursiveDocumentSampler(startDoc1,endDoc1,myBatch + 1,limit),
						new RecursiveDocumentSampler(startDoc2,endDoc2,myBatch + 2,limit));
			}
		}
	}

	/*
	class DocumentSampler extends Thread {
		int [] idxs;
		int myBatch;

		public DocumentSampler(int [] docIndices, int myBatch) {
			this.idxs = docIndices;
			this.myBatch = myBatch;
		}

		public void run() {
			try {
				for(int docIdx : idxs) {
					FeatureSequence tokenSequence =
							(FeatureSequence) data.get(docIdx).instance.getData();
					LabelSequence topicSequence =
							(LabelSequence) data.get(docIdx).topicSequence;
					sampleTopicAssignmentsParallel (tokenSequence, topicSequence, myBatch);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			try {
				samplingResults.put(myBatch);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}*/

	protected void loopOverBatches() {
		RecursiveDocumentSampler dslr = new RecursiveDocumentSampler(0,data.size(),0,200);                
		documentSamplerPool.invoke(dslr);
	}

	void debugPrintDoc(int doc, int[] tokSeq, int[] topSeq) {
		if(debug) {
			printDoc(doc,tokSeq,topSeq);
		}
	}

	private void printDoc(int doc, int[] tokSeq, int[] topSeq) {
		if(tokSeq==null) { 
			System.out.println("Token Sequence is null");
			return;
		};
		System.out.print("Doc: " + doc + " Tokens:");
		for (int i = 0; i < tokSeq.length; i++) {
			System.out.print(String.format("%02d, ", tokSeq[i]));
		}
		System.out.println();
		if(topSeq==null) { 
			System.out.println("Token Sequence is null");
			return;
		}
		System.out.print("Doc: " + doc + " Topics:");
		for (int i = 0; i < topSeq.length; i++) {
			System.out.print(String.format("%02d, ",topSeq[i]));
		}
		System.out.println();System.out.println();
	}

	protected void sampleTopicAssignmentsParallel(LDADocSamplingContext ctx) {
		FeatureSequence tokens = ctx.getTokens();
		LabelSequence topics = ctx.getTopics();
		int myBatch = ctx.getMyBatch();

		final int docLength = tokens.getLength();
		if(docLength==0) return;
		
		int [] tokenSequence = tokens.getFeatures();
		int [] oneDocTopics = topics.getFeatures();

		double[] localTopicCounts = new double[numTopics];
		double[] localTopicCounts_i = new double[numTopics];
		
		// Populate topic counts
		int nonZeroTopicCnt = 0; // Only needed for statistics
		for (int position = 0; position < docLength; position++) {
			int topicInd = oneDocTopics[position];
			localTopicCounts[topicInd]++;
			localTopicCounts_i[topicInd]++;
			if(localTopicCounts[topicInd]==1) nonZeroTopicCnt++;
		}
		
		kdDensities.addAndGet(nonZeroTopicCnt);
		
		// Cashed values
		// TODO: Is this the correct way?
		double beta_bar = beta * typeTopicCounts.length; // beta * V (beta_bar in article)
		
		//	Iterate over the words in the document
		for (int position = 0; position < docLength; position++) {
			int type = tokenSequence[position];
			int oldTopic = oneDocTopics[position]; // z_position
			int newTopic = oldTopic;
			
			if(localTopicCounts[oldTopic]<0) 
				throw new IllegalStateException("LightPC-LDA: Counts cannot be negative! Count for topic:" 
						+ oldTopic + " is: " + localTopicCounts[oldTopic]);

			decrement(myBatch, oldTopic, type);

			// #####################################
			// Word-Topic Proposal 
			// #####################################
			
			// N_{d,Z_i} => # counts of topic Z_i in document d
			// s = old state
			// t = proposed state
			
			localTopicCounts_i[oldTopic]--;
			
			// Draw topic proposal t
			// TODO: I cant find an array that contain the number of tokens per word type? I set this to be called typeTokens, need to be fixed.
			double u_w = ThreadLocalRandom.current().nextDouble() * (typeTokens[type] + (numTopics*beta)); // (n_w + K*beta) * u where u ~ U(0,1)

			int wordTopicIndicatorProposal = -1;
			if(u_w < typeTokens[type]) {
				// TODO: Assuming that the Alias table is returning an index to use in backmapping
				wordTopicIndicatorProposal = nonZeroTypeTopicsBackMapping[type][aliasTables[type].generateSample(u_w)];
			} else {
				wordTopicIndicatorProposal = (int) (((u_w - typeTokens[type]) / (numTopics*beta)) * numTopics); // assume symmetric beta, just draws one topic
			}
						
			// Make sure we actually sampled a valid topic
			if (wordTopicIndicatorProposal < 0 || wordTopicIndicatorProposal > numTopics) {
				throw new IllegalStateException ("Collapsed Light-LDA: New valid topic not sampled (" + newTopic + ").");
			}
			
			
			if(wordTopicIndicatorProposal!=oldTopic) {
				// If we drew a new topic indicator, do MH step for Word proposal
				
				double n_d_s_i = localTopicCounts_i[oldTopic];
				double n_d_t_i = localTopicCounts_i[wordTopicIndicatorProposal];
				double n_w_s = typeTopicCounts[type][oldTopic];
				double n_w_t = typeTopicCounts[type][wordTopicIndicatorProposal];					
				double n_w_s_i = typeTopicCounts[type][oldTopic] - 1.0;
				double n_w_t_i = n_w_t; // Since wordTopicIndicatorProposal!=oldTopic above promise that s!=t and hence n_tw=n_t_i
				double n_t = tokensPerTopic[wordTopicIndicatorProposal]; // Global counts of the number of topic indicators in each topic
				double n_s = tokensPerTopic[oldTopic]; 
				double n_t_i = n_t; 
				double n_s_i = n_s - 1.0; 
				
				// Calculate rejection rate
				// TODO: Is this a correct way of calculate a large product?
				double pi_w = ((alpha + n_d_t_i) / (alpha + n_d_s_i));
				pi_w *= ((beta + n_w_t_i) / (beta + n_w_s_i));
				pi_w *= ((beta_bar + n_s_i) / (beta_bar + n_t_i));
				pi_w *= ((beta + n_w_s) / (beta + n_w_t));
				pi_w *= ((beta + n_t) / (beta + n_s));
				
				if(pi_w > 1){
					localTopicCounts[oldTopic]--;
					localTopicCounts[wordTopicIndicatorProposal]++;
					oldTopic = wordTopicIndicatorProposal;
				} else {
					double u_pi_w = ThreadLocalRandom.current().nextDouble();
					boolean accept_pi_w = u_pi_w < pi_w;

					if(accept_pi_w) {				
						localTopicCounts[oldTopic]--;
						localTopicCounts[wordTopicIndicatorProposal]++;
	
						// Set oldTopic to the new wordTopicIndicatorProposal just accepted.
						// By doing this the below document proposal will be relative to the 
						// new best proposal so far
						oldTopic = wordTopicIndicatorProposal;
						//wordAccepts.incrementAndGet();
					} 
				}

			}
			
			// #####################################
			// Document-Topic Proposal  
			// #####################################
			 
			double u_i = ThreadLocalRandom.current().nextDouble() * (oneDocTopics.length + (numTopics*alpha)); // (n_d + K*alpha) * u where u ~ U(0,1)

			int docTopicIndicatorProposal = -1;
			if(u_i < oneDocTopics.length) {
				docTopicIndicatorProposal = oneDocTopics[(int) u_i];
			} else {
				docTopicIndicatorProposal = (int) (((u_i - oneDocTopics.length) / (numTopics*alpha)) * numTopics); // assume symmetric alpha, just draws one alpha
			}
			
			// Make sure we actually sampled a valid topic
			if (docTopicIndicatorProposal < 0 || docTopicIndicatorProposal > numTopics) {
				throw new IllegalStateException ("Collapsed Light-LDA: New valid topic not sampled (" + newTopic + ").");
			}

			// If we drew a new topic indicator, do MH step for Document proposal
			if(docTopicIndicatorProposal!=oldTopic) {
				double n_d_s = localTopicCounts[oldTopic];
				double n_d_t = localTopicCounts[wordTopicIndicatorProposal];
				double n_d_s_i = localTopicCounts_i[oldTopic];
				double n_d_t_i = localTopicCounts_i[wordTopicIndicatorProposal];
				double n_w_s_i = typeTopicCounts[type][oldTopic] - 1.0;
				double n_w_t_i = typeTopicCounts[type][wordTopicIndicatorProposal]; // Since wordTopicIndicatorProposal!=oldTopic above promise that s!=t and hence n_tw=n_t_i
				double n_t_i = tokensPerTopic[wordTopicIndicatorProposal]; // Global counts of the number of topic indicators in each topic
				double n_s_i = tokensPerTopic[oldTopic] - 1.0; 
				
				// Calculate rejection rate
				// TODO: Is this a correct way of calculate a large product?
				double pi_d = ((alpha + n_d_t_i) / (alpha + n_d_s_i));
				pi_d *= ((beta + n_w_t_i) / (beta + n_w_s_i));
				pi_d *= ((beta_bar + n_s_i) / (beta_bar + n_t_i));
				pi_d *= ((alpha + n_d_s) / (alpha + n_d_t));
				
				// Calculate MH acceptance Min.(1,ratio) but as an if else
				if (pi_d > 1){
					newTopic = docTopicIndicatorProposal;
				} else {
					double u_pi_d = ThreadLocalRandom.current().nextDouble();
					boolean accept_pi_d = u_pi_d < pi_d;
	
					if (accept_pi_d) {
						newTopic = docTopicIndicatorProposal;
						//docAccepts.incrementAndGet();
					} else {
						// We did not accept either word or document proposal 
						// so oldTopic is still the best indicator
						newTopic = oldTopic;
						//oldAccepts.incrementAndGet();
					}	
				}
			}
			increment(myBatch, newTopic, type);

			// Make sure we actually sampled a valid topic
			if (newTopic < 0 || newTopic > numTopics) {
				throw new IllegalStateException ("LightPC-LDA: New valid topic not sampled (" + newTopic + ").");
			}

			// Remove one count from old topic
			localTopicCounts[oldTopic]--;
			// Update the word topic indicator
			oneDocTopics[position] = newTopic;
			// Put that new topic into the counts
			localTopicCounts[newTopic]++;
			// Make sure the "_i" version is also up to date!
			localTopicCounts_i[newTopic]++;
		}
	}

	protected void increment(int myBatch, int newTopic, int type) {
		//TODO: Is tokensPerTopic updated as well? This is now used in the sampler.
		//TODO: This should update nonZeroTypeTopics nonZeroTypeTopicsBackMapping as well.
		//batchLocalTopicTypeUpdates[myBatch][newTopic][type] += 1;
		batchLocalTopicTypeUpdates[newTopic][type].incrementAndGet();
		//System.out.println("(Batch=" + myBatch + ") Incremented: topic=" + newTopic + " type=" + type + " => " + batchLocalTopicUpdates[myBatch][newTopic][type]);		
	}

	protected void decrement(int myBatch, int oldTopic, int type) {
		//TODO: Is tokensPerTopic updated as well? This is now used in the sampler.
		//TODO: This should update nonZeroTypeTopics nonZeroTypeTopicsBackMapping as well.
		//batchLocalTopicTypeUpdates[myBatch][oldTopic][type] -= 1;
		batchLocalTopicTypeUpdates[oldTopic][type].addAndGet(-1);
		//System.out.println("(Batch=" + myBatch + ") Decremented: topic=" + oldTopic + " type=" + type + " => " + batchLocalTopicUpdates[myBatch][oldTopic][type]);
	}

	//	@Override
	//	/* 
	//	 * Uses SimpleLDA logLikelihood calculation
	//	 */
	//	public double modelLogLikelihood() {
	//		// Parent uses typeTopicCounts, fetch these on demand
	//		typeTopicCounts = getTypeTopicCounts();
	//		return super.modelLogLikelihood();
	//	}

	/* 
	 * Uses AD-LDA logLikelihood calculation
	 *  
	 * Here we override SimpleLDA's original likelihood calculation and use the
	 * AD-LDA logLikelihood calculation. 
	 * With this approach all models likelihoods are calculated the same way
	 */
	@Override
	public double modelLogLikelihood() {
		double logLikelihood = 0.0;
		//int nonZeroTopics;

		// The likelihood of the model is a combination of a 
		// Dirichlet-multinomial for the words in each topic
		// and a Dirichlet-multinomial for the topics in each
		// document.

		// The likelihood function of a dirichlet multinomial is
		//	 Gamma( sum_i alpha_i )	 prod_i Gamma( alpha_i + N_i )
		//	prod_i Gamma( alpha_i )	  Gamma( sum_i (alpha_i + N_i) )

		// So the log likelihood is 
		//	logGamma ( sum_i alpha_i ) - logGamma ( sum_i (alpha_i + N_i) ) + 
		//	 sum_i [ logGamma( alpha_i + N_i) - logGamma( alpha_i ) ]

		// Do the documents first

		int[] topicCounts = new int[numTopics];
		double[] topicLogGammas = new double[numTopics];
		int[] docTopics;

		for (int topic=0; topic < numTopics; topic++) {
			topicLogGammas[ topic ] = Dirichlet.logGammaStirling( alpha );
		}

		for (int doc=0; doc < data.size(); doc++) {
			LabelSequence topicSequence =	(LabelSequence) data.get(doc).topicSequence;

			docTopics = topicSequence.getFeatures();

			for (int token=0; token < docTopics.length; token++) {
				topicCounts[ docTopics[token] ]++;
			}

			for (int topic=0; topic < numTopics; topic++) {
				if (topicCounts[topic] > 0) {
					logLikelihood += (Dirichlet.logGammaStirling(alpha + topicCounts[topic]) -
							topicLogGammas[ topic ]);
				}
			}

			// subtract the (count + parameter) sum term
			logLikelihood -= Dirichlet.logGammaStirling(alphaSum + docTopics.length);

			Arrays.fill(topicCounts, 0);
		}

		// add the parameter sum term
		logLikelihood += data.size() * Dirichlet.logGammaStirling(alphaSum);

		// And the topics

		// Count the number of type-topic pairs that are not just (logGamma(beta) - logGamma(beta))
		int nonZeroTypeTopics = 0;

		for (int type=0; type < numTypes; type++) {
			// reuse this array as a pointer

			topicCounts = typeTopicCounts[type];

			for (int topic = 0; topic < numTopics; topic++) {
				if (topicCounts[topic] == 0) { continue; }

				nonZeroTypeTopics++;
				logLikelihood += Dirichlet.logGammaStirling(beta + topicCounts[topic]);

				if (Double.isNaN(logLikelihood)) {
					System.err.println("NaN in log likelihood calculation: " + topicCounts[topic]);
					System.exit(1);
				} 
				else if (Double.isInfinite(logLikelihood)) {
					logger.warning("infinite log likelihood");
					System.exit(1);
				}
			}
		}

		for (int topic=0; topic < numTopics; topic++) {
			logLikelihood -= 
					Dirichlet.logGammaStirling( (beta * numTypes) +
							tokensPerTopic[ topic ] );

			if (Double.isNaN(logLikelihood)) {
				logger.info("NaN after topic " + topic + " " + tokensPerTopic[ topic ]);
				return 0;
			}
			else if (Double.isInfinite(logLikelihood)) {
				logger.info("Infinite value after topic " + topic + " " + tokensPerTopic[ topic ]);
				return 0;
			}

		}

		// logGamma(|V|*beta) for every topic
		logLikelihood += 
				Dirichlet.logGammaStirling(beta * numTypes) * numTopics;

		// logGamma(beta) for all type/topic pairs with non-zero count
		logLikelihood -=
				Dirichlet.logGammaStirling(beta) * nonZeroTypeTopics;

		if (Double.isNaN(logLikelihood)) {
			logger.info("at the end");
		}
		else if (Double.isInfinite(logLikelihood)) {
			logger.info("Infinite value beta " + beta + " * " + numTypes);
			return 0;
		}

		return logLikelihood;
	}

	@Override
	/*
	 * This was copied from SimpleLDA and updated to use the new internal representations
	 * 
	 * @see cc.mallet.topics.SimpleLDA#topWords(int)
	 */
	public String topWords (int numWords) {

		StringBuilder output = new StringBuilder();

		IDSorter[] sortedWords = new IDSorter[numTypes];

		for (int topic = 0; topic < numTopics; topic++) {

			int [] typeMap = topicTypeCountMapping[topic];
			for (int token = 0; token < numTypes; token++) {
				Integer thisCount = typeMap[token];
				sortedWords[token] = new IDSorter(token, (thisCount != null) ? thisCount : 0);
			}

			Arrays.sort(sortedWords);

			output.append(topic + "\t" + tokensPerTopic[topic] + "\t");
			for (int i=0; i < numWords; i++) {
				output.append(alphabet.lookupObject(sortedWords[i].getID()) + " ");
			}
			output.append("\n");
		}

		return output.toString();
	}

	@Override
	public void setConfiguration(LDAConfiguration config) {
		super.setConfiguration(config);	
	}

	public void setZIndicators(int[][] zIndicators) {
		// First reset the counts so new counts are not added to old ones
		batchLocalTopicTypeUpdates = new AtomicInteger[numTopics][numTypes];
		for (int i = 0; i < batchLocalTopicTypeUpdates.length; i++) {
			for (int j = 0; j < batchLocalTopicTypeUpdates[i].length; j++) {
				batchLocalTopicTypeUpdates[i][j] = new AtomicInteger();
			}
		}
		for( int topic = 0; topic < numTopics; topic++) {
			for ( int type = 0; type < numTypes; type++ ) {
				topicTypeCountMapping[topic][type] = 0;
				typeTopicCounts[type][topic] = 0;
			}
			tokensPerTopic[topic] = 0;
		}

		int sumtotal = 0;
		for (int docCnt = 0; docCnt < data.size(); docCnt++) {
			data.get(docCnt).topicSequence = 
					new LabelSequence(topicAlphabet, zIndicators[docCnt]);
			FeatureSequence tokenSequence =
					(FeatureSequence) data.get(docCnt).instance.getData();
			int [] tokens = tokenSequence.getFeatures();
			sumtotal += zIndicators[docCnt].length;
			for (int pos = 0; pos < zIndicators[docCnt].length; pos++) {
				int type = tokens[pos];
				int topic = zIndicators[docCnt][pos];
				updateTypeTopicCount(type, topic, 1);
			}
		}

		if(sumtotal != corpusWordCount) {
			throw new IllegalArgumentException("Count does not sum to nr. types! Sumtotal: " + sumtotal + " no.types: " + corpusWordCount);
		}

		if(logger.getLevel()==Level.INFO) {
			System.out.println("loaded sumtotal: " + sumtotal + " tokens");
		}
		
		int [] topicIndices = new int[numTopics];
		for (int i = 0; i < numTopics; i++) {
			topicIndices[i] = i;
		}
	}

	/**
	 * Return the type indices for non-zero count updates in the last iteration
	 *
	 */
	@Override
	public int[][] getDeltaStatistics() {
		int [][] topicTypeUpdates = new int[numTopics][];
		for (int topic = 0; topic < topicTypeUpdates.length; topic++) {
			topicTypeUpdates[topic] = new int[globalDeltaNUpdates[topic].size()];
			TIntIntHashMap topicUpdates = globalDeltaNUpdates[topic];
			// Remove Zero count updates
			topicUpdates.retainEntries(new TIntIntProcedure() {
				@Override
				public boolean execute(int a, int b) {
					return b>0;
				}
			});
			// now we can get the keys which are the non zero type indices for this topic
			topicTypeUpdates[topic] = topicUpdates.keys();
		}
		return topicTypeUpdates;
	}
	
	protected void initNonZeroTypeTopic() {		
		for (int typeidx = 0; typeidx < numTypes; typeidx++) {
			for (int topicidx = 0; topicidx < numTopics; topicidx++) {
			if(typeTopicCounts[typeidx][topicidx] > 0){
				nonZeroTypeTopicCnt[typeidx] = insertNonZeroTopicTypes(topicidx, nonZeroTypeTopics[typeidx], nonZeroTypeTopicsBackMapping[typeidx], nonZeroTypeTopicCnt[typeidx]);
				}
			}
		}
	}
	
	protected static int insertNonZeroTopicTypes(int newTopic, int[] nonZeroTopics, int[] nonZeroTopicsBackMapping, int nonZeroTopicCnt) {
		// TODO: This is a identical to insert in NZVSSpaliasUncollapsed
		//// We have a new non-zero topic put it in the last empty slot and increase the count
		nonZeroTopics[nonZeroTopicCnt] = newTopic;
		nonZeroTopicsBackMapping[newTopic] = nonZeroTopicCnt;
		return ++nonZeroTopicCnt;
	}
}
