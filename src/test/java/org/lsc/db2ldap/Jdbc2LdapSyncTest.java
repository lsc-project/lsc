package org.lsc.db2ldap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.directory.api.ldap.model.name.Rdn;
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
import org.lsc.SimpleSynchronize;
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
/**
 * The DB -> LDAP test class
 */
public class Jdbc2LdapSyncTest extends AbstractLdapTestUnit {
	private static final Logger LOGGER = LoggerFactory.getLogger(Jdbc2LdapSyncTest.class);

	public final static String SYNC_TASK_NAME = "db2ldapSyncTestTask";
	public final static String CLEAN_TASK_NAME = "db2ldapCleanTestTask";
	public final static String DEST_DN = "ou=db2ldapTestTask,ou=Test Data,dc=lsc-project,dc=org";
	public final static String SRC_TABLE = "inetorgperson";
	public final static String DEST_TABLE_DEF = "id VARCHAR(36) PRIMARY KEY, MAIL VARCHAR(256), MAIL_LOWER VARCHAR(256), LAST_UPDATE TIMESTAMP, SN VARCHAR(64), CN VARCHAR(128), DESCRIPTION VARCHAR(512), TELEPHONENUMBER VARCHAR(128)";

	public String getTaskName() {
		return SYNC_TASK_NAME;
	}

	public String getDestDn() {
		return DEST_DN;
	}

	public String DN_ADD_DST = "cn=CN0001," + getDestDn(); // DN from LDAP
	public String DN_ADD_EXID = "12345678-1234-1234-1234-123456123456"; // externalId from LDAP
	public String DN_ADD_DESC = "Number one's descriptive text"; // Description from LDAP
	public String DN_ADD_TEL = "+49-89-5293-79"; // Telephone number from LDAP
	public String DB_MOD_MAIL = "Hans.Test@lsc-project.org";
	public String DB_MOD_DESC = "Modified description";

	protected static SqlMapClient srcSqlMapClient;
	protected static JndiServices dstJndiServices;
	protected static DatabaseConnectionType databaseConnectionType;
	private Connection dbConnection;

	private static final int ID_ROW = 1;
	private static final int UID_ROW = 2;
	private static final int DATE_ROW = 3;
	private static final int SN_ROW = 4;
	private static final int CN_ROW = 5;
	private static final int GN_ROW = 6;
	private static final int MAIL_ROW = 7;
	private static final int O_ROW = 8;
	private static final int ADDRESS_ROW = 9;
	private static final int TELEPHONE_ROW = 10;
	private static final int CAR_ROW = 11;
	private static final int PASSWD_ROW = 12;


	private String[][] data = {
			{"1", "j.clarke", "31/12/2015", "Clarke", "Clarke, Jonathan", "Jonathan", "jonathan@philipou.net", "Normation", "", "+33 (0)1 83 62 26 96", "BHU772|DED899", "aaa"},
			{"2", "r.schermesser", "31/12/2015", "Schermesser", "Schermesser, Remy-Christophe", "Remy-Christophe", "remy@schermesser.com", "Octo", "", "", "", "bbb"},
			{"3", "t.chemineau", "31/12/2015", "Chemineau", "Chemineau, Thomas", "Thomas", "thomas@aepik.net", "AFNOR", "", "", "", "ccc"},
			{"4", "s.bahloul", "31/12/2015", "Bahloul", "Bahloul, Sebastien", "Sebastien", "sebastien.bahloul@gmail.com", "Dictao", "156 av. de Malakof, 75116 PARIS, France", "", "", "ddd"},
			{"5", "c.oudot", "31/12/2015", "Oudot", "Oudot, Clement", "Clement", "clem.oudot@gmail.com", "Linagora", "", "33(0)810251251", "", "eee"},
			{"6", "r.ouazana", "31/12/2015", "Ouazana", "Ouazana, Raphael", "Raphael", "rouazana@linagora.com", "Linagora", "", "33(0)810251251", "", "fff"},
			{"7", "d.coutadeur", "31/12/2015", "Coutadeur", "Coutadeur, David", "David", "dcoutadeur@linagora.com", "Linagora", "", "33(0)810251251", "", "ggg"},
			{"8", "e.pereira", "31/12/2015", "Pereira", "Pereira, Esteban", "Esteban", "epereira@linagora.com", "Linagora", "", "33(0)810251251", "", "hhh"},
			{"9", "e.lecharny", "31/12/2015", "Pereira", "Lecharny, Emmanuel", "Emmanuel", "epereira@linagora.com", "Worteks", "", "33(0)810251251", "", "iii"}
	};


