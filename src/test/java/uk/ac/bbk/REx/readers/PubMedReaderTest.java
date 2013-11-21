package uk.ac.bbk.REx.readers;

import org.apache.commons.io.IOUtils;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.junit.Test;
import uk.ac.bbk.REx.db.documentDB.DocumentDB;
import uk.ac.bbk.REx.program.CLI;

import java.io.IOException;
import java.sql.SQLException;

import static junit.framework.Assert.assertEquals;

public class PubMedReaderTest
{
    @Test
    public void test() throws InvalidXMLException, ResourceInitializationException, IOException, CollectionException, ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException
    {
        DocumentDB docDB = new DocumentDB();
        docDB.clearDatabase();
        docDB.close();

        XMLInputSource reader = new XMLInputSource(
                this.getClass().getResourceAsStream("/uk/ac/bbk/REx/desc/PubMedReader.xml"), null);
        CollectionReaderDescription crDesc = UIMAFramework.getXMLParser().parseCollectionReaderDescription(reader);

        ConfigurationParameterSettings crParams = crDesc.getMetaData().getConfigurationParameterSettings();
        crParams.setParameterValue("queries", "[24155869]");
        crParams.setParameterValue("maxReturn", 1);

        CollectionReader cr = UIMAFramework.produceCollectionReader(crDesc);

        XMLInputSource in = new XMLInputSource(
                this.getClass().getResourceAsStream("/uk/ac/bbk/REx/desc/RExAnnotator.xml"), null);
        AnalysisEngineDescription aeDesc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in);
        AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(aeDesc);

        CAS cas = ae.newCAS();
        cr.getNext(cas);

        String expected = IOUtils.toString(this.getClass().getResourceAsStream("/uk/ac/bbk/REx/readers/24155869.txt"));

        assertEquals(expected, cas.getDocumentText());
    }
}
