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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.exception.InternalServerError;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;
import com.oceanbase.odc.service.objectstorage.model.ObjectUploadStatus;
import com.oceanbase.odc.service.objectstorage.model.StorageObject;
import com.oceanbase.odc.service.objectstorage.operator.LocalFileOperator;
import com.oceanbase.odc.service.objectstorage.operator.ObjectBlockIterator;
import com.oceanbase.odc.service.objectstorage.operator.ObjectBlockOperator;
import com.oceanbase.odc.service.objectstorage.operator.ObjectMetaOperator;
import com.oceanbase.odc.service.objectstorage.util.ObjectStorageUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2022/3/9 下午9:13
 * @Description: []
 */
@Slf4j
@Component("localObjectStorageFacade")
public class LocalObjectStorageFacade extends AbstractObjectStorageFacade {
    private final String localDownloadBaseUrl = "/api/v2/objectstorage/files";

    @Autowired
    private ObjectBlockOperator blockOperator;

    @Autowired
    private ObjectMetaOperator metaOperator;

    @Autowired
    private LocalFileOperator localFileOperator;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private TempId2ObjectMetaCache tempId2ObjectMetaCache;

    public LocalObjectStorageFacade(@Value("${odc.objectstorage.max-concurrent-count:16}") int maxConcurrentCount,
            @Value("${odc.objectstorage.try-lock-timeout-milliseconds:10000}") long tryLockTimeoutMillisSeconds,
            PlatformTransactionManager transactionManager) {
        super(maxConcurrentCount, tryLockTimeoutMillisSeconds, transactionManager);
    }

    @Override
    public String getDownloadUrl(String bucket, String objectId) {
        String tempId = StringUtils.uuid();
        tempId2ObjectMetaCache.put(tempId, metaOperator.getObjectMeta(bucket, objectId));
        return localDownloadBaseUrl.concat("/").concat(tempId);
    }

    @Override
    public ObjectMetadata putObject(String bucket, String objectName, long totalLength, InputStream inputStream,
            boolean isPersistent) {
        return putObject(bucket, objectName, currentUserId(), totalLength, inputStream, isPersistent);
    }

    @Override
    public ObjectMetadata updateObject(String bucket, String objectName, String objectId, long totalLength,
            InputStream inputStream) {
        try {
            return objectStorageExecutor.concurrentSafeExecute(
                    () -> doUpdateObject(bucket, objectName, objectId, currentUserId(), totalLength, inputStream));
        } catch (Exception ex) {
            log.warn("update object failed, cause={}", ex.getMessage());
            throw new InternalServerError("put object failed", ex);
        }
    }

    @Override
    public ObjectMetadata deleteObject(String bucket, String objectId) {
        ObjectMetadata metadata = metaOperator.getObjectMeta(bucket, objectId);
        log.info("Delete object, bucket={}, name={}, objectId={}", bucket, objectId, objectId);
        transactionTemplate.executeWithoutResult(t -> {
            blockOperator.deleteByObjectId(objectId);
            metaOperator.deleteByObjectId(Arrays.asList(objectId));
        });
        localFileOperator.deleteLocalFile(bucket, ObjectStorageUtils.concatObjectId(metadata.getObjectId(),
                metadata.getExtension()));
        log.info("delete local file successfully, bucket={}, objectId={}", bucket, objectId);
        return metadata;
    }

