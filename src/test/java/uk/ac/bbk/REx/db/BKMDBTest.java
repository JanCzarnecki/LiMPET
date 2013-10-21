package uk.ac.bbk.REx.db;

import org.junit.Before;
import org.junit.Test;
import uk.ac.bbk.REx.db.bkmDB.BKMDB;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class BKMDBTest
{
    BKMDB bkmDB;

    @Before
    public void initialise() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, IOException
    {
        bkmDB = new BKMDB();
    }

    @Test
    public void getReactionsInPathwayTest() throws SQLException
    {
        List<Integer> reactionIDs = bkmDB.getReactionsInPathway("PWY-40");
        assertEquals("[659, 1249]", reactionIDs.toString());
    }
}
