package cc.mallet.configuration;

import java.net.UnknownHostException;
import java.util.Arrays;

import org.apache.commons.configuration.ConfigurationException;


public class ParsedRemoteLDAConfiguration extends ParsedLDAConfiguration implements Configuration, LDARemoteConfiguration {

	private static final long serialVersionUID = 1L;
	
	protected int DEFAULT_WIRE_COMPRESSION_LEVEL = 5;

	public ParsedRemoteLDAConfiguration(LDACommandLineParser cp) throws ConfigurationException {
		super(cp);
	}

	public ParsedRemoteLDAConfiguration(String path) throws ConfigurationException {
		super(path);
	}

	/* (non-Javadoc)
	 * @see configuration.LDAConfiguration#getNoBatches(int)
	 */
	@Override
	public Integer getNoBatches(int defaultValue) {
		int batches = 0;
		int [] remoteCores = getRemoteWorkerCores();
		// If cores are not set
		if(remoteCores==null||remoteCores.length==0) {
			String [] remoteMachines = getRemoteWorkerMachines();
			// and no remote machines, return default
			if(remoteMachines==null||remoteMachines.length==0) {
				return defaultValue;
				// else assume one core per remote machine
			} else {
				return remoteMachines.length;
			}
		}
		// Otherwise sum the number of cores on the remote machines
		for (int i = 0; i < remoteCores.length; i++) {
			batches += remoteCores[i];
		}
		return batches;
	}

	@Override
	public String[] getRemoteWorkerMachines() {
		return getStringArrayProperty("remote_worker_machines");
	}

	@Override
	public int[] getRemoteWorkerCores() {
		int [] defaultVal = {};
		return getIntArrayProperty("remote_worker_cores", defaultVal);
	}

	@Override
	public int[] getRemoteWorkerPorts(int defaultValue) {
		int noMachines = getRemoteWorkerMachines().length;
		int [] defaultVal = new int[noMachines];
		int port = getRemoteWorkerPort(defaultValue);
		Arrays.fill(defaultVal, port);
		return getIntArrayProperty("remote_worker_ports", defaultVal);
	}
	
	boolean getUseWireCompression() {
		return getBooleanProperty("use_wire_compression");
	}

	public Boolean getSendPartials() {
		return getBooleanPropertyOrNull("send_partials");
	}

	int getWireCompressionLevel() {
		return getInteger("wire_compression_level", DEFAULT_WIRE_COMPRESSION_LEVEL);
	}

	@Override
	public String getRemoteMaster() {
		return getStringProperty("remote_master");
	}

	@Override
	public int getRemoteWorkerPort(int defaultValue) {
		return getInteger("remote_worker_port", defaultValue);
	}

	public int getInboxSize(int defaultValue) {
		return getInteger("inbox_size", defaultValue);
	}

	@Override
	public String getAkkaMasterConfig() {
		String compression = "";
		if(getUseWireCompression()) {			
			compression = "			  compression-scheme = \"zlib\" # Options: \"zlib\" (lzf to come), leave out for no compression\n"
					    + "			  zlib-compression-level = " + getWireCompressionLevel() + "  # Options: 0-9 (1 being fastest and 9 being the most compressed), default is " + DEFAULT_WIRE_COMPRESSION_LEVEL;
			System.err.println("USING WIRE COMPRESSION: Level = "+ getWireCompressionLevel() + "!!");
		} else {
			System.err.println("NOT - USING WIRE COMPRESSION!!");
		}

		String config = "akka {\n"
				+ "			  loglevel = \"INFO\"\n"
				+ "			  actor {\n"
				+ "			    provider = \"akka.remote.RemoteActorRefProvider\"\n"
				+ "           # Configuration items which are used by the akka.actor.ActorDSL._ methods\n"
				+ "           dsl {\n"
				+ "             # Maximum queue size of the actor created by newInbox(); this protects\n"
				+ "             # against faulty programs which use select() and consistently miss messages\n"
				+ "             inbox-size = " + getInboxSize(1000) + "\n"
				+ ""           
				+ "             # Default timeout to assume for operations like Inbox.receive et al\n"
				+ "             default-timeout = 5s\n"
				+ "           }\n"
				+ "			  }\n"
				+ "			  remote {\n"
				+             compression
				+ "			    enabled-transports = [\"akka.remote.netty.tcp\"]\n"
				+ "			    netty.tcp {\n"
				+ "			      hostname = \"" + getRemoteMaster() + "\"\n"
				+ "			      port = 0\n"
				+ "			      maximum-frame-size = 128000m\n"
				+ "			    }\n"
				+ "			    log-sent-messages = on\n"
				+ "			    log-received-messages = on\n"
				+ "			  }\n"
				+ "			}\n";
		return config;
	}

	@Override
	public String getAkkaWorkerConfig() {
		String fullHostName;
		try {
			//fullHostName = java.net.InetAddress.getLocalHost().getCanonicalHostName();
			fullHostName = java.net.InetAddress.getLocalHost().getHostAddress().toString();
		} catch (UnknownHostException e) {
			throw new IllegalStateException("Unable to determine own hostname.");
		}
		String compression = "";
		if(getUseWireCompression()) {			
			compression = "			  compression-scheme = \"zlib\" # Options: \"zlib\" (lzf to come), leave out for no compression\n"
					    + "			  zlib-compression-level = " + getWireCompressionLevel() + "  # Options: 0-9 (1 being fastest and 9 being the most compressed), default is " + DEFAULT_WIRE_COMPRESSION_LEVEL;
			System.err.println("USING WIRE COMPRESSION: Level = "+ getWireCompressionLevel() + "!!");
		} else {
			System.err.println("NOT - USING WIRE COMPRESSION!!");
		}

		String config = "akka {\n"
				+ "			  loglevel = \"INFO\"\n"
				+ "			  actor {\n"
				+ "			    provider = \"akka.remote.RemoteActorRefProvider\"\n"
				+ "			  }\n"
				+ "			  remote {\n"
				+             compression
				+ "			    enabled-transports = [\"akka.remote.netty.tcp\"]\n"
				+ "			    netty.tcp {\n"
				+ "			      hostname = \"" + fullHostName + "\"\n"
				+ "			      port = " + getRemoteWorkerPort(LDARemoteConfiguration.REMOTE_PORT_DEFAULT) + "\n"
				+ "			      maximum-frame-size = 128000m\n"
				+ "			    }\n"
				+ "			    log-sent-messages = on\n"
				+ "			    log-received-messages = on\n"
				+ "			  }\n"
				+ "			}\n";
		return config;
	}
}
