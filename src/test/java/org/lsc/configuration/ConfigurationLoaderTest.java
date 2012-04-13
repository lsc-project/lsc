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

import org.apache.commons.configuration.ConfigurationException;
import org.junit.Test;
import org.lsc.configuration.TaskType.AuditLog;
import org.lsc.exception.LscConfigurationException;

/**
 *
 * @author rschermesser
 */
public class ConfigurationLoaderTest {

//	private String path = "src/test/resources/test-config-xml/";

	private Lsc getFile(String filename) throws FileNotFoundException, LscConfigurationException {
		JaxbXmlConfigurationHelper c = new JaxbXmlConfigurationHelper();
		return c.getConfiguration(filename);
	}

	@Test
	public void testLoadSimpleConfiguration() throws FileNotFoundException, LscConfigurationException {
		Lsc c = getFile(this.getClass().getClassLoader().getResource("test.xml").getPath());
		assertNotNull(c);
		LscConfiguration.loadFromInstance(c);
		assertEquals(3, LscConfiguration.getConnections().size());
		assertEquals(1, LscConfiguration.getAudits().size());
		assertEquals(2, LscConfiguration.getTasks().size());
	}

	@Test
	public void testDumpSimpleConfiguration() throws ConfigurationException, IOException, LscConfigurationException {
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
		} catch (ConfigurationException e) {
			e.printStackTrace();
		} catch (LscConfigurationException e) {
			e.printStackTrace();
		}
	}
}
