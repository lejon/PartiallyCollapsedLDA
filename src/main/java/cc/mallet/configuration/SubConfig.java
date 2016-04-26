package cc.mallet.configuration;

import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;

public class SubConfig extends HierarchicalINIConfiguration {

	private static final long serialVersionUID = 1L;
	protected String subConfName = null;
	protected String whereAmI;
	protected String configFn = null;
	protected String fullPath = null;
	protected LDACommandLineParser commandlineParser = null;

	public SubConfig(String configFn) throws ConfigurationException {
		super(configFn);
		whereAmI = configFn;
	}

	public void activateSubconfig(String subConfName) {
		boolean foundIt = false;
		String [] configs = super.getStringArray("configs");
		for( String cfg : configs ) {
			cfg = cfg.trim();
			if( subConfName.equals(cfg) ) {
				foundIt = true;
			}
		}
		if( !foundIt ) {
			throw new IllegalArgumentException("No such configuration: " + subConfName);
		}
		this.subConfName = subConfName; 
	}

	public void forceActivateSubconfig(String subConfName) {
		this.subConfName = subConfName; 
	}

	public String getActiveSubConfig() {
		return subConfName;
	}
	
	public void setProperty(String key, Object value) {
		super.setProperty(translateKey(key), value);
	}

	/**
	 * First check if we are in a subconfig and have the key, if so return it. 
	 * Else check if the key is in the global scope, if so return it.
	 * Else if we are in a subconfig scope, return the subconfig key otherwise 
	 * just return the key.
	 * @param key
	 * @return
	 */
	protected String translateKey(String key) {
		if(subConfName!=null && containsKey(subConfName + "." + key)) {
			return subConfName + "." + key;
		} else if (containsKey(key)) {
			return key;
		} else if(subConfName!=null) {
			return subConfName + "." + key;
		} else {
			return key;
		}
	}

	protected String [] trimStringArray(String [] toTrim) {
		for (int i = 0; i < toTrim.length; i++) {
			toTrim[i] = toTrim[i].trim();
		}
		return toTrim;
	}

	public String [] getStringArrayProperty(String key) {
		return trimStringArray(super.getStringArray(translateKey(key)));
	}

	public int [] getIntArrayProperty(String key, int [] defaultValues) {
		String [] ints = super.getStringArray(translateKey(key));
		if(ints==null || ints.length==0) {
			//throw new IllegalArgumentException("Could not find any int array for key:" + translateKey(key));
			return defaultValues;
		}
		int [] result = new int[ints.length];
		for (int i = 0; i < ints.length; i++) {
			result[i] = Integer.parseInt(ints[i].trim());
		}
		return result;
	}
	
	public String getStringProperty(String key) {
		if(commandlineParser!= null && commandlineParser.hasOption(key) && commandlineParser.getOption(key)!=null) {
			return commandlineParser.getOption(key);
		} else {
			// This hack lets us have "," in strings
			String strProp = "";
			Object prop = super.getProperty(translateKey(key));
			if(prop instanceof java.util.List) {
				@SuppressWarnings("unchecked")
				List<String> propParts = (List<String>) prop;
				for (String string : propParts) {
					strProp += string + ",";
				}
				strProp = strProp.substring(0, strProp.length()-1);
			} else {
				strProp = (String) prop;
			}
			return strProp;
		}
	}
	
	public boolean configHasProperty(String key) {
		return super.getProperty(translateKey(key))!=null;
	}

	public boolean configOrCmdLineHasProperty(String key) {
		return getStringProperty(key)!=null;
	}

	public Object getConfProperty(String key) {
		return super.getProperty(translateKey(key));
	}

	@Override
	public boolean getBoolean(String key) {
		return super.getBoolean(translateKey(key));
	}

	@Override
	public boolean getBoolean(String key, boolean defaultValue) {
		try {
			return getBoolean(key);
		} catch (Exception e) {
			return false;
		}
	}

	public String[] getSubConfigs() {
		return trimStringArray(super.getStringArray("configs"));
	}

	public Integer getInteger(String key, Integer defaultValue) {
		if(commandlineParser != null && commandlineParser.hasOption(key) && commandlineParser.getOption(key)!=null) {
			return Integer.parseInt(commandlineParser.getOption(key.trim()));
		} else {
			return super.getInteger(translateKey(key),defaultValue);
		}
	}

	public Double getDouble(String key, Double defaultValue) {
		if(commandlineParser!= null && commandlineParser.hasOption(key) && commandlineParser.getOption(key)!=null) {
			return Double.parseDouble(commandlineParser.getOption(key.trim()));
		} else {
			return super.getDouble(translateKey(key),defaultValue);
		}
	}

	public String whereAmI() {
		return whereAmI;
	}

	public String getOption(String key) {
		return commandlineParser.getOption(key);
	}

	public void setConfigFn(String configFn) {
		this.configFn = configFn;
	}

	public String getFullPath() {
		return fullPath;
	}

	public void setFullPath(String fullPath) {
		this.fullPath = fullPath;
	}
	
	
	/**
	 * This method returns null if the property is not set in the config file
	 * 
	 * @param key
	 * @return null if not set, otherwise value from config file
	 */
	public Boolean getBooleanPropertyOrNull(String key) {
		String stringProperty = getStringProperty(key);
		if(stringProperty==null) return null;
		return (stringProperty.trim().equalsIgnoreCase("true") || stringProperty.trim().equalsIgnoreCase("yes") || stringProperty.trim().equals("1"));
	}
	
	public boolean hasBooleanProperty(String key) {
		String stringProperty = getStringProperty(key);
		return (stringProperty!=null) && (stringProperty.trim().equalsIgnoreCase("true") || stringProperty.trim().equalsIgnoreCase("yes") || stringProperty.trim().equals("1"));
	}

	public boolean getBooleanProperty(String key) {
		String stringProperty = getStringProperty(key);
		return (stringProperty!=null) && (stringProperty.trim().equalsIgnoreCase("true") || stringProperty.trim().equalsIgnoreCase("yes") || stringProperty.trim().equals("1"));
	}
	
	public double [] getDoubleArrayProperty(String key) {
		String [] ints = super.getStringArray(translateKey(key));
		if(ints==null || ints.length==0) { 
			throw new IllegalArgumentException("Could not find any double array for key:" 
					+ translateKey(key)); 
		}
		double [] result = new double[ints.length];
		for (int i = 0; i < ints.length; i++) {
			result[i] = Double.parseDouble(ints[i].trim());
		}
		return result;
	}
}
