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
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith( { ApacheDSTestExtension.class } )
@CreateDS(
    name = "DSWithPartitionAndServer",
    loadedSchemas =
        {
            @LoadSchema(name = "other", enabled = true)
        },
    partitions =
        {
            @CreatePartition(
                name = "lsc-project",
                suffix = "dc=lsc-project,dc=org",
                contextEntry = @ContextEntry(
                    entryLdif =
                    "dn: dc=lsc-project,dc=org\n" +
                        "dc: lsc-project\n" +
                        "objectClass: top\n" +
                        "objectClass: domain\n\n"),
                indexes =
                    {
                        @CreateIndex(attribute = "objectClass"),
                        @CreateIndex(attribute = "dc"),
                        @CreateIndex(attribute = "ou")
                })
    })
@CreateLdapServer(
    allowAnonymousAccess = true, 
    transports =
        {
            @CreateTransport(protocol = "LDAP", port = 33389),
            @CreateTransport(protocol = "LDAPS", port = 33636)
    })
@ApplyLdifs(
        {
            // Entry # 0
            "dn: cn=Directory Manager,ou=system",
            "objectClass: person",
            "objectClass: top",
            "cn: Directory Manager",
            "description: Directory Manager",
            "sn: Directory Manager",
            "userpassword: secret"
        })
@ApplyLdifFiles({"lsc-schema.ldif","lsc-project.ldif"})
public class Ldap2LdapSamePivotConversionSyncTest extends Ldap2LdapBinaryPivotSyncTest {

	public static String SOURCE_DN = "ou=ldap2ldapSamePivotConversionTestTaskSrc,ou=Test Data,dc=lsc-project,dc=org";
	public static String DESTINATION_DN = "ou=ldap2ldapSamePivotConversionTestTaskDst,ou=Test Data,dc=lsc-project,dc=org";
	public static String TASK_NAME = "ldap2ldapSamePivotConversionTestTask";

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}
	
	@Override
	public String getSourceDn() {
		return SOURCE_DN;
	}
	
	@Override
	public String getDestinationDn() {
		return DESTINATION_DN;
	}
}
