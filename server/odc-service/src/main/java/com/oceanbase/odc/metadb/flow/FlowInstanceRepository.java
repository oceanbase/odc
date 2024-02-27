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
import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.oceanbase.odc.config.jpa.OdcJpaRepository;
import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.constant.TaskType;

/**
 * Repository layer for {@link FlowInstanceEntity}
 *
 * @author yh263208
 * @date 2022-02-07 14:35
 * @since ODC_release_3.3.0
 * @see org.springframework.data.jpa.repository.JpaRepository
 */
public interface FlowInstanceRepository
        extends OdcJpaRepository<FlowInstanceEntity, Long>, JpaSpecificationExecutor<FlowInstanceEntity> {

    List<FlowInstanceEntity> findByIdIn(Collection<Long> ids);

    @Transactional
    @Query("update FlowInstanceEntity as fi set fi.name=:#{#param.name},fi.processDefinitionId=:#{#param.processDefinitionId},"
            + "fi.processInstanceId=:#{#param.processInstanceId},fi.status=:#{#param.status},"
            + "fi.flowConfigSnapshotXml=:#{#param.flowConfigSnapshotXml} where fi.id=:#{#param.id}")
    @Modifying
    int update(@Param("param") FlowInstanceEntity entity);

    @Transactional
    @Query(value = "update flow_instance as fi set fi.process_instance_id=:processInstanceId where fi.id=:flowInstanceId",
            nativeQuery = true)
    @Modifying
    int updateProcessInstanceIdById(@Param("flowInstanceId") Long flowInstanceId,
            @Param("processInstanceId") String processInstanceId);

    @Transactional
    @Query(value = "update flow_instance as fi set fi.process_definition_id=:processDefinitionId where fi.id=:flowInstanceId",
            nativeQuery = true)
    @Modifying
    int updateProcessDefinitionIdById(@Param("flowInstanceId") Long flowInstanceId,
            @Param("processDefinitionId") String processDefinitionId);

    @Transactional
    @Query(value = "update flow_instance set status=:#{#status.name()} where id=:id", nativeQuery = true)
    @Modifying
    int updateStatusById(@Param("id") Long id, @Param("status") FlowStatus status);

    @Transactional
    @Query(value = "update flow_instance set status=:#{#status.name()} where id in (:ids)", nativeQuery = true)
    @Modifying
    int updateStatusByIds(@Param("ids") Collection<Long> ids, @Param("status") FlowStatus status);

    @Query("select e.creatorId from FlowInstanceEntity e where e.id=:id")
    Set<Long> findCreatorIdById(@Param("id") Long id);

    @Query("select e.id from FlowInstanceEntity e where e.creatorId=:creatorId")
    Set<Long> findIdByCreatorId(@Param("creatorId") Long creatorId);

    List<FlowInstanceEntity> findByParentInstanceId(Long parentInstanceId);

    @Query(value = "select distinct i.* from flow_instance i left join flow_instance_node_task t "
            + "on i.id=t.flow_instance_id where t.task_task_id=:taskId", nativeQuery = true)
    List<FlowInstanceEntity> findByTaskId(@Param("taskId") Long taskId);

    @Query(value = "select a.parent_instance_id from flow_instance a left join flow_instance_node_task b on a.id = b.flow_instance_id"
            + " where a.id=:id and task_task_id is not null and b.task_type='ALTER_SCHEDULE' LIMIT 1",
            nativeQuery = true)
    Long findScheduleIdByFlowInstanceId(@Param("id") Long id);

    @Query(value = "select a.* from flow_instance a left join flow_instance_node_task b on a.id = b.flow_instance_id where a.id in (:ids) and b.task_type=:#{#type.name()}",
            nativeQuery = true)
    List<FlowInstanceEntity> findByFlowInstanceIdsAndTaskType(@Param("ids") Collection<Long> ids,
            @Param("type") TaskType type);

    @Query(value = "select e.id from FlowInstanceEntity e where e.parentInstanceId=:id and e.status=:status")
    Set<Long> findFlowInstanceIdByScheduleIdAndStatus(@Param("id") Long scheduleId, @Param("status") FlowStatus status);

    @Query(value = "select * from flow_instance  where parent_instance_id in (:scheduleIds) and status=:#{#status.name()}",
            nativeQuery = true)
    Set<FlowInstanceEntity> findByScheduleIdAndStatus(@Param("scheduleIds") Set<Long> scheduleIds,
            @Param("status") FlowStatus status);

    @Query("select e.parentInstanceId as parentInstanceId, count(1) as count from FlowInstanceEntity e where e.parentInstanceId in (?1) group by parentInstanceId")
    List<ParentInstanceIdCount> findByParentInstanceIdIn(Collection<Long> parentInstanceId);

    interface ParentInstanceIdCount {

        Long getParentInstanceId();

        Integer getCount();
    }
}
