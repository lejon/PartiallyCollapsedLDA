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
	int index;
	String className; 
	
	public StringClassArrayIterator (String[] data, String className)
	{
		this.data = data;
		this.index = 0;
		this.className = className;
	}

	public Instance next ()
	{
		URI uri = null;
		try { uri = new URI ("array:" + index); }
		catch (Exception e) { e.printStackTrace(); throw new IllegalStateException(); }
		return new Instance (data[index++], className, uri, null);
	}

	public boolean hasNext ()	{	return index < data.length;	}
	
	public void remove () {
		throw new IllegalStateException ("This Iterator<Instance> does not support remove().");
	}

}