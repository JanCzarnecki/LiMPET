package uk.ac.bbk.REx.utils;

import hu.u_szeged.rgai.bio.uima.tagger.LinnaeusSpecies;

import java.util.*;

import opennlp.uima.Sentence;
import opennlp.uima.Token;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.EmptyFSList;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.tcas.Annotation;

import uk.ac.bbk.REx.types.Chemical;
import uk.ac.bbk.REx.types.Document;
import uk.ac.bbk.REx.types.Gene;
import uk.ac.bbk.REx.types.ReactionKeyword;
import uk.ac.ebi.mdk.domain.annotation.InChI;
import uk.ac.ebi.mdk.domain.entity.Metabolite;

public class JCasUtils
{
	private JCasUtils(){}
	
	/**
	 * Returns a user readable overview of the text and the location of annotations.
	 * 
	 * @param jcas the marked up document.
	 * @return a String contains the overview.
	 */
	public static String getGraphicalOutput(JCas jcas)
	{
		StringBuilder output = new StringBuilder();
		
		for(Annotation sentAnnotation : jcas.getAnnotationIndex(Sentence.type))
		{
			Sentence sent = (Sentence)sentAnnotation;
			
			output.append("                    " + sent.getCoveredText() + "\n");
			output.append("GENES:              " + getGraphicalSubOutput(jcas, sent, Gene.type) + "\n");
			output.append("CHEMICALS:          " + getGraphicalSubOutput(jcas, sent, Chemical.type) + "\n");
			output.append("REACTION KEYWORDS:  " + getGraphicalSubOutput(jcas, sent, ReactionKeyword.type) + "\n");
			output.append("ORGANISMS:          " + getGraphicalSubOutput(jcas, sent, LinnaeusSpecies.type) + "\n");
			output.append("TOKENS:             " + getGraphicalSubOutput(jcas, sent, Token.type) + "\n");
			
			output.append("\n\n");
		}
		
		return output.toString();
	}
	
	private static String getGraphicalSubOutput(JCas jcas, Sentence aSentence, int aType)
	{
		StringBuilder output = new StringBuilder();
		
		Iterator<Annotation> subIterator = jcas.getAnnotationIndex(aType).subiterator(aSentence);
		int pos = 0;
		while(subIterator.hasNext())
		{
			Annotation gene = subIterator.next();
			
			while(pos<gene.getBegin()-aSentence.getBegin())
			{
				output.append(" ");
				pos++;
			}
			
			for(int i=gene.getBegin(); i<gene.getEnd(); i++)
			{
				output.append("-");
				pos++;
			}
		}
		
		return output.toString();
	}
	
	/**
	 * Removes any acronyms in the text which are with their definition.
	 * 
	 * @param annotations
	 * @return
	 */
	public static <A extends uk.ac.bbk.REx.types.Annotation> List<A> removeAcronymsWithDefinition(List<A> annotations)
	{
		List<A> output = new ArrayList<A>();
		
		for(A annotation : annotations)
		{
			if(annotation.getIsAcronym()
					&& !annotation.getIsWithDefinition())
			{
				output.add(annotation);
			}
			else if(!annotation.getIsAcronym())
			{
				output.add(annotation);
			}
		}
		
		return output;
	}
	
	public static uk.ac.bbk.REx.types.Annotation getParentAnnotation(uk.ac.bbk.REx.types.Annotation annotation)
	{
		Set<uk.ac.bbk.REx.types.Annotation> seen = new HashSet<uk.ac.bbk.REx.types.Annotation>();
		
		while(annotation.getRefersTo() != null
				&& !seen.contains(annotation.getRefersTo()))
		{
			annotation = annotation.getRefersTo();
			seen.add(annotation);
		}
		
		return annotation;
	}
	
	/**
	 * Gets the full name of an entity, ie. if it is an acronym, get the entity that it refers to.
	 * 
	 * @param annotation
	 * @return
	 */
	public static String getFullName(uk.ac.bbk.REx.types.Annotation annotation)
	{
		uk.ac.bbk.REx.types.Annotation parent = getParentAnnotation(annotation);
		
		return parent.getCoveredText();
	}
	
