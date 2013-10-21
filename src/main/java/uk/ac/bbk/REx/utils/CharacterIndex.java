package uk.ac.bbk.REx.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * CharacterIndex is more usable index for UIMA annotations.
 * 
 * @param <A> a UIMA annotation type
 */
public class CharacterIndex<A extends Annotation>
{
	private JCas jcas;
	private Map<Integer, Set<A>> index;

    /**
     * Initialise an index of annotations of a particular type within a CAS.
     *
     * @param aJcas The CAS containing annotations to be indexed.
     * @param aType The type of annotation to be indexed.
     */
	public CharacterIndex(JCas aJcas, int aType)
	{
		jcas = aJcas;
		index = new HashMap<Integer, Set<A>>();
		
		for(Annotation annotation : jcas.getAnnotationIndex(aType))
		{
			A castedAnnotation = (A)annotation;
			for(int i = castedAnnotation.getBegin(); i < castedAnnotation.getEnd(); i++)
			{
				if(!index.containsKey(i))
				{
					index.put(i, new HashSet<A>());
				}
				
				index.get(i).add(castedAnnotation);
			}
		}
	}

    /**
     * Determine if an annotation is present at a particular index in the document.
     *
     * @param anIndex The index at which to check for an annotation.
     * @return
     */
	public boolean isAnnotationAtIndex(int anIndex)
	{
        return index.containsKey(anIndex);
	}

    /**
     * Get the annotation at a particular index. If multiple annotations are found, an IllegalArgumentException
     * is thrown.
     *
     * @param anIndex The index at which to find the annotation.
     * @return
     */
	public A getAnnotationAtIndex(int anIndex)
	{
		if(index.get(anIndex).size() == 1)
		{
			A output = null;
			
			for(A annotation : index.get(anIndex))
			{
				output = annotation;
			}
			
			return output;
		}
		else
		{
			throw new IllegalArgumentException("Index " + anIndex + " does not contain a single annotation.");
		}
	}

    /**
     * Get all annotations at a particular index.
     *
     * @param anIndex The index at which to find the annotations.
     * @return
     */
	public Set<A> getAnnotationsAtIndex(int anIndex)
	{
		return index.get(anIndex);
	}

    /**
     * Get all annotations that overlap with the provided annotation. The provided annotation can be any type.
     *
     * @param annotation An annotation of any type.
     * @return
     */
	public List<A> getOverlappingAnnotations(Annotation annotation)
	{
		List<A> overlappingAnnotations = new ArrayList<A>();
		Set<A> encounteredAnnotations = new HashSet<A>();
		
		for(int i = annotation.getBegin(); i < annotation.getEnd(); i++)
		{
			if(isAnnotationAtIndex(i)
					&& !encounteredAnnotations.containsAll(getAnnotationsAtIndex(i)))
			{
				overlappingAnnotations.addAll(getAnnotationsAtIndex(i));
				encounteredAnnotations.addAll(getAnnotationsAtIndex(i));
			}
		}
		
		return overlappingAnnotations;
	}

    /**
     * Get all annotations that occur before the provided annotation. The provided annotation can be any type.
     *
     * @param annotation An annotation of any type.
     * @return
     */
	public List<A> getAnnotationsBefore(Annotation annotation)
	{
		List<A> annotationsBefore = new ArrayList<A>();
		Set<A> encounteredAnnotations = new HashSet<A>();
		
		for(int i=0; i<annotation.getBegin(); i++)
		{
			if(isAnnotationAtIndex(i)
					&& !encounteredAnnotations.contains(getAnnotationAtIndex(i)))
			{
				annotationsBefore.addAll(getAnnotationsAtIndex(i));
				encounteredAnnotations.addAll(getAnnotationsAtIndex(i));
			}
		}
		
		return annotationsBefore;
	}

    /**
     * Get all annotations that occur between the provided annotations. The provided annotations can be any type.
     *
     * @param firstAnnotation An annotation of any type.
     * @param secondAnnotation An annotation of any type.
     * @return
     */
	public List<A> getAnnotationsBetween(Annotation firstAnnotation, Annotation secondAnnotation)
	{
		List<A> annotationsBetween = new ArrayList<A>();
		Set<A> encounteredAnnotations = new HashSet<A>();
		
		for(int i=firstAnnotation.getEnd()+1; i<secondAnnotation.getBegin(); i++)
		{
			if(isAnnotationAtIndex(i)
					&& !encounteredAnnotations.contains(getAnnotationAtIndex(i)))
			{
				annotationsBetween.addAll(getAnnotationsAtIndex(i));
				encounteredAnnotations.addAll(getAnnotationsAtIndex(i));
			}
		}
		
		return annotationsBetween;
	}

    /**
     * Get all annotations that occur between two indices.
     *
     * @return
     */
	public List<A> getAnnotationsBetween(int start, int end)
	{
		List<A> annotationsBetween = new ArrayList<A>();
		Set<A> encounteredAnnotations = new HashSet<A>();
		
		for(int i=start+1; i<end; i++)
		{
			if(isAnnotationAtIndex(i)
					&& !encounteredAnnotations.contains(getAnnotationAtIndex(i)))
			{
				annotationsBetween.addAll(getAnnotationsAtIndex(i));
				encounteredAnnotations.addAll(getAnnotationsAtIndex(i));
			}
		}
		
		return annotationsBetween;
	}
}
