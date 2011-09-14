package org.lsc.utils;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;
import org.lsc.service.IService;
import org.lsc.utils.ClasstypeFinder;

public class ClasstypeFinderTest {

	@Test
	public void testOk() {
		File[] cp = new File[] {
				new File(this.getClass().getClassLoader().getResource(".").getFile())
		};
		ClasstypeFinder.getInstance().loadClasspath(cp);
		Assert.assertNotNull(ClasstypeFinder.getInstance().findEquivalence("SimpleJdbcDstService", IService.class));
	}
	
}
