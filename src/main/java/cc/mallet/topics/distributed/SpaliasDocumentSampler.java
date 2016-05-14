package cc.mallet.topics.distributed;

import gnu.trove.TIntArrayList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

import akka.actor.UntypedActor;
import cc.mallet.util.LDAUtils;
import cc.mallet.util.OptimizedGentleAliasMethod;
import cc.mallet.util.WalkerAliasTable;


public class SpaliasDocumentSampler extends UntypedActor {
	
	int [][] myDocuments;
	double [][] phi;
	
	public int numTopics;
	public int numTypes;
	public double alpha;
	public double beta;
	public int resultsSize;
	public int [] docIndices;
	public  int myBatch;
	public boolean sendPartials = true;
	
	// TODO: Would using a Set be more efficient?
	TIntArrayList typeTopicsUpdated;

	int [][] localTopicTypeUpdates;
	
	boolean configured = false;
	
	WalkerAliasTable [] aliasTables; 
	double [] typeNorm; // Array with doubles with sum of alpha * phi
	private ExecutorService tableBuilderExecutor = Executors.newFixedThreadPool(10);

	public static enum Msg {
		START, EXIT, DONE, DOC_INIT, CONFIGURED;
	}
		
	@Override
	public void onReceive(Object msg) {
		if (msg == Msg.EXIT) {
			System.out.println("Quitting...");
			postSample();
			getSender().tell(Msg.DONE, getSelf());
		} else if (msg == Msg.START) {
			System.out.println("Actor started...");
		} else if (msg instanceof DocumentBatchLocation) {
			System.out.println(getSelf()  + ": Got new document batch location!");
			DocumentBatchLocation loc = (DocumentBatchLocation) msg;
			try {
				myDocuments = LDAUtils.readASCIIIntMatrix(loc.filename, ",");
				System.out.println(getSelf()  + ": Successfully loaded document batch from: " + loc.filename + "!");
			} catch (IOException e) {
				e.printStackTrace();
			}
			getSender().tell(Msg.DOC_INIT, getSelf());
		} else if (msg instanceof DocumentBatch) {
			System.out.println(getSelf()  + ": Got new document batch!");
			DocumentBatch rm = (DocumentBatch) msg;
			myDocuments = rm.docTopics;
			getSender().tell(Msg.DOC_INIT, getSelf());
		} else if (msg instanceof PhiUpdate) {
			if(!configured) throw new IllegalStateException(getSelf() + ": Is not configured yet!!");
			PhiUpdate phiUpdate = (PhiUpdate) msg;
			System.out.println(getSelf() + ": Received new Phi: " + phiUpdate);
			this.phi = phiUpdate.phi;
			buildAliasTablesParallel();
			localTopicTypeUpdates = new int[numTopics][numTypes];
			typeTopicsUpdated.clear();
			loopOverDocuments(myDocuments, docIndices, myBatch);
		} else if (msg instanceof DocumentSamplerConfig) {
			DocumentSamplerConfig conf = (DocumentSamplerConfig) msg;
			this.numTopics = conf.numTopics;
			this.numTypes = conf.numTypes;
			this.alpha = conf.alpha;
			this.beta = conf.beta;
			this.myBatch = conf.batchId;
			this.docIndices = conf.docIndices;
			this.resultsSize = conf.resultSize;
			this.sendPartials = conf.sendPartials;
			aliasTables = new WalkerAliasTable[numTypes];
			typeNorm    = new double[numTypes];
			typeTopicsUpdated = new TIntArrayList();
			printConfig();
			preSample();
			configured = true;
			getSender().tell(Msg.CONFIGURED, getSelf());
		}  else
			unhandled(msg);
	}
	
