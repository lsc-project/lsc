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

import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.lsc.LscDatasetModification;
import org.lsc.LscDatasetModification.LscDatasetModificationType;
import org.lsc.LscModifications;
import org.lsc.exception.LscServiceException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Convert {@link LscModifications} objects into JSON payloads
 * understood by ldap-rest.
 *
 * <p>The class uses a single {@link ObjectMapper} configured for
 * compact, deterministic output (no pretty-printing, insertion-order
 * preserving maps). This is critical because the same bytes that go
 * on the wire are also used to compute the HMAC signature; any
 * formatting change would break the server-side signature
 * verification.</p>
 *
 * <p>Mapping cheat-sheet:</p>
 * <ul>
 *     <li><b>CREATE flat/org</b>: a single JSON object whose keys are
 *         the attribute names and values are either a scalar
 *         (single-valued) or an array (multi-valued).</li>
 *     <li><b>CREATE group</b>: same shape, ldap-rest expects {@code cn},
 *         optional {@code description} and {@code member}.</li>
 *     <li><b>UPDATE</b>: an object with {@code add}, {@code replace}
 *         and/or {@code delete} buckets, mirroring LSC's
 *         {@link LscDatasetModificationType}.</li>
 *     <li><b>MODRDN flat/org</b>: {@code { "targetOrgDn": "..." }}.</li>
 *     <li><b>MODRDN group</b>: {@code { "newCn": "..." }}.</li>
 * </ul>
 */
public class ModificationTranslator {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .disable(SerializationFeature.INDENT_OUTPUT)
            // serializing Maps preserves insertion order by default;
            // we just need to ensure no other reordering kicks in.
            .disable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    private final ResourceMapper resourceMapper;

    public ModificationTranslator(ResourceMapper resourceMapper) {
        this.resourceMapper = resourceMapper;
    }

    /**
     * Build the body for a CREATE call (POST /collection).
     */
    public String buildCreateBody(LscModifications lm) throws LscServiceException {
        Map<String, Object> body = new LinkedHashMap<>();
        for (LscDatasetModification mod : safeList(lm.getLscAttributeModifications())) {
            if (mod.getOperation() == LscDatasetModificationType.DELETE_VALUES) {
                // CREATE shouldn't carry deletions; ignore defensively.
                continue;
            }
            Object val = collapse(mod.getAttributeName(), mod.getValues());
            if (val != null) {
                body.put(mod.getAttributeName(), val);
            }
        }
        return writeJson(body);
    }

    /**
     * Build the body for an UPDATE call (PUT /collection/id).
     *
     * <p>Output shape:
     * <pre>
     *   {
     *     "replace": { attr: [v1, v2] },
     *     "add":     { attr: [v1] },
     *     "delete":  { attr: [v1] }   // value-targeted
     *               | ["attr1", "attr2"]   // attribute-wide
     *   }
     * </pre>
     * If both forms of {@code delete} appear, they are merged into
     * the value-targeted shape with {@code []} marking attribute-wide
     * deletes (an empty list is the convention used by ldap-rest's
     * server-side parser).</p>
     */
    public String buildUpdateBody(LscModifications lm) throws LscServiceException {
        Map<String, List<Object>> replace = new LinkedHashMap<>();
        Map<String, List<Object>> add = new LinkedHashMap<>();
        Map<String, List<Object>> deleteValues = new LinkedHashMap<>();
        List<String> deleteAttrs = new ArrayList<>();

        for (LscDatasetModification mod : safeList(lm.getLscAttributeModifications())) {
            String name = mod.getAttributeName();
            List<Object> vals = sanitizeValues(name, mod.getValues());
            switch (mod.getOperation()) {
                case REPLACE_VALUES:
                    replace.put(name, vals);
                    break;
                case ADD_VALUES:
                    add.put(name, vals);
                    break;
                case DELETE_VALUES:
                    if (vals.isEmpty()) {
                        deleteAttrs.add(name);
                    } else {
                        deleteValues.put(name, vals);
                    }
                    break;
                default:
                    // UNKNOWN — skip
                    break;
            }
        }

        Map<String, Object> body = new LinkedHashMap<>();
        if (!replace.isEmpty()) body.put("replace", replace);
        if (!add.isEmpty()) body.put("add", add);
        if (!deleteValues.isEmpty() || !deleteAttrs.isEmpty()) {
            if (deleteValues.isEmpty()) {
                body.put("delete", deleteAttrs);
            } else {
                // merge: attribute-wide deletes become attr -> [] entries
                for (String attr : deleteAttrs) {
                    deleteValues.putIfAbsent(attr, Collections.emptyList());
                }
                body.put("delete", deleteValues);
            }
        }
        return writeJson(body);
    }

