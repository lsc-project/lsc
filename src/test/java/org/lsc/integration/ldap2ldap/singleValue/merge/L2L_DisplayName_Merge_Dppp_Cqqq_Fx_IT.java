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
package org.lsc.integration.ldap2ldap.singleValue.merge;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.directory.api.ldap.model.entry.DefaultEntry;
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
import org.lsc.CommonLdapSyncTest;
import org.lsc.SimpleSynchronize;
import org.lsc.configuration.LdapConnectionType;
import org.lsc.configuration.LscConfiguration;
import org.lsc.configuration.PolicyType;
import org.lsc.utils.annotations.CreateLSC;
import org.lsc.utils.annotations.CreateTask;
import org.lsc.utils.annotations.CreateValuesType;
import org.lsc.utils.annotations.CreateConnection;
import org.lsc.utils.annotations.CreateDataset;
import org.lsc.utils.annotations.CreateLdapDestinationService;
import org.lsc.utils.annotations.CreateLdapSourceService;
import org.lsc.utils.annotations.CreatePropertiesBasedSyncOptions;
import org.lsc.utils.annotations.LscTestExtension;

/**
 * This test case will check that a LDAP to LDAP synchronization
 * for a single value attribute with a MERGE policy works for the 
 * various use cases :
 * 
 * <pre>
 * +--------+--------+---------+--------+-------+----------+
 * |  src   |  dst   | default | create | force | expected |
 * +--------+--------+---------+--------+-------+----------+
 * | absent | absent |   ppp   |  qqq   | ///// |   ppp    |
 * |  aaa   | absent |   ppp   |  qqq   | ///// |  absent  |
 * | absent |  bbb   |   ppp   |  qqq   | ///// |   bbb    |
 * |  ccc   |  ccc   |   ppp   |  qqq   | ///// |   ccc    |
 * |  ddd   |  eee   |   ppp   |  qqq   | ///// |   eee    |
 * | absent |  N/A   |   ppp   |  qqq   | ///// |   qqq    |
 * |  aaa   |  N/A   |   ppp   |  qqq   | ///// |   ///    |
 * +--------+--------+---------+--------+-------+----------+
 * </pre>
 */
@ExtendWith({ LscTestExtension.class })

// Define the Directory Server
@CreateDS(name = "DSWithPartitionAndServer", loadedSchemas = {
        @LoadSchema(name = "inetOrgPerson", enabled = true) 
        }, 
        partitions = {
                @CreatePartition(
                        name = "lsc-project", 
                        suffix = "dc=lsc-project,dc=org", 
                        contextEntry = @ContextEntry(entryLdif = 
                            "dn: dc=lsc-project,dc=org\n" +
                            "dc: lsc-project\n" + 
                            "objectClass: top\n" + 
                            "objectClass: domain\n\n"), 
                        indexes = {
                                @CreateIndex(attribute = "objectClass"), 
                                @CreateIndex(attribute = "dc"),
                                @CreateIndex(attribute = "ou")
                }) 
        })

// And the LdapServer
@CreateLdapServer(allowAnonymousAccess = true, transports = { 
        @CreateTransport(protocol = "LDAP", port = 33389) 
        })

// And the schema to use
@ApplyLdifFiles({ "lsc-schema.ldif" })

/**
 * The data structure is the following:
 * 
 * <pre>
 * dc=lsc-project,dc=org
 *   ou=Test Data
 *     ou=L2L001Src
 *     ou=L2L001Dst
 * </pre>
 */

