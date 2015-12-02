package cc.mallet.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class LDAThreadFactory implements ThreadFactory {
    private static final AtomicInteger poolNumber = new AtomicInteger(1);
    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;

	public LDAThreadFactory(String namePrePrefix) {
		SecurityManager s = System.getSecurityManager();
        group = (s != null) ? s.getThreadGroup() :
                              Thread.currentThread().getThreadGroup();
        namePrefix = namePrePrefix + "-" +
                      poolNumber.getAndIncrement() +
                     "-thread-";
	}

	
	@Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(group, r,
                              namePrefix + threadNumber.getAndIncrement(),
                              0);
        
        if (t.isDaemon())
            t.setDaemon(false);
        // I can't really see that we want non-daemon threads...
        // Seems we might get problem with daemon-threads?? Does this mess up join after invoke??
        // t.setDaemon(true);
        if (t.getPriority() != Thread.NORM_PRIORITY)
            t.setPriority(Thread.NORM_PRIORITY);
        return t;
    }
}
