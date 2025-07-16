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
package com.oceanbase.tools.dbbrowser.util;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class HostUtils {

    public static boolean isValidIPv6Address(String ipAddress) {
        try {
            InetAddress address = InetAddress.getByName(ipAddress);
            return address instanceof Inet6Address;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    public static String addBracketsToIpv6AddressIfNeed(String ipv6Address) {
        if (isValidIPv6Address(ipv6Address) && !ipv6Address.startsWith("[")) {
            log.debug("add brackets to address={}", ipv6Address);
            return "[" + ipv6Address + "]";
        }
        return ipv6Address;
    }
}
