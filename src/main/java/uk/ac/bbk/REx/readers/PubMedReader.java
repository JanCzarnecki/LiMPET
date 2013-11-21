package uk.ac.bbk.REx.readers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uk.ac.bbk.REx.db.documentDB.DocumentDB;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.*;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

public class PubMedReader extends CollectionReader_ImplBase
{
    private final Logger LOGGER = Logger.getLogger(this.getClass().getName());
    private DocumentDB documentDB;
    private List<String> pmids;
    private int count;
    private Long timeOfLastQuery;

    @Override
    public void initialize() throws ResourceInitializationException
    {
        timeOfLastQuery = null;
        LOGGER.info("Initialising PubMed Reader.");
        try
        {
            documentDB = new DocumentDB();
        }
        catch(Exception e)
        {
            throw new ResourceInitializationException(e);
        }

        count = 0;

        int maxReturn = (Integer)getConfigParameterValue("maxReturn");

        String queriesJSON = (String)getConfigParameterValue("queries");
        LOGGER.info("Recieved queries JSON: " + queriesJSON);
        Gson gson = new Gson();
        Type listType = new TypeToken<List<String>>(){}.getType();
        List<String> queries = gson.fromJson(queriesJSON, listType);
        List<String> encodedQueries = new ArrayList<String>();
        for(String query : queries)
        {
            try
            {
                encodedQueries.add(URLEncoder.encode(query, "UTF-8"));
            }
            catch (UnsupportedEncodingException e)
            {
                throw new ResourceInitializationException(e);
            }
        }

        String esearchURLString = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi";
        URL esearchURL;
        try
        {
            esearchURL = new URL(esearchURLString);
        }
        catch (MalformedURLException e)
        {
            throw new ResourceInitializationException();
        }
        String paramsFormat = "db=pubmed&term=%s&retmax=%s";

        DocumentBuilder builder;
        try
        {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        }
        catch (ParserConfigurationException e)
        {
            throw new ResourceInitializationException(e);
        }

        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        XPathExpression idsExp;
        try
        {
            idsExp = xpath.compile("/eSearchResult/IdList/Id/text()");
        }
        catch (XPathExpressionException e)
        {
            throw new ResourceInitializationException(e);
        }

        Map<String, Integer> pmidsMap = new HashMap<String, Integer>();

        for(String query : encodedQueries)
        {
            String params = String.format(paramsFormat, query, maxReturn);

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
                    try
                    {
                        Thread.sleep(timeToWait);
                    }
                    catch (InterruptedException e)
                    {
                        throw new ResourceInitializationException(e);
                    }
                }
                else
                {
                    LOGGER.info(String.format("%f seconds have passed since the last query. No need to wait for the next.",
                            (double)timeSinceLastQuery/1000.00));
                }
            }

            Document doc;
            try
            {
                LOGGER.info(String.format("Attempting to retrieve eSearch document from the URL %s " +
                        "and with the following POST parameters: %s", esearchURLString, params));
                URLConnection con = esearchURL.openConnection();
                timeOfLastQuery = new Long(System.currentTimeMillis());
                con.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes(params);
                wr.flush();
                wr.close();

                InputStream stream = new BufferedInputStream(con.getInputStream());

                LOGGER.info(String.format("eSearch document successfully retrieved from the URL %s " +
                        "with the following POST parameters: %s", esearchURLString, params));

                doc = builder.parse(stream);
            }
            catch(IOException e)
            {
                throw new ResourceInitializationException(e);
            }
            catch(SAXException e)
            {
                throw new ResourceInitializationException(e);
            }

            NodeList pmids;
            try
            {
                pmids = (NodeList)idsExp.evaluate(doc, XPathConstants.NODESET);
            }
            catch (XPathExpressionException e)
            {
                throw new ResourceInitializationException(e);
            }

            for(int i=0; i<pmids.getLength(); i++)
            {
                String pmid = pmids.item(i).getNodeValue();
                if(pmidsMap.containsKey(pmid))
                {
                    if(pmidsMap.get(pmid) > i+1)
                    {
                        pmidsMap.put(pmid, i+1);
                    }
                }
                else
                {
                    pmidsMap.put(pmid, i+1);
                }
            }
        }

        String pmidCutoffsPath = (String)getConfigParameterValue("pmidCutOffs");

        if(pmidCutoffsPath != null)
        {
            StringBuilder pmidsLog = new StringBuilder();
            for(String pmid : pmidsMap.keySet())
            {
                pmidsLog.append(pmid + "," + pmidsMap.get(pmid) + "\n");
            }

            try
            {
                Writer w = new BufferedWriter(new FileWriter(new File(pmidCutoffsPath)));
                w.write(pmidsLog.toString());
                w.close();
            }
            catch (IOException e)
            {
                throw new ResourceInitializationException(e);
            }
        }

        pmids = new ArrayList<String>(pmidsMap.keySet());
        try
        {
            PubMedDownloader pmd = new PubMedDownloader(pmids);
        }
        catch(Exception e)
        {
            throw new ResourceInitializationException(e);
        }
    }

    @Override
    public void getNext(CAS cas) throws IOException, CollectionException
    {
        String nextPMID = pmids.get(count);

        String content;
        if(documentDB.contains(nextPMID))
        {
            content = documentDB.get(nextPMID);
        }
        else
        {
            content = " ";
        }

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

        docAnnotation.setId(nextPMID);
        docAnnotation.addToIndexes();
        count++;
    }

    @Override
    public boolean hasNext() throws IOException, CollectionException
    {
        if(count == pmids.size())
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    @Override
    public Progress[] getProgress()
    {
        return new Progress[]
        {
                new ProgressImpl(count, pmids.size(), Progress.ENTITIES)
        };
    }

    @Override
    public void close() throws IOException
    {
        try
        {
            documentDB.close();
        }
        catch (SQLException e)
        {
            throw new IOException(e);
        }
    }
}
