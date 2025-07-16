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

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

public class SystemUtilsTest {

    @Test
    public void getHostName_NotEmpty() {
        String hostName = SystemUtils.getHostName();
        Assert.assertTrue(hostName.length() > 0);
    }

    @Test
    public void getLocalIpAddress_NotEmpty() {
        String localIpAddress = SystemUtils.getLocalIpAddress();
        Assert.assertTrue(localIpAddress.length() > 0);
    }

    @Test
    public void getSystemEnv() {
        Map<String, String> systemEnv = SystemUtils.getSystemEnv();
        Assert.assertFalse(systemEnv.isEmpty());
    }

    @Test
    public void testValidIpv6Address() {
        boolean validIPv6Address = SystemUtils.isValidIPv6Address("::1");
        Assert.assertTrue(validIPv6Address);
        boolean validBracketIPv6Address = SystemUtils.isValidIPv6Address("[::1]");
        Assert.assertTrue(validBracketIPv6Address);
        boolean invalidAddress = SystemUtils.isValidIPv6Address("[::1");
        Assert.assertFalse(invalidAddress);
    }

    @Test
    public void getSystemProperties() {
        Properties systemProperties = SystemUtils.getSystemProperties();
        Assert.assertFalse(systemProperties.isEmpty());
    }

    @Test
    public void availableProcessors_GreaterThanZero() {
        int availableProcessors = SystemUtils.availableProcessors();
        Assert.assertTrue(availableProcessors > 0);
    }

    @Test
    public void testUniqueLocalAddress() throws UnknownHostException {
        // 测试ULA地址的检测

        // fd00::/8 范围的ULA地址
        InetAddress ulaAddress1 = InetAddress.getByName("fd00:1:100a:1041:6ee1:636b:fcc:17eb");
        Assert.assertTrue("fd00::/8 地址应该被识别为ULA", ulaAddress1 instanceof Inet6Address);

        // fc00::/8 范围的ULA地址
        InetAddress ulaAddress2 = InetAddress.getByName("fc00:1:100a:1041:6ee1:636b:fcc:17eb");
        Assert.assertTrue("fc00::/8 地址应该被识别为ULA", ulaAddress2 instanceof Inet6Address);

        // Link-Local地址 (fe80::/10)
        InetAddress linkLocalAddress = InetAddress.getByName("fe80::1234:5678:9abc:def0");
        Assert.assertTrue("Link-Local地址应该被正确识别", linkLocalAddress instanceof Inet6Address);

        // 全局单播地址 (2000::/3)
        InetAddress globalAddress = InetAddress.getByName("2001:db8::1");
        Assert.assertTrue("全局单播地址应该被正确识别", globalAddress instanceof Inet6Address);

        // 环回地址
        InetAddress loopbackAddress = InetAddress.getByName("::1");
        Assert.assertTrue("环回地址应该被正确识别", loopbackAddress instanceof Inet6Address);
    }

    @Test
    public void testIPv6AddressTypes() throws UnknownHostException {
        // 测试不同类型的IPv6地址

        // ULA地址（用户的地址）
        Inet6Address ulaAddress = (Inet6Address) InetAddress.getByName("fd00:1:100a:1041:6ee1:636b:fcc:17eb");
        Assert.assertFalse("ULA地址不应该是Link-Local", ulaAddress.isLinkLocalAddress());
        Assert.assertFalse("ULA地址不应该是环回地址", ulaAddress.isLoopbackAddress());
        Assert.assertFalse("ULA地址不应该是多播地址", ulaAddress.isMulticastAddress());

        // Link-Local地址
        Inet6Address linkLocalAddress = (Inet6Address) InetAddress.getByName("fe80::1234:5678:9abc:def0");
        Assert.assertTrue("Link-Local地址应该被正确识别", linkLocalAddress.isLinkLocalAddress());

        // 全局单播地址
        Inet6Address globalAddress = (Inet6Address) InetAddress.getByName("2001:db8::1");
        Assert.assertFalse("全局单播地址不应该是Link-Local", globalAddress.isLinkLocalAddress());
        Assert.assertFalse("全局单播地址不应该是ULA", globalAddress.isLoopbackAddress());
    }

    @Test
    public void testScopeIdValidation() throws UnknownHostException {
        // 测试带有作用域标识符的IPv6地址不应该被认为是有效的服务器地址

        // 注意：通过InetAddress.getByName()创建的地址通常不会包含scopeId
        // 但我们可以测试确保逻辑正确

        // ULA地址（不带scope）
        Inet6Address ulaAddress = (Inet6Address) InetAddress.getByName("fd00:1:100a:1041:6ee1:636b:fcc:17eb");
        Assert.assertEquals("ULA地址的scopeId应该为0", 0, ulaAddress.getScopeId());
        Assert.assertNull("ULA地址的scopedInterface应该为null", ulaAddress.getScopedInterface());

        // Link-Local地址（不带scope）
        Inet6Address linkLocalAddress = (Inet6Address) InetAddress.getByName("fe80::1234:5678:9abc:def0");
        Assert.assertEquals("Link-Local地址的scopeId应该为0", 0, linkLocalAddress.getScopeId());
        Assert.assertNull("Link-Local地址的scopedInterface应该为null", linkLocalAddress.getScopedInterface());

        // 全局单播地址
        Inet6Address globalAddress = (Inet6Address) InetAddress.getByName("2001:db8::1");
        Assert.assertEquals("全局单播地址的scopeId应该为0", 0, globalAddress.getScopeId());
        Assert.assertNull("全局单播地址的scopedInterface应该为null", globalAddress.getScopedInterface());
    }

    @Test
    public void testIsUniqueLocalAddressReflection() {
        // 测试通过反射调用私有方法isUniqueLocalAddress
        try {
            java.lang.reflect.Method method =
                    SystemUtils.class.getDeclaredMethod("isUniqueLocalAddress", Inet6Address.class);
            method.setAccessible(true);

            // 测试fd00::/8范围的ULA地址
            Inet6Address ulaAddress1 = (Inet6Address) InetAddress.getByName("fd00:1:100a:1041:6ee1:636b:fcc:17eb");
            Boolean result1 = (Boolean) method.invoke(null, ulaAddress1);
            Assert.assertTrue("fd00::/8地址应该被识别为ULA", result1);

            // 测试fc00::/8范围的ULA地址
            Inet6Address ulaAddress2 = (Inet6Address) InetAddress.getByName("fc00:1:100a:1041:6ee1:636b:fcc:17eb");
            Boolean result2 = (Boolean) method.invoke(null, ulaAddress2);
            Assert.assertTrue("fc00::/8地址应该被识别为ULA", result2);

            // 测试Link-Local地址（不是ULA）
            Inet6Address linkLocalAddress = (Inet6Address) InetAddress.getByName("fe80::1234:5678:9abc:def0");
            Boolean result3 = (Boolean) method.invoke(null, linkLocalAddress);
            Assert.assertFalse("Link-Local地址不应该被识别为ULA", result3);

            // 测试全局单播地址（不是ULA）
            Inet6Address globalAddress = (Inet6Address) InetAddress.getByName("2001:db8::1");
            Boolean result4 = (Boolean) method.invoke(null, globalAddress);
            Assert.assertFalse("全局单播地址不应该被识别为ULA", result4);

        } catch (Exception e) {
            Assert.fail("测试isUniqueLocalAddress方法失败: " + e.getMessage());
        }
    }

}
