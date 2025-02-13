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
import java.io.InputStream;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
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

import cn.hutool.core.io.IoUtil;
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
    private FlowPermissionHelper flowPermissionHelper;

    @SneakyThrows
    public String getLogContent(OdcTaskLogLevel level, Long flowInstanceId) {
        try {
            Optional<TaskEntity> taskEntityOptional =
                    flowTaskInstanceService.getLogDownloadableTaskEntity(flowInstanceId,
                            flowPermissionHelper.withProjectMemberCheck());
            return getLogContent(taskEntityOptional, level, flowInstanceId);
        } catch (Exception e) {
            log.warn("Task log file not found, flowInstanceId={}", flowInstanceId, e);
            return LogUtils.DEFAULT_LOG_CONTENT;
        }
    }

    @SneakyThrows
    public String getLogContentWithoutPermission(OdcTaskLogLevel level, Long flowInstanceId) {
        try {
            Optional<TaskEntity> taskEntityOptional =
                    flowTaskInstanceService.getLogDownloadableTaskEntity(flowInstanceId, null);
            return getLogContent(taskEntityOptional, level, flowInstanceId);
        } catch (Exception e) {
            log.warn("get log failed, task log file not found, flowInstanceId={}", flowInstanceId);
            return LogUtils.DEFAULT_LOG_CONTENT;
        }
    }

    @SneakyThrows
    public InputStreamResource downloadLogFile(Long flowInstanceId) {
        try {
            return new InputStreamResource(downloadLog(flowInstanceId));
        } catch (Exception e) {
            log.warn("download log failed, task log file not found, flowInstanceId={}", flowInstanceId);
            return new InputStreamResource(IoUtil.toUtf8Stream(LogUtils.DEFAULT_LOG_CONTENT));
        }
    }

    @SneakyThrows
    public String getLogContent(Optional<TaskEntity> taskEntityOptional, OdcTaskLogLevel level,
            Long flowInstanceId) {
        if (!taskEntityOptional.isPresent()) {
            log.warn("get log failed, flowInstanceId={}", flowInstanceId);
            return LogUtils.DEFAULT_LOG_CONTENT;
        }
        TaskEntity taskEntity = taskEntityOptional.get();
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
        File logFile = getLogFile(taskEntity.getCreatorId(), taskEntity.getId() + "",
                taskEntity.getTaskType(), level);
        return LogUtils.getLatestLogContent(logFile, loggerProperty.getMaxLines(), loggerProperty.getMaxSize());
    }

    @SneakyThrows
    private InputStream downloadLog(Long flowInstanceId) {
        Optional<TaskEntity> taskEntityOptional =
                flowTaskInstanceService.getLogDownloadableTaskEntity(flowInstanceId,
                        flowPermissionHelper.withProjectMemberCheck());
        TaskEntity taskEntity = taskEntityOptional
                .orElseThrow(() -> new NotFoundException(ErrorCodes.NotFound, new Object[] {flowInstanceId},
                        ErrorCodes.TaskLogNotFound.getLocalizedMessage(new Object[] {"Id", flowInstanceId})));
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
        File logFile = getLogFile(taskEntity.getCreatorId(), taskEntity.getId() + "",
                taskEntity.getTaskType(), OdcTaskLogLevel.ALL);
        return IoUtil.toStream(logFile);
    }

    private File getLogFile(Long userId, String flowTaskInstanceId, TaskType type, OdcTaskLogLevel logLevel) {
        String logFilePath = taskService.getLogFilePath(userId, flowTaskInstanceId, type, logLevel);
        File file = new File(logFilePath);
        log.info("get flow task log file, path={}，exist={}", logFilePath, file.exists());
        return file;
    }
}
