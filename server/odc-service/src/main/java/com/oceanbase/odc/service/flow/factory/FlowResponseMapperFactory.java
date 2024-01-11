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
package com.oceanbase.odc.service.flow.factory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.metadb.connection.ConnectionConfigRepository;
import com.oceanbase.odc.metadb.connection.ConnectionEntity;
import com.oceanbase.odc.metadb.connection.ConnectionSpecs;
import com.oceanbase.odc.metadb.flow.FlowInstanceEntity;
import com.oceanbase.odc.metadb.flow.FlowInstanceRepository;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceEntity;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceRepository;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceSpecs;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceCandidateEntity;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceCandidateRepository;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceEntity;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceRepository;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceSpecs;
import com.oceanbase.odc.metadb.iam.RoleEntity;
import com.oceanbase.odc.metadb.iam.RoleRepository;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.iam.UserRepository;
import com.oceanbase.odc.metadb.iam.UserRoleEntity;
import com.oceanbase.odc.metadb.iam.UserRoleRepository;
import com.oceanbase.odc.metadb.iam.resourcerole.UserResourceRoleEntity;
import com.oceanbase.odc.metadb.iam.resourcerole.UserResourceRoleRepository;
import com.oceanbase.odc.metadb.integration.IntegrationEntity;
import com.oceanbase.odc.metadb.regulation.risklevel.RiskLevelRepository;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.metadb.task.TaskRepository;
import com.oceanbase.odc.metadb.task.TaskSpecs;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.util.ConnectionMapper;
import com.oceanbase.odc.service.flow.ApprovalPermissionService;
import com.oceanbase.odc.service.flow.instance.FlowInstance;
import com.oceanbase.odc.service.flow.model.FlowInstanceDetailResp;
import com.oceanbase.odc.service.flow.model.FlowInstanceDetailResp.FlowInstanceMapper;
import com.oceanbase.odc.service.flow.model.FlowNodeInstanceDetailResp;
import com.oceanbase.odc.service.flow.model.FlowNodeInstanceDetailResp.FlowNodeInstanceMapper;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;
import com.oceanbase.odc.service.flow.model.FlowTaskExecutionStrategy;
import com.oceanbase.odc.service.flow.task.model.DBStructureComparisonParameter;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.integration.IntegrationService;
import com.oceanbase.odc.service.integration.client.ApprovalClient;
import com.oceanbase.odc.service.integration.model.ApprovalProperties;
import com.oceanbase.odc.service.integration.model.IntegrationConfig;
import com.oceanbase.odc.service.integration.model.TemplateVariables;
import com.oceanbase.odc.service.integration.model.TemplateVariables.Variable;
import com.oceanbase.odc.service.regulation.risklevel.RiskLevelMapper;

import lombok.NonNull;

/**
 * Mapper factory to generate
 * {@link com.oceanbase.odc.service.flow.model.FlowInstanceDetailResp.FlowInstanceMapper} and
 * {@link com.oceanbase.odc.service.flow.model.FlowNodeInstanceDetailResp.FlowNodeInstanceMapper}
 *
 * @author yh263208
 * @date 2022-03-09 18:07
 * @since ODC_release_3.3.0
 */
@Component
public class FlowResponseMapperFactory {

    @Autowired
    private UserTaskInstanceRepository userTaskInstanceRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRoleRepository userRoleRepository;
    @Autowired
    private ConnectionConfigRepository connectionRepository;
    @Autowired
    private UserTaskInstanceCandidateRepository userTaskCandidateRepository;
    @Autowired
    private ServiceTaskInstanceRepository serviceTaskRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private DatabaseService databaseService;
    @Autowired
    private ApprovalPermissionService approvalPermissionService;
    @Autowired
    private ApprovalClient approvalClient;
    @Autowired
    private IntegrationService integrationService;
    @Autowired
    private FlowInstanceRepository flowInstanceRepository;
    @Autowired
    private UserResourceRoleRepository userResourceRoleRepository;
    @Autowired
    private RiskLevelRepository riskLevelRepository;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    private final ConnectionMapper connectionMapper = ConnectionMapper.INSTANCE;

