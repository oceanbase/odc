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
package com.oceanbase.odc.service.flow;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.flow.model.AbstractFlowTaskResult;
import com.oceanbase.odc.core.flow.model.FlowTaskResult;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.shared.exception.AccessDeniedException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.dispatch.DispatchResponse;
import com.oceanbase.odc.service.dispatch.RequestDispatcher;
import com.oceanbase.odc.service.dispatch.TaskDispatchChecker;
import com.oceanbase.odc.service.flow.instance.FlowInstance;
import com.oceanbase.odc.service.flow.instance.FlowTaskInstance;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.schedule.ScheduleLogProperties;
import com.oceanbase.odc.service.task.TaskService;
import com.oceanbase.odc.service.task.executor.logger.LogUtils;
import com.oceanbase.odc.service.task.model.ExecutorInfo;
import com.oceanbase.odc.service.task.model.OdcTaskLogLevel;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * @author mayang
 */
@Service
@Slf4j
@SkipAuthorize("odc internal usage")
public class FlowTaskInstanceLoggerService {

    @Autowired
    private FlowTaskInstanceService flowTaskInstanceService;

    @Autowired
    private RequestDispatcher requestDispatcher;

    @Autowired
    private TaskDispatchChecker dispatchChecker;

    @Autowired
    private TaskService taskService;

    @Autowired
    private ScheduleLogProperties loggerProperty;

    @Autowired
    private CloudObjectStorageService cloudObjectStorageService;

    @Autowired
    private FlowPermissionHelper flowPermissionHelper;

    public String getLogContent(OdcTaskLogLevel level, Long flowInstanceId) {
        Optional<TaskEntity> loggableTaskOption = flowTaskInstanceService.getLogDownloadableTaskEntity(flowInstanceId, flowPermissionHelper.withProjectMemberCheck());
        if (!loggableTaskOption.isPresent()) {
            log.warn("when to get log but the task is not exist, may not have permission, flowInstanceId={}", flowInstanceId);
            throw new AccessDeniedException(ErrorCodes.NotFound, ErrorCodes.TaskLogNotFound.getLocalizedMessage(new Object[] {"Id", flowInstanceId}));
        }
        return getLogContent(loggableTaskOption.get(), level, flowInstanceId);
    }

    public String getLogContentWithoutPermission(OdcTaskLogLevel level, Long flowInstanceId) {
        Optional<TaskEntity> loggableTaskOption = flowTaskInstanceService.getLogDownloadableTaskEntity(flowInstanceId, flowPermissionHelper.skipCheck());
        if (!loggableTaskOption.isPresent()) {
            log.warn("when to get log but the task is not exist, flowInstanceId={}", flowInstanceId);
            throw new NotFoundException(ErrorCodes.NotFound, new Object[] {flowInstanceId},
                ErrorCodes.TaskLogNotFound.getLocalizedMessage(new Object[] {"Id", flowInstanceId}));
        }
        return getLogContent(loggableTaskOption.get(), level, flowInstanceId);
    }

    public InputStreamResource downloadLogFile(Long flowInstanceId) {
        Optional<TaskEntity> loggableTaskOption = flowTaskInstanceService.getLogDownloadableTaskEntity(flowInstanceId, flowPermissionHelper.withProjectMemberCheck());
        if (!loggableTaskOption.isPresent()) {
            log.warn("when to download log but the task is not exist, may not have permission, flowInstanceId={}", flowInstanceId);
            throw new AccessDeniedException(ErrorCodes.NotFound, ErrorCodes.TaskLogNotFound.getLocalizedMessage(new Object[] {"Id", flowInstanceId}));
        }
        return new InputStreamResource(downloadLog(loggableTaskOption.get(), flowInstanceId));
    }

    @SneakyThrows
    public String getLogContent(TaskEntity taskEntity, OdcTaskLogLevel level,
            Long flowInstanceId) {
        if (!dispatchChecker.isTaskEntityOnThisMachine(taskEntity)) {
            ExecutorInfo executorInfo = JsonUtils.fromJson(taskEntity.getExecutor(), ExecutorInfo.class);
            try {
                DispatchResponse response = requestDispatcher.forward(executorInfo.getHost(), executorInfo.getPort());
                return response.getContentByType(new TypeReference<SuccessResponse<String>>() {}).getData();
            } catch (Exception e) {
                log.warn("forward request to get flow task log failed, host={}, port={}, flowInstanceId={}",
                        executorInfo.getHost(), executorInfo.getPort(), flowInstanceId, e);
                throw e;
            }
        }
        File logFile = getLogFile(flowInstanceId, taskEntity, level);
        return LogUtils.getLatestLogContent(logFile, loggerProperty.getMaxLines(), loggerProperty.getMaxSize());
    }

