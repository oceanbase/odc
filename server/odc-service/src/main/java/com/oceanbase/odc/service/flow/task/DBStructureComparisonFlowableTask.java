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
package com.oceanbase.odc.service.flow.task;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.commons.lang3.Validate;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.structurecompare.StructureComparisonTaskEntity;
import com.oceanbase.odc.metadb.structurecompare.StructureComparisonTaskRepository;
import com.oceanbase.odc.metadb.structurecompare.StructureComparisonTaskResultEntity;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.flow.exception.ServiceTaskError;
import com.oceanbase.odc.service.flow.task.model.DBStructureComparisonParameter;
import com.oceanbase.odc.service.flow.task.model.DBStructureComparisonParameter.ComparisonScope;
import com.oceanbase.odc.service.flow.task.model.DBStructureComparisonParameter.DBStructureComparisonMapper;
import com.oceanbase.odc.service.flow.task.model.DBStructureComparisonTaskResult;
import com.oceanbase.odc.service.flow.task.model.DBStructureComparisonTaskResult.Comparing;
import com.oceanbase.odc.service.flow.util.FlowTaskUtil;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.util.SecurityContextUtils;
import com.oceanbase.odc.service.objectstorage.ObjectStorageFacade;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;
import com.oceanbase.odc.service.session.factory.DruidDataSourceFactory;
import com.oceanbase.odc.service.structurecompare.DefaultDBStructureComparator;
import com.oceanbase.odc.service.structurecompare.StructureComparisonService;
import com.oceanbase.odc.service.structurecompare.StructureComparisonTraceContextHolder;
import com.oceanbase.odc.service.structurecompare.model.DBObjectComparisonResult;
import com.oceanbase.odc.service.structurecompare.model.DBStructureComparisonConfig;
import com.oceanbase.odc.service.task.TaskService;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author jingtian
 * @date 2024/1/9
 * @since ODC_release_4.2.4
 */
@Slf4j
public class DBStructureComparisonFlowableTask extends BaseODCFlowTaskDelegate<Void> {
    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private DatabaseService databaseService;
    @Autowired
    private StructureComparisonTaskRepository structureComparisonTaskRepository;
    @Autowired
    private ObjectStorageFacade objectStorageFacade;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private StructureComparisonService structureComparisonService;
    private volatile DBStructureComparisonTaskResult taskResult =
            new DBStructureComparisonTaskResult(TaskStatus.PREPARING);
    private final String STRUCTURE_COMPARISON_RESULT_FILE_NAME = "structure-comparison-result.sql";
    private final String STRUCTURE_COMPARISON_BUCKET_NAME = "structure-comparison";
    private DefaultDBStructureComparator comparator = new DefaultDBStructureComparator();
    private DBStructureComparisonConfig srcConfig;
    private DBStructureComparisonConfig tgtConfig;

    @Override
    protected Void start(Long taskId, TaskService taskService, DelegateExecution execution)
            throws Exception {
        StructureComparisonTraceContextHolder.trace(authenticationFacade.currentUser().getId(), taskId);
        try {
            log.info("Structure comparison task starts, taskId={}, activityId={}", taskId,
                    execution.getCurrentActivityId());
            taskService.start(taskId, taskResult);
            TaskEntity taskEntity = taskService.detail(taskId);
            if (taskEntity == null) {
                throw new IllegalStateException("Can not find task entity by id " + taskId);
            }

            DBStructureComparisonParameter parameters = FlowTaskUtil.getDBStructureComparisonParameter(execution);
            Validate.notNull(parameters, "Structure comparison task parameters can not be null");

            StructureComparisonTaskEntity structureComparisonTaskEntity =
                    structureComparisonTaskRepository.saveAndFlush(DBStructureComparisonMapper.ofTaskEntity(parameters,
                            taskEntity.getCreatorId(), FlowTaskUtil.getFlowInstanceId(execution)));
            log.info("Successfully created a new structure comparison task entity, id={}",
                    structureComparisonTaskEntity.getId());

            srcConfig = initSourceComparisonConfig(parameters);
            tgtConfig = initTargetComparisonConfig(parameters);
            initTaskResult(srcConfig, structureComparisonTaskEntity.getId());
            taskService.updateResult(taskId, taskResult);

            log.info("Start to compare source and target schema, StructureComparisonTaskId={}", taskResult.getTaskId());
            long startTimestamp = System.currentTimeMillis();

            List<DBObjectComparisonResult> results = comparator.compare(srcConfig, tgtConfig);
            saveComparisonResult(results, tgtConfig.getConnectType().getDialectType());

            taskResult.setStatus(TaskStatus.DONE);
            log.info("Structure comparison task ends, id={}, task status={}, time consuming={} seconds",
                    taskResult.getTaskId(), taskResult.getStatus(),
                    (System.currentTimeMillis() - startTimestamp) / 1000);
        } catch (Exception e) {
            log.warn("Structure comparison task failed, taskId={}", taskId, e);
            throw new ServiceTaskError(e);
        } finally {
            closeDataSource(srcConfig.getDataSource());
            closeDataSource(tgtConfig.getDataSource());
            StructureComparisonTraceContextHolder.clear();
        }
        return null;
    }

