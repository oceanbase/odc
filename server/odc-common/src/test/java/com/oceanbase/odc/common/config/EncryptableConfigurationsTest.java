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
package com.oceanbase.odc.common.config;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EncryptableConfigurationsTest {
    private static final String TEST_CONFIG_FILE = "src/test/resources/test-encrypt-configuration.properties";
    private static final String GENERATE_INPUT_FILE = "src/test/resources/generate-input.properties";

    @Test
    public void loadProperties_WithEncryptedValue() {
        Map<String, String> load = EncryptableConfigurations.loadProperties(TEST_CONFIG_FILE);

        Map<String, String> expected = new HashMap<>();
        expected.put("key1", "value1");
        expected.put("key2", "value2");

        Assert.assertEquals(expected, load);
    }

    @Test
    public void encryptIfRequires_ValueContainsENC() throws ConfigurationException, IOException {
        EncryptableConfigurations.encryptFileIfRequires(GENERATE_INPUT_FILE);

        String fileContent = FileUtils.readFileToString(new File(GENERATE_INPUT_FILE));

        Assert.assertTrue(fileContent.contains("ENC@"));
    }
}
