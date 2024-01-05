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

import java.io.Serializable;
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

import com.oceanbase.odc.service.task.enums.JobStatus;

import lombok.Data;

/**
 * @author yaobin
 * @date 2023-12-06
 * @since 4.2.4
 */
@Data
@Entity
@Table(name = "job_job")
public class JobEntity implements Serializable {

    private static final long serialVersionUID = 2744695847461276123L;

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_Class", nullable = false)
    private String jobClass;

    @Column(name = "job_type", nullable = false)
    private String jobType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private JobStatus status;

    @Column(name = "executor_endpoint")
    private String executorEndpoint;

    @Column(name = "execution_times", nullable = false)
    private Integer executionTimes;

    @Column(name = "executor_identifier")
    private String executorIdentifier;

    @Column(name = "run_mode")
    private String runMode;

    @Column(name = "job_parameters_json")
    private String jobParametersJson;

    @Column(name = "result_json")
    private String resultJson;

    @Column(name = "progress_percentage")
    private double progressPercentage;

    @Column(name = "description")
    private String description;

    @Column(name = "creator_id")
    private Long creatorId;

    @Column(name = "organization_id")
    private Long organizationId;

    @Column(name = "started_time")
    private Date startedTime;

    @Column(name = "finished_time")
    private Date finishedTime;

    @Column(name = "last_report_time")
    private Date lastReportTime;

    @Generated(GenerationTime.ALWAYS)
    @Column(name = "create_time", insertable = false, updatable = false)
    private Date createTime;

    @Generated(GenerationTime.ALWAYS)
    @Column(name = "update_time", insertable = false, updatable = false)
    private Date updateTime;

}
