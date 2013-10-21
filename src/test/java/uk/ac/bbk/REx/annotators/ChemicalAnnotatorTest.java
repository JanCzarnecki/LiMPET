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

public class ChemicalAnnotatorTest 
{
	AnalysisEngine chemAE;
	JCas cas;
	
	@Before
	public void initialise() throws ResourceInitializationException, InvalidXMLException
	{
		XMLInputSource chemIn = new XMLInputSource(getClass().getResourceAsStream("/uk/ac/bbk/REx/desc/ChemicalAnnotator.xml"), null);
		AnalysisEngineDescription chemDesc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(chemIn);
		chemAE = UIMAFramework.produceAnalysisEngine(chemDesc);
		
		cas = chemAE.newJCas();
	}
	
	@Test
	public void test() throws AnalysisEngineProcessException 
	{
		cas.setDocumentText("Acetyl coenzyme A and pyruvate and biological small molecules.");
		chemAE.process(cas);
		
		List<String> results = new ArrayList<String>();
		for(Annotation chemical : cas.getAnnotationIndex(Chemical.type))
		{
			results.add(chemical.getCoveredText());
		}
		
		assertEquals("[Acetyl coenzyme A, pyruvate]", results.toString());
	}
}
