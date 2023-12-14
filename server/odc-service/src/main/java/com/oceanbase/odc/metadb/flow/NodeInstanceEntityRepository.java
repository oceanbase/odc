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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

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

    JdbcTemplate getJdbcTemplate();

    default List<NodeInstanceEntity> bulkSave(List<NodeInstanceEntity> entities) {
        String psSql = "insert into flow_instance_node(instance_id,instance_type,"
                + "activity_id,name,flowable_element_type,flow_instance_id) values(?,?,?,?,?,?)";
        JdbcTemplate jdbcTemplate = getJdbcTemplate();
        return jdbcTemplate.execute((ConnectionCallback<List<NodeInstanceEntity>>) con -> {
            PreparedStatement ps = con.prepareStatement(psSql, Statement.RETURN_GENERATED_KEYS);
            for (NodeInstanceEntity item : entities) {
                ps.setLong(1, item.getInstanceId());
                ps.setString(2, item.getInstanceType().name());
                ps.setString(3, item.getActivityId());
                ps.setString(4, item.getName());
                ps.setString(5, item.getFlowableElementType().name());
                ps.setLong(6, item.getFlowInstanceId());
                ps.addBatch();
            }
            ps.executeBatch();
            ResultSet resultSet = ps.getGeneratedKeys();
            int i = 0;
            while (resultSet.next()) {
                NodeInstanceEntity entity = entities.get(i++);
                if (resultSet.getObject("id") != null) {
                    entity.setId(Long.valueOf(resultSet.getObject("id").toString()));
                } else if (resultSet.getObject("ID") != null) {
                    entity.setId(Long.valueOf(resultSet.getObject("ID").toString()));
                } else if (resultSet.getObject("GENERATED_KEY") != null) {
                    entity.setId(Long.valueOf(resultSet.getObject("GENERATED_KEY").toString()));
                }
            }
            return entities;
        });
    }

}
