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
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import com.oceanbase.odc.service.structurecompare.model.ComparisonResult;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

import lombok.Data;

/**
 * @author jingtian
 * @date 2024/1/16
 * @since ODC_release_4.2.4
 */
@Data
@Entity
@Table(name = "structure_comparison_task_result")
public class StructureComparisonTaskResultEntity {
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
     * Related structure comparison task id, refer to structure_comparison_task(id)
     */
    @Column(name = "structure_comparison_task_id", nullable = false)
    private long structureComparisonTaskId;
    /**
     * The type of the database object to be compared
     */
    @Enumerated(value = EnumType.STRING)
    @Column(name = "database_object_type", nullable = false)
    private DBObjectType databaseObjectType;
    /**
     * The source database object name to be compared
     */
    @Column(name = "database_object_name", nullable = false)
    private String databaseObjectName;
    /**
     * Structural analysis results
     */
    @Enumerated(value = EnumType.STRING)
    @Column(name = "comparing_result", nullable = false)
    private ComparisonResult comparingResult;
    /**
     * Source database object DDL
     */
    @Column(name = "source_database_object_ddl")
    private String sourceDatabaseObjectDdl;
    /**
     * Target database object DDL
     */
    @Column(name = "target_database_object_ddl")
    private String targetDatabaseObjectDdl;
    /**
     * Change sql script to convert target database object to source database object
     */
    @Column(name = "change_sql_script")
    private String changeSqlScript;
}