    @Override
    public StorageObject loadObject(String bucket, String objectId) throws IOException {
        ObjectMetadata metadata = metaOperator.getObjectMeta(bucket, objectId);
        log.info("Load object data, bucket={}, name={}, objectId={}", bucket, metadata.getObjectId(), objectId);
        Resource resource = objectStorageExecutor.concurrentSafeExecute(() -> loadResourceFromLocalhost(metadata));
        return StorageObject.builder().metadata(metadata)
                .content(resource.getInputStream()).build();
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

    public ObjectMetadata loadMetaData(String bucket, String objectId) {
        ObjectMetadata metadata = metaOperator.getObjectMeta(bucket, objectId);
        if (localFileOperator.isLocalFileAbsent(metadata)) {
            try {
                loadDbDataToLocalhost(metadata);
            } catch (Exception e) {
                log.warn("Load file from remote failed, bucketName={}, objectId={}", bucket, objectId, e);
                throw new InternalServerError("load file from remote failed", e);
            }
        }
        return metadata;
    }

    private long currentUserId() {
        return authenticationFacade.currentUserId();
    }

    private void cleanFailedRecord(String objectId) {
        log.info("Clean failed upload record, objectId={}", objectId);
        transactionTemplate.executeWithoutResult(t -> {
            blockOperator.deleteByObjectId(objectId);
            metaOperator.deleteByObjectId(Arrays.asList(objectId));
        });
    }

    private Resource loadResourceFromLocalhost(ObjectMetadata metadata) {
        String bucket = metadata.getBucketName();
        String objectId = metadata.getObjectId();
        // 如果本地文件不存在，那么从 DB 中加载文件内容到本地.
        if (localFileOperator.isLocalFileAbsent(metadata)) {
            try {
                loadDbDataToLocalhost(metadata);
            } catch (IOException e) {
                log.warn("load file from db to local failed, bucketName={}, objectId={}", bucket, objectId);
                throw new InternalServerError("load file failed");
            }
        }
        return localFileOperator.loadAsResource(bucket, ObjectStorageUtils.concatObjectId(metadata.getObjectId(),
                metadata.getExtension()));
    }

    private void loadDbDataToLocalhost(ObjectMetadata metadata) throws IOException {
        localFileOperator.deleteLocalFile(metadata.getBucketName(),
                ObjectStorageUtils.concatObjectId(metadata.getObjectId(),
                        metadata.getExtension()));
        ObjectBlockIterator iterator = blockOperator.getBlockIterator(metadata.getObjectId());
        File localFile = localFileOperator.getOrCreateLocalFile(metadata.getBucketName(),
                ObjectStorageUtils.concatObjectId(metadata.getObjectId(),
                        metadata.getExtension()));
        try (FileOutputStream fileOutputStream = new FileOutputStream(localFile)) {
            while (iterator.hasNext()) {
                fileOutputStream.write(iterator.next());
            }
        }
    }

    private ObjectMetadata doPutObject(String bucket, String objectName, long creatorId, long totalLength,
            InputStream inputStream, boolean isPersistent) {
        ObjectMetadata metadata = metaOperator.initSaving(bucket, creatorId, objectName, StringUtils.uuid(),
                totalLength,
                blockSplitLength);
        log.info("Init saving object, creatorId={}, bucket={}, name={}, objectId={}",
                creatorId, metadata.getBucketName(), metadata.getBucketName(), metadata.getObjectId());

        String objectId = metadata.getObjectId();
        try {
            String sha1 = localFileOperator.saveLocalFile(bucket,
                    ObjectStorageUtils.concatObjectId(objectId, metadata.getExtension()), totalLength,
                    inputStream);
            File localFile = localFileOperator.getOrCreateLocalFile(bucket,
                    ObjectStorageUtils.concatObjectId(objectId, metadata.getExtension()));
            if (isPersistent) {
                blockOperator.saveObjectBlock(metadata, localFile);
            }
            metaOperator.finishSaving(metadata.getObjectId(), sha1);
            log.info("Finish saving object, objectId={}, name={}", objectId, objectName);
            metadata.setSha1(sha1);
            return metadata;
        } catch (Exception e) {
            log.warn("Unexpected exception when saving object", e);
            // 如果保存过程出现异常，那么清除掉失败记录
            if (StringUtils.isNotBlank(objectId)) {
                cleanFailedRecord(objectId);
            }
            throw e;
        }
    }

    /**
     * 更新 Object，先保存新的 object，保存成功后再删除旧的 object
     */
    private ObjectMetadata doUpdateObject(String bucket, String objectName, String objectId, long creatorId,
            long totalLength, InputStream inputStream) {
        preCheckUpdateObject(bucket, objectId);
        ObjectMetadata createdObjectMetadata;
        try {
            createdObjectMetadata = doPutObject(bucket, objectName, creatorId, totalLength, inputStream, true);
        } catch (Exception exception) {
            log.warn("put object failed, bucket={}, objectName={}", bucket, objectName);
            throw new InternalServerError("put object failed, ex={}", exception);
        }
        // if exists then delete
        if (metaOperator.existsByBucketNameAndObjectId(bucket, objectId)) {
            deleteObject(bucket, objectId);
        }

        return createdObjectMetadata;
    }

    /**
     * 更新 Object 的前置检查：先检查该 object 是否存在，再检查状态是否为上传失败
     */
    private void preCheckUpdateObject(String bucketName, String objectId) {
        // 获取元信息，若元信息不存在则抛异常
        ObjectMetadata objectMetadata = metaOperator.getObjectMeta(bucketName, objectId);

        // 如果记录存在，状态是初始化，并且状态是上传失败则删除原纪录.
        if (objectMetadata.getStatus() == ObjectUploadStatus.INIT
                && blockOperator.isUploadingFailed(objectMetadata.getObjectId())) {
            cleanFailedRecord(objectMetadata.getObjectId());
        }
    }

}
