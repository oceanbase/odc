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

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2022/3/9 上午11:49
 * @Description: []
 */
@Data
@Entity
@Table(name = "objectstorage_object_block")
public class ObjectBlockEntity {

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
     * 关联的文件ID，FK refer to object_storage_meta.object_id<br>
     */
    @Column(name = "object_id", nullable = false)
    private String objectId;

    /**
     * 当前文件块 索引编号
     */
    @Column(name = "block_index", nullable = false)
    private long index;

    /**
     * 当前文件块 大小，单位为 bytes
     */
    @Column(name = "length", nullable = false)
    private long length;

    /**
     * 文件块内容（MEDIUMBLOB 存储长度上限 16M）
     */
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "content")
    private byte[] content;

}
