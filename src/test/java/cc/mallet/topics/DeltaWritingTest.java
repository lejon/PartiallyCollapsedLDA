package cc.mallet.topics;

/*import static org.junit.Assert.assertEquals;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.Test;

import utils.LoggingUtils;
import configuration.SimpleLDAConfiguration;*/

public class DeltaWritingTest {

	// This test is no longer valid since we no longer have sampling results on 
	// doc level since we removed SamplingResult
	//@Test
	/*public void test() throws IOException {
		String whichModel = "uncollapsed";
		Integer numTopics = 20;
		Double alphaSum = 1.0; 
		Double beta = 0.01;
		Integer numIter = 1000;
		Integer numBatches = 6;
		Integer rareWordThreshold = 0;
		Integer showTopicsInterval = 50;
		Integer startDiagnosticOutput = 500;

		SimpleLDAConfiguration config = new SimpleLDAConfiguration(new LoggingUtils(), whichModel,
				numTopics, alphaSum, beta, numIter,
				numBatches, rareWordThreshold, showTopicsInterval,
				startDiagnosticOutput,4711,"src/main/resources/datasets/nips.txt");
		
		UncollapsedParallelLDA uncollapsed = new UncollapsedParallelLDA(config);
		
		int noTopics = 5;
		int [][] localTopicTypeUpdates = new int [5][1];
		int [] vocabMapping = {0,0,0,0,0,0,0,0};
		int type = 7;
		int count = 1;
		for (int topic = 0; topic < noTopics; topic++) {
			localTopicTypeUpdates[topic][vocabMapping[type]] += count;
		}

		int [] oneDocTopics = {12,13,14,15};
		int [] docVocabMapping = {7};

		SamplingResult sr = uncollapsed.new SamplingResult(localTopicTypeUpdates, oneDocTopics,docVocabMapping);
		File temp = File.createTempFile("PCPLDA_DeltaN_TEST", ".tmp"); 

		System.out.println("Temp file : " + temp.getAbsolutePath());

		int iterNo = 13;		
		int processDoc = 5;
		DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(temp.getAbsolutePath())));
		sr.writeStats(dos, iterNo, processDoc );
		dos.flush();
		dos.close();
		
		DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(temp.getAbsolutePath())));
				
		TIntObjectHashMap<TIntIntHashMap> readTopicTypeUpdates = new TIntObjectHashMap<TIntIntHashMap>();
		
		assertEquals(iterNo,dis.readInt());
		assertEquals(processDoc,dis.readInt());
		assertEquals(noTopics,dis.readInt());
		for (int i = 0; i < noTopics; i++) {
			int topic = dis.readInt();
			int readNoTypes = dis.readInt();
			assertEquals(1,readNoTypes);
			int readType = dis.readInt();
			assertEquals(7,readType);
			int readCount = dis.readInt();
			assertEquals(1,readCount);
			TIntIntHashMap hashTopic = null;
			if (!readTopicTypeUpdates.containsKey(topic)) {
				hashTopic = new TIntIntHashMap();
				hashTopic.put(readType, 0);
				readTopicTypeUpdates.put(topic, hashTopic);
			}
			if(hashTopic==null)	hashTopic = readTopicTypeUpdates.get(topic);
			if (!hashTopic.containsKey(readType)) {
				hashTopic.put(readType, 0);
			}
		
			hashTopic.put(readType, ( readTopicTypeUpdates.get(topic).get(readType) + readCount ) );
		}
		assertEquals(0,dis.available());
		
		dis.close();
	}*/
}
