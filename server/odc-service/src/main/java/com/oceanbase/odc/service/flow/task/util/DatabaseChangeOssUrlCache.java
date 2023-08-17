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
package com.oceanbase.odc.service.flow.task.util;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeResult;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.objectstorage.cloud.model.OssConfiguration;
import com.oceanbase.odc.service.task.TaskService;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2021/10/20 下午2:07
 * @Description: [This class is responsible for maintaining relationships from async task id to its
 *               result set temp OSS download url.]
 */
@Service
@Slf4j
public class DatabaseChangeOssUrlCache {
    /**
     * 异步任务 ID 到 OSS 下载链接的映射关系
     */
    private final LoadingCache<Long, String> taskId2OssUrl;

    @Autowired
    private TaskService taskService;
    @Autowired
    private OssConfiguration ossConfiguration;
    @Autowired
    private CloudObjectStorageService cloudObjectStorageService;

    public DatabaseChangeOssUrlCache() {
        taskId2OssUrl = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build(this::loadUrl);
    }

    public String get(Long taskId) {
        return taskId2OssUrl.get(taskId);
    }


    public String loadUrl(Long taskId) {
        TaskEntity taskEntity = taskService.detail(taskId);
        String ossDownloadUrl = null;
        if (!StringUtils.isEmpty(taskEntity.getResultJson())) {
            DatabaseChangeResult asyncTaskResult =
                    JsonUtils.fromJson(taskEntity.getResultJson(), DatabaseChangeResult.class);
            if (!Objects.isNull(asyncTaskResult)) {
                String ossPath = asyncTaskResult.getZipFileDownloadUrl();
                if (StringUtils.isEmpty(ossPath)) {
                    return null;
                }
                try {
                    String bucketName = ossPath.split("/")[0];
                    String objectName = ossPath.substring(bucketName.length() + 1);
                    ossDownloadUrl = cloudObjectStorageService.generateDownloadUrl(objectName,
                            ossConfiguration.getDownloadUrlExpirationIntervalSeconds()).toString();
                } catch (Exception ex) {
                    log.warn("Illegal ossPath or file on OSS has already been deleted, ossPath={}", ossPath, ex);
                    throw new NotFoundException(ResourceType.ODC_FILE, "ossPath", ossPath);
                }
            }
        }
        return ossDownloadUrl;
    }
}
