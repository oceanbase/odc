/*
 * Copyright (c) 2023 OceanBase.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oceanbase.odc.service.iam.auth.accesskey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.oceanbase.odc.service.iam.auth.accesskey.SecureSignatureUtils.AuthorizationInfo;

public class SecureSignatureUtilsTest {

    private static final String TEST_ACCESS_KEY_ID = "AK1234567890ABCDEF";
    private static final String TEST_SECRET_ACCESS_KEY = "test-secret-key-12345";
    private static final String TEST_HOST = "api.example.com";
    private static final String TEST_PATH = "/api/v1/test";
    private static final String TEST_METHOD = "GET";
    private static final String TEST_RFC_DATE = "Wed, 21 Oct 2015 07:28:00 GMT";
    private static final String TEST_NONCE = "test-nonce-12345";

    @Test
    public void testGenerateSecureSignature_GetRequest() {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("page", "1");
        queryParams.put("size", "10");

        String signature = SecureSignatureUtils.generateSecureSignature(
                TEST_HOST, TEST_PATH, TEST_METHOD, queryParams, null,
                TEST_SECRET_ACCESS_KEY, TEST_RFC_DATE, TEST_NONCE, "HMACSHA256");

        assertNotNull(signature);
        assertFalse(signature.isEmpty());
        assertTrue(signature.matches("[a-f0-9]+")); // 应该是十六进制字符串
    }

    @Test
    public void testGenerateSecureSignature_PostRequestWithBody() {
        String requestBody = "{\"name\":\"test\",\"value\":\"data\"}";
        byte[] bodyBytes = requestBody.getBytes(StandardCharsets.UTF_8);

        String signature = SecureSignatureUtils.generateSecureSignature(
                TEST_HOST, TEST_PATH, "POST", new HashMap<>(), bodyBytes,
                TEST_SECRET_ACCESS_KEY, TEST_RFC_DATE, TEST_NONCE, "HMACSHA256");

        assertNotNull(signature);
        assertFalse(signature.isEmpty());
        assertTrue(signature.matches("[a-f0-9]+"));
    }

    @Test
    public void testGenerateSecureSignature_DifferentAlgorithms() {
        String[] algorithms = {"HMACSHA1", "HMACSHA256", "HMACSHA512"};

        for (String algorithm : algorithms) {
            String signature = SecureSignatureUtils.generateSecureSignature(
                    TEST_HOST, TEST_PATH, TEST_METHOD, new HashMap<>(), null,
                    TEST_SECRET_ACCESS_KEY, TEST_RFC_DATE, TEST_NONCE, algorithm);

            assertNotNull(signature);
            assertFalse(signature.isEmpty());
            assertTrue(signature.matches("[a-f0-9]+"));
        }
    }

    @Test
    public void testGenerateSecureSignature_EmptyBody() {
        String signature = SecureSignatureUtils.generateSecureSignature(
                TEST_HOST, TEST_PATH, TEST_METHOD, new HashMap<>(), null,
                TEST_SECRET_ACCESS_KEY, TEST_RFC_DATE, TEST_NONCE, "HMACSHA256");

        assertNotNull(signature);
        assertFalse(signature.isEmpty());
    }

    @Test
    public void testGenerateSecureSignature_WithQueryParams() {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("filter", "active");
        queryParams.put("sort", "name");
        queryParams.put("order", "asc");

        String signature = SecureSignatureUtils.generateSecureSignature(
                TEST_HOST, TEST_PATH, TEST_METHOD, queryParams, null,
                TEST_SECRET_ACCESS_KEY, TEST_RFC_DATE, TEST_NONCE, "HMACSHA256");

        assertNotNull(signature);
        assertFalse(signature.isEmpty());
    }

    @Test
    public void testVerifySignature_ValidSignature() {
        String signature = SecureSignatureUtils.generateSecureSignature(
                TEST_HOST, TEST_PATH, TEST_METHOD, new HashMap<>(), null,
                TEST_SECRET_ACCESS_KEY, TEST_RFC_DATE, TEST_NONCE, "HMACSHA256");

        AuthenticationParam param = AuthenticationParam.builder()
                .host(TEST_HOST)
                .path(TEST_PATH)
                .method(TEST_METHOD)
                .queryParams(new HashMap<>())
                .body(null)
                .headerDate(TEST_RFC_DATE)
                .nonce(TEST_NONCE)
                .algorithm("HMACSHA256")
                .accessKeyId(TEST_ACCESS_KEY_ID)
                .signature(signature)
                .build();

        boolean isValid = SecureSignatureUtils.verifySignature(param, TEST_SECRET_ACCESS_KEY);
        assertTrue("Signature should be valid", isValid);
    }

    @Test
    public void testVerifySignature_InvalidSignature() {
        AuthenticationParam param = AuthenticationParam.builder()
                .host(TEST_HOST)
                .path(TEST_PATH)
                .method(TEST_METHOD)
                .queryParams(new HashMap<>())
                .body(null)
                .headerDate(TEST_RFC_DATE)
                .nonce(TEST_NONCE)
                .algorithm("HMACSHA256")
                .accessKeyId(TEST_ACCESS_KEY_ID)
                .signature("invalid-signature")
                .build();

        boolean isValid = SecureSignatureUtils.verifySignature(param, TEST_SECRET_ACCESS_KEY);
        assertFalse("Signature should be invalid", isValid);
    }

    @Test
    public void testVerifySignature_WithBody() {
        String requestBody = "{\"name\":\"test\",\"value\":\"data\"}";
        byte[] bodyBytes = requestBody.getBytes(StandardCharsets.UTF_8);

        String signature = SecureSignatureUtils.generateSecureSignature(
                TEST_HOST, TEST_PATH, "POST", new HashMap<>(), bodyBytes,
                TEST_SECRET_ACCESS_KEY, TEST_RFC_DATE, TEST_NONCE, "HMACSHA256");

        AuthenticationParam param = AuthenticationParam.builder()
                .host(TEST_HOST)
                .path(TEST_PATH)
                .method("POST")
                .queryParams(new HashMap<>())
                .body(bodyBytes)
                .headerDate(TEST_RFC_DATE)
                .nonce(TEST_NONCE)
                .algorithm("HMACSHA256")
                .accessKeyId(TEST_ACCESS_KEY_ID)
                .signature(signature)
                .build();

        boolean isValid = SecureSignatureUtils.verifySignature(param, TEST_SECRET_ACCESS_KEY);
        assertTrue("Signature should be valid", isValid);
    }

    @Test
    public void testGenerateNonce() {
        String nonce1 = SecureSignatureUtils.generateNonce();
        String nonce2 = SecureSignatureUtils.generateNonce();

        assertNotNull(nonce1);
        assertNotNull(nonce2);
        assertFalse(nonce1.isEmpty());
        assertFalse(nonce2.isEmpty());
        assertFalse(nonce1.contains("-"));
        assertFalse(nonce2.contains("-"));

        assertFalse("Nonces should be different", nonce1.equals(nonce2));
    }

    @Test
    public void testIsTimestampValid_ValidTimestamp() {
        String currentTime = ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);

        boolean isValid = SecureSignatureUtils.isTimestampValid(currentTime, 300L);
        assertTrue("Current timestamp should be valid", isValid);
    }

    @Test
    public void testIsTimestampValid_ExpiredTimestamp() {
        String expiredTime = ZonedDateTime.now().minusHours(1).format(DateTimeFormatter.RFC_1123_DATE_TIME);

        boolean isValid = SecureSignatureUtils.isTimestampValid(expiredTime, 300L);
        assertFalse("Expired timestamp should be invalid", isValid);
    }

    @Test
    public void testIsTimestampValid_FutureTimestamp() {
        String futureTime = ZonedDateTime.now().plusHours(1).format(DateTimeFormatter.RFC_1123_DATE_TIME);

        boolean isValid = SecureSignatureUtils.isTimestampValid(futureTime, 300L);
        assertFalse("Future timestamp should be invalid", isValid);
    }

    @Test
    public void testIsTimestampValid_InvalidFormat() {
        String invalidTime = "invalid-timestamp";

        boolean isValid = SecureSignatureUtils.isTimestampValid(invalidTime, 300L);
        assertFalse("Invalid timestamp format should be invalid", isValid);
    }

    @Test
    public void testBuildAuthorizationHeader() {
        String signature = "test-signature-12345";
        String algorithm = "HMACSHA256";

        String authHeader = SecureSignatureUtils.buildAuthorizationHeader(
                TEST_ACCESS_KEY_ID, signature, algorithm);

        String expected = "ODC-ACCESS-KEY-HMACSHA256 " + TEST_ACCESS_KEY_ID + ":" + signature;
        assertEquals("Authorization header should match expected format", expected, authHeader);
    }

    @Test
    public void testParseAuthorizationHeader_ValidFormat() {
        String authHeader = "ODC-ACCESS-KEY-HMACSHA256 " + TEST_ACCESS_KEY_ID + ":test-signature";

        AuthorizationInfo info = SecureSignatureUtils.parseAuthorizationHeader(authHeader);

        assertNotNull(info);
        assertEquals("HMACSHA256", info.getAlgorithm());
        assertEquals(TEST_ACCESS_KEY_ID, info.getAccessKeyId());
        assertEquals("test-signature", info.getSignature());
    }

    @Test
    public void testParseAuthorizationHeader_InvalidPrefix() {
        String authHeader = "INVALID-PREFIX " + TEST_ACCESS_KEY_ID + ":test-signature";

        AuthorizationInfo info = SecureSignatureUtils.parseAuthorizationHeader(authHeader);

        assertTrue("Should return null for invalid prefix", info == null);
    }

    @Test
    public void testParseAuthorizationHeader_InvalidFormat() {
        String authHeader = "ODC-ACCESS-KEY-HMACSHA256 invalid-format";

        AuthorizationInfo info = SecureSignatureUtils.parseAuthorizationHeader(authHeader);

        assertTrue("Should return null for invalid format", info == null);
    }

    @Test
    public void testParseAuthorizationHeader_NullInput() {
        AuthorizationInfo info = SecureSignatureUtils.parseAuthorizationHeader(null);

        assertTrue("Should return null for null input", info == null);
    }

    @Test
    public void testParseAuthorizationHeader_EmptyInput() {
        AuthorizationInfo info = SecureSignatureUtils.parseAuthorizationHeader("");

        assertTrue("Should return null for empty input", info == null);
    }

    @Test
    public void testSignatureConsistency() {
        String signature1 = SecureSignatureUtils.generateSecureSignature(
                TEST_HOST, TEST_PATH, TEST_METHOD, new HashMap<>(), null,
                TEST_SECRET_ACCESS_KEY, TEST_RFC_DATE, TEST_NONCE, "HMACSHA256");

        String signature2 = SecureSignatureUtils.generateSecureSignature(
                TEST_HOST, TEST_PATH, TEST_METHOD, new HashMap<>(), null,
                TEST_SECRET_ACCESS_KEY, TEST_RFC_DATE, TEST_NONCE, "HMACSHA256");

        assertEquals("Signatures should be consistent for same parameters", signature1, signature2);
    }

    @Test
    public void testSignatureUniqueness() {
        String signature1 = SecureSignatureUtils.generateSecureSignature(
                TEST_HOST, TEST_PATH, TEST_METHOD, new HashMap<>(), null,
                TEST_SECRET_ACCESS_KEY, TEST_RFC_DATE, TEST_NONCE, "HMACSHA256");

        String signature2 = SecureSignatureUtils.generateSecureSignature(
                TEST_HOST, TEST_PATH, TEST_METHOD, new HashMap<>(), null,
                TEST_SECRET_ACCESS_KEY, TEST_RFC_DATE, "different-nonce", "HMACSHA256");

        assertFalse("Signatures should be different for different nonces", signature1.equals(signature2));
    }

    @Test
    public void testSignatureWithSpecialCharacters() {
        String pathWithSpecialChars = "/api/v1/test/path with spaces";
        Map<String, String> paramsWithSpecialChars = new HashMap<>();
        paramsWithSpecialChars.put("param with spaces", "value with spaces");
        paramsWithSpecialChars.put("param&with=special", "value&with=special");

        String signature = SecureSignatureUtils.generateSecureSignature(
                TEST_HOST, pathWithSpecialChars, TEST_METHOD, paramsWithSpecialChars, null,
                TEST_SECRET_ACCESS_KEY, TEST_RFC_DATE, TEST_NONCE, "HMACSHA256");

        assertNotNull(signature);
        assertFalse(signature.isEmpty());
    }

    @Test
    public void testSignatureWithLargeBody() {
        StringBuilder largeBody = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeBody.append("This is a large request body for testing signature generation. ");
        }
        byte[] bodyBytes = largeBody.toString().getBytes(StandardCharsets.UTF_8);

        String signature = SecureSignatureUtils.generateSecureSignature(
                TEST_HOST, TEST_PATH, "POST", new HashMap<>(), bodyBytes,
                TEST_SECRET_ACCESS_KEY, TEST_RFC_DATE, TEST_NONCE, "HMACSHA256");

        assertNotNull(signature);
        assertFalse(signature.isEmpty());
    }
}
