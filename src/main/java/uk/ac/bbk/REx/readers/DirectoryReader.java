package uk.ac.bbk.REx.readers;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;

/**
 * A UIMA collection reader that, given an absolute path to a directory, fetches all documents
 * and extracts the title, abstract and, if possible, the introduction.
 */
public class DirectoryReader extends CollectionReader_ImplBase
{
	private File[] docs;
	int count;

    private DocumentBuilder builder;
    private XPath xpath;
    private XPathExpression pubmedExp;
    private XPathExpression pmcExp;
    private XPathExpression pubmedHasTitleExp;
    private XPathExpression pubmedTitleExp;
    private XPathExpression pubmedHasAbstractExp;
    private XPathExpression pubmedAbstractExp;
    private XPathExpression pmcHasTitleExp;
    private XPathExpression pmcTitleExp;
    private XPathExpression pmcHasAbstractExp;
    private XPathExpression pmcAbstractExp;
    private XPathExpression pmcHasBodyExp;
    private XPathExpression pmcBodyExp;
	
	@Override
	public void initialize() throws ResourceInitializationException
	{
		getUimaContext().getLogger().log(Level.INFO, "Initialising Directory Reader.");
		File directory = new File((String)getConfigParameterValue("directory"));
		docs = directory.listFiles();
		count = 0;

        try
        {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        }
        catch (ParserConfigurationException e)
        {
            throw new ResourceInitializationException(e);
        }
        XPathFactory factory = XPathFactory.newInstance();
        xpath = factory.newXPath();
        try
        {
            pubmedExp = xpath.compile("boolean(/PubmedArticle)");
            pmcExp = xpath.compile("boolean(/article)");

            pubmedHasTitleExp = xpath.compile(
                    "boolean(/PubmedArticle/MedlineCitation/Article/ArticleTitle/text())");
            pubmedTitleExp = xpath.compile(
                    "/PubmedArticle/MedlineCitation/Article/ArticleTitle/text()");
            pubmedHasAbstractExp = xpath.compile(
                    "boolean(/PubmedArticle/MedlineCitation/Article/Abstract/AbstractText/text())");
            pubmedAbstractExp = xpath.compile(
                    "/PubmedArticle/MedlineCitation/Article/Abstract/AbstractText/text()");

            pmcHasTitleExp = xpath.compile(
                    "boolean(/article/front/article-meta/title-group/article-title/text())");
            pmcTitleExp = xpath.compile(
                    "/article/front/article-meta/title-group/article-title/text()");
            pmcHasAbstractExp = xpath.compile(
                    "boolean(/article/front/article-meta/abstract/text())");
            pmcAbstractExp = xpath.compile(
                    "/article/front/article-meta/abstract/text()");
            pmcHasBodyExp = xpath.compile("boolean(/article/body/text())");
            pmcBodyExp = xpath.compile("/article/body/text()");
        }
        catch (XPathExpressionException e)
        {
            throw new ResourceInitializationException(e);
        }
    }
	
	@Override
	public void getNext(CAS cas) throws IOException, CollectionException
	{
		File doc = docs[count];
		getUimaContext().getLogger().log(Level.INFO, "Retrieving document " + doc.getName());
		count++;
        String extension = FilenameUtils.getExtension(doc.getName()).toLowerCase();
		String content = "";
		
		if(extension.equals("txt"))
		{
			content = readTXT(doc);
		}
        else if(extension.equals("pht"))
        {
            content = readPHT(doc);
        }
        else if(extension.equals("xml"))
        {
            try
            {
                content = readXML(doc);
            }
            catch (SAXException e)
            {
                throw new IOException(e);
            }
            catch (XPathExpressionException e)
            {
                throw new IOException(e);
            }
        }
		else if(extension.equals("pdf"))
		{
			content = readPDF(doc);
		}
        else
        {
            throw new IOException(String.format("File %s does not have a recognised extension",
                    doc.getName()));
        }

        content = content.replaceAll("\\n", " ");
        content = content.replaceAll("\\r", " ");
		cas.setDocumentText(content);
		uk.ac.bbk.REx.types.Document docAnnotation;
		
		try
		{
			docAnnotation = new uk.ac.bbk.REx.types.Document(cas.getJCas());
		} 
		catch (CASException e)
		{
			throw new CollectionException(e);
		}
		
		docAnnotation.setId(doc.getName());
		docAnnotation.addToIndexes();
	}

