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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.annotations.LoadSchema;
import org.apache.directory.server.core.hash.SshaPasswordHashingInterceptor;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.lsc.configuration.LdapConnectionType;
import org.lsc.configuration.LscConfiguration;
import org.lsc.configuration.PolicyType;
import org.lsc.utils.annotations.CreateConnection;
import org.lsc.utils.annotations.CreateDataset;
import org.lsc.utils.annotations.CreateLSC;
import org.lsc.utils.annotations.CreateLdapDestinationService;
import org.lsc.utils.annotations.CreateLdapSourceService;
import org.lsc.utils.annotations.CreatePropertiesBasedSyncOptions;
import org.lsc.utils.annotations.CreateTask;
import org.lsc.utils.annotations.CreateValuesType;
import org.lsc.utils.annotations.LscTestExtension;

/**
 * This test case leverage a scripted filter which select entries older than a specific date
 * which will not be synced
 */
@ExtendWith({ LscTestExtension.class })
@CreateDS(
    name = "DSWithPartitionAndServer",
    loadedSchemas = {
		@LoadSchema(name = "other", enabled = true),
        @LoadSchema(name = "inetOrgPerson", enabled = true) 
	}, 
    additionalInterceptors = {
		SshaPasswordHashingInterceptor.class }, 
    partitions = {
		@CreatePartition(
		    name = "lsc-project", 
		    suffix = "dc=lsc-project,dc=org", 
		    contextEntry = 
		        @ContextEntry(
		            entryLdif = 
		                "dn: dc=lsc-project,dc=org\n" +
						"dc: lsc-project\n" + 
		                "objectClass: top\n" + 
						"objectClass: domain\n\n"
		        ), 
		    indexes = {
				@CreateIndex(attribute = "objectClass"), 
				@CreateIndex(attribute = "dc"),
				@CreateIndex(attribute = "ou") 
			}) 
	})
@CreateLdapServer(
    allowAnonymousAccess = true, 
    transports = { 
        @CreateTransport(protocol = "LDAP", port = 33389),
		@CreateTransport(protocol = "LDAPS", port = 33636) 
    })
@ApplyLdifs({
    // Entry # 0
    "dn: cn=Directory Manager,ou=system", 
    "objectClass: top", 
    "objectClass: person", 
    "cn: Directory Manager",
    "description: Directory Manager", 
    "sn: Directory Manager", 
    "userpassword: secret",
    
    // Test data entry
    "dn: ou=Test Data,dc=lsc-project,dc=org",
    "objectClass: top",
    "objectClass: organizationalUnit",
    "ou: Test Data",

        // Source data
        "dn: ou=source,ou=Test Data,dc=lsc-project,dc=org",
        "objectClass: top",
        "objectClass: organizationalUnit",
        "ou: source",
        
            // Source entry test, present in destination, but is one year old
            "dn: uid=test1,OU=source,OU=Test Data,DC=lsc-project,DC=org",
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "objectClass: inetOrgPerson",
            "cn: test1",
            "sn: test1",
            "uid: test1",
            "description: A one year old entry present on both servers",
            "createTimeStamp: 20250330000000Z",

            // Source entry test2, not present in destination, but is one year old
            "dn: uid=test2,ou=source,ou=Test Data,dc=lsc-project,dc=org",
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "objectClass: inetOrgPerson",
            "cn: test2",
            "sn: test2",
            "uid: test2",
            "description: A year old entry",
            "createTimeStamp: 20250330000000Z",

            // Source entry test, present in destination, which will be updated
            "dn: uid=test3,ou=source,ou=Test Data,dc=lsc-project,dc=org",
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "objectClass: inetOrgPerson",
            "cn: test3",
            "sn: test3",
            "uid: test3",
            "description: A recent entry present in both servers",

            // Source entry test, absent from destination, which will be added
            "dn: uid=test4,ou=source,ou=Test Data,dc=lsc-project,dc=org",
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "objectClass: inetOrgPerson",
            "cn: test4",
            "sn: test4",
            "uid: test4",
            "description: A recent entry absent from destination",
            
        // Destination data
        "dn: cn=destination,ou=Test Data,dc=lsc-project,dc=org",
        "objectClass: top",
        "objectClass: organizationalRole",
        "cn: destination",

            // This entry will not be updated because the source entry is one year old
            "dn: cn=test1,cn=destination,ou=Test Data,dc=lsc-project,dc=org",
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "objectClass: inetOrgPerson",
            "cn: test1",
            "sn: test1",
            
            // This entry will be updated because the source entry is recent
            "dn: cn=test3,cn=destination,ou=Test Data,dc=lsc-project,dc=org",
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "objectClass: inetOrgPerson",
            "cn: test3",
            "sn: test3",

})
@ApplyLdifFiles({ 
    "lsc-schema.ldif"
})

