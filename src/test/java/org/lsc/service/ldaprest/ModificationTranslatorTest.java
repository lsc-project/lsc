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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.lsc.LscDatasetModification;
import org.lsc.LscDatasetModification.LscDatasetModificationType;
import org.lsc.LscModificationType;
import org.lsc.LscModifications;
import org.lsc.exception.LscServiceException;

import com.fasterxml.jackson.databind.ObjectMapper;

class ModificationTranslatorTest {

    private static final ObjectMapper M = new ObjectMapper();

    private LscModifications mods(LscModificationType op, String mainId,
                                  LscDatasetModification... attrs) {
        LscModifications m = new LscModifications(op, "test-task");
        m.setMainIdentifer(mainId);
        m.setLscAttributeModifications(Arrays.asList(attrs));
        return m;
    }

    private LscDatasetModification attr(LscDatasetModificationType op, String name, Object... vals) {
        return new LscDatasetModification(op, name, Arrays.asList(vals));
    }

    private Map<?, ?> parse(String json) throws Exception {
        return M.readValue(json, Map.class);
    }

    // ---------------- CREATE ----------------
    @Test
    void createFlatSingleValue() throws Exception {
        ModificationTranslator t = new ModificationTranslator(new ResourceMapper("users"));
        LscModifications lm = mods(LscModificationType.CREATE_OBJECT, "uid=alice,ou=users",
                attr(LscDatasetModificationType.REPLACE_VALUES, "uid", "alice"),
                attr(LscDatasetModificationType.REPLACE_VALUES, "sn", "Doe"));
        String json = t.buildCreateBody(lm);
        Map<?, ?> m = parse(json);
        assertEquals("alice", m.get("uid"));
        assertEquals("Doe", m.get("sn"));
    }

    @Test
    void createFlatMultiValueAsArray() throws Exception {
        ModificationTranslator t = new ModificationTranslator(new ResourceMapper("users"));
        LscModifications lm = mods(LscModificationType.CREATE_OBJECT, "uid=bob",
                attr(LscDatasetModificationType.REPLACE_VALUES, "objectClass", "top", "person"));
        String json = t.buildCreateBody(lm);
        Map<?, ?> m = parse(json);
        assertEquals(Arrays.asList("top", "person"), m.get("objectClass"));
    }

    @Test
    void createGroup() throws Exception {
        ModificationTranslator t = new ModificationTranslator(new ResourceMapper("groups"));
        LscModifications lm = mods(LscModificationType.CREATE_OBJECT, "cn=admins,ou=groups",
                attr(LscDatasetModificationType.REPLACE_VALUES, "cn", "admins"),
                attr(LscDatasetModificationType.REPLACE_VALUES, "description", "Administrators"),
                attr(LscDatasetModificationType.REPLACE_VALUES, "member",
                        "uid=alice,ou=users", "uid=bob,ou=users"));
        String json = t.buildCreateBody(lm);
        Map<?, ?> m = parse(json);
        assertEquals("admins", m.get("cn"));
        assertEquals("Administrators", m.get("description"));
        assertEquals(Arrays.asList("uid=alice,ou=users", "uid=bob,ou=users"), m.get("member"));
    }

    @Test
    void createOrganization() throws Exception {
        ModificationTranslator t = new ModificationTranslator(new ResourceMapper("organizations"));
        LscModifications lm = mods(LscModificationType.CREATE_OBJECT, "ou=sales,dc=ex,dc=org",
                attr(LscDatasetModificationType.REPLACE_VALUES, "ou", "sales"),
                attr(LscDatasetModificationType.REPLACE_VALUES, "description", "Sales team"));
        String json = t.buildCreateBody(lm);
        Map<?, ?> m = parse(json);
        assertEquals("sales", m.get("ou"));
        assertEquals("Sales team", m.get("description"));
    }

