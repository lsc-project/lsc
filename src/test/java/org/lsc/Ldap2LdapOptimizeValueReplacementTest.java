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
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

import java.util.ArrayList;
import java.util.List;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;

/**
 * This test case attempts to reproduce a ldap2ldap setup via SimpleSynchronize.
 * It attempts to launch the tasks defined in src/test/resources/etc/lsc.xml:
 * ldap2ldapOptimizeValueReplacementTestCreate
 * ldap2ldapOptimizeValueReplacementTestUpdate1
 * ldap2ldapOptimizeValueReplacementTestUpdate2
 */
@ExtendWith({ LscTestExtension.class })
@CreateDS(name = "DSWithPartitionAndServer", 
    loadedSchemas = {
		@LoadSchema(name = "other", enabled = true) }, 
    partitions = {
		@CreatePartition(
			name = "lsc-project", 
			suffix = "dc=lsc-project,dc=org", 
			contextEntry = @ContextEntry(
			     entryLdif = "dn: dc=lsc-project,dc=org\n" +
			                 "dc: lsc-project\n" + 
			                 "objectClass: top\n" + 
			                 "objectClass: domain\n\n"), 
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
    
            // Destination data
            "dn: ou=L2Ldst,ou=Test Data,dc=lsc-project,dc=org",
            "objectClass: top",
            "objectClass: organizationalUnit",
            "ou: L2Ldst",
            
                // Source entry 0001
                "dn: cn=CN0001,ou=L2Ldst,ou=Test Data,dc=lsc-project,dc=org",
                "objectClass: top",
                "objectClass: person",
                "objectClass: organizationalPerson",
                "objectClass: inetOrgPerson",
                "cn: CN0001",
                "sn: SN0001",
                "telephoneNumber: 123456",
                "telephoneNumber: 456789",
                "seeAlso: cn=CN0001,ou=L2Ldst,ou=Test Data,dc=lsc-project,dc=org",
                
                // Source entry 0002
                "dn: cn=CommonName0002,ou=L2Ldst,ou=Test Data,dc=lsc-project,dc=org",
                "objectClass: top",
                "objectClass: person",
                "objectClass: organizationalPerson",
                "objectClass: inetOrgPerson",
                "cn: CommonName0002",
                "sn: SN0002",
                
                // Source entry 0004
                "dn: cn=CN0004,ou=L2Ldst,ou=Test Data,dc=lsc-project,dc=org",
                "objectClass: top",
                "objectClass: person",
                "objectClass: organizationalPerson",
                "objectClass: inetOrgPerson",
                "cn: CN0004",
                "sn: SN0002",
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
       name = "src-ldap",
       type = LdapConnectionType.class,
       id = "LdapSrc",
       url = "ldap://localhost:33389/dc=lsc-project,dc=org",
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
       id = "ldap2ldapOptimizeValueReplacementTestCreate",
       name = "ldap2ldapOptimizeValueReplacementTestCreate",
       // bean, default
       // cleanHook, empty
       // syncHook, empty
       ldapSourceService = 
           @CreateLdapSourceService(
               name = "ldap2ldapOptimizeValueReplacementTestCreate-src",
               connectionRef = "src-ldap",
               baseDn = "ou=L2Ldst,ou=Test Data,dc=lsc-project,dc=org",
               pivotAttributes = {"cn"},
               allFilter = "(CN=CN0001)",
               cleanFilter="(CN=CN0001)",
               dateFormat = "yyyyMMddHHmmss'Z'",
               interval = 5,
               fetchedAttributes = {
                   "cn"
               }),
       ldapDestinationService = 
           @CreateLdapDestinationService(
               name = "ldap2ldapOptimizeValueReplacementTestCreate-dst",
               connectionRef = "dst-ldap",
               baseDn = "ou=L2Ldst,ou=Test Data,dc=lsc-project,dc=org",
               pivotAttributes = {"cn"},
               allFilter = "(cn=optimizevaluereplacement)",
               fetchedAttributes = {
                   "description", 
                   "cn", 
                   "sn", 
                   "objectClass"
               }),
       propertiesBasedSyncOptions = 
           @CreatePropertiesBasedSyncOptions(
               mainIdentifier = "\"cn=optimizevaluereplacement,ou=L2Ldst,ou=Test Data,dc=lsc-project,dc=org\"",
               defaultDelimiter = ",",
               defaultPolicy = PolicyType.KEEP,
               //conditions = ''
               //hooks = "",
               dataset = {
                   // ObjectClass dataset
                   @CreateDataset(
                       name = "objectclass",
                       policy = PolicyType.FORCE,
                       forceValues = {
                           @CreateValuesType(string = {"\"inetOrgPerson\"","\"person\"","\"top\""})
                       }),
                   // CN dataset
                   @CreateDataset(
                       name = "cn",
                       policy = PolicyType.FORCE,
                       forceValues = {
                           @CreateValuesType(string = {"\"optimizevaluereplacement\""})
                       }),
                   // SN dataset
                   @CreateDataset(
                       name = "sn",
                       policy = PolicyType.FORCE,
                       forceValues = {
                           @CreateValuesType(string = {"\"optimizevaluereplacement\""})
                       }),
                   @CreateDataset(
                       name = "description",
                       policy = PolicyType.FORCE,
                       forceValues = {
                           @CreateValuesType(string = {
                               "\"value001\"", "\"value002\"", "\"value003\"", "\"value004\"", "\"value005\"",
                               "\"value006\"", "\"value007\"", "\"value008\"", "\"value009\"", "\"value010\"",
                               "\"value011\"", "\"value012\"", "\"value013\"", "\"value014\"", "\"value015\"",
                               "\"value016\"", "\"value017\"", "\"value018\"", "\"value019\"", "\"value020\"",
                               "\"value021\"", "\"value022\"", "\"value023\"", "\"value024\"", "\"value025\"",
                               "\"value026\"", "\"value027\"", "\"value028\"", "\"value029\"", "\"value030\"",
                               "\"value031\"", "\"value032\"", "\"value033\"", "\"value034\"", "\"value035\"",
                               "\"value036\"", "\"value037\"", "\"value038\"", "\"value039\"", "\"value040\"",
                               "\"value041\"", "\"value042\"", "\"value043\"", "\"value044\"", "\"value045\"",
                               "\"value046\"", "\"value047\"", "\"value048\"", "\"value049\"", "\"value050\"",
                               "\"value051\"", "\"value052\"", "\"value053\"", "\"value054\"", "\"value055\"",
                               "\"value056\"", "\"value057\"", "\"value058\"", "\"value059\"", "\"value060\"",
                               "\"value061\"", "\"value062\"", "\"value063\"", "\"value064\"", "\"value065\"",
                               "\"value066\"", "\"value067\"", "\"value068\"", "\"value069\"", "\"value070\"",
                               "\"value071\"", "\"value072\"", "\"value073\"", "\"value074\"", "\"value075\"",
                               "\"value076\"", "\"value077\"", "\"value078\"", "\"value079\"", "\"value080\"",
                               "\"value081\"", "\"value082\"", "\"value083\"", "\"value084\"", "\"value085\"",
                               "\"value086\"", "\"value087\"", "\"value088\"", "\"value089\"", "\"value090\"",
                               "\"value091\"", "\"value092\"", "\"value093\"", "\"value094\"", "\"value095\"",
                               "\"value096\"", "\"value097\"", "\"value098\"", "\"value099\"", "\"value100\""
                           })
                       })
               })
   ),
   @CreateTask(
        id = "ldap2ldapOptimizeValueReplacementTestUpdate1",
        name = "ldap2ldapOptimizeValueReplacementTestUpdate1",
        // bean, default
        // cleanHook, empty
        // syncHook, empty
        ldapSourceService = 
            @CreateLdapSourceService(
                name = "ldap2ldapOptimizeValueReplacementTestUpdate1-src",
                connectionRef = "src-ldap",
                baseDn = "ou=L2Ldst,ou=Test Data,dc=lsc-project,dc=org",
                pivotAttributes = {"cn"},
                allFilter = "(cn=optimizevaluereplacement)",
                cleanFilter="(cn=optimizevaluereplacement)",
                dateFormat = "yyyyMMddHHmmss'Z'",
                interval = 5,
                fetchedAttributes = {
                    "cn", "description"
                }),
        ldapDestinationService = 
            @CreateLdapDestinationService(
                name = "ldap2ldapOptimizeValueReplacementTestUpdate1-dst",
                connectionRef = "dst-ldap",
                baseDn = "ou=L2Ldst,ou=Test Data,dc=lsc-project,dc=org",
                pivotAttributes = {"cn"},
                allFilter = "(cn=optimizevaluereplacement)",
                fetchedAttributes = {
                    "description", 
                    "cn"
                }),
        propertiesBasedSyncOptions = 
            @CreatePropertiesBasedSyncOptions(
                mainIdentifier = "\"cn=optimizevaluereplacement,ou=L2Ldst,ou=Test Data,dc=lsc-project,dc=org\"",
                defaultDelimiter = ",",
                defaultPolicy = PolicyType.FORCE,
                //conditions = ''
                //hooks = "",
                dataset = {
                    // CN dataset
                    @CreateDataset(
                        name = "cn",
                        policy = PolicyType.FORCE,
                        forceValues = {
                            @CreateValuesType(string = {"\"optimizevaluereplacement\""})
                        }),
                    @CreateDataset(
                        name = "description",
                        policy = PolicyType.FORCE,
                        forceValues = {
                            @CreateValuesType(string = {
                                "\"value001\"", "\"value002\"", "\"value003\"", "\"value004\"", "\"value005\"",
                                "\"value006\"", "\"value007\"", "\"value008\"", "\"value009\"", "\"value010\"",
                                "\"value011\"", "\"value012\"", "\"value013\"", "\"value014\"", "\"value015\"",
                                "\"value016\"", "\"value017\"", "\"value018\"", "\"value019\"", "\"value020\"",
                                "\"value021\"", "\"value022\"", "\"value023\"", "\"value024\"", "\"value025\"",
                                "\"value026\"", "\"value027\"", "\"value028\"", "\"value029\"", "\"value030\"",
                                "\"value031\"", "\"value032\"", "\"value033\"", "\"value034\"", "\"value035\"",
                                "\"value036\"", "\"value037\"", "\"value038\"", "\"value039\"", "\"value040\"",
                                "\"value041\"", "\"value042\"", "\"value043\"", "\"value044\"", "\"value045\"",
                                "\"value046\"", "\"value047\"", "\"value048\"", "\"value049\"", "\"value050\"",
                                "\"value051\"", "\"value052\"", "\"value053\"", "\"value054\"", "\"value055\"",
                                "\"value056\"", "\"value057\"", "\"value058\"", "\"value059\"", "\"value060\"",
                                "\"value061\"", "\"value062\"", "\"value063\"", "\"value064\"", "\"value065\"",
                                "\"value066\"", "\"value067\"", "\"value068\"", "\"value069\"", "\"value070\"",
                                "\"value071\"", "\"value072\"", "\"value073\"", "\"value074\"", "\"value075\"",
                                "\"value076\"", "\"value077\"", "\"value078\"", "\"value079\"", "\"value080\"",
                                "\"value081\"", "\"value082\"", "\"value083\"", "\"value084\"", "\"value085\"",
                                "\"value086\"", "\"value087\"", "\"value088\"", "\"value089\"", "\"value090\"",
                                "\"value091\"", "\"value092\"", "\"value093\"", "\"value094\"", "\"value095\"",
                                "\"value096\"", "\"value097\"", "\"value098\"", "\"value099\"", 
                                // 1 new value (101), 1 value fewer (100)
                                "\"value101\""
                            })
                        })
                })
    ),
    @CreateTask(
         id = "ldap2ldapOptimizeValueReplacementTestUpdate2",
         name = "ldap2ldapOptimizeValueReplacementTestUpdate2",
         // bean, default
         // cleanHook, empty
         // syncHook, empty
         ldapSourceService = 
             @CreateLdapSourceService(
                 name = "ldap2ldapOptimizeValueReplacementTestUpdate2-src",
                 connectionRef = "src-ldap",
                 baseDn = "ou=L2Ldst,ou=Test Data,dc=lsc-project,dc=org",
                 pivotAttributes = {"cn"},
                 allFilter = "(cn=optimizevaluereplacement)",
                 cleanFilter="(cn=optimizevaluereplacement)",
                 dateFormat = "yyyyMMddHHmmss'Z'",
                 interval = 5,
                 fetchedAttributes = {
                     "cn", "description"
                 }),
         ldapDestinationService = 
             @CreateLdapDestinationService(
                 name = "ldap2ldapOptimizeValueReplacementTestUpdate2-dst",
                 connectionRef = "dst-ldap",
                 baseDn = "ou=L2Ldst,ou=Test Data,dc=lsc-project,dc=org",
                 pivotAttributes = {"cn"},
                 allFilter = "(cn=optimizevaluereplacement)",
                 fetchedAttributes = {
                     "description", 
                     "cn"
                 }),
         propertiesBasedSyncOptions = 
             @CreatePropertiesBasedSyncOptions(
                 mainIdentifier = "\"cn=optimizevaluereplacement,ou=L2Ldst,ou=Test Data,dc=lsc-project,dc=org\"",
                 defaultDelimiter = ",",
                 defaultPolicy = PolicyType.FORCE,
                 //conditions = ''
                 //hooks = "",
                 dataset = {
                     // CN dataset
                     @CreateDataset(
                         name = "cn",
                         policy = PolicyType.FORCE,
                         forceValues = {
                             @CreateValuesType(string = {"\"optimizevaluereplacement\""})
                         }),
                     @CreateDataset(
                         name = "description",
                         policy = PolicyType.FORCE,
                         forceValues = {
                             @CreateValuesType(string = {
                                 //  1 identical value (101), 99 new values (102 to 200)
                                 "\"value101\"", "\"value102\"", "\"value103\"", "\"value104\"", "\"value105\"",
                                 "\"value106\"", "\"value107\"", "\"value108\"", "\"value109\"", "\"value110\"",
                                 "\"value111\"", "\"value112\"", "\"value113\"", "\"value114\"", "\"value115\"",
                                 "\"value116\"", "\"value117\"", "\"value118\"", "\"value119\"", "\"value120\"",
                                 "\"value121\"", "\"value122\"", "\"value123\"", "\"value124\"", "\"value125\"",
                                 "\"value126\"", "\"value127\"", "\"value128\"", "\"value129\"", "\"value130\"",
                                 "\"value131\"", "\"value132\"", "\"value133\"", "\"value134\"", "\"value135\"",
                                 "\"value136\"", "\"value137\"", "\"value138\"", "\"value139\"", "\"value140\"",
                                 "\"value141\"", "\"value142\"", "\"value143\"", "\"value144\"", "\"value145\"",
                                 "\"value146\"", "\"value147\"", "\"value148\"", "\"value149\"", "\"value150\"",
                                 "\"value151\"", "\"value152\"", "\"value153\"", "\"value154\"", "\"value155\"",
                                 "\"value156\"", "\"value157\"", "\"value158\"", "\"value159\"", "\"value160\"",
                                 "\"value161\"", "\"value162\"", "\"value163\"", "\"value164\"", "\"value165\"",
                                 "\"value166\"", "\"value167\"", "\"value168\"", "\"value169\"", "\"value170\"",
                                 "\"value171\"", "\"value172\"", "\"value173\"", "\"value174\"", "\"value175\"",
                                 "\"value176\"", "\"value177\"", "\"value178\"", "\"value179\"", "\"value180\"",
                                 "\"value181\"", "\"value182\"", "\"value183\"", "\"value184\"", "\"value185\"",
                                 "\"value186\"", "\"value187\"", "\"value188\"", "\"value189\"", "\"value190\"",
                                 "\"value191\"", "\"value192\"", "\"value193\"", "\"value194\"", "\"value195\"",
                                 "\"value196\"", "\"value197\"", "\"value198\"", "\"value199\"", "\"value200\""
                             })
                         })
                 })
    )}
)
public class Ldap2LdapOptimizeValueReplacementTest extends CommonLdapSyncTest {
    /** The Ldap connection to the defined Ldap server */
    private LdapConnection connection;

