package uk.ac.bbk.REx.db.bioCycDB;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uk.ac.bbk.REx.exception.BioCycException;

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
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class BioCyc
{
    private final Logger LOGGER = Logger.getLogger(this.getClass().getName());

    private String getXML = "http://websvc.biocyc.org/getxml?";
    private String apiXML = "http://websvc.biocyc.org/apixml?";
    private long timeOfLastQuery;
    private DocumentBuilder builder;

    private XPath xpath;
    private XPathExpression subPathwayIDsExp;
    private XPathExpression reactionIDsExp;
    private XPathExpression substrateIDsExp;
    private XPathExpression productIDsExp;
    private XPathExpression compoundNameExp;
    private XPathExpression compoundInchiExp;

    public BioCyc() throws BioCycException {
        timeOfLastQuery = 0;
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new BioCycException(e);
        }
        XPathFactory factory = XPathFactory.newInstance();
        xpath = factory.newXPath();

        try {
            subPathwayIDsExp = xpath.compile("/ptools-xml/Pathway/reaction-list/Pathway/@frameid");
            reactionIDsExp = xpath.compile("/ptools-xml/Pathway/reaction-list/Reaction/@frameid");
            substrateIDsExp = xpath.compile("/ptools-xml/Reaction/left/Compound/@frameid");
            productIDsExp = xpath.compile("/ptools-xml/Reaction/right/Compound/@frameid");
            compoundNameExp = xpath.compile("/ptools-xml/Compound/cml/molecule/@title");
            compoundInchiExp = xpath.compile("/ptools-xml/Compound/inchi/text()");
        } catch (XPathExpressionException e) {
            throw new BioCycException(e);
        }
    }

    public List<String> getReactionIDs(String dataset, String pathwayID) throws BioCycException {
        String params = dataset + ":" + pathwayID;
        Document doc = null;
        try {
            doc = request(getXMLURL(params), 1000);
        } catch (MalformedURLException e) {
            throw new BioCycException(e);
        }
        List<String> reactionIDs = new ArrayList<String>();

        NodeList subPathwayIDNodes = null;
        try {
            subPathwayIDNodes = (NodeList)subPathwayIDsExp.evaluate(doc, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new BioCycException(e);
        }
        for(int i=0; i<subPathwayIDNodes.getLength(); i++)
        {
            String subPathwayID = subPathwayIDNodes.item(i).getNodeValue();
            reactionIDs.addAll(getReactionIDs(dataset, subPathwayID));
        }

        NodeList reactionIDNodes = null;
        try {
            reactionIDNodes = (NodeList)reactionIDsExp.evaluate(doc, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new BioCycException(e);
        }
        for(int i=0; i<reactionIDNodes.getLength(); i++)
        {
            String reactionID = reactionIDNodes.item(i).getNodeValue();
            reactionIDs.add(reactionID);
        }

        return reactionIDs;
    }

    public List<String> getSubstrateIDs(String dataset, String reactionID) throws BioCycException {
        String params = dataset + ":" + reactionID;
        Document doc = null;
        try {
            doc = request(getXMLURL(params), 1000);
        } catch (MalformedURLException e) {
            throw new BioCycException(e);
        }
        List<String> substrateIDs = new ArrayList<String>();

        NodeList substrateIDNodes = null;
        try {
            substrateIDNodes = (NodeList)substrateIDsExp.evaluate(doc, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new BioCycException(e);
        }
        for(int i=0; i<substrateIDNodes.getLength(); i++)
        {
            String substrateID = substrateIDNodes.item(i).getNodeValue();
            substrateIDs.add(substrateID);
        }

        return substrateIDs;
    }

    public List<String> getProductIDs(String dataset, String reactionID) throws BioCycException {
        String params = dataset + ":" + reactionID;
        Document doc = null;
        try {
            doc = request(getXMLURL(params), 1000);
        } catch (MalformedURLException e) {
            throw new BioCycException(e);
        }
        List<String> productIDs = new ArrayList<String>();

        NodeList productIDNodes = null;
        try {
            productIDNodes = (NodeList)productIDsExp.evaluate(doc, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new BioCycException(e);
        }
        for(int i=0; i<productIDNodes.getLength(); i++)
        {
            String productID = productIDNodes.item(i).getNodeValue();
            productIDs.add(productID);
        }

        return productIDs;
    }

    public Map<String, String> getCompoundNameAndInchi(String dataset, String compoundID) throws BioCycException {
        String params = dataset + ":" + compoundID;
        Document doc = null;
        try {
            doc = request(getXMLURL(params), 1000);
        } catch (MalformedURLException e) {
            throw new BioCycException(e);
        }
        Map<String, String> output = new HashMap<String, String>();

        Node nameNode = null;
        try {
            nameNode = (Node)compoundNameExp.evaluate(doc, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new BioCycException(e);
        }
        output.put("name", nameNode.getNodeValue());

        Node inchiNode = null;
        try {
            inchiNode = (Node)compoundInchiExp.evaluate(doc, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new BioCycException(e);
        }

        if(inchiNode == null)
        {
            output.put("inchi", null);
        }
        else
        {
            output.put("inchi", inchiNode.getNodeValue());
        }

        return output;
    }

    private URL getXMLURL(String params) throws MalformedURLException {
        return new URL(getXML + params);
    }

    private URL apiXMLURL(String params) throws MalformedURLException {
        return new URL(apiXML + params);
    }

    private Document request(URL url, long waitTime) throws BioCycException {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastQuery = currentTime - timeOfLastQuery;
        if(timeSinceLastQuery < waitTime)
        {
            long timeToWait = waitTime - timeSinceLastQuery;
            LOGGER.info(String.format("Only %d ms has passed since the last BioCyc request. Waiting for %d ms.",
                    timeSinceLastQuery, timeToWait));
            try {
                Thread.sleep(timeToWait);
            } catch (InterruptedException e) {
                throw new BioCycException(e);
            }
        }

        LOGGER.info(String.format("Requesting URL %s", url.toString()));
        try {
            URLConnection con = url.openConnection();
            timeOfLastQuery = System.currentTimeMillis();
            InputStream stream = new BufferedInputStream(con.getInputStream());
            Document doc = builder.parse(stream);
            stream.close();
            LOGGER.info(String.format("URL %s successfully retrieved and parsed.", url.toString()));
            return doc;
        } catch (IOException e) {
            LOGGER.info(String.format("Error retrieving URL %s", url.toString()));
            throw new BioCycException(e);
        } catch (SAXException e) {
            LOGGER.info(String.format("Error parsing document retrieved from URL %s", url.toString()));
            throw new BioCycException(e);
        }
    }
}
