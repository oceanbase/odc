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

import org.apache.commons.lang3.Validate;

import lombok.Getter;

/**
 * @author yizhou.xw
 * @version : VersionUtils.java, v 0.1 2021-03-26 13:44
 */
public class VersionUtils {
    private static final String VERSION_SPLITTER = "\\.";

    public static boolean isGreaterThanOrEqualsTo(String currentVersion, String targetVersion) {
        return compareVersions(currentVersion, targetVersion) >= 0;
    }

    public static boolean isGreaterThan(String currentVersion, String targetVersion) {
        return compareVersions(currentVersion, targetVersion) > 0;
    }

    public static boolean isGreaterThan0(String currentVersion) {
        return compareVersions(currentVersion, "0") > 0;
    }

    public static boolean isLessThanOrEqualsTo(String currentVersion, String targetVersion) {
        return !isGreaterThan(currentVersion, targetVersion);
    }

    public static boolean isLessThan(String currentVersion, String targetVersion) {
        return !isGreaterThanOrEqualsTo(currentVersion, targetVersion);
    }

    public static int compareVersions(String version1, String version2) {
        Validate.notEmpty(version1, "parameter version1 may not be empty");
        Validate.notEmpty(version2, "parameter version2 may not be empty");

        String[] version1Parts = version1.split(VERSION_SPLITTER);
        String[] version2Parts = version2.split(VERSION_SPLITTER);
        int maxLength = Math.max(version1Parts.length, version2Parts.length);
        for (int i = 0; i < maxLength; i++) {
            Integer version1Part = i < version1Parts.length ? Integer.parseInt(version1Parts[i]) : 0;
            Integer version2Part = i < version2Parts.length ? Integer.parseInt(version2Parts[i]) : 0;
            int result = version1Part.compareTo(version2Part);
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }

    public static class Version implements Comparable<Version> {
        @Getter
        private final String version;

        public Version(String version) {
            this.version = version;
        }

        @Override
        public int compareTo(Version o) {
            return compareVersions(this.version, o.getVersion());
        }
    }
}