    // ---------------- UPDATE ----------------
    @Test
    void updateReplaceOnly() throws Exception {
        ModificationTranslator t = new ModificationTranslator(new ResourceMapper("users"));
        LscModifications lm = mods(LscModificationType.UPDATE_OBJECT, "uid=alice,ou=users",
                attr(LscDatasetModificationType.REPLACE_VALUES, "sn", "NewName"));
        Map<?, ?> m = parse(t.buildUpdateBody(lm));
        assertTrue(m.containsKey("replace"));
        Map<?, ?> rep = (Map<?, ?>) m.get("replace");
        assertEquals(Collections.singletonList("NewName"), rep.get("sn"));
        assertTrue(!m.containsKey("add"));
        assertTrue(!m.containsKey("delete"));
    }

    @Test
    void updateAddOnly() throws Exception {
        ModificationTranslator t = new ModificationTranslator(new ResourceMapper("users"));
        LscModifications lm = mods(LscModificationType.UPDATE_OBJECT, "uid=alice",
                attr(LscDatasetModificationType.ADD_VALUES, "mail", "a@x.fr"));
        Map<?, ?> m = parse(t.buildUpdateBody(lm));
        assertTrue(m.containsKey("add"));
        Map<?, ?> add = (Map<?, ?>) m.get("add");
        assertEquals(Collections.singletonList("a@x.fr"), add.get("mail"));
    }

    @Test
    void updateDeleteAttributeWide() throws Exception {
        ModificationTranslator t = new ModificationTranslator(new ResourceMapper("users"));
        LscModifications lm = mods(LscModificationType.UPDATE_OBJECT, "uid=alice",
                new LscDatasetModification(LscDatasetModificationType.DELETE_VALUES,
                        "telephoneNumber", Collections.emptyList()));
        Map<?, ?> m = parse(t.buildUpdateBody(lm));
        assertEquals(Collections.singletonList("telephoneNumber"), m.get("delete"));
    }

    @Test
    void updateDeleteSpecificValues() throws Exception {
        ModificationTranslator t = new ModificationTranslator(new ResourceMapper("users"));
        LscModifications lm = mods(LscModificationType.UPDATE_OBJECT, "uid=alice",
                attr(LscDatasetModificationType.DELETE_VALUES, "mail", "old@x.fr"));
        Map<?, ?> m = parse(t.buildUpdateBody(lm));
        Map<?, ?> del = (Map<?, ?>) m.get("delete");
        assertEquals(Collections.singletonList("old@x.fr"), del.get("mail"));
    }

    @Test
    void updateMixReplaceAddDelete() throws Exception {
        ModificationTranslator t = new ModificationTranslator(new ResourceMapper("users"));
        LscModifications lm = mods(LscModificationType.UPDATE_OBJECT, "uid=alice",
                attr(LscDatasetModificationType.REPLACE_VALUES, "sn", "Doe"),
                attr(LscDatasetModificationType.ADD_VALUES, "mail", "n@x.fr"),
                attr(LscDatasetModificationType.DELETE_VALUES, "mail", "o@x.fr"),
                new LscDatasetModification(LscDatasetModificationType.DELETE_VALUES,
                        "telephoneNumber", Collections.emptyList()));
        Map<?, ?> m = parse(t.buildUpdateBody(lm));
        assertTrue(m.containsKey("replace"));
        assertTrue(m.containsKey("add"));
        // delete merged: telephoneNumber → [], mail → [o@x.fr]
        Map<?, ?> del = (Map<?, ?>) m.get("delete");
        assertEquals(Collections.singletonList("o@x.fr"), del.get("mail"));
        assertEquals(Collections.emptyList(), del.get("telephoneNumber"));
    }

