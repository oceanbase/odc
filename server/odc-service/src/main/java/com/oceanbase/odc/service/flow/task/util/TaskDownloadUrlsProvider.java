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
import com.oceanbase.odc.core.flow.model.AbstractFlowTaskResult;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeResult;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.objectstorage.cloud.model.OssConfiguration;
import com.oceanbase.odc.service.task.TaskService;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2021/10/20 下午2:07
 * @Description: [This class is responsible for maintaining relationships from async task id to its
 *               result set temp OSS download url.]
 */
@Service
@Slf4j
public class TaskDownloadUrlsProvider {
    private final LoadingCache<Long, TaskDownloadUrls> taskId2OssUrls;

    @Autowired
    private TaskService taskService;
    @Autowired
    private OssConfiguration ossConfiguration;
    @Autowired
    private CloudObjectStorageService cloudObjectStorageService;

    public TaskDownloadUrlsProvider() {
        taskId2OssUrls = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build(this::loadUrls);
    }

    public TaskDownloadUrls get(Long taskId) {
        return taskId2OssUrls.get(taskId);
    }


    private TaskDownloadUrls loadUrls(Long taskId) {
        TaskEntity taskEntity = taskService.detail(taskId);
        TaskDownloadUrls urls = new TaskDownloadUrls();
        if (StringUtils.isNotEmpty(taskEntity.getResultJson())) {
            AbstractFlowTaskResult taskResult =
                    JsonUtils.fromJson(taskEntity.getResultJson(), AbstractFlowTaskResult.class);
            if (Objects.nonNull(taskResult)) {
                urls.setLogDownloadUrl(generateUrl(taskResult.getFullLogDownloadUrl()));
            }
            if (taskEntity.getTaskType() == TaskType.ASYNC) {
                DatabaseChangeResult asyncTaskResult =
                        JsonUtils.fromJson(taskEntity.getResultJson(), DatabaseChangeResult.class);
                if (Objects.nonNull(asyncTaskResult)) {
                    urls.setDatabaseChangeZipFileDownloadUrl(generateUrl(asyncTaskResult.getZipFileDownloadUrl()));
                    if (Objects.nonNull(asyncTaskResult.getRollbackPlanResult())) {
                        urls.setRollBackPlanResultFileDownloadUrl(
                                generateUrl(asyncTaskResult.getRollbackPlanResult().getResultFileDownloadUrl()));
                    }
                }
            }
        }
        return urls;
    }

    private String generateUrl(String bucketAndObjectName) {
        if (StringUtils.isEmpty(bucketAndObjectName)) {
            return null;
        }
        try {
            String bucketName = bucketAndObjectName.split("/")[0];
            String objectName = bucketAndObjectName.substring(bucketName.length() + 1);
            return cloudObjectStorageService
                    .generateDownloadUrl(objectName, ossConfiguration.getDownloadUrlExpirationIntervalSeconds())
                    .toString();
        } catch (Exception ex) {
            log.warn("Illegal ossPath or file on OSS has already been deleted, bucketAndObjectName={}",
                    bucketAndObjectName, ex);
            throw new NotFoundException(ResourceType.ODC_FILE, "bucketAndObjectName", bucketAndObjectName);
        }
    }

    public static String concatBucketAndObjectName(String bucketName, String objectName) {
        return bucketName + "/" + objectName;
    }

    @Data
    public static class TaskDownloadUrls {
        private String logDownloadUrl;
        private String databaseChangeZipFileDownloadUrl;
        private String rollBackPlanResultFileDownloadUrl;
    }
}
