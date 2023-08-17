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
package com.oceanbase.odc.metadb.objectstorage;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.service.objectstorage.model.ObjectUploadStatus;

public interface ObjectMetadataRepository extends JpaSpecificationExecutor<ObjectMetadataEntity>,
        JpaRepository<ObjectMetadataEntity, String> {

    /**
     * 根据文件 ID 获取文件元数据
     *
     * @param objectId 文件 ID
     * @return 文件元数据
     */
    Optional<ObjectMetadataEntity> findByObjectId(String objectId);

    /**
     * 根据存储空间名 和 文件 ID 获取文件元数据
     *
     * @param objectId 文件 ID
     * @return 文件元数据
     */
    Optional<ObjectMetadataEntity> findByBucketNameAndObjectId(String bucketName, String objectId);


    boolean existsByBucketNameAndObjectId(String bucketName, String objectId);

    /**
     * 使用文件id列表删除文件记录.
     *
     * @param objectIds 文件唯一 ID 的列表
     * @return 删除记录数量
     */
    @Modifying
    @Query("DELETE FROM ObjectMetadataEntity e WHERE e.objectId IN (:objectIds)")
    @Transactional(rollbackFor = Exception.class)
    int deleteAllByObjectIdIn(@Param("objectIds") Collection<String> objectIds);

    /**
     * 更新指定记录的存储状态.
     *
     * @param objectId 对象 id
     * @param sha1 文件的 sha1
     * @param status 对象存储状态
     * @return 修改数量
     */
    @Modifying
    @Query("UPDATE ObjectMetadataEntity e SET e.status=:status, e.sha1=:sha1 WHERE e.objectId=:objectId")
    @Transactional(rollbackFor = Exception.class)
    int updateStatusAndSha1ByObjectId(@Param("objectId") String objectId, @Param("sha1") String sha1,
            @Param("status") ObjectUploadStatus status);

    /**
     * 根据存储空间和存储状态查询对象 id 集合.
     *
     * @param bucketName 存储空间
     * @param status 对象存储状态
     * @return 存储对象 id 集合
     */
    @Query("SELECT e.objectId FROM ObjectMetadataEntity e WHERE e.bucketName=:bucketName and e.status=:status")
    Set<Long> findAllObjectIdByBucketAndStatus(@Param("bucketName") String bucketName,
            @Param("status") ObjectUploadStatus status);

    /**
     * 根据存储空间、文件名、存储状态查询对象 id 集合.
     *
     * @param bucketName 存储空间
     * @param objectNameLike 文件模糊查询选项
     * @param status 对象存储状态
     * @return 存储对象 id 集合
     */
    @Query("SELECT e.objectId FROM ObjectMetadataEntity e WHERE e.bucketName=:bucketName and e.status=:status " +
            "AND e.objectName LIKE CONCAT('%',:objectName,'%')")
    Set<Long> findAllObjectIdByBucketAndNameLikeAndStatus(@Param("bucketName") String bucketName,
            @Param("objectName") String objectNameLike,
            @Param("status") ObjectUploadStatus status);
}
