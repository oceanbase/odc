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
package com.oceanbase.odc.service.audit.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class AuditUtilsTest {
    @Parameter(0)
    public String input;
    @Parameter(1)
    public String except;

    @Parameters(name = "{index}: getFirstIpFromRemoteAddress({0})={1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {"", "N/A"},
                {null, "N/A"},
                {"unknown", "N/A"},
                {"UNKNOWN", "N/A"},
                {"123", "123"},
                {"192.168.1.1", "192.168.1.1"},
                {",192.168.1.1", "192.168.1.1"},
                {"192.168.1.1,122.122.1.1,127.0.0.1", "192.168.1.1"},
                {"unknown,192.168.1.1,122.122.1.1,127.0.0.1", "192.168.1.1"}
        });
    }

    @Test
    public void getFirstIpFromRemoteAddress() {
        assertEquals(except, AuditUtils.getFirstIpFromRemoteAddress(input));
    }
}
