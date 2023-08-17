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
package com.oceanbase.odc.service.shadowtable;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.shared.constant.TaskErrorStrategy;
import com.oceanbase.odc.service.flow.task.model.ShadowTableSyncTaskResult;
import com.oceanbase.odc.service.flow.task.model.ShadowTableSyncTaskResult.TableSyncExecuting;
import com.oceanbase.odc.service.shadowtable.model.ShadowTableSyncResp.TableComparing;
import com.oceanbase.odc.service.shadowtable.model.TableSyncExecuteStatus;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2022/9/22 下午7:44
 * @Description: []
 */
@Slf4j
public class ShadowTableSyncTask implements Callable<ShadowTableSyncTaskResult> {
    private final Long taskId;
    private final Long userId;
    private final Long shadowTableComparingId;
    private final List<TableComparing> tables;
    private final ConnectionSession connectionSession;
    private ShadowTableSyncTaskResult result;
    private final int totalTableCount;
    private final TaskErrorStrategy errorStrategy;
    private int succeedCount = 0;
    private int failedCount = 0;


    ShadowTableSyncTask(Long shadowTableComparingId, TaskErrorStrategy errorStrategy, List<TableComparing> tables,
            ConnectionSession connectionSession, Long taskId, Long userId) {
        this.taskId = taskId;
        this.shadowTableComparingId = shadowTableComparingId;
        this.errorStrategy = errorStrategy;
        this.tables = tables;
        this.connectionSession = connectionSession;
        this.totalTableCount = tables.size();
        this.userId = userId;
    }

    @Override
    public ShadowTableSyncTaskResult call() {
        try {
            ShadowTaskTraceContextHolder.trace(userId, taskId);
            init();
            log.info("shadow table sync task starts to run, taskId={}, total tables to sync={}", taskId, tables.size());
            for (int i = 0; i < tables.size(); i++) {
                TableSyncExecuting executingResult = result.getTables().get(i);
                executingResult.setStatus(TableSyncExecuteStatus.EXECUTING);
                TableComparing tableComparing = tables.get(i);
                String sql = tableComparing.getComparingDDL();
                try {
                    connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY).execute(sql);
                    succeedCount++;
                    executingResult.setStatus(TableSyncExecuteStatus.SUCCESS);
                    log.info("table sync succeed, originalTableName={}, destTableName={}",
                            tableComparing.getOriginTableName(),
                            tableComparing.getDestTableName());
                } catch (Exception ex) {
                    log.warn(
                            "shadow table sync task meets error, originalTableName={}, destTableName={}, executingSql={}, ex={}",
                            tableComparing.getOriginTableName(), tableComparing.getDestTableName(), sql, ex);
                    failedCount++;
                    executingResult.setStatus(TableSyncExecuteStatus.FAILED);
                    if (errorStrategy == TaskErrorStrategy.ABORT) {
                        log.info("skip remained tables...");
                        for (int skipIndex = i + 1; skipIndex < tables.size(); skipIndex++) {
                            TableSyncExecuting skipExecutingResult = result.getTables().get(skipIndex);
                            skipExecutingResult.setStatus(TableSyncExecuteStatus.SKIP);
                            failedCount++;
                            log.info("skip table sync, originalTableName={}, destTableName={}",
                                    skipExecutingResult.getOriginTableName(),
                                    skipExecutingResult.getDestTableName());
                        }
                        break;
                    }
                }
            }
            log.info("shadow table sync task completed, taskId={}, totalTableCount={}, succeedCount={}, failedCount={}",
                    taskId,
                    tables.size(),
                    succeedCount, failedCount);
            ShadowTaskTraceContextHolder.clear();
            return result;
        } finally {
            this.connectionSession.expire();
        }
    }

    private void init() {
        result = new ShadowTableSyncTaskResult();
        result.setShadowTableComparingId(shadowTableComparingId);
        result.setTables(tables.stream().map(comparing -> {
            TableSyncExecuting executing = new TableSyncExecuting();
            executing.setId(comparing.getId());
            executing.setOriginTableName(comparing.getOriginTableName());
            executing.setDestTableName(comparing.getDestTableName());
            executing.setStatus(TableSyncExecuteStatus.WAITING);
            return executing;
        }).collect(Collectors.toList()));
    }

    public ShadowTableSyncTaskResult getResult() {
        return this.result;
    }

    public Double getProgress() {
        if (totalTableCount == 0) {
            return 1D;
        }
        return (succeedCount + failedCount) * 100D / totalTableCount;
    }
}
