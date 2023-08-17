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
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import com.oceanbase.odc.common.util.HashUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.exception.InternalServerError;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.objectstorage.cloud.util.CloudObjectStorageUtil;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;
import com.oceanbase.odc.service.objectstorage.model.StorageObject;
import com.oceanbase.odc.service.objectstorage.operator.LocalFileOperator;
import com.oceanbase.odc.service.objectstorage.operator.ObjectMetaOperator;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2022/3/9 下午9:15
 * @Description: []
 */
@Slf4j
@Component("cloudEnvironmentObjectStorageFacade")
public class CloudEnvironmentObjectStorageFacade extends AbstractObjectStorageFacade {

    @Autowired
    private ObjectMetaOperator metaOperator;

    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private CloudObjectStorageService cloudObjectStorageService;
    @Autowired
    private LocalFileOperator localFileOperator;

    public CloudEnvironmentObjectStorageFacade(
            @Value("${odc.objectstorage.max-concurrent-count:16}") int maxConcurrentCount,
            @Value("${odc.objectstorage.try-lock-timeout-milliseconds:10000}") long tryLockTimeoutMillisSeconds,
            PlatformTransactionManager transactionManager) {
        super(maxConcurrentCount, tryLockTimeoutMillisSeconds, transactionManager);
    }

    @Override
    public String getDownloadUrl(String bucket, String objectId) {
        try {
            return cloudObjectStorageService.generateDownloadUrl(objectId).toString();
        } catch (IOException ex) {
            log.warn("Get oss temp download url failed, objectId={}", objectId);
            throw new InternalServerError("Get oss temp download url failed", ex);
        }
    }

    @Override
    public ObjectMetadata putObject(String bucket, String objectName, long totalLength, InputStream inputStream,
            boolean isPersistent) {
        return putObject(bucket, objectName, currentUserId(), totalLength, inputStream, isPersistent);
    }

    @Override
    public ObjectMetadata putObject(String bucket, String objectName, long userId, long totalLength,
            InputStream inputStream, boolean isPersistent) {
        try {
            return objectStorageExecutor.concurrentSafeExecute(
                    () -> doPutObject(bucket, objectName, userId, totalLength, inputStream, isPersistent));
        } catch (Exception ex) {
            log.warn("put object failed, cause={}", ex.getMessage());
            throw new InternalServerError("put object failed", ex);
        }
    }

    @Override
    public ObjectMetadata updateObject(String bucket, String objectName, String objectId, long totalLength,
            InputStream inputStream) {
        ObjectMetadata createdMetadata = putObject(bucket, objectName, totalLength, inputStream, true);
        deleteObject(bucket, objectId);
        return createdMetadata;

    }

    @Override
    public ObjectMetadata deleteObject(String bucket, String objectId) {
        ObjectMetadata metadata = metaOperator.getObjectMeta(bucket, objectId);
        transactionTemplate.executeWithoutResult(t -> metaOperator.deleteByObjectId(Arrays.asList(objectId)));
        try {
            // use objectId as objectName in cloud object storage,
            // due fileName may duplicate, and objectId was ODC generated uuid, must be unique
            String objectName = metadata.getObjectId();
            cloudObjectStorageService.delete(objectName);
        } catch (IOException ex) {
            log.warn("delete oss object failed, objectId={}", objectId, ex);
        }
        localFileOperator.deleteLocalFile(bucket, objectId);
        return metadata;
    }

    @Override
    public StorageObject loadObject(String bucket, String objectId) {
        try {
            ObjectMetadata metadata = metaOperator.getObjectMeta(bucket, objectId);
            if (localFileOperator.isLocalFileAbsent(metadata)) {
                loadObjectFromOss(metadata);
            }
            Resource resource = localFileOperator.loadAsResource(metadata.getBucketName(), metadata.getObjectId());
            return StorageObject.builder().content(resource.getInputStream()).metadata(metadata).build();
        } catch (IOException ex) {
            log.warn("Load object failed, bucket={}, objectId={}", bucket, objectId, ex);
            throw new InternalServerError("Load object failed", ex);
        }
    }

    @Override
    public ObjectMetadata loadMetaData(String bucket, String objectId) {
        ObjectMetadata metadata = metaOperator.getObjectMeta(bucket, objectId);
        if (localFileOperator.isLocalFileAbsent(metadata)) {
            try {
                loadObjectFromOss(metadata);
            } catch (Exception ex) {
                log.warn("Load object from remote failed, bucket={}, objectId={}", bucket, objectId);
                throw new InternalServerError("Load object from remote failed", ex);
            }
        }
        return metadata;
    }

    public ObjectMetadata putObjectFromCloudStorage(String bucket, String objectName) throws IOException {
        createBucketIfNotExists(bucket);

        File tempFile = cloudObjectStorageService.downloadToTempFile(objectName);
        try (FileInputStream inputStream = new FileInputStream(tempFile)) {
            return saveObjectMetadata(bucket, CloudObjectStorageUtil.getOriginalFileName(objectName), objectName,
                    currentUserId(),
                    tempFile.length(), inputStream);
        } finally {
            FileUtils.deleteQuietly(tempFile);
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

    private long currentUserId() {
        return authenticationFacade.currentUserId();
    }

    private ObjectMetadata doPutObject(String bucket, String objectName, long creatorId, long totalLength,
            InputStream inputStream, boolean isPersistent) {
        String objectId;
        try {
            objectId = isPersistent ? cloudObjectStorageService.upload(objectName, inputStream)
                    : cloudObjectStorageService.uploadTemp(objectName, inputStream);
        } catch (IOException ex) {
            log.warn("Failed to put object onto OSS, objectName={}", objectName, ex);
            throw new InternalServerError("Failed to put object onto OSS", ex);
        }
        Verify.notNull(objectId, "objectId");
        return saveObjectMetadata(bucket, objectName, objectId, creatorId, totalLength, inputStream);
    }


    private ObjectMetadata saveObjectMetadata(String bucket, String objectName, String objectId, long creatorId,
            long totalLength,
            InputStream inputStream) {
        String sha1;
        try {
            sha1 = HashUtils.sha1(IOUtils.toByteArray(inputStream));
        } catch (IOException ex) {
            log.warn("Failed to calculate sha1 of object, objectName={}", objectName, ex);
            throw new InternalServerError("Failed to put object", ex);
        }
        ObjectMetadata metadata =
                metaOperator.save(bucket, creatorId, objectName, objectId, totalLength, blockSplitLength,
                        sha1);
        log.info("Finish saving object, objectName={}", objectName);
        return metadata;
    }
}
