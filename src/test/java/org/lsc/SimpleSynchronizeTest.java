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
import static org.junit.jupiter.api.Assertions.assertFalse;

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
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.lsc.configuration.LscConfiguration;

/**
 * Tests for the SimpleSynchronize class.
 * 
 * Mostly sanity checks for the moment.
 * 
 * @author Jonathan Clarke &ltjonathan@phillipoux.net&gt;
 */
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
    transports =
        {
            @CreateTransport(protocol = "LDAP", port = 33389)
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
public class SimpleSynchronizeTest extends AbstractLdapTestUnit {

	@BeforeEach
	public void setup() {
		LscConfiguration.reset();
	}
	
	@Test
	public void testParameters() throws Exception {
		SimpleSynchronize sync = new SimpleSynchronize();
		
		List<String> asyncTasks = new ArrayList<String>();
		List<String> syncTasks = new ArrayList<String>();
		List<String> cleanTasks = new ArrayList<String>();

		assertFalse(sync.launch(asyncTasks, syncTasks, cleanTasks));

		syncTasks.add("nonexistent");
		
		assertFalse(sync.launch(asyncTasks, syncTasks, cleanTasks));
	}
}
