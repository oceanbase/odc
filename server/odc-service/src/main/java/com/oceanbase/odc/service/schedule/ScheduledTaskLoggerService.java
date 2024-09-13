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
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
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
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * @author mayang
 */
@Service
@Slf4j
public class ScheduledTaskLoggerService {

    private static final String LOG_PATH_PATTERN = "%s/scheduleTask/%s-%s/%s/log.%s";
    private static final String DOWNLOAD_LOG_URL_PATTERN = "/api/v2/schedule/schedules/%s/tasks/%s/log/download";

    private final ScheduleTaskService scheduleTaskService;
    private final TaskFrameworkEnabledProperties taskFrameworkEnabledProperties;
    private final RequestDispatcher requestDispatcher;
    private final TaskDispatchChecker dispatchChecker;
    private final TaskFrameworkService taskFrameworkService;
    private final JobDispatchChecker jobDispatchChecker;
    private final TaskExecutorClient taskExecutorClient;

    private final ScheduleLogProperties loggerProperty;
    private final CloudObjectStorageService cloudObjectStorageService;

    public ScheduledTaskLoggerService(ScheduleTaskService scheduleTaskService,
            TaskFrameworkEnabledProperties taskFrameworkEnabledProperties,
            RequestDispatcher requestDispatcher,
            TaskDispatchChecker dispatchChecker,
            TaskFrameworkService taskFrameworkService,
            JobDispatchChecker jobDispatchChecker,
            TaskExecutorClient taskExecutorClient,
            ScheduleLogProperties loggerProperty,
            CloudObjectStorageService cloudObjectStorageService) {
        this.scheduleTaskService = scheduleTaskService;
        this.taskFrameworkEnabledProperties = taskFrameworkEnabledProperties;
        this.requestDispatcher = requestDispatcher;
        this.dispatchChecker = dispatchChecker;
        this.taskFrameworkService = taskFrameworkService;
        this.jobDispatchChecker = jobDispatchChecker;
        this.taskExecutorClient = taskExecutorClient;
        this.loggerProperty = loggerProperty;
        this.cloudObjectStorageService = cloudObjectStorageService;
    }

    public String getLogContent(Long scheduleTaskId, OdcTaskLogLevel level) {
        try {
            return LogUtils.getLatestLogContent(getLogFile(scheduleTaskId, level), loggerProperty.getMaxLines(),
                    loggerProperty.getMaxSize());
        } catch (Exception e) {
            log.warn("get log failed, scheduleTaskId={}", scheduleTaskId);
            return LogUtils.DEFAULT_LOG_CONTENT;
        }
    }

    public File downloadLog(Long scheduleTaskId, OdcTaskLogLevel level) {
        return getLogFile(scheduleTaskId, level);
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
    private File getLogFileFromTaskFramework(Long jobId, OdcTaskLogLevel level) {
        JobEntity jobEntity = taskFrameworkService.find(jobId);
        PreConditions.notNull(jobEntity, "job not found by id " + jobId);
        if (JobUtils.isK8sRunMode(jobEntity.getRunMode())) {
            if (!cloudObjectStorageService.supported()) {
                throw new RuntimeException("CloudObjectStorageService is not supported.");
            }

            String tempFilePath =
                    FileUtil.normalize(loggerProperty.getTempLogDir() + File.separator
                            + String.format("tmp-task-%s.log", jobId));
            String attributeKey = OdcTaskLogLevel.ALL.equals(level) ? JobAttributeKeyConstants.LOG_STORAGE_ALL_OBJECT_ID
                    : JobAttributeKeyConstants.LOG_STORAGE_WARN_OBJECT_ID;
            Optional<String> objId = taskFrameworkService.findByJobIdAndAttributeKey(jobId, attributeKey);
            Optional<String> bucketName = taskFrameworkService.findByJobIdAndAttributeKey(jobId,
                    JobAttributeKeyConstants.LOG_STORAGE_BUCKET_NAME);
            if (objId.isPresent() && bucketName.isPresent()) {
                if (log.isDebugEnabled()) {
                    log.debug("job: {} is finished, try to get log from local or oss.", jobEntity.getId());
                }
                FileUtil.del(tempFilePath);
                File localFile = new File(LogUtils.getTaskLogFileWithPath(jobEntity.getId(), level));
                if (localFile.exists()) {
                    return localFile;
                }

                File tempFile = cloudObjectStorageService.downloadToTempFile(objId.get());
                try (FileInputStream inputStream = new FileInputStream(tempFile)) {
                    FileUtils.copyInputStreamToFile(inputStream, localFile);
                } finally {
                    FileUtils.deleteQuietly(tempFile);
                }
                return localFile;
            }
            if (jobEntity.getExecutorDestroyedTime() == null && jobEntity.getExecutorEndpoint() != null) {
                if (log.isDebugEnabled()) {
                    log.debug("job: {} is not finished, try to get log from remote pod.", jobEntity.getId());
                }
                String logContent = taskExecutorClient.getLogContent(jobEntity.getExecutorEndpoint(), jobId, level);
                return FileUtil.writeUtf8String(logContent, tempFilePath);
            }
        }

        if (!jobDispatchChecker.isExecutorOnThisMachine(jobEntity)) {
            log.info("job: {} is not current machine, try to forward.", jobEntity.getId());
            ExecutorIdentifier ei = ExecutorIdentifierParser.parser(jobEntity.getExecutorIdentifier());
            try {
                DispatchResponse response = requestDispatcher.forward(ei.getHost(), ei.getPort());
                return response.getContentByType(new TypeReference<SuccessResponse<File>>() {}).getData();
            } catch (Exception ex) {
                log.warn("Forward to remote odc occur error, jobId={}, executorIdentifier={}",
                        jobEntity.getId(), jobEntity.getExecutorIdentifier(), ex);
            }
        }
        return new File(LogUtils.getTaskLogFileWithPath(jobEntity.getId(), level));
    }

    private File getLogFileFromCurrentMachine(ScheduleTaskEntity scheduleTask, OdcTaskLogLevel level) {
        String filePath = String.format(LOG_PATH_PATTERN, loggerProperty.getDirectory(),
                scheduleTask.getJobName(), scheduleTask.getJobGroup(), scheduleTask.getId(),
                level.name().toLowerCase());
        return new File(filePath);
    }

    private File getLogFile(Long scheduleTaskId, OdcTaskLogLevel level) {
        ScheduleTaskEntity taskEntity = scheduleTaskService.nullSafeGetById(scheduleTaskId);
        if (taskFrameworkEnabledProperties.isEnabled() && taskEntity.getJobId() != null) {
            try {
                return getLogFileFromTaskFramework(taskEntity.getJobId(), level);
            } catch (Exception e) {
                log.warn("Copy input stream to file failed.", e);
                throw new UnexpectedException("Copy input stream to file failed.");
            }
        }
        ExecutorInfo executorInfo = JsonUtils.fromJson(taskEntity.getExecutor(), ExecutorInfo.class);
        if (!dispatchChecker.isThisMachine(executorInfo)) {
            try {
                DispatchResponse response =
                        requestDispatcher.forward(executorInfo.getHost(), executorInfo.getPort());
                return response.getContentByType(
                        new TypeReference<SuccessResponse<File>>() {}).getData();
            } catch (Exception e) {
                log.warn("Remote get task log failed, jobId={}", scheduleTaskId, e);
                throw new UnexpectedException(String.format("Remote interrupt task failed, jobId=%s", scheduleTaskId));
            }
        }
        return getLogFileFromCurrentMachine(taskEntity, level);
    }
}
