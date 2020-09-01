package cc.mallet.util;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.junit.Test;

public class LogginUtilsTest {

	@SuppressWarnings("resource")
	@Test
	public void testCreation() throws FileNotFoundException {
		new NullPrintWriter();
	}

	@Test
	public void testWriting() throws FileNotFoundException {
		LDALoggingUtils lu = new LDANullLogger();
		PrintWriter pw = lu.checkCreateAndCreateLogPrinter(
				lu.getLogDir() + "/timing_data",
				"thr_Phi_sampling.txt");
		pw.println("before" + "," + 20000);
		pw.flush();
		pw.close();
	}

}
