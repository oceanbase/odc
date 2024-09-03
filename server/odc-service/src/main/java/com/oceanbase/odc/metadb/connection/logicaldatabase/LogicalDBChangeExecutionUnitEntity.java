/*
 * Copyright (c) 2024 OceanBase.
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

package com.oceanbase.odc.metadb.connection.logicaldatabase;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import com.oceanbase.odc.service.session.model.SqlExecuteResult;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2024/9/3 19:56
 * @Description: []
 */
@Data
@Entity
@Table(name = "logicaldatabase_database_change_execution_unit")
public class LogicalDBChangeExecutionUnitEntity {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execution_id", nullable = false, updatable = false)
    private String executionId;

    @Column(name = "logical_database_id", nullable = false, updatable = false)
    private Long logicalDatabaseId;

    @Column(name = "physical_database_id", nullable = false, updatable = false)
    private Long physicalDatabaseId;

    @Column(name = "sql", nullable = false, updatable = false)
    private String sql;

    @Column(name = "execution_result_json", nullable = false)
    private String executionResultJson;

    @Generated(GenerationTime.ALWAYS)
    @Column(name = "create_time", insertable = false, updatable = false)
    private Date createTime;

    @Generated(GenerationTime.ALWAYS)
    @Column(name = "update_time", insertable = false, updatable = false)
    private Date updateTime;
}
