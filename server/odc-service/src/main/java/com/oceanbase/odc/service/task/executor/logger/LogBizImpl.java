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
package com.oceanbase.odc.service.task.executor.logger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.task.constants.JobAttributeKeyConstants;
import com.oceanbase.odc.service.task.model.OdcTaskLogLevel;
import com.oceanbase.odc.service.task.schedule.JobIdentity;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-12-13
 * @since 4.2.4
 */
@Slf4j
public class LogBizImpl implements LogBiz {

    @Override
    public String getLog(Long jobId, String logType, Long fetchMaxLine, Long fetchMaxByteSize) {
        log.info("Accept log request, job id = {}, logType = {}.", jobId, logType);
        OdcTaskLogLevel logTypeLevel = null;
        try {
            logTypeLevel = OdcTaskLogLevel.valueOf(logType);
        } catch (IllegalArgumentException e) {
            log.warn("logType {} is illegal.", logType);
            return "logType " + logType + " is illegal.";
        }

        String logFileStr = LogUtils.getTaskLogFileWithPath(jobId, logTypeLevel);
        return LogUtils.getLatestLogContent(logFileStr, fetchMaxLine, fetchMaxByteSize);
    }

    @Override
    public Map<String, String> uploadLogFileToCloudStorage(JobIdentity ji,
            CloudObjectStorageService storageService) {
        log.info("upload log files to cloud storage starting, jobId={}", ji.getId());

        Map<String, String> logMap = new HashMap<>();
        Optional<String> allLogObjectId = uploadTempFile(ji.getId(), OdcTaskLogLevel.ALL, storageService);
        allLogObjectId.ifPresent(a -> logMap.put(JobAttributeKeyConstants.LOG_STORAGE_ALL_OBJECT_ID, a));

        Optional<String> warnLogObjectId = uploadTempFile(ji.getId(), OdcTaskLogLevel.WARN, storageService);
        warnLogObjectId.ifPresent(a -> logMap.put(JobAttributeKeyConstants.LOG_STORAGE_WARN_OBJECT_ID, a));

        if (allLogObjectId.isPresent() || warnLogObjectId.isPresent()) {
            logMap.put(JobAttributeKeyConstants.LOG_STORAGE_BUCKET_NAME, storageService.getBucketName());
        } else {
            logMap.put(JobAttributeKeyConstants.LOG_STORAGE_FAILED_REASON, "No log file to upload.");
        }
        log.info("upload log files to cloud storage completed, jobId={}, logs={}", ji.getId(), logMap);
        return logMap;
    }

    /**
     * TODO: should not upload as temp file
     */
    private Optional<String> uploadTempFile(Long jobId, OdcTaskLogLevel logType,
            CloudObjectStorageService storageService) {
        String logFileStr = LogUtils.getTaskLogFileWithPath(jobId, logType);
        String fileId = StringUtils.uuid();
        File jobLogFile = new File(logFileStr);
        if (jobLogFile.exists() && jobLogFile.length() > 0) {
            try {
                String ossName = storageService.upload(fileId, jobLogFile);
                log.info("upload job {} log to OSS successfully, file name={}, oss object name {}.",
                        logType.getName(), fileId, ossName);
                return Optional.of(ossName);
            } catch (Exception e) {
                log.error("upload job {} log to OSS failed, file name={}, error {}.",
                        logType.getName(), fileId, e.getMessage());
                return Optional.empty();
            }
        }
        return Optional.empty();

    }

}
