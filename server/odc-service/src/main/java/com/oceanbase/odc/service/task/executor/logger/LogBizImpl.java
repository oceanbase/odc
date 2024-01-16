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

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.service.common.response.SuccessResponse;
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
    public SuccessResponse<String> getLog(Long id, String logType) {
        log.info("Accept log request, task id = {}, logType = {}", id, logType);
        OdcTaskLogLevel logTypeLevel = null;
        try {
            logTypeLevel = OdcTaskLogLevel.valueOf(logType);
        } catch (Exception e) {
            log.warn("logType {} is illegal.", logType);
            new SuccessResponse<>("logType " + logType + " is illegal.");
        }

        String logFile = LogUtils.getJobLogFileWithPath(id, logTypeLevel);
        return new SuccessResponse<>(LogUtils.getLogContent(logFile));
    }


    @Override
    public Map<String, String> uploadLogFileToCloudStorage(JobIdentity ji,
            CloudObjectStorageService cloudObjectStorageService) {
        log.info("Task id: {}, upload log", ji.getId());
        String jobLog = LogUtils.getJobLogFileWithPath(ji.getId(), OdcTaskLogLevel.ALL);
        String fileId = StringUtils.uuid();
        File jobLogFile = new File(jobLog);
        if (!jobLogFile.exists()) {
            return null;
        }
        try {
            String objectName = cloudObjectStorageService.uploadTemp(fileId, jobLogFile);
            Map<String, String> logMap = new HashMap<>();
            logMap.put(JobAttributeKeyConstants.STORAGE_LOG_ALL_OBJECT_ID, objectName);
            logMap.put(JobAttributeKeyConstants.STORAGE_LOG_WARN_OBJECT_ID, objectName);
            logMap.put(JobAttributeKeyConstants.STORAGE_BUCKET_NAME,
                    cloudObjectStorageService.getBucketName());
            log.info("upload task log to OSS successfully, file name={}", fileId);
            return logMap;
        } catch (Exception exception) {
            log.warn("upload task log to OSS failed, file name={}", fileId);
            return null;
        }

    }

}
