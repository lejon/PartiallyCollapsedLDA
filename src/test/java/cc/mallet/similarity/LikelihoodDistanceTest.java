package cc.mallet.similarity;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import cc.mallet.types.InstanceList;
import cc.mallet.util.LDADatasetStringLoadingUtils;
import cc.mallet.util.LDAUtils;

public class LikelihoodDistanceTest {

	@Test
	public void testSimplest() {
		String [] doclines = {"apa"};
		InstanceList train = LDADatasetStringLoadingUtils.loadInstancesStrings(doclines);
		
		
		LikelihoodDistance cd = new LikelihoodDistance(train);
		
		String [] doclinesTest = {"apa"};
		InstanceList test = LDADatasetStringLoadingUtils.loadInstancesStrings(doclinesTest);

		TokenFrequencyVectorizer tv = new TokenFrequencyVectorizer(); 
		double [] v1 = tv.instanceToVector(test.get(0));
		double [] v2 = tv.instanceToVector(test.get(0));
		double result = cd.calculate(v1,v2);
		
		double expected = 0.0;
		assertEquals(expected, 1-Math.exp(-result), 0.0001);
	}
	
	@Test
	public void testSimple() {
		String [] doclines = {"apa", "apa banan"};
		InstanceList train = LDADatasetStringLoadingUtils.loadInstancesStrings(doclines);
		
		
		LikelihoodDistance cd = new LikelihoodDistance(train);
		
		String [] doclinesTest = {"apa"};
		InstanceList test = LDADatasetStringLoadingUtils.loadInstancesStrings(doclinesTest,train.getPipe());

		TokenFrequencyVectorizer tv = new TokenFrequencyVectorizer(); 
		double [] v1 = tv.instanceToVector(test.get(0));
		double [] v2 = tv.instanceToVector(test.get(0));
		double result = cd.calculate(v1,v2);
		
		double expected = 0.2222;
		assertEquals(expected, 1-Math.exp(-result), 0.0001);
	}

	@Test
	public void testSimplish() {
		String [] doclines = {"apa", "apa banan"};
		InstanceList train = LDADatasetStringLoadingUtils.loadInstancesStrings(doclines);
		
		
		LikelihoodDistance cd = new LikelihoodDistance(train);
		
		String [] doclinesTest = {"apa banan"};
		InstanceList test = LDADatasetStringLoadingUtils.loadInstancesStrings(doclinesTest,train.getPipe());

		TokenFrequencyVectorizer tv = new TokenFrequencyVectorizer(); 
		double [] v1 = tv.instanceToVector(test.get(0));
		double [] v2 = tv.instanceToVector(test.get(0));
		double result = cd.calculate(v1,v2);
		
		double expected = 0.7569;
		assertEquals(expected, 1-Math.exp(-result), 0.0001);
	}
	
	@Test
	public void testSimplishish() {
		String [] doclines = {"apa", "apa banan"};
		InstanceList train = LDADatasetStringLoadingUtils.loadInstancesStrings(doclines);
		
		LikelihoodDistance cd = new LikelihoodDistance(train);
		
		String [] doclinesTest = {"apa apa apa apa apa apa"};
		InstanceList test = LDADatasetStringLoadingUtils.loadInstancesStrings(doclinesTest,train.getPipe());

		TokenFrequencyVectorizer tv = new TokenFrequencyVectorizer(); 
		double [] v1 = tv.instanceToVector(test.get(0));
		double [] v2 = tv.instanceToVector(test.get(0));
		double result = cd.calculate(v1,v2);
		
		double expected = 0.0833;
		assertEquals(expected, 1-Math.exp(-result), 0.0001);
	}

