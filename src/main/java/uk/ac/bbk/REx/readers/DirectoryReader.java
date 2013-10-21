package uk.ac.bbk.REx.readers;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
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

/**
 * A UIMA collection reader that, given an absolute path to a directory, fetches all documents
 * and extracts the title, abstract and, if possible, the introduction.
 */
public class DirectoryReader extends CollectionReader_ImplBase
{
	private File[] docs;
	int count;
	Pattern pdfExtPatt;
	
	@Override
	public void initialize() throws ResourceInitializationException
	{
		getUimaContext().getLogger().log(Level.INFO, "Initialising Directory Reader.");
		File directory = new File((String)getConfigParameterValue("directory"));
		docs = directory.listFiles();
		count = 0;
		pdfExtPatt = Pattern.compile(".*\\.pdf$");
	}
	
	@Override
	public void getNext(CAS cas) throws IOException, CollectionException
	{
		File doc = docs[count];
		getUimaContext().getLogger().log(Level.INFO, "Retrieving document " + doc.getName());
		count++;
		Matcher pdfExtMat = pdfExtPatt.matcher(doc.getName());
		String content = "";
		
		if(pdfExtMat.find())
		{
			PDDocument pdf = PDDocument.load(doc);
			PDFTextStripper stripper = new PDFTextStripper();
			content = stripper.getText(pdf);
			pdf.close();
		}
		else
		{
			content = FileUtils.readFileToString(doc);
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
		
		docAnnotation.setId(doc.getName());
		docAnnotation.addToIndexes();
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
