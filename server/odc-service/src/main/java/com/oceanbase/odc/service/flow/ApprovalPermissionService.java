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
package com.oceanbase.odc.service.flow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.common.lang.Pair;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceCandidateEntity;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceCandidateRepository;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceEntity;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceRepository;
import com.oceanbase.odc.metadb.iam.RoleRepository;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.iam.UserRepository;
import com.oceanbase.odc.metadb.iam.UserRoleEntity;
import com.oceanbase.odc.metadb.iam.UserRoleRepository;
import com.oceanbase.odc.metadb.iam.resourcerole.UserResourceRoleEntity;
import com.oceanbase.odc.metadb.iam.resourcerole.UserResourceRoleRepository;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;
import com.oceanbase.odc.service.iam.ResourceRoleService;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.model.Role;
import com.oceanbase.odc.service.iam.model.User;

import lombok.NonNull;

/**
 * Approval Permission Validator
 *
 * @author yh263208
 * @date 2022-03-09 19:07
 * @since ODC_release_3.3.0
 */
@Service
@SkipAuthorize("odc internal usage")
public class ApprovalPermissionService {

    @Autowired
    private UserService userService;
    @Autowired
    private ResourceRoleService resourceRoleService;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private UserTaskInstanceCandidateRepository userTaskCandidateRepository;
    @Autowired
    private UserTaskInstanceRepository userTaskInstanceRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRoleRepository userRoleRepository;
    @Autowired
    private UserResourceRoleRepository userResourceRoleRepository;

    /**
     * 获取审批节点可审批用户集合
     *
     * @param approvalInstanceId
     * @param enabled 关联用户或角色是否启用，true -> 角色用户启用; false -> 角色用户禁用; null -> 启用禁用用户均筛选出来
     * @return list of {@link User}
     */
    public Set<User> getCandidates(@NonNull Long approvalInstanceId, Boolean enabled) {
        List<UserTaskInstanceCandidateEntity> entities =
                userTaskCandidateRepository.findByApprovalInstanceId(approvalInstanceId);
        String roleKey = "ROLE";
        String userKey = "USER";
        Map<String, Set<Long>> candidates = entities.stream().filter(
                entity -> entity.getUserId() != null || entity.getRoleId() != null).flatMap(entity -> {
                    List<Pair<String, Long>> list = new LinkedList<>();
                    if (entity.getRoleId() != null) {
                        list.add(new Pair<>(roleKey, entity.getRoleId()));
                    }
                    if (entity.getUserId() != null) {
                        list.add(new Pair<>(userKey, entity.getUserId()));
                    }
                    return list.stream();
                }).collect(Collectors.toMap(pair -> pair.left, pair -> new HashSet<>(Collections.singleton(pair.right)),
                        (s1, s2) -> {
                            s1.addAll(s2);
                            return s1;
                        }));
        Set<Long> userIds = candidates.getOrDefault(userKey, new HashSet<>());
        Set<Long> roleIds = candidates.getOrDefault(roleKey, new HashSet<>());
        if (userIds.isEmpty() && roleIds.isEmpty()) {
            return Collections.emptySet();
        } else if (userIds.isEmpty()) {
            return userRepository.findByRoleIdsAndEnabled(roleIds, enabled).stream().map(User::new)
                    .collect(Collectors.toSet());
        } else if (roleIds.isEmpty()) {
            return userRepository.findByUserIdsAndEnabled(userIds, enabled).stream().map(User::new)
                    .collect(Collectors.toSet());
        } else {
            return userRepository.findByUserIdsOrRoleIds(userIds, roleIds, enabled)
                    .stream().map(User::new).collect(Collectors.toSet());
        }
    }

