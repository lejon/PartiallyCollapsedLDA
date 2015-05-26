package cc.mallet.configuration;

public interface Configuration extends org.apache.commons.configuration.Configuration{
	public String [] getSubConfigs();
	public String getActiveSubConfig();
	public String whereAmI();
	public String [] getStringArrayProperty(String key);
	public int [] getIntArrayProperty(String key,int [] defaultValues);
	public String getStringProperty(String key);
	Object getConfProperty(String key);
	void activateSubconfig(String subConfName);
}
