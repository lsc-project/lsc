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
package org.lsc;

import com.fasterxml.jackson.databind.ObjectMapper; // For encoding object to JSON
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import java.io.StringWriter;
import java.io.PrintWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.lsc.configuration.LscConfiguration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.File;
import java.util.Scanner;


/**
 * This test case attempts to reproduce a ldap2ldap setup via SimpleSynchronize.
 * It attempts to launch the tasks defined in src/test/resources/etc/lsc.xml:
 * ldap2ldapHookTestCreate ldap2ldapHookTestUpdate ldap2ldapHookTestDelete
 */
public class Ldap2LdapHookSyncTest extends CommonLdapSyncTest {


	@Before
	public void setup() {
		LscConfiguration.reset();
		LscConfiguration.getInstance();
		Assert.assertNotNull(LscConfiguration.getConnection("src-ldap"));
		Assert.assertNotNull(LscConfiguration.getConnection("dst-ldap"));
		reloadJndiConnections();
	}

	@Test
	public final void testLdap2LdapJSONHookSyncTest() throws Exception {

		// Declare the tasks to launch in the correct order
		List<String> sync_tasks = Arrays.asList("ldap2ldapJSONHookTestCreate", "ldap2ldapJSONHookTestUpdate");
		List<String> clean_tasks = Arrays.asList("ldap2ldapJSONHookTestDelete");

		// perform the sync
		launchSyncCleanTask(sync_tasks, false, true, false);
		launchSyncCleanTask(clean_tasks, false, false, true);

		// check the results of the synchronization
		reloadJndiConnections();

		ObjectMapper mapperCreatedEntry = new ObjectMapper();
		JsonNode expectedCreatedEntry = mapperCreatedEntry.readTree("[ { \"attributeName\" : \"objectClass\", \"values\" : [ \"inetOrgPerson\", \"person\", \"top\" ], \"operation\" : \"ADD_VALUES\" }, { \"attributeName\" : \"cn\", \"values\" : [ \"CN0001-hook\" ], \"operation\" : \"ADD_VALUES\" }, { \"attributeName\" : \"sn\", \"values\" : [ \"CN0001-hook\" ], \"operation\" : \"ADD_VALUES\" } ]");

		checkJSONSyncResults("create", expectedCreatedEntry);

		ObjectMapper mapperUpdatedEntry = new ObjectMapper();
		JsonNode expectedUpdatedEntry = mapperUpdatedEntry.readTree("[ { \"attributeName\" : \"description\", \"values\" : [ \"CN0001-hook\" ], \"operation\" : \"REPLACE_VALUES\" }, {\"attributeName\":\"userCertificate;binary\",\"values\":[\"MIIDkTCCAnmgAwIBAgIUDhx/9qofTrT+yNFFvihdDn7rjOQwDQYJKoZIhvcNAQELBQAwWDELMAkGA1UEBhMCRlIxDTALBgNVBAgMBHRlc3QxDTALBgNVBAcMBHRlc3QxDTALBgNVBAoMBHRlc3QxDTALBgNVBAsMBHRlc3QxDTALBgNVBAMMBHRlc3QwHhcNMjMxMDI3MTQzMDQxWhcNMzMxMDI0MTQzMDQxWjBYMQswCQYDVQQGEwJGUjENMAsGA1UECAwEdGVzdDENMAsGA1UEBwwEdGVzdDENMAsGA1UECgwEdGVzdDENMAsGA1UECwwEdGVzdDENMAsGA1UEAwwEdGVzdDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKdWt0QgFnEi7a1hIJQv4ZdOM5y0GGLHQYNrUNSReArvpkYUY5zasFNVzVHCApRuj0t1NMDrn1gNzKkxTIbYGaWRSn+21J0ow+Nxh2TAQW8dkJnWTksCfyGGGItI5q3ST3EUKnepaAzUYYENcSHRyx7UY/3XuzcW0aGhy4PrVTIHBpyLq0Uzv8nH5nbWM+LYt6YbQMmlAz/psTXIC2dfEZhUb4plLGSo7rZxM5geC6Z+os+I8+uw+mGjps1VP7eGq0jCGHNs2rUHMqBNgLvwMH2WlMXo/iNarAb8fUEPdp59FwiTygBlWAn6GoKHJ1HWPpqMxdtjL2Y5+ZMcp70eJqcCAwEAAaNTMFEwHQYDVR0OBBYEFLwffjUBL/Rp4a6MgeCJiFnCZFu8MB8GA1UdIwQYMBaAFLwffjUBL/Rp4a6MgeCJiFnCZFu8MA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZIhvcNAQELBQADggEBACjwsg1Z9PyauoKAhkIfyPTEnlEOCo1nN37c2vnH4fyY6fuBdV6GWtk/u9FCuDmYT/4KDRxe33ZUChwSUX0INgamOarWRES3UoPC1GeOvuMf7uustEMLcHAYZVKXSZUrsOjw+VIZ5XrD6GDE64QtvW5Ve3jf43aGgLf27NF0vhF9+gHOZjjBT33S977HUutMUKfRu9PdHAn8Yb1FmSbAvqqK+SAjn6cJC8l5yS5t0BSNQGbKSA8bPzvWI9HXYVvb+ym6GDrsr+Zad3NrqUSZGzS2JFEDVD9aAikldXu6g02fA5A7nufVePmaG7iTyylO/ZU2lTiJ0SHc2DnO0pg2i+0=\"],\"operation\":\"REPLACE_VALUES\"} ]");

		checkJSONSyncResults("update", expectedUpdatedEntry);

		checkJSONSyncResults("delete", null);
	}

