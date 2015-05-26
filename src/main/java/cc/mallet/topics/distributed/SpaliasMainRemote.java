package cc.mallet.topics.distributed;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import scala.concurrent.duration.Duration;
import akka.actor.ActorIdentity;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Identify;
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
import cc.mallet.configuration.LDARemoteConfiguration;
import cc.mallet.topics.LDAGibbsSampler;
import cc.mallet.topics.LDAUtils;
import cc.mallet.topics.tui.ParallelLDA;
import cc.mallet.types.InstanceList;
import cc.mallet.util.LoggingUtils;
import cc.mallet.util.Timer;

public class SpaliasMainRemote {

	public static final String REMOTE_WORKER_NAME = "RemoteLDADocumentSampler";

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

		// Reading in command line parameters		
		LDACommandLineParser cp = new LDACommandLineParser(args);
		LDARemoteConfiguration config = (LDARemoteConfiguration) ConfigFactory.getMainRemoteConfiguration(cp);
		LoggingUtils lu = new LoggingUtils();
		lu.checkAndCreateCurrentLogDir("Runs");
		config.setLoggingUtil(lu);
		config.activateSubconfig("DistPLDA");
		
		//com.typesafe.config.Config masterConfig = com.typesafe.config.ConfigFactory.parseFile(new File("src/main/resources/SpaliasMainRemote.conf"));
		com.typesafe.config.Config masterConfig = com.typesafe.config.ConfigFactory.parseString(config.getAkkaMasterConfig());
		System.out.println("Master config: " + masterConfig);

		ActorSystem system = ActorSystem.create("SpaliasDistributedLDASampler",masterConfig);
		final Inbox inbox = Inbox.create(system);

		System.out.println("Using Config: " + config.whereAmI());

		String dataset_fn = config.getDatasetFilename();
		System.out.println("Using dataset: " + dataset_fn);
		String whichModel = config.getScheme();
		System.out.println("Scheme: " + whichModel);

		InstanceList instances = LDAUtils.loadInstances(dataset_fn, 
				"stoplist.txt", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));

		List<ActorRef> samplers = new ArrayList<>();
		String [] machines = config.getRemoteWorkerMachines();
		int[] cores = config.getRemoteWorkerCores();
		int port = config.getRemoteWorkerPort(LDARemoteConfiguration.REMOTE_PORT_DEFAULT);
		for(int i = 0; i < machines.length; i++) {
			String machine = machines[i];
			int noCoresOnMachine = cores[i];
			for (int core = 0; core < noCoresOnMachine; core++) {				
				ActorRef a = findActor(system, inbox, core, machine, port);
				System.out.println("Master added: " + a);
				system.actorOf(Props.create(Terminator.class, a), "terminator-" + i+ "-" + core);
				samplers.add(a);
			}
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

	static ActorRef findActor(ActorSystem system, final Inbox inbox, int i, String machine, int port) {
		String actorRef = "akka.tcp://" + REMOTE_WORKER_NAME + "@" + machine + ":" + port + "/user/remote" + i;
		ActorSelection sel = system.actorSelection(actorRef);
		final String identifyId = "" + i;
		sel.tell(new Identify(identifyId), inbox.getRef());
		Object message = inbox.receive(Duration.create(10, TimeUnit.MINUTES));
		System.out.println("Master got reply: " + message);

		ActorRef ref = null;
		if (message instanceof ActorIdentity) {
			ActorIdentity identity = (ActorIdentity) message;
			if (identity.correlationId().equals(identifyId)) {
				ref = identity.getRef();
				if (ref == null) {
					throw new IllegalArgumentException("Could not find worker: " + actorRef);
				}
				else {
					System.out.println("Reference does not match: Looked for: " + actorRef + " got: " + ref);
					//throw new IllegalArgumentException("Reference does not match: Looked for: " + actorRef + " got: " + ref);
				}
			}
		} else if (message instanceof Terminated) {
			//final Terminated t = (Terminated) message;
			System.out.println("Actor terminated!");
		}
		return ref;
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