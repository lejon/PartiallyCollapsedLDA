package cc.mallet.topics;

import java.io.IOException;
import java.util.ArrayList;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.types.Alphabet;
import cc.mallet.types.InstanceList;

public interface LDAGibbsSampler extends AbortableSampler {
	void setConfiguration(LDAConfiguration config);
	LDAConfiguration getConfiguration();
	void addInstances (InstanceList training);
	void addTestInstances (InstanceList testSet);
	void sample (int iterations) throws IOException;
	void setRandomSeed(int seed);
	int getNoTopics();
	int getNumTopics();
	int getNoTypes();
	int getCurrentIteration();
	int [][] getZIndicators();
	double [][] getZbar();
	double[][] getThetaEstimate();
	void setZIndicators(int[][] zIndicators);
	InstanceList getDataset();
	ArrayList<TopicAssignment> getData();
	int[][] getDeltaStatistics();
	int[] getTopTypeFrequencyIndices();
	int[] getTypeFrequencies();
	int getCorpusSize();
	Alphabet getAlphabet();
	int getStartSeed();
	double[] getTypeMassCumSum();
	int [][] getDocumentTopicMatrix();
	int [][] getTypeTopicMatrix();
	int [] getTopicTotals();
	double getBeta();
	double[] getAlpha();
	void preIteration();
	void postIteration();
	void preSample();
	void postSample();
	void postZ();
	void preZ();
	double[] getLogLikelihood();
	double[] getHeldOutLogLikelihood();
}
