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
package com.oceanbase.odc.service.objectstorage.operator;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.objectstorage.BucketEntity;
import com.oceanbase.odc.metadb.objectstorage.BucketRepository;
import com.oceanbase.odc.metadb.objectstorage.ObjectMetadataEntity;
import com.oceanbase.odc.metadb.objectstorage.ObjectMetadataRepository;
import com.oceanbase.odc.service.objectstorage.ObjectMetadataMapper;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;
import com.oceanbase.odc.service.objectstorage.model.ObjectUploadStatus;

/**
 * @Author: Lebie
 * @Date: 2022/3/4 下午5:28
 * @Description: [Responsible for operating object meta, like CRUD]
 */
@Component
public class ObjectMetaOperator {
    @Autowired
    private ObjectMetadataRepository metadataRepository;

    @Autowired
    private BucketRepository bucketRepository;

    private ObjectMetadataMapper mapper = ObjectMetadataMapper.INSTANCE;

    public ObjectMetadata save(String bucketName, long creatorId, String objectName, String objectId, long totalLength,
            long splitLength, String sha1) {
        bucketRepository.findByName(bucketName)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_BUCKET, "bucketName",
                        bucketName));
        ObjectMetadataEntity metaEntity =
                metadataRepository.findByObjectId(objectId).orElse(new ObjectMetadataEntity());
        metaEntity.setObjectId(objectId);
        metaEntity.setBucketName(bucketName);
        metaEntity.setCreatorId(creatorId);
        metaEntity.setTotalLength(totalLength);
        metaEntity.setObjectName(objectName);
        metaEntity.setSplitLength(splitLength);
        metaEntity.setExtension(StringUtils.length(FilenameUtils.getExtension(objectName)) > 64 ? null
                : FilenameUtils.getExtension(objectName));
        metaEntity.setSha1(sha1);
        metaEntity.setStatus(ObjectUploadStatus.FINISHED);
        return mapper.entityToModel(metadataRepository.saveAndFlush(metaEntity));
    }

    /**
     * 初始化对象存储内容，此时存储状态为 INIT
     */
    public ObjectMetadata initSaving(String bucketName, long creatorId, String objectName, String objectId,
            long totalLength, long splitLength) {
        Optional<BucketEntity> bucketEntityOpt = bucketRepository.findByName(bucketName);
        if (!bucketEntityOpt.isPresent()) {
            throw new NotFoundException(ResourceType.ODC_BUCKET, "bucketName", bucketName);
        }
        Optional<ObjectMetadataEntity> metaEntityOpt =
                metadataRepository.findByObjectId(objectId);
        ObjectMetadataEntity metaEntity = metaEntityOpt.orElse(new ObjectMetadataEntity());
        metaEntity.setObjectId(objectId);
        metaEntity.setBucketName(bucketName);
        metaEntity.setCreatorId(creatorId);
        metaEntity.setTotalLength(totalLength);
        metaEntity.setObjectName(objectName);
        metaEntity.setSplitLength(splitLength);
        metaEntity.setExtension(StringUtils.length(FilenameUtils.getExtension(objectName)) > 64 ? null
                : FilenameUtils.getExtension(objectName));
        metaEntity.setStatus(ObjectUploadStatus.INIT);
        return mapper.entityToModel(metadataRepository.saveAndFlush(metaEntity));
    }

    /**
     * 完成对象存储.
     */
    public void finishSaving(String objectId, String sha1) {
        metadataRepository.updateStatusAndSha1ByObjectId(objectId, sha1, ObjectUploadStatus.FINISHED);
    }

    /**
     * 获取存储对象元信息
     *
     * @param bucketName 存储空间名
     * @param objectId 存储对象 ID
     */
    public ObjectMetadata getObjectMeta(String bucketName, String objectId) {
        Optional<ObjectMetadataEntity> existsFileMeta =
                metadataRepository.findByBucketNameAndObjectId(bucketName, objectId);
        return mapper
                .entityToModel(
                        existsFileMeta.orElseThrow(() -> new NotFoundException(ResourceType.ODC_STORAGE_OBJECT_METADATA,
                                "objectId", objectId)));
    }

    public Optional<ObjectMetadata> getObjectMetaNonException(String bucketName, String objectId) {
        return metadataRepository.findByBucketNameAndObjectId(bucketName, objectId).map(mapper::entityToModel);
    }


    public ObjectMetadata getObjectMeta(String objectId) {
        Optional<ObjectMetadataEntity> existsFileMeta =
                metadataRepository.findByObjectId(objectId);
        return mapper
                .entityToModel(
                        existsFileMeta.orElseThrow(() -> new NotFoundException(ResourceType.ODC_STORAGE_OBJECT_METADATA,
                                "objectId", objectId)));
    }

    public Optional<ObjectMetadata> getObjectMetaNonException(String objectId) {
        return metadataRepository.findByObjectId(objectId).map(mapper::entityToModel);
    }


    public List<ObjectMetadata> listAll(Specification<ObjectMetadataEntity> specs) {
        return metadataRepository.findAll(specs).stream()
                .map(mapper::entityToModel)
                .collect(Collectors.toList());
    }

    public boolean existsByBucketNameAndObjectId(String bucketName, String objectId) {
        return metadataRepository.existsByBucketNameAndObjectId(bucketName, objectId);
    }

    public void deleteByObjectId(List<String> objectId) {
        metadataRepository.deleteAllByObjectIdIn(objectId);
    }

}
