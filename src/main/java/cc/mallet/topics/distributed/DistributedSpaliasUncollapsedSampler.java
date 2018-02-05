package cc.mallet.topics.distributed;

import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntIntProcedure;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.actor.Inbox;
import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.configuration.LDARemoteConfiguration;
import cc.mallet.topics.LDAGibbsSampler;
import cc.mallet.topics.ModifiedSimpleLDA;
import cc.mallet.topics.TopicAssignment;
import cc.mallet.topics.randomscan.document.BatchBuilderFactory;
import cc.mallet.topics.randomscan.document.DocumentBatchBuilder;
import cc.mallet.topics.randomscan.topic.TopicBatchBuilder;
import cc.mallet.topics.randomscan.topic.TopicBatchBuilderFactory;
import cc.mallet.topics.randomscan.topic.TopicIndexBuilder;
import cc.mallet.topics.randomscan.topic.TopicIndexBuilderFactory;
import cc.mallet.types.ConditionalDirichlet;
import cc.mallet.types.Dirichlet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.IDSorter;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelSequence;
import cc.mallet.types.ParallelDirichlet;
import cc.mallet.util.IndexSorter;
import cc.mallet.util.LDAThreadFactory;
import cc.mallet.util.LDAUtils;
import cc.mallet.util.LoggingUtils;


public class DistributedSpaliasUncollapsedSampler extends ModifiedSimpleLDA implements LDAGibbsSampler {

	private static final long serialVersionUID = 1L;
	protected double[][] phi;

	DocumentBatchBuilder bb;
	TopicBatchBuilder tbb;

	protected int[] batchIndexes;

	boolean measureTimings = false;
	// If remotes should send partial results. Used to even out network load
	boolean sendPartials = true;

	int [] deltaNInterval;
	String dNOutputFn;
	DataOutputStream deltaOutput;

	BlockingQueue<Object> topicUpdates = new LinkedBlockingQueue<Object>();
	BlockingQueue<Object> phiSamplings = new LinkedBlockingQueue<Object>();
	TIntIntHashMap [] globalDeltaNUpdates;

	int corpusWordCount = 0;

	// Matrix M of topic-token assignments
	protected int [][] topicTypeCountMapping;
	//private Integer	noBatches;
	private Integer	noTopicBatches;
	private boolean	debug;
	private ExecutorService	phiSamplePool;
	private ExecutorService	topicUpdaters;
	Object [] topicLocks;

	private int numActiveWorkers;
	private int workersFinished;
	private TopicIndexBuilder topicIndexBuilder;

	// Used for inefficiency calculations
	protected int [][] topIndices = null;
	
	List<ActorRef> samplerNodes;
	List<ActorRef> samplerCores;
	Map<ActorRef,Integer> nodeCoreMapping;
	Inbox inbox;
	int REMOTE_BATCH_TIMEOUT_MINUTES = 60;
	int REMOTE_DOCUMENT_TIMEOUT_MINUTES = 60;

