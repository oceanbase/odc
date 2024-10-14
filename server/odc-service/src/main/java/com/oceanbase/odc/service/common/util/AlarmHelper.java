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
package com.oceanbase.odc.service.common.util;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.oceanbase.odc.core.alarm.AlarmUtils;
import com.oceanbase.odc.core.alarm.AlarmUtils.AlarmMessageBuilder;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.ScheduleTaskService;
import com.oceanbase.odc.service.schedule.model.ScheduleTask;

import lombok.NonNull;

public final class AlarmHelper {

    private static final ConcurrentHashMap<Class<?>, Object> ServiceMap = new ConcurrentHashMap<>();

    private AlarmHelper() {}

    private static <T> T getService(@NonNull Class<T> serviceClass) {
        return serviceClass
                .cast(ServiceMap.computeIfAbsent(serviceClass, k -> SpringContextUtil.getBean(serviceClass)));
    }

    public static Map<String, String> buildAlarmMessageWithJob(@NonNull Long jobId) {
        ScheduleService scheduleService = getService(ScheduleService.class);
        ScheduleTaskService scheduleTaskService = getService(ScheduleTaskService.class);
        ConnectionService connectionService = getService(ConnectionService.class);

        AlarmMessageBuilder alarmMessageBuilder = AlarmUtils.createAlarmMessageBuilder();
        Optional<ScheduleTask> scheduleTaskOptional = scheduleTaskService.findByJobId(jobId);
        ScheduleTask scheduleTask =
                scheduleTaskOptional.orElseThrow(() -> new RuntimeException("ScheduleTask is must not null"));
        alarmMessageBuilder.item(AlarmUtils.TASK_TYPE_NAME, scheduleTask.getJobGroup());
        alarmMessageBuilder.item(AlarmUtils.SCHEDULE_NAME, scheduleTask.getJobName());

        ScheduleEntity schedule = scheduleService.nullSafeGetById(Long.valueOf(scheduleTask.getJobName()));
        Verify.notNull(schedule, "Schedule");

        ConnectionConfig connection = connectionService.detail(schedule.getDataSourceId());
        Verify.notNull(connection, "ConnectionConfig");

        alarmMessageBuilder.item(AlarmUtils.CLUSTER_NAME, connection.getClusterName());
        alarmMessageBuilder.item(AlarmUtils.TENANT_NAME, connection.getTenantName());

        return alarmMessageBuilder.build();
    }
}
