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

    @Test
    public void testExtractServerAddress_IPv6_ValidExpression() {
        String ipAndPort = "[2001:db8::1]:8080";
        ServerAddress actual = HostUtils.extractServerAddress(ipAndPort);
        Assert.assertEquals("2001:db8::1", actual.getIpAddress());
        Assert.assertEquals("8080", actual.getPort());
    }

    @Test
    public void testExtractServerAddress_IPv6_Localhost() {
        String ipAndPort = "[::1]:3306";
        ServerAddress actual = HostUtils.extractServerAddress(ipAndPort);
        Assert.assertEquals("::1", actual.getIpAddress());
        Assert.assertEquals("3306", actual.getPort());
    }

    @Test
    public void testExtractServerAddress_IPv6_FullAddress() {
        String ipAndPort = "[2001:0db8:85a3:0000:0000:8a2e:0370:7334]:443";
        ServerAddress actual = HostUtils.extractServerAddress(ipAndPort);
        Assert.assertEquals("2001:0db8:85a3:0000:0000:8a2e:0370:7334", actual.getIpAddress());
        Assert.assertEquals("443", actual.getPort());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractServerAddress_IPv6_MissingCloseBracket() {
        String ipAndPort = "[2001:db8::1:8080";
        HostUtils.extractServerAddress(ipAndPort);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractServerAddress_IPv6_EmptyAddress() {
        String ipAndPort = "[:8080";
        HostUtils.extractServerAddress(ipAndPort);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractServerAddress_IPv6_NoPort() {
        String ipAndPort = "[2001:db8::1]";
        HostUtils.extractServerAddress(ipAndPort);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractServerAddress_IPv6_EmptyPort() {
        String ipAndPort = "[2001:db8::1]:";
        HostUtils.extractServerAddress(ipAndPort);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractServerAddress_IPv6_InvalidPortFormat() {
        String ipAndPort = "[2001:db8::1]8080";
        HostUtils.extractServerAddress(ipAndPort);
    }

    @Test
    public void testExtractServerAddress_IPv6WithBrackets() {
        String ipAndPort = "[::1]:8080";
        ServerAddress actual = HostUtils.extractServerAddress(ipAndPort);
        Assert.assertEquals("::1", actual.getIpAddress());
        Assert.assertEquals("8080", actual.getPort());
    }

    @Test
    public void testExtractServerAddress_IPv6ComplexAddress() {
        String ipAndPort = "[2001:db8:85a3::8a2e:370:7334]:9090";
        ServerAddress actual = HostUtils.extractServerAddress(ipAndPort);
        Assert.assertEquals("2001:db8:85a3::8a2e:370:7334", actual.getIpAddress());
        Assert.assertEquals("9090", actual.getPort());
    }

    @Test
    public void testExtractServerAddress_IPv6WithZoneId() {
        String ipAndPort = "[fe80::1%lo0]:3306";
        ServerAddress actual = HostUtils.extractServerAddress(ipAndPort);
        Assert.assertEquals("fe80::1%lo0", actual.getIpAddress());
        Assert.assertEquals("3306", actual.getPort());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractServerAddress_IPv6MissingCloseBracket() {
        String ipAndPort = "[::1:8080";
        HostUtils.extractServerAddress(ipAndPort);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractServerAddress_IPv6MissingPort() {
        String ipAndPort = "[::1]";
        HostUtils.extractServerAddress(ipAndPort);
    }
}
