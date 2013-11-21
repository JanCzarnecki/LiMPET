package uk.ac.bbk.REx.readers;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.xml.sax.SAXException;
import uk.ac.bbk.REx.db.documentDB.DocumentDB;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;

public class PubMedDownloaderTest
{
    @Test
    public void singlePMCTest() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, InterruptedException, ParserConfigurationException, SAXException, XPathExpressionException, IOException
    {
        DocumentDB docDB = new DocumentDB();
        docDB.clearDatabase();

        List<String> pmids = new ArrayList<String>();
        pmids.add("24155869");
        PubMedDownloader pmd = new PubMedDownloader(pmids);

        String expected = IOUtils.toString(this.getClass().getResourceAsStream("/uk/ac/bbk/REx/readers/24155869.txt"));
        String actual = docDB.get("24155869");
        docDB.close();

        assertEquals(expected, actual);
    }

    @Test
    public void singleNonPMCTest() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, InterruptedException, ParserConfigurationException, SAXException, XPathExpressionException, IOException
    {
        DocumentDB docDB = new DocumentDB();
        docDB.clearDatabase();

        List<String> pmids = new ArrayList<String>();
        pmids.add("19233964");
        PubMedDownloader pmd = new PubMedDownloader(pmids);

        String expected = IOUtils.toString(this.getClass().getResourceAsStream("/uk/ac/bbk/REx/readers/19233964.txt"));
        String actual = docDB.get("19233964");
        docDB.close();

        assertEquals(expected, actual);
    }

    @Test
    public void multipleTest() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, IOException, InterruptedException, ParserConfigurationException, SAXException, XPathExpressionException
    {
        DocumentDB docDB = new DocumentDB();
        docDB.clearDatabase();

        List<String> pmids = IOUtils.readLines(
                this.getClass().getResourceAsStream("/uk/ac/bbk/REx/readers/randomPMIDs.txt"));
        PubMedDownloader pmd = new PubMedDownloader(pmids);

        String expected = IOUtils.toString(this.getClass().getResourceAsStream("/uk/ac/bbk/REx/readers/21120029.txt"));
        String actual = docDB.get("21120029");
        docDB.close();

        assertEquals(expected, actual);
    }

    public static void main(String[] args)
    {
        for(int i=0; i<200; i++)
        {
            int randID = (int)Math.floor(Math.random() * 8000000) + 16000000;
            System.out.println(randID);
        }
    }
}
