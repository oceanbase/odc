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

import lombok.Data;

/**
 * ClassName: TableEntity.java Package: com.oceanbase.odc.metadb.connection Description:
 *
 * @Author: fenghao
 * @Create 2024/3/11 20:26
 * @Version 1.0
 */
@Data
@Entity
@Table(name = "connect_table")
public class TableEntity {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * actual id in specific database instance, probably fetched from dictionary tables
     */
    @Column(name = "database_id", nullable = false)
    private Long databaseId;

    @Column(name = "name", updatable = false, nullable = false)
    private String name;

    @Generated(GenerationTime.ALWAYS)
    @Column(name = "create_time", insertable = false, updatable = false)
    private Date createTime;

    @Generated(GenerationTime.ALWAYS)
    @Column(name = "update_time", insertable = false, updatable = false)
    private Date updateTime;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "sync_status", nullable = false)
    private DatabaseSyncStatus syncStatus;
}
