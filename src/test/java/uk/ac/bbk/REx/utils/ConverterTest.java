package uk.ac.bbk.REx.utils;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.impl.Serialization;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.junit.Test;
import uk.ac.bbk.REx.test.TestCASGenerator;
import uk.ac.ebi.mdk.domain.annotation.rex.RExExtract;
import uk.ac.ebi.mdk.domain.entity.Metabolite;
import uk.ac.ebi.mdk.domain.entity.reaction.BiochemicalReaction;
import uk.ac.ebi.mdk.domain.entity.reaction.MetabolicParticipant;
import uk.ac.ebi.mdk.domain.entity.reaction.MetabolicReaction;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static junit.framework.Assert.assertEquals;

public class ConverterTest
{
    @Test
    public void convertSBMLToMDKTest() throws XMLStreamException, IOException
    {
        InputStream sbmlTestIS = new BufferedInputStream(
                ConverterTest.class.getResourceAsStream("/uk/ac/bbk/REx/utils/sbmlTest.xml"));
        List<MetabolicReaction> reactions = Converter.convertSBMLToMDK(sbmlTestIS);
        sbmlTestIS.close();

        MetabolicReaction r = reactions.get(0);
        Collection<RExExtract> extracts = r.getAnnotations(RExExtract.class);

        assertEquals(5, extracts.size());
    }
/*
    @Test
    public void convertUIMAReactionsToMDKTest() throws
            CASException, ResourceInitializationException, InvalidXMLException, IOException
    {
        JCas jcas = TestCASGenerator.generateTestJCas();
        File tempFile = new File(System.getProperty("java.io.tmpdir") + "/rexTest.ser");
        tempFile.deleteOnExit();

        OutputStream out = new BufferedOutputStream(new FileOutputStream(tempFile));
        Serialization.serializeCAS(jcas.getCas(), out);
        out.close();

        XMLInputSource in = new XMLInputSource(
                ConverterTest.class.getResourceAsStream("/uk/ac/bbk/REx/desc/RExAnnotator.xml"), null);
        AnalysisEngineDescription aeDesc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in);
        ConfigurationParameterSettings aeParams = aeDesc.getMetaData().getConfigurationParameterSettings();
        aeParams.setParameterValue("organism", "111");
        AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(aeDesc);

        List<BiochemicalReaction> reactions = Converter.convertUIMAReactionsToMDK(
                new File[]{tempFile}, ae, new ArrayList<String>(), "562", new ArrayList<Metabolite>(), new ArrayList<String>());
        List<String> results = new ArrayList<String>();

        MetabolicReaction reaction = reactions.get(0);
        for(MetabolicParticipant substrate : reaction.getReactants())
        {
            results.add(substrate.getMolecule().getName());
        }

        for(MetabolicParticipant product : reaction.getProducts())
        {
            results.add(product.getMolecule().getName());
        }

        assertEquals("[agmatine, putrescine]", results.toString());
    }
    */
}