	public static String getID(Chemical c)
	{
		if(getInchi(c) == null)
		{
			return c.getCoveredText().replaceAll("[^A-Za-z0-9]", "");
		}
		else
		{
			return getInchi(c).replaceAll("[^A-Za-z0-9]", "");
		}
	}
	
	public static String getInchi(Chemical c)
	{
        String inchi = c.getInChiString();

        if(inchi == null)
        {
            Chemical parent = (Chemical)getParentAnnotation(c);
            inchi = parent.getInChiString();
        }
		
		return inchi;
	}
	
	public static String getID(Gene g)
	{
		Gene parent = (Gene)getParentAnnotation(g);
		
		return JCasUtils.getFullName(parent).replaceAll("\\W", "").toLowerCase();
	}
	
	/**
	 * FSLists are the only type of collection that an annotation can hold. This method converts a collection
	 * to an FSList.
	 * 
	 * @param aJCas
	 * @param aCollection
	 * @return
	 */
	public static <A extends Annotation> FSList createFSList(JCas aJCas, Collection<A> aCollection)
    {
            if (aCollection.size() == 0) {
                    return new EmptyFSList(aJCas);
            }

            NonEmptyFSList head = new NonEmptyFSList(aJCas);
            NonEmptyFSList list = head;
            Iterator<A> i = aCollection.iterator();
            while (i.hasNext()) {
                    head.setHead(i.next());
                    if (i.hasNext()) {
                            head.setTail(new NonEmptyFSList(aJCas));
                            head = (NonEmptyFSList) head.getTail();
                    }
                    else {
                            head.setTail(new EmptyFSList(aJCas));
                    }
            }

            return list;
    }
	
	/**
	 * Converts an FSList to a List.
	 * 
	 * @param fslist
	 * @return
	 */
	public static <A extends Annotation> List<A> convertFSListToCollection(FSList fslist)
	{
		List<A> output = new ArrayList<A>();
		
		while(fslist instanceof NonEmptyFSList)
		{
			@SuppressWarnings("unchecked")
			A head = (A)((NonEmptyFSList)fslist).getHead();
			output.add(head);
			fslist = ((NonEmptyFSList)fslist).getTail();
		}
		
		return output;
	}

    /**
     * Retrieve the PubMed ID of the article that a CAS originates from.
     *
     * @param cas
     * @return
     */
	public static String getPubMedID(JCas cas)
	{
		//Retrieve the PubMed ID from the document annotation.
		String pmid = "";
		for(Annotation annotationD : cas.getAnnotationIndex(Document.type))
		{
			Document d = (Document)annotationD;
			pmid = d.getId();
		}
		
		return pmid;
	}

    public static List<Metabolite> metabolitesInCAS(Collection<Metabolite> metabolites, JCas jcas)
    {
        Map<String, Metabolite> inchis = new HashMap<String, Metabolite>();
        for(Metabolite m : metabolites)
        {
            for(InChI inchi : m.getAnnotations(InChI.class))
            {
                inchis.put(inchi.toInChI(), m);
            }
        }

        Set<Metabolite> metabolitesFound = new HashSet<Metabolite>();

        for(Annotation chemicalAnnotation : jcas.getAnnotationIndex(Chemical.type))
        {
            Chemical chemical = (Chemical)chemicalAnnotation;

            if(chemical.getInChiString() != null)
            {
                String inchi = chemical.getInChiString();
                if(inchis.containsKey(inchi))
                {
                    metabolitesFound.add(inchis.get(inchi));
                }
            }
        }

        return new ArrayList<Metabolite>(metabolitesFound);
    }

    public static int uniqueMetabolitesInCAS(JCas jcas)
    {
        Set<String> inchis = new HashSet<String>();
        Set<String> names = new HashSet<String>();

        for(Annotation chemicalAnnotation : jcas.getAnnotationIndex(Chemical.type))
        {
            Chemical chemical = (Chemical)chemicalAnnotation;

            if(chemical.getInChiString() != null)
            {
                String inchi = chemical.getInChiString();
                inchis.add(inchi);
            }
            else
            {
                names.add(chemical.getCoveredText());
            }
        }

        int total = inchis.size() + names.size();
        return total;
    }
}
