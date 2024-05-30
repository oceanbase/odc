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
package com.oceanbase.odc.service.permission.database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.common.util.TimeUtils;
import com.oceanbase.odc.core.authority.util.Authenticated;
import com.oceanbase.odc.core.authority.util.PreAuthenticate;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.AuthorizationType;
import com.oceanbase.odc.core.shared.constant.PermissionType;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.AccessDeniedException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.iam.PermissionEntity;
import com.oceanbase.odc.metadb.iam.PermissionRepository;
import com.oceanbase.odc.metadb.iam.UserDatabasePermissionEntity;
import com.oceanbase.odc.metadb.iam.UserDatabasePermissionRepository;
import com.oceanbase.odc.metadb.iam.UserDatabasePermissionSpec;
import com.oceanbase.odc.metadb.iam.UserPermissionEntity;
import com.oceanbase.odc.metadb.iam.UserPermissionRepository;
import com.oceanbase.odc.service.collaboration.project.ProjectService;
import com.oceanbase.odc.service.collaboration.project.model.Project;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.iam.PermissionService;
import com.oceanbase.odc.service.iam.ProjectPermissionValidator;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.permission.database.model.CreateDatabasePermissionReq;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;
import com.oceanbase.odc.service.permission.database.model.ExpirationStatusFilter;
import com.oceanbase.odc.service.permission.database.model.QueryDatabasePermissionParams;
import com.oceanbase.odc.service.permission.database.model.UserDatabasePermission;

/**
 * @author gaoda.xy
 * @date 2024/1/4 11:24
 */
@Service
@Validated
@Authenticated
public class DatabasePermissionService {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectPermissionValidator projectPermissionValidator;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @Autowired
    private UserDatabasePermissionRepository userDatabasePermissionRepository;

    @Autowired
    private PermissionService permissionService;

    @Value("${odc.iam.permission.expired-retention-time-seconds:7776000}")
    private long expiredRetentionTimeSeconds;