    private DBStructureComparisonConfig initSourceComparisonConfig(DBStructureComparisonParameter parameters) {
        DBStructureComparisonConfig srcConfig = new DBStructureComparisonConfig();

        Database database = databaseService.detail((parameters.getSourceDatabaseId()));
        ConnectionConfig connectionConfig =
                connectionService.getForConnectionSkipPermissionCheck(database.getDataSource().getId());

        srcConfig.setSchemaName(database.getName());
        srcConfig.setConnectType(connectionConfig.getType());
        srcConfig.setDataSource(new DruidDataSourceFactory(connectionConfig).getDataSource());
        srcConfig.setToComparedObjectTypes(Collections.singleton(DBObjectType.TABLE));
        if (parameters.getComparisonScope() == ComparisonScope.PART) {
            Map<DBObjectType, Set<String>> blackListMap = new HashMap<>();
            blackListMap.put(DBObjectType.TABLE, new HashSet<>(parameters.getTableNamesToBeCompared()));
            srcConfig.setBlackListMap(blackListMap);
        }

        return srcConfig;
    }

    private DBStructureComparisonConfig initTargetComparisonConfig(DBStructureComparisonParameter parameters) {
        DBStructureComparisonConfig tgtConfig = new DBStructureComparisonConfig();

        Database database = databaseService.detail(parameters.getTargetDatabaseId());
        ConnectionConfig connectionConfig =
                connectionService.getForConnectionSkipPermissionCheck(database.getDataSource().getId());
        tgtConfig.setSchemaName(database.getName());
        tgtConfig.setConnectType(connectionConfig.getType());
        tgtConfig.setDataSource(new DruidDataSourceFactory(connectionConfig).getDataSource());
        return tgtConfig;
    }

    private void initTaskResult(@NonNull DBStructureComparisonConfig srcConfig,
            @NonNull Long structureComparisonTaskId) {
        this.taskResult.setTaskId(structureComparisonTaskId);
        this.taskResult.setStatus(TaskStatus.RUNNING);
        if (!srcConfig.getBlackListMap().isEmpty()) {
            this.taskResult.setComparingList(srcConfig.getBlackListMap().get(DBObjectType.TABLE).stream()
                    .map(tableName -> new Comparing(DBObjectType.TABLE, tableName))
                    .collect(Collectors.toList()));
        }
    }

    private void saveComparisonResult(List<DBObjectComparisonResult> results, DialectType dialectType) {
        List<StructureComparisonTaskResultEntity> entities =
                structureComparisonService.batchCreateTaskResults(results, dialectType, this.taskResult.getTaskId());

        StringBuilder builder = new StringBuilder();
        entities.stream().filter(entity -> Objects.nonNull(entity.getChangeSqlScript()))
                .forEach(entity -> builder.append(entity.getChangeSqlScript()));

        String totalChangeSqlScript = builder.toString();
        if (!totalChangeSqlScript.isEmpty()) {
            String objectId = putTotalChangeSqlScript(totalChangeSqlScript);
            if (Objects.nonNull(objectId)) {
                structureComparisonTaskRepository.updateStorageObjectIdById(taskResult.getTaskId(), objectId);
            }
        }
    }