	void printConfig() {
		System.out.println(getSelf() + ": I am configured: ");
		System.out.println("this.numTopics = " + this.numTopics);
		System.out.println("this.numTypes = " + this.numTypes);
		System.out.println("this.alpha = " + this.alpha);
		System.out.println("this.beta = " + this.beta);
		System.out.println("this.myBatch = " + this.myBatch);
		System.out.println("this.docIndices = " + Arrays.toString(docIndices));
		System.out.println("this.resultsSize = " + this.resultsSize);

	}

	class ParallelTableBuilder implements Callable<TableBuildResult> {
		int type;
		public ParallelTableBuilder(int type) {
			this.type = type;
		}
		@Override
		public TableBuildResult call() {
			double [] probs = new double[numTopics];
			double typeMass = 0; // Type prior mass
			for (int topic = 0; topic < numTopics; topic++) {
				typeMass += probs[topic] = phi[topic][type] * alpha; // alpha[topic]
			}
			
			if(aliasTables[type]==null) {
				aliasTables[type] = new OptimizedGentleAliasMethod(probs,typeMass);
			} else {
				aliasTables[type].reGenerateAliasTable(probs, typeMass);
			}
				
			return new TableBuildResult(type, aliasTables[type], typeMass);
		}   
	}

	static class TableBuildResult {
		public int type;
		public WalkerAliasTable table;
		public double typeNorm;
		public TableBuildResult(int type, WalkerAliasTable table, double typeNorm) {
			super();
			this.type = type;
			this.table = table;
			this.typeNorm = typeNorm;
		}
	}

	protected void preSample() {
		//int poolSize = 2;
		//tableBuilderExecutor = Executors.newFixedThreadPool(Math.max(1, poolSize));
		//System.out.println("Started sampler pool: " + tableBuilderExecutor + "!");
	}

