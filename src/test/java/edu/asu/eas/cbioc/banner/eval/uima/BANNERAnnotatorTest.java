package edu.asu.eas.cbioc.banner.eval.uima;

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

import banner.types.uima.Gene;

public class BANNERAnnotatorTest 
{
	AnalysisEngine bannerAE;
	JCas cas;
	
	@Before
	public void initialise() throws ResourceInitializationException, InvalidXMLException
	{
		XMLInputSource bannerIn = new XMLInputSource(getClass().getResourceAsStream("/edu/asu/eas/cbioc/banner/desc/BANNERAE.xml"), null);
		AnalysisEngineDescription bannerDesc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(bannerIn);
		bannerAE = UIMAFramework.produceAnalysisEngine(bannerDesc);
		
		cas = bannerAE.newJCas();
	}
	
	@Test
	public void test() throws AnalysisEngineProcessException 
	{
		cas.setDocumentText("Alcohol dehydrogenase dehydrogenates ethanol.");
		bannerAE.process(cas);
		
		List<String> results = new ArrayList<String>();
		for(Annotation gene : cas.getAnnotationIndex(Gene.type))
		{
			results.add(gene.getCoveredText());
		}
		
		assertEquals("[Alcohol dehydrogenase]", results.toString());
	}

}
