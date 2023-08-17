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
package com.oceanbase.odc.service.oss.model;

import java.util.Base64;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author wenniu.ly
 * @date 2020/12/17
 */
public class EncryptAlgorithmTest {

    /*
     * 单测验证java.util的Base64 decode&encode结果和sun.misc结果一致
     */
    @Test
    public void testEncode() {
        String input = "OceanBase No.1";
        String encodeByJavaUtil = Base64.getEncoder().encodeToString(input.getBytes());
        Assert.assertEquals("T2NlYW5CYXNlIE5vLjE=", encodeByJavaUtil);
    }

    @Test
    public void testDecode() {
        String input = "T2NlYW5CYXNlIE5vLjE=";
        byte[] decodeByJavaUtil = Base64.getDecoder().decode(input);

        Assert.assertArrayEquals("OceanBase No.1".getBytes(), decodeByJavaUtil);
    }
}
