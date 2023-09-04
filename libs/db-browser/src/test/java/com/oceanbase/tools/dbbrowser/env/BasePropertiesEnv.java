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
package com.oceanbase.tools.dbbrowser.env;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Base64;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.oceanbase.tools.dbbrowser.util.StringUtils;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link BasePropertiesEnv}
 * 
 * @author yh263208
 * @date 2023-02-21 14:08
 * @since db-browser_1.0.0-SNAPSHOT
 */
@Slf4j
public abstract class BasePropertiesEnv {

    private static final String TEST_CONFIG_FILE = "../../builds/local-unit-test.properties";
    private static final String ENCRYPTED_PREFIX = "ENC@";
    private static final Properties PROPERTIES = new Properties();

    static {
        try {
            PROPERTIES.load(new StringReader(readFromFile(new File(TEST_CONFIG_FILE))));
        } catch (IOException e) {
            log.warn("Failed to read content");
            throw new IllegalStateException(e);
        }
        decryptIfRequired();
    }

    public static String get(@NonNull String key) {
        return PROPERTIES.getProperty(key);
    }

    public static Set<String> getKeys() {
        return PROPERTIES.keySet().stream().map(Object::toString).collect(Collectors.toSet());
    }

    private static String readFromFile(File file) throws IOException {
        try (FileInputStream input = new FileInputStream(file)) {
            int len = input.available();
            byte[] buffer = new byte[len];
            if (len != input.read(buffer)) {
                throw new IllegalStateException("Failed to read");
            }
            return new String(buffer);
        }
    }

    private static void decryptIfRequired() {
        SecretKey secretKey = new SecretKey();
        for (Object key : PROPERTIES.keySet()) {
            String value = PROPERTIES.get(key).toString();
            if (!StringUtils.startsWith(value, ENCRYPTED_PREFIX)) {
                continue;
            }
            value = value.substring(ENCRYPTED_PREFIX.length());
            byte[] encrypted = Base64.getDecoder().decode(value.getBytes());
            byte[] decrypted = decrypt(encrypted, secretKey.getSecretKey().getBytes());
            PROPERTIES.put(key, new String(decrypted));
        }
    }

    public static byte[] decrypt(byte[] encrypted, byte[] password) {
        Validate.notNull(encrypted, "null input for decrypt");
        try {
            Cipher cipher = Cipher.getInstance("Blowfish/ECB/NoPadding");
            SecretKeySpec key = new SecretKeySpec(password, "Blowfish");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypted = cipher.doFinal(encrypted);
            return zeroUnpad(decrypted);
        } catch (Exception e) {
            String rootCauseMessage = ExceptionUtils.getRootCauseMessage(e);
            throw new RuntimeException(rootCauseMessage);
        }
    }

    private static byte[] zeroUnpad(byte[] decrypted) {
        int end = decrypted.length;
        while (end > 0) {
            if (decrypted[end - 1] == 0) {
                end--;
            } else {
                break;
            }
        }
        return Arrays.copyOf(decrypted, end);
    }

    private static class SecretKey {

        private static final String ENV_FILE = "../../.env";
        private static final String SECRET_ENV_KEY = "ODC_CONFIG_SECRET";
        private static final String SECRET_ENV_ACI_KEY = "ACI_VAR_ODC_CONFIG_SECRET";
        private final Properties envProperties;

        public SecretKey() {
            envProperties = getEnvProperties();
        }

        public String getSecretKey() {
            String secretKey = getSystemProperty(SECRET_ENV_KEY);
            if (org.apache.commons.lang3.StringUtils.isNotBlank(secretKey)) {
                return secretKey;
            }
            secretKey = getSystemProperty(SECRET_ENV_ACI_KEY);
            if (org.apache.commons.lang3.StringUtils.isNotBlank(secretKey)) {
                return secretKey;
            }
            throw new RuntimeException("environment variable 'ODC_CONFIG_SECRET' is not set");
        }

        private Properties getEnvProperties() {
            Properties properties = new Properties();
            File file = new File(ENV_FILE);
            if (file.exists()) {
                try (FileInputStream inputStream = new FileInputStream(file)) {
                    properties.load(inputStream);
                } catch (IOException e) {
                    log.warn("load .env failed, reason={}", e.getMessage());
                }
            } else {
                log.info("skip load due .env file not exists");
            }
            return properties;
        }

        private String getSystemProperty(String key) {
            String property = System.getProperty(key);
            if (org.apache.commons.lang3.StringUtils.isNoneBlank(property)) {
                return property;
            }
            property = System.getenv(key);
            if (org.apache.commons.lang3.StringUtils.isNotBlank(property)) {
                return property;
            }
            property = envProperties.getProperty(key);
            if (org.apache.commons.lang3.StringUtils.isNotBlank(property)) {
                return property;
            }
            return null;
        }
    }

}
