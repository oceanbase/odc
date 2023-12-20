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

import com.oceanbase.odc.core.shared.constant.TaskStatus;

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

    @Column(name = "flow_instance_id", nullable = false)
    private Long flowInstanceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TaskStatus status;

    @Column(name = "executor")
    private String executor;

    @Column(name = "schedule_times", nullable = false)
    private Integer scheduleTimes;

    @Column(name = "execution_times", nullable = false)
    private Integer executionTimes;

    @Column(name = "job_name", nullable = false)
    private String jobName;

    @Column(name = "run_mode", nullable = false)
    private String runMode;

    @Column(name = "job_data_json", nullable = false)
    private String jobDataJson;

    @Column(name = "trigger_config_json", nullable = false)
    private String triggerConfigJson;

    @Column(name = "result_json")
    private String resultJson;

    @Column(name = "progress_percentage")
    private double progressPercentage;

    @Column(name = "description")
    private String description;

    @Column(name = "creator_id", updatable = false)
    private Long creatorId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Generated(GenerationTime.ALWAYS)
    @Column(name = "create_time", insertable = false, updatable = false)
    private Date createTime;

    @Generated(GenerationTime.ALWAYS)
    @Column(name = "update_time", insertable = false, updatable = false)
    private Date updateTime;
}
