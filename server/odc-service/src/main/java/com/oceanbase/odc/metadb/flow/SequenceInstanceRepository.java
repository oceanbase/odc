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

/**
 * Repository layer for {@link SequenceInstanceEntity}
 *
 * @author yh263208
 * @date 2022-02-09 14:28
 * @since ODC_release_3.3.0
 */
public interface SequenceInstanceRepository
        extends OdcJpaRepository<SequenceInstanceEntity, Long>, JpaSpecificationExecutor<SequenceInstanceEntity> {

    @Transactional
    @Query("delete from SequenceInstanceEntity as si where si.sourceNodeInstanceId=:nodeInstanceId or si.targetNodeInstanceId=:nodeInstanceId")
    @Modifying
    int deleteByNodeInstanceId(@Param("nodeInstanceId") Long nodeInstanceId);

    @Transactional
    @Query("delete from SequenceInstanceEntity as si where si.sourceNodeInstanceId=:sourceNodeInstanceId")
    @Modifying
    int deleteBySourceNodeInstanceId(@Param("sourceNodeInstanceId") Long sourceNodeInstanceId);

    int deleteBySourceNodeInstanceIdIn(Collection<Long> sourceNodeInstanceId);

    @Transactional
    @Query("delete from SequenceInstanceEntity as si where si.flowInstanceId=:instanceId")
    @Modifying
    int deleteByFlowInstanceId(@Param("instanceId") Long instanceId);

    @Query(value = "select * from flow_instance_sequence where flow_instance_id=:flowInstanceId", nativeQuery = true)
    List<SequenceInstanceEntity> findByFlowInstanceId(@Param("flowInstanceId") Long flowInstanceId);

    default List<SequenceInstanceEntity> batchCreate(List<SequenceInstanceEntity> entities) {
        String sql = InsertSqlTemplateBuilder.from("flow_instance_sequence")
                .field(SequenceInstanceEntity_.sourceNodeInstanceId)
                .field(SequenceInstanceEntity_.targetNodeInstanceId)
                .field(SequenceInstanceEntity_.flowInstanceId)
                .build();
        JdbcTemplate jdbcTemplate = getJdbcTemplate();
        return jdbcTemplate.execute((ConnectionCallback<List<SequenceInstanceEntity>>) con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (SequenceInstanceEntity item : entities) {
                ps.setObject(1, item.getSourceNodeInstanceId());
                ps.setObject(2, item.getTargetNodeInstanceId());
                ps.setObject(3, item.getFlowInstanceId());
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