// Create the LSC instance
@CreateLSC(
    id = "L2L_DisplayName_Merge_Dppp_Cqqq_Fx_IT", 
    revision = 1, 
    connections = {
        // Source
        @CreateConnection(
            username = "uid=admin,ou=system",
            type = LdapConnectionType.class,
            id = "LdapSrc",
            name = "src-ldap",
            url = "ldap://localhost:33389",
            password = "secret"
        ),
        // Destination
        @CreateConnection(
            type = LdapConnectionType.class,
            id = "LdapDest",
            name = "dst-ldap",
            url = "ldap://localhost:33389",
            username = "uid=admin,ou=system",
            password = "secret",
            relaxRules = true
        )
    },
    tasks = {
        @CreateTask(
            id = "taskTst",
            name = "L2L_DisplayName_Merge_Dppp_Cqqq_Fx_IT",
            ldapSourceService = 
                @CreateLdapSourceService(
                    name = "ldap-source",
                    connectionRef = "src-ldap",
                    baseDn = "ou=L2L001Src,ou=Test Data,dc=lsc-project,dc=org",
                    pivotAttributes = {"cn", "sn"},
                    allFilter = "(sn=*)",
                    oneFilter = "(sn={sn})",
                    fetchedAttributes = {"*"}),
            ldapDestinationService = 
                @CreateLdapDestinationService(
                    name = "ldap-destination",
                    connectionRef = "dst-ldap",
                    baseDn = "ou=L2L001Dst,ou=Test Data,dc=lsc-project,dc=org",
                    pivotAttributes = {"cn"},
                    allFilter = "(sn=*)",
                    oneFilter = "(sn={sn})",
                    fetchedAttributes = {"*"}),
            propertiesBasedSyncOptions = 
                @CreatePropertiesBasedSyncOptions(
                    mainIdentifier = "\"cn=\" + srcBean.getDatasetFirstValueById(\"cn\") + \","
                            + "ou=L2L001Dst,ou=Test Data,dc=lsc-project,dc=org\"",
                    defaultPolicy = PolicyType.FORCE,
                    dataset = {
                        // displayName dataset
                        @CreateDataset(
                            name = "displayName",
                            policy = PolicyType.MERGE,
                            defaultValues = {
                                @CreateValuesType(string = {"\"ppp\""})
                            },
                            createValues = {
                                @CreateValuesType(string = {"\"qqq\""})
                            }
                        )
                    })
        )}
)

@ApplyLdifs({
    // Entry # 0
    "dn: cn=Directory Manager,ou=system", 
    "objectClass: person", 
    "objectClass: top", 
    "cn: Directory Manager",
    "description: Directory Manager", 
    "sn: Directory Manager", 
    "userpassword: secret",
    
    // The Test Data branch
    "dn: ou=Test Data,dc=lsc-project,dc=org",
    "ou: Test Data",
    "objectClass: organizationalUnit",
    "objectClass: top",

    // The source branch
    "dn: ou=L2L001Src,ou=Test Data,dc=lsc-project,dc=org",
    "ou: L2L001Src",
    "objectClass: organizationalUnit",
    "objectClass: top",

    // The destination branche
    "dn: ou=L2L001Dst,ou=Test Data,dc=lsc-project,dc=org",
    "ou: L2L001Dst",
    "objectClass: organizationalUnit",
    "objectClass: top"
})
public class L2L_DisplayName_Merge_Dppp_Cqqq_Fx_IT extends CommonLdapSyncTest {
    /** The Ldap connection to the defined Ldap server */
    private LdapConnection connection;
    
    /** The task name */
    private static final String TASK_NAME = "L2L_DisplayName_Merge_Dppp_Cqqq_Fx_IT";

    /**
     * First initialize LSC
     * 
     * @throws LdapException
     */
    @BeforeEach
    public void setUp() throws LdapException {
        LscConfiguration.loadFromInstance(classLscInstance);
        LscConfiguration.getInstance();
        reloadJndiConnections();
        
        connection = IntegrationUtils.getAdminConnection( getService() );
    }
    