    private final RiskLevelMapper riskLevelMapper = RiskLevelMapper.INSTANCE;

    public FlowNodeInstanceMapper generateNodeMapperByInstances(@NonNull Collection<FlowInstance> flowInstances) {
        return generateNodeMapper(getLongSet(flowInstances, FlowInstance::getId),
                getLongSet(flowInstances, FlowInstance::getCreatorId));
    }

    public FlowInstanceMapper generateMapperByInstances(@NonNull Collection<FlowInstance> flowInstances) {
        return generateMapper(getLongSet(flowInstances, FlowInstance::getId),
                getLongSet(flowInstances, FlowInstance::getCreatorId));
    }

    public FlowInstanceMapper generateMapperByEntities(@NonNull Collection<FlowInstanceEntity> entities) {
        return generateMapper(getLongSet(entities, FlowInstanceEntity::getId),
                getLongSet(entities, FlowInstanceEntity::getCreatorId));
    }

    public FlowNodeInstanceMapper generateNodeMapperByInstance(@NonNull FlowInstance flowInstance) {
        return generateNodeMapperByInstances(Collections.singleton(flowInstance));
    }

    public FlowInstanceMapper generateMapperByInstance(@NonNull FlowInstance flowInstance) {
        return generateMapperByInstances(Collections.singleton(flowInstance));
    }

    public FlowNodeInstanceMapper generateNodeMapperByInstanceIds(@NonNull Collection<Long> flowInstanceIds) {
        return generateNodeMapper(flowInstanceIds, Collections.emptySet());
    }

    public FlowInstanceMapper generateMapperByInstanceIds(@NonNull Collection<Long> flowInstanceIds) {
        return generateMapper(flowInstanceIds, Collections.emptySet());
    }

    private <T> Set<Long> getLongSet(@NonNull Collection<T> values, @NonNull Function<T, Long> function) {
        return values.stream().map(function).collect(Collectors.toSet());
    }

    private FlowNodeInstanceMapper generateNodeMapper(@NonNull Collection<Long> flowInstanceIds,
            @NonNull Set<Long> creatorIds) {
        if (flowInstanceIds.isEmpty()) {
            return FlowNodeInstanceDetailResp.mapper();
        }
        Specification<UserTaskInstanceEntity> specification =
                Specification.where(UserTaskInstanceSpecs.flowInstanceIdIn(flowInstanceIds));
        List<UserTaskInstanceEntity> userTaskEntities = userTaskInstanceRepository.findAll(specification);

        Set<Long> userIds = userTaskEntities.stream().filter(entity -> entity.getOperatorId() != null)
                .map(UserTaskInstanceEntity::getOperatorId).collect(Collectors.toSet());
        userIds.addAll(creatorIds);
        Map<Long, UserEntity> userId2User = listUsersByUserIds(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, userEntity -> userEntity));

