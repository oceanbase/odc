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
package com.oceanbase.odc.test.database;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.oceanbase.odc.test.tool.EncryptableConfigurations;

/**
 * @author gaoda.xy
 * @date 2023/2/16 17:19
 */
public class TestProperties {

    public static final String TEST_OB_ORACLE_COMMAND_LINE_ENV_KEY = "TEST_OB_ORACLE_COMMAND_LINE";
    public static final String TEST_OB_ORACLE_SYS_USERNAME_ENV_KEY = "TEST_OB_ORACLE_SYS_USERNAME";
    public static final String TEST_OB_ORACLE_SYS_PASSWORD_ENV_KEY = "TEST_OB_ORACLE_SYS_PASSWORD";
    public static final String TEST_OB_MYSQL_COMMAND_LINE_ENV_KEY = "TEST_OB_MYSQL_COMMAND_LINE";
    public static final String TEST_OB_MYSQL_SYS_USERNAME_ENV_KEY = "TEST_OB_MYSQL_SYS_USERNAME";
    public static final String TEST_OB_MYSQL_SYS_PASSWORD_ENV_KEY = "TEST_OB_MYSQL_SYS_PASSWORD";
    public static final String TEST_MYSQL_COMMAND_LINE_ENV_KEY = "TEST_MYSQL_COMMAND_LINE";

    private static final Map<String, String> PROPERTIES_ENV_MAP = new HashMap<>();
    private static final Map<String, String> TEST_PROPERTIES;


    static {
        Path envFile = getEnvFile();
        TEST_PROPERTIES =
                Files.exists(envFile) ? EncryptableConfigurations.loadProperties(envFile.toString()) : new HashMap<>();

        initPropertiesEnvMap();
        // If environment variable value is not null will cover the properties value
        // with same key in .env file
        PROPERTIES_ENV_MAP.forEach((key, value) -> {
            if (System.getenv(value) != null) {
                TEST_PROPERTIES.put(key, System.getenv(value));
            }
        });
        checkTestPropertiesValueIsNotEmpty();
    }

    public static String getProperty(String key) {

        return TEST_PROPERTIES.get(key);
    }

    private static Path getEnvFile() {
        Path envFile;
        try {
            URL location = TestProperties.class.getProtectionDomain().getCodeSource().getLocation();
            envFile = Paths.get(location.toURI())
                    .getParent().getParent().getParent().getParent()
                    .resolve("builds").resolve(".env");

        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        return envFile;
    }

    private static void checkTestPropertiesValueIsNotEmpty() {
        if (TEST_PROPERTIES.isEmpty()) {
            throw new IllegalArgumentException(
                    "Test properties is unset, set it by .env or system environment variables.");
        }

        PROPERTIES_ENV_MAP.forEach((k, v) -> {
            if (TEST_PROPERTIES.get(k) == null) {
                throw new IllegalArgumentException(
                        String.format("Test properties %s is unset, set it by .env or system environment variables.",
                                k));
            }
        });
    }

    private static void initPropertiesEnvMap() {
        PROPERTIES_ENV_MAP.put("odc.ob.default.oracle.commandline", TEST_OB_ORACLE_COMMAND_LINE_ENV_KEY);
        PROPERTIES_ENV_MAP.put("odc.ob.default.oracle.sysUsername", TEST_OB_ORACLE_SYS_USERNAME_ENV_KEY);
        PROPERTIES_ENV_MAP.put("odc.ob.default.oracle.sysPassword", TEST_OB_ORACLE_SYS_PASSWORD_ENV_KEY);
        PROPERTIES_ENV_MAP.put("odc.ob.default.mysql.commandline", TEST_OB_MYSQL_COMMAND_LINE_ENV_KEY);
        PROPERTIES_ENV_MAP.put("odc.ob.default.mysql.sysUsername", TEST_OB_MYSQL_SYS_USERNAME_ENV_KEY);
        PROPERTIES_ENV_MAP.put("odc.ob.default.mysql.sysPassword", TEST_OB_MYSQL_SYS_PASSWORD_ENV_KEY);
        PROPERTIES_ENV_MAP.put("odc.mysql.default.commandline", TEST_MYSQL_COMMAND_LINE_ENV_KEY);
    }
}
