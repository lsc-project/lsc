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
package org.lsc.integration;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.apache.directory.server.core.hash.SshaPasswordHashingInterceptor;
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
 * This test case attempts to reproduce a typical ldap2ldap setup via
 * SimpleSynchronize. This relies on settings in lsc.properties from the test
 * resources, and entries in the local OpenDS directory. testSync() performs a
 * synchronization between two branches of the local LDAP server, and should
 * perform 3 operations : 1 ADD, 1 MODRDN, 1 MODIFY. The
 * 
 * @author Jonathan Clarke &ltjonathan@phillipoux.net&gt;
 */
@ExtendWith({ LscTestExtension.class })

// Define the Directory Server
@CreateDS(name = "DSWithPartitionAndServer", loadedSchemas = {
        //@LoadSchema(name = "other", enabled = true),
        @LoadSchema(name = "inetOrgPerson", enabled = true) 
        }, 
        additionalInterceptors = {
                SshaPasswordHashingInterceptor.class 
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
        @CreateTransport(protocol = "LDAP", port = 33389),
        @CreateTransport(protocol = "LDAPS", port = 33636) 
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
    id = "L2Lconfig", 
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
            name = "L2Lconfig",
            // bean, default
            // cleanHook, empty
            // syncHook, empty
            ldapSourceService = 
                @CreateLdapSourceService(
                    name = "ldap-source",
                    connectionRef = "src-ldap",
                    baseDn = "ou=L2L001Src,ou=Test Data,dc=lsc-project,dc=org",
                    pivotAttributes = {"cn", "sn"},
                    allFilter = "\"(sn=*)\"",
                    oneFilter = "\"(sn={sn})\"",
                    fetchedAttributes = {
                        /*"description", 
                        "cn", 
                        "sn", 
                        "userPassword", 
                        "telephoneNumber", 
                        "seeAlso", 
                        "jpegPhoto",
                        "objectClass"*/
                        "*"
                    }),
            ldapDestinationService = 
                @CreateLdapDestinationService(
                    name = "ldap-destination",
                    connectionRef = "dst-ldap",
                    baseDn = "ou=L2L001Dst,ou=Test Data,dc=lsc-project,dc=org",
                    pivotAttributes = {"cn"},
                    allFilter = "\"(sn=*)\"",
                    oneFilter = "\"(sn={sn})\"",
                    fetchedAttributes = {
                        /*"description", 
                        "cn", 
                        "sn", 
                        "userPassword", 
                        "telephoneNumber", 
                        "seeAlso", 
                        "jpegPhoto",
                        "mail",
                        "objectClass"*/
                        "*"
                    }),
            // errorIfEmptySource, empty
            // errorIfEmptyDestination, empty
            propertiesBasedSyncOptions = 
                @CreatePropertiesBasedSyncOptions(
                    mainIdentifier = "\"cn=\" + srcBean.getDatasetFirstValueById(\"cn\") + \","
                            + "ou=L2L001Dst,ou=Test Data,dc=lsc-project,dc=org\"",
                    //pivotTransformation
                    //defaultDelimiter
                    defaultPolicy = PolicyType.FORCE,
                    //conditions = ''
                    //hooks = "",
                    dataset = {
                        // TelepĥoneNumber dataset
                        @CreateDataset(
                            name = "telephoneNumber",
                            policy = PolicyType.MERGE,
                            defaultValues = {
                                @CreateValuesType(string = {"\"123456\"","\"789987\""})
                            },
                            createValues = {
                                @CreateValuesType(string = {"\"000000","\"11111\""})
                            }),
                        // ObjectClass dataset
                        @CreateDataset(
                            name = "objectclass",
                            policy = PolicyType.MERGE,
                            defaultValues = {
                                @CreateValuesType(string = {"\"person\"","\"top\""})
                            },
                            createValues = {
                                @CreateValuesType(string = {"\"inetOrgPerson\""})
                            }),
                        // initials dataset
                        @CreateDataset(
                            name = "initials",
                            policy = PolicyType.FORCE,
                            createValues = {
                                @CreateValuesType(string = {"\"cn=oops\""})
                            }),
                        // description dataset
                        @CreateDataset(
                            name = "description",
                            policy = PolicyType.MERGE,
                            forceValues = {
                                @CreateValuesType(string = {
                                    "  var j=0;\n"
                                    + "var dstDescrValues = new Array();\n"
                                    + "var srcDescrValues = srcBean.getDatasetById(\"description\").toArray();\n"
                                    + "for (var i=0; i < srcDescrValues.length; i++ ) {\n"
                                    + "    if ( srcDescrValues[i] != null ) {\n"
                                    + "        // The sample just copy the value but you can do what you want here!\n"
                                    + "        // Just keep in mind to force a correct data type because the source "
                                    + "        // values are mapped to a generic Object type\n"
                                    + "        // which will not be well handled by the Javascript engine !\n"
                                    + "        dstDescrValues[j++] = \"modified: \" + srcDescrValues[i];\n"
                                    + "     }\n"
                                    + "}\n"
                                    + "dstDescrValues\n"
                                    + ""
                                })
                            }),
                        // seeAlso dataset
                        @CreateDataset(
                            name = "seealso",
                            policy = PolicyType.FORCE),
                        // default dataset
                        @CreateDataset(
                            name = "default",
                            policy = PolicyType.FORCE),
                        // default dataset
                        @CreateDataset(
                            name = "userPassword",
                            policy = PolicyType.FORCE,
                            forceValues = {
                                @CreateValuesType(string = {"\"secret\" + srcBean.getDatasetFirstValueById(\"cn\")"})
                            }),
                        // mail dataset
                        @CreateDataset(
                            name = "mail",
                            policy = PolicyType.FORCE,
                            forceValues = {
                                @CreateValuesType(string = {"\"ok@domain.net\""})
                            })
                    })
        )}
)

