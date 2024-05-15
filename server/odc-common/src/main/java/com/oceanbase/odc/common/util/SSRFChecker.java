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

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: Lebie
 * @Date: 2022/4/6 下午3:50
 * @Description: []
 */
public class SSRFChecker {
    public static boolean checkHostInWhiteList(String host, List<String> hostWhiteList) {
        if (Objects.isNull(hostWhiteList) || hostWhiteList.isEmpty()) {
            return true;
        }
        if (StringUtils.isBlank(host)) {
            return false;
        }
        for (String whiteHost : hostWhiteList) {
            String whiteHostPattern = whiteHost;
            if (StringUtils.contains(whiteHost, ".")) {
                whiteHostPattern = whiteHost.replaceAll("\\.", "\\\\.");
            }
            Pattern whitePattern = Pattern.compile(
                    "^([a-z0-9_\\-]+\\.)*" + whiteHostPattern + "$", Pattern.CASE_INSENSITIVE);
            Matcher uriMatcher = whitePattern.matcher(host);
            if (uriMatcher.find()) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkUrlInWhiteList(String url, List<String> urlWhiteList) {
        if (Objects.isNull(urlWhiteList) || urlWhiteList.isEmpty()) {
            return true;
        }
        if (StringUtils.isBlank(url)) {
            return false;
        }
        for (String whiteUrl : urlWhiteList) {
            if (StringUtils.startsWith(url, whiteUrl)) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkHostNotInBlackList(String host, List<String> hostBlackList) {
        if (Objects.isNull(hostBlackList) || hostBlackList.isEmpty()) {
            return true;
        }
        if (StringUtils.isBlank(host)) {
            return false;
        }
        for (String blackHost : hostBlackList) {
            String blackHostPattern = blackHost;
            if (StringUtils.contains(blackHost, ".")) {
                blackHostPattern = blackHost.replaceAll("\\.", "\\\\.");
            }
            Pattern whitePattern = Pattern.compile(
                    "^([a-z0-9_\\-]+\\.)*" + blackHostPattern + "$", Pattern.CASE_INSENSITIVE);
            Matcher uriMatcher = whitePattern.matcher(host);
            if (uriMatcher.find()) {
                return false;
            }
        }
        return true;
    }

}
