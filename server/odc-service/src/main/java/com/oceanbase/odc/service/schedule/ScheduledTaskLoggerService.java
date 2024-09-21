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
package com.oceanbase.odc.service.schedule;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.dispatch.DispatchResponse;
import com.oceanbase.odc.service.dispatch.JobDispatchChecker;
import com.oceanbase.odc.service.dispatch.RequestDispatcher;
import com.oceanbase.odc.service.dispatch.TaskDispatchChecker;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.task.caller.ExecutorIdentifier;
import com.oceanbase.odc.service.task.caller.ExecutorIdentifierParser;
import com.oceanbase.odc.service.task.config.TaskFrameworkEnabledProperties;
import com.oceanbase.odc.service.task.constants.JobAttributeKeyConstants;
import com.oceanbase.odc.service.task.executor.logger.LogUtils;
import com.oceanbase.odc.service.task.model.ExecutorInfo;
import com.oceanbase.odc.service.task.model.OdcTaskLogLevel;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;
import com.oceanbase.odc.service.task.util.JobUtils;
import com.oceanbase.odc.service.task.util.TaskExecutorClient;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * @author mayang
 */
@Service
@Slf4j
@SkipAuthorize("odc internal usage")
public class ScheduledTaskLoggerService {

    private static final String LOG_PATH_PATTERN = "%s/scheduleTask/%s-%s/%s/log.%s";
    private static final String DOWNLOAD_LOG_URL_PATTERN = "/api/v2/schedule/schedules/%s/tasks/%s/log/download";

    @Autowired
    private ScheduleTaskService scheduleTaskService;

    @Autowired
    private TaskFrameworkEnabledProperties taskFrameworkEnabledProperties;

    @Autowired
    private RequestDispatcher requestDispatcher;

    @Autowired
    private TaskDispatchChecker dispatchChecker;

    @Autowired
    private TaskFrameworkService taskFrameworkService;

    @Autowired
    private JobDispatchChecker jobDispatchChecker;

    @Autowired
    private TaskExecutorClient taskExecutorClient;

    @Autowired
    private ScheduleLogProperties loggerProperty;

    @Autowired
    private CloudObjectStorageService cloudObjectStorageService;

    public String getLogContent(Long scheduleTaskId, OdcTaskLogLevel level) {
        try {
            return getLog(scheduleTaskId, level);
        } catch (Exception e) {
            log.warn("get log failed, scheduleTaskId={}", scheduleTaskId);
            return LogUtils.DEFAULT_LOG_CONTENT;
        }
    }

    public InputStreamResource downloadLog(Long scheduleTaskId, OdcTaskLogLevel level) {
        try {
            return new InputStreamResource(downloadLogFile(scheduleTaskId, level));
        } catch (Exception e) {
            log.warn("download log failed, scheduleTaskId={}", scheduleTaskId);
            return new InputStreamResource(IoUtil.toUtf8Stream(LogUtils.DEFAULT_LOG_CONTENT));
        }
    }

    @SneakyThrows
    public String getFullLogDownloadUrl(Long scheduleId, Long scheduleTaskId, OdcTaskLogLevel level) {
        if (ObjectUtil.isNull(cloudObjectStorageService)) {
            return String.format(DOWNLOAD_LOG_URL_PATTERN, scheduleId, scheduleTaskId);
        }
        if (cloudObjectStorageService.supported()) {
            ScheduleTaskEntity taskEntity = scheduleTaskService.nullSafeGetById(scheduleTaskId);
            Long jobId = taskEntity.getJobId();
            if (jobId == null) {
                log.warn("job is not exist, may a historical schedule task, scheduleTaskId:{}", scheduleTaskId);
                return StrUtil.EMPTY;
            }
            JobEntity jobEntity = taskFrameworkService.find(jobId);
            PreConditions.notNull(jobEntity, "job not found by id " + jobId);
            if (JobUtils.isK8sRunMode(jobEntity.getRunMode())) {
                String attributeKey =
                        OdcTaskLogLevel.ALL.equals(level) ? JobAttributeKeyConstants.LOG_STORAGE_ALL_OBJECT_ID
                                : JobAttributeKeyConstants.LOG_STORAGE_WARN_OBJECT_ID;
                Optional<String> objId = taskFrameworkService.findByJobIdAndAttributeKey(jobId, attributeKey);
                Optional<String> bucketName = taskFrameworkService.findByJobIdAndAttributeKey(jobId,
                        JobAttributeKeyConstants.LOG_STORAGE_BUCKET_NAME);
                if (objId.isPresent() && bucketName.isPresent()) {
                    return cloudObjectStorageService.generateDownloadUrl(objId.get()).toString();
                }
            }
            return StrUtil.EMPTY;
        }
        return String.format(DOWNLOAD_LOG_URL_PATTERN, scheduleId, scheduleTaskId);
    }

