package uk.ac.bbk.REx.readers;

import org.apache.xpath.NodeSet;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;

public class PMCArticle
{
    private final Logger LOGGER = Logger.getLogger(this.getClass().getName());

    private DocumentBuilder builder;
    private XPath xpath;
    private String pmid;
    private String title;
    private String articleAbstract;
    private String pmcID;
    private String content;

    public PMCArticle(String pmid) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException
    {
        LOGGER.info("Attempting to find document with PubMed ID " + pmid + ".");
        builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        XPathFactory factory = XPathFactory.newInstance();
        xpath = factory.newXPath();

        this.pmid = pmid;

        Document pubmedEFetchDoc = getPubMedEFetchDoc(pmid);

        if(hasTitle(pubmedEFetchDoc))
        {
            title = getTitle(pubmedEFetchDoc);
            LOGGER.info(String.format("Title of document %s found: %s", pmid, title));
        }
        else
        {
            title = null;
            LOGGER.info(String.format("No title for document %s found.", pmid));
        }

        if(hasAbstract(pubmedEFetchDoc))
        {
            articleAbstract = getAbstract(pubmedEFetchDoc);
            LOGGER.info(String.format("Abstract of document %s found: %s", pmid, articleAbstract));
        }
        else
        {
            articleAbstract = null;
            LOGGER.info(String.format("No abstract for document %s found.", pmid));
        }

        Document eLinkDoc = getELinkDoc(pmid);

        if(isInPMC(eLinkDoc))
        {
            pmcID = getPMCID(eLinkDoc);

            LOGGER.info(String.format("Document %s found in PubMedCentral with ID %s.", pmid, pmcID));

            Document pmcEFetchDoc = getPMCEFetchDoc(pmcID);
            content = getContent(pmcEFetchDoc);
            LOGGER.info(String.format("Content of document %s found.", pmid));
            LOGGER.fine(String.format("Content of document %s: %s", pmid, content));
        }
        else
        {
            LOGGER.info(String.format("Document %s not found in PubMedCentral.", pmid));

            pmcID = null;
            content = null;
        }
    }

    private Document getPubMedEFetchDoc(String pmid) throws IOException, SAXException
    {
        String url = String.format("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?" +
                "db=pubmed&retmode=xml&id=%s", pmid);

        LOGGER.info(String.format("Attempting to retrieve eFetch document from %s", url));

        URL efetchURL = new URL(url);
        URLConnection con = efetchURL.openConnection();
        con.setConnectTimeout(20000);
        con.setReadTimeout(20000);
        InputStream is = new BufferedInputStream(con.getInputStream());
        Document doc = builder.parse(is);
        is.close();

        LOGGER.info(String.format("eFetch document retrieved from %s", url));

        return doc;
    }

    private boolean hasTitle(Document pubmedEFetchDoc) throws XPathExpressionException
    {
        XPathExpression titleBoolExp = xpath.compile("boolean(//ArticleTitle/text())");
        String titleBool = (String)titleBoolExp.evaluate(pubmedEFetchDoc, XPathConstants.STRING);
        return Boolean.parseBoolean(titleBool);
    }

    private String getTitle(Document pubmedEFetchDoc) throws XPathExpressionException
    {
        XPathExpression titleExp = xpath.compile("//ArticleTitle/text()");
        Node titleNode = (Node)titleExp.evaluate(pubmedEFetchDoc, XPathConstants.NODE);
        return titleNode.getNodeValue();
    }

    private boolean hasAbstract(Document pubmedEFetchDoc) throws XPathExpressionException
    {
        XPathExpression abstractBoolExp = xpath.compile("boolean(//AbstractText/text())");
        String abstractBool = (String)abstractBoolExp.evaluate(pubmedEFetchDoc, XPathConstants.STRING);
        return Boolean.parseBoolean(abstractBool);
    }

