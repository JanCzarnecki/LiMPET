package uk.ac.bbk.REx.readers;

import org.apache.xpath.NodeSet;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

public class PubMedDownloader
{
    private final Logger LOGGER = Logger.getLogger(this.getClass().getName());
    private Long timeOfLastQuery;

    private DocumentBuilder builder;
    private XPath xpath;
    private XPathExpression articlesExp;
    private XPathExpression pmidExp;
    private XPathExpression titleBoolExp;
    private XPathExpression titleExp;
    private XPathExpression abstractBoolExp;
    private XPathExpression abstractExp;
    private XPathExpression pmcBoolExp;
    private XPathExpression pmcExp;
    private XPathExpression pmcArticlesExp;
    private XPathExpression pmidFromPMCExp;
    private XPathExpression bodyBoolExp;
    private XPathExpression bodyExp;

    public PubMedDownloader(List<String> pmids, File outputDir) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException, ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, InterruptedException, TransformerException {
        timeOfLastQuery = null;

        builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        XPathFactory factory = XPathFactory.newInstance();
        xpath = factory.newXPath();

        //Initialise XPaths - all will be used for each PMID.
        articlesExp = xpath.compile("/PubmedArticleSet/PubmedArticle");
        pmidExp = xpath.compile("MedlineCitation/PMID/text()");
        titleBoolExp = xpath.compile("boolean(MedlineCitation/Article/ArticleTitle/text())");
        titleExp = xpath.compile("MedlineCitation/Article/ArticleTitle/text()");
        abstractBoolExp = xpath.compile("boolean(MedlineCitation/Article/Abstract/AbstractText/text())");
        abstractExp = xpath.compile("MedlineCitation/Article/Abstract/AbstractText/text()");
        pmcBoolExp = xpath.compile("boolean(PubmedData/ArticleIdList/ArticleId[@IdType='pmc']/text())");
        pmcExp = xpath.compile("PubmedData/ArticleIdList/ArticleId[@IdType='pmc']/text()");
        pmcArticlesExp = xpath.compile("/pmc-articleset/article");
        pmidFromPMCExp = xpath.compile("front/article-meta/article-id[@pub-id-type='pmid']/text()");
        bodyBoolExp = xpath.compile("boolean(body)");
        bodyExp = xpath.compile("body");

        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

        Map<String, String> titles = new HashMap<String, String>();
        Map<String, String> abstracts = new HashMap<String, String>();
        List<String> pmcIDs = new ArrayList<String>();

        //Separate ID's into bundles of 100. Fetching limitless amounts could result in memory problems.
        for(int i=0; i<pmids.size(); i+=100)
        {
            int endIndex = i+100;
            if(endIndex > pmids.size())
            {
                endIndex = pmids.size();
            }

            //Make a comma separated list of IDs to use as a query for eFetch.
            StringBuilder pmidsString = new StringBuilder();
            int count = 0;
            for(int j=i; j<endIndex; j++)
            {
                count++;
                pmidsString.append(pmids.get(j));

                if(count < endIndex)
                {
                    pmidsString.append(",");
                }
            }

            Document eFetchPubMedDoc = getEFetchXML("db=pubmed&retmode=xml&id=" + pmidsString);

            NodeList articleNodes = (NodeList)articlesExp.evaluate(eFetchPubMedDoc, XPathConstants.NODESET);

            //Go through each returned article and save the necessary info.
            for(int j=0; j<articleNodes.getLength(); j++)
            {
                Node articleNode = articleNodes.item(j);
                String pmid = getPMID(articleNode);

                //If it's in PMC just save the info until we get the full text.
                if(isInPMC(articleNode))
                {
                    pmcIDs.add(getPMCID(articleNode));
                }
                //If it's not in PMC just store the info in the database immediately.
                else
                {
                    File outputFile = new File(outputDir, pmid + ".xml");
                    Writer writer = new BufferedWriter(new FileWriter(outputFile));
                    t.transform(new DOMSource(articleNode), new StreamResult(writer));
                }
            }
        }

        //Separate ID's into bundles of 20. Fetching limitless amounts could result in memory problems.
        for(int i=0; i<pmcIDs.size(); i+=20)
        {
            int endIndex = i+20;
            if(endIndex > pmcIDs.size())
            {
                endIndex = pmcIDs.size();
            }

            //Make a comma separated list of PMC IDs.
            StringBuilder pmcIDsString = new StringBuilder();
            int count = 0;
            for(int j=i; j<endIndex; j++)
            {
                count++;
                pmcIDsString.append(pmcIDs.get(j));

                if(count < pmcIDs.size())
                {
                    pmcIDsString.append(",");
                }
            }

            Document eFetchPMCDoc = getEFetchXML("db=pmc&id=" + pmcIDsString);

            //Go through each PMC article and save the body.
            NodeList pmcArticleNodes = (NodeList)pmcArticlesExp.evaluate(eFetchPMCDoc, XPathConstants.NODESET);

            for(int j=0; j<pmcArticleNodes.getLength(); j++)
            {
                Node pmcArticleNode = pmcArticleNodes.item(j);
                String pmid = getPMIDFromPMCArticle(pmcArticleNode);

                File outputFile = new File(outputDir, pmid + ".xml");
                Writer writer = new BufferedWriter(new FileWriter(outputFile));
                t.transform(new DOMSource(pmcArticleNode), new StreamResult(writer));
            }
        }
    }

