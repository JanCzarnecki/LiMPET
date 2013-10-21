package uk.ac.bbk.REx.db;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.ac.bbk.REx.db.chebiDB.CHEBIDB;
import uk.ac.bbk.REx.exception.CHEBIException;
import uk.ac.bbk.REx.exception.NameNotFoundException;
import uk.ac.bbk.REx.exception.NameTooShortException;

import java.sql.SQLException;
import java.util.*;

import static org.junit.Assert.assertEquals;

public class CHEBIDBTest
{
    CHEBIDB db;

    @Before
    public void initialise() throws CHEBIException
    {
        db = new CHEBIDB();
    }

    @Test
    public void getCHEBIIDTest() throws NameNotFoundException, CHEBIException, NameTooShortException, SQLException
    {
        int id = db.getCHEBIID("acetyl-CoA");
        String inchi = db.getInchi(id);

        assertEquals("InChI=1S/C23H38N7O17P3S/c1-12(31)51-7-6-25-14(32)4-5-26-21(35)18(34)23(2,3)9-44-50(41,42)47-49" +
                "(39,40)43-8-13-17(46-48(36,37)38)16(33)22(45-13)30-11-29-15-19(24)27-10-28-20(15)30/" +
                "h10-11,13,16-18,22,33-34H,4-9H2,1-3H3,(H,25,32)(H,26,35)(H,39,40)(H,41,42)(H2,24,27,28)(H2,36,37,38)",
                inchi);
    }

    @Test
    public void getNamesTest() throws CHEBIException
    {
        List<String> names = new ArrayList<String>(db.getNames(57288));
        Collections.sort(names);

        assertEquals("[3'-phosphonatoadenosine 5'-(3-{(3r)-4-[(3-{[2-(acetylsulfanyl)ethyl]amino}-3-oxopropyl)amino]-3-hydroxy-2,2-dimethyl-4-oxobutyl} diphosphate), " +
                "accoa(4-), acetyl-coa, acetyl-coa tetraanion, acetyl-coa(4-), acetyl-coenzyme a(4-)]",
                names.toString());
    }

    @After
    public void finalise() throws CHEBIException
    {
        db.close();
    }
}