	@BeforeEach
	public void setup() {
		LscConfiguration.reset();
		LscConfiguration.getInstance();
		assertNotNull(LscConfiguration.getConnection("dst-ldap"));
		assertNotNull(LscConfiguration.getConnection("src-jdbc"));
		reloadConnections();
		loadDbData();
	}


	private void deleteFromDb() throws SQLException {
		try (Statement statempent = dbConnection.createStatement()) {
			statempent.executeUpdate("DELETE FROM " + SRC_TABLE);
			dbConnection.commit();
		} catch ( SQLException s) {
			// That's ok
		}


		try (Statement statement = dbConnection.createStatement()) {
			String request = String.format("Select * FROM %s ", SRC_TABLE);

			try (ResultSet resultSet = statement.executeQuery(request)) {
				int rowcount = 0;

				while (resultSet.next()) {
					rowcount++;

					System.out.println("Entry :"+resultSet.getString("cn"));
				}

				if (rowcount == 0) {
					System.out.println("----> No entry in the database");
				}
			}
		}
	}

	private void loadDbData() {
		try {
			DatabaseConnectionType pc = (DatabaseConnectionType) LscConfiguration.getConnection("src-jdbc");
			pc.setUrl("jdbc:hsqldb:file:target/hsqldb/lsc");

			try {
				Class.forName(pc.getDriver()).newInstance();
				dbConnection = DriverManager.getConnection(pc.getUrl());
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | SQLException e) {
				// error
			}

			try (Statement statempent = dbConnection.createStatement()) {

				try {
					statempent.executeUpdate("DROP TABLE " + SRC_TABLE);
				} catch ( SQLException s) {
					// That's ok
				}

				// Create the table
				statempent.executeUpdate("CREATE TABLE " + SRC_TABLE + "(" +
						"id BIGINT," +
						"uid VARCHAR(20)," +
						"endOfValidity DATE, " +
						"sn VARCHAR(100)," +
						"cn VARCHAR(100)," +
						"givenName VARCHAR(100)," +
						"mail VARCHAR(100), " +
						"o VARCHAR(100)," +
						"address VARCHAR(100)," +
						"telephoneNumber VARCHAR(100)," +
						"carLicense VARCHAR(100)," +
						"userpassword VARCHAR(20))");

				dbConnection.commit();
			}

			// Inject the data

			try (PreparedStatement pstmt = dbConnection.prepareStatement("INSERT INTO " + SRC_TABLE + " VALUES (?,?,?,?,?,?,?,?,?,?,?, ?)")) {

				for (String[] values:data) {
					int colNb = 1;

					for (String value:values) {
						switch (colNb) {

							case ID_ROW:
								pstmt.setLong(ID_ROW, Long.valueOf(value));
								break;

							case DATE_ROW:
								SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");

								try {
									Date date = new Date(df.parse(value).getTime());
									pstmt.setDate(DATE_ROW, date);
								} catch (ParseException pe) {
									//
								}

								break;

							case UID_ROW:
							case SN_ROW:
							case CN_ROW:
							case GN_ROW:
							case MAIL_ROW:
							case O_ROW:
							case ADDRESS_ROW:
							case TELEPHONE_ROW:
							case CAR_ROW:
							case PASSWD_ROW:
								pstmt.setString(colNb, value);
						}

						colNb++;
					}

					pstmt.execute();
				}
			}
		} catch (SQLException s) {
				s.printStackTrace();
		}
	}

