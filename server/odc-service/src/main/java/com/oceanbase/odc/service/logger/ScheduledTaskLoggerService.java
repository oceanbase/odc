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
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.dispatch.DispatchResponse;
import com.oceanbase.odc.service.dispatch.JobDispatchChecker;
import com.oceanbase.odc.service.dispatch.RequestDispatcher;
import com.oceanbase.odc.service.dispatch.TaskDispatchChecker;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.ScheduleTaskService;
import com.oceanbase.odc.service.task.caller.ExecutorIdentifier;
import com.oceanbase.odc.service.task.caller.ExecutorIdentifierParser;
import com.oceanbase.odc.service.task.config.TaskFrameworkEnabledProperties;
import com.oceanbase.odc.service.task.constants.JobAttributeKeyConstants;
import com.oceanbase.odc.service.task.constants.JobUrlConstants;
import com.oceanbase.odc.service.task.executor.logger.LogUtils;
import com.oceanbase.odc.service.task.model.ExecutorInfo;
import com.oceanbase.odc.service.task.model.OdcTaskLogLevel;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;
import com.oceanbase.odc.service.task.util.HttpUtil;
import com.oceanbase.odc.service.task.util.JobUtils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * @author mayang
 */
@Service
@Slf4j
public class ScheduledTaskLoggerService {

    private final ScheduleService scheduleService;
    private final ScheduleTaskService scheduleTaskService;
    private final TaskFrameworkEnabledProperties taskFrameworkEnabledProperties;
    private final RequestDispatcher requestDispatcher;
    private final TaskDispatchChecker dispatchChecker;
    private final TaskFrameworkService taskFrameworkService;
    private final CloudObjectStorageService cloudObjectStorageService;
    private final JobDispatchChecker jobDispatchChecker;

    @Value("${odc.log.maxLogLimitedCount: 10000}")
    private Long maxLogLimitedCount;

    // unit：B
    @Value("${odc.log.maxLogSizeCount: #{1024 * 1024}}")
    private Long maxLogSizeCount;

    public ScheduledTaskLoggerService(ScheduleService scheduleService,
            ScheduleTaskService scheduleTaskService,
            TaskFrameworkEnabledProperties taskFrameworkEnabledProperties,
            RequestDispatcher requestDispatcher,
            TaskDispatchChecker dispatchChecker,
            TaskFrameworkService taskFrameworkService,
            CloudObjectStorageService cloudObjectStorageService,
            JobDispatchChecker jobDispatchChecker) {
        this.scheduleService = scheduleService;
        this.scheduleTaskService = scheduleTaskService;
        this.taskFrameworkEnabledProperties = taskFrameworkEnabledProperties;
        this.requestDispatcher = requestDispatcher;
        this.dispatchChecker = dispatchChecker;
        this.taskFrameworkService = taskFrameworkService;
        this.cloudObjectStorageService = cloudObjectStorageService;
        this.jobDispatchChecker = jobDispatchChecker;
    }

    public String getLog(OdcTaskLogLevel level, Long jobId, boolean skipAuth) {
        if (!skipAuth) {
            scheduleService.nullSafeGetByIdWithCheckPermission(jobId);
        }
        return getLogWithoutPermission(level, jobId);
    }

    public File getLogFile(Long jobId, boolean skipAuth) {
        if (!skipAuth) {
            scheduleService.nullSafeGetByIdWithCheckPermission(jobId);
        }
        return downloadLogWithoutPermission(jobId);
    }

    @SneakyThrows
    public String getFullLogDownloadUrl(Long scheduleId, Long taskId, OdcTaskLogLevel level) {
        JobEntity jobEntity = taskFrameworkService.find(taskId);
        PreConditions.notNull(jobEntity, "job not found by id " + taskId);
        // it is currently considered that all scheduled tasks run on the task framework
        if (taskFrameworkEnabledProperties.isEnabled() && JobUtils.isK8sRunMode(jobEntity.getRunMode())
                && cloudObjectStorageService.supported()) {
            String attributeKey = OdcTaskLogLevel.ALL.equals(level) ? JobAttributeKeyConstants.LOG_STORAGE_ALL_OBJECT_ID
                    : JobAttributeKeyConstants.LOG_STORAGE_WARN_OBJECT_ID;
            Optional<String> objId = taskFrameworkService.findByJobIdAndAttributeKey(taskId, attributeKey);
            Optional<String> bucketName = taskFrameworkService.findByJobIdAndAttributeKey(taskId,
                    JobAttributeKeyConstants.LOG_STORAGE_BUCKET_NAME);
            if (objId.isPresent() && bucketName.isPresent()) {
                return cloudObjectStorageService.generateDownloadUrl(objId.get()).toString();
            }
        }
        return String.format("/api/v2/schedule/schedules/%s/tasks/%s/log/download", scheduleId, taskId);
    }

