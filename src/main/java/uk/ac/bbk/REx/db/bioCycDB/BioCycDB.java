package uk.ac.bbk.REx.db.bioCycDB;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class BioCycDB
{
    private final Logger LOGGER = Logger.getLogger(this.getClass().getName());

    private Connection con;
    private PreparedStatement getPathwayIDsStmt;
    private PreparedStatement getPathwayIDStmt;
    private PreparedStatement getPathwayNameStmt;
    private PreparedStatement getReactionIDsStmt;
    private PreparedStatement getSubstrateNamesStmt;
    private PreparedStatement getProductNamesStmt;

    public BioCycDB(String username, String password) throws ClassNotFoundException, SQLException, IOException
    {
        LOGGER.info("Connecting to Biowarehouse.");

        Class.forName("com.mysql.jdbc.Driver");
        String urlTemplate = "jdbc:mysql://publichouse.ai.sri.com:3306/biodemo1?user=%s&password=%s";
        String url = String.format(urlTemplate, username, password);
        con = DriverManager.getConnection(url);

        LOGGER.info("Connected to Biowarehouse.");

        getPathwayIDsStmt = con.prepareStatement(
                IOUtils.toString(BioCycDB.class.getResourceAsStream("getPathwayIDsStmt.sql")));
        getPathwayIDStmt = con.prepareStatement(
                IOUtils.toString(BioCycDB.class.getResourceAsStream("getPathwayIDStmt.sql")));
        getPathwayNameStmt = con.prepareStatement(
                IOUtils.toString(BioCycDB.class.getResourceAsStream("getPathwayNameStmt.sql")));
        getReactionIDsStmt = con.prepareStatement(
                IOUtils.toString(BioCycDB.class.getResourceAsStream("getReactionIDsStmt.sql")));
        getSubstrateNamesStmt = con.prepareStatement(
                IOUtils.toString(BioCycDB.class.getResourceAsStream("getSubstrateNamesStmt.sql")));
        getProductNamesStmt = con.prepareStatement(
                IOUtils.toString(BioCycDB.class.getResourceAsStream("getProductNamesStmt.sql")));
    }

    public List<String> getPathwayIDs(String dataSet, String pathway) throws SQLException
    {
        pathway = pathway + "%";

        getPathwayIDsStmt.setString(1, dataSet);
        getPathwayIDsStmt.setString(2, pathway);

        return getListOfStrings(getPathwayIDsStmt, "WID");
    }

    public String getPathwayID(String dataSet, String pathway) throws SQLException
    {
        getPathwayIDStmt.setString(1, dataSet);
        getPathwayIDStmt.setString(2, pathway);

        ResultSet rs = getPathwayIDStmt.executeQuery();
        rs.first();
        return rs.getString("WID");
    }

    public String getPathwayName(String dataSet, String pathwayID) throws SQLException
    {
        getPathwayNameStmt.setString(1, dataSet);
        getPathwayNameStmt.setString(2, pathwayID);

        ResultSet rs = getPathwayNameStmt.executeQuery();
        rs.first();
        return rs.getString("Name");
    }

    public List<String> getReactionIDs(String pathwayID) throws SQLException
    {
        getReactionIDsStmt.setString(1, pathwayID);
        return getListOfStrings(getReactionIDsStmt, "ReactionWID");
    }

    public List<String> getSubstrateNames(String reactionID) throws SQLException
    {
        getSubstrateNamesStmt.setString(1, reactionID);
        return getListOfStrings(getSubstrateNamesStmt, "Name");
    }

    public List<String> getProductNames(String reactionID) throws SQLException
    {
        getProductNamesStmt.setString(1, reactionID);
        return getListOfStrings(getProductNamesStmt, "Name");
    }

    private List<String> getListOfStrings(PreparedStatement stmt, String col) throws SQLException
    {
        List<String> strings = new ArrayList<String>();
        ResultSet rs = stmt.executeQuery();
        while(rs.next())
        {
            strings.add(rs.getString(col));
        }

        return strings;
    }

    public void close() throws SQLException
    {
        con.close();
    }
}
