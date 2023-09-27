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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.PropertiesConfigurationLayout;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;

import com.oceanbase.odc.test.crypto.Encryptors;
import com.oceanbase.odc.test.crypto.TextEncryptor;
import com.oceanbase.odc.test.util.PropertiesUtil;

import lombok.NoArgsConstructor;
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

    public static String getDecryptedProperty(String key) {
        String property = PropertiesUtil.getSystemEnvProperty(key);
        if (StringUtils.isNotBlank(property)) {
            return decryptIfRequired(property);
        }
        property = PropertiesUtil.getDotEnvProperties(key);
        if (StringUtils.isNotBlank(property)) {
            return decryptIfRequired(property);
        }
        return null;
    }

    private static void encryptFileIfRequires(String fileName) {
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

    @NoArgsConstructor
    private static class SecretKeyGetter {
        private static final String SECRET_ENV_KEY = "ODC_CONFIG_SECRET";
        @Deprecated
        private static final String SECRET_ENV_ACI_KEY = "ACI_VAR_ODC_CONFIG_SECRET";

        public String getSecretKey() {
            String secretKey = getProperty(SECRET_ENV_KEY);
            if (StringUtils.isNotBlank(secretKey)) {
                return secretKey;
            }
            secretKey = getProperty(SECRET_ENV_ACI_KEY);
            if (StringUtils.isNotBlank(secretKey)) {
                return secretKey;
            }
            throw new RuntimeException("environment variable 'ODC_CONFIG_SECRET' is not set");
        }

        private String getProperty(String key) {
            String property = PropertiesUtil.getSystemEnvProperty(key);
            if (StringUtils.isNoneBlank(property)) {
                return property;
            }
            property = PropertiesUtil.getDotEnvProperties(key);
            if (StringUtils.isNotBlank(property)) {
                return property;
            }
            return null;
        }
    }

}
