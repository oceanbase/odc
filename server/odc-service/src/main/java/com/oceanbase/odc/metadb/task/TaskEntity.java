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
package com.oceanbase.odc.metadb.task;

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

import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.shared.constant.TaskType;

import lombok.Data;
import lombok.ToString;

/**
 * @author wenniu.ly
 * @date 2022/2/11
 */

@Data
@Entity
@ToString
@Table(name = "task_task")
public class TaskEntity {

    /**
     * Id for task
     */
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
     * Execution expire interval seconds
     */
    @Column(name = "execution_expire_interval_seconds", nullable = false)
    private Integer executionExpirationIntervalSeconds;

    /**
     * Organization id, references iam_organization(id)
     */
    @Column(name = "organization_id", updatable = false, nullable = false)
    private Long organizationId;

    /**
     * Task type, enum: ASYNC,IMPORT,EXPORT,MOCKDATA
     */
    @Enumerated(value = EnumType.STRING)
    @Column(name = "task_type", updatable = false, nullable = false)
    private TaskType taskType;

    /**
     * Connection id, references connect_connection(id)
     */
    @Column(name = "connection_id", updatable = false)
    private Long connectionId;

    /**
     * Database name which current task belongs to
     */
    @Column(name = "database_name")
    private String databaseName;

    /**
     * Description
     */
    @Column(name = "description")
    private String description;

    /**
     * Task parameters json string
     */
    @Column(name = "parameters_json")
    private String parametersJson;

    /**
     * Submitter messages for current task
     */
    @Column(name = "submitter")
    private String submitter;

    /**
     * Executor message for current task
     */
    @Column(name = "executor")
    private String executor;

    /**
     * Task status, enum: PREPARING,RUNNING,FAILED,CANCELED,DONE
     */
    @Enumerated(value = EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TaskStatus status;

    /**
     * Task progress
     */
    @Column(name = "progress_percentage")
    private double progressPercentage;

    /**
     * Task result by json string
     */
    @Column(name = "result_json")
    private String resultJson;

    /**
     * Risk level id, refer to regulation_risklevel.id
     */
    @Column(name = "risk_level_id")
    private Long riskLevelId;

    /**
     * Database id, refer to connect_database.id
     */
    @Column(name = "database_id")
    private Long databaseId;


    @Column(name = "job_id")
    private Long jobId;

}
