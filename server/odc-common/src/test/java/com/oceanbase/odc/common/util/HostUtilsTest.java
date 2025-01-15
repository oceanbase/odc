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

import org.junit.Test;

import com.oceanbase.odc.common.util.HostUtils.ServerAddress;

import junit.framework.Assert;

public class HostUtilsTest {
    @Test
    public void testExtractServerAddress_ValidExpression() {
        String ipAndPort = "1.1.1.1:1234";
        ServerAddress actual = HostUtils.extractServerAddress(ipAndPort);
        Assert.assertEquals("1.1.1.1", actual.getIpAddress());
        Assert.assertEquals("1234", actual.getPort());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractServerAddress_InvalidExpression() {
        String ipAndPort = "1.1.1.1";
        HostUtils.extractServerAddress(ipAndPort);
    }
}
