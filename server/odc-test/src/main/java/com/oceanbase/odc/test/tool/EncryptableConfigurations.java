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
package com.oceanbase.odc.test.tool;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.PropertiesConfigurationLayout;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;

import com.oceanbase.odc.test.crypto.Encryptors;
import com.oceanbase.odc.test.crypto.TextEncryptor;
import com.oceanbase.odc.test.database.TestProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2023/2/17 10:20
 */
@Slf4j
public class EncryptableConfigurations {
    private static final Pattern ENCRYPT_KEY_PATTERN =
            Pattern.compile(".*(password|username|host|port|commandline|key|secret).*", CASE_INSENSITIVE);
    private static final String ENCRYPTED_PREFIX = "ENC@";
    private static final EncryptablePropertyDetector encryptableDetector;
    private static final TextEncryptor valueEncryptor;

    static {
        encryptableDetector = new EncryptablePropertyDetector();
        valueEncryptor = Encryptors.aes256Base64(new SecretKeyGetter().getSecretKey(), "");
    }

    public static Map<String, String> loadProperties(String path) {
        encryptFileIfRequires(path);
        File file = new File(path);
        PropertiesConfiguration config = new PropertiesConfiguration();
        PropertiesConfigurationLayout layout = new PropertiesConfigurationLayout();
        try {
            layout.load(config, new FileReader(file));
        } catch (ConfigurationException | FileNotFoundException e) {
            throw new RuntimeException("load properties file failed:", e);
        }
        Map<String, String> properties = new HashMap<>();
        Set<String> keys = layout.getKeys();
        for (String key : keys) {
            String value = config.getProperty(key).toString();
            value = decryptIfRequired(value);
            properties.put(key, value);
        }
        return properties;
    }

    public static void encryptFileIfRequires(String fileName) {
        File file = new File(fileName);
        PropertiesConfiguration config = new PropertiesConfiguration();
        PropertiesConfigurationLayout layout = new PropertiesConfigurationLayout();
        try {
            layout.load(config, new FileReader(file));
        } catch (ConfigurationException | FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        Set<String> keys = layout.getKeys();
        boolean detectUnencryptedValue = false;
        for (String key : keys) {
            String value = config.getProperty(key).toString();
            if (ENCRYPT_KEY_PATTERN.matcher(key).matches()
                    && StringUtils.isNotBlank(value)
                    && !encryptableDetector.isEncrypted(value)) {
                String encryptedValue = ENCRYPTED_PREFIX + valueEncryptor.encrypt(value);
                config.setProperty(key, encryptedValue);
                detectUnencryptedValue = true;
            }
        }
        if (detectUnencryptedValue) {
            log.info("detect unencrypted value, encrypt and save it, fileName={}", fileName);
            try {
                layout.save(config, new FileWriter(file));
            } catch (ConfigurationException | IOException e) {
                throw new RuntimeException("save properties file failed:", e);
            }
        }
    }

    private static String decryptIfRequired(String value) {
        if (!encryptableDetector.isEncrypted(value)) {
            return value;
        }
        return valueEncryptor.decrypt(encryptableDetector.unwrapEncryptedValue(value));
    }

    private static class EncryptablePropertyDetector {
        public boolean isEncrypted(String value) {
            if (value != null) {
                return value.startsWith(ENCRYPTED_PREFIX);
            }
            return false;
        }

        public String unwrapEncryptedValue(String value) {
            return value.substring(ENCRYPTED_PREFIX.length());
        }
    }

    private static class SecretKeyGetter {
        private static final String SECRET_ENV_KEY = "ODC_CONFIG_SECRET";
        private static final String SECRET_ENV_ACI_KEY = "ACI_VAR_ODC_CONFIG_SECRET";
        private static final String SECRET_ENV_GITHUB_KEY = "GITHUB_ACTION_ODC_CONFIG_SECRET";
        private final Properties envProperties;

        public SecretKeyGetter() {
            envProperties = getEnvProperties();
        }

        public String getSecretKey() {
            String secretKey = getSystemProperty(SECRET_ENV_KEY);
            if (StringUtils.isNotBlank(secretKey)) {
                return secretKey;
            }
            secretKey = getSystemProperty(SECRET_ENV_GITHUB_KEY);
            if (StringUtils.isNotBlank(secretKey)) {
                return secretKey;
            }
            secretKey = getSystemProperty(SECRET_ENV_ACI_KEY);
            if (StringUtils.isNotBlank(secretKey)) {
                return secretKey;
            }
            throw new RuntimeException("environment variable 'ODC_CONFIG_SECRET' is not set");
        }

        private Properties getEnvProperties() {
            Properties properties = new Properties();
            File file;
            try {
                URL location = TestProperties.class.getProtectionDomain().getCodeSource().getLocation();
                file = Paths.get(location.toURI())
                        .getParent().getParent().getParent().getParent()
                        .resolve(".env").toFile();
            } catch (URISyntaxException e) {
                throw new IllegalStateException(e);
            }
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
            if (StringUtils.isNoneBlank(property)) {
                return property;
            }
            property = System.getenv(key);
            if (StringUtils.isNotBlank(property)) {
                return property;
            }
            property = envProperties.getProperty(key);
            if (StringUtils.isNotBlank(property)) {
                return property;
            }
            return null;
        }
    }
}
