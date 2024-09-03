/*
 * Copyright (c) 2024 OceanBase.
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

package com.oceanbase.odc.service.task.processor;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.connection.logicaldatabase.LogicalDatabaseChangeService;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionResult;
import com.oceanbase.odc.service.connection.logicaldatabase.core.model.LogicalDBChangeExecutionUnit;
import com.oceanbase.odc.service.dlm.model.DlmTableUnit;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;
import com.oceanbase.odc.service.task.executor.task.TaskResult;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2024/9/3 19:43
 * @Description: []
 */
@Slf4j
@Component
public class LogicalDBChangeResultProcessor implements ResultProcessor {
    @Autowired
    private LogicalDatabaseChangeService logicalDatabaseChangeService;

    @Override
    public void process(TaskResult result) {
        log.info("Start refresh result, result={}", result.getResultJson());
        try {
            Map<String, ExecutionResult<SqlExecuteResult>> executionId2Result = JsonUtils.fromJson(result.getResultJson(),
                    new TypeReference<Map<String, ExecutionResult<SqlExecuteResult>>>() {});
            if (CollectionUtils.isEmpty(executionId2Result)) {
                log.warn("Task result is empty, jobIdentity={}", result.getJobIdentity());
                return;
            }
            executionId2Result.entrySet().stream().map(entry -> {
                LogicalDBChangeExecutionUnit executionUnit = new LogicalDBChangeExecutionUnit();
                executionUnit.setExecutionId(entry.getKey());
                executionUnit.setExecutionResult(entry.getValue().getResult());
                executionUnit.setSql(entry.getValue().getResult().getExecuteSql());
                return executionUnit;
            }).forEach(logicalDatabaseChangeService::upsert);
            logicalDatabaseChangeService.upsert(dlmTableUnits);
            log.info("Create or update dlm tableUnits success,jobIdentity={},scheduleTaskId={}",
                    result.getJobIdentity(),
                    dlmTableUnits.get(0).getScheduleTaskId());
            TaskStatus taskStatus = dlmService.getTaskStatus(dlmTableUnits);
            taskService.updateStatusById(dlmTableUnits.get(0).getScheduleTaskId(), taskStatus);
            log.info("Update schedule task status to {} success", taskStatus);
        } catch (Exception e) {
            log.warn("Refresh result failed.", e);
        }
    }
}
