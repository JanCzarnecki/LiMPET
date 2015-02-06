package uk.ac.bbk.REx.annotators;

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
 * Date: 14/06/13
 * Time: 09:30
 * To change this template use File | Settings | File Templates.
 */
public class CHEBIAnnotatorTest
{
    AnalysisEngine chebiAE;
    JCas cas;

    @Before
    public void initialise() throws ResourceInitializationException, InvalidXMLException
    {
        XMLInputSource chebiIn = new XMLInputSource(getClass().getResourceAsStream("/uk/ac/bbk/REx/desc/CHEBIAnnotator.xml"), null);
        AnalysisEngineDescription chebiDesc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(chebiIn);
        chebiAE = UIMAFramework.produceAnalysisEngine(chebiDesc);

        cas = chebiAE.newJCas();
    }

    @Test
    public void test() throws AnalysisEngineProcessException
    {
        cas.setDocumentText("tricarboxylic acid");

        Chemical c = new Chemical(cas);
        c.setBegin(0);
        c.setEnd(10);
        c.addToIndexes();

        chebiAE.process(cas);

        List<String> inchis = new ArrayList<String>();
        for(Annotation chemicalA : cas.getAnnotationIndex(Chemical.type))
        {
            Chemical chemical = (Chemical)chemicalA;
            inchis.add(chemical.getInChiString());
        }

        assertEquals("[InChI=1S/C8H16NO9P/c1-3(11)9-5-7(13)6(12)4(2-10)17-8(5)18-19(14,15)16/h4-8,10,12-13H,2H2,1H3,(H,9,11)(H2,14,15,16)]", inchis.toString());
    }
}
