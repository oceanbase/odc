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

public class ShellUtils {
    private static final char BACK_SLASH = '\\';
    private static final char DOUBLE_QUOTE = '\"';
    private static final char BLANK_SPACE = ' ';
    private static final Pattern ESCAPE_ON_LINUX_PATTERN = Pattern.compile("[()<>`!#&|;.\"'$\\\\\\s]");
    private static final String ESCAPE_ON_WINDOWS_PATTERN = "<>&|^";

    /**
     * Use backslash to escape special characters on Linux instead of quote.
     */
    public static String escapeOnLinux(String input) {
        Matcher matcher = ESCAPE_ON_LINUX_PATTERN.matcher(input);
        StringBuilder stringBuilder = new StringBuilder();
        int regionBegin = 0;
        while (matcher.find()) {
            stringBuilder.append(input, regionBegin, matcher.start())
                    .append(BACK_SLASH);
            regionBegin = matcher.start();
        }
        return stringBuilder.append(input, regionBegin, input.length()).toString();
    }

    public static String escapeOnWindows(String input) {
        StringBuilder output = new StringBuilder();
        int totalQuoteCount = 0;
        int unescapedQuoteCount = 0;
        char[] chars = input.toCharArray();
        for (char c : chars) {
            if (DOUBLE_QUOTE == c) {
                output.append(BACK_SLASH);
                totalQuoteCount++;
            } else if (ESCAPE_ON_WINDOWS_PATTERN.indexOf(c) != -1 && totalQuoteCount % 2 == 0) {
                output.append(DOUBLE_QUOTE);
                totalQuoteCount++;
                unescapedQuoteCount++;
            } else if (BLANK_SPACE == c && unescapedQuoteCount % 2 == 0) {
                output.append(DOUBLE_QUOTE);
                totalQuoteCount++;
                unescapedQuoteCount++;
            }
            output.append(c);
        }
        return output.toString();
    }

    public static String escape(String input) {
        return SystemUtils.isOnWindows() ? escapeOnWindows(input) : escapeOnLinux(input);
    }

}