/**
 * a test 
 */
public class Ldap2LdapSingleEntryITTest extends CommonLdapSyncTest {
    private static final String TASK_NAME = "L2Lconfig";
    
    private static final String JPEG_PHOTO =
        "/9j/4AAQSkZJRgABAQEBLAEsAAD/4QdkRXhpZgAASUkqAAgAAAAFABoBBQABAAAASgAAABsBBQABAAAAUgAAACgBAwABAAAA"
        + "AgAAADEBAgAMAAAAWgAAADIBAgAUAAAAZgAAAHoAAAAsAQAAAQAAACwBAAABAAAAR0lNUCAyLjEwLjgAMjAxOTowMzowNSAxNzozMjo1Nw"
        + "AIAAABBAABAAAAAAEAAAEBBAABAAAAAAEAAAIBAwADAAAA4AAAAAMBAwABAAAABgAAAAYBAwABAAAABgAAABUBAwABAAAAAwAAAAECBAAB"
        + "AAAA5gAAAAICBAABAAAAdQYAAAAAAAAIAAgACAD/2P/gABBKRklGAAEBAAABAAEAAP/bAEMACAYGBwYFCAcHBwkJCAoMFA0MCwsMGRITDx"
        + "QdGh8eHRocHCAkLicgIiwjHBwoNyksMDE0NDQfJzk9ODI8LjM0Mv/bAEMBCQkJDAsMGA0NGDIhHCEyMjIyMjIyMjIyMjIyMjIyMjIyMjIy"
        + "MjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMv/AABEIAQABAAMBIgACEQEDEQH/xAAfAAABBQEBAQEBAQAAAAAAAAAAAQIDBAUGBwgJCg"
        + "v/xAC1EAACAQMDAgQDBQUEBAAAAX0BAgMABBEFEiExQQYTUWEHInEUMoGRoQgjQrHBFVLR8CQzYnKCCQoWFxgZGiUmJygpKjQ1Njc4OTpD"
        + "REVGR0hJSlNUVVZXWFlaY2RlZmdoaWpzdHV2d3h5eoOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1d"
        + "bX2Nna4eLj5OXm5+jp6vHy8/T19vf4+fr/xAAfAQADAQEBAQEBAQEBAAAAAAAAAQIDBAUGBwgJCgv/xAC1EQACAQIEBAMEBwUEBAABAncA"
        + "AQIDEQQFITEGEkFRB2FxEyIygQgUQpGhscEJIzNS8BVictEKFiQ04SXxFxgZGiYnKCkqNTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaG"
        + "lqc3R1dnd4eXqCg4SFhoeIiYqSk5SVlpeYmZqio6Slpqeoqaqys7S1tre4ubrCw8TFxsfIycrS09TV1tfY2dri4+Tl5ufo6ery8/T19vf4"
        + "+fr/2gAMAwEAAhEDEQA/AOfooor9MPnwooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKA"
        + "CiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAoooo"
        + "AKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiii"
        + "gAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKK"
        + "KACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAoo"
        + "ooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACi"
        + "iigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAK"
        + "KKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigA"
        + "ooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKA"
        + "CiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAoooo"
        + "AKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiii"
        + "gAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKK"
        + "KACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAoo"
        + "ooAKKKKACiiigD/9kA/9sAQwD/////////////////////////////////////////////////////////////////////////////////"
        + "/////9sAQwH//////////////////////////////////////////////////////////////////////////////////////8IAEQgAAQ"
        + "ABAwERAAIRAQMRAf/EABQAAQAAAAAAAAAAAAAAAAAAAAH/xAAUAQEAAAAAAAAAAAAAAAAAAAAB/9oADAMBAAIQAxAAAAET/8QAFBABAAAA"
        + "AAAAAAAAAAAAAAAAAP/aAAgBAQABBQJ//8QAFBEBAAAAAAAAAAAAAAAAAAAAAP/aAAgBAwEBPwF//8QAFBEBAAAAAAAAAAAAAAAAAAAAAP"
        + "/aAAgBAgEBPwF//8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQAGPwJ//8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPyF//9oA"
        + "DAMBAAIAAwAAABD/AP/EABQRAQAAAAAAAAAAAAAAAAAAAAD/2gAIAQMBAT8Qf//EABQRAQAAAAAAAAAAAAAAAAAAAAD/2gAIAQIBAT8Qf/"
        + "/EABQQAQAAAAAAAAAAAAAAAAAAAAD/2gAIAQEAAT8Qf//Z";

