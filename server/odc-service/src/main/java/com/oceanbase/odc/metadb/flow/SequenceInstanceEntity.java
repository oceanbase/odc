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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * {@link SequenceInstanceEntity}
 *
 * @author yh263208
 * @date 2022-02-08 14:25
 * @since ODC_release_3.3.0
 */
@Getter
@Setter
@ToString
@Entity
@EqualsAndHashCode(exclude = {"updateTime", "createTime"})
@Table(name = "flow_instance_sequence")
public class SequenceInstanceEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * Refer to {@link NodeInstanceEntity#getId()}
     */
    @Column(name = "source_node_instance_id", nullable = false)
    private Long sourceNodeInstanceId;
    /**
     * Refer to {@link NodeInstanceEntity#getId()}
     */
    @Column(name = "target_node_instance_id", nullable = false)
    private Long targetNodeInstanceId;
    /**
     * Refer to {@link FlowInstanceEntity#getId()}
     */
    @Column(name = "flow_instance_id", nullable = false)
    private long flowInstanceId;
    /**
     * Create time for a {@link SequenceInstanceEntity}
     */
    @Generated(GenerationTime.ALWAYS)
    @Column(name = "create_time", insertable = false, updatable = false)
    private Date createTime;
    /**
     * Update time for a {@link SequenceInstanceEntity}
     */
    @Generated(GenerationTime.ALWAYS)
    @Column(name = "update_time", insertable = false, updatable = false)
    private Date updateTime;

}