	@Test
	public void testIR() {
		String [] doclines = {
				"Xyzzy reports a profit but revenue is down", 
				"Quorus narrows quarter loss but revenue decreases further"};

		String [] classNames = new String [doclines.length];
		for (int i = 0; i < classNames.length; i++) {
			classNames[i] = "X";
		}

		InstanceList train = LDADatasetStringLoadingUtils.loadInstancesStrings(doclines, classNames);
		System.out.println(train.getAlphabet());
		assertEquals(14,train.getAlphabet().size());
		
		LikelihoodDistance cd = new LikelihoodDistance(train);
		cd.setMixtureRatio(1/2.0);
		
		String [] doclinesTest = {"revenue down"};
		
		
		InstanceList test = LDADatasetStringLoadingUtils.loadInstancesStrings(doclinesTest,train.getPipe());
		assertEquals("revenue, down", LDAUtils.instanceToString(test.get(0)));

		TokenFrequencyVectorizer tv = new TokenFrequencyVectorizer(); 
		double [] v1 = tv.instanceToVector(test.get(0));
		//System.out.println("v1=" + Arrays.toString(v1));
		double [] v2 = tv.instanceToVector(train.get(0));
		//System.out.println("v2=" + Arrays.toString(v2));
		double result1 = cd.calculate(v1,v2);
		
		double expected = 1 - (3/256.0);
		assertEquals(expected, 1-Math.exp(-result1), 0.0001);

		double [] v3 = tv.instanceToVector(train.get(1));
		double result2 = cd.calculate(v1,v3);
		
		double expected2 = 1-(1/256.0);
		assertEquals(expected2, 1-Math.exp(-result2), 0.0001);
	}
	