	public DistributedSpaliasUncollapsedSampler(LDAConfiguration config, List<ActorRef> nodes, List<ActorRef> cores, 
			Map<ActorRef,Integer> nodeCoreMapping, Inbox inbox) {		
		super(config);
		
		this.inbox = inbox;
		this.samplerNodes = nodes;
		this.samplerCores = cores;
		this.nodeCoreMapping = nodeCoreMapping;
		Boolean sp = ((LDARemoteConfiguration)config).getSendPartials();
		// If not set, use true as default, else us value from config
		this.sendPartials = sp == null ? true : sp;
		
		//noBatches = config.getNoBatches(LDAConfiguration.NO_BATCHES_DEFAULT);
		// Cannot have more batches than sampler nodes
		noTopicBatches = Math.min(samplerNodes.size(),config.getNoTopicBatches(LDAConfiguration.NO_TOPIC_BATCHES_DEFAULT));
		//vocabMapping = new int [noBatches][];
		//docVocabMapping = new int [noBatches][];

		debug = config.getDebug();
		//Cannot have more batches than sampler nodes
		Integer noBatches = Math.min(samplerNodes.size(),config.getNoBatches(LDAConfiguration.NO_BATCHES_DEFAULT));
		if(samplerNodes.size() < config.getNoBatches(LDAConfiguration.NO_BATCHES_DEFAULT)) {
			System.err.println("WARNING: DistributedSpaliasUncollapsedSampler(): Requested no. batches is bigger than available no. samplers. Setting no. batches to no. available samplers!");
		}
		this.batchIndexes = new int[noBatches];
		for (int bb = 0; bb < noBatches; bb++) batchIndexes[bb] = bb;

		startupThreadPools();

		topicLocks = new Object[numTopics];
		for (int i = 0; i < numTopics; i++) {
			topicLocks[i] = new Object();
		}

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


	public void ensureConsistentTopicTypeCountDelta(int [][] counts, int batch) {
		int sumtotal = 0;
		//int deltacount = 0;
		for (int [] topic : counts ) {
			for (int type = 0; type < topic.length; type++ ) { 
				sumtotal += topic[type];
				//if(topic[type]!=0) deltacount++;
			}
		}
		if(sumtotal != 0) {
			//printMMatrix(counts, "Broken Batch:");
			throw new IllegalArgumentException("(Iteration = " + currentIteration + ", Batch = " + batch + ") Delta does not sum to Zero! Sumtotal: " + sumtotal);
		}
		//if(deltacount==0)
		//	throw new IllegalArgumentException("(Iteration = " + currentIteration + ", Batch = " + batch + ") Deltacount was Zero!");

		//System.out.println("(Iteration = " + currentIteration + ", Batch = " + batch + ") Deltacount = " + deltacount + " Sumtotal: " + sumtotal);
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
		ensureConsistentTopicTypeCounts(counts,true);
	}

	public void ensureConsistentTopicTypeCounts(int [][] counts, boolean beVerbose) {
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
		if(beVerbose) System.out.println("Type Topic count is consistent...");
	}
	

	/**
	 * Imports the training instances and initializes the LDA model internals.
	 */
	@Override
	public void addInstances (InstanceList training) {
		alphabet = training.getDataAlphabet();
		numTypes = alphabet.size();
		typeCounts = new int[numTypes];

		// Initializing fields needed to sample phi
		betaSum = beta * numTypes;
		topicTypeCountMapping = new int [numTopics][numTypes];
		// Transpose of the above
		typeTopicCounts       = new int [numTypes][numTopics];
		//topicTypeCounts.trimToSize();

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
			//System.out.println("Added doc:");
			//debugPrintDoc(data.size(),tokens.getFeatures(),topicSequence.getFeatures());
			TopicAssignment t = new TopicAssignment (instance, topicSequence);
			data.add (t);
		}

		typeFrequencyIndex = IndexSorter.getSortedIndices(typeCounts);
		typeFrequencyCumSum = calcTypeFrequencyCumSum(typeFrequencyIndex,typeCounts);

		// Initialize the distribution of words in topics, phi, to the prior value
		phi = new double[numTopics][numTypes];
		// Sample up the initial Phi Matrix according to random initialization
		int [] topicIndices = new int[numTopics];
		for (int i = 0; i < numTopics; i++) {
			topicIndices[i] = i;
		}
		loopOverTopics(topicIndices, phi);

		bb = BatchBuilderFactory.get(config, this);
		tbb = TopicBatchBuilderFactory.get(config, this);
		topicIndexBuilder = TopicIndexBuilderFactory.get(config,this);

		System.out.println("Loaded " + data.size() + " documents, with " + corpusWordCount + " words in total.");
	}