    // The Ldap connection to the defined Ldap server */
    private LdapConnection connection;
    
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
     * Synchronize the following branches:
     * 
     * <pre>
     * dc=lsc-project,dc=org
     *   ou=Test Data
     *       ou=L2L001Src
     *         cn=CN0001
     *           cn: CN0001
     *           sn: SN0001
     *           objectClass: inetOrgPerson
     *           objectClass: organizationalPerson
     *           objectClass: person
     *           objectClass: top
     *           userPassword: secret0001
     *           description: Number one's descriptive text
     *           jpegPhoto:: /9j/4AAQSkZJRgABAQEBLAEsAAD/4QdkRX...
     *       ou=L2L001Dst
     *           cn=CN0001
     *             cn: CN0001
     *             sn: SN0001
     *             objectClass: inetOrgPerson
     *             objectClass: organizationalPerson
     *             objectClass: person
     *             objectClass: top
     *             telephoneNumber: 123456
     *             telephoneNumber: 456789
     *             seeAlso: cn=CN0001,ou=ldap2ldap2TestTaskSrc,ou=Test Data,dc=lsc-project,dc=org
     *             description: Number one's descriptive text
     * </pre>
     * 
     * The expected result is that the DST server will contain:
     * 
     * <pre>
     * dc=lsc-project,dc=org
     *   ou=Test Data
     *       ou=L2L001Dst
     *           cn=CN0001
     *             cn: CN0001
     *             sn: SN0001
     *             objectClass: inetOrgPerson
     *             objectClass: organizationalPerson
     *             objectClass: person
     *             objectClass: top
     *             telephoneNumber: 123456
     *             telephoneNumber: 456789
     *             telephoneNumber: 789987
     *             seeAlso: cn=CN0001,ou=ldap2ldap2TestTaskSrc,ou=Test Data,dc=lsc-project,dc=org
     *             description: Number one's descriptive text
     *             description: modified: Number one's descriptive text
     *             jpegPhoto:: /9j/4AAQSkZJRgABAQEBLAEsAAD/4QdkRXhpZgAAS...
     *             userPassword:: e1NTSEF9TWpnLyt5ZGo2WnVCb0tYcy9ybU9CTUYydjNNM24yVjZsQjMwUHc9P
     *             mail: ok@domain.net
     * </pre>
     * 
     * <ul>
     *   <li>The telephoneNumber will have one value injected from the LSC configuration</li>
     *   <li>The description will have and added coputed value</li>
     *   <li>The jpegPhoto has been copied from the source</li>
     *   <li>The mail has been created from the LSC configuration</li>
     *   <li>The userPassword has been copied from the source</li>
     * </ul>
     */
    // Inject data into the server
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

