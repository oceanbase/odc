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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SecureSignatureUtils {

    /**
     *
     * @param host 主机地址
     * @param path 请求路径
     * @param method HTTP方法
     * @param queryParams 查询参数
     * @param body 请求体
     * @param accessKeySecret 访问密钥
     * @param rfcDate RFC格式日期
     * @param nonce 随机数（防重放）
     * @param algorithm 签名算法（如：HMACSHA256）
     * @return 签名结果
     */
    public static String generateSecureSignature(String host, String path, String method,
            Map<String, String> queryParams,
            byte[] body,
            String accessKeySecret, String rfcDate, String nonce, String algorithm) {
        try {
            String stringToSign = buildStringToSign(accessKeySecret, algorithm, host, path, method, queryParams, body,
                    rfcDate, nonce);

            byte[] signatureBytes = hmacSign(accessKeySecret, stringToSign.getBytes(StandardCharsets.UTF_8), algorithm);
            return Hex.encodeHexString(signatureBytes);
        } catch (Exception e) {
            log.error("Failed to generate secure signature", e);
            throw new RuntimeException("Signature generation failed", e);
        }
    }


    private static String buildStringToSign(String accessKeySecret, String algorithm, String host, String path,
            String method,
            Map<String, String> queryParams, byte[] body, String rfcDate, String nonce) {
        StringJoiner strToSign = new StringJoiner("\n");

        strToSign.add(method);

        if (body == null || body.length == 0) {
            strToSign.add("");
        } else {
            try {
                strToSign.add(secureBodyHash(accessKeySecret, body, algorithm));
            } catch (Exception e) {
                log.error("Failed to hash body", e);
                strToSign.add("");
            }
        }

        strToSign.add(rfcDate);

        strToSign.add(host);

        strToSign.add(nonce != null ? nonce : "");

        String pathAndParams = buildPathAndParams(path, queryParams);
        strToSign.add(pathAndParams);

        String stringToSign = strToSign.toString();
        log.debug("String to sign: {}", stringToSign);

        return stringToSign;
    }

    private static String secureBodyHash(String accessKeySecret, byte[] data, String algorithm)
            throws NoSuchAlgorithmException, InvalidKeyException {
        if (algorithm.startsWith("HMAC")) {
            Mac mac = Mac.getInstance(algorithm);
            SecretKeySpec keySpec = new SecretKeySpec(accessKeySecret.getBytes(StandardCharsets.UTF_8), algorithm);
            mac.init(keySpec);
            return Hex.encodeHexString(mac.doFinal(data));
        } else {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            return Hex.encodeHexString(digest.digest(data));
        }
    }

    private static byte[] hmacSign(String key, byte[] data, String algorithm)
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(algorithm);
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algorithm));
        return mac.doFinal(data);
    }

    private static String buildPathAndParams(String path, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return path;
        }

        List<Map.Entry<String, String>> sortedParams = new ArrayList<>(params.entrySet());
        sortedParams.sort(Map.Entry.comparingByKey());

        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<String, String> entry : sortedParams) {
            String encodedKey = URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8);
            String encodedValue = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
            joiner.add(String.format("%s=%s", encodedKey, encodedValue));
        }

        return path + "?" + joiner;
    }

    public static boolean verifySignature(AuthenticationParam params,
            String decryptAccessKeySecret) {
        try {
            String expectedSignature = params.getSignature();
            String calculatedSignature = generateSecureSignature(params.getHost(), params.getPath(), params.getMethod(),
                    params.getQueryParams(), params.getBody(), decryptAccessKeySecret, params.getHeaderDate(),
                    params.getNonce(), params.getAlgorithm());
            return expectedSignature.equals(calculatedSignature);
        } catch (Exception e) {
            log.error("Failed to verify signature", e);
            return false;
        }
    }


    public static String generateNonce() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }


    public static boolean isTimestampValid(String rfcDate, long maxTimeSkewSeconds) {
        try {
            java.time.format.DateTimeFormatter formatter =
                    java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
            java.time.ZonedDateTime requestTime =
                    java.time.ZonedDateTime.parse(rfcDate, formatter);
            java.time.ZonedDateTime now = java.time.ZonedDateTime.now();

            long diffSeconds = Math.abs(java.time.Duration.between(requestTime, now).getSeconds());
            return diffSeconds <= maxTimeSkewSeconds;
        } catch (Exception e) {
            log.error("Failed to parse timestamp", e);
            return false;
        }
    }


    public static String buildAuthorizationHeader(String accessKeyId, String signature,
            String algorithm) {
        return String.format("ODC-ACCESS-KEY-%s %s:%s",
                algorithm, accessKeyId, signature);
    }

    public static AuthorizationInfo parseAuthorizationHeader(String authorization) {
        if (authorization == null || !authorization.startsWith("ODC-ACCESS-KEY-")) {
            return null;
        }

        try {
            // format: "ODC-ACCESS-KEY-HMACSHA256 test-access-key:test-signature"
            String[] parts = authorization.split(" ", 2); // 最多分割2次
            if (parts.length != 2) {
                return null;
            }

            String algorithm = parts[0].substring("ODC-ACCESS-KEY-".length());
            String[] credentials = parts[1].split(":");
            if (credentials.length != 2) {
                return null;
            }

            return AuthorizationInfo.builder()
                    .algorithm(algorithm)
                    .accessKeyId(credentials[0])
                    .signature(credentials[1])
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse authorization header", e);
            return null;
        }
    }


    @Setter
    @Getter
    public static class AuthorizationInfo {
        private String algorithm;
        private String accessKeyId;
        private String signature;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private final AuthorizationInfo info = new AuthorizationInfo();

            public Builder algorithm(String algorithm) {
                info.algorithm = algorithm;
                return this;
            }

            public Builder accessKeyId(String accessKeyId) {
                info.accessKeyId = accessKeyId;
                return this;
            }

            public Builder signature(String signature) {
                info.signature = signature;
                return this;
            }

            public AuthorizationInfo build() {
                return info;
            }
        }
    }
}
