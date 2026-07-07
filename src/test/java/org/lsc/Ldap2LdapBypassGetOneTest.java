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

 *    * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of the LSC Project nor the names of its
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
 * This test case verify the usage of bypassGetOne feature
 */
@ExtendWith({ LscTestExtension.class })
@CreateDS(
    name = "DSWithPartitionAndServer",
loadedSchemas = {
    @LoadSchema(name = "other", enabled = true),
    @LoadSchema(name = "inetOrgPerson", enabled = true)
},
additionalInterceptors = {
    SshaPasswordHashingInterceptor.class
},
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

    "dn: uid=test1,OU=source,OU=Test Data,DC=lsc-project,DC=org",
    "objectClass: top",
    "objectClass: person",
    "objectClass: organizationalPerson",
    "objectClass: inetOrgPerson",
    "cn: test1",
    "sn: test1",
    "uid: test1",
    "description: test1 entry",

    "dn: uid=test2,ou=source,ou=Test Data,dc=lsc-project,dc=org",
    "objectClass: top",
    "objectClass: person",
    "objectClass: organizationalPerson",
    "objectClass: inetOrgPerson",
    "cn: test2",
    "sn: test2",
    "uid: test2",
    "description: test2 entry",

    "dn: uid=test3,ou=source,ou=Test Data,dc=lsc-project,dc=org",
    "objectClass: top",
    "objectClass: person",
    "objectClass: organizationalPerson",
    "objectClass: inetOrgPerson",
    "cn: test3",
    "sn: test3",
    "uid: test3",
    "description: test3 entry",

    "dn: uid=test4,ou=source,ou=Test Data,dc=lsc-project,dc=org",
    "objectClass: top",
    "objectClass: person",
    "objectClass: organizationalPerson",
    "objectClass: inetOrgPerson",
    "cn: test4",
    "sn: test4",
    "uid: test4",
    "description: test4 entry",

    // Destination data
    "dn: cn=destination,ou=Test Data,dc=lsc-project,dc=org",
    "objectClass: top",
    "objectClass: organizationalRole",
    "cn: destination",

    "dn: uid=test1,cn=destination,ou=Test Data,dc=lsc-project,dc=org",
    "objectClass: top",
    "objectClass: person",
    "objectClass: organizationalPerson",
    "objectClass: inetOrgPerson",
    "cn: test1",
    "sn: test1",
    "uid: test1",

    "dn: uid=test2,cn=destination,ou=Test Data,dc=lsc-project,dc=org",
    "objectClass: top",
    "objectClass: person",
    "objectClass: organizationalPerson",
    "objectClass: inetOrgPerson",
    "cn: test2",
    "sn: test2",
    "uid: test2",

    "dn: uid=test3,cn=destination,ou=Test Data,dc=lsc-project,dc=org",
    "objectClass: top",
    "objectClass: person",
    "objectClass: organizationalPerson",
    "objectClass: inetOrgPerson",
    "cn: test3",
    "sn: test3",
    "uid: test3"

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
            id = "L2LTestTaskSync",
            name = "L2LTestTaskSync",
            ldapSourceService =
            @CreateLdapSourceService(
                name = "ldap-source",
                connectionRef = "src-ldap",
                baseDn = "ou=source,ou=Test Data,dc=lsc-project,dc=org",
                pivotAttributes = {"uid"},
                // The allFilter select the 4 entries from source
                allEntriesFilter = "\"(uid=*)\"",
                // the oneEntry filter, which should not be used, only gets 3 entries
                oneEntryFilter = "\"(&(uid=\" + pivotAttributes['uid'] + \")(|(uid=test1)(uid=test2)(uid=test3)))\"",
                cleanEntryFilter = "\"(uid=\" + pivotAttributes['uid'] + \")\"",
                bypassOneEntry = true,
                fetchedAttributes = {
                                        "cn",
                                        "description",
                                        "sn",
                                        "uid"
                                    }
            ),
            ldapDestinationService =
            @CreateLdapDestinationService(
                name = "ldap-destination",
                connectionRef = "dst-ldap",
                baseDn = "cn=destination,ou=Test Data,dc=lsc-project,dc=org",
                pivotAttributes = {"uid"},
                allEntriesFilter = "\"(uid=*)\"",
                oneEntryFilter = "\"(uid=\" + pivotAttributes['uid'] + \")\"",
                fetchedAttributes = {
                                        "cn",
                                        "description",
                                        "objectClass",
                                        "sn",
                                        "uid"
                }
            ),
            propertiesBasedSyncOptions =
            @CreatePropertiesBasedSyncOptions(
                // The main identifier will be the destination DN we want to operate on
                // Here, in source the entry's RDN uses an uid
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
                "\"uid=\" + uid + \",\" + suffix;",
                defaultDelimiter = ",",
                defaultPolicy = PolicyType.FORCE,
                dataset = {
                    @CreateDataset(
                        name = "cn",
                        policy = PolicyType.FORCE,
                        forceValues = {
                            @CreateValuesType(string = {"srcBean.getDatasetFirstBinaryValueById(\"cn\")"})
                        }),
                    @CreateDataset(
                        name = "sn",
                        policy = PolicyType.FORCE,
                        forceValues = {
                            @CreateValuesType(string = {"srcBean.getDatasetFirstBinaryValueById(\"sn\")"})
                        }),
                    @CreateDataset(
                        name = "objectClass",
                        policy = PolicyType.FORCE,
                        forceValues = {
                            @CreateValuesType(string = {"\"top\"", "\"person\"", "\"organizationalPerson\"", "\"inetOrgPerson\""})
                        }),
                    @CreateDataset(
                        name = "description",
                        policy = PolicyType.FORCE,
                        forceValues = {
                            @CreateValuesType(string = {"srcBean.getDatasetFirstBinaryValueById(\"description\")"})
                        }
                    )
                }
            )
        )
    }
)