    private String getAbstract(Document pubmedEFetchDoc) throws XPathExpressionException
    {
        XPathExpression abstractExp = xpath.compile("//AbstractText/text()");
        Node abstractNode = (Node)abstractExp.evaluate(pubmedEFetchDoc, XPathConstants.NODE);
        return abstractNode.getNodeValue();
    }

    private Document getELinkDoc(String pmid) throws IOException, SAXException
    {
        String url = String.format("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/elink.fcgi?" +
                "dbfrom=pubmed&db=pmc&id=%s", pmid);

        LOGGER.info(String.format("Attempting to retrieve eLink document from %s", url));

        URL efetchURL = new URL(url);
        URLConnection con = efetchURL.openConnection();
        con.setConnectTimeout(20000);
        con.setReadTimeout(20000);
        InputStream is = new BufferedInputStream(con.getInputStream());
        Document doc = builder.parse(is);
        is.close();

        LOGGER.info(String.format("eLink document retrieved from %s", url));

        return doc;
    }

    private boolean isInPMC(Document eLinkDoc) throws XPathExpressionException
    {
        XPathExpression pmcExp = xpath.compile("//LinkSetDb/LinkName[text()='pubmed_pmc']");
        NodeList pmcNodes = (NodeList)pmcExp.evaluate(eLinkDoc, XPathConstants.NODESET);

        boolean nodesExist = false;
        if(pmcNodes.getLength() > 0)
        {
            nodesExist = true;
        }
        return nodesExist;
    }

    private String getPMCID(Document eLinkDoc) throws XPathExpressionException
    {
        XPathExpression idExp = xpath.compile("//LinkSetDb[LinkName='pubmed_pmc']/Link/Id/text()");
        return (String)idExp.evaluate(eLinkDoc, XPathConstants.STRING);
    }

    private Document getPMCEFetchDoc(String pmcid) throws IOException, SAXException
    {
        String url = String.format("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?" +
                "db=pmc&id=%s", pmcid);

        LOGGER.info(String.format("Attempting to retrieve eFetch document from %s", url));

        URL efetchURL = new URL(url);
        URLConnection con = efetchURL.openConnection();
        con.setConnectTimeout(20000);
        con.setReadTimeout(20000);
        InputStream is = new BufferedInputStream(con.getInputStream());
        Document doc = builder.parse(is);
        is.close();

        LOGGER.info(String.format("eFetch document retrieved from %s", url));

        return doc;
    }

    private String getContent(Document pmcEFetchDoc) throws XPathExpressionException
    {
        XPathExpression bodyExp = xpath.compile("//body");
        Node bodyNode = (Node)bodyExp.evaluate(pmcEFetchDoc, XPathConstants.NODE);

        return bodyNode.getTextContent();
    }

    public String getPMID()
    {
        return pmid;
    }

    public boolean hasTitle()
    {
        boolean hasTitle = false;
        if(title != null)
        {
            hasTitle = true;
        }
        return hasTitle;
    }

    public String getTitle()
    {
        return title;
    }

    public boolean hasAbstract()
    {
        boolean hasAbstract = false;
        if(articleAbstract != null)
        {
            hasAbstract = true;
        }
        return hasAbstract;
    }

    public String getAbstract()
    {
        return articleAbstract;
    }

    public boolean hasPMCID()
    {
        boolean hasPMCID = false;
        if(pmcID != null)
        {
            hasPMCID = true;
        }
        return hasPMCID;
    }

    public String getPMCID()
    {
        return pmcID;
    }

    public boolean hasContent()
    {
        boolean hasContent = false;
        if(content != null)
        {
            hasContent = true;
        }
        return hasContent;
    }

    public String getContent()
    {
        return content;
    }

    public String getAvailableText()
    {
        StringBuilder text = new StringBuilder();

        if(hasTitle())
        {
            text.append(getTitle() + " ");
        }

        if(hasAbstract())
        {
            text.append(getAbstract() + " ");
        }

        if(hasContent())
        {
            text.append(getContent());
        }

        return text.toString();
    }
}
