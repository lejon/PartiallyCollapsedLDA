package cc.mallet.util;

import java.util.ArrayList;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.topics.TopicAssignment;

public interface TopicIndicatorLogger {
    void log(ArrayList<TopicAssignment> data, LDAConfiguration config, int iteration);
}
