package org.lsc.utils;

import java.io.File;

import org.lsc.utils.ClasstypeFinder;
import org.lsc.webai.base.EditSettings;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ClasstypeFinderTest {

	@Test
	public void testOk() {
		File[] cp = new File[] {
				new File(this.getClass().getClassLoader().getResource(".").getFile())
		};
		ClasstypeFinder.getInstance().loadClasspath(cp);
		Assert.assertNotNull(ClasstypeFinder.getInstance().findEquivalence("EditAudit", EditSettings.class));
	}
	
}
