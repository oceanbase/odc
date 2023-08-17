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
package com.oceanbase.odc.test.tool;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.SystemUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yizhou.xw
 */
@Slf4j
public class IsolatedNameGenerator {

    private static final int MAX_HOST_NAME_LENGTH = 16;
    private static final String HOST_NAME;

    static {
        String hostName = SystemUtils.getHostName();
        if (hostName == null) {
            try {
                hostName = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                log.warn("get localhost failed, message={}", e.getMessage());
            }
            if (hostName == null) {
                hostName = RandomStringUtils.randomAlphabetic(8).toLowerCase();
                log.warn("get host name failed, use random string instead");
            }
        }
        String removeSpecial = removeSpecial(hostName);
        if (removeSpecial.length() > MAX_HOST_NAME_LENGTH) {
            HOST_NAME = removeSpecial.substring(0, 8) + removeSpecial.substring(removeSpecial.length() - 8);
        } else {
            HOST_NAME = removeSpecial;
        }
        log.info("hostName={}, removeSpecial={}, HOST_NAME={}", hostName, removeSpecial, HOST_NAME);
    }

    public static String generateLowerCase(String prefix) {
        return generate(prefix).toLowerCase();
    }

    public static String generateUpperCase(String prefix) {
        return generate(prefix).toUpperCase();
    }

    /**
     * 生成用于不同环境UT执行相互隔离的名称
     *
     * @param prefix 名称前缀
     * @return 本机使用的隔离名称
     */
    public static String generate(String prefix) {
        long currentMillis = System.currentTimeMillis() % 1000000;
        return prefix + "_" + HOST_NAME + "_" + currentMillis;
    }

    private static String removeSpecial(String source) {
        return source.replaceAll("[.\\-\\s]", "");
    }
}
