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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

/**
 * Routes LSC operations to the right ldap-rest endpoint depending on
 * the configured resource type, and parses LSC-style DNs into
 * identifiers usable on the wire.
 *
 * <p>Three resource families are supported:</p>
 * <ul>
 *     <li>{@code groups} — endpoints under {@code /groups}, identified
 *         by the bare {@code cn} value.</li>
 *     <li>{@code organizations} — endpoints under {@code /organizations},
 *         identified by the URL-encoded full DN.</li>
 *     <li>everything else (e.g. {@code users}) — flat resources where
 *         the identifier is the RDN value (e.g. {@code uid=alice}'s
 *         {@code alice}).</li>
 * </ul>
 */
public class ResourceMapper {

    public enum Family { GROUPS, ORGANIZATIONS, FLAT }

    private final String resourceType;
    private final Family family;
    private final String apiPrefix;

    public ResourceMapper(String resourceType) {
        this(resourceType, "/api");
    }

    public ResourceMapper(String resourceType, String apiPrefix) {
        this.resourceType = Objects.requireNonNull(resourceType, "resourceType").toLowerCase(Locale.ROOT);
        this.apiPrefix = Objects.requireNonNull(apiPrefix, "apiPrefix");
        switch (this.resourceType) {
            case "groups":
                this.family = Family.GROUPS;
                break;
            case "organizations":
                this.family = Family.ORGANIZATIONS;
                break;
            default:
                this.family = Family.FLAT;
        }
    }

    public String getResourceType() {
        return resourceType;
    }

    public Family getFamily() {
        return family;
    }

    /** {@code /api/v1/ldap/{resource}} — for CREATE. */
    public String collectionPath() {
        return apiPrefix + "/v1/ldap/" + resourceType;
    }

    /** {@code /api/v1/ldap/{resource}/{id}} — for UPDATE/DELETE. */
    public String itemPath(String dn) {
        return collectionPath() + "/" + encodeId(dn);
    }

    /** {@code /api/v1/ldap/{resource}/{id}/move} — for MODRDN flat/org. */
    public String movePath(String dn) {
        return itemPath(dn) + "/move";
    }

    /** {@code /api/v1/ldap/groups/{cn}/rename} — for MODRDN groups. */
    public String renamePath(String dn) {
        if (family != Family.GROUPS) {
            throw new IllegalStateException("rename is only valid for groups");
        }
        return itemPath(dn) + "/rename";
    }

    /** {@code /api/v1/ldap/groups/{cn}/members} — group member add. */
    public String membersPath(String dn) {
        if (family != Family.GROUPS) {
            throw new IllegalStateException("members is only valid for groups");
        }
        return itemPath(dn) + "/members";
    }

    /** {@code /api/v1/ldap/groups/{cn}/members/{member}} — group member delete. */
    public String memberItemPath(String groupDn, String memberDn) {
        if (family != Family.GROUPS) {
            throw new IllegalStateException("members is only valid for groups");
        }
        return membersPath(groupDn) + "/" + encode(memberDn);
    }

    /**
     * Build the URL-suitable identifier from a LSC main identifier.
     * For groups/flat resources we want the bare RDN value (so
     * {@code uid=alice,ou=users,dc=...} → {@code alice}). For
     * organizations we want the entire DN URL-encoded.
     */
    public String encodeId(String dn) {
        Objects.requireNonNull(dn, "dn");
        switch (family) {
            case ORGANIZATIONS:
                return encode(dn);
            case GROUPS:
            case FLAT:
            default:
                return encode(rdnValue(dn));
        }
    }

    /**
     * Extract the unescaped RDN value from a DN, parsed via
     * {@link javax.naming.ldap.LdapName} / {@link javax.naming.ldap.Rdn}
     * so all RFC 4514 / RFC 2253 escapes are handled correctly
     * ({@code \,}, {@code \\}, {@code \=}, hex {@code \xx}, leading/trailing
     * spaces, hex-string DER values, etc.).
     *
     * <p>Examples:</p>
     * <ul>
     *     <li>{@code uid=alice,ou=users,dc=ex,dc=org} → {@code alice}</li>
     *     <li>{@code cn=Doe\, John,ou=users,dc=ex,dc=org} → {@code Doe, John}</li>
     *     <li>{@code cn=Doe\2c John,ou=users,dc=ex,dc=org} → {@code Doe, John}</li>
     *     <li>{@code alice} → {@code alice} (bare value passthrough)</li>
     * </ul>
     *
     * <p>The result is the *logical* identifier value that ldap-rest
     * expects in path params (the server re-escapes it itself via
     * {@code escapeDnValue}). Returning the escaped form would
     * double-escape on the wire.</p>
     */
    public static String rdnValue(String dn) {
        Objects.requireNonNull(dn, "dn");
        String trimmed = dn.trim();
        if (trimmed.isEmpty()) return trimmed;
        try {
            LdapName name = new LdapName(trimmed);
            if (name.isEmpty()) return trimmed;
            Rdn rdn = name.getRdn(name.size() - 1);
            Object v = rdn.getValue();
            return v == null ? "" : v.toString();
        } catch (InvalidNameException e) {
            // Bare value (e.g. "alice") is not a valid DN — fall through
            // and return it as-is. Same for anything LdapName refuses.
            return trimmed;
        }
    }

    /**
     * Return the parent DN (everything but the leftmost RDN), or {@code ""}
     * if {@code dn} has no parent (single RDN or bare value). Uses
     * {@link LdapName} so escape rules are RFC 4514 compliant.
     */
    public static String parentDn(String dn) {
        Objects.requireNonNull(dn, "dn");
        String trimmed = dn.trim();
        if (trimmed.isEmpty()) return "";
        try {
            LdapName name = new LdapName(trimmed);
            if (name.size() <= 1) return "";
            return name.getPrefix(name.size() - 1).toString();
        } catch (InvalidNameException e) {
            return "";
        }
    }

    /**
     * Return the first RDN component (e.g. {@code uid=alice}) of a DN,
     * in its canonical RFC 4514 form. Used internally for diagnostics;
     * for the unescaped value alone use {@link #rdnValue(String)}.
     */
    public static String firstRdn(String dn) {
        Objects.requireNonNull(dn, "dn");
        String trimmed = dn.trim();
        if (trimmed.isEmpty()) return "";
        try {
            LdapName name = new LdapName(trimmed);
            if (name.isEmpty()) return trimmed;
            return name.getRdn(name.size() - 1).toString();
        } catch (InvalidNameException e) {
            return trimmed;
        }
    }

    static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8)
                // URLEncoder uses '+' for spaces; ldap-rest decodes
                // percent-encoded form but not application/x-www-form-urlencoded
                // for path components, so use %20 to be safe.
                .replace("+", "%20");
    }
}
