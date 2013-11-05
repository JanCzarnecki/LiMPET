package uk.ac.bbk.REx.readers;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Article represents a page linked to by PubMed. The class provides methods to retrieve the title,
 * abstract and, if possible, the full text.
 */
public class Article
{
	private DocumentBuilder builder;
    private XPath xpath;

	private String pmid;
    private String title;
    private String articleAbstract;
    private org.jsoup.nodes.Document webPage;
	private String pdfLink;
    private String webContent;
    private String pdfContent;
    private String cookie;
	private final Logger LOGGER = Logger.getLogger(this.getClass().getName());

    private boolean hasRunWebRetrieval;
    private boolean hasRunPDFRetrieval;

	public Article(String pmid) throws ParserConfigurationException, XPathExpressionException, SAXException, IOException, TransformerException
	{
		builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        XPathFactory factory = XPathFactory.newInstance();
        xpath = factory.newXPath();

        this.pmid = pmid;

        Document doc = getEFetchDoc(pmid);

        title = null;
        if(hasTitle(doc))
        {
            title = getTitle(doc);
        }

        articleAbstract = null;
        if(hasAbstract(doc))
        {
            articleAbstract = getAbstract(doc);
        }

        cookie = "";

        hasRunPDFRetrieval = false;
        hasRunWebRetrieval = false;
	}
	
	public String getPMID()
	{
		return pmid;
	}

    public String getTitle()
    {
        return title;
    }

    public String getAbstract()
    {
        return articleAbstract;
    }

    private void webRetrieval() throws IOException
    {
        if(!hasRunWebRetrieval)
        {
            hasRunWebRetrieval = true;
            webPage = getArticleWebPage(getPMID());

            webContent = null;
            if(doesWebPageContainFullArticle(webPage))
            {
                webContent = getContentFromWebPage(webPage);
            }
        }
    }

    private void pdfRetrieval() throws IOException
    {
        if(!hasRunPDFRetrieval)
        {
            hasRunPDFRetrieval = true;
            webRetrieval();
            pdfLink = getPDFLink(webPage);
            if(pdfLink != null)
            {
                pdfContent = getContentFromPDF(getPDFStream());
            }
        }
    }

    public boolean hasPDFLink() throws IOException
    {
        pdfRetrieval();
        boolean hasPDFLink = false;
        if(pdfLink != null)
        {
            hasPDFLink = true;
        }

        return hasPDFLink;
    }

	public String getPDFLink() throws IOException
    {
        pdfRetrieval();
		return pdfLink;
	}

    public boolean hasPDFStream() throws IOException
    {
        return hasPDFLink();
    }

    public InputStream getPDFStream() throws IOException
    {
        pdfRetrieval();
        URLConnection con = new URL(pdfLink).openConnection();
        if(!cookie.equals(""))
        {
            con.setRequestProperty("Cookie", cookie);
        }
        return new BufferedInputStream(con.getInputStream());
    }

    public boolean hasWebContent() throws IOException
    {
        webRetrieval();
        boolean hasWebContent = false;
        if(webContent != null)
        {
            hasWebContent = true;
        }

        return hasWebContent;
    }
	
	public String getWebContent() throws IOException
    {
        webRetrieval();
		return webContent;
	}

    public boolean hasPDFContent() throws IOException
    {
        pdfRetrieval();
        boolean hasPDFContent = false;
        if(pdfContent != null)
        {
            hasPDFContent = true;
        }

        return hasPDFContent;
    }

    public String getPDFContent() throws IOException
    {
        pdfRetrieval();
        return pdfContent;
    }
	
	public boolean hasFullText() throws IOException
	{
		boolean hasFullText = false;
        if(hasWebContent() || hasPDFContent())
        {
            hasFullText = true;
        }

        return hasFullText;
	}

    public String getFullText() throws IOException
    {
        String fullText = null;

        if(hasWebContent())
        {
            fullText = getWebContent();
        }
        else if(hasPDFContent())
        {
            fullText = getPDFContent();
        }

        return fullText;
    }

    private Document getEFetchDoc(String pmid) throws IOException, SAXException
    {
        URL efetchURL = new URL(
                "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&retmode=xml&id=" + pmid);
        URLConnection con = efetchURL.openConnection();
        con.setConnectTimeout(20000);
        con.setReadTimeout(20000);
        InputStream is = new BufferedInputStream(con.getInputStream());
        Document doc = builder.parse(is);
        is.close();
        return doc;
    }

    private boolean hasTitle(Document doc) throws XPathExpressionException
    {
        XPathExpression titleBoolExp = xpath.compile("boolean(//ArticleTitle/text())");
        String titleBool = (String)titleBoolExp.evaluate(doc, XPathConstants.STRING);
        return Boolean.parseBoolean(titleBool);
    }