    private Document getEFetchXML(String params) throws IOException, SAXException, InterruptedException
    {
        String url = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi";
        URL eUtilsURL = new URL(url);

        //No more than 3 queries should be sent to eUtils per second, so make sure a third of a second has elapsed
        //before sending another.
        if(timeOfLastQuery != null)
        {
            long timeSinceLastQuery = System.currentTimeMillis() - timeOfLastQuery;

            if(timeSinceLastQuery < 333)
            {
                long timeToWait = 333 - timeSinceLastQuery;
                LOGGER.info(String.format("Only %d ms have passed since the last query was sent. " +
                        "The thread will sleep for %d ms.", timeSinceLastQuery));
                Thread.sleep(timeToWait);
            }
            else
            {
                LOGGER.info(String.format("%f seconds have passed since the last query. No need to wait for the next.",
                        (double)timeSinceLastQuery/1000.00));
            }
        }

        LOGGER.info(String.format("Attempting to retrieve eFetch document from url %s with parameters: %s",
                url, params));
        URLConnection con = eUtilsURL.openConnection();
        timeOfLastQuery = new Long(System.currentTimeMillis());
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(params);
        wr.flush();
        wr.close();

        InputStream stream = new BufferedInputStream(con.getInputStream());

        LOGGER.info(String.format("eFetch document successfully retrieved from url %s with parameters: %s",
                url, params));

        Document doc = builder.parse(stream);
        stream.close();
        return doc;
    }

    private String getPMID(Node articleNode) throws XPathExpressionException
    {
        Node pmidNode = (Node)pmidExp.evaluate(articleNode, XPathConstants.NODE);
        return pmidNode.getNodeValue();
    }

    private boolean hasTitle(Node articleNode) throws XPathExpressionException
    {
        String titleBool = (String)titleBoolExp.evaluate(articleNode, XPathConstants.STRING);
        return Boolean.parseBoolean(titleBool);
    }

    private String getTitle(Node articleNode) throws XPathExpressionException
    {
        Node titleNode = (Node)titleExp.evaluate(articleNode, XPathConstants.NODE);
        return titleNode.getNodeValue();
    }

    private boolean hasAbstract(Node articleNode) throws XPathExpressionException
    {
        String abstractBool = (String)abstractBoolExp.evaluate(articleNode, XPathConstants.STRING);
        return Boolean.parseBoolean(abstractBool);
    }

    private String getAbstract(Node articleNode) throws XPathExpressionException
    {
        Node abstractNode = (Node)abstractExp.evaluate(articleNode, XPathConstants.NODE);
        return abstractNode.getNodeValue();
    }

    private boolean isInPMC(Node articleNode) throws XPathExpressionException
    {
        String pmcBool = (String)pmcBoolExp.evaluate(articleNode, XPathConstants.STRING);
        return Boolean.parseBoolean(pmcBool);
    }

    private String getPMCID(Node articleNode) throws XPathExpressionException
    {
        Node pmcNode = (Node)pmcExp.evaluate(articleNode, XPathConstants.NODE);
        return pmcNode.getNodeValue().replaceAll("PMC", "");
    }

    private String getPMIDFromPMCArticle(Node pmcArticleNode) throws XPathExpressionException
    {
        return (String)pmidFromPMCExp.evaluate(pmcArticleNode, XPathConstants.STRING);
    }

    private boolean hasBody(Node pmcArticleNode) throws XPathExpressionException
    {
        return (Boolean)bodyBoolExp.evaluate(pmcArticleNode, XPathConstants.BOOLEAN);
    }

    private String getBody(Node pmcArticleNode) throws XPathExpressionException
    {
        Node bodyNode = (Node)bodyExp.evaluate(pmcArticleNode, XPathConstants.NODE);
        return bodyNode.getTextContent();
    }
}