    /**
     * Source and destination don't have a SV displayName.
     * Policy is Merge
     * There is a default (ppp), a create (qqq) and no force value
     * 
     * <ul>
     *   <li>The displayName attribute will be added wit the value ppp</li>
     * </ul>
     */
    // Inject data into the server
    @ApplyLdifs({
            // Entry #1 from source
            "dn: cn=CN0001,ou=L2L001Src,ou=Test Data,dc=lsc-project,dc=org",
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "objectClass: inetOrgPerson",
            "cn: CN0001",
            "sn: SN0001",

            // Entry #1 from destination
            "dn: cn=CN0001,ou=L2L001Dst,ou=Test Data,dc=lsc-project,dc=org",
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "objectClass: inetOrgPerson",
            "cn: CN0001",
            "sn: SN0001",
    })
    @Test
    public final void testL2L_Merge_Absent_Absent_Dppp_Cqqq_Fx() throws Exception {
        SimpleSynchronize sync = new SimpleSynchronize();
        List<String> syncType = new ArrayList<String>();
        
        syncType.add(TASK_NAME);

        boolean ret = sync.launch(null, syncType, null);
        assertTrue(ret);
        
        // Now check the result
        //Entry sourceEntry = connection.lookup( "cn=CN0001,ou=L2L001Src,ou=Test Data,dc=lsc-project,dc=org");
        Entry destinationEntry = connection.lookup( "cn=CN0001,ou=L2L001Dst,ou=Test Data,dc=lsc-project,dc=org");
        
        Entry expectedResult = new DefaultEntry( connection.getSchemaManager(),
                "cn=CN0001,ou=L2L001Dst,ou=Test Data,dc=lsc-project,dc=org",
                "cn: CN0001",
                "sn: SN0001",
                "objectClass: top",
                "objectClass: person",
                "objectClass: organizationalPerson",
                "objectClass: inetOrgPerson",
                "displayName: ppp"
                );
        
        assertNotNull(destinationEntry);
        assertTrue(destinationEntry.contains( "displayName", "ppp"));
        assertTrue(destinationEntry.equals(expectedResult));
    }
    
    /**
     * Source has a displayName, and destination hasn't.
     * Policy is Merge
     * There is a default (ppp), a create (qqq) and no force value
     * 
     * <ul>
     *   <li>The destination will not have any displayName attribute</li>
     * </ul>
     */
    // Inject data into the server
    @ApplyLdifs({
            // Entry #1 from source
            "dn: cn=CN0002,ou=L2L001Src,ou=Test Data,dc=lsc-project,dc=org",
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "objectClass: inetOrgPerson",
            "cn: CN0002",
            "sn: SN0002",
            "displayName: aaa",

            // Entry #1 from destination
            "dn: cn=CN0002,ou=L2L001Dst,ou=Test Data,dc=lsc-project,dc=org",
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "objectClass: inetOrgPerson",
            "cn: CN0002",
            "sn: SN0002",
    })
    @Test
    public final void testL2L_Merge_aaa_Absent_Dppp_Cqqq_Fx() throws Exception {
        SimpleSynchronize sync = new SimpleSynchronize();
        List<String> syncType = new ArrayList<String>();
        
        syncType.add(TASK_NAME);

        boolean ret = sync.launch(null, syncType, null);
        assertTrue(ret);
        
        // Now check the result
        Entry destinationEntry = connection.lookup( "cn=CN0002,ou=L2L001Dst,ou=Test Data,dc=lsc-project,dc=org");
        
        Entry expectedResult = new DefaultEntry( connection.getSchemaManager(),
                "cn=CN0002,ou=L2L001Dst,ou=Test Data,dc=lsc-project,dc=org",
                "cn: CN0002",
                "sn: SN0002",
                "objectClass: top",
                "objectClass: person",
                "objectClass: organizationalPerson",
                "objectClass: inetOrgPerson"
                );
        
        assertNotNull(destinationEntry);
        assertFalse(destinationEntry.containsAttribute( "displayName"));
        assertTrue(expectedResult.equals(destinationEntry));
    }
    
