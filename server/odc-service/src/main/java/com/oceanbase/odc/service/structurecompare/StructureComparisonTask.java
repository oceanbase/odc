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
package com.oceanbase.odc.service.structurecompare;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.structurecompare.StructureComparisonTaskRepository;
import com.oceanbase.odc.metadb.structurecompare.StructureComparisonTaskResultEntity;
import com.oceanbase.odc.metadb.structurecompare.StructureComparisonTaskResultRepository;
import com.oceanbase.odc.service.flow.task.model.DBStructureComparisonTaskResult;
import com.oceanbase.odc.service.flow.task.model.DBStructureComparisonTaskResult.Comparing;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.iam.util.SecurityContextUtils;
import com.oceanbase.odc.service.objectstorage.ObjectStorageFacade;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;
import com.oceanbase.odc.service.structurecompare.model.DBObjectComparisonResult;
import com.oceanbase.odc.service.structurecompare.model.DBStructureComparisonConfig;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author jingtian
 * @date 2024/1/18
 * @since ODC_release_4.2.4
 */
@Slf4j
public class StructureComparisonTask implements Callable<DBStructureComparisonTaskResult> {
    private final DBStructureComparisonConfig srcConfig;
    private final DBStructureComparisonConfig tgtConfig;
    private final Long taskId;
    private final User user;
    private StructureComparisonTaskResultRepository structureComparisonTaskResultRepository;
    private StructureComparisonTaskRepository structureComparisonTaskRepository;
    private ObjectStorageFacade objectStorageFacade;
    private DefaultDBStructureComparator comparator = new DefaultDBStructureComparator();
    @Getter
    private volatile DBStructureComparisonTaskResult taskResult =
            new DBStructureComparisonTaskResult(TaskStatus.PREPARING);
    private final String STRUCTURE_COMPARISON_RESULT_FILE_NAME = "structure-comparison-result.sql";
    private final String STRUCTURE_COMPARISON_BUCKET_NAME = "structure-comparison";

    public StructureComparisonTask(@NonNull DBStructureComparisonConfig srcConfig,
            @NonNull DBStructureComparisonConfig tgtConfig,
            @NonNull Long taskId,
            @NonNull Long structureComparisonTaskId,
            @NonNull User user,
            @NonNull StructureComparisonTaskRepository structureComparisonTaskRepository,
            @NonNull StructureComparisonTaskResultRepository structureComparisonTaskResultRepository,
            @NonNull ObjectStorageFacade objectStorageFacade) {
        this.srcConfig = srcConfig;
        this.tgtConfig = tgtConfig;
        initTaskResult(structureComparisonTaskId);
        this.taskId = taskId;
        this.user = user;
        this.structureComparisonTaskResultRepository = structureComparisonTaskResultRepository;
        this.structureComparisonTaskRepository = structureComparisonTaskRepository;
        this.objectStorageFacade = objectStorageFacade;
    }

    private void initTaskResult(Long structureComparisonTaskId) {
        this.taskResult.setTaskId(structureComparisonTaskId);
        if (!srcConfig.getBlackListMap().isEmpty()) {
            this.taskResult.setComparingList(srcConfig.getBlackListMap().get(DBObjectType.TABLE).stream()
                    .map(tableName -> new Comparing(DBObjectType.TABLE, tableName))
                    .collect(Collectors.toList()));
        }
    }

    @Override
    public DBStructureComparisonTaskResult call() throws Exception {
        SecurityContextUtils.setCurrentUser(user);
        StructureComparisonTraceContextHolder.trace(user.getId(), taskId);
        try {
            log.info("Structure comparison task starts, id={}", taskResult.getTaskId());
            long startTimestamp = System.currentTimeMillis();
            taskResult.setStatus(TaskStatus.RUNNING);
            try {
                List<DBObjectComparisonResult> results = comparator.compare(srcConfig, tgtConfig);
                saveComparisonResult(results, tgtConfig.getConnectType().getDialectType());
                taskResult.setStatus(TaskStatus.DONE);
            } catch (Exception e) {
                log.warn("Failed to run structure comparison task, id={}, error message={}", taskResult.getTaskId(),
                        e.getMessage(), e);
                taskResult.setStatus(TaskStatus.FAILED);
            }
            log.info("Structure comparison task ends, id={}, task status={}, time consuming={} seconds",
                    taskResult.getTaskId(), taskResult.getStatus(),
                    (System.currentTimeMillis() - startTimestamp) / 1000);
            return taskResult;
        } finally {
            StructureComparisonTraceContextHolder.clear();
        }
    }

    private void saveComparisonResult(List<DBObjectComparisonResult> results, DialectType dialectType) {
        List<StructureComparisonTaskResultEntity> entities =
                results.stream().map(result -> result.toEntity(taskResult.getTaskId(), dialectType)).collect(
                        Collectors.toList());
        structureComparisonTaskResultRepository.saveAll(entities);

        StringBuilder builder = new StringBuilder();
        entities.stream().filter(entity -> Objects.nonNull(entity.getChangeSqlScript()))
                .forEach(entity -> builder.append(entity.getChangeSqlScript()));

        String totalChangeSqlScript = builder.toString();
        if (!totalChangeSqlScript.isEmpty()) {
            String objectId = putTotalChangeSqlScript(totalChangeSqlScript);
            structureComparisonTaskRepository.updateStorageObjectIdById(taskResult.getTaskId(), objectId);
        }
    }

    private String putTotalChangeSqlScript(String changeSqlScript) {
        try {
            String bucketName =
                    STRUCTURE_COMPARISON_BUCKET_NAME.concat(File.separator).concat(user.getId().toString());
            objectStorageFacade.createBucketIfNotExists(bucketName);
            InputStream inputStream = new ByteArrayInputStream(changeSqlScript.getBytes());
            long totalSize = changeSqlScript.getBytes().length;
            ObjectMetadata metadata =
                    objectStorageFacade.putObject(bucketName, STRUCTURE_COMPARISON_RESULT_FILE_NAME, totalSize,
                            inputStream);
            return metadata.getObjectId();
        } catch (Exception e) {
            log.warn(
                    "Failed to put structure comparison result file for structure comparison task, id={}, error message={}",
                    taskResult.getTaskId(), e.getMessage(), e);
            return null;
        }
    }

    public Double getProgress() {
        return comparator.getProgress();
    }

}
