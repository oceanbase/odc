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
package com.oceanbase.odc.service.schedule.alarm;

import java.util.Date;

import com.oceanbase.odc.service.common.util.SpringContextUtil;

/**
 * @Author：tinker
 * @Date: 2024/7/8 13:40
 * @Descripition:
 */
public class ScheduleAlarmUtils {

    private static ScheduleAlarmClient scheduleAlarmClient;

    public static void misfire(Long scheduleId, Date fireTime) {
        getAlarmClient().misfire(scheduleId, fireTime);
    }

    public static void fail(Long scheduleTaskId) {
        getAlarmClient().fail(scheduleTaskId);
    }

    public static void timeout(Long scheduleTaskId) {
        getAlarmClient().timeout(scheduleTaskId);
    }

    private static ScheduleAlarmClient getAlarmClient() {
        if (scheduleAlarmClient == null) {
            scheduleAlarmClient = SpringContextUtil.getBean(ScheduleAlarmClient.class);
        }
        return scheduleAlarmClient;
    }
}