    public Map<Long, Set<Long>> getInstanceId2CandidateRoleIds(@NonNull Collection<Long> approvalInstanceIds) {
        if (CollectionUtils.isEmpty(approvalInstanceIds)) {
            return new HashMap<>();
        }
        List<UserTaskInstanceCandidateEntity> entities =
                userTaskCandidateRepository.findByApprovalInstanceIds(approvalInstanceIds);
        return entities.stream().collect(Collectors.groupingBy(UserTaskInstanceCandidateEntity::getApprovalInstanceId))
                .entrySet().stream().collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().stream()
                        .map(UserTaskInstanceCandidateEntity::getRoleId).collect(Collectors.toSet())));
    }

    public Map<Long, Set<String>> getInstanceId2CandidateResourceRoleIdentifierIds(
            @NonNull Collection<Long> approvalInstanceIds) {
        if (CollectionUtils.isEmpty(approvalInstanceIds)) {
            return new HashMap<>();
        }
        List<UserTaskInstanceCandidateEntity> entities =
                userTaskCandidateRepository.findByApprovalInstanceIds(approvalInstanceIds);
        return entities.stream().collect(Collectors.groupingBy(UserTaskInstanceCandidateEntity::getApprovalInstanceId))
                .entrySet().stream().collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().stream()
                        .map(UserTaskInstanceCandidateEntity::getResourceRoleIdentifier).collect(Collectors.toSet())));
    }


    public Set<Long> getCandidateUserIds(@NonNull Collection<Long> approvalInstanceIds) {
        if (CollectionUtils.isEmpty(approvalInstanceIds)) {
            return Collections.emptySet();
        }
        List<Long> userIds = new ArrayList<>();

        List<UserTaskInstanceCandidateEntity> entities =
                userTaskCandidateRepository.findByApprovalInstanceIds(approvalInstanceIds);
        Set<Long> roleIds =
                entities.stream().map(UserTaskInstanceCandidateEntity::getRoleId).collect(Collectors.toSet());
        if (CollectionUtils.isNotEmpty(roleIds)) {
            userIds.addAll(userRoleRepository.findByRoleIdIn(roleIds).stream().map(UserRoleEntity::getUserId)
                    .collect(Collectors.toSet()));
        }
        Set<String> resourceRoleIdentifiers =
                entities.stream().map(UserTaskInstanceCandidateEntity::getResourceRoleIdentifier)
                        .collect(Collectors.toSet());
        if (CollectionUtils.isNotEmpty(resourceRoleIdentifiers)) {
            userIds.addAll(
                    userResourceRoleRepository.findByResourceIdsAndResourceRoleIdsIn(resourceRoleIdentifiers)
                            .stream().map(UserResourceRoleEntity::getUserId).collect(Collectors.toSet()));
        }
        return userIds.stream().collect(Collectors.toSet());
    }


    public List<Role> getCandidateRoles(@NonNull Long approvalInstanceId, Boolean enabled) {
        List<UserTaskInstanceCandidateEntity> entities =
                userTaskCandidateRepository.findByApprovalInstanceId(approvalInstanceId);
        String roleKey = "ROLE";
        Map<String, Set<Long>> candidates = entities.stream().filter(
                entity -> entity.getRoleId() != null).flatMap(entity -> {
                    List<Pair<String, Long>> list = new LinkedList<>();
                    if (entity.getRoleId() != null) {
                        list.add(new Pair<>(roleKey, entity.getRoleId()));
                    }
                    return list.stream();
                }).collect(Collectors.toMap(pair -> pair.left, pair -> new HashSet<>(Collections.singleton(pair.right)),
                        (s1, s2) -> {
                            s1.addAll(s2);
                            return s1;
                        }));
        Set<Long> roleIds = candidates.getOrDefault(roleKey, new HashSet<>());
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return roleRepository.findByRoleIdsAndEnabled(roleIds, enabled).stream().map(Role::new)
                .collect(Collectors.toList());
    }

    public boolean isApprovable(@NonNull Long approvalInstanceId) {
        Set<Long> ids = getApprovableApprovalInstances().stream().map(UserTaskInstanceEntity::getId)
                .collect(Collectors.toSet());
        return ids.contains(approvalInstanceId);
    }

    public List<UserTaskInstanceEntity> getApprovableApprovalInstances() {
        long userId = authenticationFacade.currentUserId();
        Set<Long> roleIds = userService.getCurrentUserRoleIds();
        Set<String> resourceRoleIdentifiers = userService.getCurrentUserResourceRoleIdentifiers();
        List<UserTaskInstanceEntity> userTaskInstanceEntities = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(resourceRoleIdentifiers)) {
            userTaskInstanceEntities
                    .addAll(userTaskInstanceRepository.findByResourceRoleIdentifierIn(resourceRoleIdentifiers));
        }
        if (CollectionUtils.isEmpty(roleIds)) {
            if (CollectionUtils.isEmpty(resourceRoleIdentifiers)) {
                userTaskInstanceEntities.addAll(userTaskInstanceRepository.findByCandidateUserId(userId));
            } else {
                userTaskInstanceEntities
                        .addAll(userTaskInstanceRepository.findByCandidateUserIdOrResourceRoleIdentifier(userId,
                                resourceRoleIdentifiers));
            }
        } else {
            userTaskInstanceEntities.addAll(userTaskInstanceRepository.findByCandidateUserIdOrRoleIds(userId, roleIds));
        }
        return userTaskInstanceEntities;
    }

    public void setCandidateResourceRole(@NonNull Long approvalInstanceId, @NonNull String resourceRoleIdentifier) {
        UserTaskInstanceCandidateEntity candidateEntity = new UserTaskInstanceCandidateEntity();
        candidateEntity.setApprovalInstanceId(approvalInstanceId);
        candidateEntity.setResourceRoleIdentifier(resourceRoleIdentifier);
        userTaskCandidateRepository.save(candidateEntity);
    }

    public List<UserTaskInstanceEntity> listApprovableExternalInstances() {
        return userTaskInstanceRepository.findByStatus(FlowNodeStatus.EXECUTING).stream()
                .filter(entity -> entity.getExternalApprovalId() != null && entity.getExternalFlowInstanceId() != null)
                .collect(Collectors.toList());
    }

    public Map<Long, Set<UserEntity>> getCandidatesByFlowInstanceIds(@NonNull Collection<Long> flowInstanceIds) {

        // find executing approval instance
        if (flowInstanceIds.isEmpty()) {
            return new HashMap<>();
        }
        Map<Long, Long> approvalInstanceId2FlowInstanceId =
                userTaskInstanceRepository.findApprovalInstanceIdByFlowInstanceIdAndStatus(
                        flowInstanceIds,
                        FlowNodeStatus.EXECUTING).stream().collect(
                                Collectors.toMap(UserTaskInstanceEntity::getId,
                                        UserTaskInstanceEntity::getFlowInstanceId));
        // get resource role identifier by approval isntance id
        if (approvalInstanceId2FlowInstanceId.isEmpty()) {
            return new HashMap<>();
        }
        Map<Long, Set<String>> instanceId2CandidateResourceRoleIdentifierIds =
                getInstanceId2CandidateResourceRoleIdentifierIds(approvalInstanceId2FlowInstanceId.keySet());
        // find users by resource role identifier
        Set<String> resourceRoleIdentifiers =
                instanceId2CandidateResourceRoleIdentifierIds.values().stream().flatMap(Set::stream).collect(
                        Collectors.toSet());

        if (resourceRoleIdentifiers.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, Set<Long>> identifier2UserIds = userResourceRoleRepository.findByResourceIdsAndResourceRoleIdsIn(
                resourceRoleIdentifiers).stream()
                .collect(Collectors.groupingBy(o -> String.format("%s:%s", o.getResourceId(), o.getResourceRoleId())))
                .entrySet()
                .stream().collect(Collectors.toMap(entry -> entry.getKey(),
                        entry -> entry.getValue().stream().map(UserResourceRoleEntity::getUserId).collect(
                                Collectors.toSet())));
        // map approval instance ids to users ids
        Map<Long, Set<Long>> instanceId2UserIds =
                instanceId2CandidateResourceRoleIdentifierIds.entrySet().stream().collect(
                        Collectors.toMap(Entry::getKey,
                                entry -> entry.getValue().stream()
                                        .filter(i -> CollectionUtils.isNotEmpty(identifier2UserIds.get(i)))
                                        .map(identifier2UserIds::get).flatMap(Set::stream)
                                        .collect(Collectors.toSet())));
        Set<Long> approvalUserIds =
                identifier2UserIds.values().stream().flatMap(Set::stream).collect(Collectors.toSet());

        if (approvalUserIds.isEmpty()) {
            return new HashMap<>();
        }
        Map<Long, UserEntity> userId2User = userRepository.findByUserIds(approvalUserIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, userEntity -> userEntity));

        Map<Long, Set<UserEntity>> instanceId2Candidates = instanceId2UserIds.entrySet().stream().collect(
                Collectors.toMap(Entry::getKey, entry -> entry.getValue().stream().map(userId2User::get).collect(
                        Collectors.toSet())));
        // map flow instance ids to users entity
        return instanceId2UserIds.entrySet().stream().collect(
                Collectors.toMap(entry -> approvalInstanceId2FlowInstanceId.get(entry.getKey()),
                        entry -> instanceId2Candidates.get(entry.getKey())));
    }

}
