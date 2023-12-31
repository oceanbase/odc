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

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author gaoda.xy
 * @date 2023/9/28 10:24
 */
public class EncryptableConfigurationsTest {
    private static final String TEST_CONFIG_FILE = "src/test/resources/test-encrypt-configuration.properties";

    @Test
    public void loadProperties_WithEncryptedValue() {
        Map<String, String> load = EncryptableConfigurations.loadProperties(TEST_CONFIG_FILE);

        Map<String, String> expected = new HashMap<>();
        expected.put("key1", "oceanbase developer center");
        expected.put("key2", "build the best database develop platform");

        Assert.assertEquals(expected, load);
    }

}