	private void reloadConnections() {
		dstJndiServices = JndiServices.getInstance((LdapConnectionType) LscConfiguration.getConnection("dst-ldap"));
		databaseConnectionType = (DatabaseConnectionType) LscConfiguration.getConnection("src-jdbc");
		databaseConnectionType.setUrl("jdbc:hsqldb:file:target/hsqldb/lsc");

		try {
			srcSqlMapClient = DaoConfig.getSqlMapClient(databaseConnectionType);
		} catch (LscServiceConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testSyncDb2ldap() throws Exception {
		String functionName = "testSyncLdap2Db";

		// check ADD
		assertTrue(dstJndiServices.exists(DN_ADD_DST), functionName + " - srcJndiServices missing");
		assertNotNull(srcSqlMapClient, functionName + " - dstSqlMapClient is null");
		Connection con = null;
		SqlMapSession sqlMapSession = null;

		try {
			// Initialize the Database
			sqlMapSession = srcSqlMapClient.openSession();
			sqlMapSession.startTransaction();
			con = sqlMapSession.getCurrentConnection();
			assertNotNull(con, functionName + " - Connection is null");

			// Start the Sync Process for the first time to fill up the LddapServer
			launchSyncTask(SYNC_TASK_NAME);

			// Check the result. We should have 2 entries that aren't updated
			dstJndiServices = JndiServices.getInstance((LdapConnectionType) LscConfiguration.getConnection("dst-ldap"));
			int nbRows = 0;
			int nbExpectedfailures = 2;

			for (String[] values:data) {
				String sn = values[SN_ROW - 1];
				String gn = values[GN_ROW - 1];

				// The cn is a composition of the DB givename + commonname
				String dn = String.format("cn=%s %s,ou=db2ldap2TestTaskDst,ou=Test Data,dc=lsc-project,dc=org",
						Rdn.escapeValue(gn), Rdn.escapeValue(sn)); 

				if (dstJndiServices.exists(dn)) {
					nbRows++;
				}
			}

			assertEquals(nbRows, data.length - nbExpectedfailures);

			// Now try a clean phase
			deleteFromDb();
			launchCleanTask(SYNC_TASK_NAME);

			// We should have nothing in LDAP now
			// Check the result
			dstJndiServices = JndiServices.getInstance((LdapConnectionType) LscConfiguration.getConnection("dst-ldap"));
			nbRows = 0;

			for (String[] values:data) {
				String sn = values[SN_ROW - 1];
				String gn = values[GN_ROW - 1];

				// The cn is a composition of the DB givename + commonname
				String dn = String.format("cn=%s %s,ou=db2ldap2TestTaskDst,ou=Test Data,dc=lsc-project,dc=org",
						Rdn.escapeValue(gn), Rdn.escapeValue(sn)); 
				assertFalse(dstJndiServices.exists(dn));
			}
		} finally {
			try {
				LOGGER.debug("Closing SQL Session");
				sqlMapSession.endTransaction();

				if (con != null) {
					con.close();
				}
			} finally {
				sqlMapSession.close();
			}
		}
	}

	public static void launchSyncTask(String taskName) throws Exception {
		// initialize required stuff
		SimpleSynchronize synchronize = new SimpleSynchronize();
		List<String> asyncType = new ArrayList<String>();
		List<String> syncType = new ArrayList<String>();
		List<String> cleanType = new ArrayList<String>();

		syncType.add(taskName);

		boolean ret = synchronize.launch(asyncType, syncType, cleanType);
		assertTrue(ret, "launchSyncTask failed");
	}


	public static void launchCleanTask(String taskName) throws Exception {
		// initialize required stuff
		SimpleSynchronize synchronize = new SimpleSynchronize();
		List<String> asyncType = new ArrayList<String>();
		List<String> syncType = new ArrayList<String>();
		List<String> cleanType = new ArrayList<String>();

		cleanType.add(taskName);

		boolean ret = synchronize.launch(asyncType, syncType, cleanType);
		assertTrue(ret, "launchCleanTask failed");
	}
}
