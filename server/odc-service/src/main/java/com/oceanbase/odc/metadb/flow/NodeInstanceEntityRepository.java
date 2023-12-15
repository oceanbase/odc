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
import java.util.Collection;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.odc.common.jpa.InsertSqlTemplateBuilder;
import com.oceanbase.odc.config.jpa.OdcJpaRepository;
import com.oceanbase.odc.core.flow.model.FlowableElementType;
import com.oceanbase.odc.service.flow.model.FlowNodeType;

/**
 * Repository layer for {@link NodeInstanceEntity}
 *
 * @author yh263208
 * @date 2022-02-09 14:18
 * @since ODC_release_3.3.0
 */
public interface NodeInstanceEntityRepository extends OdcJpaRepository<NodeInstanceEntity, Long>,
        JpaSpecificationExecutor<NodeInstanceEntity> {

    @Query(value = "select * from flow_instance_node where instance_id=:instanceId and instance_type=:#{#instanceType.name()} and "
            + "flowable_element_type=:#{#flowableElementType.name()}", nativeQuery = true)
    List<NodeInstanceEntity> findByInstanceIdAndInstanceTypeAndFlowableElementType(@Param("instanceId") Long instanceId,
            @Param("instanceType") FlowNodeType instanceType,
            @Param("flowableElementType") FlowableElementType flowableElementType);

    List<NodeInstanceEntity> findByFlowInstanceId(Long flowInstanceId);

    List<NodeInstanceEntity> findByInstanceIdIn(Collection<Long> instanceId);

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

    default List<NodeInstanceEntity> batchCreate(List<NodeInstanceEntity> entities) {
        String sql = InsertSqlTemplateBuilder.from("flow_instance_node")
                .field(NodeInstanceEntity_.instanceId)
                .field(NodeInstanceEntity_.instanceType)
                .field(NodeInstanceEntity_.flowInstanceId)
                .field(NodeInstanceEntity_.activityId)
                .field(NodeInstanceEntity_.name)
                .field(NodeInstanceEntity_.flowableElementType)
                .build();
        JdbcTemplate jdbcTemplate = getJdbcTemplate();
        return jdbcTemplate.execute((ConnectionCallback<List<NodeInstanceEntity>>) con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (NodeInstanceEntity item : entities) {
                ps.setObject(1, item.getInstanceId());
                ps.setObject(2, item.getInstanceType().name());
                ps.setObject(3, item.getFlowInstanceId());
                ps.setObject(4, item.getActivityId());
                ps.setObject(5, item.getName());
                ps.setObject(6, item.getFlowableElementType().name());
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
