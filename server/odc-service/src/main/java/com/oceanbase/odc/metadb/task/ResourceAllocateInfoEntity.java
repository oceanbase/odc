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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import lombok.Data;

/**
 * @author longpeng.zlp
 * @date 2024/12/4 16:48
 */
@Data
@Entity
@Table(name = "resource_allocate_info")
public class ResourceAllocateInfoEntity {

    /**
     * Id for supervisor endpoint
     */
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * task id relate to job_job
     */
    @Column(name = "task_id", nullable = false)
    private Long taskId;

    /**
     * resource allocate state, update by resource allocator, including PREPARING, AVAILABLE, FAILED
     */
    @Column(name = "resource_allocate_state", nullable = false)
    private String resourceAllocateState;

    /**
     * resource usage state update by resource user, including PREPARING, USING, FINISHED
     */
    @Column(name = "resource_usage_state", nullable = false)
    private String resourceUsageState;

    /**
     * supervisor endpoint, in format host:port
     */
    @Column(name = "endpoint")
    private String endpoint;

    /**
     * resource id associate to supervisor endpoint resource id
     */
    @Column(name = "resource_id")
    private Long resourceId;

    /**
     * resource region to find
     */
    @Column(name = "resource_region", nullable = false)
    private String resourceRegion;

    /**
     * resource group to find
     */
    @Column(name = "resource_group", nullable = false)
    private String resourceGroup;

    /**
     * resource type
     */
    @Column(name = "resource_applier_name", nullable = false)
    private String resourceApplierName;

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
}
