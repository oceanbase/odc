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

import org.quartz.JobExecutionContext;

import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.service.dlm.model.DataArchiveParameters;
import com.oceanbase.odc.service.quartz.util.ScheduleTaskUtils;
import com.oceanbase.odc.service.schedule.model.DataArchiveClearParameters;
import com.oceanbase.odc.service.schedule.model.ScheduleTask;
import com.oceanbase.tools.migrator.common.enums.JobType;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2023/6/13 09:51
 * @Descripition:
 */
@Slf4j
public class DataArchiveDeleteJob extends AbstractDlmJob {
    @Override
    public void executeJob(JobExecutionContext context) {
        DataArchiveClearParameters dataArchiveClearParameters =
                ScheduleTaskUtils.getDataArchiveClearParameters(context);

        ScheduleTask dataArchiveTask;
        try {
            dataArchiveTask = scheduleTaskService.nullSafeGetModelById(
                    dataArchiveClearParameters.getDataArchiveTaskId());
        } catch (NotFoundException e) {
            log.warn("Data archive task not found,rollback task fast failed.scheduleTaskId={}",
                    dataArchiveClearParameters.getDataArchiveTaskId());
            onFailure();
            return;
        }

        DataArchiveParameters dataArchiveParameters = (DataArchiveParameters) dataArchiveTask.getParameters();

        if (dataArchiveTask.getStatus() != TaskStatus.DONE) {
            log.warn("Data archive task do not finish,scheduleTaskId = {}", dataArchiveTask.getId());
            onFailure();
            return;
        }

        DLMJobReq parameters = getDLMJobReq(dataArchiveTask.getJobId());
        parameters.setJobType(JobType.DELETE);
        parameters.setScheduleTaskId(getScheduleTaskId());
        parameters
                .setRateLimit(limiterService.getByOrderIdOrElseDefaultConfig(getScheduleId()));
        publishJob(parameters, dataArchiveParameters.getTimeoutMillis(),
                dataArchiveParameters.getSourceDatabaseId());
    }
}
