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

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2025/1/15 13:47
 * @Description: []
 */
@Slf4j
public class HostUtils {

    @NotNull
    public static ServerAddress extractServerAddress(String ipAndPort) {
        String trimmed = StringUtils.trim(ipAndPort);
        if (StringUtils.isBlank(trimmed)) {
            log.info("unable to extract server address, text is empty");
            throw new IllegalArgumentException("Empty server address!");
        }

        if (trimmed.startsWith("[")) {
            return getIpv6ServerAddress(trimmed);
        }

        return getIpv4ServerAddress(trimmed);
    }

    private static ServerAddress getIpv4ServerAddress(String trimmed) {
        String[] segments = StringUtils.split(trimmed, ":");
        if (segments.length != 2) {
            log.info("unable to extract server address, segments={}", segments);
            throw new IllegalArgumentException("Invalid server address!");
        }
        if (StringUtils.isEmpty(segments[0]) || StringUtils.isEmpty(segments[1])) {
            log.info("unable to extract server address, segments={}", segments);
            throw new IllegalArgumentException("Invalid server address!");
        }
        return new ServerAddress(segments[0], segments[1]);
    }

    private static ServerAddress getIpv6ServerAddress(String trimmed) {
        int closeBracketIndex = trimmed.indexOf(']');
        if (closeBracketIndex == -1) {
            log.info("unable to extract server address, invalid IPv6 format: {}", trimmed);
            throw new IllegalArgumentException("Invalid IPv6 server address format!");
        }

        String ipv6Address = trimmed.substring(1, closeBracketIndex);
        if (StringUtils.isEmpty(ipv6Address)) {
            log.info("unable to extract server address, empty IPv6 address: {}", trimmed);
            throw new IllegalArgumentException("Empty IPv6 address!");
        }

        if (closeBracketIndex + 1 >= trimmed.length()) {
            log.info("unable to extract server address, no port specified for IPv6: {}", trimmed);
            throw new IllegalArgumentException("No port specified for IPv6 address!");
        }

        if (trimmed.charAt(closeBracketIndex + 1) != ':') {
            log.info("unable to extract server address, invalid IPv6 port format: {}", trimmed);
            throw new IllegalArgumentException("Invalid IPv6 port format!");
        }

        String port = trimmed.substring(closeBracketIndex + 2);
        if (StringUtils.isEmpty(port)) {
            log.info("unable to extract server address, empty port for IPv6: {}", trimmed);
            throw new IllegalArgumentException("Empty port for IPv6 address!");
        }

        return new ServerAddress(ipv6Address, port);
    }

    @Data
    public static class ServerAddress {
        String ipAddress;
        String port;

        public ServerAddress(String ipAddress, String port) {
            this.ipAddress = ipAddress;
            this.port = port;
        }
    }
}
