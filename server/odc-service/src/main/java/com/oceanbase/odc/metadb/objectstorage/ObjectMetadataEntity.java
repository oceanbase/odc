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

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.oceanbase.odc.service.objectstorage.model.ObjectUploadStatus;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2022/3/9 上午11:50
 * @Description: []
 */
@Data
@Entity
@Table(name = "objectstorage_object_metadata")
public class ObjectMetadataEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", insertable = false, nullable = false)
    private long id;

    /**
     * 创建时间
     */
    @Column(name = "create_time", insertable = false, updatable = false)
    private Date createTime;

    /**
     * 修改时间
     */
    @Column(name = "update_time", insertable = false, updatable = false)
    private Date updateTime;

    /**
     * 文件对象 ID
     */
    @Column(name = "object_id", nullable = false)
    private String objectId;

    /**
     * 对象名
     */
    @Column(name = "object_name", nullable = false)
    private String objectName;

    /**
     * 所属空间
     */
    @Column(name = "bucket_name", nullable = false)
    private String bucketName;

    /**
     * 创建者 id
     */
    @Column(name = "creator_id", nullable = false)
    private long creatorId;


    /**
     * 后缀名（保留字段，未来方便检索文件）
     */
    @Column(name = "extension")
    private String extension;

    /**
     * 文件SHA-1
     */
    @Column(name = "sha1")
    private String sha1;

    /**
     * 文件总大小，单位为 bytes
     */
    @Column(name = "total_length", nullable = false)
    private long totalLength;

    /**
     * 文件块拆分大小，单位为 bytes
     */
    @Column(name = "split_length", nullable = false)
    private long splitLength;

    /**
     * 文件上传状态
     */
    @Column(name = "status", nullable = false)
    @Enumerated(value = EnumType.STRING)
    private ObjectUploadStatus status;
}
