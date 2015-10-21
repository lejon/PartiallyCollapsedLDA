package cc.mallet.topics.distributed;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Inbox;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import cc.mallet.configuration.ConfigFactory;
import cc.mallet.configuration.Configuration;
import cc.mallet.configuration.LDACommandLineParser;
import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.topics.LDAGibbsSampler;
import cc.mallet.topics.LDAUtils;
import cc.mallet.topics.tui.ParallelLDA;
import cc.mallet.types.InstanceList;
import cc.mallet.util.LoggingUtils;
import cc.mallet.util.Timer;

public class SpaliasMainLocal {

  public static void main(String[] args) throws Exception {

	  Thread.setDefaultUncaughtExceptionHandler(new Thread.
			  UncaughtExceptionHandler() {
		  public void uncaughtException(Thread t, Throwable e) {
			  System.out.println(t + " throws exception: " + e);
			  e.printStackTrace();
			  System.err.println("Main thread Exiting.");
			  System.exit(-1);
		  }
	  });
	  
    ActorSystem system = ActorSystem.create("SpaliasDistributedLDASampler");
	final Inbox inbox = Inbox.create(system);
             
	// Reading in command line parameters		
	LDACommandLineParser cp = new LDACommandLineParser(args);
	LDAConfiguration config = (LDAConfiguration) ConfigFactory.getMainConfiguration(cp);
	LoggingUtils lu = new LoggingUtils();
	lu.checkAndCreateCurrentLogDir("Runs");
	config.setLoggingUtil(lu);
	config.activateSubconfig("PLDA");
	
	System.out.println("Using Config: " + config.whereAmI());

	String dataset_fn = config.getDatasetFilename();
	System.out.println("Using dataset: " + dataset_fn);
	String whichModel = config.getScheme();
	System.out.println("Scheme: " + whichModel);

	InstanceList instances = LDAUtils.loadInstances(dataset_fn, 
			config.getStoplistFilename("stoplist.txt"), config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD), config.keepNumbers());
	
	List<ActorRef> samplers = new ArrayList<>();
	for(int i = 0; i < config.getNoBatches(LDAConfiguration.NO_BATCHES_DEFAULT); i++) {
		ActorRef a = system.actorOf(Props.create(SpaliasDocumentSampler.class), "sampler" + i);
		system.actorOf(Props.create(Terminator.class, a), "terminator" + i);
		samplers.add(a);
	}
	
	LDAGibbsSampler model = new DistributedSpaliasUncollapsedSampler(config, samplers, inbox);
	System.out.println(
			String.format("ADLDA (%d batches).", 
					config.getNoBatches(LDAConfiguration.NO_BATCHES_DEFAULT)));

	System.out.println(String.format("Rare word threshold: %d", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD)));
	
	System.out.println("Vocabulary size: " + instances.getDataAlphabet().size() + "\n");
	System.out.println("Instance list is: " + instances.size());
	System.out.println("Loading data instances...");
	
	// Sets the frequent with which top words for each topic are printed
	//model.setShowTopicsInterval(config.getTopicInterval(LDAConfiguration.TOPIC_INTER_DEFAULT));
	model.setRandomSeed(config.getSeed(LDAConfiguration.SEED_DEFAULT));
	System.out.println("Config seed:" + config.getSeed(LDAConfiguration.SEED_DEFAULT));
	System.out.println("Start seed: " + model.getStartSeed());
	// Imports the data into the model
	model.addInstances(instances);
	
	System.out.println("Starting iterations (" + config.getNoIterations(LDAConfiguration.NO_ITER_DEFAULT) + " total).");
	System.out.println("_____________________________\n");
	
	// Runs the model
	System.out.println("Starting:" + new Date());
	Timer t = new Timer();
	t.start();
	model.sample(config.getNoIterations(LDAConfiguration.NO_ITER_DEFAULT));
	t.stop();
	System.out.println("Finished:" + new Date());

	List<String> metadata = new ArrayList<String>();
	metadata.add("No. Topics: " + model.getNoTopics());
	// Save stats for this run
	lu.dynamicLogRun("Runs", t, cp, (Configuration) config, null, 
			ParallelLDA.class.getName(), "Convergence", "HEADING", "PLDA", 1, metadata);
	File lgDir = lu.getLogDir();
	PrintWriter out = new PrintWriter(lgDir.getAbsolutePath() + "/TopWords.txt");
	out.println(LDAUtils.formatTopWords(model.getTopWords(20)));
	out.flush();
	out.close();
	
	System.out.println("Top words are: \n" + LDAUtils.formatTopWords(model.getTopWords(20)));
	
	System.out.println("I am done!");
  }

  public static class Terminator extends UntypedActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private final ActorRef ref;

    public Terminator(ActorRef ref) {
      this.ref = ref;
      getContext().watch(ref);
    }

    @Override
    public void onReceive(Object msg) {
      if (msg instanceof Terminated) {
        log.info("{} has terminated, shutting down system", ref.path());
        getContext().system().shutdown();
      } else {
        unhandled(msg);
      }
    }

  }
}