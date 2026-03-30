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
 * This test case attempts to reproduce a typical ldap2ldap setup via
 * SimpleSynchronize. This relies on settings in lsc.properties from the test
 * resources, and entries in the local OpenDS directory. testSync() performs a
 * synchronization between two branches of the local LDAP server, and should
 * perform 3 operations : 1 ADD, 1 MODRDN, 1 MODIFY. The
 * 
 * @author Jonathan Clarke &ltjonathan@phillipoux.net&gt;
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
        
            // Source entry test, present in destination, and that will be updated
            "dn: uid=test1,OU=source,OU=Test Data,DC=lsc-project,DC=org",
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "objectClass: inetOrgPerson",
            "cn: test1",
            "sn: test1",
            "uid: test1",
            "description: A test entry that does exist in both servers",

            // Source entry test2, should be added to destination
            "dn: uid=test2,ou=source,ou=Test Data,dc=lsc-project,dc=org",
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "objectClass: inetOrgPerson",
            "cn: test2",
            "sn: test2",
            "uid: test2",
            "description: Number three's descriptive text",

        // Destination data
        "dn: cn=destination,ou=Test Data,dc=lsc-project,dc=org",
        "objectClass: top",
        "objectClass: organizationalRole",
        "cn: destination",

            // This entry will be updated from source, with an added description attribute
            "dn: CN=test1,CN=destination,OU=Test Data,DC=lsc-project,DC=org",
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "objectClass: inetOrgPerson",
            "cn: test1",
            "sn: test1",
            
            // Destination entry toDelete will be deleted in clean mode
            "dn: cn=toDelete,cn=destination,ou=Test Data,dc=lsc-project,dc=org",
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "objectClass: inetOrgPerson",
            "cn: toDelete",
            "sn: toDelete"

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
                 // The allFilter select all entries from source
                 allFilter = "(uid=*)",
                 //oneFilter = "\"(&(cn={cn})(createTimeStamp>=\" + new Date().getDate + \"))\"",
                 // The one filter will select every entry from source
                 oneFilter = "(uid={uid})",
                 // The clean filter will select all entries from *destination*, thus the 'cn'
                 cleanFilter = "(uid={cn})",
                 //filterAsync = "(&(uid=*)(modifytimestamp>={0}))",
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
                 allFilter = "(&(cn=*)(sn=*))",
                 oneFilter = "(cn={uid})",
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

public class Ldap2LdapSyncCleanTest extends CommonLdapSyncTest {
    public final static String TASK_NAME = "L2LTestTaskDelete";
    
    /** The Ldap connection to the defined Ldap server */
    private LdapConnection connection;

    // A few definition overloading those from the parent class
    private final static String SOURCE_DN = "ou=source,ou=Test Data,dc=lsc-project,dc=org";
    private final static String DESTINATION_DN = "cn=destination,ou=Test Data,dc=lsc-project,dc=org";

    private final static String DN_DELETE_SRC = "uid=toDelete," + SOURCE_DN;
    private final static String DN_DELETE_DST = "CN=toDelete," + DESTINATION_DN;
    private final static String DN_UPDATE_DST = "CN=test1," + DESTINATION_DN;
    private final static String DN_CREATE_DST = "CN=test2," + DESTINATION_DN;

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
	public final void testCleanLdap2Ldap() throws Exception {
		// make sure the contents of the directory are as we expect to begin with
		assertTrue(dstJndiServices.exists(DN_DELETE_DST));
		assertFalse(srcJndiServices.exists(DN_DELETE_SRC));

		// perform the clean
		System.out.println("------> Clean phase, delete toDelete");
		launchTask(TASK_NAME, TaskType.CLEAN);

		// check the results of the clean
		reloadJndiConnections();
		
		// The CN=toDelete entry must have been deleted, the CN=test1 should be present, and
		// CN=test2 absent
		assertFalse(dstJndiServices.exists(DN_DELETE_DST));
        assertFalse(dstJndiServices.exists(DN_CREATE_DST));
        assertTrue(dstJndiServices.exists(DN_UPDATE_DST));
        
		// Now launch the create task
        launchTask(TASK_NAME, TaskType.SYNC);
        
        // check the result on sync
        reloadJndiConnections();

        // The CN+toDelete must be absent, the CN=test1 must have been updated
        // and the CN=test2 must have been added
        assertFalse(dstJndiServices.exists(DN_DELETE_DST));
        assertTrue(dstJndiServices.exists(DN_UPDATE_DST));
        assertTrue(dstJndiServices.exists(DN_CREATE_DST));

        // Check that the updated entry contains the description Attribute
        Entry test1Entry = connection.lookup(DN_UPDATE_DST);
        assertTrue(test1Entry.contains( "description", "A test entry that does exist in both servers"));

        // And check that the test2 entry has been created properly
        Entry test2Entry = connection.lookup(DN_CREATE_DST);
        assertTrue(test2Entry.contains( "cn", "test2"));
        assertTrue(test2Entry.contains( "sn", "test2"));
        assertTrue(test2Entry.contains( "objectClass", "top", "person", "organizationalPerson", "inetOrgPerson"));
        assertTrue(test2Entry.contains( "description", "Number three's descriptive text"));
        
        // The uid must not be present
        assertFalse(test2Entry.containsAttribute( "uid" ));
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
