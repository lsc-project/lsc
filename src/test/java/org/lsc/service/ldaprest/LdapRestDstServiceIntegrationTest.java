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
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lsc.LscDatasetModification;
import org.lsc.LscDatasetModification.LscDatasetModificationType;
import org.lsc.LscModificationType;
import org.lsc.LscModifications;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

class LdapRestDstServiceIntegrationTest {

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

    private LdapRestDstService usersService() {
        ResourceMapper mapper = new ResourceMapper("users");
        LdapRestClient client = new LdapRestClient(baseUrl, new BearerAuth("t"), 5000, 0);
        return new LdapRestDstService(client, mapper);
    }

    private LdapRestDstService groupsService() {
        ResourceMapper mapper = new ResourceMapper("groups");
        LdapRestClient client = new LdapRestClient(baseUrl, new BearerAuth("t"), 5000, 0);
        return new LdapRestDstService(client, mapper);
    }

    private LdapRestDstService orgsService() {
        ResourceMapper mapper = new ResourceMapper("organizations");
        LdapRestClient client = new LdapRestClient(baseUrl, new BearerAuth("t"), 5000, 0);
        return new LdapRestDstService(client, mapper);
    }

    private LscModifications lm(LscModificationType op, String main) {
        LscModifications m = new LscModifications(op, "task");
        m.setMainIdentifer(main);
        return m;
    }

    private LscDatasetModification rep(String name, Object... vals) {
        return new LscDatasetModification(LscDatasetModificationType.REPLACE_VALUES,
                name, Arrays.asList(vals));
    }

    private LscDatasetModification add(String name, Object... vals) {
        return new LscDatasetModification(LscDatasetModificationType.ADD_VALUES,
                name, Arrays.asList(vals));
    }

    private LscDatasetModification del(String name, Object... vals) {
        return new LscDatasetModification(LscDatasetModificationType.DELETE_VALUES,
                name, Arrays.asList(vals));
    }

    @Test
    void createUserPostsCollection() throws Exception {
        server.stubFor(post(urlEqualTo("/api/v1/ldap/users"))
                .willReturn(aResponse().withStatus(201).withBody("{}")));
        LscModifications m = lm(LscModificationType.CREATE_OBJECT, "uid=alice,ou=users,dc=ex,dc=org");
        m.setLscAttributeModifications(Arrays.asList(
                rep("uid", "alice"),
                rep("sn", "Doe"),
                rep("cn", "Alice Doe")));
        assertTrue(usersService().apply(m));
        server.verify(postRequestedFor(urlEqualTo("/api/v1/ldap/users"))
                .withRequestBody(equalToJson("{\"uid\":\"alice\",\"sn\":\"Doe\",\"cn\":\"Alice Doe\"}"))
                .withHeader("Authorization", equalTo("Bearer t")));
    }

    @Test
    void updateUserPutsItem() throws Exception {
        server.stubFor(put(urlEqualTo("/api/v1/ldap/users/alice"))
                .willReturn(aResponse().withStatus(200).withBody("{}")));
        LscModifications m = lm(LscModificationType.UPDATE_OBJECT, "uid=alice,ou=users,dc=ex,dc=org");
        m.setLscAttributeModifications(Arrays.asList(
                rep("sn", "NewName"),
                add("mail", "a@x.fr"),
                del("description")));
        assertTrue(usersService().apply(m));
        server.verify(putRequestedFor(urlEqualTo("/api/v1/ldap/users/alice"))
                .withRequestBody(equalToJson(
                        "{\"replace\":{\"sn\":[\"NewName\"]},\"add\":{\"mail\":[\"a@x.fr\"]},\"delete\":[\"description\"]}")));
    }

    @Test
    void deleteUser() throws Exception {
        server.stubFor(delete(urlEqualTo("/api/v1/ldap/users/alice"))
                .willReturn(aResponse().withStatus(200)));
        LscModifications m = lm(LscModificationType.DELETE_OBJECT, "uid=alice,ou=users,dc=ex,dc=org");
        assertTrue(usersService().apply(m));
        server.verify(deleteRequestedFor(urlEqualTo("/api/v1/ldap/users/alice")));
    }

    @Test
    void deleteUser404IsTreatedAsSuccess() throws Exception {
        server.stubFor(delete(urlEqualTo("/api/v1/ldap/users/ghost"))
                .willReturn(aResponse().withStatus(404).withBody("{\"error\":\"not found\"}")));
        LscModifications m = lm(LscModificationType.DELETE_OBJECT, "uid=ghost,ou=users,dc=ex,dc=org");
        assertTrue(usersService().apply(m));
    }

