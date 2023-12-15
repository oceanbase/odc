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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

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
        JdbcTemplate jdbcTemplate = getJdbcTemplate();
        return jdbcTemplate.execute((ConnectionCallback<List<GateWayInstanceEntity>>) con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (GateWayInstanceEntity item : entities) {
                ps.setObject(1, item.getOrganizationId());
                ps.setObject(2, item.getStatus().name());
                ps.setObject(3, item.isStartEndpoint());
                ps.setObject(4, item.isEndEndpoint());
                ps.setObject(5, item.getFlowInstanceId());
                ps.addBatch();
            }
            ps.executeBatch();
            ResultSet resultSet = ps.getGeneratedKeys();
            int i = 0;
            while (resultSet.next()) {
                entities.get(i++).setId(getGeneratedId(resultSet));
            }
            return entities;
        });
    }

}
