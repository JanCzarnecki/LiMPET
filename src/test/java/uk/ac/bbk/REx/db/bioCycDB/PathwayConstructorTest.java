package uk.ac.bbk.REx.db.bioCycDB;

import org.junit.Before;
import org.junit.Test;
import uk.ac.bbk.REx.exception.BioCycException;
import uk.ac.ebi.mdk.domain.entity.reaction.MetabolicReaction;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class PathwayConstructorTest
{
    private PathwayConstructor pc;

    @Before
    public void initialise() throws BioCycException {
        pc = new PathwayConstructor();
    }

    @Test
    public void constructPathwayTest() throws BioCycException {
        List<MetabolicReaction> reactions = pc.constructPathway("META", "PWY-5694");
        assertEquals("[{\"SUBSTRATE\":[\"InChI\\u003d1S/C4H8N4O4/c5-3(11)7-1(2(9)10)8-4(6)12/h1H,(H,9,10)(H3,5,7,11)(H3,6,8,12)/p-1\",\"InChI\\u003d1S/H2O/h1H2\"],\"PRODUCT\":[\"InChI\\u003d1S/C3H6N2O4/c4-3(9)5-1(6)2(7)8/h1,6H,(H,7,8)(H3,4,5,9)/p-1/t1-/m0/s1\",\"InChI\\u003d1S/CH4N2O/c2-1(3)4/h(H4,2,3,4)\"]},{\"SUBSTRATE\":[\"InChI\\u003d1S/C4H6N4O3/c5-3(10)6-1-2(9)8-4(11)7-1/h1H,(H3,5,6,10)(H2,7,8,9,11)/t1-/m0/s1\",\"InChI\\u003d1S/H2O/h1H2\"],\"PRODUCT\":[\"InChI\\u003d1S/C4H8N4O4/c5-3(11)7-1(2(9)10)8-4(6)12/h1H,(H,9,10)(H3,5,7,11)(H3,6,8,12)/p-1\",\"InChI\\u003d1S/p+1\"]},{\"SUBSTRATE\":[\"InChI\\u003d1S/C3H6N2O4/c4-3(9)5-1(6)2(7)8/h1,6H,(H,7,8)(H3,4,5,9)/p-1/t1-/m0/s1\"],\"PRODUCT\":[\"InChI\\u003d1S/C2H2O3/c3-1-2(4)5/h1H,(H,4,5)/p-1\",\"InChI\\u003d1S/CH4N2O/c2-1(3)4/h(H4,2,3,4)\"]}]",
                pc.reactionsToString(reactions));
    }
}