    private String readTXT(File doc) throws IOException
    {
        return FileUtils.readFileToString(doc);
    }

    private String readPHT(File doc) throws IOException
    {
        org.jsoup.nodes.Document document = Jsoup.parseBodyFragment(FileUtils.readFileToString(doc));
        Element body = document.body();
        return body.text();
    }

    private String readXML(File doc) throws IOException, SAXException, XPathExpressionException
    {
        InputStream is = new BufferedInputStream(new FileInputStream(doc));
        Document document = builder.parse(is);
        is.close();
        StringBuilder content = new StringBuilder();

        boolean isPubmed = (Boolean)pubmedExp.evaluate(document, XPathConstants.BOOLEAN);
        boolean isPMC = (Boolean)pmcExp.evaluate(document, XPathConstants.BOOLEAN);

        if(isPubmed)
        {
            boolean hasTitle = (Boolean)pubmedHasTitleExp.evaluate(document, XPathConstants.BOOLEAN);
            if(hasTitle)
            {
                String title = (String)pubmedTitleExp.evaluate(document, XPathConstants.STRING);
                content.append(title);
            }

            boolean hasAbstract = (Boolean)pubmedHasAbstractExp.evaluate(document, XPathConstants.BOOLEAN);
            if(hasAbstract)
            {
                String articleAbstract = (String)pubmedAbstractExp.evaluate(document, XPathConstants.STRING);
                content.append(articleAbstract);
            }
        }
        else if(isPMC)
        {
            boolean hasTitle = (Boolean)pmcHasTitleExp.evaluate(document, XPathConstants.BOOLEAN);
            if(hasTitle)
            {
                String title = (String)pmcTitleExp.evaluate(document, XPathConstants.STRING);
                content.append(title);
            }

            boolean hasAbstract = (Boolean)pmcHasAbstractExp.evaluate(document, XPathConstants.BOOLEAN);
            if(hasAbstract)
            {
                String articleAbstract = (String)pmcAbstractExp.evaluate(document, XPathConstants.STRING);
                content.append(articleAbstract);
            }

            boolean hasBody = (Boolean)pmcHasBodyExp.evaluate(document, XPathConstants.BOOLEAN);
            if(hasBody)
            {
                String body = (String)pmcBodyExp.evaluate(document, XPathConstants.STRING);
                content.append(body);
            }
        }
        else
        {
            throw new IOException(
                    String.format("File %s is not recognised as a PubMed or PMC record.", doc.getPath()));
        }

        return content.toString();
    }

    private String readPDF(File doc) throws IOException
    {
        PDFTextStripper stripper = new PDFTextStripper();

        PDDocument document = PDDocument.load(doc);
        String text;
        try
        {
            text = stripper.getText(document);
        }
        catch(IOException e)
        {
            throw new IOException(e);
        }
        finally
        {
            document.close();
        }

        text = text.replaceAll("-\\n", "-");
        text = text.replaceAll("\\n", " ");
        text = text.replaceAll("\\r", " ");
        text = text.replaceAll("[^A-Za-z\\s(){}_,.:;<>!=&\\-+\"'0-9|%]", " ");
        return text;
    }

	@Override
	public void close() throws IOException 
	{
		
	}

	@Override
	public Progress[] getProgress() 
	{
		return new Progress[]
		{
			new ProgressImpl(count, docs.length, Progress.ENTITIES)
		};
	}

	@Override
	public boolean hasNext() throws IOException, CollectionException 
	{
		if(count == docs.length)
		{
			return false;
		}
		else
		{
			return true;
		}
	}
}
