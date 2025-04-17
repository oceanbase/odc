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
package com.oceanbase.odc.metadb.connection.logicaldatabase;

import java.util.Date;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.ExecutionStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    @Column(name = "execution_order", nullable = false, updatable = false)
    private Long executionOrder;

    @Column(name = "schedule_task_id", nullable = false)
    private Long scheduleTaskId;

    @Column(name = "logical_database_id", nullable = false, updatable = false)
    private Long logicalDatabaseId;

    @Column(name = "physical_database_id", nullable = false, updatable = false)
    private Long physicalDatabaseId;

    @Column(name = "sql_content", nullable = false, updatable = false)
    private String sql;

    @Column(name = "execution_result_json", nullable = false)
    private String executionResultJson;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ExecutionStatus status;

    @Generated(GenerationTime.ALWAYS)
    @Column(name = "create_time", insertable = false, updatable = false)
    private Date createTime;

    @Generated(GenerationTime.ALWAYS)
    @Column(name = "update_time", insertable = false, updatable = false)
    private Date updateTime;
}