    private String getTitle(Document doc) throws XPathExpressionException
    {
        XPathExpression titleExp = xpath.compile("//ArticleTitle/text()");
        Node titleNode = (Node)titleExp.evaluate(doc, XPathConstants.NODE);
        return titleNode.getNodeValue();
    }

    private boolean hasAbstract(Document doc) throws XPathExpressionException
    {
        XPathExpression abstractBoolExp = xpath.compile("boolean(//AbstractText/text())");
        String abstractBool = (String)abstractBoolExp.evaluate(doc, XPathConstants.STRING);
        return Boolean.parseBoolean(abstractBool);
    }

    private String getAbstract(Document doc) throws XPathExpressionException
    {
        XPathExpression abstractExp = xpath.compile("//AbstractText/text()");
        Node abstractNode = (Node)abstractExp.evaluate(doc, XPathConstants.NODE);
        return abstractNode.getNodeValue();
    }

    private org.jsoup.nodes.Document getArticleWebPage(String pmid) throws IOException
    {
        String url = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/elink.fcgi?dbfrom=pubmed&id=" + pmid + "&retmode=ref&cmd=prlinks";
        LOGGER.log(Level.INFO, "Fetching " + url);

        //Get the cookie returned by the request. This is often required for retrieving the PDF.
        URLConnection con = new URL(url).openConnection();
        List<String> cookies = con.getHeaderFields().get("Set-Cookie");
        StringBuilder sb = new StringBuilder();
        if (cookies != null) {
            for (String cookie : cookies) {
                sb.append(cookie);
            }
        }
        cookie = sb.toString();

        org.jsoup.nodes.Document webPage = Jsoup.connect(url).timeout(20000).get();

        if(webPage.baseUri().substring(0, 19).equals("http://pubs.acs.org"))
        {
            webPage.select("ol#references").remove();
        }

        int wordCount = webPage.text().split("\\s+").length;

        if(wordCount < 4000)
        {
            Elements links = webPage.select("a");

            for(Element link : links)
            {
                String text = link.text().toLowerCase();

                if((text.contains("full text")
                        || text.contains("fulltext")
                        || text.contains("full article"))
                        || text.contains("html")
                        && !text.contains("pdf")
                        && !text.contains("publisher"))
                {
                    org.jsoup.nodes.Document nextWebPage = null;
                    try
                    {
                        nextWebPage = Jsoup.connect(link.attr("abs:href")).timeout(20000).get();
                        LOGGER.log(Level.INFO, "Fetching: " + link.attr("abs:href"));
                    }
                    catch(IOException e)
                    {
                        LOGGER.log(Level.INFO, "Could not connect: " + link.attr("abs:href"));
                        continue;
                    }
                    //Check if the linked page is big enough
                    if(nextWebPage.text().split("\\s+").length >= 4000)
                    {
                        webPage = nextWebPage;
                        if(webPage.baseUri().substring(0, 19).equals("http://pubs.acs.org"))
                        {
                            webPage.select("ol#references").remove();
                        }
                        url = link.attr("abs:href");
                        LOGGER.log(Level.INFO, "Found full text: " + link.attr("abs:href"));
                        return webPage;
                    }
                }
            }
        }
        else
        {
            LOGGER.log(Level.INFO, "Found full text: " + webPage.baseUri());
        }

        return webPage;
    }

    private boolean doesWebPageContainFullArticle(org.jsoup.nodes.Document webPage)
    {
        int wordCount = webPage.text().split("\\s+").length;
        boolean containsFullArticle = false;
        if(wordCount >= 4000)
        {
            containsFullArticle = true;
        }

        return containsFullArticle;
    }

    private String getContentFromWebPage(org.jsoup.nodes.Document webPage)
    {
        Element currentElement = webPage.select("body").first();

        if(currentElement == null)
        {
            return null;
        }

        boolean found = false;

        while(!found)
        {
            int currentElementWordCount = currentElement.text().split("\\s+").length;
            Elements children = currentElement.children();

            if(children.size() == 0)
            {
                return null;
            }

            Element biggestChild = children.first();
            int biggestChildWordCount = biggestChild.text().split("\\s+").length;

            for(Element child : children)
            {
                int thisWordCount = child.text().split("\\s+").length;

                if(thisWordCount > biggestChildWordCount)
                {
                    biggestChild = child;
                    biggestChildWordCount = biggestChild.text().split("\\s+").length;
                }
            }

            if((double)currentElementWordCount/(double)biggestChildWordCount > 1.2)
            {
                found = true;
            }
            else
            {
                currentElement = biggestChild;
            }
        }

        return currentElement.text();
    }

