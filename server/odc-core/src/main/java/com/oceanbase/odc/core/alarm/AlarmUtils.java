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

import java.util.HashMap;
import java.util.Map;

public final class AlarmUtils {

    /**
     * Base alarm message names
     */
    public static final String CLUSTER_NAME = "Cluster";
    public static final String TENANT_NAME = "Tenant";
    public static final String ORGANIZATION_NAME = "OrganizationId";
    public static final String MESSAGE_NAME = "Message";
    public static final String ALARM_TARGET_NAME = "AlarmTarget";

    /**
     * TaskFramework alarm message names
     */
    public static final String TASK_TYPE_NAME = "TaskType";
    public static final String SCHEDULE_NAME = "ScheduleId";

    private AlarmUtils() {}

    static AlarmService alarmService = new AlarmService();

    public static void alarm(String eventName, String eventMessage) {
        alarmService.alarm(eventName, eventMessage);
    }

    public static void alarm(String eventName, Map<String, String> eventMessageNode) {
        alarmService.alarm(eventName, eventMessageNode);
    }

    public static void alarm(String eventName, Throwable e) {
        alarmService.alarm(eventName, e);
    }

    public static void warn(String eventName, String eventMessage) {
        alarmService.warn(eventName, eventMessage);
    }

    public static void warn(String eventName, Map<String, String> eventMessageNode) {
        alarmService.warn(eventName, eventMessageNode);
    }

    public static void warn(String eventName, Throwable e) {
        alarmService.warn(eventName, e);
    }

    public static void info(String eventName, String eventMessage) {
        alarmService.info(eventName, eventMessage);
    }

    public static AlarmMessageBuilder createAlarmMessageBuilder() {
        return new AlarmMessageBuilder();
    }

    public static class AlarmMessageBuilder {

        final Map<String, String> alarmMessage;

        public AlarmMessageBuilder() {
            alarmMessage = new HashMap<>();
        }

        public AlarmMessageBuilder item(String key, String value) {
            alarmMessage.put(key, value);
            return this;
        }

        public Map<String, String> build() {
            return alarmMessage;
        }
    }
}