    @Test
    void modrdnUserCallsMove() throws Exception {
        server.stubFor(post(urlEqualTo("/api/v1/ldap/users/alice/move"))
                .willReturn(aResponse().withStatus(200)));
        LscModifications m = lm(LscModificationType.CHANGE_ID, "uid=alice,ou=users,dc=ex,dc=org");
        m.setNewMainIdentifier("uid=alice,ou=admins,dc=ex,dc=org");
        assertTrue(usersService().apply(m));
        server.verify(postRequestedFor(urlEqualTo("/api/v1/ldap/users/alice/move"))
                .withRequestBody(equalToJson("{\"targetOrgDn\":\"ou=admins,dc=ex,dc=org\"}")));
    }

    @Test
    void createGroupPostsCollection() throws Exception {
        server.stubFor(post(urlEqualTo("/api/v1/ldap/groups"))
                .willReturn(aResponse().withStatus(201)));
        LscModifications m = lm(LscModificationType.CREATE_OBJECT, "cn=admins,ou=groups,dc=ex,dc=org");
        m.setLscAttributeModifications(Arrays.asList(
                rep("cn", "admins"),
                rep("description", "Admins"),
                rep("member", "uid=alice,ou=users,dc=ex,dc=org")));
        assertTrue(groupsService().apply(m));
        server.verify(postRequestedFor(urlEqualTo("/api/v1/ldap/groups"))
                .withRequestBody(equalToJson(
                        "{\"cn\":\"admins\",\"description\":\"Admins\",\"member\":\"uid=alice,ou=users,dc=ex,dc=org\"}")));
    }

    @Test
    void updateGroupAddMemberUsesMembersEndpoint() throws Exception {
        server.stubFor(post(urlEqualTo("/api/v1/ldap/groups/admins/members"))
                .willReturn(aResponse().withStatus(200)));
        LscModifications m = lm(LscModificationType.UPDATE_OBJECT, "cn=admins,ou=groups,dc=ex,dc=org");
        m.setLscAttributeModifications(Collections.singletonList(
                add("member", "uid=alice,ou=users,dc=ex,dc=org")));
        assertTrue(groupsService().apply(m));
        server.verify(postRequestedFor(urlEqualTo("/api/v1/ldap/groups/admins/members"))
                .withRequestBody(equalToJson(
                        "{\"member\":\"uid=alice,ou=users,dc=ex,dc=org\"}")));
    }

    @Test
    void updateGroupDeleteMemberUsesMembersEndpoint() throws Exception {
        server.stubFor(delete(urlEqualTo("/api/v1/ldap/groups/admins/members/uid%3Dalice%2Cou%3Dusers%2Cdc%3Dex%2Cdc%3Dorg"))
                .willReturn(aResponse().withStatus(200)));
        LscModifications m = lm(LscModificationType.UPDATE_OBJECT, "cn=admins,ou=groups,dc=ex,dc=org");
        m.setLscAttributeModifications(Collections.singletonList(
                del("member", "uid=alice,ou=users,dc=ex,dc=org")));
        assertTrue(groupsService().apply(m));
    }

    @Test
    void updateGroupAttributeWideDeleteOnMemberFetchesAndRemovesAll() throws Exception {
        // Reconcile: GET the group, parse "member" array, fire one DELETE per member.
        server.stubFor(get(urlEqualTo("/api/v1/ldap/groups/admins"))
                .willReturn(aResponse().withStatus(200).withBody(
                        "{\"cn\":\"admins\",\"member\":["
                        + "\"uid=alice,ou=users,dc=ex,dc=org\","
                        + "\"uid=bob,ou=users,dc=ex,dc=org\"]}")));
        server.stubFor(delete(urlEqualTo("/api/v1/ldap/groups/admins/members/uid%3Dalice%2Cou%3Dusers%2Cdc%3Dex%2Cdc%3Dorg"))
                .willReturn(aResponse().withStatus(200)));
        server.stubFor(delete(urlEqualTo("/api/v1/ldap/groups/admins/members/uid%3Dbob%2Cou%3Dusers%2Cdc%3Dex%2Cdc%3Dorg"))
                .willReturn(aResponse().withStatus(200)));

        LscModifications m = lm(LscModificationType.UPDATE_OBJECT, "cn=admins,ou=groups,dc=ex,dc=org");
        // attribute-wide DELETE: empty values list
        m.setLscAttributeModifications(Collections.singletonList(
                new LscDatasetModification(LscDatasetModificationType.DELETE_VALUES,
                        "member", Collections.emptyList())));
        assertTrue(groupsService().apply(m));

        server.verify(deleteRequestedFor(urlEqualTo(
                "/api/v1/ldap/groups/admins/members/uid%3Dalice%2Cou%3Dusers%2Cdc%3Dex%2Cdc%3Dorg")));
        server.verify(deleteRequestedFor(urlEqualTo(
                "/api/v1/ldap/groups/admins/members/uid%3Dbob%2Cou%3Dusers%2Cdc%3Dex%2Cdc%3Dorg")));
    }