        Set<Long> approvalInstanceIds =
                userTaskEntities.stream().map(UserTaskInstanceEntity::getId).collect(Collectors.toSet());
        List<UserTaskInstanceCandidateEntity> candidateEntities = new LinkedList<>();
        if (!approvalInstanceIds.isEmpty()) {
            candidateEntities = userTaskCandidateRepository.findByApprovalInstanceIds(approvalInstanceIds);
        }
        Map<Long, List<UserEntity>> approvalId2Candidates = candidateEntities.stream().collect(Collectors
                .groupingBy(UserTaskInstanceCandidateEntity::getApprovalInstanceId)).entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, entry -> {
                    Set<Long> candidateUserIds = new HashSet<>();
                    Set<Long> candidateRoleIds = new HashSet<>();
                    Set<String> candidateResourceRoleIdentifiers = new HashSet<>();
                    entry.getValue().forEach(entity -> {
                        if (entity.getUserId() != null) {
                            candidateUserIds.add(entity.getUserId());
                        }
                        if (entity.getRoleId() != null) {
                            candidateRoleIds.add(entity.getRoleId());
                        }
                        if (entity.getResourceRoleIdentifier() != null) {
                            candidateResourceRoleIdentifiers.add(entity.getResourceRoleIdentifier());
                        }
                    });
                    if (candidateUserIds.isEmpty() && candidateRoleIds.isEmpty()
                            && candidateResourceRoleIdentifiers.isEmpty()) {
                        return Collections.emptyList();
                    } else if (!candidateResourceRoleIdentifiers.isEmpty()) {
                        Set<Long> resourceRoleUserIds = userResourceRoleRepository
                                .findByResourceIdsAndResourceRoleIdsIn(candidateResourceRoleIdentifiers)
                                .stream().map(UserResourceRoleEntity::getUserId).collect(Collectors.toSet());
                        return CollectionUtils.isEmpty(resourceRoleUserIds) ? Collections.emptyList()
                                : userRepository.findByUserIdsAndEnabled(resourceRoleUserIds, true);
                    } else if (candidateUserIds.isEmpty()) {
                        return userRepository.findByRoleIdsAndEnabled(candidateRoleIds, true);
                    } else if (candidateRoleIds.isEmpty()) {
                        return userRepository.findByUserIdsAndEnabled(candidateUserIds, true);
                    } else {
                        return userRepository.findByUserIdsOrRoleIds(candidateUserIds, candidateRoleIds, true);
                    }
                }));
        approvalId2Candidates.values().stream()
                .flatMap((Function<List<UserEntity>, Stream<UserEntity>>) Collection::stream)
                .forEach(entity -> userId2User.putIfAbsent(entity.getId(), entity));

        Map<Long, List<RoleEntity>> userId2Roles = getUserId2Roles(userId2User.keySet());

        Specification<ServiceTaskInstanceEntity> serviceSpec =
                Specification.where(ServiceTaskInstanceSpecs.flowInstanceIdIn(flowInstanceIds));
        List<ServiceTaskInstanceEntity> serviceEntities = serviceTaskRepository.findAll(serviceSpec);
        Set<Long> taskIds = serviceEntities.stream().filter(entity -> entity.getTargetTaskId() != null)
                .map(ServiceTaskInstanceEntity::getTargetTaskId).collect(Collectors.toSet());
        Map<Long, TaskEntity> taskId2TaskEntity = listTasksByTaskIdsWithoutPermissionCheck(taskIds).stream()
                .collect(Collectors.toMap(TaskEntity::getId, taskEntity -> taskEntity));

        return FlowNodeInstanceDetailResp.mapper()
                .withGetCandidatesByApprovalId(approvalId2Candidates::get)
                .withGetTaskById(taskId2TaskEntity::get)
                .withGetUserById(userId2User::get)
                .withGetRolesByUserId(userId2Roles::get)
                .withGetExternalApprovalNameById(externalApprovalId -> {
                    IntegrationEntity config = integrationService.nullSafeGet(externalApprovalId);
                    return config.getName();
                })
                .withGetExternalUrlByExternalId(externalApproval -> {
                    IntegrationConfig config =
                            integrationService.detailWithoutPermissionCheck(externalApproval.getApprovalId());
                    ApprovalProperties properties = ApprovalProperties.from(config);
                    if (StringUtils.isEmpty(properties.getAdvanced().getHyperlinkExpression())) {
                        return null;
                    }
                    TemplateVariables variables = new TemplateVariables();
                    variables.setAttribute(Variable.PROCESS_INSTANCE_ID, externalApproval.getInstanceId());
                    return approvalClient.buildHyperlink(properties.getAdvanced().getHyperlinkExpression(), variables);
                });
    }

    private FlowInstanceMapper generateMapper(@NonNull Collection<Long> flowInstanceIds,
            @NonNull Set<Long> creatorIds) {
        if (flowInstanceIds.isEmpty()) {
            return FlowInstanceDetailResp.mapper();
        }
        Specification<ServiceTaskInstanceEntity> serviceSpec =
                Specification.where(ServiceTaskInstanceSpecs.flowInstanceIdIn(flowInstanceIds));
        List<ServiceTaskInstanceEntity> serviceEntities = serviceTaskRepository.findAll(serviceSpec);

        Map<Long, List<Date>> flowInstanceId2ExecutionTime = serviceEntities.stream()
                .filter(entity -> entity.getExecutionTime() != null)
                .collect(Collectors.groupingBy(ServiceTaskInstanceEntity::getFlowInstanceId,
                        Collectors.mapping(ServiceTaskInstanceEntity::getExecutionTime, Collectors.toList())));

        Map<Long, List<FlowTaskExecutionStrategy>> flowInstanceId2ExecutionStrategy = serviceEntities.stream()
                .filter(e -> e.getTaskType().needForExecutionStrategy())
                .collect(Collectors.groupingBy(ServiceTaskInstanceEntity::getFlowInstanceId,
                        Collectors.mapping(ServiceTaskInstanceEntity::getStrategy, Collectors.toList())));

        Map<Long, Boolean> flowInstanceId2Rollbackable = flowInstanceIds.stream().collect(Collectors
                .toMap(Function.identity(), id -> flowInstanceRepository.findByParentInstanceId(id).size() == 0));

        /**
         * In order to improve the interface efficiency, it is necessary to find out the task entity
         * corresponding to the process instance at one time
         */
        Map<Long, Set<TaskEntity>> flowInstanceId2Tasks = new HashMap<>();
        Set<Long> taskIds = serviceEntities.stream().filter(entity -> entity.getTargetTaskId() != null)
                .map(ServiceTaskInstanceEntity::getTargetTaskId).collect(Collectors.toSet());
        Map<Long, TaskEntity> taskId2TaskEntity = listTasksByTaskIdsWithoutPermissionCheck(taskIds).stream()
                .collect(Collectors.toMap(TaskEntity::getId, taskEntity -> taskEntity));
        serviceEntities.stream().filter(entity -> entity.getTargetTaskId() != null).forEach(entity -> {
            Set<TaskEntity> taskEntities =
                    flowInstanceId2Tasks.computeIfAbsent(entity.getFlowInstanceId(), id -> new HashSet<>());
            TaskEntity taskEntity = taskId2TaskEntity.get(entity.getTargetTaskId());
            if (taskEntity != null) {
                taskEntities.add(taskEntity);
            }
        });

        /**
         * Get Database associated with each TaskEntity
         */
        Map<Long, Database> id2Database = new HashMap<>();
        Set<Long> databaseIds = new HashSet<>();
        databaseIds.addAll(taskId2TaskEntity.values().stream().map(TaskEntity::getDatabaseId).filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        databaseIds.addAll(taskId2TaskEntity.values().stream()
                .filter(task -> task.getTaskType().equals(TaskType.STRUCTURE_COMPARISON))
                .map(taskEntity -> JsonUtils
                        .fromJson(taskEntity.getParametersJson(), DBStructureComparisonParameter.class)
                        .getTargetDatabaseId())
                .collect(Collectors.toSet()));
        if (CollectionUtils.isNotEmpty(databaseIds)) {
            id2Database = databaseService.listDatabasesByIds(databaseIds).stream()
                    .collect(Collectors.toMap(Database::getId, database -> database));
        }
        /**
         * find the ConnectionConfig associated with each Database
         */
        Set<Long> connectionIds = id2Database.values().stream()
                .filter(e -> e.getDataSource() != null && e.getDataSource().getId() != null)
                .map(e -> e.getDataSource().getId()).collect(Collectors.toSet());
        Map<Long, ConnectionConfig> id2Connection = listConnectionsByConnectionIdsWithoutPermissionCheck(connectionIds)
                .stream().collect(Collectors.toMap(ConnectionEntity::getId, connectionMapper::entityToModel));
        id2Database.values().forEach(database -> {
            if (id2Connection.containsKey(database.getDataSource().getId())) {
                database.setDataSource(id2Connection.get(database.getDataSource().getId()));
            }
        });

        /**
         * list candidates
         */
        Map<Long, Set<UserEntity>> candidatesByFlowInstanceIds =
                approvalPermissionService.getCandidatesByFlowInstanceIds(flowInstanceIds);

        /**
         * In order to improve the interface efficiency, it is necessary to find out the user entity
         * corresponding to the process instance at one time
         */
        Set<Long> userIds = flowInstanceId2Tasks.values().stream()
                .flatMap(Collection::stream)
                .filter(entity -> entity.getCreatorId() != null)
                .map(TaskEntity::getCreatorId).collect(Collectors.toSet());
        userIds.addAll(creatorIds);
        Map<Long, UserEntity> userId2User = listUsersByUserIds(userIds)
                .stream().collect(Collectors.toMap(UserEntity::getId, entity -> entity));

        Map<Long, List<RoleEntity>> userId2Roles = getUserId2Roles(userId2User.keySet());

        Set<Long> approvableFlowInstanceIds = approvalPermissionService.getApprovableApprovalInstances()
                .stream()
                .filter(entity -> FlowNodeStatus.EXECUTING == entity.getStatus()
                        || entity.getStatus() == FlowNodeStatus.WAIT_FOR_CONFIRM)
                .map(UserTaskInstanceEntity::getFlowInstanceId).collect(Collectors.toSet());
        return FlowInstanceDetailResp.mapper()
                .withRollbackable(flowInstanceId2Rollbackable::get)
                .withApprovable(approvableFlowInstanceIds::contains)
                .withGetTaskByFlowInstanceId(flowInstanceId2Tasks::get)
                .withGetRolesByUserId(userId2Roles::get)
                .withGetUserById(userId2User::get)
                .withGetExecutionTimeByFlowInstanceId(flowInstanceId2ExecutionTime::get)
                .withGetExecutionStrategyByFlowInstanceId(flowInstanceId2ExecutionStrategy::get)
                .withGetRiskLevelByRiskLevelId(
                        id -> riskLevelRepository.findById(id).map(riskLevelMapper::entityToModel).orElse(null))
                .withGetCandidatesByFlowInstanceId(candidatesByFlowInstanceIds::get)
                .withGetDatabaseById(id2Database::get);
    }

    public Map<Long, List<RoleEntity>> getUserId2Roles(@NonNull Collection<Long> userIds) {
        Map<Long, Set<Long>> userId2RoleIds = userRoleRepository
                .findByOrganizationIdAndUserIdIn(authenticationFacade.currentOrganizationId(), userIds).stream()
                .collect(Collectors.groupingBy(UserRoleEntity::getUserId)).entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey,
                        entry -> entry.getValue().stream().map(UserRoleEntity::getRoleId).collect(Collectors.toSet())));
        Set<Long> roleIds = userId2RoleIds.entrySet().stream().flatMap(
                (Function<Entry<Long, Set<Long>>, Stream<Long>>) entry -> entry.getValue().stream())
                .collect(Collectors.toSet());
        if (roleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<RoleEntity> roleEntities = roleRepository.findByRoleIdsAndEnabled(roleIds, true);
        return userId2RoleIds.entrySet().stream().collect(Collectors.toMap(Entry::getKey, entry -> {
            Set<Long> ids = entry.getValue();
            return roleEntities.stream().filter(roleEntity -> ids.contains(roleEntity.getId()))
                    .collect(Collectors.toList());
        }));
    }

    public List<UserEntity> listUsersByUserIds(@NonNull Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return new ArrayList<>();
        }
        return userRepository.findByUserIds(userIds);
    }

    public List<UserEntity> listUsersByRoleIds(@NonNull Collection<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return new ArrayList<>();
        }
        return userRepository.findByRoleIds(roleIds);
    }

    private List<TaskEntity> listTasksByTaskIdsWithoutPermissionCheck(@NonNull Collection<Long> taskIds) {
        if (CollectionUtils.isEmpty(taskIds)) {
            return Collections.emptyList();
        }
        Specification<TaskEntity> specification = Specification.where(TaskSpecs.idIn(taskIds));
        return taskRepository.findAll(specification);
    }

    private List<ConnectionEntity> listConnectionsByConnectionIdsWithoutPermissionCheck(
            @NonNull Collection<Long> connectionIds) {
        if (CollectionUtils.isEmpty(connectionIds)) {
            return Collections.emptyList();
        }
        Specification<ConnectionEntity> specification = Specification.where(ConnectionSpecs.idIn(connectionIds));
        return connectionRepository.findAll(specification);
    }

}
