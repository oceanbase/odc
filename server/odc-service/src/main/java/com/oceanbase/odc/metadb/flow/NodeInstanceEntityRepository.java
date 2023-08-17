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

import java.util.Collection;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.oceanbase.odc.core.flow.model.FlowableElementType;
import com.oceanbase.odc.service.flow.model.FlowNodeType;

/**
 * Repository layer for {@link NodeInstanceEntity}
 *
 * @author yh263208
 * @date 2022-02-09 14:18
 * @since ODC_release_3.3.0
 */
public interface NodeInstanceEntityRepository extends JpaRepository<NodeInstanceEntity, Long>,
        JpaSpecificationExecutor<NodeInstanceEntity> {

    @Query(value = "select * from flow_instance_node where instance_id=:instanceId and instance_type=:#{#instanceType.name()} and "
            + "flowable_element_type=:#{#flowableElementType.name()}", nativeQuery = true)
    List<NodeInstanceEntity> findByInstanceIdAndInstanceTypeAndFlowableElementType(@Param("instanceId") Long instanceId,
            @Param("instanceType") FlowNodeType instanceType,
            @Param("flowableElementType") FlowableElementType flowableElementType);

    List<NodeInstanceEntity> findByFlowInstanceId(Long flowInstanceId);

    @Query(value = "select * from flow_instance_node where id in (:ids)", nativeQuery = true)
    List<NodeInstanceEntity> findByIds(@Param("ids") Collection<Long> ids);

    @Transactional
    @Query("delete from NodeInstanceEntity as ni where ni.flowInstanceId=:instanceId")
    @Modifying
    int deleteByFlowInstanceId(@Param("instanceId") Long instanceId);

    @Transactional
    @Query("delete from NodeInstanceEntity as ni where ni.instanceId=:instanceId and ni.instanceType=:instanceType")
    @Modifying
    int deleteByInstanceIdAndInstanceType(@Param("instanceId") Long instanceId,
            @Param("instanceType") FlowNodeType instanceType);

}
