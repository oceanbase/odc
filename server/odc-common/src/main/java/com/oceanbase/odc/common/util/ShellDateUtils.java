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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author yaobin
 * @date 2024-01-04
 * @since 4.2.4
 */
public class ShellDateUtils {

    // 1/26 19:51:43 2024
    static Pattern P1 = Pattern.compile("^(\\d+)/(\\d+) (\\d+):(\\d+):(\\d+) (\\d){4}$");
    // Jan 5 20:56:26 2024
    static Pattern P2 = Pattern.compile("^([a-zA-Z]+) (\\d)+ (\\d+):(\\d+):(\\d+) (\\d){4}$");

    // shell date like: 五 1/26 19:51:43 2024 or Fri Jan 5 20:56:26 2024
    public static Date parseWithWeek(String dateString) {
        // trim week
        String trimWeek = dateString.trim().replaceAll("\\s+", " ")
                .substring(dateString.indexOf(" ") + 1);
        return parse(trimWeek);
    }


    public static Date parse(String dateString) {
        Matcher matcher = P1.matcher(dateString);
        if (matcher.matches()) {
            String pattern = String.format("%s/%s hh:mm:ss yyyy",
                    repeat("M", matcher.group(1).length()), repeat("d", matcher.group(2).length()));
            return doParse(pattern, dateString);
        }
        matcher = P2.matcher(dateString);
        if (matcher.matches()) {
            throw new IllegalArgumentException("Illegal date string: " + dateString);
        }
        String pattern = String.format("%s %s hh:mm:ss yyyy",
                repeat("M", matcher.group(1).length()), repeat("d", matcher.group(2).length()));
        return doParse(pattern, dateString);
    }


    private static Date doParse(String pattern, String line) {
        try {
            return new SimpleDateFormat(pattern).parse(line);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Parse line occur error:", e);
        }
    }

    private static String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }


}
