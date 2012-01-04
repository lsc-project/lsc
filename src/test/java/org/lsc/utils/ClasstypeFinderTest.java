package org.lsc.utils;

import org.junit.Assert;
import org.junit.Test;
import org.lsc.service.IService;

public class ClasstypeFinderTest {

	@Test
	public void testOk() {
		ClasstypeFinder.getInstance().setup();
		Assert.assertNotNull(ClasstypeFinder.getInstance().findEquivalence("SimpleJdbcDstService", IService.class));
	}
	
}
