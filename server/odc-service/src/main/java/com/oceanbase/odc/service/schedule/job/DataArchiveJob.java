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
package com.oceanbase.odc.service.schedule.job;

import java.util.List;

import org.quartz.JobExecutionContext;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.service.dlm.model.DataArchiveParameters;
import com.oceanbase.odc.service.dlm.model.DlmTask;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2023/5/9 14:46
 * @Descripition:
 */
@Slf4j
public class DataArchiveJob extends AbstractDlmJob {
    @Override
    public void execute(JobExecutionContext context) {

        jobThread = Thread.currentThread();

        ScheduleTaskEntity taskEntity = (ScheduleTaskEntity) context.getResult();

        List<DlmTask> taskUnits = getTaskUnits(taskEntity);

        executeTask(taskEntity.getId(), taskUnits);
        TaskStatus taskStatus = getTaskStatus(taskUnits);
        scheduleTaskRepository.updateStatusById(taskEntity.getId(), taskStatus);

        DataArchiveParameters parameters = JsonUtils.fromJson(taskEntity.getParametersJson(),
                DataArchiveParameters.class);

        if (taskStatus == TaskStatus.DONE && parameters.isDeleteAfterMigration()) {
            log.info("Start to create clear job,taskId={}", taskEntity.getId());
            scheduleService.dataArchiveDelete(Long.parseLong(taskEntity.getJobName()), taskEntity.getId());
            log.info("Clear job is created,");
        }
    }
}
