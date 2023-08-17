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
package com.oceanbase.odc.common.util;

import org.junit.Assert;
import org.junit.Test;

public class UrlUtilTest {

    @Test
    public void addParam_withQusetionMark() {
        String redirectUrl = "http://example.org?a=2";
        String s = UrlUtils.appendQueryParameter(redirectUrl, "b", "3");
        String[] split = s.split("\\?");
        Assert.assertEquals(2, split.length);
    }

    @Test
    public void addParam_withoutQusetionMark() {
        String redirectUrl = "http://example.org";
        String s = UrlUtils.appendQueryParameter(redirectUrl, "b", "3");
        String[] split = s.split("\\?");
        Assert.assertEquals(2, split.length);
    }

}