//Create the LSC instance
@CreateLSC(
 id = "L2Lconfig", 
 revision = 1, 
 connections = {
     // Source
     @CreateConnection(
         url = "ldap://localhost:33389",
         name = "src-ldap",
         type = LdapConnectionType.class,
         id = "LdapSrc",
         username="cn=Directory Manager,ou=system",
         password = "secret",
         binaryAttributes = {@CreateValuesType(string={"personalSignature"})},
         pageSize = -1
     ),
     // Destination
     @CreateConnection(
         name = "dst-ldap",
         type = LdapConnectionType.class,
         id = "LdapDest",
         url = "ldap://localhost:33389",
         username="cn=Directory Manager,ou=system",
         password = "secret",
         binaryAttributes = {@CreateValuesType(string={"personalSignature"})},
         pageSize = -1
     )
 },
 tasks = {
     @CreateTask(
         id = "L2LTestTaskDelete",
         name = "L2LTestTaskDelete",
         // bean, default
         // cleanHook, empty
         // syncHook, empty
         ldapSourceService = 
             @CreateLdapSourceService(
                 name = "ldap-source",
                 connectionRef = "src-ldap",
                 baseDn = "ou=source,ou=Test Data,dc=lsc-project,dc=org",
                 pivotAttributes = {"uid"},
                 // The allFilter select all entries from source, as soon as they are less than 1 years old
                 allFilter = "\"(&(uid=*)(createTimeStamp>=\" + (new Date().getFullYear()-1) + \"1231235959Z))\"",
                 oneFilter = "\"(&(uid={uid})(createTimeStamp>=\" + (new Date().getFullYear()-1) + \"1231235959Z))\"",
                 // The clean filter will select all entries from *destination*, thus the 'cn'
                 cleanFilter = "\"(uid={cn})\"",
                 dateFormat = "yyyyMMddHHmmss'Z'",
                 interval = 5,
                 fetchedAttributes = {
                     "cn", 
                     "sn",
                     "uid",
                     "description"
                 }),
         ldapDestinationService = 
             @CreateLdapDestinationService(
                 name = "ldap-destination",
                 connectionRef = "dst-ldap",
                 baseDn = "cn=destination,ou=Test Data,dc=lsc-project,dc=org",
                 pivotAttributes = {"cn", "sn"},
                 allFilter = "\"(&(cn=*)(sn=*))\"",
                 oneFilter = "\"(cn={uid})\"",
                 fetchedAttributes = {
                     "cn",
                     "sn",
                     "description",
                     "objectClass"
                 }),
         propertiesBasedSyncOptions = 
             @CreatePropertiesBasedSyncOptions(
                 // The main identifier will be the destination DN we want to operate on
                 // Here, in source the entry's RDN uses an uid, while in the destination
                 // the RDN is using a cn
                 mainIdentifier = 
                     // We get the source DN
                     "var srcDn = srcBean.getMainIdentifier();\n" +
                     // then get the UID value (from source) 
                     "var uid = srcBean.getDatasetFirstValueById(\"uid\").trim();\n" +
                     // We construct the suffix which is the source parent DN
                     "var suffix = srcDn.substring(srcDn.indexOf(\",\") + 1);\n" +
                     // Uper case ou and dc
                     "suffix = suffix.replace(/ou=/gi, \"OU=\").replace(/dc=/gi, \"DC=\");\n" +
                     // then replace OU=source by CN=destination
                     "suffix = suffix.replace(/OU=source/i, \"CN=destination\");\n" +
                     // The result is a new DN.
                     // If the source DN was uid=test,ou=Test Data,dc=lsc-project,dc=org
                     // the the destination DN will be CN=test,CN=destination,OU=Test Data,DC=msc-project,DC=org
                     "\"CN=\" + uid + \",\" + suffix;",
                 defaultDelimiter = ",",
                 defaultPolicy = PolicyType.MERGE,
                 dataset = {
                     // cn dataset: convert UID to CN
                     @CreateDataset(
                         name = "cn",
                         policy = PolicyType.FORCE,
                         forceValues = {
                             @CreateValuesType(string = {"srcBean.getDatasetFirstBinaryValueById(\"uid\")"})
                         }),
                     // sn dataset: convert UID to SN
                     @CreateDataset(
                         name = "sn",
                         policy = PolicyType.FORCE,
                         forceValues = {
                             @CreateValuesType(string = {"srcBean.getDatasetFirstBinaryValueById(\"uid\")"})
                         }),
                     // ObjectClasss dataset
                     @CreateDataset(
                         name = "objectClass",
                         policy = PolicyType.FORCE,
                         forceValues = {
                             @CreateValuesType(string = {"\"top\"", "\"person\"", "\"organizationalPerson\"", "\"inetOrgPerson\""})
                         }),
                     // Description dataset
                     @CreateDataset(
                         name = "description",
                         policy = PolicyType.MERGE)
                 })
     )}
)

