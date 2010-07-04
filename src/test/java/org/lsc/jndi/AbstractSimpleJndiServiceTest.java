/**
 * 
 */
package org.lsc.jndi;

import static org.junit.Assert.*;

import java.util.Properties;

import javax.naming.NamingException;
import javax.naming.directory.SearchResult;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lsc.Configuration;
import org.lsc.LscAttributes;

/**
 * @author Jonathan Clarke &lt;jonathan@phillipoux.net&gt;
 *
 */
public class AbstractSimpleJndiServiceTest {

	private Properties props;
	private AbstractSimpleJndiService testService;
	private LscAttributes pivotAttrs;
	private SearchResult res = null;
	
	@Before
	public void setUp() { 
		// set up an instance of AbstractSimpleJndiService
		
		props = Configuration.getAsProperties(Configuration.LSC_TASKS_PREFIX + ".ldap2ldapTestTask.srcService");

		pivotAttrs = new LscAttributes();
		pivotAttrs.put("cn", "CN0001");
		pivotAttrs.put("sn", "SN0001");		
	}
	
	@After
	public void check() throws NamingException {
		assertNotNull(res);
		assertNotNull(res.getAttributes());
		assertNotNull(res.getAttributes().get("cn"));
		assertNotNull(res.getAttributes().get("sn"));

		assertEquals(1, res.getAttributes().get("sn").size());
		assertEquals(1, res.getAttributes().get("cn").size());

		assertEquals("CN0001", res.getAttributes().get("cn").get());
		assertEquals("SN0001", res.getAttributes().get("sn").get());
	}
	
	/**
	 * Test method for {@link org.lsc.jndi.AbstractSimpleJndiService#get(java.lang.String, org.lsc.LscAttributes)}.
	 * @throws NamingException Error
	 */
	@Test
	public void testGetWithMultiplePivotAttributes() throws NamingException {
		props.put("filterId", "(&(cn={cn})(sn={sn}))");

		testService = new SimpleJndiSrcService(props, "org.lsc.beans.SimpleBean");
		res = testService.get("Random string that shouldn't matter", pivotAttrs);
	}
	
}
