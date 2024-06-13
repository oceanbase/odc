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
import java.util.Optional;

import org.quartz.JobExecutionContext;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.service.dlm.model.DataArchiveParameters;
import com.oceanbase.odc.service.dlm.model.DlmTableUnit;
import com.oceanbase.odc.service.dlm.utils.DlmJobIdUtil;
import com.oceanbase.odc.service.schedule.model.DataArchiveClearParameters;
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

        ScheduleTaskEntity taskEntity = (ScheduleTaskEntity) context.getResult();
        DataArchiveClearParameters dataArchiveClearParameters = JsonUtils.fromJson(taskEntity.getParametersJson(),
                DataArchiveClearParameters.class);

        // find data archive task by id.
        Optional<ScheduleTaskEntity> dataArchiveTaskOption =
                scheduleTaskRepository.findById(dataArchiveClearParameters.getDataArchiveTaskId());

        if (!dataArchiveTaskOption.isPresent()) {
            log.warn("Data archive task not found,rollback task fast failed.scheduleTaskId={}",
                    dataArchiveClearParameters.getDataArchiveTaskId());
            scheduleTaskRepository.updateStatusById(taskEntity.getId(), TaskStatus.FAILED);
            return;
        }

        ScheduleTaskEntity dataArchiveTask = dataArchiveTaskOption.get();
        DataArchiveParameters dataArchiveParameters = JsonUtils.fromJson(dataArchiveTask.getParametersJson(),
                DataArchiveParameters.class);

        if (dataArchiveTask.getStatus() != TaskStatus.DONE) {
            log.warn("Data archive task do not finish,scheduleTaskId = {}", dataArchiveTask.getId());
            scheduleTaskRepository.updateStatusById(taskEntity.getId(), TaskStatus.FAILED);
            return;
        }

        // execute in task framework.
        if (taskFrameworkProperties.isEnabled()) {
            DLMJobReq parameters = getDLMJobReq(dataArchiveTask.getJobId());
            parameters.setJobType(JobType.DELETE);
            parameters.setScheduleTaskId(taskEntity.getId());
            Long jobId = publishJob(parameters, dataArchiveParameters.getTimeoutMillis());
            log.info("Publish DLM job to task framework succeed,scheduleTaskId={},jobIdentity={}", taskEntity.getId(),
                    jobId);
            scheduleTaskRepository.updateJobIdById(taskEntity.getId(), jobId);
            scheduleTaskRepository.updateTaskResult(taskEntity.getId(), JsonUtils.toJson(parameters));
            return;
        }
        List<DlmTableUnit> dlmTableUnits = dlmService.findByScheduleTaskId(taskEntity.getId());
        if (dlmTableUnits.isEmpty()) {
            dlmTableUnits = dlmService.findByScheduleTaskId(dataArchiveTask.getId());
            for (int i = 0; i < dlmTableUnits.size(); i++) {
                dlmTableUnits.get(i)
                        .setDlmTableUnitId(
                                DlmJobIdUtil.generateHistoryJobId(taskEntity.getJobName(), taskEntity.getJobGroup(),
                                        taskEntity.getId(),
                                        i));
                dlmTableUnits.get(i).setType(JobType.DELETE);
                dlmTableUnits.get(i).setStatus(TaskStatus.PREPARING);
                dlmTableUnits.get(i).setScheduleTaskId(taskEntity.getId());
            }
            dlmService.createDlmTableUnits(dlmTableUnits);
        }
        executeTask(taskEntity.getId(), dlmTableUnits, dataArchiveParameters.getTimeoutMillis());
        TaskStatus taskStatus = getTaskStatus(taskEntity.getId());
        scheduleTaskRepository.updateStatusById(taskEntity.getId(), taskStatus);
    }
}
