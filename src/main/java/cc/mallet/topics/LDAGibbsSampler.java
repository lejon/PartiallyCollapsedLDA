package cc.mallet.topics;

import java.io.IOException;
import java.util.ArrayList;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.topics.TopicAssignment;
import cc.mallet.types.Alphabet;
import cc.mallet.types.InstanceList;

public interface LDAGibbsSampler {
	void setConfiguration(LDAConfiguration config);
	public void addInstances (InstanceList training);
	public void sample (int iterations) throws IOException;
	public void setRandomSeed(int seed);
	public String [][] getTopWords(int noWords);
	public int getNoTopics();
	public int getCurrentIteration();
	public int [][] getZIndicators();
	double [][] getZbar();
	double[][] getThetaEstimate();
	public void setZIndicators(int[][] zIndicators);
	public ArrayList<TopicAssignment> getDataset();
	int[][] getDeltaStatistics();
	int[] getTopTypeFrequencyIndices();
	int[] getTypeFrequencies();
	int getCorpusSize();
	Alphabet getAlphabet();
	int getStartSeed();
	double[] getTypeMassCumSum();
	int [][] getDocumentTopicMatrix();
	int [][] getTypeTopicMatrix();
}
