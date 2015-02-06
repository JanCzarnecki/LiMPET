package uk.ac.bbk.REx.readers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.util.Scanner;

import static junit.framework.Assert.assertEquals;

public class PMCArticleTest
{
    @Test
    public void getTitleTest() throws ParserConfigurationException, SAXException, XPathExpressionException, IOException
    {
        PMCArticle article = new PMCArticle("24155869");
        assertEquals("Learning to Recognize Phenotype Candidates in the Auto-Immune Literature Using SVM Re-Ranking.",
                article.getTitle());
    }

    @Test
    public void getAbstractTest() throws ParserConfigurationException, SAXException, XPathExpressionException, IOException
    {
        PMCArticle article = new PMCArticle("24155869");
        Scanner abstractScanner = new Scanner(new BufferedInputStream(
                this.getClass().getResourceAsStream("/uk/ac/bbk/REx/readers/pmcArticleAbstract.txt")));
        abstractScanner.useDelimiter("\\A");
        String abstractString = abstractScanner.next();
        abstractScanner.close();

        assertEquals(abstractString, article.getAbstract());
    }

    @Test
    public void getPMCIDTest() throws ParserConfigurationException, SAXException, XPathExpressionException, IOException
    {
        PMCArticle article = new PMCArticle("24155869");
        assertEquals("3796529", article.getPMCID());
    }

    @Test
    public void getContentTest() throws IOException, XPathExpressionException, SAXException, ParserConfigurationException
    {
        PMCArticle article = new PMCArticle("24155869");
        Scanner contentScanner = new Scanner(new BufferedInputStream(
                this.getClass().getResourceAsStream("/uk/ac/bbk/REx/readers/pmcArticleContent.txt")));
        contentScanner.useDelimiter("\\A");
        String contentString = contentScanner.next();
        contentScanner.close();

        assertEquals(contentString, article.getAvailableText());
    }

    @Test
    public void hasPMCIDTest() throws ParserConfigurationException, SAXException, XPathExpressionException, IOException
    {
        PMCArticle article = new PMCArticle("24155869");
        assertEquals(true, article.hasPMCID());
    }

    @Test
    public void hasPMCIDNegativeTest() throws ParserConfigurationException, SAXException, XPathExpressionException, IOException
    {
        PMCArticle article = new PMCArticle("23105108");
        assertEquals(false, article.hasPMCID());
    }
}
