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
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.PermissionType;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.metadb.iam.PermissionEntity;
import com.oceanbase.odc.metadb.iam.PermissionRepository;
import com.oceanbase.odc.metadb.iam.RolePermissionRepository;
import com.oceanbase.odc.metadb.iam.UserPermissionRepository;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.util.ResourceContextUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2021/8/10
 */

@Slf4j
@Service
@SkipAuthorize("odc internal usage")
public class PermissionService {

    @Autowired
    private PermissionRepository permissionRepository;
    @Autowired
    private RolePermissionRepository rolePermissionRepository;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @Transactional(rollbackFor = Exception.class)
    public List<PermissionEntity> deleteResourceRelatedPermissions(Long resourceId, ResourceType resourceType,
            PermissionType permissionType) {
        // TODO: low performance, may delete all at one time
        List<PermissionEntity> permissionEntityList = permissionRepository.findByTypeAndOrganizationId(permissionType,
                authenticationFacade.currentOrganizationId());
        List<PermissionEntity> deletedEntityList = new ArrayList<>();
        for (PermissionEntity permissionEntity : permissionEntityList) {
            String resourceIdentifier = permissionEntity.getResourceIdentifier();
            if (ResourceContextUtil.matchResourceIdentifier(resourceIdentifier, resourceId, resourceType)
                    && !permissionEntity.getBuiltIn()) {
                deletedEntityList.add(permissionEntity);
                permissionRepository.deleteById(permissionEntity.getId());
                rolePermissionRepository.deleteByPermissionId(permissionEntity.getId());
            }
        }
        if (!deletedEntityList.isEmpty()) {
            userPermissionRepository.deleteByPermissionIds(deletedEntityList.stream()
                    .map(PermissionEntity::getId).collect(Collectors.toList()));
        }
        log.info("Permission related to resource type={}, id={} has been deleted, affted rows={}", resourceType,
                resourceId, deletedEntityList.size());
        return deletedEntityList;
    }

    @SkipAuthorize
    @Transactional(rollbackFor = Exception.class)
    public List<PermissionEntity> deleteResourceRelatedPermissions(Set<Long> resourceIds, ResourceType resourceType) {
        if (CollectionUtils.isEmpty(resourceIds)) {
            return Collections.emptyList();
        }
        List<PermissionEntity> permissions = permissionRepository.findByTypeAndOrganizationId(
                PermissionType.PUBLIC_RESOURCE, authenticationFacade.currentOrganizationId());
        List<PermissionEntity> delete = permissions.stream().filter(e -> {
            String identifier = e.getResourceIdentifier();
            return !e.getBuiltIn() && resourceIds.stream()
                    .anyMatch(id -> ResourceContextUtil.matchResourceIdentifier(identifier, id, resourceType));
        }).collect(Collectors.toList());
        List<Long> permissionIds = delete.stream().map(PermissionEntity::getId).collect(Collectors.toList());
        int affectRows = this.permissionRepository.deleteByIds(permissionIds);
        if (affectRows != delete.size()) {
            throw new IllegalStateException("Failed to delete permission entity, affect rows " + affectRows);
        }
        affectRows = this.rolePermissionRepository.deleteByPermissionIds(permissionIds);
        log.info("Delete related role permission entity, affectRows={}", affectRows);
        affectRows = this.userPermissionRepository.deleteByPermissionIds(permissionIds);
        log.info("Delete related user permission entity, affectRows={}", affectRows);
        return delete;
    }

    @SkipAuthorize
    @Transactional(rollbackFor = Exception.class)
    public void clearExpiredPermission(Date expiredTime) {
        int count = permissionRepository.deleteByExpireTimeBefore(expiredTime);
        log.info("Clear expired permission, count: {}, expired time: {}", count, expiredTime);
    }

}
