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
package com.oceanbase.odc.common.security;

import java.util.Arrays;
import java.util.Collection;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(Parameterized.class)
public class SensitiveDataUtilsTest {
    @Parameter(0)
    public String message;
    @Parameter(1)
    public String expectedMasked;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Parameters(name = "{index}: masked value for {0} expected {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {"nomask", "nomask"},
                {"password=123456", "password=***"},
                {"-p 123456", "-p ***"},
                {"-p123456", "-p***"},
                {"somepassword=123456", "somepassword=***"},
                {"PASSWORD=123456", "PASSWORD=***"},
                {"password = 123456", "password = ***"},
                {"password=123456,secret=654321", "password=***,secret=***"},
                {"\"password\"=\"123456\"", "\"password\"=\"***\""},
                {"\"password\" : \"123456\"", "\"password\" : \"***\""},
                {"hello:{\"password\"=\"123456\",\"secret\"=\"654321\"}",
                        "hello:{\"password\"=\"***\",\"secret\"=\"***\"}"}
        });
    }

    @Parameters(name = "{index}: masked value for {0} expected {1}")
    public static Collection<Object[]> jsonData() {
        return Arrays.asList(new Object[][] {
                {"nomask", "nomask"},
                {"password=123456", "password=***"},
                {"-p 123456", "-p ***"},
                {"-p123456", "-p***"},
                {"somepassword=123456", "somepassword=***"},
                {"PASSWORD=123456", "PASSWORD=***"},
                {"password = 123456", "password = ***"},
                {"password=123456,secret=654321", "password=***,secret=***"},
                {"\"password\"=\"123456\"", "\"password\"=\"***\""},
                {"\"password\" : \"123456\"", "\"password\" : \"***\""},
                {"hello:{\"password\"=\"123456\",\"secret\"=\"654321\"}",
                        "hello:{\"password\"=\"***\",\"secret\"=\"***\"}"}
        });
    }


    @Test
    public void mask() {
        String masked = SensitiveDataUtils.mask(message);
        Assert.assertEquals(expectedMasked, masked);
    }

    @Test
    public void testMaskJsonWithSensitiveKey() throws Exception {
        String json = "{\"password\":\"123456\", \"username\":\"user1\"}";
        String expectedJson = "{\"password\":\"***\", \"username\":\"user1\"}";

        String result = SensitiveDataUtils.maskJson(json);
        Assert.assertEquals(objectMapper.readTree(expectedJson), objectMapper.readTree(result));
    }

    @Test
    public void testMaskJsonWithSensitiveValue() throws Exception {
        String json = "{\"apiKey\":\"mySecretKey123\", \"email\":\"test@example.com\"}";
        String expectedJson = "{\"apiKey\":\"***\", \"email\":\"***\"}";

        String result = SensitiveDataUtils.maskJson(json);
        Assert.assertEquals(objectMapper.readTree(expectedJson), objectMapper.readTree(result));
    }

    @Test
    public void testMaskJsonWithoutSensitiveData() throws Exception {
        String json = "{\"name\":\"John\", \"age\":30}";
        String expectedJson = "{\"name\":\"John\", \"age\":30}";

        String result = SensitiveDataUtils.maskJson(json);
        Assert.assertEquals(objectMapper.readTree(expectedJson), objectMapper.readTree(result));
    }

    @Test
    public void testMaskJsonWithNestedSensitiveData() throws Exception {
        String json = "{\"user\":{\"password\":\"mypassword\", \"details\":{\"email\":\"private@example.com\"}}}";
        String expectedJson = "{\"user\":{\"password\":\"***\", \"details\":{\"email\":\"***\"}}}";

        String result = SensitiveDataUtils.maskJson(json);
        Assert.assertEquals(objectMapper.readTree(expectedJson), objectMapper.readTree(result));
    }

    @Test
    public void testMaskJsonArrayWithSensitiveData() throws Exception {
        String json = "[{\"password222\":\"abc\"}, {\"email\":\"email@domain.com\"}]";
        String expectedJson = "[{\"password222\":\"***\"}, {\"email\":\"***\"}]";

        String result = SensitiveDataUtils.maskJson(json);
        Assert.assertEquals(objectMapper.readTree(expectedJson), objectMapper.readTree(result));
    }

    @Test
    public void testMaskJsonWithVlueSensitiveData() throws Exception {
        String json = "[{\"nomask\":\"password=123\"}, {\"nomask2\":\"email@domain.com\"}]";
        String expectedJson = "[{\"nomask\":\"password=***\"}, {\"nomask2\":\"email***\"}]";

        String result = SensitiveDataUtils.maskJson(json);
        Assert.assertEquals(objectMapper.readTree(expectedJson), objectMapper.readTree(result));
    }

    @Test
    public void testMaskJsonTooDeep() throws Exception {
        String json =
                "{\"user\":{\"name\":\"Alice\",\"age\":30,\"contact\":{\"email\":\"alice@example.com\",\"phone\":\"123-456-7890\"}}}";
        String message = "MESSAGE_MASK_FAILED, origin message start with " + json.substring(0, 10);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("msg", message);
        String expectedJson = jsonObject.toString();

        String result = SensitiveDataUtils.maskJson(json, 1);
        Assert.assertEquals(objectMapper.readTree(expectedJson), objectMapper.readTree(result));
    }
}
