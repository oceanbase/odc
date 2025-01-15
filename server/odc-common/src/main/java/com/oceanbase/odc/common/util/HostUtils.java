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

    @NotNull
    public static ServerAddress extractServerAddress(String ipAndPort) {
        String trimmed = StringUtils.trim(ipAndPort);
        if (StringUtils.isBlank(trimmed)) {
            log.info("unable to extract server address, text is empty");
            throw new IllegalArgumentException("Empty server address!");
        }
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
