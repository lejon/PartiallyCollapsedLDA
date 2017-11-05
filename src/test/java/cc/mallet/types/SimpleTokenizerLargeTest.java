package cc.mallet.types;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;

import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.Test;

import cc.mallet.configuration.ConfigFactory;
import cc.mallet.configuration.LDACommandLineParser;
import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.configuration.ParsedLDAConfiguration;
import cc.mallet.pipe.SimpleTokenizerLarge;
import cc.mallet.util.LDAUtils;

public class SimpleTokenizerLargeTest {
	int BS = 100000;
	String fn = "stoplist.txt";

	@Test
	public void test() {
		SimpleTokenizerLarge stl = new SimpleTokenizerLarge(new File(fn), BS);
		assertEquals(BS,stl.getTokenBufferSize());	
	}
	
	@Test
	public void testNullArg() {
		File fl = null;
		SimpleTokenizerLarge stl = new SimpleTokenizerLarge(fl, BS);
		assertEquals(BS,stl.getTokenBufferSize());	
	}

	@Test
	public void testIntegration() throws ConfigurationException, ParseException {
		ConfigFactory.setMainConfiguration(null);
		String [] args = {"--run_cfg=src/test/resources/max_doc_buf.cfg"};
		
		LDACommandLineParser cp = new LDACommandLineParser(args);
		ParsedLDAConfiguration config = (ParsedLDAConfiguration) ConfigFactory.getMainConfiguration(cp);
		config.activateSubconfig("large_wiki_random_100_spalias_cores_16_seed_4711");
		String stlfn = config.getStoplistFilename("stoplist.txt");
		SimpleTokenizerLarge stl = new SimpleTokenizerLarge(new File(stlfn), config.getMaxDocumentBufferSize(10));
		assertEquals(471100,stl.getTokenBufferSize());	
	}
	
	@Test
	public void testIntegrationTfIdfPrune() throws ConfigurationException, ParseException, FileNotFoundException {
		ConfigFactory.setMainConfiguration(null);
		String [] args = {"--run_cfg=src/test/resources/max_doc_buf-small.cfg"};
		
		LDACommandLineParser cp = new LDACommandLineParser(args);
		ParsedLDAConfiguration config = (ParsedLDAConfiguration) ConfigFactory.getMainConfiguration(cp);
		config.activateSubconfig("tf_idf_prune");
		
		assertEquals(10,config.getMaxDocumentBufferSize(LDAConfiguration.MAX_DOC_BUFFFER_SIZE_DEFAULT));

		assertEquals(7700,(int)config.getTfIdfVocabSize(LDAConfiguration.TF_IDF_VOCAB_SIZE_DEFAULT));
		try {
			if(config.getTfIdfVocabSize(LDAConfiguration.TF_IDF_VOCAB_SIZE_DEFAULT)>0) {
				LDAUtils.loadInstancesKeep(config.getDatasetFilename(), 
						config.getStoplistFilename("stoplist.txt"), config.getTfIdfVocabSize(LDAConfiguration.TF_IDF_VOCAB_SIZE_DEFAULT), config.keepNumbers(), config.getMaxDocumentBufferSize(LDAConfiguration.MAX_DOC_BUFFFER_SIZE_DEFAULT), false, null);					
			} else {
				fail("TF-IDF was not > 0");
			}
		} catch(java.lang.ArrayIndexOutOfBoundsException aiob) {
			return;
		}
		fail("Test did not throw ArrayIndexOutOfBoundsException");
		
		LDAUtils.loadInstancesKeep(config.getDatasetFilename(), 
				config.getStoplistFilename("stoplist.txt"), config.getTfIdfVocabSize(LDAConfiguration.TF_IDF_VOCAB_SIZE_DEFAULT), config.keepNumbers(), 10000, false, null);
	}
	
	@Test
	public void testSpecialChars() throws ConfigurationException, ParseException, FileNotFoundException {
		ConfigFactory.setMainConfiguration(null);
		String [] args = {"--run_cfg=src/test/resources/special_chars.cfg"};
		
		LDACommandLineParser cp = new LDACommandLineParser(args);
		ParsedLDAConfiguration config = (ParsedLDAConfiguration) ConfigFactory.getMainConfiguration(cp);
		config.activateSubconfig("special");

		InstanceList instances = LDAUtils.loadInstancesKeep(config.getDatasetFilename(), 
				null, config.getTfIdfVocabSize(LDAConfiguration.TF_IDF_VOCAB_SIZE_DEFAULT), config.keepNumbers(), config.getMaxDocumentBufferSize(LDAConfiguration.MAX_DOC_BUFFFER_SIZE_DEFAULT), false, null);
		
		Alphabet alphabet = instances.getDataAlphabet();
		assertTrue("Alphabet does not contain: 'but_i_can'",!alphabet.contains("but_i_can"));
		
		instances = LDAUtils.loadInstancesKeep(config.getDatasetFilename(), 
				null, config.getTfIdfVocabSize(LDAConfiguration.TF_IDF_VOCAB_SIZE_DEFAULT), config.keepNumbers(), config.getMaxDocumentBufferSize(LDAConfiguration.MAX_DOC_BUFFFER_SIZE_DEFAULT), true, null);
		
		alphabet = instances.getDataAlphabet();
		assertTrue("Alphabet does not contain: 'but_i_can'",alphabet.contains("but_i_can"));

	}
	
