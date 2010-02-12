/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lsc.configuration;

import java.io.FileNotFoundException;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.lsc.configuration.objects.LscConfiguration;
import static org.junit.Assert.*;

/**
 *
 * @author rschermesser
 */
public class ConfigurationLoaderTest extends TestCase {

	private String path = "src/test/resources/test-config-xml/";

	@Before
	@Override
	public void setUp() {
	}

	@After
	@Override
	public void tearDown() {
	}

	private LscConfiguration getFile(String filename) throws FileNotFoundException {
		ConfigurationLoader c = new ConfigurationLoader(path + filename);
		return c.getConfiguration();
	}

	public void testSimpleConfiguration() throws FileNotFoundException {
		LscConfiguration c = getFile("simple-config.xml");
		assertNotNull(c);
		assertEquals(1, c.getConnections().size());
		assertEquals(1, c.getAudits().size());
		assertEquals(1, c.getServices().size());
		assertEquals(1, c.getTasks().size());


	}

}
