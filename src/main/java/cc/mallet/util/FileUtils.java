package cc.mallet.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {
	
	public static String[] readLines(String filename) throws IOException {
        FileReader fileReader = null;
        try {
        fileReader = new FileReader(filename);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        List<String> lines = new ArrayList<String>();
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            lines.add(line);
        }
        bufferedReader.close();
        return lines.toArray(new String[lines.size()]);
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (fileReader != null) {
				try {
					fileReader.close();
				} catch (IOException e) {
				}
			}
		}
    }

	/**
	 * Assumes a text file so that readLine makes sense
	 * The resulting string will end in a new line even if the original does not
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static String readTextFile(String file) throws IOException {
		FileReader f = null;
		try {
			f = new FileReader (file);
			BufferedReader reader = new BufferedReader(f);
			String         line = null;
			StringBuilder  stringBuilder = new StringBuilder();
			String         ls = System.getProperty("line.separator");

			while( ( line = reader.readLine() ) != null ) {
				stringBuilder.append( line );
				stringBuilder.append( ls );
			}
			reader.close();
			return stringBuilder.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (file != null) {
				try {
					f.close();
				} catch (IOException e) { 
				}
			}
		}
	}

	/**
	 * Reads a file as bytes and converts the result to a string
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static String readFileAsString(String filename) throws IOException {
		File file = new File(filename);
		
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		byte[] readData = null;
		try (FileInputStream fis = new FileInputStream(file)) {
			int nRead;
			byte[] data = new byte[16384];

			while ((nRead = fis.read(data, 0, data.length)) != -1) {
				buffer.write(data, 0, nRead);
			}
			fis.close();
			buffer.flush();
			readData = buffer.toByteArray();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		return new String(readData, 0, readData.length, "UTF-8");
	}
	
	/**
	 * @param filename
	 * @param text
	 * @throws Exception
	 */
	public static void saveToFile(String filename, String text) throws IOException {
		BufferedWriter writer = null;
		try
		{
		    writer = new BufferedWriter(new FileWriter(filename));
		    writer.write(text);
		}
		finally
		{
		    try
		    {
		        if ( writer != null)
		        writer.close( );
		    }
		    catch ( IOException e)
		    {
		    	throw new RuntimeException(e);
		    }
		}
	}

	/**
	 * @param filename
	 * @param text
	 * @throws Exception
	 */
	public static void saveToFile(File file, String text) throws IOException {
		BufferedWriter writer = null;
		try
		{
		    writer = new BufferedWriter(new FileWriter(file));
		    writer.write(text);
		    writer.flush();
		}
		finally
		{
		    try
		    {
		        if ( writer != null)
		        writer.close( );
		    }
		    catch ( IOException e)
		    {
		    	throw new RuntimeException(e);
		    }
		}
	}

	
	@SuppressWarnings("resource") // Will be closed in the filnally...
	public static void copyFile(File sourceFile, File destFile) throws IOException {
	    if(!destFile.exists()) {
	        if(!destFile.createNewFile()) throw new IllegalArgumentException("File "+destFile.getName()+" was suddenly created behind my back!?");
	    }

	    FileChannel source = null;
	    FileChannel destination = null;
	    try {
	        source = new FileInputStream(sourceFile).getChannel();
	        destination = new FileOutputStream(destFile).getChannel();
	        long count = 0;
	        long size = source.size();              
	        while((count += destination.transferFrom(source, count, size-count))<size);
            source.close();
            destination.close();
	    }
	    finally {
	        if(source != null) {
	            source.close();
	        }
	        if(destination != null) {
	            destination.close();
	        }
	    }
	}
	
}
