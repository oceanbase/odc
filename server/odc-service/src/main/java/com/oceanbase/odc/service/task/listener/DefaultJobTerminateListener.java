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

package com.oceanbase.odc.service.task.listener;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.event.AbstractEventListener;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.dlm.DLMService;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.ScheduleTaskService;
import com.oceanbase.odc.service.schedule.alarm.ScheduleAlarmUtils;
import com.oceanbase.odc.service.schedule.job.DLMJobReq;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;
import com.oceanbase.tools.migrator.common.enums.JobType;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-12-15
 * @since 4.2.4
 */
@Component
@Slf4j
public class DefaultJobTerminateListener extends AbstractEventListener<JobTerminateEvent> {

    @Autowired
    private TaskFrameworkService taskFrameworkService;
    @Autowired
    private ScheduleTaskService scheduleTaskService;
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private DLMService dlmService;

    @Override
    public void onEvent(JobTerminateEvent event) {
        JobEntity jobEntity = taskFrameworkService.find(event.getJi().getId());
        scheduleTaskService.findByJobId(jobEntity.getId()).ifPresent(o -> {
            TaskStatus taskStatus = "DLM".equals(jobEntity.getJobType()) ? dlmService.getTaskStatus(o.getId())
                    : event.getStatus().convertTaskStatus();
            scheduleTaskService.updateStatusById(o.getId(), taskStatus);
            log.info("Update schedule task status to {} succeed,scheduleTaskId={}", taskStatus, o.getId());
            // Refresh the schedule status after the task is completed.
            scheduleService.refreshScheduleStatus(Long.parseLong(o.getJobName()));
            // Trigger the alarm if the task is failed or canceled.
            if (taskStatus == TaskStatus.FAILED) {
                ScheduleAlarmUtils.fail(o.getId());
            }
            if (taskStatus == TaskStatus.CANCELED) {
                ScheduleAlarmUtils.timeout(o.getId());
            }
            // Trigger the data-delete job if necessary after the data-archive task is completed.
            if ("DLM".equals(jobEntity.getJobType())) {
                DLMJobReq parameters = JsonUtils.fromJson(
                        JsonUtils
                                .fromJson(jobEntity.getJobParametersJson(), new TypeReference<Map<String, String>>() {})
                                .get(JobParametersKeyConstants.META_TASK_PARAMETER_JSON),
                        DLMJobReq.class);
                if (parameters.getJobType() == JobType.MIGRATE && parameters.isDeleteAfterMigration()
                        && taskStatus == TaskStatus.DONE) {
                    scheduleTaskService.triggerDataArchiveDelete(o.getId());
                    log.info("Trigger delete job succeed.");
                }
            }
        });
    }
}
