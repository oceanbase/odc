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
package com.oceanbase.odc.service.common;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import com.oceanbase.odc.service.common.util.WebRequestUtils;

public class WebRequestUtilsTest {

    @Test
    public void isRedirectUrlValid_OnlyPath_Valid() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        boolean redirectUrlValid = WebRequestUtils.isRedirectUrlValid(request, "/hello");

        Assert.assertTrue(redirectUrlValid);
    }

    @Test
    public void isRedirectUrlValid_Null_Invalid() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        boolean redirectUrlValid = WebRequestUtils.isRedirectUrlValid(request, null);

        Assert.assertFalse(redirectUrlValid);
    }

    @Test
    public void isRedirectUrlValid_HostNotMatch_InValid() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "http://localhost:8989");

        boolean redirectUrlValid = WebRequestUtils.isRedirectUrlValid(request, "http://another:8989/hello");

        Assert.assertFalse(redirectUrlValid);
    }

    @Test
    public void testGetTrulyClientIp() {
        Map<String, String> tables = new HashMap<String, String>() {
            {
                put("", "N/A");
                put(null, "N/A");
                put("unknown", "N/A");
                put("UNKNOWN", "N/A");
                put("123", "123");
                put("192.168.1.1", "192.168.1.1");
                put(",192.168.1.1", "192.168.1.1");
                put("192.168.1.1,122.122.1.1,127.0.0.1", "192.168.1.1");
                put("unknown,192.168.1.1,122.122.1.1,127.0.0.1", "192.168.1.1");
            }
        };

        for (Map.Entry<String, String> entry : tables.entrySet()) {
            String input = entry.getKey();
            String except = entry.getValue();
            String trulyClientIp = WebRequestUtils.getTrulyClientIp(input);
            Assert.assertEquals(except, trulyClientIp);
        }
    }
}