	protected void updateTypeTopicCount(int type, int topic, int count) {
		topicTypeCountMapping[topic][type] += count;
		typeTopicCounts[type][topic] += count;
		tokensPerTopic[topic] += count;
		// We allow partial results in this sampler so we 
		// ensure consistent type type-topic counts after each iteration instead
		/*if(topicTypeCountMapping[topic][type]<0) {
			System.out.println("Emergency print!");
			debugPrintMMatrix();
			throw new IllegalArgumentException("Negative count for topic: " + topic 
					+ "! Count: " + topicTypeCountMapping[topic][type] + " type:" 
					+ alphabet.lookupObject(type) + "(" + type + ") update:" + count);
		}*/
	}

	private double[] calcTypeFrequencyCumSum(int[] typeFrequencyIndex,int[] typeCounts) {
		double [] result = new double[typeCounts.length];
		result[0] = ((double)typeCounts[typeFrequencyIndex[0]]) / corpusWordCount;
		for (int i = 1; i < typeFrequencyIndex.length; i++) {
			result[i] = (((double)typeCounts[typeFrequencyIndex[i]]) / corpusWordCount) + result[i-1];
			//if(i%100==0) System.out.println("Cumsum ["+ (((double)i)/typeCounts.length) +"]: " + result[i]);
		}
		return result;
	}

