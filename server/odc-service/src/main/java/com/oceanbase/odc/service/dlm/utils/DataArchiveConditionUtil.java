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
package com.oceanbase.odc.service.dlm.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.service.dlm.model.OffsetConfig;

/**
 * @Author：tinker
 * @Date: 2023/5/30 10:08
 * @Descripition:
 */
public class DataArchiveConditionUtil {
    private static final Pattern CONDITION_VARIABLES_PATTERN = Pattern.compile("\\$\\{(.+?)\\}");

    public static String parseCondition(String condition, List<OffsetConfig> variables, Date baseDate) {
        Map<String, String> variablesMap = getVariablesMap(variables);
        return replaceVariables(condition, variablesMap, baseDate);
    }

    private static String replaceVariables(String condition, Map<String, String> variables, Date baseDate) {
        Matcher matcher = CONDITION_VARIABLES_PATTERN.matcher(condition);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String name = matcher.group(1);
            if (!variables.containsKey(name)) {
                throw new IllegalArgumentException(String.format("Variable not found,name=%s", name));
            }
            String value = calculateDateTime(baseDate, variables.get(name));
            String replacement = Matcher.quoteReplacement(value);
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static Map<String, String> getVariablesMap(List<OffsetConfig> variables) {
        if (CollectionUtils.isEmpty(variables)) {
            return Collections.emptyMap();
        }
        Map<String, String> map = new HashMap<>();
        variables.forEach(obj -> {
            if (map.containsKey(obj.getName())) {
                throw new IllegalArgumentException(String.format("Duplicate variable found,name=%s", obj.getName()));
            }
            map.put(obj.getName(), obj.getPattern());
        });
        return map;
    }

    private static String calculateDateTime(Date baseDate, String pattern) {
        String[] parts = pattern.split("\\|");
        String offsetString = parts[1].substring(1);
        ChronoUnit unit = parseUnit(offsetString);
        long offsetSeconds = parseValue(offsetString) * unit.getDuration().getSeconds();
        if (parts[1].startsWith("-")) {
            offsetSeconds = -offsetSeconds;
        }
        LocalDateTime localDateTime = baseDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                .plusSeconds(offsetSeconds);
        return localDateTime.format(DateTimeFormatter.ofPattern(parts[0]));
    }

    private static int parseValue(String offsetString) {
        return Integer.parseInt(offsetString.substring(0, offsetString.length() - 1));
    }

    private static ChronoUnit parseUnit(String offsetString) {
        switch (offsetString.charAt(offsetString.length() - 1)) {
            case 'y':
                return ChronoUnit.YEARS;
            case 'M':
                return ChronoUnit.MONTHS;
            case 'd':
                return ChronoUnit.DAYS;
            case 'h':
                return ChronoUnit.HOURS;
            case 'm':
                return ChronoUnit.MINUTES;
            case 's':
                return ChronoUnit.SECONDS;
            case 'w':
                return ChronoUnit.WEEKS;
            default:
                throw new IllegalArgumentException("Unknown unit: " + offsetString);
        }
    }
}
