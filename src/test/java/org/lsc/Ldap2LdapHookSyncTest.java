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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import java.io.StringWriter;
import java.io.PrintWriter;

import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.annotations.LoadSchema;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.lsc.configuration.LscConfiguration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.io.File;
import java.io.FileNotFoundException;

/**
 * This test case attempts to reproduce a ldap2ldap setup via SimpleSynchronize.
 * It attempts to launch the tasks defined in src/test/resources/etc/lsc.xml:
 * ldap2ldapHookTestCreate ldap2ldapHookTestUpdate ldap2ldapHookTestDelete
 */
@ExtendWith({ ApacheDSTestExtension.class })
@CreateDS(
        name = "DSWithPartitionAndServer", loadedSchemas = {
            @LoadSchema(name = "inetOrgPerson", enabled = true)
		},
        partitions = {
                @CreatePartition(
                    name = "lsc-project",
                    suffix = "dc=lsc-project,dc=org",
                    contextEntry = @ContextEntry(
                            entryLdif = "dn: dc=lsc-project,dc=org\n"
                                        + "dc: lsc-project\n"
                                        + "objectClass: top\n"
                                        + "objectClass: domain\n\n"),
                    indexes = {
                            @CreateIndex(attribute = "objectClass"),
                            @CreateIndex(attribute = "dc"),
							@CreateIndex(attribute = "ou")
                    })
        })
@CreateLdapServer(
            allowAnonymousAccess = true,
    transports = { @CreateTransport(protocol = "LDAP", port = 33389) })
@ApplyLdifs({
		// Entry # 0
		"dn: cn=Directory Manager,ou=system",
		"objectClass: person",
		"objectClass: top",
		"cn: Directory Manager",
		"description: Directory Manager",
		"sn: Directory Manager",
		"userpassword: secret"
})
@ApplyLdifFiles({ "lsc-schema.ldif", "lsc-project.ldif" })
public class Ldap2LdapHookSyncTest extends CommonLdapSyncTest {


	Comparator<JsonNode> nodeComparator = Comparator.comparing(JsonNode::toString);