	/** Trains the LDA model for the given number of iterations. The training is done by sampling z in parallel, updating
	 * the topic-token counts matrix centrally and resampling phi.
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
		boolean printPhi = config.getPrintPhi();
		int startDiagnostic = config.getStartDiagnostic(LDAConfiguration.START_DIAG_DEFAULT);

		String loggingPath = config.getLoggingUtil().getLogDir().getAbsolutePath();

		double logLik = modelLogLikelihood();	
		String tw = topWords (wordsPerTopic);
		LDAUtils.logLikelihoodToFile(logLik,0,tw,loggingPath,logger);

		for (int iteration = 1; iteration <= iterations; iteration++) {
			preIteration();
			currentIteration = iteration;
			//if((iteration%100)==0) System.out.println("Iteration: " + iteration);
			long iterationStart = System.currentTimeMillis();

			// Saves timestamp
			//long beforeLoopOverBatches = System.currentTimeMillis();

			// Sample z by dividing the corpus in batches
			loopOverBatches();

			long beforeSync = System.currentTimeMillis();
			try {
				updateCounts();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			long endTypeTopicUpdate = System.currentTimeMillis();
			logger.fine("Time for updating type-topic counts: " + 
					(endTypeTopicUpdate - beforeSync) + "ms\t");

			//long beforeSamplePhi = System.currentTimeMillis();

			samplePhi();

			long elapsedMillis = System.currentTimeMillis();
			logger.fine("Time for sampling phi: " + (elapsedMillis + iterationStart - endTypeTopicUpdate) + "ms\t");

			if (startDiagnostic > 0 && iteration >= startDiagnostic && printPhi) {
				LDAUtils.writeBinaryDoubleMatrix(phi, iteration, numTopics, numTypes, loggingPath + "/phi");	
			}
			if(output_interval.length == 2 && iteration >= output_interval[0] && iteration <= output_interval[1]) {
				LDAUtils.writeBinaryDoubleMatrix(phi, iteration, numTopics, numTypes, binOutput.getAbsolutePath() + "/phi");
				LDAUtils.writeBinaryIntMatrix(typeTopicCounts, iteration, numTypes, numTopics, binOutput.getAbsolutePath() + "/N");
				LDAUtils.writeBinaryIntMatrix(LDAUtils.getDocumentTopicCounts(data, numTopics), iteration, data.size(), numTopics, binOutput.getAbsolutePath() + "/M");
			}

			logger.fine("\nIteration " + iteration + "\tTotal time: " + elapsedMillis + "ms\t");
			logger.fine("--------------------");

			// Occasionally print more information
			if (showTopicsInterval > 0 && iteration % showTopicsInterval == 0) {
				if(testSet != null) {
					System.out.println("SHOULD PRINT PERPLEXITY!!!");
					//double testPerplexity = LDAUtils.perplexity(config, testSet, getHashTopicTypeCounts(), phi);
					//LDAUtils.perplexityToFile(loggingPath, iteration, testPerplexity, logger);
				}

				logLik = modelLogLikelihood();	
				tw = topWords (wordsPerTopic);
				LDAUtils.logLikelihoodToFile(logLik,iteration,tw,loggingPath,logger);
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
				LDAUtils.writeBinaryDoubleMatrixIndices(phi, iteration, binOutput.getAbsolutePath() + "/Phi_KxV", topIndices);
			}
			postIteration();
		}

		postSample();
	}


	public void postSample() {
		// By now we don't need the thread pools any more
		shutdownThreadPools();

		flushDeltaOut();
	}

	void shutdownThreadPools() {
		phiSamplePool.shutdown();
		try {
			phiSamplePool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		}
		catch (InterruptedException ex) {}

		topicUpdaters.shutdown();
		try {
			topicUpdaters.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		}
		catch (InterruptedException ex) {}
	}

	public void preSample() {
		bb.calculateBatch();
		final int resultsSize = bb.getDocResultsSize();
		int samplerIdx = 0;		
		
		int myBatch = 0;
		for (final int [] docIndices : bb.documentBatches()) {
			final int [][] batch = createBatch(docIndices);
			ActorRef sampler = samplerCores.get(samplerIdx++);
			System.out.println("Telling: " + sampler + " to configure!");
			sampler.tell(new DocumentSamplerConfig(numTopics, numTypes, alphaSum / numTopics, 
					beta, resultsSize, myBatch++, docIndices, sendPartials), inbox.getRef());
			Object reply;
			try {
				reply = inbox.receive(Duration.create(5, TimeUnit.MINUTES));
			} catch (TimeoutException e) {
				throw new IllegalArgumentException("Didn't get DocumentSamplerConfig reply within 10 minutes!");
			}
			if(reply==SpaliasDocumentSampler.Msg.CONFIGURED) {
				System.out.println("Sampler: " + (myBatch-1) + " has confirmed initialization...");
			}
			if(getSendDocs()) {
				sampler.tell(new DocumentBatch(batch), inbox.getRef());
			} else {
				if(!(new File("tmp/TmpBatchStorage").exists())) {
					new File("tmp/TmpBatchStorage").mkdirs();
				}
				String fn = "tmp/TmpBatchStorage/batch-" + (myBatch-1) + ".csv";
				try {
					System.out.println("Saving batch to: "+ fn);
					LDAUtils.writeASCIIIntMatrix(batch, fn, ",");
				} catch (Exception e) {
					throw new IllegalArgumentException(e);
				} 
				sampler.tell(new DocumentBatchLocation(fn), inbox.getRef());
			}
			try {
				reply = inbox.receive(Duration.create(REMOTE_DOCUMENT_TIMEOUT_MINUTES, TimeUnit.MINUTES));
			} catch (TimeoutException e) {
				throw new IllegalArgumentException("Didn't get DocumentBatch reply within " + REMOTE_DOCUMENT_TIMEOUT_MINUTES + " minutes!");
			}
			if(reply==SpaliasDocumentSampler.Msg.DOC_INIT) {
				System.out.println("Sampler: " + (myBatch-1) + " has received document batches...");
			}

		}
		
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
	}

	protected boolean getSendDocs() {
		return false;
	}

	void startupThreadPools() {
		// If we call sample again the thread pool have been shutdown so we create a new one
		if(phiSamplePool == null || phiSamplePool.isShutdown()) {
			phiSamplePool = Executors.newFixedThreadPool(noTopicBatches,new LDAThreadFactory("PhiSampler"));
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

		while ( workersFinished < numActiveWorkers ) {
			//if((docCnt%100)==0) System.out.println("Doc cnt: " + docCnt );

			// Wait until a worker sends us an update structure
			Object reply;
			try {
				reply = inbox.receive(Duration.create(REMOTE_BATCH_TIMEOUT_MINUTES, TimeUnit.MINUTES));
			} catch (TimeoutException e) {
				throw new IllegalArgumentException("Didn't get any update in " + REMOTE_BATCH_TIMEOUT_MINUTES + " minutes!", e);
			}
			if(reply instanceof TypeTopicUpdates) {
				TypeTopicUpdates typeTopicUpdates = (TypeTopicUpdates) reply;
				for (int i = 0; i < typeTopicUpdates.updates.length; i++) {
					int topic = typeTopicUpdates.updates[i] % numTopics;
					int type  = typeTopicUpdates.updates[i] / numTopics;
					i++;
					int count = typeTopicUpdates.updates[i];
					//System.out.println("Updating type:" + type + " topic: " + topic + " => " + count);
					updateTypeTopicCount(type, topic, count);
				}
				if(typeTopicUpdates.isFinal) {
					workersFinished++;
				} 
			} else {
				System.out.println("Unhandled Reply was: " + reply);
			}
		}		
		ensureConsistentTopicTypeCounts(topicTypeCountMapping, false);

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


	void ensureConsistentPhi(double [][] Phi) {
		for (int i = 0; i < Phi.length; i++) {
			double  sum = 0.0;
			for (int j = 0; j < Phi[0].length; j++) {
				sum += Phi[i][j];
			}
			if(sum>1.01&&sum<0.09&&sum>0) throw new IllegalArgumentException("Inconsistent Phi!");
		}
	}

	/**
	 * Spreads the sampling of phi matrix rows on different threads
	 * Creates Runnable() objects that call functions from the superclass
	 */
	protected void samplePhi() {
		tbb.calculateBatch();
		int[][] topicBatches = tbb.topicBatches();

		for (final int [] topicIndices : topicBatches) {
			final int [][] topicTypeIndices = topicIndexBuilder.getTopicTypeIndices();
			Runnable newTask = new Runnable() {
				public void run() {
					try {
						long beforeThreads = System.currentTimeMillis();
						loopOverTopics(topicIndices, topicTypeIndices, phi);
						logger.fine("Time of Thread: " + 
								(System.currentTimeMillis() - beforeThreads) + "ms\t");
						phiSamplings.put(new Object());
					} catch (Exception ex) {
						ex.printStackTrace();
						throw new IllegalStateException(ex);
					}
				}
			};
			phiSamplePool.execute(newTask);
		}
		int phiSamplingsDone = 0;
		while(phiSamplingsDone<topicBatches.length) {
			try {
				phiSamplings.take();
				phiSamplingsDone++;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Samples rows of the phi matrix using the internal data structure for
	 * token-topic assignments
	 * 
	 * @param	first	Index of the first row that should be generated
	 * @param	size	Amount of rows to generate
	 * @param	phiMatrix	Pointer to the phi matrix
	 */
	public void loopOverTopics(int [] indices, double[][] phiMatrix) {
		loopOverTopics(indices, null, phiMatrix);
	}

	/**
	 * Samples new Phi's. If <code>topicTypeIndices</code> is NOT null it will sample phi conditionally
	 * on the indices in <code>topicTypeIndices</code>
	 * 
	 * @param indices
	 * @param topicTypeIndices
	 * @param phiMatrix
	 */
	public void loopOverTopics(int [] indices, int[][] topicTypeIndices, double[][] phiMatrix) {
		long beforeSamplePhi = System.currentTimeMillis();		
		for (int topic : indices) {
			int [] relevantTypeTopicCounts = topicTypeCountMapping[topic]; 
			double[] dirichletParams = new double[numTypes];
			// Generates a standard array to feed to the Dirichlet constructor
			// from the dictionary representation. 
			for (int type = 0; type < numTypes; type++) {
				int thisCount = relevantTypeTopicCounts[type];
				dirichletParams[type] = beta + thisCount; 
			}
			if(topicTypeIndices==null) {
				Dirichlet dist = new ParallelDirichlet(dirichletParams);
				phiMatrix[topic] = dist.nextDistribution();

			} else {
				//System.out.println("Sampling: " + topicTypeIndices[topic].length + " words in topic " + topic);
				ConditionalDirichlet dist = new ConditionalDirichlet(dirichletParams);
				//dist.setNextConditionalDistribution(phiMatrix[topic],topicTypeIndices[topic]);
				double [] newPhi = dist.nextConditionalDistribution(phiMatrix[topic],topicTypeIndices[topic]); 
				phiMatrix[topic] = newPhi;
			}
		}
		long elapsedMillis = System.currentTimeMillis();
		long threadId = Thread.currentThread().getId();

		if(measureTimings) {
			PrintWriter pw = LoggingUtils.checkCreateAndCreateLogPrinter(
					config.getLoggingUtil().getLogDir() + "/timing_data",
					"thr_" + threadId + "_Phi_sampling.txt");
			pw.println(beforeSamplePhi + "," + elapsedMillis);
			pw.flush();
			pw.close();
		}
	}

	/**
	 * This function splits the training set into batches which can be processed separately. 
	 */
	protected void loopOverBatches() {
		// Reset the active workers count
		numActiveWorkers = 0;
		workersFinished = 0;

		for (ActorRef sampler : samplerNodes) {
			sampler.tell(new PhiUpdate(phi), inbox.getRef());
			numActiveWorkers += nodeCoreMapping.get(sampler);
		}
	}

	private int[][] createBatch(int [] indices) {

		// The "times 2" below is because for each array of token sequences we have a corresponding array of topic assignemnts
		int [][] batch = new int[indices.length*2][];
		int docIdx = 0;

		for (int idx = 0; idx < batch.length;) {
			int doc = indices[docIdx++];

			FeatureSequence tokenSequence =
					(FeatureSequence) data.get(doc).instance.getData();
			LabelSequence topicSequence =
					(LabelSequence) data.get(doc).topicSequence;
			int [] tokSeq = new int[tokenSequence.size()];
			System.arraycopy(tokenSequence.getFeatures(), 0, tokSeq, 0, tokenSequence.size()); 
			int [] topSeq = new int[tokenSequence.size()];
			System.arraycopy(topicSequence.getFeatures(), 0, topSeq, 0, tokenSequence.size()); 

			//debugPrintDoc(doc, tokSeq, topSeq);
			batch[idx++] = tokSeq;
			batch[idx++] = topSeq;
		}

		return batch;
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
	 * AD-LDA logLikelihood calculation. It's unclear to me which is "correct"
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
			topicLogGammas[ topic ] = Dirichlet.logGammaStirling( alpha[topic] );
		}

		for (int doc=0; doc < data.size(); doc++) {
			LabelSequence topicSequence =	(LabelSequence) data.get(doc).topicSequence;

			docTopics = topicSequence.getFeatures();

			for (int token=0; token < docTopics.length; token++) {
				topicCounts[ docTopics[token] ]++;
			}

			for (int topic=0; topic < numTopics; topic++) {
				if (topicCounts[topic] > 0) {
					logLikelihood += (Dirichlet.logGammaStirling(alpha[topic] + topicCounts[topic]) -
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
					System.out.println("NaN in log likelihood calculation: " + topicCounts[topic]);
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

		System.out.println("loaded sumtotal: " + sumtotal + " tokens");

		int [] topicIndices = new int[numTopics];
		for (int i = 0; i < numTopics; i++) {
			topicIndices[i] = i;
		}

		loopOverTopics(topicIndices, phi);
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

	/**
	 * This is not used yet, current random scan only looks at most frequent words
	 *
	 */
	class TypeChangePair implements Comparable<TypeChangePair> {
		public int type;
		public int deltaCount = 0;
		public TypeChangePair(int type, int deltaCount) {
			super();
			this.type = type;
			this.deltaCount = deltaCount;
		}
		@Override
		public int compareTo(TypeChangePair o) {
			return deltaCount - o.getDeltaCount();
		}
		public int getType() {
			return type;
		}
		public void setType(int type) {
			this.type = type;
		}
		public int getDeltaCount() {
			return deltaCount;
		}
		public void setDeltaCount(int deltaCount) {
			this.deltaCount = deltaCount;
		}
	}
}
