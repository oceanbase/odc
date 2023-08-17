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

public class HashUtilsTest {

    @Test
    public void sha1_Bytes() {
        String hash = HashUtils.sha1("123456".getBytes(StandardCharsets.UTF_8));
        Assert.assertEquals("7c4a8d09ca3762af61e59520943dc26494f8941b", hash);
    }

    @Test
    public void sha1_String() {
        String hash = HashUtils.sha1("123456");
        Assert.assertEquals("7c4a8d09ca3762af61e59520943dc26494f8941b", hash);
    }

    @Test
    public void sha256_String() {
        String hash = HashUtils.sha256("123456");
        Assert.assertEquals("8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92", hash);
    }

    @Test
    public void md5_Bytes() {
        String hash = HashUtils.md5("123456".getBytes(StandardCharsets.UTF_8));
        Assert.assertEquals("e10adc3949ba59abbe56e057f20f883e", hash);
    }

    @Test
    public void md5_String() {
        String hash = HashUtils.md5("123456");
        Assert.assertEquals("e10adc3949ba59abbe56e057f20f883e", hash);
    }
}
