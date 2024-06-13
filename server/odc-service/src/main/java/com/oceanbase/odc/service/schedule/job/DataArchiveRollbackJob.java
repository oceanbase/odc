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
import com.oceanbase.odc.service.schedule.model.DataArchiveRollbackParameters;
import com.oceanbase.tools.migrator.common.configure.DataSourceInfo;
import com.oceanbase.tools.migrator.common.enums.JobType;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2023/6/26 20:05
 * @Descripition:
 */
@Slf4j
public class DataArchiveRollbackJob extends AbstractDlmJob {

    @Override
    public void executeJob(JobExecutionContext context) {

        ScheduleTaskEntity taskEntity = (ScheduleTaskEntity) context.getResult();
        DataArchiveRollbackParameters rollbackParameters = JsonUtils.fromJson(taskEntity.getParametersJson(),
                DataArchiveRollbackParameters.class);

        // find data archive task by id.
        Optional<ScheduleTaskEntity> dataArchiveTaskOption =
                scheduleTaskRepository.findById(rollbackParameters.getDataArchiveTaskId());

        if (!dataArchiveTaskOption.isPresent()) {
            log.warn("Data archive task not found,rollback task fast failed,scheduleTaskId={}",
                    rollbackParameters.getDataArchiveTaskId());
            scheduleTaskRepository.updateStatusById(taskEntity.getId(), TaskStatus.FAILED);
            return;
        }

        ScheduleTaskEntity dataArchiveTask = dataArchiveTaskOption.get();
        DataArchiveParameters dataArchiveParameters = JsonUtils.fromJson(dataArchiveTask.getParametersJson(),
                DataArchiveParameters.class);
        // execute in task framework.
        if (taskFrameworkProperties.isEnabled()) {
            DLMJobReq parameters = getDLMJobReq(dataArchiveTask.getJobId());
            parameters.setJobType(JobType.ROLLBACK);
            DataSourceInfo tempDataSource = parameters.getSourceDs();
            parameters.setSourceDs(parameters.getTargetDs());
            parameters.setTargetDs(tempDataSource);
            parameters.getTables().forEach(o -> {
                String temp = o.getTableName();
                o.setTableName(o.getTargetTableName());
                o.setTargetTableName(temp);
            });
            parameters.setScheduleTaskId(taskEntity.getId());
            Long jobId = publishJob(parameters, dataArchiveParameters.getTimeoutMillis());
            log.info("Publish DLM job to task framework succeed,scheduleTaskId={},jobIdentity={}", taskEntity.getId(),
                    jobId);
            scheduleTaskRepository.updateJobIdById(taskEntity.getId(), jobId);
            scheduleTaskRepository.updateTaskResult(taskEntity.getId(), JsonUtils.toJson(parameters));
            return;
        }
        // prepare tasks for rollback
        List<DlmTableUnit> dlmTableUnits = dlmService.findByScheduleTaskId(dataArchiveTask.getId());
        for (int i = 0; i < dlmTableUnits.size(); i++) {
            DlmTableUnit dlmTableUnit = dlmTableUnits.get(i);
            DataSourceInfo temp = dlmTableUnit.getSourceDatasourceInfo();
            dlmTableUnit.setDlmTableUnitId(
                    DlmJobIdUtil.generateHistoryJobId(taskEntity.getJobName(), taskEntity.getJobGroup(),
                            taskEntity.getId(),
                            i));
            dlmTableUnit.setSourceDatasourceInfo(dlmTableUnit.getTargetDatasourceInfo());
            dlmTableUnit.setTargetDatasourceInfo(temp);
            String tmp = dlmTableUnit.getTableName();
            dlmTableUnit.setTableName(dlmTableUnit.getTargetTableName());
            dlmTableUnit.setTargetTableName(tmp);
            dlmTableUnit.setType(JobType.ROLLBACK);
            dlmTableUnit.setStatus(
                    dlmTableUnit.getStatus() == TaskStatus.PREPARING ? TaskStatus.DONE : TaskStatus.PREPARING);
            dlmTableUnits.get(i).setScheduleTaskId(taskEntity.getId());
        }
        dlmService.createDlmTableUnits(dlmTableUnits);
        executeTask(taskEntity.getId(), dlmTableUnits, dataArchiveParameters.getTimeoutMillis());
        TaskStatus taskStatus = getTaskStatus(taskEntity.getId());
        scheduleTaskRepository.updateStatusById(taskEntity.getId(), taskStatus);
    }
}
