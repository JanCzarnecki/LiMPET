package uk.ac.bbk.REx.annotators;

import hu.u_szeged.rgai.bio.uima.tagger.LinnaeusSpecies;
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

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created with IntelliJ IDEA.
 * User: jan
 * Date: 11/09/13
 * Time: 11:43
 * To change this template use File | Settings | File Templates.
 */
public class LinnaeusAnnotatorTest
{
    AnalysisEngine linAE;
    JCas cas;

    @Before
    public void initialise() throws ResourceInitializationException, InvalidXMLException
    {
        XMLInputSource linIn = new XMLInputSource(getClass().getResourceAsStream("/hu/u_szeged/rgai/bio/uima/RgaiLinnaeusWrapper.xml"), null);
        AnalysisEngineDescription linDesc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(linIn);
        linAE = UIMAFramework.produceAnalysisEngine(linDesc);

        cas = linAE.newJCas();
    }

    @Test
    public void test() throws AnalysisEngineProcessException
    {
        cas.setDocumentText("Nicotiana sylvestris");
        linAE.process(cas);

        List<String> results = new ArrayList<String>();
        String mostProbableSpecies = "";
        for(Annotation speciesA : cas.getAnnotationIndex(LinnaeusSpecies.type))
        {
            LinnaeusSpecies species = (LinnaeusSpecies)speciesA;
            mostProbableSpecies = species.getMostProbableSpeciesId();
        }

        assertEquals("species:ncbi:4096", mostProbableSpecies);
    }
}
