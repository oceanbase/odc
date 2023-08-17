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
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.flowable.engine.repository.ProcessDefinition;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.Immutable;

import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.constant.TaskType;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Query flow_instance with task_type by create view use flow_instance a left join
 * flow_instance_node_task b on a.id = b.flow_instance_id {@link FlowInstanceEntity}
 * {@link ServiceTaskInstanceEntity}
 *
 * @author jingtian
 * @date 2023/8/10
 * @since ODC_release_4.2.0
 */
@Getter
@Entity
@EqualsAndHashCode(exclude = {"updateTime", "createTime"})
@ToString(exclude = "flowConfigSnapshotXml")
@Immutable
@Table(name = "list_flow_instance_view")
public class FlowInstanceViewEntity {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_instance_id")
    private Long parentInstanceId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "flow_config_id")
    private long flowConfigId;

    @Column(name = "creator_id", nullable = false)
    private long creatorId;

    @Column(name = "organization_id", nullable = false)
    private long organizationId;
    /**
     * Refer to {@link ProcessDefinition#getId()}
     */
    @Column(name = "process_definition_id", nullable = false)
    private String processDefinitionId;
    /**
     * Refer to {@link org.flowable.engine.runtime.ProcessInstance#getId()}
     */
    @Column(name = "process_instance_id", nullable = false)
    private String processInstanceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FlowStatus status;

    @Column(name = "flow_config_snapshot_xml", nullable = false)
    private String flowConfigSnapshotXml;

    @Column(name = "description")
    private String description;
    /**
     * Create time for a {@link FlowInstanceEntity}
     */
    @Generated(GenerationTime.ALWAYS)
    @Column(name = "create_time", insertable = false, updatable = false)
    private Date createTime;
    /**
     * Update time for a {@link FlowInstanceEntity}
     */
    @Generated(GenerationTime.ALWAYS)
    @Column(name = "update_time", insertable = false, updatable = false)
    private Date updateTime;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "task_type", updatable = false, nullable = false)
    private TaskType taskType;

    @OneToMany
    @JoinColumn(name = "flow_instance_id")
    private List<FlowInstanceApprovalViewEntity> approvals;

}
