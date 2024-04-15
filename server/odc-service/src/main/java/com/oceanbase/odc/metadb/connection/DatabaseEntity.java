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
package com.oceanbase.odc.metadb.connection;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import com.oceanbase.odc.service.connection.database.model.DatabaseSyncStatus;
import com.oceanbase.odc.service.db.schema.model.DBObjectSyncStatus;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2023/4/19 20:36
 * @Description: []
 */
@Data
@Entity
@Table(name = "connect_database")
public class DatabaseEntity {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * actual id in specific database instance, probably fetched from dictionary tables
     */
    @Column(name = "database_id", nullable = false)
    private String databaseId;

    @Generated(GenerationTime.ALWAYS)
    @Column(name = "create_time", insertable = false, updatable = false)
    private Date createTime;

    @Generated(GenerationTime.ALWAYS)
    @Column(name = "update_time", insertable = false, updatable = false)
    private Date updateTime;

    @Column(name = "organization_id", updatable = false, nullable = false)
    private Long organizationId;

    @Column(name = "name", updatable = false, nullable = false)
    private String name;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "connection_id", updatable = false, nullable = false)
    private Long connectionId;

    @Column(name = "environment_id", nullable = false)
    private Long environmentId;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "sync_status", nullable = false)
    private DatabaseSyncStatus syncStatus;

    @Column(name = "last_sync_time", nullable = false)
    private Date lastSyncTime;

    @Column(name = "charset_name", nullable = false)
    private String charsetName;

    @Column(name = "collation_name", nullable = false)
    private String collationName;

    @Column(name = "table_count")
    private Long tableCount;

    /**
     * if this database actually existed in the datasource instance
     */
    @Column(name = "is_existed", nullable = false)
    private Boolean existed;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "object_sync_status", nullable = false)
    private DBObjectSyncStatus objectSyncStatus;

    @Column(name = "object_last_sync_time")
    private Date objectLastSyncTime;

}
