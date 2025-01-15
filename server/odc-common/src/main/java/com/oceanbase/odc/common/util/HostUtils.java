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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2025/1/15 13:47
 * @Description: []
 */
@Slf4j
public class HostUtils {
    // valid expression examples: 0.0.0.0:8888, 127.1:8888, etc
    private static final String SERVER_REGEX = "(?<ip>([0-9]{1,3}\\.){1,3}([0-9]{1,3})):(?<port>[0-9]{1,5})";
    private static final Pattern SERVER_PATTERN = Pattern.compile(SERVER_REGEX);


    @NotNull
    public static ServerAddress extractServerAddress(String ipAndPort) {
        String trimmed = StringUtils.trim(ipAndPort);
        if (StringUtils.isBlank(trimmed)) {
            log.info("unable to extract server address, text is empty");
            throw new IllegalArgumentException("Empty server address!");
        }
        Matcher matcher = SERVER_PATTERN.matcher(trimmed);
        if (!matcher.matches()) {
            log.info("unable to extract server address, does not match pattern");
            throw new IllegalArgumentException("Invalid server address!");
        }
        String ipAddress = matcher.group("ip");
        String port = matcher.group("port");
        if (StringUtils.isEmpty(ipAddress) || StringUtils.isEmpty(port)) {
            log.info("unable to extract server address, ipAddress={}, port={}", ipAddress, port);
            throw new IllegalArgumentException("Invalid server address!");
        }
        return new ServerAddress(ipAddress, port);
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