	@BeforeEach
	public void setup()  throws LdapException {
        LscConfiguration.loadFromInstance(classLscInstance);
        LscConfiguration.getInstance();
        reloadJndiConnections();
        
        connection = IntegrationUtils.getAdminConnection( getService() );
	}

	@Test
	public final void testLdap2LdapOptimizeValueReplacementTest() throws Exception {
		launchSyncTask("ldap2ldapOptimizeValueReplacementTestCreate");

	      // check that the entry has been created with all 100 values for description
        Entry createdEntry = connection.lookup( 
                "cn=optimizevaluereplacement,ou=L2LDst,ou=Test Data,dc=lsc-project,dc=org" );

        // Check the CN and SN
        assertTrue(createdEntry.contains("cn", "optimizevaluereplacement"));
        assertTrue(createdEntry.contains("sn", "optimizevaluereplacement"));
        
        // Check that the 100 values have been created
        for (int i=1; i < 101; i++) {
            String value = "value" + String.format("%03d", i);
            assertTrue(createdEntry.contains("description", value));
        }

        // Modify the created entry
		launchSyncTask("ldap2ldapOptimizeValueReplacementTestUpdate1");
		
		// Check that value 100 has been deleted, and value 101 added
        Entry modifiedEntry = connection.lookup( 
                "cn=optimizevaluereplacement,ou=L2LDst,ou=Test Data,dc=lsc-project,dc=org" );

        // Check the CN and SN
        assertTrue(modifiedEntry.contains("cn", "optimizevaluereplacement"));
        assertTrue(modifiedEntry.contains("sn", "optimizevaluereplacement"));
        
        // Check that the 99 first values have been created
        for (int i=1; i <= 99; i++) {
            String value = "value" + String.format("%03d", i);
            assertTrue(modifiedEntry.contains("description", value));
        }
        
        // The modified entry should not have anymore the 100 value
        assertFalse(modifiedEntry.contains("description", "value100"));
        
        // The modified entry should have a new 101 value
        assertTrue(modifiedEntry.contains("description", "value101"));

        // Modify it again, replacing values from 000 to 099 plus 101
        // by values from 101 to 200
		launchSyncTask("ldap2ldapOptimizeValueReplacementTestUpdate2");

        Entry updatedEntry = connection.lookup( 
                "cn=optimizevaluereplacement,ou=L2LDst,ou=Test Data,dc=lsc-project,dc=org" );

        // Check the CN and SN
        assertTrue(updatedEntry.contains("cn", "optimizevaluereplacement"));
        assertTrue(updatedEntry.contains("sn", "optimizevaluereplacement"));
        
        // Check that the 99 first values have been removed
        for (int i=1; i < 100; i++) {
            String value = "value" + String.format("%03d", i);
            assertFalse(updatedEntry.contains("description", value));
        }
        
        // The modified entry should not have anymore the 100 value
        assertFalse(updatedEntry.contains("description", "value100"));
        
        // The modified entry should have new values from 101 to 200
        for ( int i = 101; i <=200; i++) {
            String value = "value" + String.format("%03d", i);
            assertTrue(updatedEntry.contains("description", value));

        }

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
