/*
 ****************************************************************************
 * Ldap Synchronization Connector provides tools to synchronize
 * electronic identities from a list of data sources including
 * any database with a JDBC connector, another LDAP directory,
 * flat files...
 *
 *                  ==LICENSE NOTICE==
 * 
 * Copyright (c) 2008 - 2011 LSC Project 
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:

 *    * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of the LSC Project nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *                  ==LICENSE NOTICE==
 *
 *               (c) 2008 - 2011 LSC Project
 *         Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 *         Thomas Chemineau &lt;thomas@lsc-project.org&gt;
 *         Jonathan Clarke &lt;jon@lsc-project.org&gt;
 *         Remy-Christophe Schermesser &lt;rcs@lsc-project.org&gt;
 ****************************************************************************
 */
package org.lsc.beans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;

import org.junit.jupiter.api.Test;
import org.lsc.LscModificationType;
import org.lsc.LscModifications;
import org.lsc.Task;
import org.lsc.beans.syncoptions.ISyncOptions;
import org.lsc.configuration.PolicyType;
import org.lsc.exception.LscServiceException;
import org.lsc.jndi.SimpleJndiDstService;
import org.lsc.utils.SetUtils;

/**
 * @author Jonathan Clarke &lt;jonathan@phillipoux.net&gt;
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class BeanComparatorTest {

	Task task = mock(Task.class);
    SimpleJndiDstService dstService = mock(SimpleJndiDstService.class);
	
	/**
	 * Test method for {@link org.lsc.beans.BeanComparator#calculateModificationType(Task, IBean, IBean)}.
	 * @throws CloneNotSupportedException As thrown by {@link org.lsc.beans.BeanComparator#calculateModificationType(Task, IBean, IBean)}.
	 * @throws LscServiceException 
	 */
	@Test
	public void testCalculateModificationType() throws CloneNotSupportedException, LscServiceException {
		ISyncOptions syncOptions = mock(ISyncOptions.class);
		
		when(syncOptions.getDn()).thenReturn("\"destination DN\"");
		when(task.getSyncOptions()).thenReturn(syncOptions);

        IBean srcBean = new SimpleBean();
		IBean dstBean = new SimpleBean();

		// test null and null --> null
		assertNull(BeanComparator.calculateModificationType(task, null, null));

		// test not null and null --> add
		assertEquals(LscModificationType.CREATE_OBJECT, BeanComparator.calculateModificationType(task, srcBean, null));

		// test null and not null --> delete
		assertEquals(LscModificationType.DELETE_OBJECT, BeanComparator.calculateModificationType(task, null, dstBean));

		// test both not null, and syncoptions to make DNs identical --> modify
		dstBean.setMainIdentifier("destination DN");
		assertEquals(LscModificationType.UPDATE_OBJECT, BeanComparator.calculateModificationType(task, srcBean, dstBean));

		when(syncOptions.getDn()).thenReturn(null);
		when(task.getSyncOptions()).thenReturn(syncOptions);

		// test both not null, with different DNs and no DN in syncoptions --> modrdn
		srcBean.setMainIdentifier("source DN");
		dstBean.setMainIdentifier("destination DN");
		assertEquals(LscModificationType.CHANGE_ID, BeanComparator.calculateModificationType(task, srcBean, dstBean));
	}

	/**
	 * This test ensures that a source bean containing fields with only
	 * empty string values is not output as a modification to be applied
	 * to the destination.
	 *
	 * If this is not the case, LDAP operations follow for LDIF like this:
	 * modificationType: add
	 * add: sn
	 * sn:
	 *
	 * With an invalid syntax error.
	 * @throws NamingException As thrown when reading JNDI Attribute values.
	 * @throws CloneNotSupportedException As thrown by {@link org.lsc.beans.BeanComparator#calculateModificationType(Task, IBean, IBean)}.
	 * @throws LscServiceException 
	 */
	@Test
	public void testCalculateModificationsWithEmptyFieldsAdd() throws NamingException, CloneNotSupportedException, LscServiceException {
        ISyncOptions syncOptions = mock(ISyncOptions.class);

		when(syncOptions.getStatus(anyString(), anyString())).thenReturn(PolicyType.FORCE);
		when(syncOptions.getForceValues(anyString(), anyString())).thenReturn(null);
		when(task.getSyncOptions()).thenReturn(syncOptions);
		when(task.getDestinationService()).thenReturn(dstService);
		when(dstService.getWriteDatasetIds()).thenReturn(Arrays.asList(new String[] {"cn", "sn"}));

		IBean srcBean, destBean;

		// test add
		srcBean = new SimpleBean();
		srcBean.setMainIdentifier("something");
		srcBean.setDataset("sn", new HashSet<Object>());
        srcBean.setDataset("cn", new HashSet<Object>(Arrays.asList(new String[] {"real cn"})));
		destBean = null;

		LscModifications lm = BeanComparator.calculateModifications(task, srcBean, destBean);

		assertEquals("something", lm.getMainIdentifier());
		assertEquals(1, lm.getLscAttributeModifications().size());
		assertEquals("cn", lm.getLscAttributeModifications().get(0).getAttributeName());
		assertEquals("real cn", lm.getLscAttributeModifications().get(0).getValues().get(0));
	}

	@Test
	public void testCalculateModificationsWithEmptyFieldsModify() throws NamingException, CloneNotSupportedException, LscServiceException {
        ISyncOptions syncOptions = mock(ISyncOptions.class);

        when(task.getDestinationService()).thenReturn(dstService);
        when(dstService.getWriteDatasetIds()).thenReturn(Arrays.asList(new String[] {"cn", "sn"}));
        when(syncOptions.getStatus(anyString(), anyString())).thenReturn(PolicyType.FORCE);
        when(syncOptions.getForceValues(anyString(), anyString())).thenReturn(null);
        when(task.getSyncOptions()).thenReturn(syncOptions);

		IBean srcBean, destBean;

		// test mod
		srcBean = new SimpleBean();
		srcBean.setMainIdentifier("something");
        srcBean.setDataset("sn", new HashSet<Object>());
        srcBean.setDataset("cn", new HashSet<Object>(Arrays.asList(new String[] {"real cn"})));

		destBean = new SimpleBean();
		destBean.setMainIdentifier("something");
		destBean.setDataset("cn", new HashSet<Object>(Arrays.asList(new String[] {"old cn"})));

		LscModifications lam = BeanComparator.calculateModifications(task, srcBean, destBean);

		assertEquals("something", lam.getMainIdentifier());
		assertEquals(1, lam.getLscAttributeModifications().size());
		assertEquals("cn", lam.getLscAttributeModifications().get(0).getAttributeName());
		assertEquals("real cn", lam.getLscAttributeModifications().get(0).getValues().get(0));
	}

	/**
	 * Test method for {@link org.lsc.beans.BeanComparator#getValuesToSet(Task, String, Set, Set, Map, LscModificationType)}.
	 * @throws LscServiceException 
	 */
	@Test
	public void testGetValuesToSet() throws LscServiceException {
        ISyncOptions syncOptions = mock(ISyncOptions.class);

        when(task.getDestinationService()).thenReturn(dstService);
        when(task.getSyncOptions()).thenReturn(syncOptions);
        when(dstService.getWriteDatasetIds()).thenReturn(Arrays.asList(new String[] {"cn", "sn"}));
        when(syncOptions.getStatus(anyString(), anyString())).thenReturn(PolicyType.KEEP);
        when(syncOptions.getForceValues(anyString(), anyString())).thenReturn(null);
        when(syncOptions.getDefaultValues(anyString(), anyString())).thenReturn(null);
        when(syncOptions.getCreateValues(anyString(), anyString())).thenReturn(null);

		// Set up objects needed to test
		String attrName = "cn";

		Set<Object> srcAttrValues = null;
        Set<Object> dstAttrValues = null;
		Map<String, Object> javaScriptObjects = null;

//		Map<String, STATUS_TYPE> statusMap = null;

		final List<String> jsValues = new ArrayList<String>();
		jsValues.add("\"JavaScript \" + (true ? \"has\" : \"hasn't\") + \" parsed this value (0)\"");
		jsValues.add("\"JavaScript \" + (true ? \"has\" : \"hasn't\") + \" parsed this value (1)\"");

		final List<String> jsCreateValues = new ArrayList<String>();
		jsCreateValues.add("\"Created by JavaScript \" + (true ? \"successfully\" : \"or not\") + \" (0)\"");
		jsCreateValues.add("\"Created by JavaScript \" + (true ? \"successfully\" : \"or not\") + \" (1)\"");

		Set<Object> res = null;


		// First test: no default values, no force values, no source values (empty list)
		// Should return an empty List
		srcAttrValues = new HashSet<Object>();
        dstAttrValues = new HashSet<Object>();
		javaScriptObjects = new HashMap<String, Object>();

		res = BeanComparator.getValuesToSet(task, attrName, srcAttrValues, dstAttrValues, javaScriptObjects, LscModificationType.UPDATE_OBJECT);

		assertNotNull(res);
		assertEquals(0, res.size());


		// First test again: no default values, no force values, no source values (null list)
		// Should return an empty List
		srcAttrValues = null;
		javaScriptObjects = new HashMap<String, Object>();

		res = BeanComparator.getValuesToSet(task, attrName, srcAttrValues, dstAttrValues, javaScriptObjects, LscModificationType.UPDATE_OBJECT);

		assertNotNull(res);
		assertEquals(0, res.size());


		// Second test: just source values.
		// Should return the source values, unchanged
		srcAttrValues = new HashSet<Object>();
		srcAttrValues.add("Megan Fox");
		srcAttrValues.add("Lucy Liu");

        when(task.getDestinationService()).thenReturn(dstService );
        when(dstService.getWriteDatasetIds()).thenReturn(Arrays.asList(new String[] {"cn", "sn"}));
        when(syncOptions.getStatus(anyString(), anyString())).thenReturn(PolicyType.KEEP);
        when(syncOptions.getForceValues(anyString(), anyString())).thenReturn(null);
        when(syncOptions.getDefaultValues(anyString(), anyString())).thenReturn(null);
        when(syncOptions.getCreateValues(anyString(), anyString())).thenReturn(jsCreateValues);
        when(task.getSyncOptions()).thenReturn(syncOptions);

		res = BeanComparator.getValuesToSet(task, attrName, srcAttrValues, dstAttrValues, javaScriptObjects, LscModificationType.UPDATE_OBJECT);

		assertNotNull(res);
		assertEquals(srcAttrValues.size(), res.size());
		assertTrue(SetUtils.doSetsMatch(srcAttrValues, res));


		// Third test: source values to be replaced with force values
		// Should return just the force values
        when(task.getDestinationService()).thenReturn(dstService);
        when(dstService.getWriteDatasetIds()).thenReturn(Arrays.asList(new String[] {"cn", "sn"}));
        when(syncOptions.getStatus(anyString(), anyString())).thenReturn(PolicyType.KEEP);
        when(syncOptions.getForceValues(anyString(), anyString())).thenReturn(jsValues);
        when(syncOptions.getDefaultValues(anyString(), anyString())).thenReturn(null);
        when(syncOptions.getCreateValues(anyString(), anyString())).thenReturn(null);
        when(task.getSyncOptions()).thenReturn(syncOptions);

		res = BeanComparator.getValuesToSet(task, attrName, srcAttrValues, dstAttrValues, javaScriptObjects, LscModificationType.UPDATE_OBJECT);

		assertNotNull(res);
		assertEquals(jsValues.size(), res.size());
		for (int i = 0; i < jsValues.size(); i++) {
			assertTrue(res.contains("JavaScript has parsed this value (" + i + ")"));
		}


		// Fourth test: no source values, no force values, just default values
		// Should return just the default values
		srcAttrValues = new HashSet<Object>();

	    when(task.getDestinationService()).thenReturn(dstService);
	    when(dstService.getWriteDatasetIds()).thenReturn(Arrays.asList(new String[] {"cn", "sn"}));
	    when(syncOptions.getStatus(anyString(), anyString())).thenReturn(PolicyType.KEEP);
	    when(syncOptions.getForceValues(anyString(), anyString())).thenReturn(null);
	    when(syncOptions.getDefaultValues(anyString(), anyString())).thenReturn(jsValues);
	    when(syncOptions.getCreateValues(anyString(), anyString())).thenReturn(null);
	    when(task.getSyncOptions()).thenReturn(syncOptions);

		res = BeanComparator.getValuesToSet(task, attrName, srcAttrValues, dstAttrValues, javaScriptObjects, LscModificationType.UPDATE_OBJECT);

		assertNotNull(res);
		assertEquals(jsValues.size(), res.size());
		for (int i = 0; i < jsValues.size(); i++) {
			assertTrue(res.contains("JavaScript has parsed this value (" + i + ")"));
		}


		// 5th test: source values, and default values, attribute status "Force"
		// Should return just source values
		srcAttrValues = new HashSet<Object>();

		when(task.getDestinationService()).thenReturn(dstService);
		when(dstService.getWriteDatasetIds()).thenReturn(Arrays.asList(new String[] {"cn", "sn"}));
		when(syncOptions.getStatus(anyString(), anyString())).thenReturn(PolicyType.FORCE);
		when(syncOptions.getForceValues(anyString(), anyString())).thenReturn(null);
		when(syncOptions.getDefaultValues(anyString(), anyString())).thenReturn(jsValues);
		when(syncOptions.getCreateValues(anyString(), anyString())).thenReturn(null);
		when(task.getSyncOptions()).thenReturn(syncOptions);

		res = BeanComparator.getValuesToSet(task, attrName, srcAttrValues, dstAttrValues, javaScriptObjects, LscModificationType.UPDATE_OBJECT);

		assertNotNull(res);
		assertEquals(jsValues.size(), res.size());
		for (int i = 0; i < jsValues.size(); i++) {
			assertTrue(res.contains("JavaScript has parsed this value (" + i + ")"));
		}


		// 6th test: source values, and default values, attribute status "Merge"
		// Should return source values AND default values
		srcAttrValues = new HashSet<Object>();
		srcAttrValues.add("Megan Fox");
		srcAttrValues.add("Lucy Liu");

		when(task.getDestinationService()).thenReturn(dstService);
		when(dstService.getWriteDatasetIds()).thenReturn(Arrays.asList(new String[] {"cn", "sn"}));
		when(syncOptions.getStatus(anyString(), anyString())).thenReturn(PolicyType.MERGE);
		when(syncOptions.getForceValues(anyString(), anyString())).thenReturn(null);
		when(syncOptions.getDefaultValues(anyString(), anyString())).thenReturn(jsValues);
		when(syncOptions.getCreateValues(anyString(), anyString())).thenReturn(null);
		when(task.getSyncOptions()).thenReturn(syncOptions);

		res = BeanComparator.getValuesToSet(task, attrName, srcAttrValues, dstAttrValues, javaScriptObjects, LscModificationType.UPDATE_OBJECT);

		assertNotNull(res);
		assertEquals(jsValues.size() + srcAttrValues.size(), res.size());
		Set<Object> expectedValues = new HashSet<Object>(jsValues.size() + srcAttrValues.size());
		for (Object srcAttrValue : srcAttrValues) {
			expectedValues.add(srcAttrValue);
		}
		for (int i = 0; i < jsValues.size(); i++) {
			expectedValues.add("JavaScript has parsed this value (" + i + ")");
		}
		assertTrue(SetUtils.doSetsMatch(res, expectedValues));


		// 7th test: source values, and create values, attribute status "Merge", creating new entry
		// Should return source values AND create values but no default values
		srcAttrValues = new HashSet<Object>();
		srcAttrValues.add("Megan Fox");
		srcAttrValues.add("Lucy Liu");

		when(task.getDestinationService()).thenReturn(dstService);
		when(dstService.getWriteDatasetIds()).thenReturn(Arrays.asList(new String[] {"cn", "sn"}));
		when(syncOptions.getStatus(anyString(), anyString())).thenReturn(PolicyType.MERGE);
		when(syncOptions.getForceValues(anyString(), anyString())).thenReturn(null);
		when(syncOptions.getDefaultValues(anyString(), anyString())).thenReturn(null);
		when(syncOptions.getCreateValues(anyString(), anyString())).thenReturn(jsCreateValues);
		when(task.getSyncOptions()).thenReturn(syncOptions);


		res = BeanComparator.getValuesToSet(task, attrName, srcAttrValues, dstAttrValues, javaScriptObjects, LscModificationType.CREATE_OBJECT);

		assertNotNull(res);
		assertEquals(jsCreateValues.size() + srcAttrValues.size(), res.size());
		expectedValues = new HashSet<Object>(jsCreateValues.size() + srcAttrValues.size());
		for (Object srcAttrValue : srcAttrValues) {
			expectedValues.add(srcAttrValue);
		}
		for (int i = 0; i < jsCreateValues.size(); i++) {
			expectedValues.add("Created by JavaScript successfully (" + i + ")");
		}
		assertTrue(SetUtils.doSetsMatch(res, expectedValues));


		// test that attributes configured for create values only
		// (no force_values, default_values or source values)
		// are ignored if this is not an Add operation
		srcAttrValues = new HashSet<Object>();

		when(task.getDestinationService()).thenReturn(dstService);
		when(dstService.getWriteDatasetIds()).thenReturn(Arrays.asList(new String[] {"cn", "sn"}));
		when(syncOptions.getStatus(anyString(), anyString())).thenReturn(PolicyType.FORCE);
		when(syncOptions.getForceValues(anyString(), anyString())).thenReturn(null);
		when(syncOptions.getDefaultValues(anyString(), anyString())).thenReturn(null);
		when(syncOptions.getCreateValues(anyString(), anyString())).thenReturn(jsCreateValues);
		when(task.getSyncOptions()).thenReturn(syncOptions);

		res = BeanComparator.getValuesToSet(task, attrName, srcAttrValues, dstAttrValues, javaScriptObjects, LscModificationType.UPDATE_OBJECT);

		assertNull(res);

		// Check that neither create nor default values are checked if the destination already contains at least one value with a KEEP policy
		

	    final List<String> jsThrowException = new ArrayList<String>();
	    jsThrowException.add("throw 'We should not reach this point !'");

	    when(task.getDestinationService()).thenReturn(dstService);
	    when(dstService.getWriteDatasetIds()).thenReturn(Arrays.asList(new String[] {"cn", "sn"}));
	    when(syncOptions.getStatus(anyString(), anyString())).thenReturn(PolicyType.KEEP);
	    when(syncOptions.getForceValues(anyString(), anyString())).thenReturn(null);
	    when(syncOptions.getDefaultValues(anyString(), anyString())).thenReturn(jsThrowException);
	    when(syncOptions.getCreateValues(anyString(), anyString())).thenReturn(jsThrowException);
	    when(task.getSyncOptions()).thenReturn(syncOptions);

        dstAttrValues.add("At least a single CN value");

        res = BeanComparator.getValuesToSet(task, attrName, srcAttrValues, dstAttrValues, javaScriptObjects, LscModificationType.UPDATE_OBJECT);

        assertNull(res);
	}

	@Test
	public void testGetValuesToSetWithDelimitersForDefault() throws LscServiceException {
        ISyncOptions syncOptions = mock(ISyncOptions.class);

        when(task.getDestinationService()).thenReturn(dstService);
        when(dstService.getWriteDatasetIds()).thenReturn(Arrays.asList(new String[] {"cn"}));
        when(syncOptions.getStatus(anyString(), anyString())).thenReturn(PolicyType.KEEP);
        when(syncOptions.getForceValues(anyString(), anyString())).thenReturn(null);
        when(syncOptions.getDefaultValues(anyString(), anyString())).thenReturn(Arrays.asList(new String[] {"\"Doe;Smith\""}));
        when(syncOptions.getCreateValues(anyString(), anyString())).thenReturn(null);
        when(syncOptions.getDelimiter(anyString())).thenReturn(";");
        when(task.getSyncOptions()).thenReturn(syncOptions);

		HashSet<Object> srcAttrValues = new HashSet<Object>();
        HashSet<Object> dstAttrValues = new HashSet<Object>();
		HashMap<String, Object> javaScriptObjects = new HashMap<String, Object>();
		Set<Object> res = BeanComparator.getValuesToSet(task, "cn", srcAttrValues, dstAttrValues, javaScriptObjects, LscModificationType.UPDATE_OBJECT);
		assertEquals(2, res.size());
	}

	@Test
	public void testGetValuesToSetWithDelimitersForCreate() throws LscServiceException {
        ISyncOptions syncOptions = mock(ISyncOptions.class);

        when(task.getDestinationService()).thenReturn(dstService);
        when(dstService.getWriteDatasetIds()).thenReturn(Arrays.asList(new String[] {"cn"}));
        when(syncOptions.getStatus(anyString(), anyString())).thenReturn(PolicyType.KEEP);
        when(syncOptions.getForceValues(anyString(), anyString())).thenReturn(null);
        when(syncOptions.getDefaultValues(anyString(), anyString())).thenReturn(null);
        when(syncOptions.getCreateValues(anyString(), anyString())).thenReturn(Arrays.asList(new String[] {"\"Doe;Smith\""}));
        when(syncOptions.getDelimiter(anyString())).thenReturn(";");
        when(task.getSyncOptions()).thenReturn(syncOptions);

		HashSet<Object> srcAttrValues = new HashSet<Object>();
        HashSet<Object> dstAttrValues = new HashSet<Object>();
		HashMap<String, Object> javaScriptObjects = new HashMap<String, Object>();
		Set<Object> res = BeanComparator.getValuesToSet(task, "cn", srcAttrValues, dstAttrValues, javaScriptObjects, LscModificationType.CREATE_OBJECT);
		assertEquals(2, res.size());
	}

	@Test
	public void testGetValuesToSetWithDelimitersForForce() throws LscServiceException {
        ISyncOptions syncOptions = mock(ISyncOptions.class);

        when(task.getDestinationService()).thenReturn(dstService);
        when(dstService.getWriteDatasetIds()).thenReturn(Arrays.asList(new String[] {"cn"}));
        when(syncOptions.getStatus(anyString(), anyString())).thenReturn(PolicyType.FORCE);
        when(syncOptions.getForceValues(anyString(), anyString())).thenReturn(Arrays.asList(new String[] {"\"Doe;Smith\""}));
        when(syncOptions.getDefaultValues(anyString(), anyString())).thenReturn(null);
        when(syncOptions.getCreateValues(anyString(), anyString())).thenReturn(null);
        when(syncOptions.getDelimiter(anyString())).thenReturn(";");
        when(task.getSyncOptions()).thenReturn(syncOptions);

		HashSet<Object> srcAttrValues = new HashSet<Object>();
        HashSet<Object> dstAttrValues = new HashSet<Object>();
		HashMap<String, Object> javaScriptObjects = new HashMap<String, Object>();
		Set<Object> res = BeanComparator.getValuesToSet(task, "cn", srcAttrValues, dstAttrValues, javaScriptObjects, LscModificationType.UPDATE_OBJECT);
		assertEquals(2, res.size());
	}
}
