/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lsc.configuration;

import java.io.FileNotFoundException;
import org.lsc.configuration.objects.LscConfiguration;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author rschermesser
 */
public class ConfigurationLoaderTest {

	private String path = "src/test/resources/test-config-xml/";

	private LscConfiguration getFile(String filename) throws FileNotFoundException {
		ConfigurationLoader c = new ConfigurationLoader(path + filename);
		return c.getConfiguration();
	}

	@Test
	public void testSimpleConfiguration() throws FileNotFoundException {
		LscConfiguration c = getFile("simple-config.xml");
		assertNotNull(c);
		assertEquals(1, c.getConnections().size());
		assertEquals(1, c.getAudits().size());
		assertEquals(1, c.getServices().size());
		assertEquals(1, c.getTasks().size());
	}

}