    @Test
    void updateGroupShape() throws Exception {
        ModificationTranslator t = new ModificationTranslator(new ResourceMapper("groups"));
        LscModifications lm = mods(LscModificationType.UPDATE_OBJECT, "cn=admins",
                attr(LscDatasetModificationType.REPLACE_VALUES, "description", "New desc"));
        Map<?, ?> m = parse(t.buildUpdateBody(lm));
        Map<?, ?> rep = (Map<?, ?>) m.get("replace");
        assertEquals(Collections.singletonList("New desc"), rep.get("description"));
    }

    @Test
    void updateOrgShape() throws Exception {
        ModificationTranslator t = new ModificationTranslator(new ResourceMapper("organizations"));
        LscModifications lm = mods(LscModificationType.UPDATE_OBJECT, "ou=sales,dc=ex,dc=org",
                attr(LscDatasetModificationType.REPLACE_VALUES, "description", "Updated"));
        Map<?, ?> m = parse(t.buildUpdateBody(lm));
        assertTrue(m.containsKey("replace"));
    }

    // ---------------- MODRDN ----------------
    @Test
    void modrdnFlatProducesMoveTargetOrgDn() throws Exception {
        ModificationTranslator t = new ModificationTranslator(new ResourceMapper("users"));
        LscModifications lm = mods(LscModificationType.CHANGE_ID, "uid=alice,ou=users,dc=ex,dc=org");
        lm.setNewMainIdentifier("uid=alice,ou=admins,dc=ex,dc=org");
        ModificationTranslator.ModrdnPayload p = t.buildModrdnPayload(lm);
        assertEquals(ModificationTranslator.ModrdnKind.MOVE, p.kind);
        Map<?, ?> m = parse(p.body);
        assertEquals("ou=admins,dc=ex,dc=org", m.get("targetOrgDn"));
    }

    @Test
    void modrdnGroupSameParentProducesRename() throws Exception {
        ModificationTranslator t = new ModificationTranslator(new ResourceMapper("groups"));
        LscModifications lm = mods(LscModificationType.CHANGE_ID, "cn=oldname,ou=groups,dc=ex,dc=org");
        lm.setNewMainIdentifier("cn=newname,ou=groups,dc=ex,dc=org");
        ModificationTranslator.ModrdnPayload p = t.buildModrdnPayload(lm);
        assertEquals(ModificationTranslator.ModrdnKind.RENAME, p.kind);
        Map<?, ?> m = parse(p.body);
        assertEquals("newname", m.get("newCn"));
    }

    @Test
    void modrdnGroupDifferentParentProducesMove() throws Exception {
        ModificationTranslator t = new ModificationTranslator(new ResourceMapper("groups"));
        LscModifications lm = mods(LscModificationType.CHANGE_ID, "cn=admins,ou=groups,dc=ex,dc=org");
        lm.setNewMainIdentifier("cn=admins,ou=other,dc=ex,dc=org");
        ModificationTranslator.ModrdnPayload p = t.buildModrdnPayload(lm);
        assertEquals(ModificationTranslator.ModrdnKind.MOVE, p.kind);
    }

    @Test
    void modrdnOrgProducesMove() throws Exception {
        ModificationTranslator t = new ModificationTranslator(new ResourceMapper("organizations"));
        LscModifications lm = mods(LscModificationType.CHANGE_ID, "ou=sales,dc=ex,dc=org");
        lm.setNewMainIdentifier("ou=sales,ou=parent,dc=ex,dc=org");
        ModificationTranslator.ModrdnPayload p = t.buildModrdnPayload(lm);
        assertEquals(ModificationTranslator.ModrdnKind.MOVE, p.kind);
        Map<?, ?> m = parse(p.body);
        assertEquals("ou=parent,dc=ex,dc=org", m.get("targetOrgDn"));
    }

    @Test
    void modrdnFailsWithoutNewMainIdentifier() {
        ModificationTranslator t = new ModificationTranslator(new ResourceMapper("users"));
        LscModifications lm = mods(LscModificationType.CHANGE_ID, "uid=alice");
        assertThrows(LscServiceException.class, () -> t.buildModrdnPayload(lm));
    }

