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

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.oceanbase.odc.common.jpa.InsertSqlTemplateBuilder;
import com.oceanbase.odc.config.jpa.OdcJpaRepository;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;
import com.oceanbase.odc.service.flow.model.FlowNodeType;

/**
 * Repository layer for {@link GateWayInstanceEntity}
 *
 * @author yh263208
 * @date 2022-02-08 20:47
 * @since ODC_release_3.3.0
 */
public interface GateWayInstanceRepository
        extends OdcJpaRepository<GateWayInstanceEntity, Long>, JpaSpecificationExecutor<GateWayInstanceEntity> {

    @Transactional
    @Query("delete from GateWayInstanceEntity as ut where ut.flowInstanceId=:instanceId")
    @Modifying
    int deleteByFlowInstanceId(@Param("instanceId") Long instanceId);

    @Transactional
    @Query(value = "update flow_instance_node_gateway set status=:#{#status.name()} where id=:id", nativeQuery = true)
    @Modifying
    int updateStatusById(@Param("id") Long id, @Param("status") FlowNodeStatus status);

    @Query(value = "select na.* from flow_instance_node_gateway as na inner join flow_instance_node as n on na.id=n.instance_id "
            + "where n.instance_type=:#{#instanceType.name()} and n.activity_id=:activityId and n.flow_instance_id=:flowInstanceId",
            nativeQuery = true)
    Optional<GateWayInstanceEntity> findByInstanceTypeAndActivityId(@Param("instanceType") FlowNodeType instanceType,
            @Param("activityId") String activityId, @Param("flowInstanceId") Long flowInstanceId);

    @Query(value = "select na.* from flow_instance_node_gateway as na inner join flow_instance_node as n on na.id=n.instance_id "
            + "where n.instance_type=:#{#instanceType.name()} and n.name=:name and n.flow_instance_id=:flowInstanceId",
            nativeQuery = true)
    Optional<GateWayInstanceEntity> findByInstanceTypeAndName(@Param("instanceType") FlowNodeType instanceType,
            @Param("name") String name, @Param("flowInstanceId") Long flowInstanceId);

    default List<GateWayInstanceEntity> batchCreate(List<GateWayInstanceEntity> entities) {
        String sql = InsertSqlTemplateBuilder.from("flow_instance_node_gateway")
                .field(GateWayInstanceEntity_.organizationId)
                .field(GateWayInstanceEntity_.status)
                .field("is_start_endpoint")
                .field("is_end_endpoint")
                .field(GateWayInstanceEntity_.flowInstanceId)
                .build();

        List<Function<GateWayInstanceEntity, Object>> getter = valueGetterBuilder().add(
                GateWayInstanceEntity::getOrganizationId)
                .add((GateWayInstanceEntity e) -> e.getStatus().name())
                .add(GateWayInstanceEntity::isStartEndpoint)
                .add(GateWayInstanceEntity::isEndEndpoint)
                .add(GateWayInstanceEntity::getFlowInstanceId)
                .build();

        return batchCreate(entities, sql, getter, GateWayInstanceEntity::setId);
    }

}
