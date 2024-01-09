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
package com.oceanbase.odc.service.task;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import javax.validation.constraints.NotNull;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.flow.model.FlowTaskResult;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.metadb.task.TaskRepository;
import com.oceanbase.odc.metadb.task.TaskSpecs;
import com.oceanbase.odc.service.common.model.HostProperties;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.flow.model.QueryTaskInstanceParams;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.task.model.ExecutorInfo;
import com.oceanbase.odc.service.task.model.OdcTaskLogLevel;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2022/2/17
 */

@Validated
@Service
@Slf4j
@SkipAuthorize("odc internal usage")
public class TaskService {
    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private HostProperties properties;

    private static String logFilePrefix;
    private static final String ASYNC_LOG_PATH_PATTERN = "%s/async/%d/%s/asynctask.%s";
    private static final String MOCKDATA_LOG_PATH_PATTERN = "%s/data-mocker/%s/ob-mocker.%s";
    private static final String DATATRANSFER_LOG_PATH_PATTERN = "%s/data-transfer/%s/ob-loader-dumper.%s";
    private static final String SHADOWTABLE_LOG_PATH_PATTERN = "%s/shadowtable/%d/%s/shadowtable.%s";

    private static final String ALTER_SCHEDULE_LOG_PATH_PATTERN = "%s/alterschedule/%d/%s/alterschedule.%s";
    private static final String PARTITIONPLAN_LOG_PATH_PATTERN = "%s/partitionplan/%d/%s/partitionplan.%s";
    private static final long MAX_LOG_LINE_COUNT = 10000;
    private static final long MAX_LOG_BYTE_COUNT = 1024 * 1024;
    private static final String ONLINE_SCHEMA_CHANGE_LOG_PATH_PATTERN =
            "%s/onlineschemachange/%d/%s/onlineschemachange.%s";
    private static final String EXPORT_RESULT_SET_LOG_PATH_PATTERN = "%s/result-set-export/%s/ob-loader-dumper.%s";
    private static final String APPLY_PROJECT_LOG_PATH_PATTERN = "%s/apply-project/%d/%s/apply-project-task.%s";
    private static final String APPLY_DATABASE_LOG_PATH_PATTERN = "%s/apply-database/%d/%s/apply-database-task.%s";

    @Autowired
    public TaskService(@Value("${odc.log.directory:./log}") String baseTaskLogDir) {
        logFilePrefix = baseTaskLogDir;
        log.info("Task service initialized");
    }

    @Transactional(rollbackFor = Exception.class)
    public TaskEntity create(@NotNull CreateFlowInstanceReq req, int executionExpirationIntervalSeconds) {
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setCreatorId(authenticationFacade.currentUserId());
        taskEntity.setOrganizationId(authenticationFacade.currentOrganizationId());

        TaskType taskType = req.getTaskType();
        taskEntity.setTaskType(taskType);
        taskEntity.setConnectionId(req.getConnectionId());
        taskEntity.setExecutionExpirationIntervalSeconds(executionExpirationIntervalSeconds);
        taskEntity.setDatabaseName(req.getDatabaseName());
        taskEntity.setDatabaseId(req.getDatabaseId());
        taskEntity.setDescription(req.getDescription());
        taskEntity.setParametersJson(JsonUtils.toJson(req.getParameters()));

        String currentExecutor = JsonUtils.toJson(new ExecutorInfo(properties));
        taskEntity.setSubmitter(currentExecutor);
        taskEntity.setExecutor(currentExecutor);
        taskEntity.setStatus(TaskStatus.PREPARING);
        TaskEntity entity = taskRepository.saveAndFlush(taskEntity);
        log.info("Task record has been created, taskId={}, createFlowInstanceReq={}", entity.getId(), req);
        return entity;
    }

    @Transactional(rollbackFor = Exception.class)
    public int update(@NotNull TaskEntity taskEntity) {
        int affectRows = taskRepository.update(taskEntity);
        log.info("Task record has been updated, taskEntity={}, affectRows={}", taskEntity, affectRows);
        return affectRows;
    }

    @Transactional(rollbackFor = Exception.class)
    public int updateParametersJson(@NotNull TaskEntity taskEntity) {
        int affectRows = taskRepository.updateParametersJson(taskEntity);
        log.info("Task parametersJson has been updated, taskEntity={}, affectRows={}", taskEntity, affectRows);
        return affectRows;
    }