    private static final UserDatabasePermissionMapper mapper = UserDatabasePermissionMapper.INSTANCE;
    private static final int EXPIRING_DAYS = 7;

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("permission check inside")
    public Page<UserDatabasePermission> list(@NotNull Long projectId, @NotNull QueryDatabasePermissionParams params,
            Pageable pageable) {
        if (params.getUserId() == null || params.getUserId() != authenticationFacade.currentUserId()) {
            projectPermissionValidator.checkProjectRole(projectId,
                    Arrays.asList(ResourceRoleName.OWNER, ResourceRoleName.DBA));
        } else {
            projectPermissionValidator.checkProjectRole(projectId, ResourceRoleName.all());
        }
        Date expiredTime = new Date(System.currentTimeMillis() - expiredRetentionTimeSeconds * 1000);
        permissionService.deleteExpiredPermission(expiredTime);
        Date expireTimeThreshold = TimeUtils.getStartOfDay(new Date());
        Specification<UserDatabasePermissionEntity> spec = Specification
                .where(UserDatabasePermissionSpec.projectIdEqual(projectId))
                .and(UserDatabasePermissionSpec.organizationIdEqual(authenticationFacade.currentOrganizationId()))
                .and(UserDatabasePermissionSpec.userIdEqual(params.getUserId()))
                .and(UserDatabasePermissionSpec.ticketIdEqual(params.getTicketId()))
                .and(UserDatabasePermissionSpec.databaseNameLike(params.getFuzzyDatabaseName()))
                .and(UserDatabasePermissionSpec.dataSourceNameLike(params.getFuzzyDataSourceName()))
                .and(UserDatabasePermissionSpec.typeIn(params.getTypes()))
                .and(UserDatabasePermissionSpec.authorizationTypeEqual(params.getAuthorizationType()))
                .and(UserDatabasePermissionSpec.filterByExpirationStatus(params.getStatuses(), expireTimeThreshold));
        return userDatabasePermissionRepository.findAll(spec, pageable).map(
                e -> {
                    UserDatabasePermission permission = mapper.entityToModel(e);
                    Date expireTime = permission.getExpireTime();
                    if (expireTime.before(expireTimeThreshold)) {
                        permission.setStatus(ExpirationStatusFilter.EXPIRED);
                    } else if (expireTime.before(DateUtils.addDays(expireTimeThreshold, EXPIRING_DAYS))) {
                        permission.setStatus(ExpirationStatusFilter.EXPIRING);
                    } else {
                        permission.setStatus(ExpirationStatusFilter.NOT_EXPIRED);
                    }
                    return permission;
                });
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(hasAnyResourceRole = {"OWNER", "DBA"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public List<UserDatabasePermission> batchCreate(@NotNull Long projectId,
            @NotNull @Valid CreateDatabasePermissionReq req) {
        Set<Long> projectIds = projectService.getMemberProjectIds(req.getUserId());
        if (!projectIds.contains(projectId)) {
            throw new AccessDeniedException();
        }
        Map<Long, Database> id2Database = databaseService.listDatabasesByIds(req.getDatabaseIds()).stream()
                .collect(Collectors.toMap(Database::getId, d -> d, (d1, d2) -> d1));
        for (Long databaseId : req.getDatabaseIds()) {
            if (!id2Database.containsKey(databaseId)) {
                throw new NotFoundException(ResourceType.ODC_DATABASE, "id", databaseId);
            }
            Project project = id2Database.get(databaseId).getProject();
            if (project == null || !Objects.equals(project.getId(), projectId)) {
                throw new AccessDeniedException();
            }
        }
        List<PermissionEntity> permissionEntities = new ArrayList<>();
        Long organizationId = authenticationFacade.currentOrganizationId();
        Long creatorId = authenticationFacade.currentUserId();
        Date expireTime = req.getExpireTime() == null ? TimeUtils.getMySQLMaxDatetime()
                : TimeUtils.getEndOfDay(req.getExpireTime());
        for (Long databaseId : req.getDatabaseIds()) {
            for (DatabasePermissionType permissionType : req.getTypes()) {
                PermissionEntity entity = new PermissionEntity();
                entity.setAction(permissionType.getAction());
                entity.setResourceIdentifier(ResourceType.ODC_DATABASE.name() + ":" + databaseId);
                entity.setType(PermissionType.PUBLIC_RESOURCE);
                entity.setCreatorId(creatorId);
                entity.setOrganizationId(organizationId);
                entity.setBuiltIn(false);
                entity.setExpireTime(expireTime);
                entity.setAuthorizationType(AuthorizationType.USER_AUTHORIZATION);
                entity.setResourceType(ResourceType.ODC_DATABASE);
                entity.setResourceId(databaseId);
                permissionEntities.add(entity);
            }
        }
        List<PermissionEntity> saved = permissionRepository.batchCreate(permissionEntities);
        List<UserPermissionEntity> userPermissionEntities = new ArrayList<>();
        for (PermissionEntity permissionEntity : saved) {
            UserPermissionEntity userPermissionEntity = new UserPermissionEntity();
            userPermissionEntity.setUserId(req.getUserId());
            userPermissionEntity.setPermissionId(permissionEntity.getId());
            userPermissionEntity.setCreatorId(creatorId);
            userPermissionEntity.setOrganizationId(organizationId);
            userPermissionEntities.add(userPermissionEntity);
        }
        userPermissionRepository.batchCreate(userPermissionEntities);
        return saved.stream().map(PermissionEntity::getId).map(UserDatabasePermission::from)
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(hasAnyResourceRole = {"OWNER", "DBA"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public List<UserDatabasePermission> batchRevoke(@NotNull Long projectId, @NotEmpty List<Long> ids) {
        List<UserDatabasePermissionEntity> entities =
                userDatabasePermissionRepository.findByProjectIdAndIdIn(projectId, ids);
        List<Long> permissionIds =
                entities.stream().map(UserDatabasePermissionEntity::getId).collect(Collectors.toList());
        permissionRepository.deleteByIds(permissionIds);
        userPermissionRepository.deleteByPermissionIds(permissionIds);
        return permissionIds.stream().map(UserDatabasePermission::from).collect(Collectors.toList());
    }

}
