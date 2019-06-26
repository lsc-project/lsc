package org.lsc;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.lsc.configuration.DatabaseConnectionType;
import org.lsc.configuration.LdapConnectionType;
import org.lsc.configuration.LscConfiguration;
import org.lsc.exception.LscServiceConfigurationException;
import org.lsc.jndi.JndiServices;
import org.lsc.persistence.DaoConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibatis.sqlmap.client.SqlMapClient;
import com.ibatis.sqlmap.client.SqlMapSession;

public class Ldap2JdbcSyncTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(Ldap2JdbcSyncTest.class);

	public final static String TASK_NAME = "ldap2dbTestTask";
	public final static String SOURCE_DN = "ou=ldap2db2TestTask,ou=Test Data,dc=lsc-project,dc=org";
	public final static String DEST_TABLE = "testdata";
	public final static String DEST_TABLE_DEF = "id VARCHAR(36) PRIMARY KEY, MAIL VARCHAR(256), MAIL_LOWER VARCHAR(256), LAST_UPDATE TIMESTAMP, SN VARCHAR(64), CN VARCHAR(128), DESCRIPTION VARCHAR(512), TELEPHONENUMBER VARCHAR(128)";
	public String getTaskName() {
		return TASK_NAME;
	}

	public String getSourceDn() {
		return SOURCE_DN;
	}

	public String DN_ADD_SRC = "cn=CN0001," + getSourceDn(); // DN from LDAP
	public String DN_ADD_EXID = "12345678-1234-1234-1234-123456123456"; // externalId from LDAP
	public String DN_ADD_DESC = "Number one's descriptive text"; // Description from LDAP
	public String DN_ADD_TEL = "+49-89-5293-79"; // Telephone number from LDAP
	public String DB_MOD_MAIL = "Hans.Test@lsc-project.org";
	public String DB_MOD_DESC = "Modified description";

	protected JndiServices srcJndiServices;
	protected SqlMapClient dstSqlMapClient;

	@Before
	public void setup() {
		LscConfiguration.reset();
		LscConfiguration.getInstance();
		Assert.assertNotNull(LscConfiguration.getConnection("src-ldap"));
		Assert.assertNotNull(LscConfiguration.getConnection("dst-jdbc"));
		reloadConnections();
	}

	private void reloadConnections() {
		srcJndiServices = JndiServices.getInstance((LdapConnectionType)LscConfiguration.getConnection("src-ldap"));
		DatabaseConnectionType pc = (DatabaseConnectionType)LscConfiguration.getConnection("dst-jdbc");
		pc.setUrl("jdbc:hsqldb:file:target/hsqldb/lsc");
		try {
			dstSqlMapClient = DaoConfig.getSqlMapClient(pc);
		} catch (LscServiceConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	@Test
	public final void testSyncLdap2Db() throws Exception {
		String functionName = "testSyncLdap2Db";
		// check ADD
		assertTrue(functionName + " - srcJndiServices missing", srcJndiServices.exists(DN_ADD_SRC));
		Assert.assertNotNull(functionName + " - dstSqlMapClient is null", dstSqlMapClient);
		Connection con = null;
		SqlMapSession sqlMapSession = null;
		ResultSet rs = null;
		int rowcount = 0;
		try {
			// Initialize the Database
			
			sqlMapSession = dstSqlMapClient.openSession();
			sqlMapSession.startTransaction();
			con = sqlMapSession.getCurrentConnection();
			Assert.assertNotNull(functionName + " - Connection is null", con);
			
			Statement stm = con.createStatement();
			String sql = String.format("DROP TABLE %s IF EXISTS; CREATE TABLE %s (%s)",DEST_TABLE,DEST_TABLE,DEST_TABLE_DEF);
			rs = stm.executeQuery(sql);
			assertNotNull(functionName + " - ResultSet is null", rs);
			sqlMapSession.commitTransaction();
			
			
			// Start the Sync Process for the first time to fill up the database
			
			launchSyncCleanTask(TASK_NAME, false, true, false);
			
			// Check existence of row in destination with data
			
			stm = con.createStatement();
			sql = String.format("Select * FROM %s WHERE ID = '%s'",DEST_TABLE,DN_ADD_EXID);
			rs = stm.executeQuery(sql);
			rowcount = 0;
			ResultSetMetaData metadata = rs.getMetaData();
			StringBuffer text = new StringBuffer();
			int columnCount = metadata.getColumnCount();
			for (int i = 1; i <= columnCount; i++) {
				if (text.length() > 0) 
					text.append(", ");
				text.append(metadata.getColumnName(i));
			}
			LOGGER.debug(text.toString());
			
			text = new StringBuffer();
			while (rs.next()) {
				rowcount ++;
				for (int i = 1; i <= columnCount; i++) {
					if (i == 2) // Mail
						Assert.assertEquals("After 1st Sync wrong Mail", null,rs.getString(i));
					if (i == 7) // Description
						Assert.assertEquals("After 1st Sync wrong Description", DN_ADD_DESC,rs.getString(i));
					if (i == 8) // Telephone
						Assert.assertEquals("After 1st Sync wrong Telephone", DN_ADD_TEL,rs.getString(i));
					if (text.length() > 0) 
						text.append(", ");
					text.append(rs.getString(i));
				}
				LOGGER.debug(text.toString());
			}
			assertTrue(functionName + " - ResultSet size after insert != 1", rowcount == 1);
			
			// Modify data in DB
			// Add Mail
			// Remove Telephonenumber
			// Modify Description
			
			sqlMapSession.startTransaction();
			stm = con.createStatement();
			sql = String.format("UPDATE %s SET MAIL = '%s', MAIL_LOWER = LCASE('%s'), DESCRIPTION = '%s', TELEPHONENUMBER = null WHERE ID = '%s'",DEST_TABLE, DB_MOD_MAIL, DB_MOD_MAIL, DB_MOD_DESC, DN_ADD_EXID);
			LOGGER.debug(sql);
			rowcount = stm.executeUpdate(sql);
			assertTrue(functionName + " - update row count != 1", rowcount == 1);
			sqlMapSession.commitTransaction();
			
			// Check the result in the DB before the sync
			stm = con.createStatement();
			sql = String.format("Select * FROM %s WHERE ID = '%s'",DEST_TABLE,DN_ADD_EXID);
			rs = stm.executeQuery(sql);
			rowcount = 0;
			metadata = rs.getMetaData();
			text = new StringBuffer();
			columnCount = metadata.getColumnCount();
			for (int i = 1; i <= columnCount; i++) {
				if (text.length() > 0) 
					text.append(", ");
				text.append(metadata.getColumnName(i));
			}
			LOGGER.debug(text.toString());
			
			text = new StringBuffer();
			while (rs.next()) {
				rowcount ++;
				for (int i = 1; i <= columnCount; i++) {
					if (i == 2) // Mail
						Assert.assertEquals("After update wrong Mail", DB_MOD_MAIL,rs.getString(i));
					if (i == 7) // Description
						Assert.assertEquals("After update wrong Description", DB_MOD_DESC,rs.getString(i));
					if (i == 8) // Telephone
						Assert.assertEquals("After update wrong Telephone", null,rs.getString(i));
					if (text.length() > 0) 
						text.append(", ");
					text.append(rs.getString(i));
				}
				LOGGER.debug(text.toString());
			}
			assertTrue(functionName + " - ResultSet size after insert != 1", rowcount == 1);
			
			// Start the Sync Process for the first time to fill up the database
			
			launchSyncCleanTask(TASK_NAME, false, true, false);
			
			// Check the result in the DB after the sync
			stm = con.createStatement();
			sql = String.format("Select * FROM %s WHERE ID = '%s'",DEST_TABLE,DN_ADD_EXID);
			rs = stm.executeQuery(sql);
			rowcount = 0;
			metadata = rs.getMetaData();
			text = new StringBuffer();
			columnCount = metadata.getColumnCount();
			for (int i = 1; i <= columnCount; i++) {
				if (text.length() > 0) 
					text.append(", ");
				text.append(metadata.getColumnName(i));
			}
			LOGGER.debug(text.toString());
			
			text = new StringBuffer();
			while (rs.next()) {
				rowcount ++;
				for (int i = 1; i <= columnCount; i++) {
					if (i == 2) // Mail
						Assert.assertEquals("After 2nd Sync wrong Mail", null,rs.getString(i));
					if (i == 7) // Description
						Assert.assertEquals("After 2nd Sync wrong Description", DN_ADD_DESC,rs.getString(i));
					if (i == 8) // Telephone
						Assert.assertEquals("After 2nd Sync wrong Telephone", DN_ADD_TEL,rs.getString(i));
					if (text.length() > 0) 
						text.append(", ");
					text.append(rs.getString(i));
				}
				LOGGER.debug(text.toString());
			}
			assertTrue(functionName + " - ResultSet size after insert and syc != 1", rowcount == 1);
			
			
			
		} finally {
			try {
				LOGGER.debug("Closing SQL Session");
				sqlMapSession.endTransaction();
				if (con != null)
					con.close();
			} finally {
				sqlMapSession.close();
			}
		}
	}

	public static void launchSyncCleanTask(String taskName, boolean doAsync, boolean doSync,
			boolean doClean) throws Exception {
		// initialize required stuff
		SimpleSynchronize sync = new SimpleSynchronize();
		List<String> asyncType = new ArrayList<String>();
		List<String> syncType = new ArrayList<String>();
		List<String> cleanType = new ArrayList<String>();
		
		
		if (doAsync) {
			asyncType.add(taskName);
		}
		
		if (doSync) {
			syncType.add(taskName);
		}
		
		if (doClean) {
			cleanType.add(taskName);
		}
		
		boolean ret = sync.launch(asyncType, syncType, cleanType);
		assertTrue("launchSyncCleanTask failed", ret);
	}
	
}
