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

import com.oceanbase.odc.service.partitionplan.model.PeriodUnit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author：tianke
 * @Date: 2022/9/16 16:41
 * @Descripition:
 */

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "table_partition_plan")
public class TablePartitionPlanEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "connection_id", nullable = false, updatable = false)
    private Long connectionId;
    @Column(name = "database_id", nullable = false, updatable = false)
    private Long databaseId;
    @Column(name = "database_partition_plan_id", updatable = false)
    private Long databasePartitionPlanId;
    @Column(name = "flow_instance_id", nullable = false, updatable = false)
    private Long flowInstanceId;
    @Column(name = "schema_name", nullable = false, updatable = false)
    private String schemaName;
    @Column(name = "table_name", nullable = false, updatable = false)
    private String tableName;
    @Column(name = "organization_id", nullable = false, updatable = false)
    private Long organizationId;

    @Column(name = "is_config_enabled", nullable = false)
    private Boolean isConfigEnable;
    @Column(name = "is_auto_partition", nullable = false)
    private Boolean isAutoPartition;
    @Column(name = "pre_create_partition_count", nullable = false)
    private Integer preCreatePartitionCount;
    @Column(name = "expire_period", nullable = false)
    private Integer expirePeriod;
    @Column(name = "expire_period_unit", nullable = false)
    private PeriodUnit expirePeriodUnit;
    @Column(name = "partition_interval", nullable = false)
    private Integer partitionInterval;
    @Column(name = "partition_interval_unit", nullable = false)
    private PeriodUnit partitionIntervalUnit;
    @Column(name = "partition_naming_prefix", nullable = false)
    private String partitionNamingPrefix;
    @Column(name = "partition_naming_suffix_expression", nullable = false)
    private String partitionNamingSuffixExpression;

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
