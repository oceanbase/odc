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
import java.io.IOException;
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
    public String getLog(Long id, String logType) {
        log.info("Accept log request, task id = {}, logType = {}", id, logType);
        OdcTaskLogLevel logTypeLevel = null;
        try {
            logTypeLevel = OdcTaskLogLevel.valueOf(logType);
        } catch (Exception e) {
            log.warn("logType {} is illegal.", logType);
            new SuccessResponse<>("logType " + logType + " is illegal.");
        }

        String logFileStr = LogUtils.getJobLogFileWithPath(id, logTypeLevel);
        return LogUtils.getLogContent(logFileStr);
    }


    @Override
    public Map<String, String> uploadLogFileToCloudStorage(JobIdentity ji,
            CloudObjectStorageService cloudObjectStorageService) throws IOException {
        log.info("Task id: {}, upload log", ji.getId());
        String logFileStr = LogUtils.getJobLogFileWithPath(ji.getId(), OdcTaskLogLevel.ALL);
        String fileId = StringUtils.uuid();
        File jobLogFile = new File(logFileStr);
        if (!jobLogFile.exists()) {
            return null;
        }
        String objectName = cloudObjectStorageService.uploadTemp(fileId, jobLogFile);
        Map<String, String> logMap = new HashMap<>();
        logMap.put(JobAttributeKeyConstants.LOG_STORAGE_ALL_OBJECT_ID, objectName);
        logMap.put(JobAttributeKeyConstants.LOG_STORAGE_WARN_OBJECT_ID, objectName);
        logMap.put(JobAttributeKeyConstants.LOG_STORAGE_BUCKET_NAME,
                cloudObjectStorageService.getBucketName());
        log.info("upload task log to OSS successfully, file name={}", fileId);
        return logMap;

    }

}
