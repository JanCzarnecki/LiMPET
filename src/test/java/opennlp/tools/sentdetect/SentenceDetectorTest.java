package opennlp.tools.sentdetect;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import opennlp.uima.Sentence;

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

public class SentenceDetectorTest 
{
	AnalysisEngine sentenceAE;
	JCas cas;
	
	@Before
	public void initialise() throws ResourceInitializationException, InvalidXMLException
	{
		XMLInputSource sentenceIn = new XMLInputSource(getClass().getResourceAsStream("/org/apache/opennlp/opennlpuima/desc/SentenceDetector.xml"), null);
		AnalysisEngineDescription sentenceDesc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(sentenceIn);
		sentenceAE = UIMAFramework.produceAnalysisEngine(sentenceDesc);
		
		cas = sentenceAE.newJCas();
	}
	
	@Test
	public void test() throws AnalysisEngineProcessException 
	{
		cas.setDocumentText("Increasingly biological text mining research is focusing on the extraction of complex relationships relevant to the construction and curation of biological networks and pathways. However, one important category of pathway - metabolic pathways - has been largely neglected. Here we present a relatively simple method for extracting metabolic reaction information from free text that scores different permutations of assigned entities (enzymes and metabolites) within a given sentence based on the presence and location of stemmed keywords. This method extends an approach that has proved effective in the context of the extraction of protein-protein interactions.");
		sentenceAE.process(cas);
		
		List<Integer> results = new ArrayList<Integer>();
		for(Annotation sentence : cas.getAnnotationIndex(Sentence.type))
		{
			results.add(sentence.getBegin());
		}
		
		assertEquals("[0, 179, 273, 539]", results.toString());
	}
}
