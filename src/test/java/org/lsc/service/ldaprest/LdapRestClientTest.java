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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lsc.exception.LscServiceException;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

class LdapRestClientTest {

    private WireMockServer server;
    private String baseUrl;

    @BeforeEach
    void start() {
        server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        server.start();
        baseUrl = "http://127.0.0.1:" + server.port();
    }

    @AfterEach
    void stop() {
        if (server != null) server.stop();
    }

    @Test
    void postSendsBodyAndAuthHeader() throws Exception {
        server.stubFor(post(urlEqualTo("/api/v1/ldap/users"))
                .willReturn(aResponse().withStatus(201).withBody("{\"ok\":true}")));
        LdapRestClient c = new LdapRestClient(baseUrl, new BearerAuth("tok"), 5000, 0);
        HttpResponse<String> r = c.post("/api/v1/ldap/users", "{\"uid\":\"alice\"}");
        assertEquals(201, r.statusCode());
        server.verify(postRequestedFor(urlEqualTo("/api/v1/ldap/users"))
                .withRequestBody(equalToJson("{\"uid\":\"alice\"}"))
                .withHeader("Authorization", equalTo("Bearer tok"))
                .withHeader("Content-Type", equalTo("application/json")));
    }

    @Test
    void putSendsBody() throws Exception {
        server.stubFor(put(urlEqualTo("/api/v1/ldap/users/alice"))
                .willReturn(aResponse().withStatus(200).withBody("{}")));
        LdapRestClient c = new LdapRestClient(baseUrl, new BearerAuth("t"), 5000, 0);
        HttpResponse<String> r = c.put("/api/v1/ldap/users/alice", "{\"replace\":{\"sn\":[\"X\"]}}");
        assertEquals(200, r.statusCode());
    }

    @Test
    void deleteWithoutBody() throws Exception {
        server.stubFor(delete(urlEqualTo("/api/v1/ldap/users/alice"))
                .willReturn(aResponse().withStatus(200)));
        LdapRestClient c = new LdapRestClient(baseUrl, new BearerAuth("t"), 5000, 0);
        HttpResponse<String> r = c.delete("/api/v1/ldap/users/alice");
        assertEquals(200, r.statusCode());
    }

    @Test
    void getWorks() throws Exception {
        server.stubFor(get(urlEqualTo("/api/v1/ldap/users"))
                .willReturn(aResponse().withStatus(200).withBody("[]")));
        LdapRestClient c = new LdapRestClient(baseUrl, new BearerAuth("t"), 5000, 0);
        HttpResponse<String> r = c.get("/api/v1/ldap/users");
        assertEquals(200, r.statusCode());
    }

    @Test
    void retriesOn5xxThenSucceeds() throws Exception {
        server.stubFor(post(urlEqualTo("/api/v1/ldap/users"))
                .inScenario("retry")
                .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("after1"));
        server.stubFor(post(urlEqualTo("/api/v1/ldap/users"))
                .inScenario("retry")
                .whenScenarioStateIs("after1")
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("after2"));
        server.stubFor(post(urlEqualTo("/api/v1/ldap/users"))
                .inScenario("retry")
                .whenScenarioStateIs("after2")
                .willReturn(aResponse().withStatus(201).withBody("{\"ok\":true}")));

        LdapRestClient c = new LdapRestClient(baseUrl, new BearerAuth("t"), 5000, 3,
                HttpClient.newHttpClient(), ms -> { /* no-op */ });
        HttpResponse<String> r = c.post("/api/v1/ldap/users", "{}");
        assertEquals(201, r.statusCode());
        // 3 attempts total
        assertEquals(3, server.getAllServeEvents().size());
    }

    @Test
    void retriesExhaustedOn5xxThrows() {
        server.stubFor(post(urlEqualTo("/api/v1/ldap/users"))
                .willReturn(aResponse().withStatus(500)));
        LdapRestClient c = new LdapRestClient(baseUrl, new BearerAuth("t"), 5000, 2,
                HttpClient.newHttpClient(), ms -> { /* no-op */ });
        LscServiceException ex = assertThrows(LscServiceException.class,
                () -> c.post("/api/v1/ldap/users", "{}"));
        assertTrue(ex.getMessage().contains("HTTP 500"));
    }

    @Test
    void non2xxNon5xxFailsImmediately() {
        server.stubFor(post(urlEqualTo("/api/v1/ldap/users"))
                .willReturn(aResponse().withStatus(400).withBody("{\"error\":\"bad\"}")));
        LdapRestClient c = new LdapRestClient(baseUrl, new BearerAuth("t"), 5000, 3,
                HttpClient.newHttpClient(), ms -> { /* no-op */ });
        LscServiceException ex = assertThrows(LscServiceException.class,
                () -> c.post("/api/v1/ldap/users", "{}"));
        assertTrue(ex.getMessage().contains("HTTP 400"));
        // No retries: only 1 server call
        assertEquals(1, server.getAllServeEvents().size());
    }

    @Test
    void timeoutCausesException() throws Exception {
        server.stubFor(post(urlEqualTo("/slow"))
                .willReturn(aResponse().withStatus(200).withFixedDelay(2000)));
        LdapRestClient c = new LdapRestClient(baseUrl, new BearerAuth("t"), 200, 0,
                HttpClient.newHttpClient(), ms -> { /* no-op */ });
        assertThrows(LscServiceException.class, () -> c.post("/slow", "{}"));
    }

    @Test
    void hmacAuthHeaderIsPresent() throws Exception {
        server.stubFor(post(urlEqualTo("/api/v1/ldap/users"))
                .willReturn(aResponse().withStatus(201)));
        LdapRestClient c = new LdapRestClient(baseUrl,
                new HmacAuth("svc", "very-long-secret-32-chars-min-xxx", () -> 1234L),
                5000, 0);
        c.post("/api/v1/ldap/users", "{\"uid\":\"x\"}");
        server.verify(postRequestedFor(urlEqualTo("/api/v1/ldap/users"))
                .withHeader("Authorization",
                        equalTo("HMAC-SHA256 svc:1234:" + HmacAuth.computeSignature(
                                "very-long-secret-32-chars-min-xxx",
                                "POST", "/api/v1/ldap/users", 1234L,
                                "{\"uid\":\"x\"}".getBytes(StandardCharsets.UTF_8)))));
    }

    @Test
    void retriesOnIoExceptionThenThrows() {
        // Use an unbound port to force connection refused / IOException
        int port;
        try (java.net.ServerSocket s = new java.net.ServerSocket(0)) {
            port = s.getLocalPort();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        // socket closed → port unbound
        LdapRestClient c = new LdapRestClient("http://127.0.0.1:" + port,
                new BearerAuth("t"), 1000, 1, HttpClient.newHttpClient(), ms -> { /* no-op */ });
        assertThrows(LscServiceException.class, () -> c.post("/x", "{}"));
    }

    @Test
    void baseUrlTrailingSlashIsStripped() throws Exception {
        server.stubFor(get(urlEqualTo("/x")).willReturn(aResponse().withStatus(200)));
        LdapRestClient c = new LdapRestClient(baseUrl + "/", new BearerAuth("t"), 5000, 0);
        HttpResponse<String> r = c.get("/x");
        assertEquals(200, r.statusCode());
    }
}
