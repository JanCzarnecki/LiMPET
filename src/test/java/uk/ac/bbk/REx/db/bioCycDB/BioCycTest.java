package uk.ac.bbk.REx.db.bioCycDB;

import org.junit.Before;
import org.junit.Test;
import uk.ac.bbk.REx.exception.BioCycException;

import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;

public class BioCycTest
{
    private BioCyc bc;

    @Before
    public void initialise() throws BioCycException {
        bc = new BioCyc();
    }

    @Test
    public void getReactionIDsTest() throws BioCycException {
        List<String> reactionIDs = bc.getReactionIDs("META", "PWY-5694");
        assertEquals("[ALLANTOICASE-RXN, ALLANTOINASE-RXN, UREIDOGLYCOLATE-LYASE-RXN]", reactionIDs.toString());
    }

    @Test
    public void getSubstrateIDsTest() throws BioCycException {
        List<String> substrateIDs = bc.getSubstrateIDs("META", "UREIDOGLYCOLATE-LYASE-RXN");
        assertEquals("[CPD-1091]", substrateIDs.toString());
    }

    @Test
    public void getProductIDsTest() throws BioCycException {
        List<String> productIDs = bc.getProductIDs("META", "UREIDOGLYCOLATE-LYASE-RXN");
        assertEquals("[UREA, GLYOX]", productIDs.toString());
    }

    @Test
    public void getCompoundNameAndInchiTest() throws BioCycException {
        Map<String, String> nameAndInchi = bc.getCompoundNameAndInchi("META", "CPD-1091");
        assertEquals("S-ureidoglycolate", nameAndInchi.get("name"));
        assertEquals("InChI=1S/C3H6N2O4/c4-3(9)5-1(6)2(7)8/h1,6H,(H,7,8)(H3,4,5,9)/p-1/t1-/m0/s1", nameAndInchi.get("inchi"));
    }
}