    private String putTotalChangeSqlScript(String changeSqlScript) {
        try {
            String bucketName =
                    STRUCTURE_COMPARISON_BUCKET_NAME.concat(File.separator)
                            .concat(authenticationFacade.currentUserIdStr());
            objectStorageFacade.createBucketIfNotExists(bucketName);
            InputStream inputStream = new ByteArrayInputStream(changeSqlScript.getBytes());
            long totalSize = changeSqlScript.getBytes().length;
            ObjectMetadata metadata = objectStorageFacade.putObject(bucketName, STRUCTURE_COMPARISON_RESULT_FILE_NAME,
                    totalSize, inputStream);
            return metadata.getObjectId();
        } catch (Exception e) {
            log.warn(
                    "Failed to put structure comparison result file for structure comparison task, StructureComparisonTaskId={}, error message={}",
                    taskResult.getTaskId(), e.getMessage(), e);
            return null;
        }
    }

    private void closeDataSource(DataSource dataSource) {
        if (dataSource instanceof AutoCloseable) {
            try {
                ((AutoCloseable) dataSource).close();
            } catch (Exception e) {
                log.warn("Structure comparison task failed to close dataSource, structureComparisonTaskId={}",
                        this.taskResult.getTaskId(), e);
            }
        }
    }

    @Override
    protected boolean cancel(boolean mayInterruptIfRunning, Long taskId, TaskService taskService) {
        throw new UnsupportedOperationException("Structure comparison task can not be canceled");
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    protected boolean isSuccessful() {
        return this.taskResult.getStatus() == TaskStatus.DONE;
    }

    @Override
    protected boolean isFailure() {
        return this.taskResult.getStatus() == TaskStatus.FAILED;
    }

    @Override
    protected void onTimeout(Long taskId, TaskService taskService) {
        SecurityContextUtils.setCurrentUser(authenticationFacade.currentUser());
        StructureComparisonTraceContextHolder.trace(authenticationFacade.currentUser().getId(), taskId);
        try {
            log.warn("Structure comparison task timeout, taskId={}", taskId);
        } finally {
            StructureComparisonTraceContextHolder.clear();
        }
        super.onTimeout(taskId, taskService);
    }

    @Override
    protected void onFailure(Long taskId, TaskService taskService) {
        SecurityContextUtils.setCurrentUser(authenticationFacade.currentUser());
        StructureComparisonTraceContextHolder.trace(authenticationFacade.currentUser().getId(), taskId);
        try {
            log.warn("Structure comparison task failed, taskId={}", taskId);
            taskResult.setStatus(TaskStatus.FAILED);
            taskService.fail(taskId, comparator.getProgress(), taskResult);
            super.onFailure(taskId, taskService);
        } finally {
            StructureComparisonTraceContextHolder.clear();
        }

        super.onFailure(taskId, taskService);
    }

    @Override
    protected void onSuccessful(Long taskId, TaskService taskService) {
        SecurityContextUtils.setCurrentUser(authenticationFacade.currentUser());
        StructureComparisonTraceContextHolder.trace(authenticationFacade.currentUser().getId(), taskId);
        log.info("Structure comparison task succeed, taskId={}", taskId);
        try {
            taskService.succeed(taskId, taskResult);
            updateFlowInstanceStatus(FlowStatus.EXECUTION_SUCCEEDED);
            super.onSuccessful(taskId, taskService);
        } catch (Exception e) {
            log.warn("Failed to record structure comparison task successful result", e);
        } finally {
            StructureComparisonTraceContextHolder.clear();
        }
        super.onSuccessful(taskId, taskService);
    }

    @Override
    protected void onProgressUpdate(Long taskId, TaskService taskService) {
        taskService.updateProgress(taskId, comparator.getProgress());
    }

}
