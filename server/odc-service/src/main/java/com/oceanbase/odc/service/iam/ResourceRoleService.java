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
package com.oceanbase.odc.service.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotEmpty;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.metadb.iam.resourcerole.ResourceRoleEntity;
import com.oceanbase.odc.metadb.iam.resourcerole.ResourceRoleRepository;
import com.oceanbase.odc.metadb.iam.resourcerole.UserResourceRoleEntity;
import com.oceanbase.odc.metadb.iam.resourcerole.UserResourceRoleRepository;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.model.ResourceRole;
import com.oceanbase.odc.service.iam.model.UserResourceRole;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/5/4 19:18
 * @Description: []
 */
@Service
@Slf4j
public class ResourceRoleService {
    @Autowired
    private ResourceRoleRepository resourceRoleRepository;

    @Autowired
    private UserResourceRoleRepository userResourceRoleRepository;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    private final ResourceRoleMapper resourceRoleMapper = ResourceRoleMapper.INSTANCE;

    @SkipAuthorize("internal usage")
    @Transactional(rollbackFor = Exception.class)
    public List<UserResourceRole> saveAll(List<UserResourceRole> userResourceRoleList) {
        if (CollectionUtils.isEmpty(userResourceRoleList)) {
            return Collections.emptyList();
        }
        List<UserResourceRole> userResourceRoles = new ArrayList<>();
        List<UserResourceRoleEntity> entities = userResourceRoleList.stream().map(i -> {
            ResourceRoleEntity resourceRoleEntity = resourceRoleRepository
                    .findByResourceTypeAndRoleName(i.getResourceType(), i.getResourceRole())
                    .orElseThrow(() -> new UnexpectedException("No such resource role name"));
            UserResourceRoleEntity entity = new UserResourceRoleEntity();
            entity.setResourceId(i.getResourceId());
            entity.setUserId(i.getUserId());
            entity.setResourceRoleId(resourceRoleEntity.getId());
            entity.setOrganizationId(authenticationFacade.currentOrganizationId());
            userResourceRoles.add(fromEntity(entity, resourceRoleEntity));
            return entity;
        }).collect(Collectors.toList());
        userResourceRoleRepository.saveAll(entities);
        return userResourceRoles;
    }

    @SkipAuthorize("odc internal usage")
    public Set<String> getResourceRoleIdentifiersByUserId(long organizationId, long userId) {
        List<UserResourceRoleEntity> userResourceRoleEntities =
                userResourceRoleRepository.findByOrganizationIdAndUserId(organizationId, userId);
        if (CollectionUtils.isEmpty(userResourceRoleEntities)) {
            return Collections.emptySet();
        }
        return userResourceRoleEntities.stream()
                .map(i -> StringUtils.join(i.getResourceId(), ":", i.getResourceRoleId()))
                .collect(Collectors.toSet());
    }

    @SkipAuthorize
    public Map<Long, Set<ResourceRoleName>> getProjectId2ResourceRoleNames() {
        Map<Long, ResourceRole> id2ResourceRoles = listResourceRoles().stream().collect(Collectors
                .toMap(ResourceRole::getId, resourceRole -> resourceRole, (existingValue, newValue) -> newValue));
        return userResourceRoleRepository.findByUserId(authenticationFacade.currentUserId()).stream()
                .collect(Collectors.groupingBy(UserResourceRoleEntity::getResourceId, Collectors.mapping(
                        e -> id2ResourceRoles.get(e.getResourceRoleId()).getRoleName(), Collectors.toSet())));
    }

    @SkipAuthorize("internal usage")
    public Optional<ResourceRoleEntity> findResourceRoleById(@NonNull Long id) {
        return resourceRoleRepository.findById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("internal usage")
    public List<UserResourceRole> listByResourceId(Long resourceId) {
        List<UserResourceRoleEntity> entities = userResourceRoleRepository.findByResourceId(resourceId);
        return entities.stream().map(e -> {
            ResourceRoleEntity resourceRole = resourceRoleRepository
                    .findById(e.getResourceRoleId())
                    .orElseThrow(() -> new UnexpectedException("resource role not found, id=" + e.getResourceRoleId()));
            return fromEntity(e, resourceRole);

        }).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("internal usage")
    public List<UserResourceRole> listByResourceIdIn(@NonNull Set<Long> resourceIds) {
        List<UserResourceRoleEntity> entities = userResourceRoleRepository.findByResourceIdIn(resourceIds);
        Map<Long, ResourceRoleEntity> id2ResourceRoles = resourceRoleRepository.findAll().stream()
                .collect(Collectors.toMap(ResourceRoleEntity::getId, v -> v));
        return entities.stream().map(entity -> fromEntity(entity, id2ResourceRoles.get(entity.getResourceRoleId())))
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("only for unit test")
    public int deleteByResourceId(Long resourceId) {
        return userResourceRoleRepository.deleteByResourceId(resourceId);
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("internal usage")
    public int deleteByResourceIdAndUserId(@NonNull Long resourceId, @NonNull Long userId) {
        return userResourceRoleRepository.deleteByResourceIdAndUserId(resourceId, userId);
    }

    @SkipAuthorize("internal usage")
    @Transactional(rollbackFor = Exception.class)
    public int deleteByUserIdAndResourceIdIn(@NonNull Long userId, @NotEmpty Set<Long> resourceIds) {
        return userResourceRoleRepository.deleteByUserIdAndResourceIdIn(userId, resourceIds);
    }

    @SkipAuthorize("internal usage")
    public int deleteByUserId(@NonNull Long userId) {
        return userResourceRoleRepository.deleteByUserId(userId);
    }

    @SkipAuthorize("internal usage")
    public List<ResourceRole> listResourceRoles() {
        return resourceRoleRepository.findAll().stream()
                .map(resourceRoleMapper::entityToModel).collect(Collectors.toList());
    }

    @SkipAuthorize("internal authenticated")
    public List<UserResourceRole> listByOrganizationIdAndUserId(Long organizationId, Long userId) {
        List<UserResourceRoleEntity> entities =
                userResourceRoleRepository.findByOrganizationIdAndUserId(organizationId, userId);
        return entities.stream().map(e -> {
            ResourceRoleEntity resourceRole = resourceRoleRepository
                    .findById(e.getResourceRoleId())
                    .orElseThrow(() -> new UnexpectedException("resource role not found, id=" + e.getResourceRoleId()));
            return fromEntity(e, resourceRole);
        }).collect(Collectors.toList());
    }

    @SkipAuthorize("internal usage")
    public List<UserResourceRole> listByUserId(Long userId) {
        List<UserResourceRoleEntity> entities =
                userResourceRoleRepository.findByUserId(userId);
        return entities.stream().map(e -> {
            ResourceRoleEntity resourceRole = resourceRoleRepository
                    .findById(e.getResourceRoleId())
                    .orElseThrow(() -> new UnexpectedException("resource role not found, id=" + e.getResourceRoleId()));
            return fromEntity(e, resourceRole);
        }).collect(Collectors.toList());
    }

    private UserResourceRole fromEntity(UserResourceRoleEntity entity, ResourceRoleEntity resourceRole) {
        UserResourceRole model = new UserResourceRole();
        model.setResourceRole(resourceRole.getRoleName());
        model.setResourceType(resourceRole.getResourceType());
        model.setResourceId(entity.getResourceId());
        model.setUserId(entity.getUserId());
        return model;
    }

}
