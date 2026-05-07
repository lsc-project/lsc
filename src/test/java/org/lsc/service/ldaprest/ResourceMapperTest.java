/*
 ****************************************************************************
 * Ldap Synchronization Connector provides tools to synchronize
 * electronic identities from a list of data sources including
 * any database with a JDBC connector, another LDAP directory,
 * flat files...
 *
 *                  ==LICENSE NOTICE==
 *
 * Copyright (c) 2008 - 2026 LSC Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
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
 ****************************************************************************
 */
package org.lsc.service.ldaprest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ResourceMapperTest {

    @Test
    void rdnValueExtractsSimpleUid() {
        assertEquals("alice", ResourceMapper.rdnValue("uid=alice,ou=users,dc=ex,dc=org"));
    }

    @Test
    void rdnValueExtractsCn() {
        assertEquals("admins", ResourceMapper.rdnValue("cn=admins,ou=groups,dc=ex,dc=org"));
    }

    @Test
    void rdnValueOnBareValue() {
        assertEquals("alice", ResourceMapper.rdnValue("alice"));
    }

    @Test
    void rdnValueUnescapesRfc4514EscapedComma() {
        // ldap-rest expects the unescaped logical value in :id path params;
        // the server re-escapes it via escapeDnValue. Returning the still-
        // escaped form would double-escape on the wire.
        assertEquals("Doe, John",
                ResourceMapper.rdnValue("cn=Doe\\, John,ou=users,dc=ex,dc=org"));
    }

    @Test
    void rdnValueUnescapesHexEscape() {
        // \2c is an RFC 4514 hex escape for ','
        assertEquals("Doe, John",
                ResourceMapper.rdnValue("cn=Doe\\2c John,ou=users,dc=ex,dc=org"));
    }

    @Test
    void rdnValueUnescapesBackslash() {
        assertEquals("a\\b", ResourceMapper.rdnValue("cn=a\\\\b,ou=x"));
    }

    @Test
    void rdnValueUnescapesPlus() {
        // \+ is the RFC 4514 escape for '+' (which separates multi-valued RDNs)
        assertEquals("a+b", ResourceMapper.rdnValue("cn=a\\+b,ou=x"));
    }

    @Test
    void rdnValueUnescapesEquals() {
        assertEquals("foo=bar", ResourceMapper.rdnValue("cn=foo\\=bar,ou=x"));
    }

    @Test
    void rdnValueExtractsLeftmostFromMultiValuedRdn() {
        // Multi-valued RDN: "cn=A+sn=B,...". LdapName parses both as one
        // RDN; we extract the value picked up by Rdn.getValue() (the
        // first attribute defined in the RDN).
        String got = ResourceMapper.rdnValue("cn=Smith+sn=John,ou=users,dc=ex,dc=org");
        // Either "Smith" or "John" depending on JDK ordering; both are
        // legitimate logical identifiers for this entry.
        org.junit.jupiter.api.Assertions.assertTrue(
                "Smith".equals(got) || "John".equals(got),
                "expected Smith or John, got: " + got);
    }

    @Test
    void parentDnReturnsTail() {
        assertEquals("ou=users,dc=ex,dc=org",
                ResourceMapper.parentDn("uid=alice,ou=users,dc=ex,dc=org"));
    }

    @Test
    void parentDnEmptyForRdnOnly() {
        assertEquals("", ResourceMapper.parentDn("uid=alice"));
    }

    @Test
    void firstRdnHandlesEscapedComma() {
        assertEquals("cn=Doe\\, John",
                ResourceMapper.firstRdn("cn=Doe\\, John,ou=users,dc=ex,dc=org"));
    }

    @Test
    void collectionPathFlat() {
        ResourceMapper m = new ResourceMapper("users");
        assertEquals("/api/v1/ldap/users", m.collectionPath());
    }

    @Test
    void itemPathFlatUsesRdnValue() {
        ResourceMapper m = new ResourceMapper("users");
        assertEquals("/api/v1/ldap/users/alice",
                m.itemPath("uid=alice,ou=users,dc=ex,dc=org"));
    }

    @Test
    void itemPathOrgUsesEncodedDn() {
        ResourceMapper m = new ResourceMapper("organizations");
        String got = m.itemPath("ou=sales,dc=ex,dc=org");
        // commas and equals are URL-encoded
        assertEquals("/api/v1/ldap/organizations/ou%3Dsales%2Cdc%3Dex%2Cdc%3Dorg", got);
    }

    @Test
    void movePathFlat() {
        ResourceMapper m = new ResourceMapper("users");
        assertEquals("/api/v1/ldap/users/alice/move",
                m.movePath("uid=alice,ou=users"));
    }

    @Test
    void renamePathOnlyForGroups() {
        ResourceMapper g = new ResourceMapper("groups");
        assertEquals("/api/v1/ldap/groups/admins/rename",
                g.renamePath("cn=admins,ou=groups"));
        ResourceMapper u = new ResourceMapper("users");
        assertThrows(IllegalStateException.class, () -> u.renamePath("uid=x"));
    }

    @Test
    void membersPath() {
        ResourceMapper g = new ResourceMapper("groups");
        assertEquals("/api/v1/ldap/groups/admins/members",
                g.membersPath("cn=admins,ou=groups"));
    }

    @Test
    void memberItemPathEncodesMemberDn() {
        ResourceMapper g = new ResourceMapper("groups");
        String p = g.memberItemPath("cn=admins,ou=groups", "uid=alice,ou=users,dc=ex,dc=org");
        assertEquals(
                "/api/v1/ldap/groups/admins/members/uid%3Dalice%2Cou%3Dusers%2Cdc%3Dex%2Cdc%3Dorg",
                p);
    }

    @Test
    void resourceTypeIsCaseInsensitive() {
        ResourceMapper m = new ResourceMapper("Groups");
        assertEquals(ResourceMapper.Family.GROUPS, m.getFamily());
    }
}
