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
package com.oceanbase.odc.service.permission.table;

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
import org.springframework.context.annotation.Lazy;
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
import com.oceanbase.odc.metadb.dbobject.DBObjectEntity;
import com.oceanbase.odc.metadb.dbobject.DBObjectRepository;
import com.oceanbase.odc.metadb.iam.PermissionEntity;
import com.oceanbase.odc.metadb.iam.PermissionRepository;
import com.oceanbase.odc.metadb.iam.UserPermissionEntity;
import com.oceanbase.odc.metadb.iam.UserPermissionRepository;
import com.oceanbase.odc.metadb.iam.UserTablePermissionEntity;
import com.oceanbase.odc.metadb.iam.UserTablePermissionRepository;
import com.oceanbase.odc.metadb.iam.UserTablePermissionSpec;
import com.oceanbase.odc.service.collaboration.project.ProjectService;
import com.oceanbase.odc.service.collaboration.project.model.Project;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.iam.PermissionService;
import com.oceanbase.odc.service.iam.ProjectPermissionValidator;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;
import com.oceanbase.odc.service.permission.database.model.ExpirationStatusFilter;
import com.oceanbase.odc.service.permission.table.model.CreateTablePermissionReq;
import com.oceanbase.odc.service.permission.table.model.QueryTablePermissionParams;
import com.oceanbase.odc.service.permission.table.model.UserTablePermission;

import lombok.extern.slf4j.Slf4j;

/**
 *
 * @Author: fenghao
 * @Create 2024/3/11 20:49
 * @Version 1.0
 */
@Service
@Validated
@Authenticated
@Slf4j
public class TablePermissionService {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectPermissionValidator projectPermissionValidator;

    @Autowired
    @Lazy
    private DatabaseService databaseService;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @Autowired
    private UserTablePermissionRepository userTablePermissionRepository;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private DBObjectRepository dbObjectRepository;

    @Value("${odc.iam.permission.expired-retention-time-seconds:7776000}")
    private long expiredRetentionTimeSeconds;

    private static final UserTablePermissionMapper mapper = UserTablePermissionMapper.INSTANCE;
    private static final int EXPIRING_DAYS = 7;

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("permission check inside")
    public Page<UserTablePermission> list(@NotNull Long projectId, @NotNull QueryTablePermissionParams params,
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
        Specification<UserTablePermissionEntity> spec = Specification
                .where(UserTablePermissionSpec.projectIdEqual(projectId))
                .and(UserTablePermissionSpec.organizationIdEqual(authenticationFacade.currentOrganizationId()))
                .and(UserTablePermissionSpec.userIdEqual(params.getUserId()))
                .and(UserTablePermissionSpec.ticketIdEqual(params.getTicketId()))
                .and(UserTablePermissionSpec.tableNameLike(params.getFuzzyTableName()))
                .and(UserTablePermissionSpec.databaseNameLike(params.getFuzzyDatabaseName()))
                .and(UserTablePermissionSpec.dataSourceNameLike(params.getFuzzyDataSourceName()))
                .and(UserTablePermissionSpec.typeIn(params.getTypes()))
                .and(UserTablePermissionSpec.authorizationTypeEqual(params.getAuthorizationType()))
                .and(UserTablePermissionSpec.filterByExpirationStatus(params.getStatuses(), expireTimeThreshold));
        return userTablePermissionRepository.findAll(spec, pageable).map(
                e -> {
                    UserTablePermission permission = mapper.entityToModel(e);
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
    public List<UserTablePermission> batchCreate(@NotNull Long projectId,
            @NotNull @Valid CreateTablePermissionReq req) {
        Set<Long> projectIds = projectService.getMemberProjectIds(req.getUserId());
        if (!projectIds.contains(projectId)) {
            throw new AccessDeniedException();
        }
        List<DBObjectEntity> tables = dbObjectRepository.findByIdIn(req.getTableIds());
        Map<Long, DBObjectEntity> id2TableEntity = tables.stream()
                .collect(Collectors.toMap(DBObjectEntity::getId, t -> t, (t1, t2) -> t1));
        Map<Long, Database> id2Database = databaseService
                .listDatabasesByIds(tables.stream().map(DBObjectEntity::getDatabaseId).collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(Database::getId, d -> d, (d1, d2) -> d1));
        for (Long tableId : req.getTableIds()) {
            DBObjectEntity table = id2TableEntity.get(tableId);
            if (table == null) {
                throw new NotFoundException(ResourceType.ODC_TABLE, "id", tableId);
            }
            Project project = id2Database.get(table.getDatabaseId()).getProject();
            if (project == null || !Objects.equals(project.getId(), projectId)) {
                throw new AccessDeniedException();
            }
        }
        List<PermissionEntity> permissionEntities = new ArrayList<>();
        Long organizationId = authenticationFacade.currentOrganizationId();
        Long creatorId = authenticationFacade.currentUserId();
        Date expireTime = req.getExpireTime() == null ? TimeUtils.getMySQLMaxDatetime()
                : TimeUtils.getEndOfDay(req.getExpireTime());
        for (Long tableId : req.getTableIds()) {
            DBObjectEntity table = id2TableEntity.get(tableId);
            for (DatabasePermissionType permissionType : req.getTypes()) {
                PermissionEntity entity = new PermissionEntity();
                entity.setAction(permissionType.getAction());
                entity.setResourceIdentifier(ResourceType.ODC_DATABASE.name() + ":" + table.getDatabaseId() + "/"
                        + ResourceType.ODC_TABLE.name() + ":" + table.getId());
                entity.setType(PermissionType.PUBLIC_RESOURCE);
                entity.setCreatorId(creatorId);
                entity.setOrganizationId(organizationId);
                entity.setBuiltIn(false);
                entity.setExpireTime(expireTime);
                entity.setAuthorizationType(AuthorizationType.USER_AUTHORIZATION);
                entity.setResourceType(ResourceType.ODC_TABLE);
                entity.setResourceId(table.getId());
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
        return saved.stream().map(PermissionEntity::getId).map(UserTablePermission::from).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(hasAnyResourceRole = {"OWNER", "DBA"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public List<UserTablePermission> batchRevoke(@NotNull Long projectId, @NotEmpty List<Long> ids) {
        List<UserTablePermissionEntity> entities =
                userTablePermissionRepository.findByProjectIdAndIdIn(projectId, ids);
        List<Long> permissionIds =
                entities.stream().map(UserTablePermissionEntity::getId).collect(Collectors.toList());
        permissionRepository.deleteByIds(permissionIds);
        userPermissionRepository.deleteByPermissionIds(permissionIds);
        return permissionIds.stream().map(UserTablePermission::from).collect(Collectors.toList());
    }

}
