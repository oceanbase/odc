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
package com.oceanbase.odc.service.schedule.factory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.metadb.flow.FlowInstanceEntity;
import com.oceanbase.odc.metadb.flow.FlowInstanceRepository;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceEntity;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.iam.UserRepository;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.dlm.DlmLimiterService;
import com.oceanbase.odc.service.dlm.model.RateLimitConfiguration;
import com.oceanbase.odc.service.flow.ApprovalPermissionService;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;
import com.oceanbase.odc.service.schedule.model.ScheduleDetailResp.ScheduleResponseMapper;

import lombok.NonNull;

/**
 * @Author：tinker
 * @Date: 2022/12/6 11:31
 * @Descripition:
 */

@Component
public class ScheduleResponseMapperFactory {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FlowInstanceRepository flowInstanceRepository;
    @Autowired
    private ApprovalPermissionService approvalPermissionService;
    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private DlmLimiterService dlmLimiterService;

    public ScheduleResponseMapper generate(ScheduleEntity entity) {
        return generate(Collections.singletonList(entity));
    }

    public ScheduleResponseMapper generateInternalWithoutPermission(@NonNull Collection<ScheduleEntity> entities) {
        if (entities.isEmpty()) {
            return new ScheduleResponseMapper();
        }
        Set<Long> scheduleIds = entities.stream().map(ScheduleEntity::getId).collect(Collectors.toSet());
        Set<Long> creators = entities.stream().map(ScheduleEntity::getCreatorId).collect(Collectors.toSet());
        List<UserEntity> userEntities = userRepository.findByUserIds(creators);
        Map<Long, UserEntity> userEntityMap = userEntities.stream().collect(
                Collectors.toMap(UserEntity::getId, userEntity -> userEntity));
        Set<Long> databaseIds = entities.stream().map(ScheduleEntity::getDatabaseId).collect(Collectors.toSet());
        Map<Long, Database> id2Database = databaseService.listDatabasesByIds(databaseIds).stream().collect(
                Collectors.toMap(Database::getId, o -> o));


        Map<Long, RateLimitConfiguration> scheduleId2RateLimitConfiguration =
                dlmLimiterService.findByOrderIds(scheduleIds).stream().collect(
                        Collectors.toMap(RateLimitConfiguration::getOrderId, o -> o));


        return new ScheduleResponseMapper()
                .withGetUserById(userEntityMap::get)
                .withGetApproveInstanceIdById(t -> null)
                .withGetDatabaseById(id2Database::get)
                .withGetCandidatesById(t -> null)
                .withGetDLMRateLimitConfigurationById(scheduleId2RateLimitConfiguration::get);
    }


    public ScheduleResponseMapper generate(@NonNull Collection<ScheduleEntity> entities) {
        if (entities.isEmpty()) {
            return new ScheduleResponseMapper();
        }
        ScheduleResponseMapper scheduleResponseMapper = generateInternalWithoutPermission(entities);
        Set<Long> scheduleIds = entities.stream().map(ScheduleEntity::getId).collect(Collectors.toSet());
        Set<Long> approvableFlowInstanceIds = approvalPermissionService.getApprovableApprovalInstances()
                .stream()
                .filter(entity -> FlowNodeStatus.EXECUTING == entity.getStatus())
                .map(UserTaskInstanceEntity::getFlowInstanceId).collect(Collectors.toSet());
        Map<Long, Long> approveInstanceIdMap = Collections.EMPTY_MAP;
        if (!approvableFlowInstanceIds.isEmpty()) {
            List<FlowInstanceEntity> flowInstances = flowInstanceRepository.findByFlowInstanceIdsAndTaskType(
                    approvableFlowInstanceIds, TaskType.ALTER_SCHEDULE);
            approveInstanceIdMap =
                    flowInstances.stream().filter(entity -> entity.getParentInstanceId() != null).collect(
                            Collectors.toMap(FlowInstanceEntity::getParentInstanceId, FlowInstanceEntity::getId,
                                    (oldValue, newValue) -> oldValue));
        }
        Map<Long, Long> scheduleId2FlowInstanceId = flowInstanceRepository.findByScheduleIdAndStatus(scheduleIds,
                FlowStatus.APPROVING).stream().collect(
                        Collectors.toMap(FlowInstanceEntity::getParentInstanceId, FlowInstanceEntity::getId));

        Map<Long, Set<UserEntity>> flowInstanceId2Candidates =
                approvalPermissionService.getCandidatesByFlowInstanceIds(scheduleId2FlowInstanceId.values());

        Map<Long, Set<UserEntity>> scheduleId2Candidates = scheduleId2FlowInstanceId.entrySet().stream()
                .filter(entry -> flowInstanceId2Candidates.get(entry.getValue()) != null).collect(
                        Collectors.toMap(Entry::getKey, entry -> flowInstanceId2Candidates.get(entry.getValue())));

        Map<Long, RateLimitConfiguration> scheduleId2RateLimitConfiguration =
                dlmLimiterService.findByOrderIds(scheduleIds).stream().collect(
                        Collectors.toMap(RateLimitConfiguration::getOrderId, o -> o));

        return scheduleResponseMapper
                .withGetApproveInstanceIdById(approveInstanceIdMap::get)
                .withGetCandidatesById(scheduleId2Candidates::get)
                .withGetDLMRateLimitConfigurationById(scheduleId2RateLimitConfiguration::get);
    }
}