    /**
     * Destination has a displayName, and source hasn't.
     * Policy is Merge
     * There is a default (ppp), a create (qqq) and no force value
     * 
     * <ul>
     *   <li>The destination displayName attribute will be present with value bbb</li>
     * </ul>
     */
    // Inject data into the server
    @ApplyLdifs({
            // Entry #1 from source
            "dn: cn=CN0003,ou=L2L001Src,ou=Test Data,dc=lsc-project,dc=org",
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "objectClass: inetOrgPerson",
            "cn: CN0003",
            "sn: SN0003",

            // Entry #1 from destination
            "dn: cn=CN0003,ou=L2L001Dst,ou=Test Data,dc=lsc-project,dc=org",
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "objectClass: inetOrgPerson",
            "cn: CN0003",
            "sn: SN0003",
            "displayName: bbb"
    })
    @Test
    public final void testL2L_Merge_Absent_bbb_Dppp_Cqqq_Fx() throws Exception {
        SimpleSynchronize sync = new SimpleSynchronize();
        List<String> syncType = new ArrayList<String>();
        
        syncType.add(TASK_NAME);

        boolean ret = sync.launch(null, syncType, null);
        assertTrue(ret);
        
        // Now check the result
        Entry destinationEntry = connection.lookup( "cn=CN0003,ou=L2L001Dst,ou=Test Data,dc=lsc-project,dc=org");
        
        Entry expectedResult = new DefaultEntry( connection.getSchemaManager(),
                "cn=CN0003,ou=L2L001Dst,ou=Test Data,dc=lsc-project,dc=org",
                "cn: CN0003",
                "sn: SN0003",
                "objectClass: top",
                "objectClass: person",
                "objectClass: organizationalPerson",
                "objectClass: inetOrgPerson",
                "displayName: bbb"
                );
        
        assertNotNull(destinationEntry);
        
        // The destination value should remain
        assertTrue(destinationEntry.contains( "displayName", "bbb"));
        assertFalse(destinationEntry.contains( "displayName", "ppp"));
        
        assertTrue(expectedResult.equals(destinationEntry));
    }
    
    /**
     * Source and destination have the same displayName
     * Policy is Merge
     * There is a default (ppp), a create (qqq) and no force value
     * 
     * <ul>
     *   <li>The destination displayName attribute will be present with value ccc</li>
     * </ul>
     */
    // Inject data into the server
    @ApplyLdifs({
            // Entry #1 from source
            "dn: cn=CN0004,ou=L2L001Src,ou=Test Data,dc=lsc-project,dc=org",
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "objectClass: inetOrgPerson",
            "cn: CN0004",
            "sn: SN0004",
            "displayName: ccc",

            // Entry #1 from destination
            "dn: cn=CN0004,ou=L2L001Dst,ou=Test Data,dc=lsc-project,dc=org",
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "objectClass: inetOrgPerson",
            "cn: CN0004",
            "sn: SN0004",
            "displayName: ccc"
    })
    @Test
    public final void testL2L_Merge_ccc_ccc_Dppp_Cqqq_Fx() throws Exception {
        SimpleSynchronize sync = new SimpleSynchronize();
        List<String> syncType = new ArrayList<String>();
        
        syncType.add(TASK_NAME);

        boolean ret = sync.launch(null, syncType, null);
        assertTrue(ret);
        
        // Now check the result
        Entry destinationEntry = connection.lookup( "cn=CN0004,ou=L2L001Dst,ou=Test Data,dc=lsc-project,dc=org");
        
        Entry expectedResult = new DefaultEntry( connection.getSchemaManager(),
                "cn=CN0004,ou=L2L001Dst,ou=Test Data,dc=lsc-project,dc=org",
                "cn: CN0004",
                "sn: SN0004",
                "objectClass: top",
                "objectClass: person",
                "objectClass: organizationalPerson",
                "objectClass: inetOrgPerson",
                "displayName: ccc"
                );
        
        assertNotNull(destinationEntry);
        assertTrue(destinationEntry.contains( "displayName", "ccc"));
        assertTrue(expectedResult.equals(destinationEntry));
    }
    
