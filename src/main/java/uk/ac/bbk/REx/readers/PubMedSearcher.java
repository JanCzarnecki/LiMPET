package uk.ac.bbk.REx.readers;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class PubMedSearcher
{
    private final Logger LOGGER = Logger.getLogger(this.getClass().getName());
    private Long timeOfLastQuery;

    private String eSearchURLString;
    private URL eSearchURL;
    private String paramsFormat;
    private DocumentBuilder builder;
    private XPathExpression idsExp;

    public PubMedSearcher() throws XPathExpressionException, MalformedURLException, ParserConfigurationException
    {
        timeOfLastQuery = null;

        eSearchURLString = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi";
        eSearchURL = new URL(eSearchURLString);
        paramsFormat = "db=pubmed&term=%s&retmax=%d";

        builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        idsExp = xpath.compile("/eSearchResult/IdList/Id/text()");
    }

    public List<String> getPMIDs(String query, int retMax) throws IOException, SAXException, XPathExpressionException, InterruptedException
    {
        String params = String.format(paramsFormat, URLEncoder.encode(query, "UTF-8"), retMax);

        LOGGER.info(String.format("Attempting to retrieve eSearch document from url %s with parameters: %s",
                eSearchURLString, params));

        URLConnection con = eSearchURL.openConnection();
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(params);
        wr.flush();
        wr.close();

        InputStream stream = new BufferedInputStream(con.getInputStream());

        LOGGER.info(String.format("eFetch document successfully retrieved from url %s with parameters: %s",
                eSearchURLString, params));

        Document doc = builder.parse(stream);
        NodeList pmidNodes = (NodeList)idsExp.evaluate(doc, XPathConstants.NODESET);

        List<String> pmids = new ArrayList<String>();
        for(int i=0; i<pmidNodes.getLength(); i++)
        {
            String pmid = pmidNodes.item(i).getNodeValue();
            pmids.add(pmid);
        }
        return pmids;
    }
}