    /**
     * Build the body for a MODRDN call. For flat/org resources the
     * payload is {@code { "targetOrgDn": newParent }}. For groups
     * with only an RDN change it is {@code { "newCn": newCn }}.
     *
     * <p>The translator picks the form based on the resource family
     * <em>and</em> on whether the new parent DN differs from the
     * current parent DN. If the parent DN is unchanged we treat it
     * as a pure rename (group only); if the parent changed we issue
     * a move.</p>
     *
     * @return a result object holding both the JSON body and the
     *         endpoint hint (rename vs move) so the caller can pick
     *         the right path.
     */
    public ModrdnPayload buildModrdnPayload(LscModifications lm) throws LscServiceException {
        String oldDn = lm.getMainIdentifier();
        String newDn = lm.getNewMainIdentifier();
        if (newDn == null || newDn.isEmpty()) {
            throw new LscServiceException("MODRDN without newMainIdentifier on " + oldDn);
        }
        String oldParent = ResourceMapper.parentDn(oldDn == null ? "" : oldDn);
        String newParent = ResourceMapper.parentDn(newDn);
        boolean parentChanged = !oldParent.equalsIgnoreCase(newParent);

        if (resourceMapper.getFamily() == ResourceMapper.Family.GROUPS && !parentChanged) {
            String newCn = ResourceMapper.rdnValue(newDn);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("newCn", newCn);
            return new ModrdnPayload(ModrdnKind.RENAME, writeJson(body));
        }
        // flat/org or group with parent change: use /move
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("targetOrgDn", newParent);
        return new ModrdnPayload(ModrdnKind.MOVE, writeJson(body));
    }

    /** Result of {@link #buildModrdnPayload(LscModifications)}. */
    public static final class ModrdnPayload {
        public final ModrdnKind kind;
        public final String body;

        public ModrdnPayload(ModrdnKind kind, String body) {
            this.kind = kind;
            this.body = body;
        }
    }

    public enum ModrdnKind { MOVE, RENAME }

    // -------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------

    /**
     * Collapse a values list to a JSON-friendly representation:
     * single value → scalar, multi-value → array, empty → {@code null}.
     * Fails on binary data.
     */
    private static Object collapse(String attr, List<Object> values) throws LscServiceException {
        List<Object> sanitized = sanitizeValues(attr, values);
        if (sanitized.isEmpty()) return null;
        if (sanitized.size() == 1) return sanitized.get(0);
        return sanitized;
    }

    private static List<Object> sanitizeValues(String attr, List<Object> values) throws LscServiceException {
        List<Object> out = new ArrayList<>();
        if (values == null) return out;
        for (Object v : values) {
            if (v == null) continue;
            if (v instanceof byte[]) {
                byte[] b = (byte[]) v;
                String s = decodeUtf8Strict(b);
                if (s == null) {
                    throw new LscServiceException(
                            "binary attributes not supported in this version: " + attr);
                }
                out.add(s);
            } else {
                out.add(v);
            }
        }
        return out;
    }

    /**
     * Try to decode bytes as strict UTF-8. Returns {@code null} if
     * the bytes do not form valid UTF-8 (which we treat as opaque
     * binary).
     */
    private static String decodeUtf8Strict(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    private static <T> List<T> safeList(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

    public static String writeJson(Object o) throws LscServiceException {
        try {
            return MAPPER.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new LscServiceException("failed to encode JSON payload: " + e.getMessage(), e);
        }
    }

    /**
     * Extract the {@code member} attribute values from a ldap-rest group
     * response body. Accepts the value as either an array of strings
     * (the common case) or a single string. Returns an empty list if
     * the response has no {@code member} field.
     */
    public static List<Object> readMembers(String json) throws LscServiceException {
        if (json == null || json.isEmpty()) return Collections.emptyList();
        try {
            com.fasterxml.jackson.databind.JsonNode root = MAPPER.readTree(json);
            com.fasterxml.jackson.databind.JsonNode m = root.get("member");
            if (m == null || m.isNull()) return Collections.emptyList();
            List<Object> out = new ArrayList<>();
            if (m.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode v : m) {
                    if (v != null && !v.isNull()) out.add(v.asText());
                }
            } else if (m.isTextual()) {
                out.add(m.asText());
            }
            return out;
        } catch (JsonProcessingException e) {
            throw new LscServiceException("failed to parse group response JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Lower-case a DN attribute type for comparison; trivial helper
     * exposed for {@link LdapRestDstService}.
     */
    static String lowerType(String type) {
        return type == null ? "" : type.toLowerCase(Locale.ROOT);
    }
}