    @Transactional(rollbackFor = Exception.class)
    public int updateExecutorInfo(@NotNull Long taskId, @NonNull ExecutorInfo executorInfo) {
        int affectRows = taskRepository.updateExecutorById(taskId, JsonUtils.toJson(executorInfo));
        log.info("Task ExecutorInfo has been updated, taskId={}, executorInfo={}, affectRows={}", taskId,
                executorInfo, affectRows);
        return affectRows;
    }

    public Page<TaskEntity> list(@NotNull Pageable pageable, @NotNull QueryTaskInstanceParams params) {
        Specification<TaskEntity> specification = Specification
                .where(TaskSpecs.statusIn(params.getStatuses()))
                .and(TaskSpecs.taskTypeEquals(params.getType()))
                .and(TaskSpecs.organizationIdEquals(authenticationFacade.currentOrganizationId()))
                .and(TaskSpecs.connectionIdIn(params.getConnectionIds()))
                .and(TaskSpecs.idEquals(params.getId()))
                .and(TaskSpecs.createTimeLaterThan(params.getStartTime()))
                .and(TaskSpecs.createTimeEarlierThan(params.getEndTime()));
        if (Objects.nonNull(params.getCreatedByCurrentUser()) && params.getCreatedByCurrentUser()) {
            specification.and(TaskSpecs.creatorIdEquals(authenticationFacade.currentUserId()));
        }
        if (StringUtils.isNotBlank(params.getFuzzyDatabaseName())) {
            specification.and(TaskSpecs.databaseNameLike(params.getFuzzyDatabaseName()));
        }
        Page<TaskEntity> taskEntities = taskRepository.findAll(specification, pageable);
        return taskEntities;
    }

    public TaskEntity detail(Long id) {
        return nullSafeFindById(id);
    }

