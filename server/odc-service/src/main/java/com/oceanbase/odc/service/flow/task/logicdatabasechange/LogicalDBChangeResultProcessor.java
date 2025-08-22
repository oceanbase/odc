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
package com.oceanbase.odc.service.flow.task.logicdatabasechange;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.connection.logicaldatabase.LogicalDatabaseChangeService;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.ExecutionResult;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.sql.SqlExecutionResultWrapper;
import com.oceanbase.odc.service.connection.logicaldatabase.core.model.LogicalDBChangeExecutionUnit;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2024/9/3 19:43
 * @Description: []
 */
@Slf4j
public class LogicalDBChangeResultProcessor {

    public TaskStatus process(Map<String, ExecutionResult<SqlExecutionResultWrapper>> executionId2Result,
            LogicalDatabaseChangeService logicalDatabaseChangeService, Long flowInstanceId) {
        log.info("Start refresh result, result={}", executionId2Result);
        try {
            if (CollectionUtils.isEmpty(executionId2Result)) {
                log.warn("Task result is empty, flowInstanceId={}", flowInstanceId);
                return null;
            }
            List<LogicalDBChangeExecutionUnit> executionUnits = executionId2Result.entrySet().stream().map(entry -> {
                LogicalDBChangeExecutionUnit executionUnit = new LogicalDBChangeExecutionUnit();
                executionUnit.setExecutionId(entry.getKey());
                executionUnit.setStatus(entry.getValue().getStatus());
                executionUnit.setResult(entry.getValue().getResult());
                executionUnit.setSql(entry.getValue().getResult().getExecuteSql());
                executionUnit.setFlowInstanceId(entry.getValue().getResult().getFlowInstanceId());
                executionUnit.setLogicalDatabaseId(entry.getValue().getResult().getLogicalDatabaseId());
                executionUnit.setPhysicalDatabaseId(entry.getValue().getResult().getPhysicalDatabaseId());
                executionUnit.setOrder(entry.getValue().getOrder());
                return executionUnit;
            }).collect(Collectors.toList());
            logicalDatabaseChangeService.upsert(executionUnits);
            log.info("Create or update logical database change execution units success,jobIdentity={}",
                    flowInstanceId);

            TaskStatus taskStatus = getTaskStatus(executionId2Result.values());
            log.info("current schedule task status to {} success", taskStatus);
            return taskStatus;
        } catch (Exception e) {
            log.warn("Refresh result failed.", e);
            return null;
        }
    }

    private TaskStatus getTaskStatus(Collection<ExecutionResult<SqlExecutionResultWrapper>> results) {
        if (results.stream().allMatch(ExecutionResult::isCompleted)) {
            return TaskStatus.DONE;
        } else {
            return TaskStatus.RUNNING;
        }
    }
}
