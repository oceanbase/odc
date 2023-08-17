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
package com.oceanbase.odc.metadb.schedule;

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

import com.oceanbase.odc.service.quartz.model.MisfireStrategy;
import com.oceanbase.odc.service.schedule.model.JobType;
import com.oceanbase.odc.service.schedule.model.ScheduleStatus;

import lombok.Data;

/**
 * @Author：tinker
 * @Date: 2022/11/14 18:35
 * @Descripition:
 */

@Data
@Entity
@Table(name = "schedule_schedule")
public class ScheduleEntity implements Serializable {

    private static final long serialVersionUID = 2744695847461276123L;

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;
    @Column(name = "connection_id", nullable = false)
    private Long connectionId;
    @Column(name = "database_name", nullable = false)
    private String databaseName;
    @Column(name = "database_id", nullable = false)
    private Long databaseId;
    @Column(name = "project_id")
    private Long projectId;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ScheduleStatus status;
    @Column(name = "allow_concurrent", nullable = false)
    private Boolean allowConcurrent;
    @Enumerated(EnumType.STRING)
    @Column(name = "misfire_strategy", nullable = false)
    private MisfireStrategy misfireStrategy;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false)
    private JobType jobType;
    @Column(name = "job_parameters_json", nullable = false)
    private String jobParametersJson;
    @Column(name = "trigger_config_json", nullable = false)
    private String triggerConfigJson;

    @Column(name = "description")
    private String description;

    @Generated(GenerationTime.ALWAYS)
    @Column(name = "create_time", insertable = false, updatable = false)
    private Date createTime;
    @Generated(GenerationTime.ALWAYS)
    @Column(name = "update_time", insertable = false, updatable = false)
    private Date updateTime;
    @Column(name = "creator_id", updatable = false)
    private Long creatorId;
    @Column(name = "modifier_id")
    private Long modifierId;

}
