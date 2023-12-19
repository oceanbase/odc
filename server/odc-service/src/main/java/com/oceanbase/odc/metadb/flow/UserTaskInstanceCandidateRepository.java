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
import java.util.function.Function;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
                .field(UserTaskInstanceCandidateEntity_.approvalInstanceId)
                .field(UserTaskInstanceCandidateEntity_.userId)
                .field(UserTaskInstanceCandidateEntity_.roleId)
                .field(UserTaskInstanceCandidateEntity_.resourceRoleIdentifier)
                .build();

        List<Function<UserTaskInstanceCandidateEntity, Object>> getter = valueGetterBuilder().add(
                UserTaskInstanceCandidateEntity::getApprovalInstanceId)
                .add(UserTaskInstanceCandidateEntity::getUserId)
                .add(UserTaskInstanceCandidateEntity::getRoleId)
                .add(UserTaskInstanceCandidateEntity::getResourceRoleIdentifier)
                .build();

        return batchCreate(entities, sql, getter, UserTaskInstanceCandidateEntity::setId);
    }

}