public class Ldap2LdapBypassGetOneTest extends CommonLdapSyncTest {
    public final static String TASK_NAME = "L2LTestTaskSync";

    /** The Ldap connection to the defined Ldap server */
    private LdapConnection connection;

    // A few definition overloading those from the parent class
    private final static String DESTINATION_DN = "cn=destination,ou=Test Data,dc=lsc-project,dc=org";

    private final static String DN_TEST1_DST = "uid=test1," + DESTINATION_DN;
    private final static String DN_TEST2_DST = "uid=test2," + DESTINATION_DN;
    private final static String DN_TEST3_DST = "uid=test3," + DESTINATION_DN;
    private final static String DN_TEST4_DST = "uid=test4," + DESTINATION_DN;

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
     * Test that the entries have been created in destination
     */
    @Test
    public final void testSyncBypassGetOne() throws Exception {
        // Launch the create task
        launchTask(TASK_NAME, TaskType.SYNC);

        // check the result on sync
        reloadJndiConnections();

        // test1 should have been updated with description
        assertTrue(dstJndiServices.exists(DN_TEST1_DST));
        Entry test1Entry = connection.lookup(DN_TEST1_DST);
        assertTrue(test1Entry.containsAttribute( "description"));

        // test2 should have been updated with description
        assertTrue(dstJndiServices.exists(DN_TEST2_DST));
        Entry test2Entry = connection.lookup(DN_TEST2_DST);
        assertTrue(test2Entry.containsAttribute( "description"));

        // test3 should have been updated with description
        assertTrue(dstJndiServices.exists(DN_TEST3_DST));
        Entry test3Entry = connection.lookup(DN_TEST3_DST);
        assertTrue(test3Entry.containsAttribute( "description"));

        // test4 should have been created if the oneFilter step was bypassed
        assertTrue(dstJndiServices.exists(DN_TEST4_DST));
        Entry test4Entry = connection.lookup(DN_TEST4_DST);
        assertTrue(test4Entry.containsAttribute( "description"));

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