    private String getPDFLink(org.jsoup.nodes.Document webPage) throws IOException
    {
        if(webPage.baseUri().substring(0, 19).equals("http://pubs.acs.org"))
        {
            String pdfLink = webPage.baseUri().replaceAll("full", "pdf");

            try
            {
                URLConnection con = new URL(pdfLink).openConnection();
                if(!cookie.equals(""))
                {
                    con.setRequestProperty("Cookie", cookie);
                }
                InputStream stream = new BufferedInputStream(con.getInputStream());
                PDDocument doc = PDDocument.load(stream);
                LOGGER.log(Level.INFO, "Found PDF link: " + pdfLink);
                doc.close();
                return pdfLink;
            }
            catch(IOException e)
            {
                return null;
            }
        }
        else
        {
            Map<PDDocument, String> docsWithLinks = new HashMap<PDDocument, String>();
            Elements links = webPage.select("a[href]");

            for(Element link : links)
            {
                String href = link.absUrl("href");
                String text = link.text();

                if((href.contains("pdf") || text.contains("PDF"))
                        && !text.toLowerCase().contains("support")
                        && !text.toLowerCase().contains("supplementary"))
                {
                    //Does the link point directly to the PDF, or is it in a frame?
                    try
                    {
                        URLConnection con = new URL(href).openConnection();
                        if(!cookie.equals(""))
                        {
                            con.setRequestProperty("Cookie", cookie);
                        }
                        InputStream stream = new BufferedInputStream(con.getInputStream());
                        PDDocument doc = PDDocument.load(stream);
                        docsWithLinks.put(doc, href);
                        LOGGER.log(Level.INFO, "Found PDF at: " + href);
                    }
                    catch(IOException e)
                    {
                        //Therefore it is likely in a frame
                        try
                        {
                            //If href is not a valid URL, an IllegalArgumentException will be thrown.
                            Elements frames = Jsoup.connect(href).timeout(20000).get().select("frame, iframe");

                            for(Element frame : frames)
                            {
                                String frameSrc = frame.absUrl("src");
                                URL frameSrcUrl = new URL(frameSrc);
                                InputStream frameSrcStream = frameSrcUrl.openStream();

                                try
                                {
                                    URLConnection con = new URL(frameSrc).openConnection();
                                    if(!cookie.equals(""))
                                    {
                                        con.setRequestProperty("Cookie", cookie);
                                    }
                                    InputStream stream = new BufferedInputStream(con.getInputStream());
                                    PDDocument nextDoc = PDDocument.load(stream);
                                    docsWithLinks.put(nextDoc, frameSrc);
                                    LOGGER.log(Level.INFO, "Found PDF at: " + frameSrc);
                                }
                                catch(IOException ioe)
                                {
                                    continue;
                                }
                            }
                        }
                        //Simply ignore and move on if the URL is not valid.
                        catch(IllegalArgumentException iae){}
                        catch(IOException ioe){}
                    }
                }
            }

            if(pdfLink == null)
            {
                //See if a pdf is in a frame
                Elements frames = webPage.select("frame, iframe");

                for(Element frame : frames)
                {
                    String frameSrc = frame.absUrl("src");

                    try
                    {
                        URLConnection con = new URL(frameSrc).openConnection();
                        if(!cookie.equals(""))
                        {
                            con.setRequestProperty("Cookie", cookie);
                        }
                        InputStream stream = new BufferedInputStream(con.getInputStream());
                        PDDocument doc = PDDocument.load(stream);
                        docsWithLinks.put(doc, frameSrc);
                        LOGGER.log(Level.INFO, "Found PDF at: " + frameSrc);
                    }
                    catch(IOException e)
                    {
                        continue;
                    }
                }
            }

            String pdfWithMostPages = null;
            int mostPages = 0;
            for(PDDocument doc : docsWithLinks.keySet())
            {
                int pages = doc.getNumberOfPages();

                if(pages > mostPages)
                {
                    pdfWithMostPages = docsWithLinks.get(doc);
                    mostPages = pages;
                }
                doc.close();
            }

            LOGGER.log(Level.INFO, "Full text PDF determined to be: " + pdfWithMostPages);
            return pdfWithMostPages;
        }
    }

    private String getContentFromPDF(InputStream stream) throws IOException
    {
        PDFTextStripper stripper = new PDFTextStripper();

        PDDocument doc;
        try
        {
            doc = PDDocument.load(stream);
        }
        catch(IOException e)
        {
            return null;
        }
        String text = stripper.getText(doc);
        doc.close();
        text = text.replaceAll("-\\n", "-");
        text = text.replaceAll("\\n", " ");
        text = text.replaceAll("[^A-Za-z\\s(){}_,.:;<>!=&\\-+\"'0-9|%]", " ");
        text = text.replaceAll("\\r", " ");
        return text;
    }
}
