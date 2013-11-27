package uk.ac.bbk.REx.readers;

import org.apache.xpath.NodeSet;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uk.ac.bbk.REx.db.documentDB.DocumentDB;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

public class PubMedDownloader
{
    private final Logger LOGGER = Logger.getLogger(this.getClass().getName());
    private Long timeOfLastQuery;

    private DocumentDB docDB;
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

    public PubMedDownloader(Collection<String> pmids) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException, ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, InterruptedException
    {
        timeOfLastQuery = null;

        docDB = new DocumentDB();
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

        List<String> pmidsList = new ArrayList<String>();
        for(String pmid : pmids)
        {
            if(!docDB.contains(pmid))
            {
                pmidsList.add(pmid);
            }
        }

        LOGGER.info(String.format("Of the %d PubMed IDs provided, the document cache contains %d.",
                pmids.size(), pmids.size()-pmidsList.size()));

        Map<String, String> titles = new HashMap<String, String>();
        Map<String, String> abstracts = new HashMap<String, String>();
        List<String> pmcIDs = new ArrayList<String>();

        //Separate ID's into bundles of 100. Fetching limitless amounts could result in memory problems.
        for(int i=0; i<pmidsList.size(); i+=100)
        {
            int endIndex = i+100;
            if(endIndex > pmidsList.size())
            {
                endIndex = pmidsList.size();
            }

            //Make a comma separated list of IDs to use as a query for eFetch.
            StringBuilder pmidsString = new StringBuilder();
            int count = 0;
            for(int j=i; j<endIndex; j++)
            {
                count++;
                pmidsString.append(pmidsList.get(j));

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
                    if(hasTitle(articleNode))
                    {
                        titles.put(pmid, getTitle(articleNode));
                    }

                    if(hasAbstract(articleNode))
                    {
                        abstracts.put(pmid, getAbstract(articleNode));
                    }

                    pmcIDs.add(getPMCID(articleNode));
                }
                //If it's not in PMC just store the info in the database immediately.
                else
                {
                    StringBuilder availableText = new StringBuilder();

                    if(hasTitle(articleNode))
                    {
                        availableText.append(getTitle(articleNode))
                                     .append(" ");
                    }

                    if(hasAbstract(articleNode))
                    {
                        availableText.append(getAbstract(articleNode));
                    }

                    docDB.put(pmid, availableText.toString());
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

                StringBuilder availableText = new StringBuilder();

                if(titles.containsKey(pmid))
                {
                    availableText.append(titles.get(pmid))
                                 .append(" ");
                }

                if(abstracts.containsKey(pmid))
                {
                    availableText.append(abstracts.get(pmid))
                                 .append(" ");
                }

                if(hasBody(pmcArticleNode))
                {
                    availableText.append(getBody(pmcArticleNode));
                }

                docDB.put(pmid, availableText.toString());
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

    public static void main(String[] args) throws IOException, XPathExpressionException, SAXException, ParserConfigurationException, ClassNotFoundException, SQLException, InstantiationException, InterruptedException, IllegalAccessException
    {
        DocumentDB docDB = new DocumentDB();
        docDB.clearDatabase();
        List<String> pmids = new ArrayList<String>();

        pmids.add("24155869");
        pmids.add("23105108");

        PubMedDownloader dl = new PubMedDownloader(pmids);

        System.out.println(docDB.get("24155869"));
    }
}
