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
}