    @Test
    void updateGroupNonMemberAttributeUsesPut() throws Exception {
        server.stubFor(put(urlEqualTo("/api/v1/ldap/groups/admins"))
                .willReturn(aResponse().withStatus(200)));
        LscModifications m = lm(LscModificationType.UPDATE_OBJECT, "cn=admins,ou=groups,dc=ex,dc=org");
        m.setLscAttributeModifications(Collections.singletonList(
                rep("description", "New Description")));
        assertTrue(groupsService().apply(m));
        server.verify(putRequestedFor(urlEqualTo("/api/v1/ldap/groups/admins"))
                .withRequestBody(equalToJson(
                        "{\"replace\":{\"description\":[\"New Description\"]}}")));
    }

    @Test
    void deleteGroup() throws Exception {
        server.stubFor(delete(urlEqualTo("/api/v1/ldap/groups/admins"))
                .willReturn(aResponse().withStatus(200)));
        LscModifications m = lm(LscModificationType.DELETE_OBJECT, "cn=admins,ou=groups,dc=ex,dc=org");
        assertTrue(groupsService().apply(m));
    }

    @Test
    void renameGroupUsesRenameEndpoint() throws Exception {
        server.stubFor(post(urlEqualTo("/api/v1/ldap/groups/oldcn/rename"))
                .willReturn(aResponse().withStatus(200)));
        LscModifications m = lm(LscModificationType.CHANGE_ID, "cn=oldcn,ou=groups,dc=ex,dc=org");
        m.setNewMainIdentifier("cn=newcn,ou=groups,dc=ex,dc=org");
        assertTrue(groupsService().apply(m));
        server.verify(postRequestedFor(urlEqualTo("/api/v1/ldap/groups/oldcn/rename"))
                .withRequestBody(equalToJson("{\"newCn\":\"newcn\"}")));
    }

    @Test
    void createOrganizationPostsCollection() throws Exception {
        server.stubFor(post(urlEqualTo("/api/v1/ldap/organizations"))
                .willReturn(aResponse().withStatus(201)));
        LscModifications m = lm(LscModificationType.CREATE_OBJECT, "ou=sales,dc=ex,dc=org");
        m.setLscAttributeModifications(Arrays.asList(
                rep("ou", "sales"),
                rep("description", "Sales")));
        assertTrue(orgsService().apply(m));
        server.verify(postRequestedFor(urlEqualTo("/api/v1/ldap/organizations"))
                .withRequestBody(equalToJson("{\"ou\":\"sales\",\"description\":\"Sales\"}")));
    }

    @Test
    void deleteOrganizationUsesEncodedDn() throws Exception {
        server.stubFor(delete(urlEqualTo("/api/v1/ldap/organizations/ou%3Dsales%2Cdc%3Dex%2Cdc%3Dorg"))
                .willReturn(aResponse().withStatus(200)));
        LscModifications m = lm(LscModificationType.DELETE_OBJECT, "ou=sales,dc=ex,dc=org");
        assertTrue(orgsService().apply(m));
    }

    @Test
    void updateOrganizationPutsEncodedDn() throws Exception {
        server.stubFor(put(urlEqualTo("/api/v1/ldap/organizations/ou%3Dsales%2Cdc%3Dex%2Cdc%3Dorg"))
                .willReturn(aResponse().withStatus(200)));
        LscModifications m = lm(LscModificationType.UPDATE_OBJECT, "ou=sales,dc=ex,dc=org");
        m.setLscAttributeModifications(Collections.singletonList(rep("description", "Up")));
        assertTrue(orgsService().apply(m));
    }

    @Test
    void readMethodsAreEmpty() throws Exception {
        LdapRestDstService svc = usersService();
        assertEquals(null, svc.getBean("alice", new org.lsc.LscDatasets(), false));
        assertTrue(svc.getListPivots().isEmpty());
        assertTrue(svc.getSupportedConnectionType().isEmpty());
        assertTrue(svc.getWriteDatasetIds().isEmpty());
    }
}
