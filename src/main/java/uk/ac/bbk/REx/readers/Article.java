package uk.ac.bbk.REx.readers;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
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
	private transient DocumentBuilder builder;
	private String pmid;
	private transient org.jsoup.nodes.Document webPage;
	private String url;
	private String pdfLink;
	private String title;
	private String articleAbstract;
	private String content;
	private Logger LOGGER;
	
	
	//Private zero argument constructor to allow deserialization from JSON using GSON.
	@SuppressWarnings("unused")
	private Article(){}
	
	public Article(String aPMID) throws ParserConfigurationException, XPathExpressionException, SAXException, IOException, TransformerException
	{
        LOGGER = Logger.getLogger(this.getClass().getName());
		builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		pmid = aPMID;
		fetchData();
		
		try
		{
			fetchWebPage();
			fetchContent();
		}
		catch(IOException e)
		{
			//Simply ignore if we can't access the content
		}
	}
	
	public String getPMID()
	{
		return pmid;
	}
	
	public String getUrl()
	{
		return url;
	}
	
	public String getPDFLink()
	{
		return pdfLink;
	}
	
	public String getTitle()
	{
		return title;
	}
	
	public String getAbstract()
	{
		return articleAbstract;
	}
	
	public String getContent()
	{
		if(hasFullText())
		{
			return content;
		}
		else
		{
			return title + " " + articleAbstract;
		}
	}
	
	public boolean hasFullText()
	{
		if(content == null)
		{
			return false;
		}
		else
		{
			return true;
		}
	}
	
	private void fetchData() throws SAXException, IOException, TransformerException, XPathExpressionException
	{
		URL efetchURL = new URL("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&retmode=xml&id=" + pmid);
		URLConnection con = efetchURL.openConnection();
		con.setConnectTimeout(20000);
		con.setReadTimeout(20000);
        InputStream is = new BufferedInputStream(con.getInputStream());
		Document doc = builder.parse(is);
        is.close();
		
		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		
		XPathExpression titleExp = xpath.compile("//ArticleTitle/text()");
		Node titleNode = (Node)titleExp.evaluate(doc, XPathConstants.NODE);

        try
        {
		    title = titleNode.getNodeValue();
        }
        catch(NullPointerException e)
        {
            title = null;
        }
		
		XPathExpression abstractExp = xpath.compile("//AbstractText/text()");
		XPathExpression boolAbstractExp = xpath.compile("boolean(//AbstractText/text())");
		
		Node abstractNode = (Node)abstractExp.evaluate(doc, XPathConstants.NODE);
		
		if(boolAbstractExp.evaluate(doc, XPathConstants.STRING).equals("true"))
		{
			articleAbstract = abstractNode.getNodeValue();
		}
	}
	
	private void fetchWebPage() throws IOException, SAXException
	{
		url = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/elink.fcgi?dbfrom=pubmed&id=" + pmid + "&retmode=ref&cmd=prlinks";
		LOGGER.log(Level.INFO, "Fetching " + url);
		webPage = Jsoup.connect(url).timeout(20000).get();
		
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
					}
					catch(IllegalArgumentException e)
					{
						LOGGER.log(Level.INFO, "Non-valid URL: " + link.attr("abs:href"));
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
						return;
					}
				}
			}
		}
	}
	
	private String getContentFromWebPage()
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
		
		int wordCount = currentElement.text().split("\\s+").length;
		if(wordCount < 2000)
		{
			return null;
		}
		else
		{
			return currentElement.text();
		}
	}
	
	private void fetchPDFLink() throws IOException
	{
		Elements links = webPage.select("a[href]");
		
		for(Element link : links)
		{
			String href = link.absUrl("href");
			String text = link.text();
			
			if((href.contains(".pdf") || text.contains("PDF"))
					&& !text.toLowerCase().contains("support")
					&& !text.toLowerCase().contains("supplementary"))
			{
				if(!href.contains("sciencedirect"))
				{
					//Does the link point directly to the PDF, or is it in a frame?
					PDDocument doc;
					try
					{
						doc = PDDocument.load(href);
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

                                PDDocument nextDoc;
                                try
                                {
                                    nextDoc = PDDocument.load(frameSrcStream);
                                }
                                catch(IOException ioe)
                                {
                                    continue;
                                }

                                nextDoc.close();
                                pdfLink = frameSrc;
                            }
						}
                        //Simply ignore and move on if the URL is not valid.
                        catch(IllegalArgumentException iae){}
						
						continue;
					}
					
					doc.close();
				}
				
				pdfLink = link.absUrl("href");
			}
		}
		
		if(pdfLink == null)
		{
			//See if a pdf is in a frame
			Elements frames = webPage.select("frame, iframe");
			
			for(Element frame : frames)
			{
				String frameSrc = frame.absUrl("src");
				URL frameSrcUrl = new URL(frameSrc);
				InputStream frameSrcStream = frameSrcUrl.openStream();
				
				PDDocument doc;
				try
				{
					doc = PDDocument.load(frameSrcStream);
				}
				catch(IOException e)
				{
					continue;
				}

                frameSrcStream.close();
				doc.close();
				pdfLink = frameSrc;
			}
		}
	}
	
	private String getContentFromPDF() throws IOException
	{
		URL pdfURL = new URL(pdfLink);
		//ScienceDirect sends us on a merry chase to obtain their PDFs...
		URLConnection con = pdfURL.openConnection();
		StringBuilder sb = new StringBuilder();
		
		// find the cookies in the response header from the first request
		List<String> cookies = con.getHeaderFields().get("Set-Cookie");
		if (cookies != null) {
		    for (String cookie : cookies) {
		        if (sb.length() > 0) {
		            sb.append("; ");
		        }

		        // only want the first part of the cookie header that has the value
		        String value = cookie.split(";")[0];
		        sb.append(value);
		    }
		}
		
		// build request cookie header to send on all subsequent requests
		String cookieHeader = sb.toString();
		
		InputStream pdfStream;
		
		try
		{
			pdfStream = con.getInputStream();
		}
		catch(FileNotFoundException e)
		{
			con = pdfURL.openConnection();
			con.setRequestProperty("Cookie", cookieHeader);
			pdfStream = con.getInputStream();
		}
		
		PDFTextStripper stripper = new PDFTextStripper();
		PDDocument doc = PDDocument.load(pdfStream);
		String text = stripper.getText(doc);
        pdfStream.close();
		doc.close();
		
		text = text.replaceAll("-\\n", "-");
		text = text.replaceAll("\\n", " ");
		text = text.replaceAll("[^A-Za-z\\s(){}_,.:;<>!=&\\-+\"'0-9|%]", " ");
		text = text.replaceAll("\\r", " ");
		return text;
	}
	
	private void fetchContent() throws IOException
	{
		String webPageContent = getContentFromWebPage();
		if(webPageContent != null)
		{
			content = webPageContent;
		}
		else
		{
			fetchPDFLink();
			if(pdfLink != null)
			{
				content = getContentFromPDF();
			}
			else
			{
				content = title + ". " + articleAbstract;
			}
		}
		
		content = content.replaceAll("\\|", "");
	}
}
