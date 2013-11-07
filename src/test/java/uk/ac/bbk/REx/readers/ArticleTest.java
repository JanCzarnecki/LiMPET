package uk.ac.bbk.REx.readers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import static junit.framework.Assert.assertEquals;

public class ArticleTest
{
    @Test
    public void getTitleTest()
            throws ParserConfigurationException, TransformerException, SAXException, XPathExpressionException, IOException
    {
        Article a = new Article("24086341");

        assertEquals("Drug-Induced Reactivation of Apoptosis Abrogates HIV-1 Infection.", a.getTitle());
    }

    @Test
    public void getAbstractTest()
            throws ParserConfigurationException, TransformerException, SAXException, XPathExpressionException, IOException
    {
        Article a = new Article("24086341");

        Scanner abstractScanner = new Scanner(new BufferedInputStream(
                this.getClass().getResourceAsStream("/uk/ac/bbk/REx/readers/abstract.txt")));
        abstractScanner.useDelimiter("\\A");
        String abstractString = abstractScanner.next();
        abstractScanner.close();

        assertEquals(abstractString, a.getAbstract());
    }

    @Test
    public void getPDFLinkPLOSOneTest()
            throws ParserConfigurationException, TransformerException, SAXException, XPathExpressionException, IOException
    {
        Article a = new Article("24086341");

        //Ignore jsessionid, which will be different each request.
        assertEquals("http://www.plosone.org/article/fetchObject.action;?" +
                "uri=info%3Adoi%2F10.1371%2Fjournal.pone.0074414&representation=PDF",
                a.getPDFLink().replaceAll("jsessionid=[^?]+", ""));
    }

    @Test
    public void getPDFLinkACSTest()
            throws ParserConfigurationException, TransformerException, SAXException, XPathExpressionException, IOException
    {
        Article a = new Article("21627103");

        assertEquals("http://pubs.acs.org/doi/pdf/10.1021/bi200156t", a.getPDFLink());
    }

    @Test
    public void getPDFLinkScienceDirectTest()
            throws ParserConfigurationException, TransformerException, SAXException, XPathExpressionException, IOException
    {
        Article a = new Article("23747483");

        assertEquals("http://www.sciencedirect.com/science/article/pii/S0022283613003550/pdfft?" +
                "md5=bd8bbf893c9d60972433eec360b0c8e5&pid=1-s2.0-S0022283613003550-main.pdf", a.getPDFLink());
    }

    @Test
    public void getWebContentTest()
            throws ParserConfigurationException, TransformerException, SAXException, XPathExpressionException, IOException
    {
        Article a = new Article("24086341");

        assertEquals(149649, a.getWebContent().length());
    }

    @Test
    public void getPDFContentTest()
            throws ParserConfigurationException, TransformerException, SAXException, XPathExpressionException, IOException
    {
        Article a = new Article("23747483");

        assertEquals(47515, a.getPDFContent().length());
    }

    @Test
    public void getPDFStreamTest()
            throws ParserConfigurationException, TransformerException, SAXException, XPathExpressionException, IOException
    {
        Article a = new Article("23747483");
        PDDocument doc = PDDocument.load(a.getPDFStream());
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(doc);
        doc.close();
        text = text.replaceAll("-\\n", "-");
        text = text.replaceAll("\\n", " ");
        text = text.replaceAll("[^A-Za-z\\s(){}_,.:;<>!=&\\-+\"'0-9|%]", " ");
        text = text.replaceAll("\\r", " ");

        assertEquals(47515, text.length());
    }
}
