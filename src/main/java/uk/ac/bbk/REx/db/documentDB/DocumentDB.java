package uk.ac.bbk.REx.db.documentDB;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DocumentDB 
{
	Connection con;
	PreparedStatement countStmt;
	PreparedStatement getStmt;
	PreparedStatement putStmt;
	
	public DocumentDB() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
	{
		Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
		con = DriverManager.getConnection("jdbc:derby:data/db/document;create=true");
		
		countStmt = con.prepareStatement
		(
			"SELECT COUNT(*) AS count " +
			"FROM documents " +
			"WHERE pmid=?"
		);
		
		getStmt = con.prepareStatement
		(
			"SELECT * " +
			"FROM documents " +
			"WHERE pmid=?"
		);
		
		putStmt = con.prepareStatement
		(
			"INSERT INTO documents (pmid, text) VALUES (?,?)"
		);
	}
	
	public void createDatabase() throws SQLException
	{
		con.prepareStatement(
				"CREATE TABLE documents" +
				"(" +
				"pmid VARCHAR(20) NOT NULL," +
				"text CLOB(1M) NOT NULL," +
				"PRIMARY KEY (pmid)" +
				")").execute();
		con.prepareStatement("CREATE INDEX pmid ON documents(pmid)").execute();
	}
	
	public void clearDatabase() throws SQLException
	{
		con.prepareStatement("DROP TABLE documents").executeUpdate();
		createDatabase();
	}
	
	public boolean contains(String pmid) throws IOException
	{
		try
		{
			countStmt.setString(1, pmid);
			ResultSet rs = countStmt.executeQuery();
			rs.next();
			int count = rs.getInt("count");
			rs.close();
			if(count > 0)
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		catch(SQLException e)
		{
			throw new IOException(e);
		}
	}
	
	public String get(String pmid) throws IOException
	{
		try
		{
			getStmt.setString(1, pmid);
			ResultSet rs = getStmt.executeQuery();
			rs.next();
			String text = rs.getString("text");
			rs.close();
			return text;
		}
		catch(SQLException e)
		{
			throw new IOException(e);
		}
	}
	
	public void put(String pmid, String text) throws IOException
	{
		try
		{
			putStmt.setString(1, pmid);
			putStmt.setString(2, text);
			putStmt.executeUpdate();
		}
		catch(SQLException e)
		{
			throw new IOException(e);
		}
	}

    public void close() throws SQLException
    {
        con.close();
    }
}
