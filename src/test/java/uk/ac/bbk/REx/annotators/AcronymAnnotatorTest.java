package uk.ac.bbk.REx.annotators;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.junit.Before;
import org.junit.Test;

import uk.ac.bbk.REx.types.Chemical;

public class AcronymAnnotatorTest 
{
	AnalysisEngine acronymAE;
	JCas cas;
	
	@Before
	public void initialise() throws ResourceInitializationException, InvalidXMLException
	{
		XMLInputSource acronymIn = new XMLInputSource(getClass().getResourceAsStream("/uk/ac/bbk/REx/desc/AcronymAnnotator.xml"), null);
		AnalysisEngineDescription acronymDesc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(acronymIn);
		acronymAE = UIMAFramework.produceAnalysisEngine(acronymDesc);
		
		cas = acronymAE.newJCas();
	}
	
	@Test
	public void test() throws AnalysisEngineProcessException 
	{
		cas.setDocumentText("Succinate is formed via the reductive branch of the tricarboxylic acid (TCA) cycle from either phosphoenolpyruvate (PEP) or pyruvate.");
		
		Chemical c1 = new Chemical(cas, 0, 9);
		c1.addToIndexes();
		
		Chemical c2 = new Chemical(cas, 52, 70);
		c2.addToIndexes();
		
		Chemical c3 = new Chemical(cas, 95, 114);
		c3.addToIndexes();
		
		Chemical c4 = new Chemical(cas, 124, 132);
		c4.addToIndexes();
		
		acronymAE.process(cas);
		
		List<String> results = new ArrayList<String>();
		for(Annotation chemicalA : cas.getAnnotationIndex(Chemical.type))
		{
			Chemical chemical = (Chemical)chemicalA;
			if(chemical.getIsAcronym())
			{
				results.add(chemical.getCoveredText());
			}
		}
		
		assertEquals("[TCA, PEP]", results.toString());
	}
}
