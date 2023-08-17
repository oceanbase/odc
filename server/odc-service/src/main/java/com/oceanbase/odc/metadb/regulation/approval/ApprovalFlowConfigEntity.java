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
package com.oceanbase.odc.metadb.regulation.approval;

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
 * @Author: Lebie
 * @Date: 2023/6/14 20:24
 * @Description: []
 */
@Data
@Entity
@Table(name = "regulation_approval_flow_config")
public class ApprovalFlowConfigEntity {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    /**
     * Approval expire interval seconds
     */
    @Column(name = "approval_expire_interval_seconds", nullable = false)
    private Integer approvalExpirationIntervalSeconds;

    /**
     * Approval expire interval seconds
     */
    @Column(name = "wait_execution_expire_interval_seconds", nullable = false)
    private Integer waitExecutionExpirationIntervalSeconds;

    /**
     * Approval expire interval seconds
     */
    @Column(name = "execution_expire_interval_seconds", nullable = false)
    private Integer executionExpirationIntervalSeconds;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private Long organizationId;

    @Column(name = "is_builtin", nullable = false)
    private Boolean builtIn;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Generated(GenerationTime.ALWAYS)
    @Column(name = "create_time", insertable = false, updatable = false)
    private Date createTime;

    @Generated(GenerationTime.ALWAYS)
    @Column(name = "update_time", insertable = false, updatable = false)
    private Date updateTime;
}
