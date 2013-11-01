package uk.ac.bbk.REx.readers;

import java.io.*;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.Text;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.xml.sax.SAXException;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import uk.ac.bbk.REx.db.documentDB.DocumentDB;

public class MultiplePubMedReader extends CollectionReader_ImplBase 
{
	private final static Logger LOGGER = Logger.getLogger(MultiplePubMedReader.class.getName());
	private List<String> pmids;
	private int count;
	private DocumentDB documentDB;
	private Set<Pattern> patts;

    /**
     * Initialises a new MultiplePubMedReader which requires two config parameters:
     * queries - a list of PubMed queries (in the form of a JSON list) to use to obtain articles.
     * maxreturn - the maximum number of articles to mine for each query.
     *
     * @throws ResourceInitializationException
     */
	@Override
	public void initialize() throws ResourceInitializationException
	{
		LOGGER.log(Level.INFO, "Initialising PubMed Reader.");
		Map<String, Integer> pmidsMap = new HashMap<String, Integer>();
		count = 0;
		try
		{
			documentDB = new DocumentDB();
		}
		catch (InstantiationException e)
		{
			throw new ResourceInitializationException(e);
		}
		catch (IllegalAccessException e)
		{
			throw new ResourceInitializationException(e);
		}
		catch (ClassNotFoundException e)
		{
			throw new ResourceInitializationException(e);
		}
		catch (SQLException e)
		{
			throw new ResourceInitializationException(e);
		}
		
		int maxReturn = (Integer)getConfigParameterValue("maxReturn");
		
		String queriesJSON = (String)getConfigParameterValue("queries");
		LOGGER.log(Level.INFO, "Recieved queries JSON: " + queriesJSON);
		Gson gson = new Gson();
		Type setType = new TypeToken<Set<String>>(){}.getType();
		Set<String> queries = gson.fromJson(queriesJSON, setType);
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
		
		String esearchURLString = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed";
		
		SAXBuilder sax = new SAXBuilder();
		XPathFactory xp = XPathFactory.instance();
		XPathExpression<Text> idX = xp.compile("//eSearchResult/IdList/Id/text()", Filters.text());

		for(String query : encodedQueries)
		{
			URL esearchURL;
			try 
			{
				esearchURL = new URL(esearchURLString + "&term=" + query + "&retmax=" + maxReturn);
			} 
			catch (MalformedURLException e) 
			{
				throw new ResourceInitializationException(e);
			}


			Document doc;
			try 
			{
                InputStream is = new BufferedInputStream(esearchURL.openStream());
				doc = sax.build(is);
                is.close();
			}
			catch (JDOMException e)
			{
				throw new ResourceInitializationException(e);
			}
			catch (IOException e)
			{
				throw new ResourceInitializationException(e);
			}

            int count = 0;
			for(Text id : idX.evaluate(doc))
			{
                count++;
                String pmid = id.getText();
                if(pmidsMap.containsKey(pmid))
                {
                    if(pmidsMap.get(pmid) > count)
                    {
                        pmidsMap.put(pmid, count);
                    }
                }
                else
                {
                    pmidsMap.put(pmid, count);
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
		
		patts = new HashSet<Pattern>();
		patts.add(Pattern.compile("(?:Materials and [Mm]ethods\\s+(?!Results)).*"));
		patts.add(Pattern.compile("(?:MATERIALS AND METHODS\\s+(?!RESULTS)).*"));
		patts.add(Pattern.compile("(?:Methods\\s+(?!Results)).*"));
		patts.add(Pattern.compile("(?:METHODS\\s+(?!RESULTS)).*"));
		patts.add(Pattern.compile("(?:Experimental (P|p)rocedures\\s+(?!Results)).*"));
		patts.add(Pattern.compile("(?:EXPERIMENTAL PROCEDURES\\s+(?!RESULTS)).*"));
		patts.add(Pattern.compile("(?:Results\\s+(?!Discussion)).*"));
		patts.add(Pattern.compile("(?:RESULTS\\s+(?!DISCUSSION)).*"));
		patts.add(Pattern.compile("(?:Results and (D|d)iscussion\\s+(?!References)).*"));
		patts.add(Pattern.compile("(?:RESULTS AND DISCUSSION\\s+(?!REFERENCES)).*"));
	}

    /**
     * Populates the provided CAS with the next article in the sequence of PubMed articles.
     *
     * @param cas An empty CAS which will be populated which a document.
     * @throws IOException
     * @throws CollectionException
     */
	@Override
	public void getNext(CAS cas) throws IOException, CollectionException 
	{
		String pmid = pmids.get(count);
		String content = "";
		
		if(documentDB.contains(pmid))
		{
			content = documentDB.get(pmid);
		}
		else
		{
			Article pmArticle;
			try
			{
				pmArticle = new Article(pmid);
			} 
			catch (XPathExpressionException e)
			{
				throw new CollectionException(e);
			} 
			catch (ParserConfigurationException e)
			{
				throw new CollectionException(e);
			} 
			catch (SAXException e)
			{
				throw new CollectionException(e);
			} 
			catch (TransformerException e)
			{
				throw new CollectionException(e);
			}
			
			//Retrieve the article text.
			content = pmArticle.getFullText();
			
			//Just keep the Introduction.
			/*
			for(Pattern patt : patts)
			{
				Matcher mat = patt.matcher(content);
				content = mat.replaceAll("");
			}
			*/
			
			documentDB.put(pmid, content);
		}
		
		LOGGER.log(Level.FINE, "Recieved content: " + content);
		
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
		
		docAnnotation.setId(pmid);
		docAnnotation.addToIndexes();
		count++;
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
			new ProgressImpl(count, pmids.size(), Progress.ENTITIES)
		};
	}

    /**
     * Determine if there are any more articles remaining.
     *
     * @return
     * @throws IOException
     * @throws CollectionException
     */
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

}