	public void buildAliasTablesParallel() {
		List<ParallelTableBuilder> builders = new ArrayList<>();
		for (int type = 0; type < numTypes; type++) {
			builders.add(new ParallelTableBuilder(type));
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

	protected void postSample() {
		tableBuilderExecutor.shutdown();
	}
	
	protected boolean sendPartial() {
		return sendPartials;
	}
	
	protected void loopOverDocuments(int [][] batch, int [] docIndices, int myBatch) {
		for (int doc = 0; doc < batch.length;) {
			int[] tokenSequence = batch[doc++];
			int[] topicSequence = batch[doc++];
			sampleTopicAssignments (tokenSequence, topicSequence, myBatch);
			if(sendPartial() && doc % resultsSize == 0) {
				int [] updated = buildUpdateStructure();
				// now we must reset the localTopicTypeUpdates
				localTopicTypeUpdates = new int[numTopics][numTypes];
				//System.out.println("Sending partial " + updated.length + " deltas...");
				// Send a partial update, indicated by the 'false' argument
				getSender().tell(new TypeTopicUpdates(updated,false), getSelf());
			}
		}
		
		// updated contains the index of the updated count and the actual count
		int [] updated = buildUpdateStructure();
		getSender().tell(new TypeTopicUpdates(updated), getSelf());
		System.out.println("Sending final  " + updated.length + " deltas...");
	}

	int [] buildUpdateStructure() {
		int [] updated = new int[typeTopicsUpdated.size()*2];
		int uIdx = 0;
		for (int i = 0; i < typeTopicsUpdated.size(); i++) {
			// Add the index
			updated[uIdx++] = typeTopicsUpdated.get(i);
			int topic = typeTopicsUpdated.get(i) % numTopics;
			int type  = typeTopicsUpdated.get(i) / numTopics;
			// Add the count
			updated[uIdx++] = localTopicTypeUpdates[topic][type]; 	
		}
		//System.out.println("Sending " + updated.length + " deltas...");
		typeTopicsUpdated.resetQuick();
				
		return updated;
	}
	
	void sampleTopicAssignments(int [] tokenSequence, int [] oneDocTopics, int myBatch) {
		int type, oldTopic, newTopic;

		final int docLength = tokenSequence.length;
		if(docLength==0) return;
		
		double[] localTopicCounts = new double[numTopics];

		// This vector contains the indices of the topics with non-zero entries.
		// It has to be numTopics long since the non-zero topics come and go...
		int [] nonZeroTopics = new int[numTopics];

		// So we can map back from a topic to where it is in nonZeroTopics vector
		int [] nonZeroTopicsBackMapping = new int[numTopics];
		
		// Populate topic counts
		int nonZeroTopicCnt = 0;
		for (int position = 0; position < docLength; position++) {
			int topicInd = oneDocTopics[position];
			localTopicCounts[topicInd]++;
			if(localTopicCounts[topicInd]==1) {
				nonZeroTopicCnt = insert(topicInd, nonZeroTopics, nonZeroTopicsBackMapping, nonZeroTopicCnt);
			}
		}
		double sum; // sigma_likelihood
		double[] cumsum = new double[numTopics]; 

		//	Iterate over the words in the document
		for (int position = 0; position < docLength; position++) {
			type = tokenSequence[position];
			oldTopic = oneDocTopics[position]; // z_position
			localTopicCounts[oldTopic]--;

			// Potentially update nonZeroTopics mapping
			if(localTopicCounts[oldTopic]==0) {
				nonZeroTopicCnt = remove(oldTopic, nonZeroTopics, nonZeroTopicsBackMapping, nonZeroTopicCnt);
			}

			if(localTopicCounts[oldTopic]<0) 
				throw new IllegalStateException("Counts cannot be negative! Count for topic:" 
						+ oldTopic + " is: " + localTopicCounts[oldTopic]);

			// Propagates the update to the topic-token assignments
			/**
			 * Used to subtract and add 1 to the local structure containing the number of times
			 * each token is assigned to a certain topic. Called before and after taking a sample
			 * topic assignment z
			 */
			decrement(localTopicTypeUpdates,oldTopic,type);
			
			sum = calcCumSum(type, localTopicCounts, nonZeroTopics, nonZeroTopicCnt, cumsum);

			// Choose a random point between 0 and the sum of all topic scores
			// The thread local random performs better in concurrent situations 
			// than the standard random which is thread safe and incurs lock 
			// contention
			double u = ThreadLocalRandom.current().nextDouble();
			double u_sigma = u * (typeNorm[type] + sum);
			// u ~ U(0,1)  
			// u [0,1]
			// u_sigma = u * (typeNorm[type] + sum)
			// if u_sigma < typeNorm[type] -> prior
			// u * (typeNorm[type] + sum) < typeNorm[type] => u < typeNorm[type] / (typeNorm[type] + sum)
			// else -> likelihood
			// u_prior = u_sigma / typeNorm[type] -> u_prior (0,1)
			// u_likelihood = (u_sigma - typeNorm[type]) / sum  -> u_likelihood (0,1)

			newTopic = sampleNewTopic(type, nonZeroTopics, nonZeroTopicCnt, sum, cumsum, u, u_sigma);

			// Make sure we actually sampled a valid topic
			if (newTopic < 0 || newTopic > numTopics) {
				throw new IllegalStateException ("SpaliasUncollapsedParallelLDA: New valid topic not sampled (" + newTopic + ").");
			}

			// Put that new topic into the counts
			oneDocTopics[position] = newTopic;
			localTopicCounts[newTopic]++;

			// Potentially update nonZeroTopics mapping
			if(localTopicCounts[newTopic]==1) {
				nonZeroTopicCnt = insert(newTopic, nonZeroTopics, nonZeroTopicsBackMapping, nonZeroTopicCnt);
			}

			// Propagates the update to the topic-token assignments
			/**
			 * Used to subtract and add 1 to the local structure containing the number of times
			 * each token is assigned to a certain topic. Called before and after taking a sample
			 * topic assignment z
			 */
			increment(localTopicTypeUpdates,newTopic,type);
		}
		//System.out.println("Ratio: " + ((double)numPrior/(double)numLikelihood));
	}
	
	protected void increment(int[][] localTopicUpdates, int newTopic, int type) {
		localTopicUpdates[newTopic][type] += 1;
		if(localTopicUpdates[newTopic][type]==0) {
			int idx = newTopic + (type * numTopics);
			int pos = typeTopicsUpdated.lastIndexOf(idx);
			typeTopicsUpdated.remove(pos);
			//removeUpdate(oldTopic, type);
		} else if(localTopicUpdates[newTopic][type]==1) {
			int idx = newTopic + (type * numTopics);
			typeTopicsUpdated.add(idx);
			//addNewUpdate(newTopic, type);
		}
	}

	/*private void addNewUpdate(int newTopic, int type) {
		typeTopicsUpdated[typeTopicsUpdatedCnt++] = newTopic + (type * numTopics);		
	}*/

	protected void decrement(int[][] localTopicUpdates, int oldTopic, int type) {
		localTopicUpdates[oldTopic][type] -= 1;
		if(localTopicUpdates[oldTopic][type]==0) {
			int idx = oldTopic + (type * numTopics);
			int pos = typeTopicsUpdated.lastIndexOf(idx);
			typeTopicsUpdated.remove(pos);
			//removeUpdate(oldTopic, type);
		} else if(localTopicUpdates[oldTopic][type]==-1) {
			int idx = oldTopic + (type * numTopics);
			typeTopicsUpdated.add(idx);
			//addNewUpdate(newTopic, type);
		}

	}

	/*private void removeUpdate(int oldTopic, int type) {
		int storedType = -1;
		int storedTopic = -1;
		int idx = 0;
		while(storedType!=type&&storedTopic!=oldTopic) {
			storedTopic = typeTopicsUpdated[idx] % numTopics;
			storedType  = typeTopicsUpdated[idx] / numTopics;
		}		
		// We have now found where in the array the old topic / type was stored, remove it 
		// by moving the last type/topic to its place
		typeTopicsUpdated[idx] = typeTopicsUpdated[--typeTopicsUpdatedCnt]; 
	}*/

	double calcCumSum(int type, double[] localTopicCounts, int[] nonZeroTopics, int nonZeroTopicCnt, double[] cumsum) {
		double score;
		double sum;
		int topic = nonZeroTopics[0];
		score = localTopicCounts[topic] * phi[topic][type];
		cumsum[0] = score;
		// Now calculate and add up the scores for each topic for this word
		// We build a cumsum indexed by topicIndex
		int topicIdx = 1;
		while ( topicIdx < nonZeroTopicCnt ) {
			topic = nonZeroTopics[topicIdx];
			score = localTopicCounts[topic] * phi[topic][type];
			cumsum[topicIdx] = score + cumsum[topicIdx-1];
			topicIdx++;
		}
		sum = cumsum[topicIdx-1]; // sigma_likelihood
		return sum;
	}

	int sampleNewTopic(int type, int[] nonZeroTopics, int nonZeroTopicCnt, double sum, double[] cumsum, double u,
			double u_sigma) {
		int newTopic;
		if(u < (typeNorm[type]/(typeNorm[type] + sum))) {
			//numPrior++;
			newTopic = aliasTables[type].generateSample(u+((sum*u)/typeNorm[type])); // uniform (0,1)
			//System.out.println("Prior Sampled topic: " + newTopic);
		} else {
			//numLikelihood++;
			//double u_lik = (u_sigma - typeNorm[type]) / sum; // Cumsum is not normalized so don't divide by sum 
			double u_lik = (u_sigma - typeNorm[type]);
			int slot = findIdx(cumsum,u_lik,nonZeroTopicCnt);
			newTopic = nonZeroTopics[slot];
			// Make sure we actually sampled a valid topic
		}
		return newTopic;
	}

	protected static int remove(int oldTopic, int[] nonZeroTopics, int[] nonZeroTopicsBackMapping, int nonZeroTopicCnt) {
		if (nonZeroTopicCnt<1) {
			throw new IllegalArgumentException ("SpaliasUncollapsedParallelLDA: Cannot remove, count is less than 1 => " + nonZeroTopicCnt);
		}
		// We have one less non-zero topic, move the last to its place, and decrease the non-zero count
		int nonZeroIdx = nonZeroTopicsBackMapping[oldTopic];
		nonZeroTopics[nonZeroIdx] = nonZeroTopics[--nonZeroTopicCnt];
		nonZeroTopicsBackMapping[nonZeroTopics[nonZeroIdx]] = nonZeroIdx;
		return nonZeroTopicCnt;
	}

	protected static int insert(int newTopic, int[] nonZeroTopics, int[] nonZeroTopicsBackMapping, int nonZeroTopicCnt) {
		//// We have a new non-zero topic put it in the last empty slot and increase the count
		nonZeroTopics[nonZeroTopicCnt] = newTopic;
		nonZeroTopicsBackMapping[newTopic] = nonZeroTopicCnt;
		return ++nonZeroTopicCnt;
	}

	protected static int removeSorted(int oldTopic, int[] nonZeroTopics, int[] nonZeroTopicsBackMapping, int nonZeroTopicCnt) {
		if (nonZeroTopicCnt<1) {
			throw new IllegalArgumentException ("SpaliasUncollapsedParallelLDA: Cannot remove, count is less than 1");
		}
		//System.out.println("New empty topic. Cnt = " + nonZeroTopicCnt);	
		int nonZeroIdx = nonZeroTopicsBackMapping[oldTopic];
		nonZeroTopicCnt--;
		// Shift the ones above one step to the left
		for(int i=nonZeroIdx; i<nonZeroTopicCnt;i++) {
			// Move the last non-zero topic to this new empty slot 
			nonZeroTopics[i] = nonZeroTopics[i+1];
			// Do the corresponding for the back mapping
			nonZeroTopicsBackMapping[nonZeroTopics[i]] = i;
		}
		return nonZeroTopicCnt;
	}

	protected static int insertSorted(int newTopic, int[] nonZeroTopics, int[] nonZeroTopicsBackMapping, int nonZeroTopicCnt) {
		//// We have a new non-zero topic put it in the last empty slot
		int slot = 0;
		while(newTopic > nonZeroTopics[slot] && slot < nonZeroTopicCnt) slot++;

		for(int i=nonZeroTopicCnt; i>slot;i--) {
			// Move the last non-zero topic to this new empty slot 
			nonZeroTopics[i] = nonZeroTopics[i-1];
			// Do the corresponding for the back mapping
			nonZeroTopicsBackMapping[nonZeroTopics[i]] = i;
		}				
		nonZeroTopics[slot] = newTopic;
		nonZeroTopicsBackMapping[newTopic] = slot;
		nonZeroTopicCnt++;
		return nonZeroTopicCnt;
	}

	protected static int findIdx(double[] cumsum, double u, int maxIdx) {
		return findIdxBin(cumsum,u,maxIdx);
	}
	
	protected static int findIdxBin(double[] cumsum, double u, int maxIdx) {
		int slot = java.util.Arrays.binarySearch(cumsum,0,maxIdx,u);

		return slot >= 0 ? slot : -(slot+1); 
	}

	public static int findIdxLinSentinel(double[] cumsum, double u, int maxIdx) {
		cumsum[cumsum.length-1] = Double.MAX_VALUE;
		int i = 0;
		while(true) {
			if(u<=cumsum[i]) return i;
			i++;
		}
	}

	public static int findIdxLin(double[] cumsum, double u, int maxIdx) {
		for (int i = 0; i < maxIdx; i++) {
			if(u<=cumsum[i]) return i;
		}
		return cumsum.length-1;
	}
	
	
}