    public String getLog(Long userId, String taskId, TaskType type, OdcTaskLogLevel logLevel) throws IOException {
        // TODO: fix file path traversal issue
        String filePath;
        switch (type) {
            case ASYNC:
                filePath = String.format(ASYNC_LOG_PATH_PATTERN, logFilePrefix, userId, taskId,
                        logLevel.name().toLowerCase());
                break;
            case MOCKDATA:
                filePath =
                        String.format(MOCKDATA_LOG_PATH_PATTERN, logFilePrefix, taskId, logLevel.name().toLowerCase());
                break;
            case EXPORT:
            case IMPORT:
                filePath = String.format(DATATRANSFER_LOG_PATH_PATTERN, logFilePrefix, taskId,
                        logLevel.name().toLowerCase());
                break;
            case SHADOWTABLE_SYNC:
                filePath = String.format(SHADOWTABLE_LOG_PATH_PATTERN, logFilePrefix, userId, taskId,
                        logLevel.name().toLowerCase());
                break;
            case PARTITION_PLAN:
                filePath = String.format(PARTITIONPLAN_LOG_PATH_PATTERN, logFilePrefix, userId, taskId,
                        logLevel.name().toLowerCase());
                break;
            case ALTER_SCHEDULE:
                filePath = String.format(ALTER_SCHEDULE_LOG_PATH_PATTERN, logFilePrefix, userId, taskId,
                        logLevel.name().toLowerCase());
                break;
            case ONLINE_SCHEMA_CHANGE:
                filePath = String.format(ONLINE_SCHEMA_CHANGE_LOG_PATH_PATTERN, logFilePrefix, userId, taskId,
                        logLevel.name().toLowerCase());
                break;
            case EXPORT_RESULT_SET:
                filePath = String.format(EXPORT_RESULT_SET_LOG_PATH_PATTERN, logFilePrefix, taskId,
                        logLevel.name().toLowerCase());
                break;
            case APPLY_PROJECT_PERMISSION:
                filePath = String.format(APPLY_PROJECT_LOG_PATH_PATTERN, logFilePrefix, userId, taskId,
                        logLevel.name().toLowerCase());
                break;
            case APPLY_DATABASE_PERMISSION:
                filePath = String.format(APPLY_DATABASE_LOG_PATH_PATTERN, logFilePrefix, userId, taskId,
                        logLevel.name().toLowerCase());
                break;
            default:
                throw new UnsupportedException(ErrorCodes.Unsupported, new Object[] {ResourceType.ODC_TASK},
                        "Unsupported task type: " + type);
        }

        if (!new File(filePath).exists()) {
            return ErrorCodes.TaskLogNotFound.getLocalizedMessage(new Object[] {"Id", taskId});
        }
        LineIterator it = null;
        StringBuilder sb = new StringBuilder();
        int lineCount = 1;
        int byteCount = 0;
        try {
            it = FileUtils.lineIterator(new File(filePath));
            while (it.hasNext()) {
                if (lineCount > MAX_LOG_LINE_COUNT || byteCount > MAX_LOG_BYTE_COUNT) {
                    sb.append("Logs exceed max limitation (10000 rows or 1 MB), please download logs directly");
                    break;
                }
                String line = it.nextLine();
                sb.append(line).append("\n");
                lineCount++;
                byteCount = byteCount + line.getBytes().length;
            }
            return sb.toString();
        } catch (Exception ex) {
            log.warn("read task log file failed, reason={}", ex.getMessage());
            throw new UnexpectedException("read task log file failed, reason: " + ex.getMessage(), ex);
        } finally {
            LineIterator.closeQuietly(it);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void start(Long id) {
        innerStart(id, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void start(@NonNull Long id, Object taskResult) {
        innerStart(id, taskResult);
    }

    @Transactional(rollbackFor = Exception.class)
    public void succeed(Long id, Object taskResult) {
        TaskEntity taskEntity = nullSafeFindById(id);
        taskEntity.setStatus(TaskStatus.DONE);
        taskEntity.setProgressPercentage(100);
        taskEntity.setResultJson(JsonUtils.toJson(taskResult));
        taskRepository.save(taskEntity);
        log.info("Task ended: taskId={}", id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void fail(Long id, double percentage, Object taskResult) {
        TaskEntity taskEntity = nullSafeFindById(id);
        taskEntity.setStatus(TaskStatus.FAILED);
        taskEntity.setProgressPercentage(percentage);
        taskEntity.setResultJson(JsonUtils.toJson(taskResult));
        taskRepository.save(taskEntity);
        log.info("Task ended: taskId={}", id);
    }


    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        innerCancel(id, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id, Object taskResult) {
        innerCancel(id, taskResult);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateProgress(Long id, double progressPercentage) {
        TaskEntity taskEntity = nullSafeFindById(id);
        taskEntity.setProgressPercentage(progressPercentage);
        taskRepository.save(taskEntity);
        if (log.isDebugEnabled()) {
            log.debug("Task progress has been updated: taskId={}, progressPercentage={}", id, progressPercentage);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateResult(Long id, FlowTaskResult result) {
        TaskEntity taskEntity = nullSafeFindById(id);
        taskEntity.setResultJson(JsonUtils.toJson(result));
        taskRepository.save(taskEntity);
        if (log.isDebugEnabled()) {
            log.debug("Task result has been updated: taskId={}", id);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        TaskEntity taskEntity = nullSafeFindById(id);
        taskRepository.delete(taskEntity);
        log.info("Task has been deleted: taskId={}", id);
    }

    private TaskEntity nullSafeFindById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_TASK, "id", id));
    }

    private void innerCancel(@NonNull Long id, Object taskResult) {
        TaskEntity taskEntity = nullSafeFindById(id);
        taskEntity.setStatus(TaskStatus.CANCELED);
        taskRepository.save(taskEntity);
        if (Objects.nonNull(taskResult)) {
            taskEntity.setResultJson(JsonUtils.toJson(taskResult));
        }
        log.info("Task has been canceled: taskId={}", id);
    }

    private void innerStart(@NonNull Long id, Object taskResult) {
        TaskEntity taskEntity = nullSafeFindById(id);
        taskEntity.setStatus(TaskStatus.RUNNING);
        taskEntity.setProgressPercentage(0.0);
        if (taskResult != null) {
            taskEntity.setResultJson(JsonUtils.toJson(taskResult));
        }
        taskRepository.save(taskEntity);
        log.info("Task started: taskId={}", id);
    }

}
