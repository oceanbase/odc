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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.oceanbase.odc.core.alarm.AlarmEvent.AlarmMessage;

public final class AlarmUtils {

    /**
     * Base alarm message names
     */
    public static final String CLUSTER_NAME = "Cluster";
    public static final String TENANT_NAME = "Tenant";
    public static final String ORGANIZATION_NAME = "OrganizationId";
    public static final String MESSAGE_NAME = "Message";
    public static final String ALARM_TARGET_NAME = "AlarmTarget";
    public static final String ALARM_TYPE_NAME = "AlarmType";
    /**
     * TaskFramework alarm message names
     */
    public static final String TASK_JOB_ID_NAME = "JobId";
    public static final String TASK_TYPE_NAME = "TaskType";
    public static final String SCHEDULE_NAME = "ScheduleId";
    public static final Collection<String> TASK_FRAMEWORK_ALARM_DIGEST_NAMES =
            Arrays.asList(CLUSTER_NAME, TENANT_NAME, SCHEDULE_NAME);

    private AlarmUtils() {}

    static AlarmService alarmService = new AlarmService();

    public static void alarm(String eventName, String eventMessage) {
        alarmService.alarm(eventName, eventMessage);
    }

    public static void alarm(String eventName, Map<String, String> eventContent) {
        alarmService.alarm(eventName, new AlarmMessage().setAlarmContent(eventContent));
    }

    public static void alarm(String eventName, AlarmMessage alarmMessage) {
        alarmService.alarm(eventName, alarmMessage);
    }

    public static void alarm(String eventName, Throwable e) {
        alarmService.alarm(eventName, e);
    }

    public static void warn(String eventName, String eventMessage) {
        alarmService.warn(eventName, eventMessage);
    }

    public static void warn(String eventName, Map<String, String> eventContent) {
        alarmService.warn(eventName, new AlarmMessage().setAlarmContent(eventContent));
    }

    public static void warn(String eventName, AlarmMessage eventMessageNode) {
        alarmService.warn(eventName, eventMessageNode);
    }

    public static void warn(String eventName, Throwable e) {
        alarmService.warn(eventName, e);
    }

    public static void info(String eventName, String eventMessage) {
        alarmService.info(eventName, eventMessage);
    }

    public static AlarmMapBuilder createAlarmMapBuilder() {
        return new AlarmMapBuilder();
    }

    public static class AlarmMapBuilder {

        final Map<String, String> map;

        public AlarmMapBuilder() {
            map = new HashMap<>();
        }

        public AlarmMapBuilder item(String key, String value) {
            map.put(key, value);
            return this;
        }

        public Map<String, String> build() {
            return map;
        }
    }
}
