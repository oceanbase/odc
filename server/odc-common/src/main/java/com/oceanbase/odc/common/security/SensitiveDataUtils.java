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
package com.oceanbase.odc.common.security;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oceanbase.odc.common.util.StringUtils;

public class SensitiveDataUtils {

    private static final Pattern SENSITIVE_PATTERN =
            Pattern.compile("(secret|key|password|pswd|email|-p)([=|:|\\\"\\s]*)([^&,\\n\\t\\\"]+)",
                    Pattern.CASE_INSENSITIVE);
    private static final String MASKED_VALUE = "***";

    public static String mask(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        try {
            Matcher matcher = SENSITIVE_PATTERN.matcher(message);
            if (matcher.find()) {
                StringBuffer sb = new StringBuffer();
                do {
                    matcher.appendReplacement(sb, matcher.group(1) + matcher.group(2) + MASKED_VALUE);
                } while (matcher.find());
                matcher.appendTail(sb);
                return sb.toString();
            }
            return message;
        } catch (Exception ex) {
            return "MESSAGE_MASK_FAILED, origin message start with " + StringUtils.substring(message, 0, 10);
        }
    }
}
