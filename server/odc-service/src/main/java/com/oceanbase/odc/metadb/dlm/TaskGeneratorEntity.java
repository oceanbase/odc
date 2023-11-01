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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import lombok.Data;

/**
 * @Author：tinker
 * @Date: 2023/8/10 14:48
 * @Descripition:
 */
@Entity
@Data
@Table(name = "dlm_task_generator")
public class TaskGeneratorEntity {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "generator_id", nullable = false, updatable = false)
    private String generatorId;

    @Column(name = "job_id", nullable = false, updatable = false)
    private String jobId;

    @Column(name = "processed_data_size", nullable = false)
    private Long processedDataSize;

    @Column(name = "processed_row_count", nullable = false)
    private Long processedRowCount;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "task_count", nullable = false)
    private Integer taskCount;

    @Column(name = "primary_key_save_point")
    private String primaryKeySavePoint;

    @Column(name = "partition_save_point")
    private String partitionSavePoint;

    @Generated(GenerationTime.ALWAYS)
    @Column(name = "create_time", insertable = false, updatable = false)
    private Date createTime;

    @Generated(GenerationTime.ALWAYS)
    @Column(name = "update_time", insertable = false, updatable = false)
    private Date updateTime;
}