            // Entry #1 from source
            "dn: cn=CN0001,ou=L2L001Src,ou=Test Data,dc=lsc-project,dc=org",
            "cn: CN0001",
            "sn: SN0001",
            "objectClass: inetOrgPerson",
            "objectClass: organizationalPerson",
            "objectClass: person",
            "objectClass: top",
            "userPassword: secret0001",
            "description: Number one's descriptive text",
            "jpegPhoto:: /9j/4AAQSkZJRgABAQEBLAEsAAD/4QdkRXhpZgAASUkqAAgAAAAFABoBBQABAAAASgAAABsBBQABAAAAUgAAACgBAwABAAAA"
            + "AgAAADEBAgAMAAAAWgAAADIBAgAUAAAAZgAAAHoAAAAsAQAAAQAAACwBAAABAAAAR0lNUCAyLjEwLjgAMjAxOTowMzowNSAxNzozMjo1Nw"
            + "AIAAABBAABAAAAAAEAAAEBBAABAAAAAAEAAAIBAwADAAAA4AAAAAMBAwABAAAABgAAAAYBAwABAAAABgAAABUBAwABAAAAAwAAAAECBAAB"
            + "AAAA5gAAAAICBAABAAAAdQYAAAAAAAAIAAgACAD/2P/gABBKRklGAAEBAAABAAEAAP/bAEMACAYGBwYFCAcHBwkJCAoMFA0MCwsMGRITDx"
            + "QdGh8eHRocHCAkLicgIiwjHBwoNyksMDE0NDQfJzk9ODI8LjM0Mv/bAEMBCQkJDAsMGA0NGDIhHCEyMjIyMjIyMjIyMjIyMjIyMjIyMjIy"
            + "MjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMv/AABEIAQABAAMBIgACEQEDEQH/xAAfAAABBQEBAQEBAQAAAAAAAAAAAQIDBAUGBwgJCg"
            + "v/xAC1EAACAQMDAgQDBQUEBAAAAX0BAgMABBEFEiExQQYTUWEHInEUMoGRoQgjQrHBFVLR8CQzYnKCCQoWFxgZGiUmJygpKjQ1Njc4OTpD"
            + "REVGR0hJSlNUVVZXWFlaY2RlZmdoaWpzdHV2d3h5eoOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1d"
            + "bX2Nna4eLj5OXm5+jp6vHy8/T19vf4+fr/xAAfAQADAQEBAQEBAQEBAAAAAAAAAQIDBAUGBwgJCgv/xAC1EQACAQIEBAMEBwUEBAABAncA"
            + "AQIDEQQFITEGEkFRB2FxEyIygQgUQpGhscEJIzNS8BVictEKFiQ04SXxFxgZGiYnKCkqNTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaG"
            + "lqc3R1dnd4eXqCg4SFhoeIiYqSk5SVlpeYmZqio6Slpqeoqaqys7S1tre4ubrCw8TFxsfIycrS09TV1tfY2dri4+Tl5ufo6ery8/T19vf4"
            + "+fr/2gAMAwEAAhEDEQA/AOfooor9MPnwooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKA"
            + "CiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAoooo"
            + "AKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiii"
            + "gAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKK"
            + "KACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAoo"
            + "ooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACi"
            + "iigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAK"
            + "KKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigA"
            + "ooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKA"
            + "CiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAoooo"
            + "AKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiii"
            + "gAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKK"
            + "KACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAoo"
            + "ooAKKKKACiiigD/9kA/9sAQwD/////////////////////////////////////////////////////////////////////////////////"
            + "/////9sAQwH//////////////////////////////////////////////////////////////////////////////////////8IAEQgAAQ"
            + "ABAwERAAIRAQMRAf/EABQAAQAAAAAAAAAAAAAAAAAAAAH/xAAUAQEAAAAAAAAAAAAAAAAAAAAB/9oADAMBAAIQAxAAAAET/8QAFBABAAAA"
            + "AAAAAAAAAAAAAAAAAP/aAAgBAQABBQJ//8QAFBEBAAAAAAAAAAAAAAAAAAAAAP/aAAgBAwEBPwF//8QAFBEBAAAAAAAAAAAAAAAAAAAAAP"
            + "/aAAgBAgEBPwF//8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQAGPwJ//8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPyF//9oA"
            + "DAMBAAIAAwAAABD/AP/EABQRAQAAAAAAAAAAAAAAAAAAAAD/2gAIAQMBAT8Qf//EABQRAQAAAAAAAAAAAAAAAAAAAAD/2gAIAQIBAT8Qf/"
            + "/EABQQAQAAAAAAAAAAAAAAAAAAAAD/2gAIAQEAAT8Qf//Z",

