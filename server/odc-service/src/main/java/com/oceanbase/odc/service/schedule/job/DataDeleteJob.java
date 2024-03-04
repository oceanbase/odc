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

import java.util.LinkedList;
import java.util.List;

import org.quartz.JobExecutionContext;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.service.dlm.model.DataDeleteParameters;
import com.oceanbase.odc.service.dlm.model.DlmTask;
import com.oceanbase.odc.service.dlm.utils.DataArchiveConditionUtil;
import com.oceanbase.odc.service.dlm.utils.DlmJobIdUtil;
import com.oceanbase.tools.migrator.common.configure.LogicTableConfig;
import com.oceanbase.tools.migrator.common.enums.JobType;
import com.oceanbase.tools.migrator.task.CheckMode;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2023/7/13 17:24
 * @Descripition:
 */

@Slf4j
public class DataDeleteJob extends AbstractDlmJob {

    @Override
    public void execute(JobExecutionContext context) {

        jobThread = Thread.currentThread();

        ScheduleTaskEntity taskEntity = (ScheduleTaskEntity) context.getResult();

        List<DlmTask> dlmTasks = getTaskUnits(taskEntity);

        executeTask(taskEntity.getId(), dlmTasks);
        TaskStatus taskStatus = getTaskStatus(dlmTasks);
        scheduleTaskRepository.updateStatusById(taskEntity.getId(), taskStatus);
    }

    @Override
    public List<DlmTask> splitTask(ScheduleTaskEntity taskEntity) {

        DataDeleteParameters parameters = JsonUtils.fromJson(taskEntity.getParametersJson(),
                DataDeleteParameters.class);
        List<DlmTask> dlmTasks = new LinkedList<>();
        parameters.getTables().forEach(table -> {
            String condition = StringUtils.isNotEmpty(table.getConditionExpression())
                    ? DataArchiveConditionUtil.parseCondition(table.getConditionExpression(), parameters.getVariables(),
                            taskEntity.getFireTime())
                    : "";
            DlmTask dlmTask = new DlmTask();

            dlmTask.setId(DlmJobIdUtil.generateHistoryJobId(taskEntity.getJobName(), taskEntity.getJobGroup(),
                    taskEntity.getId(),
                    dlmTasks.size()));
            dlmTask.setTableName(table.getTableName());
            dlmTask.setSourceDatabaseId(parameters.getDatabaseId());
            dlmTask.setTargetDatabaseId(parameters.getDatabaseId());
            dlmTask.setFireTime(taskEntity.getFireTime());

            LogicTableConfig logicTableConfig = new LogicTableConfig();
            logicTableConfig.setMigrateRule(condition);
            logicTableConfig.setCheckMode(CheckMode.MULTIPLE_GET);
            dlmTask.setLogicTableConfig(logicTableConfig);
            dlmTask.setStatus(TaskStatus.PREPARING);
            dlmTask.setJobType(parameters.getDeleteByUniqueKey() ? JobType.QUICK_DELETE : JobType.DEIRECT_DELETE);
            dlmTasks.add(dlmTask);
        });
        return dlmTasks;
    }

}