    @SneakyThrows
    private File getLogFileFromTaskFramework(Long scheduleTaskId, OdcTaskLogLevel level) {
        JobEntity jobEntity = taskFrameworkService.find(scheduleTaskId);
        PreConditions.notNull(jobEntity, "job not found by id " + scheduleTaskId);
        if (JobUtils.isK8sRunMode(jobEntity.getRunMode()) && cloudObjectStorageService.supported()) {
            String attributeKey = OdcTaskLogLevel.ALL.equals(level) ? JobAttributeKeyConstants.LOG_STORAGE_ALL_OBJECT_ID
                    : JobAttributeKeyConstants.LOG_STORAGE_WARN_OBJECT_ID;
            Optional<String> objId = taskFrameworkService.findByJobIdAndAttributeKey(scheduleTaskId, attributeKey);
            Optional<String> bucketName = taskFrameworkService.findByJobIdAndAttributeKey(scheduleTaskId,
                    JobAttributeKeyConstants.LOG_STORAGE_BUCKET_NAME);
            if (objId.isPresent() && bucketName.isPresent()) {
                if (log.isDebugEnabled()) {
                    log.debug("job: {} is finished, try to get log from local or oss.", jobEntity.getId());
                }

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
                String hostWithUrl = jobEntity.getExecutorEndpoint() + String.format(JobUrlConstants.LOG_QUERY,
                        jobEntity.getId()) + "?logType=" + level.getName();
                log.info("hostWithUrl: {}", hostWithUrl);
                try {
                    String tempLogFile = getClass().getClassLoader()
                            .getResource(String.format("tmp-task-%s.log", scheduleTaskId)).getPath();
                    SuccessResponse<String> response =
                            HttpUtil.request(hostWithUrl, new TypeReference<SuccessResponse<String>>() {});
                    if (response != null && !StrUtil.isNotEmpty(response.getData())) {
                        return FileUtil.writeUtf8String(response.getData(), tempLogFile);
                    } else {
                        log.error("the log from task pod is null, read error");
                        return null;
                    }
                } catch (IOException e) {
                    log.warn("Query log from executor occur error, executorEndpoint={}, jobId={}",
                            jobEntity.getExecutorEndpoint(), jobEntity.getId(), e);
                    throw new IOException(
                            ErrorCodes.TaskLogNotFound.getLocalizedMessage(new Object[] {"Id", jobEntity.getId()}));
                }
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

    private File getLogFileFromCurrentMachine(Long jobId, OdcTaskLogLevel level) {
        return scheduleTaskService.getScheduleTaskLogFile(jobId, level);
    }

    private File getLogFile(OdcTaskLogLevel level, Long jobId) {
        ScheduleTaskEntity taskEntity = scheduleTaskService.nullSafeGetById(jobId);
        if (taskFrameworkEnabledProperties.isEnabled() && taskEntity.getJobId() != null) {
            log.info("get schedule task log and task framework enable = True");
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
                log.info("Remote get task log succeed,jobId={}", jobId);
                return response.getContentByType(
                        new TypeReference<SuccessResponse<File>>() {}).getData();
            } catch (Exception e) {
                log.warn("Remote get task log failed, jobId={}", jobId, e);
                throw new UnexpectedException(String.format("Remote interrupt task failed, jobId=%s", jobId));
            }
        }
        return getLogFileFromCurrentMachine(jobId, level);
    }

    private String getLogWithoutPermission(OdcTaskLogLevel level, Long jobId) {
        return LogToolUnit.readLog(getLogFile(level, jobId), maxLogLimitedCount, maxLogSizeCount);
    }

    private File downloadLogWithoutPermission(Long jobId) {
        return getLogFile(OdcTaskLogLevel.ALL, jobId);
    }
}