    // ---------------- BINARY ----------------
    @Test
    void binaryAttributeRejected() {
        ModificationTranslator t = new ModificationTranslator(new ResourceMapper("users"));
        byte[] notUtf8 = new byte[] { (byte) 0xff, (byte) 0xfe, (byte) 0xc3, 0x28 };
        LscModifications lm = mods(LscModificationType.CREATE_OBJECT, "uid=alice",
                new LscDatasetModification(LscDatasetModificationType.REPLACE_VALUES,
                        "jpegPhoto", java.util.Collections.singletonList(notUtf8)));
        LscServiceException ex = assertThrows(LscServiceException.class, () -> t.buildCreateBody(lm));
        assertTrue(ex.getMessage().contains("binary"));
        assertTrue(ex.getMessage().contains("jpegPhoto"));
    }

    @Test
    void utf8BinaryIsAcceptedAsString() throws Exception {
        ModificationTranslator t = new ModificationTranslator(new ResourceMapper("users"));
        byte[] utf8 = "héllo".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        LscModifications lm = mods(LscModificationType.CREATE_OBJECT, "uid=alice",
                new LscDatasetModification(LscDatasetModificationType.REPLACE_VALUES,
                        "description", java.util.Collections.singletonList(utf8)));
        Map<?, ?> m = parse(t.buildCreateBody(lm));
        assertEquals("héllo", m.get("description"));
    }

    // ---------------- JSON shape (signature stability) ----------------
    @Test
    void jsonOutputIsCompactNoExtraSpaces() throws Exception {
        ModificationTranslator t = new ModificationTranslator(new ResourceMapper("users"));
        LscModifications lm = mods(LscModificationType.CREATE_OBJECT, "uid=alice",
                attr(LscDatasetModificationType.REPLACE_VALUES, "uid", "alice"));
        String json = t.buildCreateBody(lm);
        // No pretty print: no spaces after ':' or ','
        assertEquals("{\"uid\":\"alice\"}", json);
    }

    @Test
    void jsonOutputPreservesInsertionOrder() throws Exception {
        ModificationTranslator t = new ModificationTranslator(new ResourceMapper("users"));
        // Insertion order matters for HMAC stability
        LscModifications lm = mods(LscModificationType.CREATE_OBJECT, "uid=alice",
                attr(LscDatasetModificationType.REPLACE_VALUES, "z", "1"),
                attr(LscDatasetModificationType.REPLACE_VALUES, "a", "2"));
        assertEquals("{\"z\":\"1\",\"a\":\"2\"}", t.buildCreateBody(lm));
    }

    @Test
    void writeJsonHelperIsCompact() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("a", 1);
        body.put("b", "two");
        assertEquals("{\"a\":1,\"b\":\"two\"}", ModificationTranslator.writeJson(body));
    }

    @Test
    void readMembersFromArray() throws Exception {
        java.util.List<Object> members = ModificationTranslator.readMembers(
                "{\"cn\":\"admins\",\"member\":[\"uid=alice,ou=users\",\"uid=bob,ou=users\"]}");
        assertEquals(2, members.size());
        assertEquals("uid=alice,ou=users", members.get(0));
        assertEquals("uid=bob,ou=users", members.get(1));
    }

    @Test
    void readMembersFromSingleString() throws Exception {
        java.util.List<Object> members = ModificationTranslator.readMembers(
                "{\"member\":\"uid=alice,ou=users\"}");
        assertEquals(1, members.size());
        assertEquals("uid=alice,ou=users", members.get(0));
    }

    @Test
    void readMembersHandlesMissingField() throws Exception {
        assertEquals(0, ModificationTranslator.readMembers("{\"cn\":\"admins\"}").size());
        assertEquals(0, ModificationTranslator.readMembers("{}").size());
        assertEquals(0, ModificationTranslator.readMembers("").size());
        assertEquals(0, ModificationTranslator.readMembers(null).size());
    }
}
