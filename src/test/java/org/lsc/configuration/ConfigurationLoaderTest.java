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
import org.lsc.configuration.objects.LscConfiguration;
import org.lsc.configuration.objects.audit.Csv;

/**
 *
 * @author rschermesser
 */
public class ConfigurationLoaderTest {

//	private String path = "src/test/resources/test-config-xml/";

	private LscConfiguration getFile(String filename) throws FileNotFoundException {
		XmlConfigurationHelper c = new XmlConfigurationHelper();
		return c.getConfiguration(filename);
	}

	@Test
	public void testLoadSimpleConfiguration() throws FileNotFoundException {
		LscConfiguration c = getFile(this.getClass().getClassLoader().getResource("test.xml").getPath());
		assertNotNull(c);
		LscConfiguration.loadFromInstance(c);
		assertEquals(3, LscConfiguration.getConnections().size());
		assertEquals(1, LscConfiguration.getAudits().size());
		assertEquals(2, LscConfiguration.getTasks().size());
	}

	@Test
	public void testDumpSimpleConfiguration() throws ConfigurationException, IOException {
		Csv csvAudit = new Csv();
		csvAudit.setAppend(false);
		csvAudit.setAttributes("cn, sn, givenName");
		csvAudit.setName("csvAudit");
		csvAudit.setSeparator(";");
		LscConfiguration.addAudit(csvAudit);
		LscConfiguration.getTasks().iterator().next().addAudit(csvAudit);
		new XmlConfigurationHelper().saveConfiguration(new File(this.getClass().getClassLoader().getResource("etc").getFile(),"test-dump.xml").toString(), LscConfiguration.getInstance());
	}

	public static void main(String[] args) {
		try {
			new ConfigurationLoaderTest().testDumpSimpleConfiguration();
			new ConfigurationLoaderTest().testLoadSimpleConfiguration();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
