package cc.mallet.topics;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.topics.randomscan.topic.AllWordsTopicIndexBuilder;
import cc.mallet.types.ConditionalDirichlet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelSequence;
import cc.mallet.util.LoggingUtils;


/**
 * Adds type priors (anchor words) to standard Spalias. Increases mem requirement with vocab x topics and
 * requires access to this PhiPrior in each topic indicator draw possibly reducing cache performance. 
 * 
 * @author Leif Jonsson
 *
 */
public class SpaliasUncollapsedParallelWithPriors extends SpaliasUncollapsedParallelLDA implements LDASamplerWithPhi, LDASamplerWithPriors {
	
	private static final long serialVersionUID = 1L;
	
	public SpaliasUncollapsedParallelWithPriors(LDAConfiguration config) {
		super(config);
	}
	
	@Override
	public void addInstances (InstanceList training) {
		alphabet = training.getDataAlphabet();
		numTypes = alphabet.size();
		initializePriors(config);
		super.addInstances(training);
	}

	@Override
	public double[][] getTopicPriors() {
		return topicPriors;
	}
		
	@Override
	protected LDADocSamplingResult sampleTopicAssignmentsParallel(LDADocSamplingContext ctx) {
		FeatureSequence tokens = ctx.getTokens();
		LabelSequence topics = ctx.getTopics();
		int myBatch = ctx.getMyBatch();
		
		int type, oldTopic, newTopic;

		final int docLength = tokens.getLength();
		if(docLength==0) return new LDADocSamplingResultSparseSimple(new int[0],0,new int[0]);
		
		int [] tokenSequence = tokens.getFeatures();
		int [] oneDocTopics = topics.getFeatures();

		int[] localTopicCounts = new int[numTopics];

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
		
		//kdDensities[myBatch] += nonZeroTopicCnt;
		kdDensities.addAndGet(nonZeroTopicCnt);
		
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
			decrement(myBatch, oldTopic, type);
			//System.out.println("(Batch=" + myBatch + ") Decremented: topic=" + oldTopic + " type=" + type + " => " + batchLocalTopicUpdates[myBatch][oldTopic][type]);
			 
			int topic = nonZeroTopics[0];
			double score = localTopicCounts[topic] * phi[topic][type] * topicPriors[topic][type];
			cumsum[0] = score;
			// Now calculate and add up the scores for each topic for this word
			// We build a cumsum indexed by topicIndex
			int topicIdx = 1;
			while ( topicIdx < nonZeroTopicCnt ) {
				topic = nonZeroTopics[topicIdx];
				score = localTopicCounts[topic] * phi[topic][type] * topicPriors[topic][type];
				cumsum[topicIdx] = score + cumsum[topicIdx-1];
				topicIdx++;
			}
			sum = cumsum[topicIdx-1]; // sigma_likelihood

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
			increment(myBatch, newTopic, type);
			//System.out.println("(Batch=" + myBatch + ") Incremented: topic=" + newTopic + " type=" + type + " => " + batchLocalTopicUpdates[myBatch][newTopic][type]);		
		}
		//System.out.println("Ratio: " + ((double)numPrior/(double)numLikelihood));
		return new LDADocSamplingResultSparseSimple(localTopicCounts,nonZeroTopicCnt,nonZeroTopics);
	}
	
	/**
	 * Samples new Phi's. If <code>topicTypeIndices</code> is NOT null it will sample phi conditionally
	 * on the indices in <code>topicTypeIndices</code>. This version takes topic priors into consideration
	 * 
	 * @param indices indices of the topics that should be sampled, the other ones are skipped 
	 * @param topicTypeIndices matrix containing the indices of the types that should be sampled (per topic)
	 * @param phiMatrix
	 */
	@Override
	public void loopOverTopics(int [] indices, int[][] topicTypeIndices, double[][] phiMatrix) {
		long beforeSamplePhi = System.currentTimeMillis();		
		for (int topic : indices) {
			int [] relevantTypeTopicCounts = topicTypeCountMapping[topic]; 
			// Generates a standard array to feed to the Dirichlet constructor
			// from the dictionary representation. 
			if(topicTypeIndices==null && !haveTopicPriors ) {
				phiMatrix[topic] = dirichletSampler.nextDistribution(relevantTypeTopicCounts);
			} else {
				double[] dirichletParams = new double[numTypes];
				for (int type = 0; type < numTypes; type++) {
					int thisCount = relevantTypeTopicCounts[type];
					dirichletParams[type] = beta + thisCount; 
				}
				
				if(topicTypeIndices==null) {
					topicTypeIndices = AllWordsTopicIndexBuilder.getAllIndicesMatrix(numTypes, numTopics);
				}
				int[] typeIndicesToSample = topicTypeIndices[topic];
				
				// If we have priors, remove any type in this topic that has zero probability
				if(haveTopicPriors) {
					List<Integer> mergedIndexList = new ArrayList<Integer>();
					double [] thisTopicPriors = topicPriors[topic];
					for (int type = 0; type < typeIndicesToSample.length; type++) {
						if(thisTopicPriors[type]!=0.0) {
							mergedIndexList.add(typeIndicesToSample[type]);
						}
					}
					int [] newTypeIndicesToSample = new int[mergedIndexList.size()];
					for (int i = 0; i < mergedIndexList.size(); i++) {
						newTypeIndicesToSample[i] = mergedIndexList.get(i);
					}
					typeIndicesToSample = newTypeIndicesToSample;
				}
								
				ConditionalDirichlet dist = new ConditionalDirichlet(dirichletParams);
				double [] newPhi = dist.nextConditionalDistribution(phiMatrix[topic],typeIndicesToSample); 
				
				phiMatrix[topic] = newPhi;
			}
			if(savePhiMeans() && samplePhiThisIteration()) {
				for (int phi = 0; phi < phiMatrix[topic].length; phi++) {
					phiMean[topic][phi] += phiMatrix[topic][phi];
				}
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
	
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeObject(topicPriors);
	}

	private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
		topicPriors = (double[][]) in.readObject();
	}

	@Override
	public void initFrom(LDAGibbsSampler source) {
		super.initFrom(source);
		LDASamplerWithPriors phiSampler = (LDASamplerWithPriors) source;
		topicPriors = phiSampler.getTopicPriors();
	}
}
