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

import java.io.File;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.dispatch.DispatchResponse;
import com.oceanbase.odc.service.dispatch.RequestDispatcher;
import com.oceanbase.odc.service.dispatch.TaskDispatchChecker;
import com.oceanbase.odc.service.schedule.ScheduleLogProperties;
import com.oceanbase.odc.service.task.TaskService;
import com.oceanbase.odc.service.task.executor.logger.LogUtils;
import com.oceanbase.odc.service.task.model.ExecutorInfo;
import com.oceanbase.odc.service.task.model.OdcTaskLogLevel;

import cn.hutool.core.io.FileUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * @author mayang
 */
@Service
@Slf4j
public class FlowTaskInstanceLoggerService {

    private final FlowTaskInstanceService flowTaskInstanceService;
    private final RequestDispatcher requestDispatcher;
    private final TaskDispatchChecker dispatchChecker;
    private final TaskService taskService;
    private final ScheduleLogProperties loggerProperty;

    public FlowTaskInstanceLoggerService(FlowTaskInstanceService flowTaskInstanceService,
            RequestDispatcher requestDispatcher,
            TaskDispatchChecker dispatchChecker,
            TaskService taskService,
            ScheduleLogProperties loggerProperty) {
        this.flowTaskInstanceService = flowTaskInstanceService;
        this.requestDispatcher = requestDispatcher;
        this.dispatchChecker = dispatchChecker;
        this.taskService = taskService;
        this.loggerProperty = loggerProperty;
    }

    @SneakyThrows
    public String getLogContent(OdcTaskLogLevel level, Long flowInstanceId) {
        Optional<TaskEntity> taskEntityOptional =
                flowTaskInstanceService.getLogDownloadableTaskEntity(flowInstanceId, false);
        return getLogContent(taskEntityOptional, level, flowInstanceId);
    }

    @SneakyThrows
    public String getLogContentWithoutPermission(OdcTaskLogLevel level, Long flowInstanceId) {
        Optional<TaskEntity> taskEntityOptional =
                flowTaskInstanceService.getLogDownloadableTaskEntity(flowInstanceId, true);
        return getLogContent(taskEntityOptional, level, flowInstanceId);
    }

    @SneakyThrows
    private String getLogContent(Optional<TaskEntity> taskEntityOptional, OdcTaskLogLevel level, Long flowInstanceId) {
        if (!taskEntityOptional.isPresent()) {
            log.warn("get log failed, flowInstanceId={}", flowInstanceId);
            return LogUtils.DEFAULT_LOG_CONTENT;
        }
        TaskEntity taskEntity = taskEntityOptional.get();
        if (!dispatchChecker.isTaskEntityOnThisMachine(taskEntity)) {
            ExecutorInfo executorInfo = JsonUtils.fromJson(taskEntity.getExecutor(), ExecutorInfo.class);
            DispatchResponse response = requestDispatcher.forward(executorInfo.getHost(), executorInfo.getPort());
            return response.getContentByType(new TypeReference<SuccessResponse<String>>() {}).getData();
        }
        return getLog(taskEntity.getCreatorId(), taskEntity.getId() + "", taskEntity.getTaskType(), level);
    }

    @SneakyThrows
    public File downloadLog(Long flowInstanceId) {
        Optional<TaskEntity> taskEntityOptional =
                flowTaskInstanceService.getLogDownloadableTaskEntity(flowInstanceId, false);
        TaskEntity taskEntity = taskEntityOptional
                .orElseThrow(() -> new NotFoundException(ErrorCodes.NotFound, new Object[] {flowInstanceId},
                        ErrorCodes.TaskLogNotFound.getLocalizedMessage(new Object[] {"Id", flowInstanceId})));
        if (!dispatchChecker.isTaskEntityOnThisMachine(taskEntity)) {
            ExecutorInfo executorInfo = JsonUtils.fromJson(taskEntity.getExecutor(), ExecutorInfo.class);
            DispatchResponse response = requestDispatcher.forward(executorInfo.getHost(), executorInfo.getPort());
            return response.getContentByType(new TypeReference<SuccessResponse<File>>() {}).getData();
        }
        return getLogFile(taskEntity.getCreatorId(), taskEntity.getId() + "", taskEntity.getTaskType(),
                OdcTaskLogLevel.ALL);
    }

    public File getLogFile(Long userId, String flowInstanceId, TaskType type, OdcTaskLogLevel logLevel) {
        String logFilePath = taskService.getLogFilePath(userId, flowInstanceId, type, logLevel);
        try {
            return taskService.getLogFile(logFilePath);
        } catch (NotFoundException ex) {
            log.warn(ErrorCodes.TaskLogNotFound.getEnglishMessage(new Object[] {"Id", flowInstanceId}));
            return FileUtil.writeUtf8String(LogUtils.DEFAULT_LOG_CONTENT, logFilePath);
        }
    }

    private String getLog(Long userId, String jobId, TaskType type, OdcTaskLogLevel logLevel) {
        return LogUtils.getLatestLogContent(getLogFile(userId, jobId, type, logLevel),
                loggerProperty.getMaxLines(),
                loggerProperty.getMaxSize());
    }
}
