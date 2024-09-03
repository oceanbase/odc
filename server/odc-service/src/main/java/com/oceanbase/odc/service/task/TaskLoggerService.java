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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.dispatch.DispatchResponse;
import com.oceanbase.odc.service.dispatch.JobDispatchChecker;
import com.oceanbase.odc.service.dispatch.RequestDispatcher;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration;
import com.oceanbase.odc.service.task.caller.ExecutorIdentifier;
import com.oceanbase.odc.service.task.caller.ExecutorIdentifierParser;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.constants.JobAttributeKeyConstants;
import com.oceanbase.odc.service.task.executor.logger.LogUtils;
import com.oceanbase.odc.service.task.model.OdcTaskLogLevel;
import com.oceanbase.odc.service.task.schedule.DefaultJobContextBuilder;
import com.oceanbase.odc.service.task.schedule.JobCredentialProvider;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;
import com.oceanbase.odc.service.task.util.CloudObjectStorageServiceBuilder;
import com.oceanbase.odc.service.task.util.JobUtils;
import com.oceanbase.odc.service.task.util.TaskExecutorClient;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2024-02-05
 * @since 4.2.4
 */
@Slf4j
@Service
public class TaskLoggerService {
    @Autowired
    private TaskFrameworkProperties taskFrameworkProperties;

    @Autowired
    private TaskFrameworkService taskFrameworkService;

    @Autowired
    private JobDispatchChecker jobDispatchChecker;

    @Autowired
    private RequestDispatcher requestDispatcher;

    @Autowired
    private TaskExecutorClient taskExecutorClient;

    @Autowired
    private JobCredentialProvider jobCredentialProvider;

    private final Map<ObjectStorageConfiguration, CloudObjectStorageService> cloudObjectStorageServiceMap =
            new ConcurrentHashMap<>();

    @SkipAuthorize("odc internal usage")
    public String getLogByTaskFramework(OdcTaskLogLevel level, Long jobId) throws IOException {
        // forward to target host when task is not be executed on this machine or running in k8s pod
        JobEntity jobEntity = taskFrameworkService.find(jobId);
        PreConditions.notNull(jobEntity, "job not found by id " + jobId);

        if (JobUtils.isK8sRunMode(jobEntity.getRunMode())) {
            CloudObjectStorageService cloudObjectStorageService = getCloudObjectStorageService(jobEntity);
            if (!cloudObjectStorageService.supported()) {
                throw new RuntimeException("CloudObjectStorageService is not supported.");
            }

            String logIdKey = level == OdcTaskLogLevel.ALL ? JobAttributeKeyConstants.LOG_STORAGE_ALL_OBJECT_ID
                    : JobAttributeKeyConstants.LOG_STORAGE_WARN_OBJECT_ID;
            Optional<String> objId = taskFrameworkService.findByJobIdAndAttributeKey(jobEntity.getId(), logIdKey);
            Optional<String> bucketName = taskFrameworkService.findByJobIdAndAttributeKey(jobEntity.getId(),
                    JobAttributeKeyConstants.LOG_STORAGE_BUCKET_NAME);

            if (objId.isPresent() && bucketName.isPresent()) {
                if (log.isDebugEnabled()) {
                    log.debug("job: {} is finished, try to get log from local or oss.", jobEntity.getId());
                }
                // check log file is exist on current disk
                String logFileStr = LogUtils.getTaskLogFileWithPath(jobEntity.getId(), level);
                if (new File(logFileStr).exists()) {
                    return LogUtils.getLatestLogContent(logFileStr, LogUtils.MAX_LOG_LINE_COUNT,
                            LogUtils.MAX_LOG_BYTE_COUNT);
                }

                File tempFile = cloudObjectStorageService.downloadToTempFile(objId.get());
                try (FileInputStream inputStream = new FileInputStream(tempFile)) {
                    FileUtils.copyInputStreamToFile(inputStream, new File(logFileStr));
                } finally {
                    FileUtils.deleteQuietly(tempFile);
                }
                return LogUtils.getLatestLogContent(logFileStr, LogUtils.MAX_LOG_LINE_COUNT,
                        LogUtils.MAX_LOG_BYTE_COUNT);

            }
            if (jobEntity.getExecutorDestroyedTime() == null && jobEntity.getExecutorEndpoint() != null) {
                if (log.isDebugEnabled()) {
                    log.debug("job: {} is not finished, try to get log from remote pod.", jobEntity.getId());
                }
                return taskExecutorClient.getLogContent(jobEntity.getExecutorEndpoint(), jobEntity.getId(), level);
            }
        } else {
            // process mode when executor is not current host, forward to target
            if (!jobDispatchChecker.isExecutorOnThisMachine(jobEntity)) {
                ExecutorIdentifier ei = ExecutorIdentifierParser.parser(jobEntity.getExecutorIdentifier());
                try {
                    DispatchResponse response = requestDispatcher.forward(ei.getHost(), ei.getPort());
                    return response.getContentByType(new TypeReference<SuccessResponse<String>>() {}).getData();
                } catch (Exception ex) {
                    // Try to get log from localhost if forward to remote failed
                    log.warn("Forward to remote odc occur error, jobId={}, executorIdentifier={}",
                            jobEntity.getId(), jobEntity.getExecutorIdentifier(), ex);
                }
            }

            String logFileStr = LogUtils.getTaskLogFileWithPath(jobEntity.getId(), level);
            return LogUtils.getLatestLogContent(logFileStr, LogUtils.MAX_LOG_LINE_COUNT, LogUtils.MAX_LOG_BYTE_COUNT);
        }
        return ErrorCodes.TaskLogNotFound.getLocalizedMessage(new Object[] {"Id", jobEntity.getId()});
    }

    private CloudObjectStorageService getCloudObjectStorageService(JobEntity jobEntity) {
        JobContext jobContext = new DefaultJobContextBuilder().build(jobEntity);
        ObjectStorageConfiguration configuration = jobCredentialProvider.getCloudObjectStorageCredential(jobContext);
        return cloudObjectStorageServiceMap.computeIfAbsent(configuration,
                k -> CloudObjectStorageServiceBuilder.build(configuration));
    }

}
