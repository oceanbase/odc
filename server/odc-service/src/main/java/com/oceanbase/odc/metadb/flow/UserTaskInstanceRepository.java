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
import java.util.Optional;
import java.util.stream.Collectors;

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
 * Repository layer for {@link UserTaskInstanceEntity}
 *
 * @author yh263208
 * @date 2022-02-07 16:12
 * @since ODC_release_3.3.0
 */
public interface UserTaskInstanceRepository
        extends OdcJpaRepository<UserTaskInstanceEntity, Long>, JpaSpecificationExecutor<UserTaskInstanceEntity> {

    List<UserTaskInstanceEntity> findByStatus(FlowNodeStatus status);

    @Query(value = "SELECT distinct(fai.*) FROM flow_instance_node_approval fai INNER JOIN flow_instance_node_approval_candidate faci ON "
            + "fai.id=faci.approval_instance_id WHERE faci.user_id=:userId or faci.role_id in (:roleIds)",
            nativeQuery = true)
    List<UserTaskInstanceEntity> findByCandidateUserIdOrRoleIds(@Param("userId") Long userId,
            @Param("roleIds") Collection<Long> roleIds);

    @Query(value = "SELECT distinct(fai.*) FROM flow_instance_node_approval fai INNER JOIN flow_instance_node_approval_candidate faci ON "
            + "fai.id=faci.approval_instance_id WHERE faci.user_id=:userId or faci.resource_role_identifier in (:resourceRoleIdentifiers)",
            nativeQuery = true)
    List<UserTaskInstanceEntity> findByCandidateUserIdOrResourceRoleIdentifier(@Param("userId") Long userId,
            @Param("resourceRoleIdentifiers") Collection<String> resourceRoleIdentifiers);

    @Query(value = "SELECT distinct(fai.*) FROM flow_instance_node_approval fai INNER JOIN flow_instance_node_approval_candidate faci ON "
            + "fai.id=faci.approval_instance_id WHERE faci.resource_role_identifier in (:resourceRoleIdentifier)",
            nativeQuery = true)
    List<UserTaskInstanceEntity> findByResourceRoleIdentifierIn(
            @Param("resourceRoleIdentifier") Collection<String> resourceRoleIdentifier);

    @Query(value = "SELECT distinct(fai.*) FROM flow_instance_node_approval fai INNER JOIN flow_instance_node_approval_candidate faci ON "
            + "fai.id=faci.approval_instance_id WHERE faci.role_id=:roleId",
            nativeQuery = true)
    List<UserTaskInstanceEntity> findByRoleId(@Param("roleId") Long roleId);

    @Query(value = "SELECT distinct(fai.*) FROM flow_instance_node_approval fai INNER JOIN flow_instance_node_approval_candidate faci "
            + "ON fai.id=faci.approval_instance_id WHERE faci.user_id=:userId", nativeQuery = true)
    List<UserTaskInstanceEntity> findByCandidateUserId(@Param("userId") Long userId);

    @Transactional
    @Query("update UserTaskInstanceEntity as ut set ut.userTaskId=:#{#param.userTaskId},ut.status=:#{#param.status},"
            + "ut.approved=:#{#param.approved},ut.operatorId=:#{#param.operatorId},ut.comment=:#{#param.comment},"
            + "ut.expireIntervalSeconds=:#{#param.expireIntervalSeconds},ut.externalFlowInstanceId=:#{#param.externalFlowInstanceId}"
            + " where ut.id=:#{#param.id}")
    @Modifying
    int update(@Param("param") UserTaskInstanceEntity entity);

    @Transactional
    @Query(value = "update flow_instance_node_approval set user_task_id=:userTaskId where id=:id", nativeQuery = true)
    @Modifying
    int updateUserTaskIdById(@Param("id") Long id, @Param("userTaskId") String userTaskId);

    @Transactional
    @Query(value = "update flow_instance_node_approval set status=:#{#status.name()} where id=:id", nativeQuery = true)
    @Modifying
    int updateStatusById(@Param("id") Long id, @Param("status") FlowNodeStatus status);

    @Transactional
    @Query("delete from UserTaskInstanceEntity as ut where ut.flowInstanceId=:instanceId")
    @Modifying
    int deleteByFlowInstanceId(@Param("instanceId") Long instanceId);

    @Query(value = "select na.* from flow_instance_node_approval as na inner join flow_instance_node as n on na.id=n.instance_id "
            + "where n.instance_type=:#{#instanceType.name()} and n.activity_id=:activityId and n.flow_instance_id=:flowInstanceId",
            nativeQuery = true)
    Optional<UserTaskInstanceEntity> findByInstanceTypeAndActivityId(@Param("instanceType") FlowNodeType instanceType,
            @Param("activityId") String activityId, @Param("flowInstanceId") Long flowInstanceId);

    @Query(value = "select na.* from flow_instance_node_approval as na inner join flow_instance_node as n on na.id=n.instance_id "
            + "where n.instance_type=:#{#instanceType.name()} and n.name=:name and n.flow_instance_id=:flowInstanceId",
            nativeQuery = true)
    Optional<UserTaskInstanceEntity> findByInstanceTypeAndName(@Param("instanceType") FlowNodeType instanceType,
            @Param("name") String name, @Param("flowInstanceId") Long flowInstanceId);

    @Query(value = "select wait_for_confirm from flow_instance_node_approval where id =:id limit 1", nativeQuery = true)
    Boolean findConfirmById(@Param("id") Long id);

    @Query(value = "SELECT * FROM flow_instance_node_approval WHERE flow_instance_id in (:flowInstanceIds) and status=:#{#status.name()}",
            nativeQuery = true)
    List<UserTaskInstanceEntity> findApprovalInstanceIdByFlowInstanceIdAndStatus(
            @Param("flowInstanceIds") Collection<Long> flowInstanceIds, @Param("status") FlowNodeStatus status);

    default void batchUpdate(Collection<UserTaskInstanceEntity> userTaskInstanceEntities) {
        String sql = InsertSqlTemplateBuilder.from("flow_instance_node_approval")
                .field(UserTaskInstanceEntity_.ORGANIZATION_ID)
                .field(UserTaskInstanceEntity_.USER_TASK_ID)
                .field(UserTaskInstanceEntity_.STATUS)
                .field(UserTaskInstanceEntity_.OPERATOR_ID)
                .field(UserTaskInstanceEntity_.COMMENT)
                .field("approval_expire_interval_seconds")
                .field("is_approved")
                .field("is_start_endpoint")
                .field("is_end_endpoint")
                .field(UserTaskInstanceEntity_.FLOW_INSTANCE_ID)
                .field("is_auto_approve")
                .field(UserTaskInstanceEntity_.WAIT_FOR_CONFIRM)
                .field(UserTaskInstanceEntity_.EXTERNAL_FLOW_INSTANCE_ID)
                .field(UserTaskInstanceEntity_.EXTERNAL_APPROVAL_ID)
                .build();
        List<Object[]> params = userTaskInstanceEntities.stream().map(i -> new Object[] {
                i.getOrganizationId(),
                i.getUserTaskId(),
                i.getStatus().name(),
                i.getOperatorId(),
                i.getComment(),
                i.getExpireIntervalSeconds(),
                i.isApproved(),
                i.isStartEndpoint(),
                i.isEndEndpoint(),
                i.getFlowInstanceId(),
                i.isAutoApprove(),
                i.getWaitForConfirm(),
                i.getExternalFlowInstanceId(),
                i.getExternalApprovalId()
        }).collect(Collectors.toList());
        getJdbcTemplate().batchUpdate(sql, params);

    }

}
