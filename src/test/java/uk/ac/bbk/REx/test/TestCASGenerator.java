package uk.ac.bbk.REx.test;

import hu.u_szeged.rgai.bio.uima.tagger.LinnaeusSpecies;
import opennlp.uima.Sentence;
import org.apache.commons.io.IOUtils;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import uk.ac.bbk.REx.types.Chemical;
import uk.ac.bbk.REx.types.Gene;
import uk.ac.bbk.REx.types.Reaction;
import uk.ac.bbk.REx.utils.JCasUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class TestCASGenerator
{
    public static JCas generateTestJCas() throws InvalidXMLException, ResourceInitializationException, IOException, CASException {
        XMLInputSource in = new XMLInputSource(
                TestCASGenerator.class.getResourceAsStream("/uk/ac/bbk/REx/desc/SectionAnnotator.xml"), null);
        AnalysisEngineDescription aeDesc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in);
        ConfigurationParameterSettings aeParams = aeDesc.getMetaData().getConfigurationParameterSettings();
        AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(aeDesc);

        CAS cas = ae.newCAS();
        InputStream is = new BufferedInputStream(
                TestCASGenerator.class.getResourceAsStream("/uk/ac/bbk/REx/test/testDocument.txt"));
        StringWriter sw = new StringWriter();
        IOUtils.copy(is, sw);
        is.close();
        String testDocument = sw.toString();
        cas.setDocumentText(testDocument);
        JCas jcas = cas.getJCas();

        Sentence sent1 = new Sentence(jcas);
        sent1.setBegin(0);
        sent1.setEnd(113);
        sent1.addToIndexes();

        Sentence sent2 = new Sentence(jcas);
        sent2.setBegin(114);
        sent2.setEnd(252);
        sent2.addToIndexes();

        Chemical chem1 = new Chemical(jcas);
        chem1.setBegin(69);
        chem1.setEnd(77);
        chem1.addToIndexes();

        Chemical chem2 = new Chemical(jcas);
        chem2.setBegin(81);
        chem2.setEnd(91);
        chem2.addToIndexes();

        Gene gene1 = new Gene(jcas);
        gene1.setBegin(114);
        gene1.setEnd(124);
        gene1.addToIndexes();

        Chemical chem3 = new Chemical(jcas);
        chem3.setBegin(192);
        chem3.setEnd(200);
        chem3.addToIndexes();

        Chemical chem4 = new Chemical(jcas);
        chem4.setBegin(204);
        chem4.setEnd(214);
        chem4.addToIndexes();

        LinnaeusSpecies species = new LinnaeusSpecies(jcas);
        species.setBegin(235);
        species.setEnd(251);
        species.setMostProbableSpeciesId("species:ncbi:562");
        species.addToIndexes();

        Reaction reaction = new Reaction(jcas);
        reaction.setBegin(114);
        reaction.setEnd(252);

        List<Chemical> substrates = new ArrayList<Chemical>();
        substrates.add(chem3);
        reaction.setSubstrates(JCasUtils.createFSList(jcas, substrates));

        List<Chemical> products = new ArrayList<Chemical>();
        products.add(chem4);
        reaction.setProducts((JCasUtils.createFSList(jcas, products)));

        List<LinnaeusSpecies> orgs = new ArrayList<LinnaeusSpecies>();
        orgs.add(species);
        reaction.setOrganisms(JCasUtils.createFSList(jcas, orgs));

        reaction.addToIndexes();

        return jcas;
    }
}
