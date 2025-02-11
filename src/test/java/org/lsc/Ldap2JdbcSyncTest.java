package org.lsc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.annotations.LoadSchema;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
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

@ExtendWith({ ApacheDSTestExtension.class })
@CreateDS(name = "DSWithPartitionAndServer", loadedSchemas = {
		@LoadSchema(name = "other", enabled = true) }, partitions = {
				@CreatePartition(name = "lsc-project", suffix = "dc=lsc-project,dc=org", contextEntry = @ContextEntry(entryLdif = "dn: dc=lsc-project,dc=org\n"
						+ "dc: lsc-project\n" + "objectClass: top\n" + "objectClass: domain\n\n"), indexes = {
								@CreateIndex(attribute = "objectClass"), @CreateIndex(attribute = "dc"),
								@CreateIndex(attribute = "ou") }) })
@CreateLdapServer(transports = { @CreateTransport(protocol = "LDAP", port = 33389),
		@CreateTransport(protocol = "LDAPS", port = 33636) })
@ApplyLdifs({
		// Entry # 0
		"dn: cn=Directory Manager,ou=system", "objectClass: person", "objectClass: top", "cn: Directory Manager",
		"description: Directory Manager", "sn: Directory Manager", "userpassword: secret" })
@ApplyLdifFiles({ "lsc-schema.ldif", "lsc-project.ldif" })
public class Ldap2JdbcSyncTest extends AbstractLdapTestUnit {

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

	protected static JndiServices srcJndiServices;
	protected static SqlMapClient dstSqlMapClient;

	@BeforeEach
	public void setup() {
		LscConfiguration.reset();
		LscConfiguration.getInstance();
		assertNotNull(LscConfiguration.getConnection("src-ldap"));
		assertNotNull(LscConfiguration.getConnection("dst-jdbc"));
		reloadConnections();
	}

	private void reloadConnections() {
		srcJndiServices = JndiServices.getInstance((LdapConnectionType) LscConfiguration.getConnection("src-ldap"));
		DatabaseConnectionType pc = (DatabaseConnectionType) LscConfiguration.getConnection("dst-jdbc");
		pc.setUrl("jdbc:hsqldb:file:target/hsqldb/lsc");

		try {
			dstSqlMapClient = DaoConfig.getSqlMapClient(pc);
		} catch (LscServiceConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testSyncLdap2Db() throws Exception {
		String functionName = "testSyncLdap2Db";

		// check ADD
		assertTrue(srcJndiServices.exists(DN_ADD_SRC), functionName + " - srcJndiServices missing");
		assertNotNull(dstSqlMapClient, functionName + " - dstSqlMapClient is null");
		Connection con = null;
		SqlMapSession sqlMapSession = null;
		ResultSet rs = null;
		int rowcount = 0;

		try {
			// Initialize the Database
			sqlMapSession = dstSqlMapClient.openSession();
			sqlMapSession.startTransaction();
			con = sqlMapSession.getCurrentConnection();
			assertNotNull(con, functionName + " - Connection is null");

			Statement stm = con.createStatement();
			String sql = String.format("DROP TABLE %s IF EXISTS; CREATE TABLE %s (%s)", DEST_TABLE, DEST_TABLE,
					DEST_TABLE_DEF);

			rs = stm.executeQuery(sql);
			assertNotNull(rs, functionName + " - ResultSet is null");
			sqlMapSession.commitTransaction();

			// Start the Sync Process for the first time to fill up the database
			launchSyncCleanTask(TASK_NAME, false, true, false);

			// Check existence of row in destination with data
			stm = con.createStatement();
			sql = String.format("Select * FROM %s WHERE ID = '%s'", DEST_TABLE, DN_ADD_EXID);
			rs = stm.executeQuery(sql);
			rowcount = 0;

			while (rs.next()) {
				rowcount++;
				assertEquals(null, rs.getString("MAIL"), "After 1st Sync wrong Mail");
				assertEquals(DN_ADD_DESC, rs.getString("DESCRIPTION"), "After 1st Sync wrong Description");
				assertEquals(DN_ADD_TEL, rs.getString("TELEPHONENUMBER"), "After 1st Sync wrong Telephone");
			}

			assertTrue(rowcount == 1, functionName + " - ResultSet size after insert != 1");

			// Modify data in DB
			// Add Mail
			// Remove Telephonenumber
			// Modify Description
			sqlMapSession.startTransaction();
			stm = con.createStatement();
			sql = String.format(
					"UPDATE %s SET MAIL = '%s', MAIL_LOWER = LCASE('%s'), DESCRIPTION = '%s',"
							+ " TELEPHONENUMBER = null WHERE ID = '%s'",
					DEST_TABLE, DB_MOD_MAIL, DB_MOD_MAIL, DB_MOD_DESC, DN_ADD_EXID);
			LOGGER.debug(sql);
			rowcount = stm.executeUpdate(sql);
			assertTrue(rowcount == 1, functionName + " - update row count != 1");
			sqlMapSession.commitTransaction();

			// Check the result in the DB before the sync
			stm = con.createStatement();
			sql = String.format("Select * FROM %s WHERE ID = '%s'", DEST_TABLE, DN_ADD_EXID);
			rs = stm.executeQuery(sql);
			rowcount = 0;

			while (rs.next()) {
				rowcount++;
				assertEquals(DB_MOD_MAIL, rs.getString("MAIL"), "After update wrong Mail");
				assertEquals(DB_MOD_DESC, rs.getString("DESCRIPTION"), "After update wrong Description");
				assertEquals(null, rs.getString("TELEPHONENUMBER"), "After update wrong Telephone");
			}

			assertTrue(rowcount == 1, functionName + " - ResultSet size after insert != 1");

			// Start the Sync Process for the second time
			launchSyncCleanTask(TASK_NAME, false, true, false);

			// Check the result in the DB after the sync
			stm = con.createStatement();
			sql = String.format("Select * FROM %s WHERE ID = '%s'", DEST_TABLE, DN_ADD_EXID);
			rs = stm.executeQuery(sql);
			rowcount = 0;

			while (rs.next()) {
				rowcount++;
				assertEquals(null, rs.getString("MAIL"), "After 2nd Sync wrong Mail");
				assertEquals(DN_ADD_DESC, rs.getString("DESCRIPTION"), "After 2nd Sync wrong Description");
				assertEquals(DN_ADD_TEL, rs.getString("TELEPHONENUMBER"), "After 2nd Sync wrong Telephone");
			}

			assertTrue(rowcount == 1, functionName + " - ResultSet size after insert and syc != 1");
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

	public static void launchSyncCleanTask(String taskName, boolean doAsync, boolean doSync, boolean doClean)
			throws Exception {
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
		assertTrue(ret, "launchSyncCleanTask failed");
	}
}
