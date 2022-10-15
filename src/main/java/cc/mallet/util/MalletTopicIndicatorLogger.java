package cc.mallet.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.topics.TopicAssignment;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.LabelSequence;

public class MalletTopicIndicatorLogger implements TopicIndicatorLogger {
	
	public void log(ArrayList<TopicAssignment> data, LDAConfiguration config, int iteration) {
		Alphabet a = data.get(0).instance.getDataAlphabet();
		File ld = config.getLoggingUtil().getLogDir();
		File z_file = new File(ld.getAbsolutePath() + "/z_" + iteration + ".csv");
		try (FileWriter fw = new FileWriter(z_file, false); 
		BufferedWriter bw = new BufferedWriter(fw);
		PrintWriter pw  = new PrintWriter(bw)) {
			pw.println ("#doc pos typeindex type topic");
			for (int di = 0; di < data.size(); di++) {
				FeatureSequence fs = (FeatureSequence) data.get(di).instance.getData();
				LabelSequence topicSequence =
				(LabelSequence) data.get(di).topicSequence;
				int [] oneDocTopics = topicSequence.getFeatures();
				for (int si = 0; si < fs.size(); si++) {
					int type = fs.getIndexAtPosition(si);
					pw.print(di); pw.print(' ');
					pw.print(si); pw.print(' ');
					pw.print(type); pw.print(' ');
					pw.print(a.lookupObject(type)); pw.print(' ');
					pw.print(oneDocTopics[si]); pw.println();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