    /**
     * Source and destination have different displayName
     * Policy is Merge
     * There is a default (ppp), a create (qqq) and no force value
     * 
     * <ul>
     *   <li>The destination displayName attribute will be present with value eee</li>
     * </ul>
     */
    // Inject data into the server
    @ApplyLdifs({
            // Entry #1 from source
            "dn: cn=CN0005,ou=L2L001Src,ou=Test Data,dc=lsc-project,dc=org",
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "objectClass: inetOrgPerson",
            "cn: CN0005",
            "sn: SN0005",
            "displayName: ddd",

            // Entry #1 from destination
            "dn: cn=CN0005,ou=L2L001Dst,ou=Test Data,dc=lsc-project,dc=org",
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "objectClass: inetOrgPerson",
            "cn: CN0005",
            "sn: SN0005",
            "displayName: eee"
    })
    @Test
    public final void testL2L_Merge_ddd_eee_Dppp_Cqqq_Fx() throws Exception {
        SimpleSynchronize sync = new SimpleSynchronize();
        List<String> syncType = new ArrayList<String>();
        
        syncType.add(TASK_NAME);

        boolean ret = sync.launch(null, syncType, null);
        assertTrue(ret);
        
        // Now check the result
        Entry destinationEntry = connection.lookup( "cn=CN0005,ou=L2L001Dst,ou=Test Data,dc=lsc-project,dc=org");
        
        Entry expectedResult = new DefaultEntry( connection.getSchemaManager(),
                "cn=CN0005,ou=L2L001Dst,ou=Test Data,dc=lsc-project,dc=org",
                "cn: CN0005",
                "sn: SN0005",
                "objectClass: top",
                "objectClass: person",
                "objectClass: organizationalPerson",
                "objectClass: inetOrgPerson",
                "displayName: eee"
                );
        
        assertNotNull(destinationEntry);
        assertTrue(destinationEntry.contains( "displayName", "eee"));
        assertTrue(expectedResult.equals(destinationEntry));
    }
    
    /**
     * There is no destination, source has a displayName attribute
     * Policy is Merge
     * There is a default (ppp), a create (qqq) and no force value
     * 
     * <ul>
     *   <li>The destination should be absent, we can't create an entry with a SV Attribute with 2 values</li>
     * </ul>
     */
    // Inject data into the server
    @ApplyLdifs({
            // Entry #1 from source
            "dn: cn=CN0006,ou=L2L001Src,ou=Test Data,dc=lsc-project,dc=org",
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "objectClass: inetOrgPerson",
            "cn: CN0006",
            "sn: SN0006",
            "displayName: aaa"
    })
    @Test
    public final void testL2L_Merge_aaa_create_Dppp_Cqqq_Fx() throws Exception {
        SimpleSynchronize sync = new SimpleSynchronize();
        List<String> syncType = new ArrayList<String>();
        
        syncType.add(TASK_NAME);

        boolean ret = sync.launch(null, syncType, null);
        assertTrue(ret);
        
        // Now check the result
        Entry destinationEntry = connection.lookup( "cn=CN0006,ou=L2L001Dst,ou=Test Data,dc=lsc-project,dc=org");
        
        assertNull(destinationEntry);
    }
    
    /**
     * There is no destination, and a source with no displayName attribute
     * Policy is Merge
     * There is a default (ppp), a create (qqq) and no force value
     * 
     * <ul>
     *   <li>The destination displayName attribute will be created with value qqq</li>
     * </ul>
     */
    // Inject data into the server
    @ApplyLdifs({
            // Entry #1 from source
            "dn: cn=CN0007,ou=L2L001Src,ou=Test Data,dc=lsc-project,dc=org",
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "objectClass: inetOrgPerson",
            "cn: CN0007",
            "sn: SN0007"
    })
    @Test
    public final void testL2L_Merge_absent_create_Dppp_Cqqq_Fx() throws Exception {
        SimpleSynchronize sync = new SimpleSynchronize();
        List<String> syncType = new ArrayList<String>();
        
        syncType.add(TASK_NAME);

        boolean ret = sync.launch(null, syncType, null);
        assertTrue(ret);
        
        // Now check the result
        Entry destinationEntry = connection.lookup( "cn=CN0007,ou=L2L001Dst,ou=Test Data,dc=lsc-project,dc=org");
        
        Entry expectedResult = new DefaultEntry( connection.getSchemaManager(),
                "cn=CN0007,ou=L2L001Dst,ou=Test Data,dc=lsc-project,dc=org",
                "cn: CN0007",
                "sn: SN0007",
                "objectClass: top",
                "objectClass: person",
                "objectClass: organizationalPerson",
                "objectClass: inetOrgPerson",
                "displayName: qqq"
                );
        
        assertNotNull(destinationEntry);
        assertTrue(destinationEntry.contains( "displayName", "qqq"));
        assertTrue(expectedResult.equals(destinationEntry));
    }
}

