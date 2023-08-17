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

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import com.oceanbase.odc.service.iam.model.Role;
import com.oceanbase.odc.service.iam.model.User;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Connections between {@link UserTaskInstanceEntity} and
 * {@link com.oceanbase.odc.service.iam.model.User} or
 * {@link com.oceanbase.odc.service.iam.model.Role}
 *
 * @author yh263208
 * @date 2022-02-07 16:40
 * @since ODC_release_3.3.0
 */
@Getter
@Setter
@ToString
@Entity
@EqualsAndHashCode(exclude = {"updateTime", "createTime"})
@Table(name = "flow_instance_node_approval_candidate")
public class UserTaskInstanceCandidateEntity {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * Refer to {@link UserTaskInstanceEntity#getId()}
     */
    @Column(name = "approval_instance_id", nullable = false)
    private long approvalInstanceId;
    /**
     * Refer to {@link User#getId()}
     */
    @Column(name = "user_id")
    private Long userId;
    /**
     * Refer to {@link Role#getId()}
     */
    @Column(name = "role_id")
    private Long roleId;
    /**
     * Refer to {@link com.oceanbase.odc.metadb.iam.resourcerole.ResourceRoleEntity#getId()}
     */
    @Column(name = "resource_role_identifier")
    private String resourceRoleIdentifier;
    /**
     * Create time for a {@link UserTaskInstanceCandidateEntity}
     */
    @Generated(GenerationTime.ALWAYS)
    @Column(name = "create_time", insertable = false, updatable = false)
    private Date createTime;
    /**
     * Update time for a {@link UserTaskInstanceCandidateEntity}
     */
    @Generated(GenerationTime.ALWAYS)
    @Column(name = "update_time", insertable = false, updatable = false)
    private Date updateTime;
}