	@Test
	public void testIntegrationRareWordPrune() throws ConfigurationException, ParseException, FileNotFoundException {
		ConfigFactory.setMainConfiguration(null);
		String [] args = {"--run_cfg=src/test/resources/max_doc_buf-small.cfg"};
		
		LDACommandLineParser cp = new LDACommandLineParser(args);
		ParsedLDAConfiguration config = (ParsedLDAConfiguration) ConfigFactory.getMainConfiguration(cp);
		config.forceActivateSubconfig("rare_word_prune");
		
		assertEquals(10,config.getMaxDocumentBufferSize(LDAConfiguration.MAX_DOC_BUFFFER_SIZE_DEFAULT));

		assertEquals(LDAConfiguration.TF_IDF_VOCAB_SIZE_DEFAULT, (int) config.getTfIdfVocabSize(LDAConfiguration.TF_IDF_VOCAB_SIZE_DEFAULT));
		try {
			if(config.getTfIdfVocabSize(LDAConfiguration.TF_IDF_VOCAB_SIZE_DEFAULT)<0) {
				LDAUtils.loadInstancesPrune(config.getDatasetFilename(), 
						config.getStoplistFilename("stoplist.txt"), config.getTfIdfVocabSize(LDAConfiguration.TF_IDF_VOCAB_SIZE_DEFAULT), config.keepNumbers(), config.getMaxDocumentBufferSize(LDAConfiguration.MAX_DOC_BUFFFER_SIZE_DEFAULT), false, null);					
			} else {
				fail("TF-IDF was not > 0");
			}
		} catch(java.lang.ArrayIndexOutOfBoundsException aiob) {
			return;
		}
		fail("Test did not throw ArrayIndexOutOfBoundsException");
		
		LDAUtils.loadInstancesPrune(config.getDatasetFilename(), 
				config.getStoplistFilename("stoplist.txt"), config.getTfIdfVocabSize(LDAConfiguration.TF_IDF_VOCAB_SIZE_DEFAULT), config.keepNumbers(), 10000, false, null);					
	}


	@Test
	public void testBug() throws ConfigurationException, ParseException, FileNotFoundException {
		ConfigFactory.setMainConfiguration(null);
		String [] args = {"--run_cfg=src/test/resources/max_doc_buf-2.cfg"};
		
		LDACommandLineParser cp = new LDACommandLineParser(args);
		ParsedLDAConfiguration config = (ParsedLDAConfiguration) ConfigFactory.getMainConfiguration(cp);
		config.activateSubconfig("large_wiki_random_100_spalias_cores_16_seed_4711");
		
		assertEquals(100000,config.getMaxDocumentBufferSize(LDAConfiguration.MAX_DOC_BUFFFER_SIZE_DEFAULT));

		assertEquals(7700,(int)config.getTfIdfVocabSize(LDAConfiguration.TF_IDF_VOCAB_SIZE_DEFAULT));
		try {
			if(config.getTfIdfVocabSize(LDAConfiguration.TF_IDF_VOCAB_SIZE_DEFAULT)>0) {
				LDAUtils.loadInstancesKeep(config.getDatasetFilename(), 
						config.getStoplistFilename("stoplist.txt"), config.getTfIdfVocabSize(LDAConfiguration.TF_IDF_VOCAB_SIZE_DEFAULT), config.keepNumbers(), 5, false, null);					
			} else {
				fail("TF-IDF was not > 0");
			}
		} catch(java.lang.ArrayIndexOutOfBoundsException aiob) {
			return;
		}
		fail("Test did not throw ArrayIndexOutOfBoundsException");
		
		LDAUtils.loadInstancesKeep(config.getDatasetFilename(), 
				config.getStoplistFilename("stoplist.txt"), config.getTfIdfVocabSize(LDAConfiguration.TF_IDF_VOCAB_SIZE_DEFAULT), config.keepNumbers(), 10000, false, null);
	}
	
}