	@Test
	public void testLonger() {
		String queryDoc = "From: marlow@sys.uea.ac.uk (Keith Marlow PG) Subject: PD apps for displaying 3D data sets Article-I.D.: radon.marlow.737220727 Organization: University of East Anglia Lines: 16 The subject line says it all really, I'm looking for a PD application which will just handle the displaying of 3D data sets (images) in cross section, or any pointers to code which will aid in the development of such a system. Thanks in advance Keith Marlow -- Keith Marlow,SYS P/G,UEA,Norwich * Phone Cyclone BBS on 0603 260973; Arc,Beeb Norwich. Norfolk NR4 7TJ * PC files + fidonet echoes + charts + Acorn Archiboard Central 2:254/405.3 * Support Area + radio info : Archiboard s/w Voice - 0603 745077 ### Tried MS-DOS once.. but didn't inhale ##";
		String ref1 = "Subject: Re: Albert Sabin From: rfox@charlie.usd.edu (Rich Fox, Univ of South Dakota) Reply-To: rfox@charlie.usd.edu Organization: The University of South Dakota Computer Science Dept. Nntp-Posting-Host: charlie Lines: 91 In article <1993Apr15.012537.26867@nntpd2.cxo.dec.com>, sharpe@nmesis.enet.dec.com (System PRIVILEGED Account) writes: > >In article <C5FtJt.885@sunfish.usd.edu>, rfox@charlie.usd.edu (Rich Fox, Univ of South Dakota) writes: >|> >|>In article <1993Apr10.213547.17644@rambo.atlanta.dg.com>, wpr@atlanta.dg.com (Bill Rawlins) writes: >|> >|>[earlier dialogue deleted] >|> >|>>|> Perhaps you should read it and stop advancing the Bible as evidence relating >|>>|> to questions of science. >|> >|>[it = _Did Jesus exist?_ by G. A. Wells] >|> >|>> There is a great fallacy in your statement. The question of origins is >|>> based on more than science alone. >|> >|>Nope, no fallacy. Yep, science is best in determining how; religions handle >|>why and who. >|> > >Rich, I am curious as to why you and others award custody of the baby to >theists and religion? I hope I didn't award custody, Rich. I purposely used \"handle\" in order to avoid doing so - i.e., that happens to be what religions do (of course there are aberrations like \"scientific\" creationism). I used \"best\" in part to indicate that science currently has a time of it with why and who, so these domains are mostly ignored. I also attempted to be brief, which no doubt confused the matter. As an aside, for science I should have written \"how and when\". Nobody seems to argue over what. >Are they [theists, theologians] any better equiped to investigate the \"who and >why\" than magicians, astrologers, housewives [not being sexists], athiests or >agnostics. Seems to me that the answer would vary from individual to individual. I'm not trying to be evasive on this, but from a societal perspective, religion works. On the other hand, sometimes it is abused and misused, and many suffer, which you know. But the net result seems positive, this from the anthropological perspective on human affairs. You might call me a neo-Fruedian insofar as I think the masses can't get along without religion. Not that generally they are incapable; they just don't, and for myriad reasons, but the main one seems to be the promise of immortality. Very seductive, that immortality. Therefore it seems that theologians are better equipped than the others you mention for dispensing answers to \"who and why\". I suggest that this holds regardless of the \"truth\" in their answers to who and why simply because people believe. In the end, spiritual beliefs are just as \"real\" as scientific facts and explanation (CAUTION TO SOME: DO NOT TAKE THIS OUT OF CONTEXT). >Do you suggest that the \"who and why\" will forever be closed to scientific >investigation? No. In fact, I don't think it is closed now, at least for some individuals. Isn't there a group of theoretical physicists who argue that matter was created from nothing in a Big Bang singularity? This approach might presuppose an absence of who and why, except that it seems it could be argued that something had to be responsible for nothing? Maybe that something doesn't have to be supernatural, maybe just mechanistic. But that's a tough one for people today to grasp. In any case, theory without empirical data is not explanation, but then your question does not require data. In other words, I agree that theorizing (within scientific parameters) is just as scientific as explaining. So the answer is, who and why are not closed to scientists, but I sense that science in these realms is currently very inadequate. Data will be necessary for improvement, and that seems a long way off, if ever. Pretty convoluted here; I hope I've made sense. >It seems to me that 200 or so years ago, the question of the origin of life on >earth was not considered open to scientific enquiry. I agree generally. But I prefer to put it this way - the *questions* of how, when, who and why were not open to inquiry. During the Enlightenment, reason was reponsible for questioning the theological answers to how and when, and not, for the most part, who and why. Science was thus born out of the naturalists' curiosity, eventually carting away the how and when while largely leaving behind the who and why. The ignorant, the selfish, the intolerant, and the arrogant, of course, still claim authority in all four domains. >|>Rich Fox, Anthro, Usouthdakota >Did like your discussion around AMHs, and I did figure out what AMH was from >your original post :-) Much obliged. Funny how facts tend to muddle things, isn't it? Well, I am sure there are plenty of \"scientific\" creationist \"rebuttals\" out there somewhere, even if they have to be created from nothing. [just for the record, again, AMH = anatomically modern humans] Best regards :-), Rich Fox, Anthro, Usouthdakota\n" + 
				"Id	Class\n" + 
				"2085	comp.windows.x\n" + 
				"From: marlow@sys.uea.ac.uk (Keith Marlow PG) Subject: PD apps for displaying 3D data sets Article-I.D.: radon.marlow.737220727 Organization: University of East Anglia Lines: 16 The subject line says it all really, I'm looking for a PD application which will just handle the displaying of 3D data sets (images) in cross section, or any pointers to code which will aid in the development of such a system. Thanks in advance Keith Marlow -- Keith Marlow,SYS P/G,UEA,Norwich * Phone Cyclone BBS on 0603 260973; Arc,Beeb Norwich. Norfolk NR4 7TJ * PC files + fidonet echoes + charts + Acorn Archiboard Central 2:254/405.3 * Support Area + radio info : Archiboard s/w Voice - 0603 745077 ### Tried MS-DOS once.. but didn't inhale ##";
		String ref2 = "From: cutter@gloster.via.mind.org (cutter) Subject: Re: Biblical Backing of Koresh's 3-02 Tape (Cites enclosed) Distribution: world Organization: Gordian Knot, Gloster,GA Lines: 22 netd@susie.sbc.com () writes: > In article <20APR199301460499@utarlg.uta.edu> b645zaw@utarlg.uta.edu (stephen > >For those who think David Koresh didn't have a solid structure, > >or sound Biblical backing for his hour long tape broadcast, > > I don't think anyone really cares about the solid structure of his > sermon. It's the deaths he's responsible for that concern most people. > And I think we ought to hold Christ accoountable for all of his followers who died at the hand of the Romans also. It was their own fault for believing. God, this society reminds me more of the Roman Empire every day; I guess I'll just log off and go watch American Gladiators. --------------------------------------------------------------------- cutter@gloster.via.mind.org (chris) All jobs are easy to the person who doesn't have to do them. Holt's law";
		
		String [] corpus = {queryDoc, ref1, ref2};
		
		String [] classNames = new String [corpus.length];
		for (int i = 0; i < classNames.length; i++) {
			classNames[i] = "X";
		}
		
		InstanceList train = LDADatasetStringLoadingUtils.loadInstancesStrings(corpus, classNames);

		LikelihoodDistance cd = new LikelihoodDistance(train);
		cd.setMixtureRatio(1/2.0);
		
		String [] doclinesTest = {queryDoc};
		
		InstanceList test = LDADatasetStringLoadingUtils.loadInstancesStrings(doclinesTest,train.getPipe());

		TokenFrequencyVectorizer tv = new TokenFrequencyVectorizer(); 
		double [] v1 = tv.instanceToVector(test.get(0));
		double [] v2 = tv.instanceToVector(train.get(0));
		double result1 = cd.calculate(v1,v2);
		assert(result1 > 1.0);
	}

	
}
