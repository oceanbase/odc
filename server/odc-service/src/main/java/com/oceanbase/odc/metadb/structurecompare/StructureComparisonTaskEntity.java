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
package com.oceanbase.odc.metadb.structurecompare;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import lombok.Data;

/**
 * @author jingtian
 * @date 2024/1/16
 * @since ODC_release_4.2.4
 */
@Data
@Entity
@Table(name = "structure_comparison_task")
public class StructureComparisonTaskEntity {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * Record insertion time
     */
    @Generated(GenerationTime.ALWAYS)
    @Column(name = "create_time", insertable = false, updatable = false,
            columnDefinition = "datetime NOT NULL DEFAULT CURRENT_TIMESTAMP")
    private Date createTime;
    /**
     * Record modification time
     */
    @Generated(GenerationTime.ALWAYS)
    @Column(name = "update_time", insertable = false, updatable = false,
            columnDefinition = "datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Date updateTime;
    /**
     * Creator id, references iam_user(id)
     */
    @Column(name = "creator_id", updatable = false, nullable = false)
    private Long creatorId;
    /**
     * Related flow instance id, references flow_instance(id)
     */
    @Column(name = "flow_instance_id", nullable = false)
    private long flowInstanceId;
    /**
     * Source connect database id, references connect_database(id)
     */
    @Column(name = "source_connect_database_id", nullable = false, updatable = false)
    private Long sourceConnectDatabaseId;
    /**
     * Target connect database id, references connect_database(id)
     */
    @Column(name = "target_connect_database_id", nullable = false, updatable = false)
    private Long targetConnectDatabaseId;
    /**
     * The storage object id of the total change script file, references
     * objectstorage_object_metadata(object_id)
     */
    @Column(name = "storage_object_id")
    private String storageObjectId;
}
