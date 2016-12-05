package cc.mallet.topics.distributed;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
import cc.mallet.types.InstanceList;
import cc.mallet.util.LDAUtils;
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
		String activeConf = config.getSubConfigs()[0];
		config.activateSubconfig(activeConf);
		
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

		InstanceList instances;
		if(config.getTfIdfVocabSize(LDAConfiguration.TF_IDF_VOCAB_SIZE_DEFAULT)>0) {
			instances = LDAUtils.loadInstancesKeep(dataset_fn, 
					config.getStoplistFilename("stoplist.txt"), config.getTfIdfVocabSize(LDAConfiguration.TF_IDF_VOCAB_SIZE_DEFAULT), config.keepNumbers());					
		} else {					
			instances = LDAUtils.loadInstancesPrune(dataset_fn, 
					config.getStoplistFilename("stoplist.txt"), config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD), config.keepNumbers());
		}

		List<ActorRef> samplerNodes = new ArrayList<>();
		List<ActorRef> samplerCores = new ArrayList<>();
		String [] machines = config.getRemoteWorkerMachines();
		int[] cores = config.getRemoteWorkerCores();
		int port = config.getRemoteWorkerPort(LDARemoteConfiguration.REMOTE_PORT_DEFAULT);
		Map<ActorRef,Integer> nodeCoreMapping = new HashMap<ActorRef,Integer>(); 
		for(int i = 0; i < machines.length; i++) {
			String machine = machines[i];
			
			String remoteRef = "akka.tcp://" + REMOTE_WORKER_NAME + "@" + machine + ":" + port + "/user/router";

			ActorRef remote = findActor(system, inbox, -1, machine, port, remoteRef);
			System.out.println("Master added: " + remote);
			system.actorOf(Props.create(Terminator.class, remote), "remote-terminator-" + i);
			samplerNodes.add(remote);

			int noCoresOnMachine = cores[i];
			nodeCoreMapping.put(remote, noCoresOnMachine);
			
			for (int core = 0; core < noCoresOnMachine; core++) {		
				String actorRef = "akka.tcp://" + REMOTE_WORKER_NAME + "@" + machine + ":" + port + "/user/remote" + core;		
				ActorRef a = findActor(system, inbox, core, machine, port, actorRef);
				System.out.println("Master added: " + a);
				system.actorOf(Props.create(Terminator.class, a), "terminator-" + i+ "-" + core);
				samplerCores.add(a);
			}
		}

		LDAGibbsSampler model = new DistributedSpaliasUncollapsedSampler(config, samplerNodes, samplerCores, nodeCoreMapping, inbox);
		System.out.println(
				String.format("DistSpalias (%d batches).", 
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
				SpaliasMainRemote.class.getName(), "Convergence", "HEADING", "PLDA", 1, metadata);
		File lgDir = lu.getLogDir();
		PrintWriter out = new PrintWriter(lgDir.getAbsolutePath() + "/TopWords.txt");
		String topWords = LDAUtils.formatTopWordsAsCsv(
				LDAUtils.getTopRelevanceWords(20, 
						model.getAlphabet().size(), 
						model.getNoTopics(), 
						model.getTypeTopicMatrix(),  
						config.getBeta(LDAConfiguration.BETA_DEFAULT),
						config.getLambda(LDAConfiguration.LAMBDA_DEFAULT), 
						model.getAlphabet()));
		out.println(topWords);
		out.flush();
		out.close();

		System.out.println("Top words are: \n" + topWords);

		System.out.println("I am done!");
	}

	static ActorRef findActor(ActorSystem system, final Inbox inbox, int i, String machine, int port, String actorRef) {
		ActorSelection sel = system.actorSelection(actorRef);
		final String identifyId = "" + i;
		sel.tell(new Identify(identifyId), inbox.getRef());
		Object message;
		try {
			message = inbox.receive(Duration.create(10, TimeUnit.MINUTES));
		} catch (TimeoutException e) {
			throw new IllegalArgumentException("Cound not find actor: " + actorRef + " (in time)!");
		}
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