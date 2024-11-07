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
package com.oceanbase.odc.service.task.processor.terminate;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.dlm.DLMService;
import com.oceanbase.odc.service.schedule.ScheduleTaskService;
import com.oceanbase.odc.service.schedule.job.DLMJobReq;
import com.oceanbase.odc.service.schedule.model.ScheduleTask;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.odc.service.task.processor.matcher.DLMProcessorMatcher;
import com.oceanbase.tools.migrator.common.enums.JobType;

import lombok.extern.slf4j.Slf4j;

/**
 * @author longpeng.zlp
 * @date 2024/10/10 11:51
 */
@Component
@Slf4j
public class DLMTerminateProcessor extends DLMProcessorMatcher implements TerminateProcessor {

    @Autowired
    protected DLMService dlmService;

    @Autowired
    protected ScheduleTaskService scheduleTaskService;

    public TaskStatus correctTaskStatus(ScheduleTask scheduleTask, TaskStatus currentStatus) {
        return dlmService.getFinalTaskStatus(scheduleTask.getId());
    }

    @Override
    public void process(ScheduleTask scheduleTask, JobEntity jobEntity) {
        // Trigger the data-delete job if necessary after the data-archive task is completed.
        DLMJobReq parameters = JsonUtils.fromJson(
                JsonUtils
                        .fromJson(jobEntity.getJobParametersJson(), new TypeReference<Map<String, String>>() {})
                        .get(JobParametersKeyConstants.META_TASK_PARAMETER_JSON),
                DLMJobReq.class);
        if (parameters.getJobType() == JobType.MIGRATE && parameters.isDeleteAfterMigration()
                && scheduleTask.getStatus() == TaskStatus.DONE) {
            scheduleTaskService.triggerDataArchiveDelete(scheduleTask.getId());
            log.info("Trigger delete job succeed.");
        }
    }
}
