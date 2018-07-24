package cc.mallet.util;

import java.net.URI;
import java.util.Iterator;

import cc.mallet.types.Instance;

/**
 * Simple Iterator for string data where you can supply the class it
 * belongs to
 * 
 * @author Leif Jonsson
 *
 */
public class StringClassArrayIterator implements Iterator<Instance> {

	String[] data;
	int index = 0;
	String [] classNames; 

	public StringClassArrayIterator (String[] data)
	{
		this.data = data;
		this.index = 0;
		this.classNames = new String[]{"no_class"};
	}
	
	public StringClassArrayIterator (String[] data, String className)
	{
		this.data = data;
		this.index = 0;
		this.classNames = new String[]{className};
	}
	
	public StringClassArrayIterator (String[] data, String [] classNames)
	{
		if(classNames.length != 1 && data.length != classNames.length) {
			throw new IllegalArgumentException("data.length != classNames.length when classNames.length != 1");
		}
		this.data = data;
		this.index = 0;
		this.classNames = classNames;
	}

	public Instance next ()
	{
		URI uri = null;
		try { uri = new URI ("array:" + index); }
		catch (Exception e) { e.printStackTrace(); throw new IllegalStateException(); }
		int cl_idx = classNames.length == 1 ? 0 : index;
		Instance i = new Instance (data[index], classNames[cl_idx], uri, null);
		index++;
		return i;
	}

	public boolean hasNext ()	{	return index < data.length;	}
	
	public void remove () {
		throw new IllegalStateException ("This Iterator<Instance> does not support remove().");
	}

}