	@BeforeEach
	public void setup() {
		LscConfiguration.reset();
		LscConfiguration.getInstance();
		assertNotNull(LscConfiguration.getConnection("src-ldap"));
		assertNotNull(LscConfiguration.getConnection("dst-ldap"));
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

		ObjectMapper mapperCreatedEntry = JsonMapper.builder().nodeFactory(new SortingNodeFactory()).build();
		JsonNode expectedCreatedEntry = mapperCreatedEntry.readTree(
			"[ { \"attributeName\" : \"objectClass\", "
			+ "\"values\" :  [ \"inetOrgPerson\",\"person\",\"top\" ], \"operation\" : \"ADD_VALUES\" }, "
			+ "{ \"attributeName\" : \"cn\", "
			+ "\"values\" : [ \"CN0001-hook\" ], \"operation\" : \"ADD_VALUES\" }, "
			+ "{ \"attributeName\" : \"sn\", "
			+ "\"values\" : [ \"CN0001-hook\" ], \"operation\" : \"ADD_VALUES\" } ]");
		sortArrays(expectedCreatedEntry);

		checkJSONSyncResults("create", expectedCreatedEntry);

		ObjectMapper mapperUpdatedEntry = JsonMapper.builder().nodeFactory(new SortingNodeFactory()).build();
		JsonNode expectedUpdatedEntry = mapperUpdatedEntry.readTree(
				"[ { \"attributeName\" : \"description\", "
				+ "\"values\" : [ \"CN0001-hook\" ], \"operation\" : \"REPLACE_VALUES\" }, "
				+ "{\"attributeName\":\"userCertificate;binary\","
				+ "\"values\":[\"MIIDkTCCAnmgAwIBAgIUDhx/9qofTrT+yNFFvihdDn7rjOQwDQYJKoZIhvc"
				+ "NAQELBQAwWDELMAkGA1UEBhMCRlIxDTALBgNVBAgMBHRlc3QxDTALBgNVBAcMBHRlc3QxDTAL"
				+ "BgNVBAoMBHRlc3QxDTALBgNVBAsMBHRlc3QxDTALBgNVBAMMBHRlc3QwHhcNMjMxMDI3MTQzM"
				+ "DQxWhcNMzMxMDI0MTQzMDQxWjBYMQswCQYDVQQGEwJGUjENMAsGA1UECAwEdGVzdDENMAsGA1"
				+ "UEBwwEdGVzdDENMAsGA1UECgwEdGVzdDENMAsGA1UECwwEdGVzdDENMAsGA1UEAwwEdGVzdDC"
				+ "CASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKdWt0QgFnEi7a1hIJQv4ZdOM5y0GGLH"
				+ "QYNrUNSReArvpkYUY5zasFNVzVHCApRuj0t1NMDrn1gNzKkxTIbYGaWRSn+21J0ow+Nxh2TAQ"
				+ "W8dkJnWTksCfyGGGItI5q3ST3EUKnepaAzUYYENcSHRyx7UY/3XuzcW0aGhy4PrVTIHBpyLq0"
				+ "Uzv8nH5nbWM+LYt6YbQMmlAz/psTXIC2dfEZhUb4plLGSo7rZxM5geC6Z+os+I8+uw+mGjps1"
				+ "VP7eGq0jCGHNs2rUHMqBNgLvwMH2WlMXo/iNarAb8fUEPdp59FwiTygBlWAn6GoKHJ1HWPpqM"
				+ "xdtjL2Y5+ZMcp70eJqcCAwEAAaNTMFEwHQYDVR0OBBYEFLwffjUBL/Rp4a6MgeCJiFnCZFu8M"
				+ "B8GA1UdIwQYMBaAFLwffjUBL/Rp4a6MgeCJiFnCZFu8MA8GA1UdEwEB/wQFMAMBAf8wDQYJKo"
				+ "ZIhvcNAQELBQADggEBACjwsg1Z9PyauoKAhkIfyPTEnlEOCo1nN37c2vnH4fyY6fuBdV6GWtk"
				+ "/u9FCuDmYT/4KDRxe33ZUChwSUX0INgamOarWRES3UoPC1GeOvuMf7uustEMLcHAYZVKXSZUr"
				+ "sOjw+VIZ5XrD6GDE64QtvW5Ve3jf43aGgLf27NF0vhF9+gHOZjjBT33S977HUutMUKfRu9PdH"
				+ "An8Yb1FmSbAvqqK+SAjn6cJC8l5yS5t0BSNQGbKSA8bPzvWI9HXYVvb+ym6GDrsr+Zad3NrqU"
				+ "SZGzS2JFEDVD9aAikldXu6g02fA5A7nufVePmaG7iTyylO/ZU2lTiJ0SHc2DnO0pg2i+0=\"],"
				+ "\"operation\":\"REPLACE_VALUES\"} ]");
		sortArrays(expectedUpdatedEntry);

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
				"sn: CN0001-hook");

		checkLDIFSyncResults("create", expectedCreatedEntry);

		List<String> expectedUpdatedEntry = Arrays.asList(
				"dn: cn=CN0001-hook,ou=ldap2ldap2TestTaskDst,ou=Test Data,dc=lsc-project,dc=org",
				"changetype: modify",
				"replace: description", "description: CN0001-hook");

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
				assertTrue(
					entry.stream().anyMatch(attr::equalsIgnoreCase),
					"Attribute " + attr + " not found in " + operation + " entry " + entry.toString()
				);
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

			try (Scanner hookReader = new Scanner(hookFile.getAbsoluteFile())) {
				while (hookReader.hasNextLine()) {
					String data = hookReader.nextLine();
					hookResults.add(data);
				}
			} catch (FileNotFoundException fnfe) {
				fail("Cannot find the  hook file" + "hook-json-" + operation + ".log " + fnfe.getMessage());
			}

