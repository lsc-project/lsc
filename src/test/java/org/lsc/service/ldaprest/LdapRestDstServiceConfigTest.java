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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.lsc.configuration.LdapRestAuthType;
import org.lsc.configuration.LdapRestDestinationServiceType;
import org.lsc.configuration.TaskType;
import org.lsc.exception.LscServiceException;

class LdapRestDstServiceConfigTest {

    /**
     * Build a {@link LdapRestDestinationServiceType} programmatically
     * using the JAXB setters. This mimics what JAXB does at runtime
     * when parsing {@code lsc.xml}.
     */
    private LdapRestDestinationServiceType svc(String baseUrl, String resourceType,
                                               LdapRestAuthType auth) {
        LdapRestDestinationServiceType s = new LdapRestDestinationServiceType();
        s.setBaseUrl(baseUrl);
        s.setResourceType(resourceType);
        s.setAuth(auth);
        return s;
    }

    private LdapRestAuthType bearer(String token) {
        LdapRestAuthType a = new LdapRestAuthType();
        a.setBearer(token);
        return a;
    }

    private LdapRestAuthType hmac(String id, String secret) {
        LdapRestAuthType a = new LdapRestAuthType();
        a.setHmacServiceId(id);
        a.setHmacSecret(secret);
        return a;
    }

    @Test
    void parsesBaseUrlAndResourceTypeAndBearer() throws Exception {
        LdapRestDestinationServiceType s = svc("https://api.test/", "users", bearer("tok"));
        s.setTimeoutMs(2500L);
        s.setRetries(5);
        LdapRestDstService.Config cfg = LdapRestDstService.parseConfig(s);
        assertEquals("https://api.test/", cfg.baseUrl);
        assertEquals("users", cfg.resourceType);
        assertEquals(2500L, cfg.timeoutMs);
        assertEquals(5, cfg.retries);
        assertTrue(cfg.auth instanceof BearerAuth);
    }

    @Test
    void parsesHmacAuth() throws Exception {
        LdapRestDestinationServiceType s = svc("https://api.test", "groups",
                hmac("lsc", "secret-32-chars-min-aaaaaaaaaaaaa"));
        LdapRestDstService.Config cfg = LdapRestDstService.parseConfig(s);
        assertTrue(cfg.auth instanceof HmacAuth);
    }

    @Test
    void requiresBaseUrl() throws Exception {
        LdapRestDestinationServiceType s = svc(null, "users", bearer("t"));
        LscServiceException ex = assertThrows(LscServiceException.class,
                () -> LdapRestDstService.parseConfig(s));
        assertTrue(ex.getMessage().contains("baseUrl"));
    }

    @Test
    void requiresResourceType() throws Exception {
        LdapRestDestinationServiceType s = svc("https://api.test", null, bearer("t"));
        LscServiceException ex = assertThrows(LscServiceException.class,
                () -> LdapRestDstService.parseConfig(s));
        assertTrue(ex.getMessage().contains("resourceType"));
    }

    @Test
    void requiresAuth() throws Exception {
        LdapRestDestinationServiceType s = svc("https://api.test", "users", null);
        LscServiceException ex = assertThrows(LscServiceException.class,
                () -> LdapRestDstService.parseConfig(s));
        assertTrue(ex.getMessage().contains("auth"));
    }

    @Test
    void requiresAuthCredentials() throws Exception {
        // <auth/> present but empty
        LdapRestDestinationServiceType s = svc("https://api.test", "users", new LdapRestAuthType());
        LscServiceException ex = assertThrows(LscServiceException.class,
                () -> LdapRestDstService.parseConfig(s));
        assertTrue(ex.getMessage().contains("bearer") || ex.getMessage().contains("hmac"));
    }

    @Test
    void defaultsAreApplied() throws Exception {
        LdapRestDestinationServiceType s = svc("https://api.test", "users", bearer("t"));
        LdapRestDstService.Config cfg = LdapRestDstService.parseConfig(s);
        assertEquals(10_000L, cfg.timeoutMs);
        assertEquals(3, cfg.retries);
        assertEquals("/api", cfg.apiPrefix);
    }

    @Test
    void rejectsZeroOrNegativeTimeout() throws Exception {
        for (long bad : new long[] { 0L, -1L, -10000L }) {
            LdapRestDestinationServiceType s = svc("https://api.test", "users", bearer("t"));
            s.setTimeoutMs(bad);
            LscServiceException ex = assertThrows(LscServiceException.class,
                    () -> LdapRestDstService.parseConfig(s),
                    "expected rejection for timeoutMs=" + bad);
            assertTrue(ex.getMessage().contains("timeoutMs"),
                    "message should mention timeoutMs, got: " + ex.getMessage());
        }
    }

    @Test
    void rejectsNegativeRetries() throws Exception {
        LdapRestDestinationServiceType s = svc("https://api.test", "users", bearer("t"));
        s.setRetries(-1);
        LscServiceException ex = assertThrows(LscServiceException.class,
                () -> LdapRestDstService.parseConfig(s));
        assertTrue(ex.getMessage().contains("retries"),
                "message should mention retries, got: " + ex.getMessage());
    }

    @Test
    void acceptsZeroRetries() throws Exception {
        // 0 retries is valid (= no retry, single attempt)
        LdapRestDestinationServiceType s = svc("https://api.test", "users", bearer("t"));
        s.setRetries(0);
        LdapRestDstService.Config cfg = LdapRestDstService.parseConfig(s);
        assertEquals(0, cfg.retries);
    }

    @Test
    void normalisesApiPrefix() throws Exception {
        LdapRestDestinationServiceType s = svc("https://api.test", "users", bearer("t"));
        s.setApiPrefix("/api/");
        LdapRestDstService.Config cfg = LdapRestDstService.parseConfig(s);
        assertEquals("/api", cfg.apiPrefix);

        s.setApiPrefix("api");
        cfg = LdapRestDstService.parseConfig(s);
        assertEquals("/api", cfg.apiPrefix);

        s.setApiPrefix("/");
        cfg = LdapRestDstService.parseConfig(s);
        assertEquals("", cfg.apiPrefix);
    }

    @Test
    void parsesFromTaskTypeWrapper() throws Exception {
        LdapRestDestinationServiceType s = svc("https://api.test", "users", bearer("t"));
        TaskType task = new TaskType();
        task.setLdapRestDestinationService(s);
        LdapRestDstService.Config cfg = LdapRestDstService.parseConfig(task);
        assertEquals("https://api.test", cfg.baseUrl);
        assertEquals("users", cfg.resourceType);
    }

    @Test
    void taskWithoutLdapRestServiceIsRejected() {
        TaskType task = new TaskType();
        LscServiceException ex = assertThrows(LscServiceException.class,
                () -> LdapRestDstService.parseConfig(task));
        assertTrue(ex.getMessage().contains("ldapRestDestinationService")
                || ex.getMessage().contains("not configured"));
    }
}
