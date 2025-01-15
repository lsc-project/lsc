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
 *               (c) 2008 - 2024 LSC Project
 ****************************************************************************
 */
package org.lsc;

import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.lsc.configuration.LscConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;

/**
 * This test case attempts to reproduce a ldap2ldap setup via SimpleSynchronize.
 * It attempts to launch the tasks defined in src/test/resources/etc/lsc.xml:
 * ldap2ldapOptimizeValueReplacementTestCreate ldap2ldapOptimizeValueReplacementTestUpdate1 ldap2ldapOptimizeValueReplacementTestUpdate2
 */
public class Ldap2LdapOptimizeValueReplacementTest extends CommonLdapSyncTest {
	@BeforeEach
	public void setup() {
		LscConfiguration.reset();
		LscConfiguration.getInstance();
		assertNotNull(LscConfiguration.getConnection("src-ldap"));
		assertNotNull(LscConfiguration.getConnection("dst-ldap"));
		reloadJndiConnections();
	}

	@Test
	public final void testLdap2LdapOptimizeValueReplacementTest() throws Exception {

		String createOutput  = launchSyncTask("ldap2ldapOptimizeValueReplacementTestCreate");
		String update1Output = launchSyncTask("ldap2ldapOptimizeValueReplacementTestUpdate1");
		String update2Output = launchSyncTask("ldap2ldapOptimizeValueReplacementTestUpdate2");

		// check the results of the synchronization
		reloadJndiConnections();

		// check that the entry has been created with all 100 values for description
		assertTrue(createOutput.contains(
		    "In object \"cn=optimizevaluereplacement,ou=ldap2ldap2TestTaskDst,ou=Test Data,dc=lsc-project,dc=org\""
		    + ":  Adding attribute \"description\" with values [value001, value002, value003, value004, value005, "
		    + "value006, value007, value008, value009, value010, value011, value012, value013, value014, value015, "
		    + "value016, value017, value018, value019, value020, value021, value022, value023, value024, value025, "
		    + "value026, value027, value028, value029, value030, value031, value032, value033, value034, value035, "
		    + "value036, value037, value038, value039, value040, value041, value042, value043, value044, value045, "
		    + "value046, value047, value048, value049, value050, value051, value052, value053, value054, value055, "
		    + "value056, value057, value058, value059, value060, value061, value062, value063, value064, value065, "
		    + "value066, value067, value068, value069, value070, value071, value072, value073, value074, value075, "
		    + "value076, value077, value078, value079, value080, value081, value082, value083, value084, value085, "
		    + "value086, value087, value088, value089, value090, value091, value092, value093, value094, value095, "
		    + "value096, value097, value098, value099, value100]"));

		// In first update task: check that the replacement 
		// has been splitted in 2 operations: 1 add + 1 delete
		assertTrue(update1Output.contains("Adding values to attribute \"description\": new values are [value101]"));
		assertTrue(update1Output.contains("Removing values from attribute \"description\": old values are [value100]"));

		// In second update task: check that the replacement 
		// has been done in one unique replace operation
		assertTrue(update2Output.contains(
		    "Replacing attribute \"description\": source values are [value001, value002, value003, value004, "
		    + "value005, value006, value007, value008, value009, value010, value011, value012, value013, "
		    + "value014, value015, value016, value017, value018, value019, value020, value021, value022, "
		    + "value023, value024, value025, value026, value027, value028, value029, value030, value031, "
		    + "value032, value033, value034, value035, value036, value037, value038, value039, value040, "
		    + "value041, value042, value043, value044, value045, value046, value047, value048, value049, "
		    + "value050, value051, value052, value053, value054, value055, value056, value057, value058, "
		    + "value059, value060, value061, value062, value063, value064, value065, value066, value067, "
		    + "value068, value069, value070, value071, value072, value073, value074, value075, value076, "
		    + "value077, value078, value079, value080, value081, value082, value083, value084, value085, "
		    + "value086, value087, value088, value089, value090, value091, value092, value093, value094, "
		    + "value095, value096, value097, value098, value099, value101], "
		    + "old values were [value001, value002, value003, value004, value005, value006, value007, "
		    + "value008, value009, value010, value011, value012, value013, value014, value015, value016, "
		    + "value017, value018, value019, value020, value021, value022, value023, value024, value025, "
		    + "value026, value027, value028, value029, value030, value031, value032, value033, value034, "
		    + "value035, value036, value037, value038, value039, value040, value041, value042, value043, "
		    + "value044, value045, value046, value047, value048, value049, value050, value051, value052, "
		    + "value053, value054, value055, value056, value057, value058, value059, value060, value061, "
		    + "value062, value063, value064, value065, value066, value067, value068, value069, value070, "
		    + "value071, value072, value073, value074, value075, value076, value077, value078, value079, "
		    + "value080, value081, value082, value083, value084, value085, value086, value087, value088, "
		    + "value089, value090, value091, value092, value093, value094, value095, value096, value097, "
		    + "value098, value099, value101], new values are [value101, value102, value103, value104, "
		    + "value105, value106, value107, value108, value109, value110, value111, value112, value113, "
		    + "value114, value115, value116, value117, value118, value119, value120, value121, value122, "
		    + "value123, value124, value125, value126, value127, value128, value129, value130, value131, "
		    + "value132, value133, value134, value135, value136, value137, value138, value139, value140, "
		    + "value141, value142, value143, value144, value145, value146, value147, value148, value149, "
		    + "value150, value151, value152, value153, value154, value155, value156, value157, value158, "
		    + "value159, value160, value161, value162, value163, value164, value165, value166, value167, "
		    + "value168, value169, value170, value171, value172, value173, value174, value175, value176, "
		    + "value177, value178, value179, value180, value181, value182, value183, value184, value185, "
		    + "value186, value187, value188, value189, value190, value191, value192, value193, value194, "
		    + "value195, value196, value197, value198, value199, value200]"));
	}

   // Function launching the desired task, and returning the standard output
    public static String launchSyncTask(String task) throws Exception {

		// Initialize the tasks
		SimpleSynchronize sync = new SimpleSynchronize();
		List<String> syncType = new ArrayList<String>();
		syncType.add(task);

		// Change system output stream to a variable
		PrintStream orgStream = System.out; // save original stream
		ByteArrayOutputStream lscOutput = new ByteArrayOutputStream();
		System.setOut(new PrintStream(lscOutput));

		// Launch tasks
		boolean ret = sync.launch(new ArrayList<String>(), syncType, new ArrayList<String>());

		// Restore standard output stream
		System.setOut(orgStream);

		// Check the global status of the task
		assertTrue(ret);

		return lscOutput.toString();
    }

}