    @SneakyThrows
    private void consumeLogFromTaskFramework(Long jobId, OdcTaskLogLevel level,
            Consumer<File> logFileConsumer,
            Consumer<String> logContentConsumer,
            Consumer<ExecutorIdentifier> jobDispatcherConsumer) {
        JobEntity jobEntity = taskFrameworkService.find(jobId);
        PreConditions.notNull(jobEntity, "job not found by id " + jobId);
        if (JobUtils.isK8sRunMode(jobEntity.getRunMode())) {
            if (!cloudObjectStorageService.supported()) {
                throw new RuntimeException("CloudObjectStorageService is not supported.");
            }

            String attributeKey = OdcTaskLogLevel.ALL.equals(level) ? JobAttributeKeyConstants.LOG_STORAGE_ALL_OBJECT_ID
                    : JobAttributeKeyConstants.LOG_STORAGE_WARN_OBJECT_ID;
            Optional<String> objId = taskFrameworkService.findByJobIdAndAttributeKey(jobId, attributeKey);
            Optional<String> bucketName = taskFrameworkService.findByJobIdAndAttributeKey(jobId,
                    JobAttributeKeyConstants.LOG_STORAGE_BUCKET_NAME);
            String logFilePath = LogUtils.getTaskLogFileWithPath(jobEntity.getId(), level);
            if (objId.isPresent() && bucketName.isPresent()) {
                if (log.isDebugEnabled()) {
                    log.debug("job: {} is finished, try to get log from local or oss.", jobEntity.getId());
                }
                File localFile = new File(logFilePath);
                if (localFile.exists()) {
                    logFileConsumer.accept(localFile);
                }

                File tempFile = cloudObjectStorageService.downloadToTempFile(objId.get());
                try (FileInputStream inputStream = new FileInputStream(tempFile)) {
                    FileUtils.copyInputStreamToFile(inputStream, localFile);
                } finally {
                    FileUtils.deleteQuietly(tempFile);
                }
                logFileConsumer.accept(localFile);
            }
            if (jobEntity.getExecutorDestroyedTime() == null && jobEntity.getExecutorEndpoint() != null) {
                if (log.isDebugEnabled()) {
                    log.debug("job: {} is not finished, try to get log from remote pod.", jobEntity.getId());
                }
                String logContent = taskExecutorClient.getLogContent(jobEntity.getExecutorEndpoint(), jobId, level);
                // ensure that the logs obtained when the final task is completed are up-to-date
                FileUtil.del(new File(logFilePath));
                logContentConsumer.accept(logContent);
            }
        }

        if (!jobDispatchChecker.isExecutorOnThisMachine(jobEntity)) {
            log.info("job: {} is not current machine, try to forward.", jobEntity.getId());
            ExecutorIdentifier ei = ExecutorIdentifierParser.parser(jobEntity.getExecutorIdentifier());
            try {
                jobDispatcherConsumer.accept(ei);
            } catch (Exception ex) {
                log.warn("Forward to remote odc occur error, jobId={}, executorIdentifier={}",
                        jobEntity.getId(), jobEntity.getExecutorIdentifier(), ex);
            }
        }
        File file = new File(LogUtils.getTaskLogFileWithPath(jobEntity.getId(), level));
        logFileConsumer.accept(file);
    }

