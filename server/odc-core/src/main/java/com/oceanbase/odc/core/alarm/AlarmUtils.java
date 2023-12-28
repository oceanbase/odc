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

package com.oceanbase.odc.core.alarm;

public final class AlarmUtils {

    private AlarmUtils() {}

    static AlarmService alarmService = new AlarmService();

    public static void alarm(String eventName, String eventMessage) {
        alarmService.alarm(eventName, eventMessage);
    }

    public static void alarm(String eventName, Throwable e) {
        alarmService.alarm(eventName, e);
    }

    public static void warn(String eventName, String eventMessage) {
        alarmService.warn(eventName, eventMessage);
    }

    public static void warn(String eventName, Throwable e) {
        alarmService.warn(eventName, e);
    }

    public static void info(String eventName, String eventMessage) {
        alarmService.info(eventName, eventMessage);
    }
}
