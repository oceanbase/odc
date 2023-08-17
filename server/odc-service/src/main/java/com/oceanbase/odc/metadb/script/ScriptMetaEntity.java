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
package com.oceanbase.odc.metadb.script;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: Lebie
 * @Date: 2022/3/21 下午8:38
 * @Description: []
 */
@Data
@Builder
@Entity
@Table(name = "script_meta")
@AllArgsConstructor
@NoArgsConstructor
public class ScriptMetaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "create_time", nullable = false, insertable = false, updatable = false)
    private Date createTime;

    @Column(name = "update_time", nullable = false, insertable = false, updatable = false)
    private Date updateTime;

    @Column(name = "object_id", nullable = false)
    private String objectId;

    @Column(name = "object_name", nullable = false)
    private String objectName;

    @Column(name = "bucket_name", nullable = false)
    private String bucketName;

    @Column(name = "content_abstract")
    private String contentAbstract;

    @Column(name = "content_length")
    private Long contentLength;

    @Column(name = "creator_id", nullable = false)
    private long creatorId;
}
