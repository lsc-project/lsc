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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

class LdapRestAuthTest {

    @Test
    void bearerSetsAuthorizationHeader() {
        BearerAuth auth = new BearerAuth("abc-123");
        HttpRequest.Builder b = HttpRequest.newBuilder().uri(URI.create("http://x/y"));
        auth.apply(b, "POST", "/api/v1/ldap/users", "{\"x\":1}".getBytes(StandardCharsets.UTF_8));
        HttpRequest req = b.GET().build();
        Optional<String> h = req.headers().firstValue("Authorization");
        assertTrue(h.isPresent());
        assertEquals("Bearer abc-123", h.get());
    }

    /**
     * Cross-implementation contract test. The Node server in
     * test/plugins/auth/hmac.test.ts ("Cross-impl vector") hard-codes the
     * same expected signature. If either side drifts (signing string format,
     * body hashing rule, timestamp encoding), both tests fail.
     *
     * Vector: secret="test-secret-min-32-chars-long-xxx", method=POST,
     * path=/api/v1/ldap/users, ts=1700000000000, body={"uid":"alice"}.
     */
    @Test
    void hmacCrossImplVector() {
        String secret = "test-secret-min-32-chars-long-xxx";
        String method = "POST";
        String path = "/api/v1/ldap/users";
        long ts = 1_700_000_000_000L;
        byte[] body = "{\"uid\":\"alice\"}".getBytes(StandardCharsets.UTF_8);
        String expected = "65b065ff10ab2a54de0ab4db485c5744fcdd32a98e2fd24a8cef5240b43bbc94";
        assertEquals(expected, HmacAuth.computeSignature(secret, method, path, ts, body),
                "HMAC signature must match the ldap-rest server's vector");
    }

    @Test
    void hmacReproducibleSignature() throws Exception {
        // Reference vector — values are also used by the cross-check
        // implemented by hand below.
        String secret = "test-secret-min-32-chars-long-xxx";
        String serviceId = "lsc";
        String method = "POST";
        String path = "/api/v1/ldap/users";
        // Body uses LinkedHashMap-style insertion order: {"uid":"alice"}.
        // That's exactly what the translator produces.
        String body = "{\"uid\":\"alice\"}";
        long fixedTs = 1_700_000_000_000L;

        // Compute expected signature manually with javax.crypto.Mac
        String bodyHash = HmacAuth.sha256Hex(body.getBytes(StandardCharsets.UTF_8));
        String signingString = method + "|" + path + "|" + fixedTs + "|" + bodyHash;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] expectedSig = mac.doFinal(signingString.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : expectedSig) hex.append(String.format("%02x", b));
        String expectedHex = hex.toString();

        // computeSignature should match
        String computed = HmacAuth.computeSignature(secret, method, path, fixedTs,
                body.getBytes(StandardCharsets.UTF_8));
        assertEquals(expectedHex, computed, "computeSignature must match Mac directly");

        // apply() must produce the same signature in the header
        HmacAuth auth = new HmacAuth(serviceId, secret, () -> fixedTs);
        HttpRequest.Builder b = HttpRequest.newBuilder().uri(URI.create("http://x" + path));
        auth.apply(b, method, path, body.getBytes(StandardCharsets.UTF_8));
        HttpRequest req = b.POST(HttpRequest.BodyPublishers.ofString(body)).build();
        String headerValue = req.headers().firstValue("Authorization").orElseThrow();
        assertEquals("HMAC-SHA256 " + serviceId + ":" + fixedTs + ":" + expectedHex, headerValue);
    }

    @Test
    void hmacUsesEmptyBodyHashForGet() {
        long ts = 1_700_000_000_000L;
        String secret = "secret";
        String path = "/api/v1/ldap/users";
        // For GET, bodyHash must be the empty string and body is ignored.
        String sigGet = HmacAuth.computeSignature(secret, "GET", path, ts, "ignored".getBytes());
        // signing string is METHOD|PATH|TS| (with empty bodyHash)
        String expectedSigningString = "GET|" + path + "|" + ts + "|";
        String expected = HmacAuth.hmacSha256Hex(secret, expectedSigningString);
        assertEquals(expected, sigGet);
    }

    @Test
    void hmacDeleteUsesEmptyBodyHash() {
        long ts = 42L;
        String secret = "k";
        String path = "/api/v1/ldap/users/alice";
        String sig = HmacAuth.computeSignature(secret, "DELETE", path, ts, null);
        String expected = HmacAuth.hmacSha256Hex(secret, "DELETE|" + path + "|" + ts + "|");
        assertEquals(expected, sig);
    }

    @Test
    void sha256HexKnownVector() {
        // Empty input vector
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                HmacAuth.sha256Hex(new byte[0]));
        // "abc" vector
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                HmacAuth.sha256Hex("abc".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void hmacSignatureNonEmpty() {
        String sig = HmacAuth.computeSignature("key", "POST", "/x", 1L, "body".getBytes());
        assertNotNull(sig);
        assertEquals(64, sig.length()); // 32 bytes hex
    }
}
