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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.compress.utils.Lists;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.objectstorage.ObjectMetadataSpecs;
import com.oceanbase.odc.service.objectstorage.model.Bucket;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;
import com.oceanbase.odc.service.objectstorage.model.QueryObjectMetadataParam;
import com.oceanbase.odc.service.objectstorage.model.StorageObject;
import com.oceanbase.odc.service.objectstorage.operator.BucketOperator;
import com.oceanbase.odc.service.objectstorage.operator.ObjectMetaOperator;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2022/3/9 下午9:10
 * @Description: []
 */
@Slf4j
public abstract class AbstractObjectStorageFacade implements ObjectStorageFacade {

    @Getter
    @Setter
    protected TransactionTemplate transactionTemplate;

    @Getter
    @Setter
    protected ObjectStorageExecutor objectStorageExecutor;

    @Getter
    @Setter
    protected long tryLockTimeoutMillisSeconds;

    /**
     * 限制单块大小最大为 1M，分块过大会因为超过 DB 的 max_allowed_packet(4194304) 出错.
     */
    @Getter
    @Setter
    @Value("${odc.objectstorage.default-block-split-length:#{1024*1024}}")
    protected long blockSplitLength = 1024 * 1024L;

    @Autowired
    protected BucketOperator bucketOperator;

    @Autowired
    protected ObjectMetaOperator metaOperator;

    public AbstractObjectStorageFacade(@Value("${odc.objectstorage.max-concurrent-count:16}") int maxConcurrentCount,
            @Value("${odc.objectstorage.try-lock-timeout-milliseconds:10000}") long tryLockTimeoutMillisSeconds,
            PlatformTransactionManager transactionManager) {
        Verify.notNull(transactionManager, "transactionManager");
        this.tryLockTimeoutMillisSeconds = tryLockTimeoutMillisSeconds;
        this.objectStorageExecutor = new ObjectStorageExecutor(maxConcurrentCount, tryLockTimeoutMillisSeconds);
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }


    /**
     * Save object in a specific bucket
     */
    abstract public ObjectMetadata putObject(String bucket, String objectName, long totalLength,
            InputStream inputStream,
            boolean isPersistent);

    /**
     * Save object in a specific bucket, for migration
     */
    abstract public ObjectMetadata putObject(String bucket, String objectName, long userId, long totalLength,
            InputStream inputStream,
            boolean isPersistent);

    public ObjectMetadata putObject(String bucket, String objectName, long totalLength,
            InputStream inputStream) {
        return putObject(bucket, objectName, totalLength, inputStream, true);
    }

    public ObjectMetadata putTempObject(String bucket, String objectName, long totalLength, InputStream inputStream) {
        return putObject(bucket, objectName, totalLength, inputStream, false);
    }

    public ObjectMetadata putTempObject(String bucket, String objectName, long userId, long totalLength,
            InputStream inputStream) {
        return putObject(bucket, objectName, userId, totalLength, inputStream, false);
    }

    public ObjectMetadata putObject(String bucket, String objectName, long userId, long totalLength,
            InputStream inputStream) {
        return putObject(bucket, objectName, userId, totalLength, inputStream, true);
    }

    @Override
    public String loadObjectContentAsString(String bucket, String objectId) throws IOException {
        StorageObject storageObject = loadObject(bucket, objectId);
        return IOUtils.toString(storageObject.getContent(), StandardCharsets.UTF_8);
    }

    @Override
    public List<StorageObject> loadObject(String bucket) throws IOException {
        List<StorageObject> objects = Lists.newArrayList();
        List<ObjectMetadata> metadataList =
                metaOperator
                        .listAll(ObjectMetadataSpecs.of(QueryObjectMetadataParam.builder().bucketName(bucket).build()));

        for (ObjectMetadata metadata : metadataList) {
            objects.add(loadObject(metadata.getBucketName(), metadata.getObjectId()));
        }
        return objects;
    }

    @Override
    public boolean isObjectExists(String bucket, String objectId) {
        if (bucketOperator.isBucketExist(bucket)) {
            return metaOperator.existsByBucketNameAndObjectId(bucket, objectId);
        }
        return false;
    }

    @Override
    public Bucket createBucketIfNotExists(String name) {
        if (isBucketExists(name)) {
            return getBucket(name);
        }
        return bucketOperator.createBucket(name);
    }

    @Override
    public Bucket getBucket(String name) {
        return bucketOperator.getBucketByName(name)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_BUCKET, "name", name));
    }

    @Override
    public boolean isBucketExists(String name) {
        return bucketOperator.isBucketExist(name);
    }
}
