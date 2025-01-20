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
package com.oceanbase.odc.service.task.processor.result;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.connection.logicaldatabase.LogicalDatabaseChangeService;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.ExecutionResult;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.sql.SqlExecutionResultWrapper;
import com.oceanbase.odc.service.connection.logicaldatabase.core.model.LogicalDBChangeExecutionUnit;
import com.oceanbase.odc.service.schedule.ScheduleTaskService;
import com.oceanbase.odc.service.task.executor.TaskResult;
import com.oceanbase.odc.service.task.processor.matcher.LogicalDBChangeProcessorMatcher;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2024/9/3 19:43
 * @Description: []
 */
@Slf4j
@Component
public class LogicalDBChangeResultProcessor extends LogicalDBChangeProcessorMatcher implements ResultProcessor {
    @Autowired
    protected LogicalDatabaseChangeService logicalDatabaseChangeService;

    @Autowired
    protected ScheduleTaskService taskService;

    @Override
    public void process(TaskResult result) {
        log.info("Start refresh result, result={}", result.getResultJson());
        try {
            Map<String, ExecutionResult<SqlExecutionResultWrapper>> executionId2Result =
                    JsonUtils.fromJson(result.getResultJson(),
                            new TypeReference<Map<String, ExecutionResult<SqlExecutionResultWrapper>>>() {});
            if (CollectionUtils.isEmpty(executionId2Result)) {
                log.warn("Task result is empty, jobIdentity={}", result.getJobIdentity());
                return;
            }
            List<LogicalDBChangeExecutionUnit> executionUnits = executionId2Result.entrySet().stream().map(entry -> {
                LogicalDBChangeExecutionUnit executionUnit = new LogicalDBChangeExecutionUnit();
                executionUnit.setExecutionId(entry.getKey());
                executionUnit.setStatus(entry.getValue().getStatus());
                executionUnit.setResult(entry.getValue().getResult());
                executionUnit.setSql(entry.getValue().getResult().getExecuteSql());
                executionUnit.setScheduleTaskId(entry.getValue().getResult().getScheduleTaskId());
                executionUnit.setLogicalDatabaseId(entry.getValue().getResult().getLogicalDatabaseId());
                executionUnit.setPhysicalDatabaseId(entry.getValue().getResult().getPhysicalDatabaseId());
                executionUnit.setOrder(entry.getValue().getOrder());
                return executionUnit;
            }).collect(Collectors.toList());
            logicalDatabaseChangeService.upsert(executionUnits);
            log.info("Create or update logical database change execution units success,jobIdentity={}",
                    result.getJobIdentity());

            TaskStatus taskStatus = getTaskStatus(executionId2Result.values());
            taskService.updateStatusById(executionUnits.get(0).getScheduleTaskId(), taskStatus);
            log.info("Update schedule task status to {} success", taskStatus);
        } catch (Exception e) {
            log.warn("Refresh result failed.", e);
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
