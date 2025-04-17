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
package com.oceanbase.odc.metadb.dlm;

import java.util.Date;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.tools.migrator.common.enums.JobType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

/**
 * @Author：tinker
 * @Date: 2024/5/13 20:49
 * @Descripition:
 */

@Entity
@Table(name = "dlm_table_unit", uniqueConstraints = @UniqueConstraint(columnNames = "dlm_table_unit_id"))
@Data
public class DlmTableUnitEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "schedule_task_id", nullable = false)
    private Long scheduleTaskId;

    @Column(name = "dlm_table_unit_id", nullable = false)
    private String dlmTableUnitId;

    @Column(name = "table_name", nullable = false)
    private String tableName;

    @Column(name = "target_table_name")
    private String targetTableName;

    @Column(name = "fire_time", nullable = false)
    private Date fireTime;

    @Column(name = "start_time", nullable = false)
    private Date startTime;

    @Column(name = "end_time", nullable = false)
    private Date endTime;

    @Column(name = "source_datasource_info", nullable = false)
    private String sourceDatasourceInfo;

    @Column(name = "target_datasource_info")
    private String targetDatasourceInfo;

    @Column(name = "statistic")
    private String statistic;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private JobType type;

    @Column(name = "parameters", nullable = false)
    private String parameters;

    @Generated(GenerationTime.ALWAYS)
    @Column(name = "create_time", insertable = false, updatable = false)
    private Date createTime;

    @Generated(GenerationTime.ALWAYS)
    @Column(name = "update_time", insertable = false, updatable = false)
    private Date updateTime;
}
