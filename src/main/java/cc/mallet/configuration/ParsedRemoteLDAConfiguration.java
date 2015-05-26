package cc.mallet.configuration;

import java.net.UnknownHostException;
import java.util.Arrays;

import org.apache.commons.configuration.ConfigurationException;


public class ParsedRemoteLDAConfiguration extends ParsedLDAConfiguration implements Configuration, LDARemoteConfiguration {

	private static final long serialVersionUID = 1L;

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

	@Override
	public int getRemoteWorkerPort(int defaultValue) {
		return getInteger("remote_worker_port", defaultValue);
	}

	@Override
	public String getAkkaMasterConfig() {
		String config = "akka {\n"
				+ "			  loglevel = \"INFO\"\n"
				+ "			  actor {\n"
				+ "			    provider = \"akka.remote.RemoteActorRefProvider\"\n"
				+ "			  }\n"
				+ "			  remote {\n"
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
		String config = "akka {\n"
				+ "			  loglevel = \"INFO\"\n"
				+ "			  actor {\n"
				+ "			    provider = \"akka.remote.RemoteActorRefProvider\"\n"
				+ "			  }\n"
				+ "			  remote {\n"
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

	@Override
	public String getRemoteMaster() {
		return getStringProperty("remote_master");
	}

}