public class Ldap2LdapScriptInFilter extends CommonLdapSyncTest {
    public final static String TASK_NAME = "L2LTestTaskDelete";
    
    /** The Ldap connection to the defined Ldap server */
    private LdapConnection connection;

    // A few definition overloading those from the parent class
    private final static String DESTINATION_DN = "cn=destination,ou=Test Data,dc=lsc-project,dc=org";

    private final static String DN_TEST1_DST = "CN=test1," + DESTINATION_DN;
    private final static String DN_TEST2_DST = "CN=test2," + DESTINATION_DN;
    private final static String DN_TEST3_DST = "CN=test3," + DESTINATION_DN;
    private final static String DN_TEST4_DST = "CN=test4," + DESTINATION_DN;

    // Define the type of task to launch
    private static enum TaskType {
        ASYNC,
        CLEAN,
        SYNC,
        SYNC_CLEAN
    }
    
	@BeforeEach
	public void setup() throws LdapException {
        LscConfiguration.loadFromInstance(classLscInstance);
        LscConfiguration.getInstance();
        reloadJndiConnections();

        connection = IntegrationUtils.getAdminConnection( getService() );
	}

	/**
	 * Test that we can forbid the deletion of entries based on a condition
	 */
	@Test
	public final void testSyncWithScriptedFilter() throws Exception {
		// Launch the create task
        launchTask(TASK_NAME, TaskType.SYNC);
        
        // check the result on sync
        reloadJndiConnections();

        // The test1 should be present, but not updated
        assertTrue(dstJndiServices.exists(DN_TEST1_DST));
        Entry test1Entry = connection.lookup(DN_TEST1_DST);
        assertFalse(test1Entry.containsAttribute( "description"));
        
        // The test2 should not have been added
        assertFalse(dstJndiServices.exists(DN_TEST2_DST));

        // The test3 should be present and have been updated
        assertTrue(dstJndiServices.exists(DN_TEST3_DST));
        
        // The test4 should have been added
        assertTrue(dstJndiServices.exists(DN_TEST4_DST));
        Entry test4Entry = connection.lookup(DN_TEST4_DST);
        System.out.println(test4Entry);
        assertTrue(test4Entry.contains( "description", " A recent entry absent from destination"));
	}

	private static void launchTask(String taskName, TaskType taskType)
			throws Exception {
		// initialize required stuff
		SimpleSynchronize sync = new SimpleSynchronize();
		List<String> asyncType = new ArrayList<String>();
		List<String> syncType = new ArrayList<String>();
		List<String> cleanType = new ArrayList<String>();

		switch ( taskType ) {
		    case ASYNC:
	            asyncType.add(taskName);
	            break;
	            
		    case CLEAN:
	            cleanType.add(taskName);
                break;

		    case SYNC:
		        syncType.add(taskName);
		        break;
		        
		    case SYNC_CLEAN:
                syncType.add(taskName);
	            cleanType.add(taskName);
                break;
		}

		boolean ret = sync.launch(asyncType, syncType, cleanType);
		assertTrue(ret);
	}
}