	@Test
	public final void testLdap2LdapLDIFHookSyncTest() throws Exception {

		// Declare the tasks to launch in the correct order
		List<String> sync_tasks = Arrays.asList("ldap2ldapLDIFHookTestCreate", "ldap2ldapLDIFHookTestUpdate");
		List<String> clean_tasks = Arrays.asList("ldap2ldapLDIFHookTestDelete");

		// perform the sync
		launchSyncCleanTask(sync_tasks, false, true, false);
		launchSyncCleanTask(clean_tasks, false, false, true);

		// check the results of the synchronization
		reloadJndiConnections();

		List<String> expectedCreatedEntry = Arrays.asList(
			"dn: cn=CN0001-hook,ou=ldap2ldap2TestTaskDst,ou=Test Data,dc=lsc-project,dc=org",
			"changetype: add",
			"objectClass: inetOrgPerson",
			"objectClass: person",
			"objectClass: top",
			"cn: CN0001-hook",
			"sn: CN0001-hook"
		);

		checkLDIFSyncResults("create", expectedCreatedEntry);

		List<String> expectedUpdatedEntry = Arrays.asList(
			"dn: cn=CN0001-hook,ou=ldap2ldap2TestTaskDst,ou=Test Data,dc=lsc-project,dc=org",
			"changetype: modify",
			"replace: description",
			"description: CN0001-hook"
		);

		checkLDIFSyncResults("update", expectedUpdatedEntry);

		checkLDIFSyncResults("delete", new ArrayList<String>());
	}

	/*
	 * Read hook log file to check passed arguments and modification passed as input
	*/
	private final void checkLDIFSyncResults(String operation, List<String> expectedEntry) throws Exception {

		List<String> hookResults = new ArrayList<String>();
		try {
			File hookFile = new File("hook-ldif-" + operation + ".log");
			Scanner hookReader = new Scanner(hookFile);

			while (hookReader.hasNextLine()) {
				String data = hookReader.nextLine();
				hookResults.add(data);
			}
			hookReader.close();
			hookFile.delete(); // remove hook log
		} catch (Exception e) {
			fail("Error while reading hook-ldif-" + operation + ".log");
		}
		assertEquals(hookResults.get(0), "cn=CN0001-hook,ou=ldap2ldap2TestTaskDst,ou=Test Data,dc=lsc-project,dc=org");
		assertEquals(hookResults.get(1), operation);

		if(operation != "delete") {
			// Make sure all attributes in expectedEntry are present in the hook file
			List<String> entry = new ArrayList(hookResults.subList(3, (hookResults.size()-1)));
			for (String attr : expectedEntry) {
				assertTrue("Attribute " + attr + " not found in " + operation + " entry " + entry.toString(), entry.contains(attr));
			}
		}

	}

	/*
	 * Read hook log file to check passed arguments and modification passed as input
	*/
	private final void checkJSONSyncResults(String operation, JsonNode expectedEntry) throws Exception {

		List<String> hookResults = new ArrayList<String>();
		try {
			File hookFile = new File("hook-json-" + operation + ".log");
			Scanner hookReader = new Scanner(hookFile);

			while (hookReader.hasNextLine()) {
				String data = hookReader.nextLine();
				hookResults.add(data);
			}
			hookReader.close();
			hookFile.delete(); // remove hook log
		} catch (Exception e) {
			fail("Error while reading hook-json-" + operation + ".log");
		}
		assertEquals(hookResults.get(0), "cn=CN0001-hook,ou=ldap2ldap2TestTaskDst,ou=Test Data,dc=lsc-project,dc=org");
		assertEquals(hookResults.get(1), operation);

		if(operation != "delete") {
			// Make sure all attributes in expectedEntry are present in the hook file
			String entry = String.join("", new ArrayList(hookResults.subList(2, hookResults.size())));

			ObjectMapper mapper = new ObjectMapper();
			JsonFactory factory = mapper.getJsonFactory();
			JsonParser jp = factory.createJsonParser(entry);
			try {
				JsonNode hookOperation = mapper.readTree(jp);
				assertEquals(hookOperation, expectedEntry);
			}
			catch (Exception e) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				fail("Error while decoding operation in hook-json-" + operation + ".log as JSON:" + sw.toString());
			}
		}

	}

	public static void launchSyncCleanTask(List<String> tasks, boolean doAsync, boolean doSync,
					boolean doClean) throws Exception {
		// initialize required stuff
		SimpleSynchronize sync = new SimpleSynchronize();
		List<String> asyncType = new ArrayList<String>();
		List<String> syncType = new ArrayList<String>();
		List<String> cleanType = new ArrayList<String>();


		if (doAsync) {
			for(String taskName: tasks) {
				asyncType.add(taskName);
			}
		}
		
		if (doSync) {
			for(String taskName: tasks) {
				syncType.add(taskName);
			}
		}

		if (doClean) {
			for(String taskName: tasks) {
				cleanType.add(taskName);
			}
		}

		boolean ret = sync.launch(asyncType, syncType, cleanType);
		assertTrue(ret);
	}
}
