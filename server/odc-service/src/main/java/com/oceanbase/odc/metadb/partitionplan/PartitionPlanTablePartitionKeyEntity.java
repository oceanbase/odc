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
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import com.oceanbase.odc.service.partitionplan.model.PartitionPlanStrategy;

import lombok.Data;

/**
 * {@link PartitionPlanTablePartitionKeyEntity}
 *
 * @author yh263208
 * @date 2024-01-10 16:51
 * @since ODC_release_4.2.4
 */
@Data
@Entity
@Table(name = "partitionplan_table_partitionkey")
public class PartitionPlanTablePartitionKeyEntity {
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
    @Column(name = "partition_key", nullable = false)
    private String partitionKey;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "strategy", nullable = false, updatable = false)
    private PartitionPlanStrategy strategy;
    @Column(name = "partitionplan_table_id", nullable = false)
    private long partitionplanTableId;
    /**
     * Enabled or not
     */
    @Column(name = "partition_key_invoker", nullable = false)
    private Boolean partitionKeyInvoker;
    @Column(name = "partition_key_invoker_parameters", nullable = false)
    private String partitionKeyInvokerParameters;
}
