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
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import com.oceanbase.odc.service.flow.model.FlowNodeStatus;
import com.oceanbase.odc.service.iam.model.User;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Entity object for {@code UserTask}
 *
 * @author yh263208
 * @date 2022-02-07 15:13
 * @since ODC_release_3.3.0
 */
@Getter
@Setter
@ToString
@Entity
@EqualsAndHashCode(exclude = {"updateTime", "createTime"})
@Table(name = "flow_instance_node_approval")
public class UserTaskInstanceEntity {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private long organizationId;
    /**
     * Refer to {@link org.flowable.task.api.Task#getId()}
     */
    @Column(name = "user_task_id")
    private String userTaskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FlowNodeStatus status;

    /**
     * Refer to {@link User#getId()}
     */
    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "comment")
    private String comment;

    @Column(name = "approval_expire_interval_seconds", nullable = false)
    private Integer expireIntervalSeconds;
    /**
     * Flag to indicate the {@link UserTaskInstanceEntity} is approved
     */
    @Column(name = "is_approved", nullable = false)
    private boolean approved;

    @Column(name = "is_start_endpoint", nullable = false)
    private boolean startEndpoint;

    @Column(name = "is_end_endpoint", nullable = false)
    private boolean endEndpoint;
    /**
     * Refer to {@link FlowInstanceEntity#getId()}
     */
    @Column(name = "flow_instance_id", nullable = false)
    private long flowInstanceId;
    /**
     * Create time for a {@link UserTaskInstanceEntity}
     */
    @Generated(GenerationTime.ALWAYS)
    @Column(name = "create_time", insertable = false, updatable = false)
    private Date createTime;
    /**
     * Update time for a {@link UserTaskInstanceEntity}
     */
    @Generated(GenerationTime.ALWAYS)
    @Column(name = "update_time", insertable = false, updatable = false)
    private Date updateTime;

    @Column(name = "is_auto_approve", nullable = false)
    private boolean autoApprove;

    @Column(name = "wait_for_confirm")
    private Boolean waitForConfirm;

    @Column(name = "external_flow_instance_id")
    private String externalFlowInstanceId;

    @Column(name = "external_approval_id")
    private Long externalApprovalId;
}
