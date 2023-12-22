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

package com.oceanbase.odc.service.task.executor.task;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.exception.InternalServerError;
import com.oceanbase.odc.service.common.util.OdcFileUtil;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;
import com.oceanbase.odc.service.objectstorage.model.StorageObject;
import com.oceanbase.odc.service.objectstorage.operator.LocalFileOperator;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-12-17
 * @since 4.2.4
 */
@Slf4j
public class ObjectStorageHandler {

    private final LocalFileOperator localFileOperator;
    private final CloudObjectStorageService cloudObjectStorageService;
    private final String localDir;

    public ObjectStorageHandler(CloudObjectStorageService cloudObjectStorageService, String localDir) {
        this.localFileOperator = new LocalFileOperator(localDir);
        this.cloudObjectStorageService = cloudObjectStorageService;
        this.localDir = localDir;
    }

    public ObjectStorageHandler(CloudObjectStorageService cloudObjectStorageService,
            LocalFileOperator localFileOperator) {
        this.localFileOperator = localFileOperator;
        this.cloudObjectStorageService = cloudObjectStorageService;
        this.localDir = localFileOperator.getLocalDir();
    }

    public String loadObjectContentAsString(ObjectMetadata metadata) throws IOException {
        StorageObject storageObject = loadObject(metadata);
        return IOUtils.toString(storageObject.getContent(), StandardCharsets.UTF_8);
    }

    public String loadObjectContentGetZipContent(ObjectMetadata metadata) throws IOException {
        localFileOperator.deleteLocalFile(metadata.getBucketName(), metadata.getObjectId());
        File tempFile = cloudObjectStorageService.downloadToTempFile(metadata.getObjectId());
        log.info("download oss tempFile {}", tempFile);
        File unzipPath = new File(localDir + File.separator + metadata.getBucketName()
                + File.pathSeparator + StringUtils.uuid());
        log.info("mk unzip path {} ", unzipPath.getAbsolutePath());
        OdcFileUtil.mkdir(unzipPath);
        if (tempFile.getName().endsWith(".zip")) {
            OdcFileUtil.unzip(tempFile, unzipPath.getAbsolutePath());
        }
        // todo log file
        String logFilePath = unzipPath + File.pathSeparator + "odc.log";
        log.info("current log path is {} ", logFilePath);
        try (FileInputStream inputStream = new FileInputStream(logFilePath)) {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } finally {
            FileUtils.deleteQuietly(tempFile);
            FileUtils.deleteQuietly(unzipPath);
        }
    }

    public StorageObject loadObject(ObjectMetadata metadata) {
        try {
            if (localFileOperator.isLocalFileAbsent(metadata)) {
                loadObjectFromOss(metadata);
            }
            Resource resource = localFileOperator.loadAsResource(metadata.getBucketName(), metadata.getObjectId());
            return StorageObject.builder().content(resource.getInputStream()).metadata(metadata).build();
        } catch (IOException ex) {
            log.warn("Load object failed, bucket={}, objectId={}", metadata.getBucketName(), metadata.getObjectId(),
                    ex);
            throw new InternalServerError("Load object failed", ex);
        }
    }

    private void loadObjectFromOss(ObjectMetadata metadata) throws IOException {
        localFileOperator.deleteLocalFile(metadata.getBucketName(), metadata.getObjectId());
        File tempFile = cloudObjectStorageService.downloadToTempFile(metadata.getObjectId());
        try (FileInputStream inputStream = new FileInputStream(tempFile)) {
            FileUtils.copyInputStreamToFile(inputStream,
                    localFileOperator.getOrCreateLocalFile(metadata.getBucketName(),
                            metadata.getObjectId()));
        } finally {
            FileUtils.deleteQuietly(tempFile);
        }
    }

}