            // The destination branche
            "dn: ou=L2L001Dst,ou=Test Data,dc=lsc-project,dc=org",
            "ou: L2L001Dst",
            "objectClass: organizationalUnit",
            "objectClass: top",

            // Entry #1 from destination
            "dn: cn=CN0001,ou=L2L001Dst,ou=Test Data,dc=lsc-project,dc=org",
            "cn: CN0001",
            "sn: SN0001",
            "objectClass: inetOrgPerson",
            "objectClass: organizationalPerson",
            "objectClass: person",
            "objectClass: top",
            "telephoneNumber: 123456",
            "telephoneNumber: 456789",
            "seeAlso: cn=CN0001,ou=L2L001Src,ou=Test Data,dc=lsc-project,dc=org",
            "description: Number one's descriptive text"
    })
    @Test
    public final void testL2LFullEntry001() throws Exception {
        SimpleSynchronize sync = new SimpleSynchronize();
        List<String> syncType = new ArrayList<String>();
        
        syncType.add(TASK_NAME);

        boolean ret = sync.launch(null, syncType, null);
        assertTrue(ret);
        
        // Now check the result
        Entry sourceEntry = connection.lookup( "cn=CN0001,ou=L2L001Src,ou=Test Data,dc=lsc-project,dc=org");
        Entry destinationEntry = connection.lookup( "cn=CN0001,ou=L2L001Dst,ou=Test Data,dc=lsc-project,dc=org");
        
        Entry expectedResult = new DefaultEntry( connection.getSchemaManager(),
                "cn=CN0001,ou=L2L001Dst,ou=Test Data,dc=lsc-project,dc=org",
                "cn: CN0001",
                "sn: SN0001",
                "objectClass: inetOrgPerson",
                "objectClass: organizationalPerson",
                "objectClass: person",
                "objectClass: top",
                "telephoneNumber: 123456",
                "telephoneNumber: 456789",
                "telephoneNumber: 789987",
                "seeAlso: cn=CN0001,ou=L2L001Src,ou=Test Data,dc=lsc-project,dc=org",
                "description: Number one's descriptive text",
                "description: modified: Number one's descriptive text",
                "jpegPhoto:: " + JPEG_PHOTO,
                "userPassword: {SSHA}KejXqeVY6sxU0a6Um0R/EdVd9OAFDrp0XzePlw==",
                "mail: ok@domain.net"
                );
        
        // No ObjectClass expected change
        assertTrue(destinationEntry.contains("objectClass", "top", "person", "organizationalPerson", "inetOrgPerson"));
        
        // The mail attribute must have been added even if it didn't exist in source
        assertFalse(sourceEntry.containsAttribute("mail"));
        assertTrue(destinationEntry.contains("mail", "ok@domain.net"));
        
        // The description attribute must have 2 values
        assertTrue(destinationEntry.containsAttribute("description"));
        assertTrue(destinationEntry.get("description").size() == 2);
        
        // The TelephoneNumber must now contain 3 values, one defined through configuration
        assertTrue(destinationEntry.contains("telephoneNumber", "123456", "456789", "789987"));
        
        // The seeAlso attribute must have been added
        assertTrue(destinationEntry.containsAttribute("seeAlso"));
                
        // The JpegPhoto must have been added from source
        assertTrue(destinationEntry.containsAttribute("jpegPhoto"));
        
        // The userPassword attribute has been compied from source
        assertTrue(destinationEntry.containsAttribute("userPassword"));
        
        // Get rid of the userPassword, as it's salted
        destinationEntry.removeAttributes("userPassword");
        expectedResult.removeAttributes("userPassword");
        
        assertTrue(destinationEntry.equals(expectedResult));
    }
}

