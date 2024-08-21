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
package com.oceanbase.odc.service.logger;

import java.io.File;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
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
import com.oceanbase.odc.service.flow.FlowTaskInstanceService;
import com.oceanbase.odc.service.task.TaskService;
import com.oceanbase.odc.service.task.model.ExecutorInfo;
import com.oceanbase.odc.service.task.model.OdcTaskLogLevel;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * @author mayang
 */
@Service("flowLoggerService")
@Slf4j
@SuppressWarnings("all")
public class FlowLoggerServiceImpl extends AbstractLoggerService implements ILoggerService {

    private final FlowTaskInstanceService flowTaskInstanceService;
    private final RequestDispatcher requestDispatcher;
    private final TaskDispatchChecker dispatchChecker;
    private final TaskService taskService;

    @Value("${odc.log.maxLogLimitedCount: 10000}")
    private Long maxLogLimitedCount;

    // unit：B
    @Value("${odc.log.maxLogSizeCount: #{1024 * 1024}}")
    private Long maxLogSizeCount;

    public FlowLoggerServiceImpl(FlowTaskInstanceService flowTaskInstanceService,
            RequestDispatcher requestDispatcher,
            TaskDispatchChecker dispatchChecker,
            TaskService taskService) {
        this.flowTaskInstanceService = flowTaskInstanceService;
        this.requestDispatcher = requestDispatcher;
        this.dispatchChecker = dispatchChecker;
        this.taskService = taskService;
    }

    @Override
    @SneakyThrows
    public String getLog(OdcTaskLogLevel level, Long jobId, boolean skipAuth) {
        Optional<TaskEntity> taskEntityOptional = flowTaskInstanceService.getLogDownloadableTaskEntity(jobId, skipAuth);
        if (!taskEntityOptional.isPresent()) {
            log.warn("get log failed, jobId: {}, skipAuth: {}", jobId, skipAuth);
            return "get log failed";
        }
        TaskEntity taskEntity = taskEntityOptional.get();
        if (!dispatchChecker.isTaskEntityOnThisMachine(taskEntity)) {
            ExecutorInfo executorInfo = JsonUtils.fromJson(taskEntity.getExecutor(), ExecutorInfo.class);
            DispatchResponse response = requestDispatcher.forward(executorInfo.getHost(), executorInfo.getPort());
            return response.getContentByType(new TypeReference<SuccessResponse<String>>() {}).getData();
        }
        return getLog(taskEntity.getCreatorId(), taskEntity.getId() + "", taskEntity.getTaskType(), level);
    }

    @Override
    @SneakyThrows
    public File downloadLog(Long jobId, boolean skipAuth) {
        Optional<TaskEntity> taskEntityOptional = flowTaskInstanceService.getLogDownloadableTaskEntity(jobId, skipAuth);
        if (!taskEntityOptional.isPresent()) {
            throw new NotFoundException(ErrorCodes.NotFound, new Object[] {jobId},
                    ErrorCodes.TaskLogNotFound.getLocalizedMessage(new Object[] {"Id", jobId}));
        }
        TaskEntity taskEntity = taskEntityOptional.get();
        if (!dispatchChecker.isTaskEntityOnThisMachine(taskEntity)) {
            ExecutorInfo executorInfo = JsonUtils.fromJson(taskEntity.getExecutor(), ExecutorInfo.class);
            DispatchResponse response = requestDispatcher.forward(executorInfo.getHost(), executorInfo.getPort());
            return response.getContentByType(new TypeReference<SuccessResponse<File>>() {}).getData();
        }
        return getLogFile(taskEntity.getCreatorId(), taskEntity.getId() + "", taskEntity.getTaskType(),
                OdcTaskLogLevel.ALL);
    }

    public File getLogFile(Long userId, String jobId, TaskType type, OdcTaskLogLevel logLevel) {
        try {
            return taskService.getLogFile(userId, jobId, type, logLevel);
        } catch (NotFoundException ex) {
            log.warn(ErrorCodes.TaskLogNotFound.getLocalizedMessage(new Object[] {"Id", jobId}));
            return null;
        }
    }

    private String getLog(Long userId, String jobId, TaskType type, OdcTaskLogLevel logLevel) {
        return readLog(getLogFile(userId, jobId, type, logLevel), maxLogLimitedCount, maxLogSizeCount);
    }
}
