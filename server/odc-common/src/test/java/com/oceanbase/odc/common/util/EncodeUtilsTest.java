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

import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

public class EncodeUtilsTest {

    @Test
    public void base64_common() {
        String src = "123456";
        String encoded = EncodeUtils.base64EncodeToString(src.getBytes(StandardCharsets.UTF_8));
        byte[] bytes = EncodeUtils.base64DecodeFromString(encoded);
        Assert.assertEquals("123456", new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    public void base64_empty() {
        String src = "";
        String encoded = EncodeUtils.base64EncodeToString(src.getBytes(StandardCharsets.UTF_8));
        byte[] bytes = EncodeUtils.base64DecodeFromString(encoded);
        Assert.assertEquals("", new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    public void base64_zh_CN() {
        String src = "中文测试";
        String encoded = EncodeUtils.base64EncodeToString(src.getBytes(StandardCharsets.UTF_8));
        byte[] bytes = EncodeUtils.base64DecodeFromString(encoded);
        Assert.assertEquals("中文测试", new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    public void base64Encode_null() {
        String encoded = EncodeUtils.base64EncodeToString(null);
        Assert.assertNull(encoded);
    }

    @Test
    public void base64Decode_null() {
        byte[] bytes = EncodeUtils.base64DecodeFromString(null);
        Assert.assertNull(bytes);
    }
}
