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
import java.nio.file.Paths;
import java.util.Map;

import com.oceanbase.odc.test.tool.EncryptableConfigurations;

/**
 * @author gaoda.xy
 * @date 2023/2/16 17:19
 */
public class TestProperties {
    private static final String TEST_CONFIG_FILE;
    private static final Map<String, String> properties;

    static {
        try {
            URL location = TestProperties.class.getProtectionDomain().getCodeSource().getLocation();
            TEST_CONFIG_FILE = Paths.get(location.toURI())
                    .getParent().getParent().getParent().getParent()
                    .resolve("builds").resolve(".env").toString();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        properties = EncryptableConfigurations.loadProperties(TEST_CONFIG_FILE);
    }

    public static String getProperty(String key) {
        return properties.get(key);
    }
}
