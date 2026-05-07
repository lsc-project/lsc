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

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.lsc.LscDatasetModification;
import org.lsc.LscDatasets;
import org.lsc.LscModifications;
import org.lsc.beans.IBean;
import org.lsc.configuration.ConnectionType;
import org.lsc.configuration.LdapRestAuthType;
import org.lsc.configuration.LdapRestDestinationServiceType;
import org.lsc.configuration.TaskType;
import org.lsc.configuration.ValuesType;
import org.lsc.exception.LscServiceException;
import org.lsc.service.IWritableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LSC {@link IWritableService} that writes through the ldap-rest
 * HTTP API.
 *
 * <p>Configuration is read from the
 * {@code <ldapRestDestinationService>} element in {@code lsc.xml};
 * LSC hands us the parsed {@link TaskType} and we use the typed
 * JAXB getters of {@link LdapRestDestinationServiceType}. Recognised
 * elements:</p>
 *
 * <ul>
 *     <li>{@code <baseUrl>} — required, e.g. {@code https://ldap-rest.example.org}</li>
 *     <li>{@code <resourceType>} — required, one of {@code users},
 *         {@code groups}, {@code organizations}, …</li>
 *     <li>{@code <auth>} block containing either {@code <bearer>} or
 *         {@code <hmacServiceId>}+{@code <hmacSecret>}</li>
 *     <li>{@code <timeoutMs>} — optional, default 10000ms</li>
 *     <li>{@code <retries>} — optional, default 3</li>
 *     <li>{@code <apiPrefix>} — optional, default {@code /api}</li>
 *     <li>{@code <writeDatasetIds>} — optional list of dataset ids
 *         this service is allowed to write</li>
 * </ul>
 *
 * <p>This service is purely <strong>destination</strong>: the read
 * methods ({@code getBean}, {@code getListPivots}) are stubbed
 * because LSC pulls source records from a separate source service
 * and only feeds us {@link LscModifications}. They return empty
 * structures so the framework does not crash if it ever calls
 * them.</p>
 *
 * <p>The {@code <connection>} reference inherited from
 * {@code serviceType} is required by the schema but ignored at
 * runtime: ldap-rest auth is configured inline via {@code <auth>}.
 * Operators usually point it to a stub LDAP connection just to keep
 * the schema happy.</p>
 */
public class LdapRestDstService implements IWritableService {

    private static final Logger LOG = LoggerFactory.getLogger(LdapRestDstService.class);

    private final LdapRestClient client;
    private final ResourceMapper mapper;
    private final ModificationTranslator translator;
    private final List<String> writeDatasetIds;

    /**
     * LSC constructor signature. Called once per task before sync.
     */
    public LdapRestDstService(TaskType task) throws LscServiceException {
        Config cfg = parseConfig(task);
        this.mapper = new ResourceMapper(cfg.resourceType, cfg.apiPrefix);
        this.translator = new ModificationTranslator(this.mapper);
        this.client = new LdapRestClient(cfg.baseUrl, cfg.auth, cfg.timeoutMs, cfg.retries);
        this.writeDatasetIds = cfg.writeDatasetIds;
        LOG.info("ldap-rest destination service initialised: baseUrl={}, resourceType={}, "
                        + "auth={}, timeoutMs={}, retries={}",
                cfg.baseUrl, cfg.resourceType, cfg.auth.getClass().getSimpleName(),
                cfg.timeoutMs, cfg.retries);
    }

    /** Test-only constructor injecting an already-built client. */
    LdapRestDstService(LdapRestClient client, ResourceMapper mapper) {
        this.client = client;
        this.mapper = mapper;
        this.translator = new ModificationTranslator(mapper);
        this.writeDatasetIds = Collections.emptyList();
    }

    @Override
    public boolean apply(LscModifications lm) throws LscServiceException {
        if (lm == null || lm.getOperation() == null) {
            throw new LscServiceException("LscModifications has no operation");
        }
        switch (lm.getOperation()) {
            case CREATE_OBJECT:
                return doCreate(lm);
            case UPDATE_OBJECT:
                return doUpdate(lm);
            case DELETE_OBJECT:
                return doDelete(lm);
            case CHANGE_ID:
                return doModrdn(lm);
            default:
                throw new LscServiceException("Unsupported LSC operation: " + lm.getOperation());
        }
    }

    private boolean doCreate(LscModifications lm) throws LscServiceException {
        String body = translator.buildCreateBody(lm);
        String path = mapper.collectionPath();
        HttpResponse<String> resp = client.post(path, body);
        LOG.debug("CREATE {} -> {}", path, resp.statusCode());
        return true;
    }

    private boolean doUpdate(LscModifications lm) throws LscServiceException {
        // For UPDATE on group, member-mutating attributes must use
        // the dedicated /members endpoints; ldap-rest itself enforces
        // this. We split the payload accordingly.
        if (mapper.getFamily() == ResourceMapper.Family.GROUPS) {
            return doGroupUpdate(lm);
        }
        String body = translator.buildUpdateBody(lm);
        String path = mapper.itemPath(lm.getMainIdentifier());
        HttpResponse<String> resp = client.put(path, body);
        LOG.debug("UPDATE {} -> {}", path, resp.statusCode());
        return true;
    }

    /**
     * Group updates are special: ldap-rest forbids touching the
     * {@code member} attribute through PUT and exposes dedicated
     * member endpoints. We extract member ADD/DELETE operations
     * first, fire dedicated calls for each, then fall back to PUT
     * for non-member attribute changes.
     */
    private boolean doGroupUpdate(LscModifications lm) throws LscServiceException {
        List<LscDatasetModification> rest = new ArrayList<>();
        List<Object> addedMembers = new ArrayList<>();
        List<Object> removedMembers = new ArrayList<>();
        boolean replaceMembers = false;
        List<Object> replacedMembers = Collections.emptyList();

        for (LscDatasetModification mod : safeList(lm.getLscAttributeModifications())) {
            if ("member".equalsIgnoreCase(mod.getAttributeName())) {
                List<Object> values = mod.getValues() == null ? Collections.emptyList() : mod.getValues();
                switch (mod.getOperation()) {
                    case ADD_VALUES:
                        addedMembers.addAll(values);
                        break;
                    case DELETE_VALUES:
                        if (values.isEmpty()) {
                            // attribute-wide delete on `member` — best-effort
                            // reconciliation: GET the group, list its current
                            // members, and queue them all for deletion. If the
                            // GET fails we surface a clear error rather than
                            // silently no-op.
                            List<Object> current = fetchCurrentMembers(lm.getMainIdentifier());
                            LOG.info("attribute-wide DELETE on group 'member' for {}: removing {} current member(s) via reconciliation",
                                    lm.getMainIdentifier(), current.size());
                            removedMembers.addAll(current);
                        } else {
                            removedMembers.addAll(values);
                        }
                        break;
                    case REPLACE_VALUES:
                        replaceMembers = true;
                        replacedMembers = new ArrayList<>(values);
                        break;
                    default:
                        break;
                }
            } else {
                rest.add(mod);
            }
        }

        // Apply replace-members as a (delete-all + add-all) sequence
        // because ldap-rest has no "replace members" endpoint.
        if (replaceMembers) {
            // We don't currently fetch the existing list (this service
            // is destination-only) so we approximate by removing the
            // explicit additions/removals first, then adding the new
            // set. Operators wanting a full reconciliation should use
            // ADD_VALUES/DELETE_VALUES explicitly in their sync map.
            LOG.warn("REPLACE on group 'member' is best-effort: missing members will not be removed."
                    + " Use ADD_VALUES/DELETE_VALUES for deterministic group sync.");
            addedMembers.addAll(replacedMembers);
        }

        for (Object m : addedMembers) {
            String memberDn = String.valueOf(m);
            String path = mapper.membersPath(lm.getMainIdentifier());
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("member", memberDn);
            client.post(path, ModificationTranslator.writeJson(body));
        }
        for (Object m : removedMembers) {
            String memberDn = String.valueOf(m);
            String path = mapper.memberItemPath(lm.getMainIdentifier(), memberDn);
            client.delete(path);
        }

        if (!rest.isEmpty()) {
            LscModifications stripped = new LscModifications(lm.getOperation(), lm.getTaskName());
            stripped.setMainIdentifer(lm.getMainIdentifier());
            stripped.setNewMainIdentifier(lm.getNewMainIdentifier());
            stripped.setLscAttributeModifications(rest);
            String body = translator.buildUpdateBody(stripped);
            // Avoid an empty PUT (no buckets) when only members changed.
            if (!"{}".equals(body)) {
                String path = mapper.itemPath(lm.getMainIdentifier());
                client.put(path, body);
            }
        }
        return true;
    }

    /**
     * GET the group at {@code groupDn}, parse its JSON, and return the
     * current {@code member} list. Used to reconcile attribute-wide
     * DELETE on {@code member} when LSC emits no explicit value list.
     */
    private List<Object> fetchCurrentMembers(String groupDn) throws LscServiceException {
        String path = mapper.itemPath(groupDn);
        HttpResponse<String> resp = client.get(path);
        return ModificationTranslator.readMembers(resp.body());
    }

    private boolean doDelete(LscModifications lm) throws LscServiceException {
        String path = mapper.itemPath(lm.getMainIdentifier());
        try {
            HttpResponse<String> resp = client.delete(path);
            LOG.debug("DELETE {} -> {}", path, resp.statusCode());
            return true;
        } catch (LscServiceException e) {
            // 404 on DELETE = soft idempotence
            String msg = e.getMessage();
            if (msg != null && msg.contains("HTTP 404")) {
                LOG.warn("DELETE {} returned 404 — already absent, treating as success", path);
                return true;
            }
            throw e;
        }
    }

    private boolean doModrdn(LscModifications lm) throws LscServiceException {
        ModificationTranslator.ModrdnPayload payload = translator.buildModrdnPayload(lm);
        String path;
        if (payload.kind == ModificationTranslator.ModrdnKind.RENAME) {
            path = mapper.renamePath(lm.getMainIdentifier());
        } else {
            path = mapper.movePath(lm.getMainIdentifier());
        }
        HttpResponse<String> resp = client.post(path, payload.body);
        LOG.debug("MODRDN {} {} -> {}", payload.kind, path, resp.statusCode());
        return true;
    }

    @Override
    public List<String> getWriteDatasetIds() {
        return writeDatasetIds == null ? Collections.emptyList() : writeDatasetIds;
    }

    /**
     * Read-side: this service is destination-only. LSC requires the
     * methods to exist (because {@link IWritableService} extends
     * {@code IService}) but we are never the source. Returning empty
     * structures keeps LSC happy in defensive code paths.
     */
    @Override
    public IBean getBean(String pivotName, LscDatasets pivotAttributes, boolean fromSameService)
            throws LscServiceException {
        LOG.debug("getBean({}) called on destination-only service — returning null", pivotName);
        return null;
    }

    @Override
    public Map<String, LscDatasets> getListPivots() throws LscServiceException {
        LOG.debug("getListPivots() called on destination-only service — returning empty map");
        return new HashMap<>();
    }

    @Override
    public Collection<Class<? extends ConnectionType>> getSupportedConnectionType() {
        // We do not bind to a typed LSC connection (no LDAP, no JDBC);
        // ldap-rest auth is configured inline. Return empty so LSC
        // doesn't try to inject one.
        return Collections.emptyList();
    }

    // -------------------------------------------------------------------
    // configuration parsing
    // -------------------------------------------------------------------

    static class Config {
        String baseUrl;
        String resourceType;
        String apiPrefix = "/api";
        long timeoutMs = 10_000L;
        int retries = 3;
        LdapRestAuth auth;
        List<String> writeDatasetIds = new ArrayList<>();
    }

    static Config parseConfig(TaskType task) throws LscServiceException {
        if (task == null) {
            throw new LscServiceException("ldap-rest destination service: TaskType is null");
        }
        LdapRestDestinationServiceType svc = task.getLdapRestDestinationService();
        if (svc == null) {
            throw new LscServiceException(
                    "ldap-rest destination service: <ldapRestDestinationService> not configured");
        }
        return parseConfig(svc);
    }

    /**
     * Parse the typed JAXB configuration directly. Exposed for unit
     * tests so they can build a {@link LdapRestDestinationServiceType}
     * on the fly without going through a full {@link TaskType}.
     */
    static Config parseConfig(LdapRestDestinationServiceType svc) throws LscServiceException {
        if (svc == null) {
            throw new LscServiceException("ldap-rest destination service: configuration is null");
        }
        Config cfg = new Config();
        cfg.baseUrl = trimToNull(svc.getBaseUrl());
        cfg.resourceType = trimToNull(svc.getResourceType());
        String prefix = trimToNull(svc.getApiPrefix());
        if (prefix != null) {
            cfg.apiPrefix = normaliseApiPrefix(prefix);
        }
        Long timeout = svc.getTimeoutMs();
        if (timeout != null) {
            if (timeout <= 0) {
                throw new LscServiceException("<timeoutMs> must be > 0, got: " + timeout);
            }
            cfg.timeoutMs = timeout;
        }
        Integer retries = svc.getRetries();
        if (retries != null) {
            if (retries < 0) {
                throw new LscServiceException("<retries> must be >= 0, got: " + retries);
            }
            cfg.retries = retries;
        }
        ValuesType ids = svc.getWriteDatasetIds();
        if (ids != null && ids.getString() != null) {
            for (String s : ids.getString()) {
                if (s == null) continue;
                String t = s.trim();
                if (!t.isEmpty()) cfg.writeDatasetIds.add(t);
            }
        }
        if (cfg.baseUrl == null) {
            throw new LscServiceException("ldap-rest destination service: <baseUrl> is required");
        }
        if (cfg.resourceType == null) {
            throw new LscServiceException("ldap-rest destination service: <resourceType> is required");
        }
        cfg.auth = parseAuth(svc.getAuth());
        return cfg;
    }

    static LdapRestAuth parseAuth(LdapRestAuthType authElement) throws LscServiceException {
        if (authElement == null) {
            throw new LscServiceException(
                    "ldap-rest destination service: <auth> block missing — provide <bearer> or "
                            + "<hmacServiceId>+<hmacSecret>");
        }
        String bearer = trimToNull(authElement.getBearer());
        String svcId = trimToNull(authElement.getHmacServiceId());
        String secret = trimToNull(authElement.getHmacSecret());
        if (bearer != null) {
            return new BearerAuth(bearer);
        }
        if (svcId != null && secret != null) {
            return new HmacAuth(svcId, secret);
        }
        throw new LscServiceException(
                "ldap-rest destination service: <auth> needs either <bearer> or both "
                        + "<hmacServiceId> and <hmacSecret>");
    }

    /**
     * Normalise {@code <apiPrefix>}: ensure leading {@code /}, strip
     * trailing {@code /}. So {@code "api"}, {@code "/api"}, {@code "/api/"}
     * all become {@code "/api"}. Also accepts the empty prefix {@code "/"}.
     */
    static String normaliseApiPrefix(String raw) {
        String s = raw.trim();
        if (s.isEmpty() || "/".equals(s)) return "";
        if (!s.startsWith("/")) s = "/" + s;
        while (s.length() > 1 && s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static <T> List<T> safeList(List<T> l) {
        return l == null ? Collections.emptyList() : l;
    }

    /** Test seam to expose internals to the integration test. */
    LdapRestClient client() { return client; }
    ResourceMapper mapper() { return mapper; }
}
