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
package com.oceanbase.odc.service.objectstorage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;

import com.oceanbase.odc.core.shared.exception.InternalServerError;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;
import com.oceanbase.odc.service.objectstorage.model.StorageObject;
import com.oceanbase.odc.service.objectstorage.operator.LocalFileOperator;
import com.oceanbase.odc.service.objectstorage.util.ObjectStorageUtils;

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

    public ObjectStorageHandler(CloudObjectStorageService cloudObjectStorageService, String localDir) {
        this.localFileOperator = new LocalFileOperator(localDir);
        this.cloudObjectStorageService = cloudObjectStorageService;
    }

    public String loadObjectContentAsString(ObjectMetadata metadata) throws IOException {
        StorageObject storageObject = loadObject(metadata);
        return IOUtils.toString(storageObject.getContent(), StandardCharsets.UTF_8);
    }

    public StorageObject loadObject(ObjectMetadata metadata) {
        Resource resource;
        try {
            if (cloudObjectStorageService.supported()) {
                // OSS supported, load file from local if exists, otherwise load file from oss
                if (localFileOperator.isLocalFileAbsent(metadata)) {
                    log.info("File is absent in local, load file from oss, bucket={}, objectId={}",
                            metadata.getBucketName(), metadata.getObjectId());
                    loadObjectFromOss(metadata);
                }
                resource = localFileOperator.loadAsResource(metadata.getBucketName(), metadata.getObjectId());
            } else {
                // OSS not supported, load file from local
                resource = localFileOperator.loadAsResource(metadata.getBucketName(),
                        ObjectStorageUtils.concatObjectId(metadata.getObjectId(), metadata.getExtension()));
            }
            return StorageObject.builder().content(resource.getInputStream()).metadata(metadata).build();
        } catch (IOException ex) {
            log.warn("Load object failed, bucket={}, objectId={}", metadata.getBucketName(), metadata.getObjectId(),
                    ex);
            throw new InternalServerError("Load object failed", ex);
        }
    }

    private void loadObjectFromOss(ObjectMetadata metadata) throws IOException {
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
