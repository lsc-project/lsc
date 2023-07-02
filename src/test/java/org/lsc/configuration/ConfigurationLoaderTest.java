/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lsc.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import org.junit.Test;
import org.lsc.configuration.TaskType.AuditLog;
import org.lsc.exception.LscConfigurationException;

import com.google.common.collect.ImmutableMap;

/**
 *
 * @author rschermesser
 */
public class ConfigurationLoaderTest {

	private static final String LDAP_CONNECTION_ID_ENV_KEY = "LDAP_CONNECTION_ID";
	private static final String LDAP_CONNECTION_ID_ENV_VALUE = "src-ldap";
	private static final String LDAP_CONNECTION_NAME_ENV_KEY = "LDAP_CONNECTION_NAME";
	private static final String LDAP_CONNECTION_NAME_ENV_VALUE = "src-ldap";
	private static final String LDAP_CONNECTION_HOST_ENV_KEY = "LDAP_CONNECTION_HOST";
	private static final String LDAP_CONNECTION_HOST_ENV_VALUE = "localhost";
	private static final String LDAP_CONNECTION_PORT_ENV_KEY = "LDAP_CONNECTION_PORT";
	private static final String LDAP_CONNECTION_PORT_ENV_VALUE = "3389";
	private static final String LDAP_CONNECTION_PASSWORD_ENV_KEY = "LDAP_CONNECTION_PASSWORD";
	private static final String LDAP_CONNECTION_PASSWORD_ENV_VALUE = "secret";
	
	private static final String LDAP_CONNECTION_USERNAME_PART_1_ENV_KEY = "LDAP_CONNECTION_USERNAME_PART_1";
	private static final String LDAP_CONNECTION_USERNAME_PART_1_ENV_VALUE = "dn=";
	private static final String LDAP_CONNECTION_USERNAME_PART_2_ENV_KEY = "LDAP_CONNECTION_USERNAME_PART_2";
	private static final String LDAP_CONNECTION_USERNAME_PART_2_ENV_VALUE = "Directory Manager";
	private static final ImmutableMap<String, String> ENV_VALUE_BY_KEY = ImmutableMap.<String, String>builder().put(LDAP_CONNECTION_ID_ENV_KEY, LDAP_CONNECTION_ID_ENV_VALUE)
			.put(LDAP_CONNECTION_NAME_ENV_KEY, LDAP_CONNECTION_NAME_ENV_VALUE)
			.put(LDAP_CONNECTION_HOST_ENV_KEY, LDAP_CONNECTION_HOST_ENV_VALUE)
			.put(LDAP_CONNECTION_PORT_ENV_KEY, LDAP_CONNECTION_PORT_ENV_VALUE)
			.put(LDAP_CONNECTION_USERNAME_PART_1_ENV_KEY, LDAP_CONNECTION_USERNAME_PART_1_ENV_VALUE)
			.put(LDAP_CONNECTION_USERNAME_PART_2_ENV_KEY, LDAP_CONNECTION_USERNAME_PART_2_ENV_VALUE)
			.build();
	
	
	private static final String DOT_ENV_KEY = ".";
	private static final String DOT_ENV_VALUE = "se";
	private static final String STAR_ENV_KEY = "*";
	private static final String STAR_ENV_VALUE = "cr";
	private static final String DOT_DOT_ENV_KEY = "..";
	private static final String DOT_DOT_ENV_VALUE = "et";
	
	private Lsc getFile(String filename, Map<String, String> env) throws FileNotFoundException, LscConfigurationException {
		JaxbXmlConfigurationHelper c = new JaxbXmlConfigurationHelper();
		return c.getConfiguration(filename, env);
	}

	@Test
	public void testLoadSimpleConfiguration() throws FileNotFoundException, LscConfigurationException {
		Lsc c = getFile(this.getClass().getClassLoader().getResource("test.xml").getPath(), ImmutableMap.<String, String>of());
		assertNotNull(c);
		LscConfiguration.loadFromInstance(c);
		assertEquals(3, LscConfiguration.getConnections().size());
		assertEquals(1, LscConfiguration.getAudits().size());
		assertEquals(2, LscConfiguration.getTasks().size());
	}

