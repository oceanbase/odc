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
package com.oceanbase.odc.service.quartz.util;

import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.quartz.CronExpression;

/**
 * @Author：tinker
 * @Date: 2023/1/4 16:00
 * @Descripition:
 */
public class QuartzCronExpressionUtils {

    protected static final Map<Character, Character> dayMap = new HashMap<>(7);

    static {

        dayMap.put('1', '2');
        dayMap.put('2', '3');
        dayMap.put('3', '4');
        dayMap.put('4', '5');
        dayMap.put('5', '6');
        dayMap.put('6', '7');
        dayMap.put('7', '1');

    }

    public static String adaptCronExpression(String cron) throws ParseException {

        StringTokenizer tokenizer = new StringTokenizer(cron, " \t",
                false);

        StringBuilder sb = new StringBuilder();

        int type = 0;

        while (tokenizer.hasMoreTokens()) {
            if (type == 5) {
                String element = tokenizer.nextToken().trim();
                sb.append(adaptDayOfWeek(element));
            } else {
                sb.append(tokenizer.nextToken());
            }
            sb.append(" ");
            type++;
        }
        return sb.substring(0, sb.length() - 1);
    }

    /**
     * If used in the day-of-week field by itself, it simply means "7" or "SAT". But if used in the
     * day-of-week field after another value, it means "the last xxx day of the month" - for example
     * "6L" means "the last friday of the month".
     * 
     * @see org.quartz.CronExpression
     */
    private static String adaptDayOfWeek(String dayOfWeek) throws ParseException {

        if ("L".equals(dayOfWeek)) {
            return "1";
        }

        int pos = 0;
        StringBuilder sb = new StringBuilder();
        while (pos < dayOfWeek.length()) {
            char c = dayOfWeek.charAt(pos);
            if (Character.isDigit(c)) {

                if (pos != 0 && dayOfWeek.charAt(pos - 1) == '#') {
                    sb.append(dayOfWeek.charAt(pos));
                } else {
                    if (c < '1' || c > '7') {
                        throw new ParseException(
                                "Day-of-Week values must be between 1 and 7", -1);
                    }
                    sb.append(dayMap.get(c));
                }

            } else {
                sb.append(dayOfWeek.charAt(pos));
            }
            pos++;
        }
        return sb.toString();
    }

    public static List<Date> getNextFireTimes(String cronExpression) {
        return getNextFireTimes(cronExpression, 5);
    }

    public static List<Date> getNextFireTimes(String cronExpression, int count) {
        try {
            String cron = adaptCronExpression(cronExpression);
            CronExpression generator =
                    new CronExpression(cron);
            List<Date> nextFireTimes = new LinkedList<>();
            Date next = generator.getNextValidTimeAfter(new Date());
            while (count > 0) {
                nextFireTimes.add(next);
                next = generator.getNextValidTimeAfter(next);
                count--;
            }
            return nextFireTimes;
        } catch (Exception e) {
            return Collections.singletonList(new Date(0));
        }
    }

}
