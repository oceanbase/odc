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
package com.oceanbase.odc.metadb.partitionplan;

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
 * {@link PartitionPlanEntity}
 *
 * @author yh263208
 * @date 2024-01-10 16:43
 * @since ODC_release_4.2.4
 */
@Data
@Entity
@Table(name = "partitionplan")
public class PartitionPlanEntity {
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
    @Column(name = "flow_instance_id", nullable = false)
    private long flowInstanceId;
    /**
     * Enabled or not
     */
    @Column(name = "is_enabled", nullable = false)
    private Boolean enabled;
    /**
     * Creator id, references odc_user_info(id)
     */
    @Column(name = "creator_id", updatable = false, nullable = false)
    private Long creatorId;
    /**
     * Last modifier id, references iam_user(id)
     */
    @Column(name = "last_modifier_id", nullable = false)
    private Long lastModifierId;
    @Column(name = "database_id", nullable = false, updatable = false)
    private Long databaseId;
}
