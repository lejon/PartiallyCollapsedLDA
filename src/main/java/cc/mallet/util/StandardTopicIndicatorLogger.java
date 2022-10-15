package cc.mallet.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.topics.TopicAssignment;
import cc.mallet.types.LabelSequence;

public class StandardTopicIndicatorLogger implements TopicIndicatorLogger {
    public void log(ArrayList<TopicAssignment> data, LDAConfiguration config, int iteration) {
        File ld = config.getLoggingUtil().getLogDir();
		File z_file = new File(ld.getAbsolutePath() + "/z_" + iteration + ".csv");
		try (FileWriter fw = new FileWriter(z_file, false); 
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter pw  = new PrintWriter(bw)) {
			for (int docIdx = 0; docIdx < data.size(); docIdx++) {
				String szs = "";
				LabelSequence topicSequence =
						(LabelSequence) data.get(docIdx).topicSequence;
				int [] oneDocTopics = topicSequence.getFeatures();
				for (int i = 0; i < topicSequence.size(); i++) {
					szs += oneDocTopics[i] + ",";
				}
				if(szs.length()>0) {
					szs = szs.substring(0, szs.length()-1);
				}
				pw.println(szs);			
			}			
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
}
