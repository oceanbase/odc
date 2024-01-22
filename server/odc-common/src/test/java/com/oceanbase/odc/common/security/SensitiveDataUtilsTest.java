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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class SensitiveDataUtilsTest {
    @Parameter(0)
    public String message;
    @Parameter(1)
    public String expectedMasked;

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

    @Test
    public void mask() {
        String masked = SensitiveDataUtils.mask(message);
        Assert.assertEquals(expectedMasked, masked);
    }
}
