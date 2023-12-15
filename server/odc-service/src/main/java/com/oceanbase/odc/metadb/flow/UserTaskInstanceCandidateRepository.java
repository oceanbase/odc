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
 * Repository layer for {@link UserTaskInstanceCandidateEntity}
 *
 * @author yh263208
 * @date 2022-02-07 16:47
 * @since ODC_release_3.3.0
 */
public interface UserTaskInstanceCandidateRepository extends OdcJpaRepository<UserTaskInstanceCandidateEntity, Long>,
        JpaSpecificationExecutor<UserTaskInstanceCandidateEntity> {

    List<UserTaskInstanceCandidateEntity> findByApprovalInstanceId(Long approvalInstanceId);

    @Query(value = "select * from flow_instance_node_approval_candidate where approval_instance_id "
            + "in (:approvalInstanceIds)", nativeQuery = true)
    List<UserTaskInstanceCandidateEntity> findByApprovalInstanceIds(
            @Param("approvalInstanceIds") Collection<Long> approvalInstanceIds);

    @Transactional
    @Query("delete from UserTaskInstanceCandidateEntity as uc where uc.approvalInstanceId=:approvalInstanceId")
    @Modifying
    int deleteByApprovalInstanceId(@Param("approvalInstanceId") Long approvalInstanceId);

    @Transactional
    @Query("delete from UserTaskInstanceCandidateEntity as uc where uc.approvalInstanceId=:approvalInstanceId and uc.userId=:candidate")
    @Modifying
    int deleteByApprovalInstanceIdAndUserId(@Param("approvalInstanceId") Long approvalInstanceId,
            @Param("candidate") Long candidate);

    @Transactional
    @Query("delete from UserTaskInstanceCandidateEntity as uc where uc.approvalInstanceId=:approvalInstanceId and uc.roleId in :candidates")
    @Modifying
    int deleteByApprovalInstanceIdAndRoleIds(@Param("approvalInstanceId") Long approvalInstanceId,
            @Param("candidates") Collection<Long> candidates);

    default List<UserTaskInstanceCandidateEntity> batchCreate(List<UserTaskInstanceCandidateEntity> entities) {
        String sql = InsertSqlTemplateBuilder.from("flow_instance_node_approval_candidate")
                .field(UserTaskInstanceCandidateEntity_.APPROVAL_INSTANCE_ID)
                .field(UserTaskInstanceCandidateEntity_.USER_ID)
                .field(UserTaskInstanceCandidateEntity_.ROLE_ID)
                .field(UserTaskInstanceCandidateEntity_.RESOURCE_ROLE_IDENTIFIER)
                .build();
        JdbcTemplate jdbcTemplate = getJdbcTemplate();
        return jdbcTemplate.execute((ConnectionCallback<List<UserTaskInstanceCandidateEntity>>) con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (UserTaskInstanceCandidateEntity e : entities) {
                ps.setLong(1, e.getApprovalInstanceId());
                ps.setLong(2, e.getUserId());
                ps.setLong(3, e.getRoleId());
                ps.setString(4, e.getResourceRoleIdentifier());
                ps.addBatch();
            }
            ps.executeBatch();
            ResultSet resultSet = ps.getGeneratedKeys();
            int i = 0;
            while (resultSet.next()) {
                UserTaskInstanceCandidateEntity entity = entities.get(i++);
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
