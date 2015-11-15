package cc.mallet.topics.distributed;

import java.util.ArrayList;
import java.util.List;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.routing.ActorRefRoutee;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;
import cc.mallet.configuration.LDACommandLineParser;
import cc.mallet.configuration.LDARemoteConfiguration;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class SpaliasRemoteRouter extends UntypedActor {

	static Router router;	
	//private final ActorRef proxy;
	
	/*public SpaliasRemoteRouter(ActorPath targetPath) {
		proxy = getContext().actorOf(ReliableProxy.props(targetPath, Duration.create(100, TimeUnit.MILLISECONDS)));
	}*/
	
	@Override
	public void onReceive(Object msg) throws Exception {
		if (msg instanceof Terminated) {
//			router = router.removeRoutee(((Terminated) msg).actor());
//			ActorRef r = getContext().actorOf(Props.create(Worker.class));
//			getContext().watch(r);
//			router = router.addRoutee(new ActorRefRoutee(r));
			throw new IllegalStateException("SpaliasRemoteRouter: NOOOOO Terminated!");
		} else {
			System.out.println("Router forwarding request: " + msg);
			router.route(msg, getSender());
			//proxy.tell(msg, getSelf());
		}
	}
	
	public static void main(String[] args) throws Exception {
		SpaliasRemoteRouter.start(args);
	}
		
	public static void start(String[] args) throws Exception {
		// Reading in command line parameters		
		LDACommandLineParser cp = new LDACommandLineParser(args);
		LDARemoteConfiguration ldaConfig = (LDARemoteConfiguration) cc.mallet.configuration.ConfigFactory.getMainRemoteConfiguration(cp);
		String activeConf = ldaConfig.getSubConfigs()[0];
		ldaConfig.activateSubconfig(activeConf);
		//int noWorkers = ldaConfig.getNoBatches(LDAConfiguration.NO_BATCHES_DEFAULT);
		int noWorkers = Runtime.getRuntime().availableProcessors();
		System.out.println("Akka worker config: " + ldaConfig.getAkkaWorkerConfig());
		//Config config = ConfigFactory.parseFile(new File("src/main/resources/SpaliasWorkerRemote.conf"));
		Config config = ConfigFactory.parseString(ldaConfig.getAkkaWorkerConfig());
		ActorSystem system = ActorSystem.create(SpaliasMainRemote.REMOTE_WORKER_NAME,config);
			
		ActorRef theRouter = system.actorOf(Props.create(SpaliasRemoteRouter.class), "router");
		List<Routee> routees = new ArrayList<Routee>();
		for (int i = 0; i < noWorkers; i++) {
			//ReliableProx
			ActorRef me = system.actorOf(Props.create(SpaliasDocumentSampler.class), "remote" + i);	
			routees.add(new ActorRefRoutee(me));
			System.out.println("Actor is: " + me);
		}
		//router = new Router(new RoundRobinRoutingLogic(), routees);
		router = new Router(new BroadcastRoutingLogic(), routees);
	}

}
