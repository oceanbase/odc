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

package com.oceanbase.odc.metadb.flow;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Immutable;

import com.oceanbase.odc.service.flow.model.FlowNodeStatus;

import lombok.Getter;
import lombok.ToString;

/**
 * {@link UserTaskInstanceEntity} {@link UserTaskInstanceCandidateEntity}
 *
 * @author jingtian
 * @date 2023/8/11
 * @since ODC_release_4.2.0
 */
@Getter
@ToString
@Entity
@Immutable
@Table(name = "flow_instance_approval_view")
public class FlowInstanceApprovalViewEntity {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * Refer to {@link FlowInstanceEntity#getId()}
     */
    @Column(name = "flow_instance_id", nullable = false)
    private long flowInstanceId;
    /**
     * Refer to {@link com.oceanbase.odc.metadb.iam.resourcerole.ResourceRoleEntity#getId()}
     */
    @Column(name = "resource_role_identifier")
    private String resourceRoleIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FlowNodeStatus status;
}