    @SneakyThrows
    private InputStream downloadLog(TaskEntity taskEntity, Long flowInstanceId) {
        if (!dispatchChecker.isTaskEntityOnThisMachine(taskEntity)) {
            ExecutorInfo executorInfo = JsonUtils.fromJson(taskEntity.getExecutor(), ExecutorInfo.class);
            try {
                DispatchResponse response = requestDispatcher.forward(executorInfo.getHost(), executorInfo.getPort());
                return new ByteArrayInputStream(response.getContent());
            } catch (Exception e) {
                log.warn("forward request to download flow task log failed, host={}, port={}, flowInstanceId={}",
                        executorInfo.getHost(), executorInfo.getPort(), flowInstanceId, e);
                throw e;
            }
        }
        File logFile = getLogFile(flowInstanceId, taskEntity, OdcTaskLogLevel.ALL);
        return IoUtil.toStream(logFile);
    }

    public File getFlowTaskLogFromCloudObjectStorage(String logFilePath, Long flowInstanceId, TaskEntity taskEntity) {
        if (taskEntity == null || taskEntity.getResultJson() == null) {
            throw new AccessDeniedException("flow task result not found, flowInstanceId=" + flowInstanceId);
        }
        if (!taskEntity.getTaskType().needsSetLogDownloadUrl()){
            log.warn("flow task get log not support task type = {}", taskEntity.getTaskType());
            return FileUtil.appendUtf8String(LogUtils.DEFAULT_LOG_CONTENT, new File(logFilePath));
        }
        FlowTaskResult taskResult = JsonUtils.fromJson(taskEntity.getResultJson(), FlowTaskResult.class);
        if (!(taskResult instanceof AbstractFlowTaskResult)) {
            throw new UnsupportedException("flow task get log not support type=" + taskResult.getClass());
        }
        String fullLogDownloadUrl = ((AbstractFlowTaskResult) taskResult).getFullLogDownloadUrl();
        if (StrUtil.isBlankIfStr(fullLogDownloadUrl)) {
            log.warn("local not exist flow task log, ready download from url={}, flowInstanceId={}", fullLogDownloadUrl,
                    flowInstanceId);
            return FileUtil.appendUtf8String(
                    "flow instance task log download url is not exist, may task log has not upload to cloud object storage, flowInstanceId=" + flowInstanceId, new File(logFilePath));
        }
        try {
            String bucketName = fullLogDownloadUrl.split("/")[0];
            String objectName = fullLogDownloadUrl.substring(bucketName.length() + 1);
            File tempFile = cloudObjectStorageService.downloadToTempFile(objectName);
            try (FileInputStream inputStream = new FileInputStream(tempFile)) {
                File logFile = new File(logFilePath);
                FileUtils.copyInputStreamToFile(inputStream, logFile);
                return logFile;
            } finally {
                FileUtils.deleteQuietly(tempFile);
            }
        } catch (Exception e) {
            log.warn("Illegal ossPath or file on OSS has already been deleted, fullLogDownloadUrl={}",
                    flowInstanceId, e);
            throw new NotFoundException(ResourceType.ODC_FILE, "fullLogDownloadUrl", fullLogDownloadUrl);
        }
    }

    private File getLogFile(long flowInstanceId, TaskEntity taskEntity, OdcTaskLogLevel logLevel) {
        String logFilePath = taskService.getLogFilePath(taskEntity.getCreatorId(), taskEntity.getId().toString(),
                taskEntity.getTaskType(), logLevel);
        File file = new File(logFilePath);
        log.info("get flow task log file, taskId={}, path={}，exist={}", taskEntity.getId(), logFilePath, file.exists());
        // only flow instance task terminated can upload log file to cloudObjectStorage
        if (!FileUtil.exist(file) && cloudObjectStorageService.supported()) {
            log.warn("local may not exist logfile={}, downloads it from OSS. flowInstanceId={}, taskId={}",
                    file.getAbsolutePath(), flowInstanceId, taskEntity.getId());
            return getFlowTaskLogFromCloudObjectStorage(logFilePath, flowInstanceId, taskEntity);
        }
        return file;
    }
}