			hookFile.delete(); // remove hook log
		} catch (Exception e) {
			fail("Error while reading hook-json-" + operation + ".log " + e.getMessage());
		}
		assertEquals(hookResults.get(0), "cn=CN0001-hook,ou=ldap2ldap2TestTaskDst,ou=Test Data,dc=lsc-project,dc=org");
		assertEquals(hookResults.get(1), operation);

		if (operation != "delete") {
			// Make sure all attributes in expectedEntry are present in the hook file
			String entry = String.join("", new ArrayList<>(hookResults.subList(2, hookResults.size())));

			ObjectMapper mapper = JsonMapper.builder().nodeFactory(new SortingNodeFactory()).build();
			JsonFactory factory = mapper.getFactory();
			JsonParser jp = factory.createParser(entry);
			try {
				JsonNode hookOperation = mapper.readTree(jp);
				sortArrays(hookOperation);
				TextNodeComparator cmp = new TextNodeComparator();
				assertTrue(hookOperation.equals(cmp, expectedEntry),
					"Unexpected JSON action in hook" +
					"\nGot:\n" + prettyPrintJsonString(hookOperation) +
					"\nbut expected:\n" + prettyPrintJsonString(expectedEntry));
			} catch (Exception e) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				fail("Error while decoding operation in hook-json-" + operation + ".log as JSON:" + sw.toString());
			}
		}

	}

	public static void launchSyncCleanTask(List<String> tasks, boolean doAsync, boolean doSync, boolean doClean)
			throws Exception {
		// initialize required stuff
		SimpleSynchronize sync = new SimpleSynchronize();
		List<String> asyncType = new ArrayList<String>();
		List<String> syncType = new ArrayList<String>();
		List<String> cleanType = new ArrayList<String>();

		if (doAsync) {
			for (String taskName : tasks) {
				asyncType.add(taskName);
			}
		}

		if (doSync) {
			for (String taskName : tasks) {
				syncType.add(taskName);
			}
		}

		if (doClean) {
			for (String taskName : tasks) {
				cleanType.add(taskName);
			}
		}

		boolean ret = sync.launch(asyncType, syncType, cleanType);
		assertTrue(ret);
	}

	// Method for sorting arrays in JSON nodes
	private void sortArrays(JsonNode node) {
		if (node.isArray()) {
			for (JsonNode child : node) {
				sortArrays(child);
			}
			// Sort elements in arrayNode
			var arrayNode = (ArrayNode) node;
			var iter = arrayNode.elements();
			var sortedElemsCopy = new ArrayList<JsonNode>(arrayNode.size());
			while(iter.hasNext()) {
				var n = iter.next();
				sortedElemsCopy.add(n);
			}
			sortedElemsCopy.sort(nodeComparator);

			arrayNode.removeAll();
			arrayNode.addAll(sortedElemsCopy);

		} else if (node.isObject()) {
			ObjectNode objectNode = (ObjectNode) node;
			Iterator<Map.Entry<String, JsonNode>> iter = objectNode.fields();
			while (iter.hasNext()) {
				Map.Entry<String, JsonNode> entry = iter.next();
				sortArrays(entry.getValue());
			}
		}
	}

	// Method for displaying JSON structure in a pretty way
	private String prettyPrintJsonString(JsonNode jsonNode) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			Object json = mapper.readValue(jsonNode.toString(), Object.class);
			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
		} catch (Exception e) {
			return "Error while pretty print of JSON structure " + e.toString();
		}
	}

}

// Class for comparing JSON element with ignore case
class TextNodeComparator implements Comparator<JsonNode> {
	@Override
	public int compare(JsonNode o1, JsonNode o2) {
		if (o1.equals(o2)) {
			return 0;
		}
		if ((o1 instanceof TextNode) && (o2 instanceof TextNode)) {
			String s1 = ((TextNode) o1).asText();
			String s2 = ((TextNode) o2).asText();
			if (s1.equalsIgnoreCase(s2)) {
				return 0;
			}
		}
		return 1;
	}
}

// Class for sorting JSON structures
class SortingNodeFactory extends JsonNodeFactory {
	@Override
	public ObjectNode objectNode() {
		return new ObjectNode(this, new TreeMap<String, JsonNode>());
	}
}