	@Test
	public void testLoadConfigurationWithEnvVariables() throws FileNotFoundException, LscConfigurationException {
		Lsc c = getFile(this.getClass().getClassLoader().getResource("test_with_env_variables.xml").getPath(),	
				ImmutableMap.<String, String>builder()
				.putAll(ENV_VALUE_BY_KEY)
				.put(LDAP_CONNECTION_PASSWORD_ENV_KEY, LDAP_CONNECTION_PASSWORD_ENV_VALUE)
				.build()
				);
		assertNotNull(c);
		LscConfiguration.loadFromInstance(c);

		ConnectionType connectionWithEnvInlined = LscConfiguration.getConnection(LDAP_CONNECTION_NAME_ENV_VALUE);
		assertNotNull(connectionWithEnvInlined);
		assertEquals("ldap://" + LDAP_CONNECTION_HOST_ENV_VALUE + ":" + LDAP_CONNECTION_PORT_ENV_VALUE
				+ "/dc=lsc-project,dc=org", connectionWithEnvInlined.url);
		assertEquals(LDAP_CONNECTION_PASSWORD_ENV_VALUE, connectionWithEnvInlined.password);
		assertEquals(LDAP_CONNECTION_USERNAME_PART_1_ENV_VALUE + LDAP_CONNECTION_USERNAME_PART_2_ENV_VALUE,
				connectionWithEnvInlined.username);

	}
	
	@Test
	public void valueFromEnvVariableShouldBeEscapedWhenLoadingConfigurationWithEnvVariables() throws FileNotFoundException, LscConfigurationException {
		Lsc c = getFile(this.getClass().getClassLoader().getResource("test_with_env_variables.xml").getPath(),
				ImmutableMap.<String, String>builder()
				.putAll(ENV_VALUE_BY_KEY)
				.put(LDAP_CONNECTION_PASSWORD_ENV_KEY, "<>\"&'")
				.build());
		assertNotNull(c);
		LscConfiguration.loadFromInstance(c);

		ConnectionType connectionWithEnvInlined = LscConfiguration.getConnection(LDAP_CONNECTION_NAME_ENV_VALUE);
		assertNotNull(connectionWithEnvInlined);
		
		assertEquals("<>\"&'", connectionWithEnvInlined.password);
	}
	
	@Test
	public void funkyEnvVariableNamesShouldBeEscapedDuringTestLoadConfiguration() throws FileNotFoundException, LscConfigurationException {

		
		Lsc c = getFile(this.getClass().getClassLoader().getResource("test_with_funky_env_variables.xml").getPath(),
				ImmutableMap.<String, String>of(
						DOT_ENV_KEY, DOT_ENV_VALUE,
						STAR_ENV_KEY, STAR_ENV_VALUE,
						DOT_DOT_ENV_KEY, DOT_DOT_ENV_VALUE
				));
		assertNotNull(c);
		LscConfiguration.loadFromInstance(c);
		
		ConnectionType connectionWithEnvInlined = LscConfiguration.getConnection("src-ldap");
		assertNotNull(connectionWithEnvInlined);
		assertEquals("se_cr_et", connectionWithEnvInlined.password);
		
	}
		
	@Test
	public void testDumpSimpleConfiguration() throws IOException, LscConfigurationException {
		Lsc c = getFile(this.getClass().getClassLoader().getResource("test.xml").getPath(), ImmutableMap.<String, String>of());
		LscConfiguration.loadFromInstance(c);

		CsvAuditType csvAudit = new CsvAuditType();
		csvAudit.setId("csvAudit-1");
		csvAudit.setAppend(false);
		csvAudit.setDatasets("cn, sn, givenName");
		csvAudit.setName("csvAudit-1");
		csvAudit.setSeparator(";");
		LscConfiguration.addAudit(csvAudit);
		AuditLog auditLog = new AuditLog();
		auditLog.setReference(csvAudit);
		LscConfiguration.getTasks().iterator().next().getAuditLog().add(auditLog);
		new JaxbXmlConfigurationHelper().saveConfiguration(new File(this.getClass().getClassLoader().getResource("etc").getFile(),"test-dump.xml").toString(), LscConfiguration.getInstance().getLsc());
	}

	public static void main(String[] args) {
		try {
			new ConfigurationLoaderTest().testDumpSimpleConfiguration();
			new ConfigurationLoaderTest().testLoadSimpleConfiguration();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (LscConfigurationException e) {
			e.printStackTrace();
		}
	}
}