    private File getLogFileFromCurrentMachine(ScheduleTaskEntity scheduleTask, OdcTaskLogLevel level) {
        String filePath = String.format(LOG_PATH_PATTERN, loggerProperty.getDirectory(),
                scheduleTask.getJobName(), scheduleTask.getJobGroup(), scheduleTask.getId(),
                level.name().toLowerCase());
        return new File(filePath);
    }

    private String getLog(Long scheduleTaskId, OdcTaskLogLevel level) {
        ScheduleTaskEntity taskEntity = scheduleTaskService.nullSafeGetById(scheduleTaskId);
        if (taskFrameworkEnabledProperties.isEnabled() && taskEntity.getJobId() != null) {
            try {
                final String[] logContents = new String[1];
                consumeLogFromTaskFramework(taskEntity.getJobId(), level,
                        logFile -> logContents[0] = LogUtils.getLatestLogContent(logFile, loggerProperty.getMaxLines(),
                                loggerProperty.getMaxSize()),
                        logContent -> logContents[0] = logContent,
                        executorIdentifier -> {
                            try {
                                logContents[0] = forwardToGetLogContent(executorIdentifier.getHost(),
                                        executorIdentifier.getPort());
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                return logContents[0];
            } catch (Exception e) {
                log.warn("Copy input stream to file failed.", e);
                throw new UnexpectedException("Copy input stream to file failed.");
            }
        }
        ExecutorInfo executorInfo = JsonUtils.fromJson(taskEntity.getExecutor(), ExecutorInfo.class);
        if (!dispatchChecker.isThisMachine(executorInfo)) {
            try {
                return forwardToGetLogContent(executorInfo.getHost(), executorInfo.getPort());
            } catch (Exception e) {
                log.warn("Remote get task log failed, jobId={}", scheduleTaskId, e);
                throw new UnexpectedException(String.format("Remote interrupt task failed, jobId=%s", scheduleTaskId));
            }
        }
        File logFile = getLogFileFromCurrentMachine(taskEntity, level);
        return LogUtils.getLatestLogContent(logFile, loggerProperty.getMaxLines(), loggerProperty.getMaxSize());
    }

    private InputStream downloadLogFile(Long scheduleTaskId, OdcTaskLogLevel level) {
        ScheduleTaskEntity taskEntity = scheduleTaskService.nullSafeGetById(scheduleTaskId);
        if (taskFrameworkEnabledProperties.isEnabled() && taskEntity.getJobId() != null) {
            try {
                final InputStream[] logStreams = new InputStream[1];
                consumeLogFromTaskFramework(taskEntity.getJobId(), level,
                        logFile -> logStreams[0] = IoUtil.toStream(logFile),
                        logContent -> logStreams[0] = IoUtil.toUtf8Stream(logContent),
                        executorIdentifier -> {
                            try {
                                logStreams[0] = forwardToGetLogStream(executorIdentifier.getHost(),
                                        executorIdentifier.getPort());
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                return logStreams[0];
            } catch (Exception e) {
                log.warn("Copy input stream to file failed.", e);
                throw new UnexpectedException("Copy input stream to file failed.");
            }
        }
        ExecutorInfo executorInfo = JsonUtils.fromJson(taskEntity.getExecutor(), ExecutorInfo.class);
        if (!dispatchChecker.isThisMachine(executorInfo)) {
            try {
                return forwardToGetLogStream(executorInfo.getHost(), executorInfo.getPort());
            } catch (Exception e) {
                log.warn("Remote download task log failed, jobId={}", scheduleTaskId, e);
                throw new UnexpectedException(String.format("Remote interrupt task failed, jobId=%s", scheduleTaskId));
            }
        }
        return IoUtil.toStream(getLogFileFromCurrentMachine(taskEntity, level));
    }

    private String forwardToGetLogContent(@NonNull String host, @NonNull Integer port) throws Exception {
        DispatchResponse response = requestDispatcher.forward(host, port);
        return response.getContentByType(new TypeReference<SuccessResponse<String>>() {}).getData();
    }

    private InputStream forwardToGetLogStream(@NonNull String host, @NonNull Integer port) throws Exception {
        return requestDispatcher.forwardGetResource(host, port)
                .getBody().getInputStream();
    }
}
