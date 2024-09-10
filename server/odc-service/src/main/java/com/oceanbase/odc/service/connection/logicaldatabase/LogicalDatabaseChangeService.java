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
package com.oceanbase.odc.service.connection.logicaldatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.ConflictException;
import com.oceanbase.odc.metadb.connection.logicaldatabase.LogicalDBChangeExecutionUnitEntity;
import com.oceanbase.odc.metadb.connection.logicaldatabase.LogicalDBExecutionRepository;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.ExecutionStatus;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.sql.SqlExecutionResultWrapper;
import com.oceanbase.odc.service.connection.logicaldatabase.core.model.LogicalDBChangeExecutionUnit;
import com.oceanbase.odc.service.connection.logicaldatabase.model.SqlExecutionUnitResp;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.odc.service.task.exception.JobException;

import lombok.NonNull;

/**
 * @Author: Lebie
 * @Date: 2024/9/3 20:09
 * @Description: []
 */
@Service
public class LogicalDatabaseChangeService {
    private final LogicalDatabaseExecutionMapper mapper = LogicalDatabaseExecutionMapper.INSTANCE;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private LogicalDBExecutionRepository executionRepository;

    @Autowired
    private JdbcLockRegistry jdbcLockRegistry;

    public boolean upsert(List<LogicalDBChangeExecutionUnit> executionUnits) throws InterruptedException {
        PreConditions.notEmpty(executionUnits, "executionUnits");
        Set<Long> scheduleTaskIds = new HashSet<>();
        executionUnits.stream().forEach(unit -> scheduleTaskIds.add(unit.getScheduleTaskId()));
        PreConditions.validSingleton(scheduleTaskIds, "scheduleTaskIds");
        Long scheduleTaskId = executionUnits.get(0).getScheduleTaskId();
        Lock lock = jdbcLockRegistry.obtain(getScheduleTaskIdLockKey(scheduleTaskId));
        List<LogicalDBChangeExecutionUnitEntity> entities = new ArrayList<>();
        if (!lock.tryLock(5, TimeUnit.SECONDS)) {
            throw new ConflictException(ErrorCodes.ResourceModifying, "Can not acquire jdbc lock");
        }
        try {
            executionUnits.stream().forEach(executionUnit -> {
                LogicalDBChangeExecutionUnitEntity entity;
                Optional<LogicalDBChangeExecutionUnitEntity> opt =
                        executionRepository.findByExecutionId(executionUnit.getExecutionId());
                if (opt.isPresent()) {
                    entity = opt.get();
                    entity.setExecutionResultJson(JsonUtils.toJson(executionUnit.getResult()));
                } else {
                    entity = mapper.modelToEntity(executionUnit);
                }
                entities.add(entity);
            });
            executionRepository.saveAll(entities);
        } finally {
            lock.unlock();
        }
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean skipCurrent(@NonNull Long scheduleTaskId, @NonNull Long physicalDatabaseId)
            throws InterruptedException, JobException {
        Lock lock = jdbcLockRegistry.obtain(getScheduleTaskIdLockKey(scheduleTaskId));
        if (!lock.tryLock(5, TimeUnit.SECONDS)) {
            throw new ConflictException(ErrorCodes.ResourceModifying, "Can not acquire jdbc lock");
        }
        try {
            Optional<LogicalDBChangeExecutionUnitEntity> unitOpt =
                    findCurrentExecutionUnit(scheduleTaskId, physicalDatabaseId);
            if (!unitOpt.isPresent()) {
                return false;
            }
            LogicalDBChangeExecutionUnitEntity unit = unitOpt.get();
            if (unit.getStatus() != ExecutionStatus.FAILED && unit.getStatus() != ExecutionStatus.TERMINATED) {
                return false;
            }
            unit.setStatus(ExecutionStatus.SKIPPED);
            executionRepository.save(unit);
            scheduleService.syncActionsToLogicalDatabaseTask(scheduleTaskId,
                    JobParametersKeyConstants.LOGICAL_DATABASE_CHANGE_SKIP_UNIT,
                    unit.getExecutionId());
        } finally {
            lock.unlock();
        }
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean terminateCurrent(@NonNull Long scheduleTaskId, @NonNull Long physicalDatabaseId)
            throws InterruptedException, JobException {
        Lock lock = jdbcLockRegistry.obtain(getScheduleTaskIdLockKey(scheduleTaskId));
        if (!lock.tryLock(5, TimeUnit.SECONDS)) {
            throw new ConflictException(ErrorCodes.ResourceModifying, "Can not acquire jdbc lock");
        }
        try {
            Optional<LogicalDBChangeExecutionUnitEntity> unitOpt =
                    findCurrentExecutionUnit(scheduleTaskId, physicalDatabaseId);
            if (!unitOpt.isPresent()) {
                return false;
            }
            LogicalDBChangeExecutionUnitEntity unit = unitOpt.get();
            if (unit.getStatus() != ExecutionStatus.RUNNING) {
                return false;
            }
            unit.setStatus(ExecutionStatus.TERMINATED);
            executionRepository.save(unit);
            scheduleService.syncActionsToLogicalDatabaseTask(scheduleTaskId,
                    JobParametersKeyConstants.LOGICAL_DATABASE_CHANGE_TERMINATE_UNIT,
                    unit.getExecutionId());

        } finally {
            lock.unlock();
        }
        return true;
    }

    public SqlExecutionUnitResp detail(@NonNull Long scheduleTaskId, @NonNull Long physicalDatabaseId) {
        List<LogicalDBChangeExecutionUnitEntity> entities =
                executionRepository.findByScheduleTaskIdAndPhysicalDatabaseIdOrderByExecutionOrderAsc(scheduleTaskId,
                        physicalDatabaseId);
        Database database = databaseService.detail(physicalDatabaseId);
        SqlExecutionUnitResp resp = new SqlExecutionUnitResp();
        resp.setId(physicalDatabaseId);
        resp.setDatabase(database);
        resp.setDataSource(database.getDataSource());
        resp.setTotalSqlCount(entities.size());
        int currentExecutionIndex = getCurrentIndex(entities);
        resp
                .setCompletedSqlCount(currentExecutionIndex + 1);
        resp.setStatus(entities.get(currentExecutionIndex).getStatus());
        resp.setSqlExecuteResults(entities.stream()
                .map(entity -> JsonUtils.fromJson(entity.getExecutionResultJson(), SqlExecutionResultWrapper.class))
                .collect(
                        Collectors.toList()));
        return resp;
    }

    public List<SqlExecutionUnitResp> listSqlExecutionUnits(@NonNull Long scheduleTaskId) {
        List<LogicalDBChangeExecutionUnitEntity> entities =
                executionRepository.findByScheduleTaskIdOrderByExecutionOrderAsc(scheduleTaskId);
        if (CollectionUtils.isEmpty(entities)) {
            return Collections.emptyList();
        }
        Map<Long, List<LogicalDBChangeExecutionUnitEntity>> databaseId2Executions = entities.stream()
                .collect(Collectors.groupingBy(LogicalDBChangeExecutionUnitEntity::getPhysicalDatabaseId));
        Map<Long, Database> id2Database = databaseService.listDatabasesDetailsByIds(databaseId2Executions.keySet())
                .stream().collect(Collectors.toMap(
                        Database::getId, database -> database));
        return databaseId2Executions.entrySet().stream().map(entry -> {
            SqlExecutionUnitResp resp = new SqlExecutionUnitResp();
            Database database = id2Database.get(entry.getKey());
            resp.setId(entry.getKey());
            resp.setDatabase(database);
            resp.setDataSource(database.getDataSource());
            List<LogicalDBChangeExecutionUnitEntity> executionUnits = entry.getValue();
            resp.setTotalSqlCount(executionUnits.size());
            int currentExecutionIndex = getCurrentIndex(executionUnits);
            resp
                    .setCompletedSqlCount(currentExecutionIndex + 1);
            resp.setStatus(executionUnits.get(currentExecutionIndex).getStatus());
            return resp;
        }).collect(Collectors.toList());
    }

    private int getCurrentIndex(List<LogicalDBChangeExecutionUnitEntity> executionUnits) {
        if (CollectionUtils.isEmpty(executionUnits)) {
            return 0;
        }
        Optional<LogicalDBChangeExecutionUnitEntity> last = executionUnits.stream().filter(
                unit -> unit.getStatus() != ExecutionStatus.SUCCESS && unit.getStatus() != ExecutionStatus.SKIPPED)
                .findFirst();
        if (last.isPresent()) {
            return Math.toIntExact(last.get().getExecutionOrder());
        } else {
            return executionUnits.size() - 1;
        }
    }

    private Optional<LogicalDBChangeExecutionUnitEntity> findCurrentExecutionUnit(@NonNull Long scheduleTaskId,
            @NonNull Long physicalDatabaseId) {
        return executionRepository
                .findByScheduleTaskIdAndPhysicalDatabaseIdOrderByExecutionOrderAsc(scheduleTaskId, physicalDatabaseId)
                .stream().filter(executionUnit -> !executionUnit.getStatus().isCompleted()).findFirst();
    }

    private String getScheduleTaskIdLockKey(Long scheduleTaskId) {
        return "logical-database-change-schedule-task-" + scheduleTaskId;
    }
}
