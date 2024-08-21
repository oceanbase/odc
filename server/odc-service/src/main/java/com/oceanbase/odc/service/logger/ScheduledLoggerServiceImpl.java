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
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.unit.BinarySizeUnit;
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

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * @author mayang
 */
@Service("scheduledLoggerService")
@Slf4j
public class ScheduledLoggerServiceImpl extends AbstractLoggerService implements ILoggerService {

    private final ScheduleService scheduleService;
    private final ScheduleTaskService scheduleTaskService;
    private final TaskFrameworkEnabledProperties taskFrameworkEnabledProperties;
    private final RequestDispatcher requestDispatcher;
    private final TaskDispatchChecker dispatchChecker;
    private final TaskFrameworkService taskFrameworkService;
    private final CloudObjectStorageService cloudObjectStorageService;
    private final JobDispatchChecker jobDispatchChecker;

    @Value("${odc.log.schedule.maxLogLimitedCount: 10000}")
    private Long maxLogLimitedCount;

    // unit：B
    @Value("${odc.log.schedule.maxLogSizeCount: #{1024 * 1024}}")
    private Long maxLogSizeCount;

    public ScheduledLoggerServiceImpl(ScheduleService scheduleService,
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

    @Override
    public String getLog(OdcTaskLogLevel level, Long jobId, boolean skipAuth) {
        if (skipAuth) {
            scheduleService.nullSafeGetByIdWithCheckPermission(jobId);
        }
        return getLogWithoutPermission(level, jobId);
    }

    @Override
    public File downloadLog(Long jobId, boolean skipAuth) {
        if (skipAuth) {
            scheduleService.nullSafeGetByIdWithCheckPermission(jobId);
        }
        return downloadLogWithoutPermission(jobId);
    }

    @SneakyThrows
    private File getLogFileFromTaskFramework(Long jobId, OdcTaskLogLevel level) {
        JobEntity jobEntity = taskFrameworkService.find(jobId);
        PreConditions.notNull(jobEntity, "job not found by id " + jobId);

        if (JobUtils.isK8sRunMode(jobEntity.getRunMode()) && cloudObjectStorageService.supported()) {
            Optional<String> objId =
                    taskFrameworkService.findByJobIdAndAttributeKey(jobEntity.getId(), level.getName());
            Optional<String> bucketName = taskFrameworkService.findByJobIdAndAttributeKey(jobEntity.getId(),
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

                String jobUrlPattern = JobUrlConstants.LOG_DOWNLOAD;
                String hostWithUrl = jobEntity.getExecutorEndpoint() + String.format(jobUrlPattern, jobEntity.getId())
                        + "?logType=" + level.getName();
                try {
                    SuccessResponse<File> response =
                            HttpUtil.request(hostWithUrl, new TypeReference<SuccessResponse<File>>() {});
                    return response.getData();
                } catch (IOException e) {
                    log.warn("Query log from executor occur error, executorEndpoint={}, jobId={}",
                            jobEntity.getExecutorEndpoint(), jobEntity.getId(), e);
                    throw new IOException(
                            ErrorCodes.TaskLogNotFound.getLocalizedMessage(new Object[] {"Id", jobEntity.getId()}));
                }
            }
        }

        if (!jobDispatchChecker.isExecutorOnThisMachine(jobEntity)) {
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
        return readLog(getLogFile(level, jobId), maxLogLimitedCount, maxLogSizeCount);
    }

    private File downloadLogWithoutPermission(Long jobId) {
        return getLogFile(OdcTaskLogLevel.ALL, jobId);
    }
}
