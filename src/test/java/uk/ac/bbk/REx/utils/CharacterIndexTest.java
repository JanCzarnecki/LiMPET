package uk.ac.bbk.REx.utils;

import opennlp.uima.Sentence;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.InvalidXMLException;
import org.junit.Before;
import org.junit.Test;
import uk.ac.bbk.REx.test.TestCASGenerator;
import uk.ac.bbk.REx.types.Chemical;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class CharacterIndexTest
{
    JCas jcas;
    CharacterIndex<Chemical> index;

    @Before
    public void initialise() throws CASException, ResourceInitializationException, InvalidXMLException, IOException
    {
        jcas = TestCASGenerator.generateTestJCas();
        index = new CharacterIndex<Chemical>(jcas, Chemical.type);
    }

    @Test
    public void boundaryTest1()
    {
        assertFalse(index.isAnnotationAtIndex(68));
    }

    @Test
    public void boundaryTest2()
    {
        assertTrue(index.isAnnotationAtIndex(69));
    }

    @Test
    public void boundaryTest3()
    {
        assertTrue(index.isAnnotationAtIndex(76));
    }

    @Test
    public void boundaryTest4()
    {
        assertFalse(index.isAnnotationAtIndex(77));
    }

    @Test
    public void overlapTest()
    {
        Sentence sent1 = null;
        for(Annotation a : jcas.getAnnotationIndex(Sentence.type))
        {
            sent1 = (Sentence)a;
            break;
        }

        List<Integer> begins = new ArrayList<Integer>();
        for(Chemical c : index.getOverlappingAnnotations(sent1))
        {
            begins.add(c.getBegin());
        }

        Collections.sort(begins);
        assertEquals("[69, 81]", begins.toString());
    }
}
