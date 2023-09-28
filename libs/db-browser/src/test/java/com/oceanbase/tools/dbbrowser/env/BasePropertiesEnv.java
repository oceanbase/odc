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
import java.util.Base64;
import java.util.Properties;

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

    private static final String TEST_CONFIG_FILE = "../../local-unit-test.properties";
    private static final String ENCRYPTED_PREFIX = "ENC@";
    private static final Properties PROPERTIES = new Properties();
    private static final AesBytesEncryptor DECRYPTOR = new AesBytesEncryptor(new SecretKey().getSecretKey(), null, 256);

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
        String property = PROPERTIES.getProperty(key);
        if (StringUtils.isNotBlank(property)) {
            return property;
        }
        // Get from environment variable
        key = StringUtils.replace(key, ".", "_").toUpperCase();
        property = PropertiesUtil.getSystemProperty(key);
        if (StringUtils.isNotBlank(property)) {
            return decryptIfRequired(property);
        }
        return null;
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
        PROPERTIES.replaceAll((k, v) -> decryptIfRequired(PROPERTIES.get(k).toString()));
    }

    private static String decryptIfRequired(String value) {
        if (!StringUtils.startsWith(value, ENCRYPTED_PREFIX)) {
            return value;
        }
        value = value.substring(ENCRYPTED_PREFIX.length());
        byte[] encrypted = Base64.getDecoder().decode(value.getBytes());
        byte[] decrypted = DECRYPTOR.decrypt(encrypted);
        return new String(decrypted);
    }

    private static class SecretKey {

        private static final String SECRET_ENV_KEY = "ODC_CONFIG_SECRET";

        public SecretKey() {}

        public String getSecretKey() {
            String secretKey = PropertiesUtil.getSystemProperty(SECRET_ENV_KEY);
            if (StringUtils.isNotBlank(secretKey)) {
                return secretKey;
            }
            throw new RuntimeException("environment variable 'ODC_CONFIG_SECRET' is not set");
        }

    }

    private static class PropertiesUtil {

        private static final String ENV_FILE = "../../.env";
        private static final Properties ENV_PROPERTIES;

        static {
            ENV_PROPERTIES = getEnvProperties();
        }

        public static String getSystemProperty(String key) {
            String property = System.getProperty(key);
            if (StringUtils.isNotBlank(property)) {
                return property;
            }
            property = System.getenv(key);
            if (StringUtils.isNotBlank(property)) {
                return property;
            }
            property = ENV_PROPERTIES.getProperty(key);
            if (StringUtils.isNotBlank(property)) {
                return property;
            }
            return null;
        }

        private static Properties getEnvProperties() {
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

    }

}
