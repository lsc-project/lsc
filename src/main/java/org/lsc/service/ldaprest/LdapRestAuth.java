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

import java.net.http.HttpRequest;

/**
 * Pluggable authentication strategy that adds the relevant
 * Authorization header on an outgoing HTTP request to ldap-rest.
 *
 * <p>The strategy receives the parsed components of the request
 * ({@code method}, {@code path}, {@code body}) so that signed schemes
 * (HMAC) can compute their signature. Stateless schemes (Bearer)
 * simply add a static header.</p>
 *
 * <p>Implementations <strong>must</strong> be safe for concurrent use
 * across multiple HTTP clients/threads.</p>
 */
public interface LdapRestAuth {

    /**
     * Apply this auth strategy to {@code builder}, signing the
     * outgoing request based on its method, path and body.
     *
     * @param builder the {@link HttpRequest.Builder} being built
     * @param method  the HTTP method, upper case (e.g. {@code POST})
     * @param path    the request path, e.g. {@code /api/v1/ldap/users}
     * @param body    the body bytes that will be sent, or empty for
     *                methods without a body (GET, DELETE, HEAD)
     */
    void apply(HttpRequest.Builder builder, String method, String path, byte[] body);
}
