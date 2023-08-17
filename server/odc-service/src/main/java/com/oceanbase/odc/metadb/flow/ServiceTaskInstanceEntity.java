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

import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;
import com.oceanbase.odc.service.flow.model.FlowTaskExecutionStrategy;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Entity object for {@link org.flowable.bpmn.model.ServiceTask}
 *
 * @author yh263208
 * @date 2022-02-15 11:39
 * @since ODC_release_3.3.0
 */
@Getter
@Setter
@ToString
@Entity
@EqualsAndHashCode(exclude = {"updateTime", "createTime"})
@Table(name = "flow_instance_node_task")
public class ServiceTaskInstanceEntity {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;
    /**
     * Refer to {@link TaskEntity#getId()}
     */
    @Column(name = "task_task_id")
    private Long targetTaskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_execution_strategy", nullable = false)
    private FlowTaskExecutionStrategy strategy;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "task_type", updatable = false, nullable = false)
    private TaskType taskType;

    @Column(name = "wait_execution_expire_interval_seconds", nullable = false, updatable = false)
    private Integer waitExecExpireIntervalSeconds;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FlowNodeStatus status;

    @Column(name = "is_start_endpoint", nullable = false)
    private boolean startEndpoint;

    @Column(name = "is_end_endpoint", nullable = false)
    private boolean endEndpoint;
    /**
     * Refer to {@link FlowInstanceEntity#getId()}
     */
    @Column(name = "flow_instance_id", nullable = false)
    private Long flowInstanceId;
    /**
     * Create time for a {@link ServiceTaskInstanceEntity}
     */
    @Generated(GenerationTime.ALWAYS)
    @Column(name = "create_time", insertable = false, updatable = false)
    private Date createTime;
    /**
     * Update time for a {@link ServiceTaskInstanceEntity}
     */
    @Generated(GenerationTime.ALWAYS)
    @Column(name = "update_time", insertable = false, updatable = false)
    private Date updateTime;
    /**
     * Timing execution time
     */
    @Column(name = "execution_time")
    private Date executionTime;
}
