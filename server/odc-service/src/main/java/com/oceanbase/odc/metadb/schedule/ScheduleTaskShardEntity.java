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

import com.oceanbase.odc.service.schedule.model.JobType;

import lombok.Data;

/**
 * @Author：tinker
 * @Date: 2024/4/28 16:34
 * @Descripition:
 */
@Data
@Entity
@Table(name = "schedule_task_shard")
public class ScheduleTaskShardEntity {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "schedule_task_id", nullable = false)
    private Long scheduleTaskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private JobType type;

    @Column(name = "task_parameters", nullable = false)
    private String taskParameters;

    @Column(name = "execution_detail", nullable = false)
    private String executionDetail;

    @Column(name = "start_time")
    private Date startTime;
    @Column(name = "end_time")
    private Date endTime;

    @Column(name = "status", nullable = false)
    private String status;

    @Generated(GenerationTime.ALWAYS)
    @Column(name = "create_time", insertable = false, updatable = false)
    private Date createTime;

    @Generated(GenerationTime.ALWAYS)
    @Column(name = "update_time", insertable = false, updatable = false)
    private Date updateTime;

}
