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
package com.oceanbase.odc.service.connection;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.service.connection.util.ConnectionPasswordUtil;

/**
 * @author wenniu.ly
 * @date 2021/1/25
 */
public class ConnectionPasswordUtilTest {

    @Test
    public void testNormal() {
        String password = "123456abc";
        String quotedResult = ConnectionPasswordUtil.wrapPassword(password);
        Assert.assertEquals("'123456abc'", quotedResult);
    }

    @Test
    public void testSpecialCharacter() {
        String password = "1$3&56abc";
        String quotedResult = ConnectionPasswordUtil.wrapPassword(password);
        Assert.assertEquals("'1$3&56abc'", quotedResult);
    }

    @Test
    public void testSingleQuote() {
        String password = "1'2'3'''";
        String quotedResult = ConnectionPasswordUtil.wrapPassword(password);
        Assert.assertEquals("'1''2''3'''''''", quotedResult);
    }
}
