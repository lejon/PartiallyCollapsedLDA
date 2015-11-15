package cc.mallet.configuration;


public interface LDARemoteConfiguration extends LDAConfiguration {
	
	public static final int REMOTE_PORT_DEFAULT = 5150;
	
	public String[] getRemoteWorkerMachines();

	public String getRemoteMaster();
	
	public int[] getRemoteWorkerCores();
	
	public int[] getRemoteWorkerPorts(int defaultValue);
	
	public int getRemoteWorkerPort(int defaultValue);
	
	public String getAkkaMasterConfig();
	
	public String getAkkaWorkerConfig();
	
	public Boolean getSendPartials();
	
}