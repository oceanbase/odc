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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

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
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.permission.database.model.CreateDatabasePermissionReq;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;
import com.oceanbase.odc.service.permission.database.model.QueryDatabasePermissionParams;
import com.oceanbase.odc.service.permission.database.model.QueryDatabasePermissionParams.PermissionExpireStatus;
import com.oceanbase.odc.service.permission.database.model.UserDatabasePermission;

import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2024/1/4 11:24
 */
@Slf4j
@Service
@Validated
@Authenticated
public class DatabasePermissionService {

    @Autowired
    private ProjectService projectService;

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

    @Value("${odc.iam.permission.expired-retention-time-seconds:7776000}")
    private long expiredRetentionTimeSeconds;

    private static final UserDatabasePermissionMapper mapper = UserDatabasePermissionMapper.INSTANCE;
    private static final int EXPIRING_DAYS = 7;

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("permission check inside")
    public Page<UserDatabasePermission> list(@NotNull Long projectId, @NotNull QueryDatabasePermissionParams params,
            Pageable pageable) {
        boolean hasPermission;
        if (params.getUserId() == null || params.getUserId() != authenticationFacade.currentUserId()) {
            hasPermission = projectService.checkPermission(projectId,
                    Arrays.asList(ResourceRoleName.OWNER, ResourceRoleName.DBA));
        } else {
            hasPermission = projectService.checkPermission(projectId, ResourceRoleName.all());
        }
        if (!hasPermission) {
            throw new AccessDeniedException();
        }
        // Delete expired permission before list
        deleteExpiredDBPermission();
        Specification<UserDatabasePermissionEntity> spec = Specification
                .where(UserDatabasePermissionSpec.projectIdEqual(projectId))
                .and(UserDatabasePermissionSpec.organizationIdEqual(authenticationFacade.currentOrganizationId()))
                .and(UserDatabasePermissionSpec.userIdEqual(params.getUserId()))
                .and(UserDatabasePermissionSpec.ticketIdEqual(params.getTicketId()))
                .and(UserDatabasePermissionSpec.databaseNameLike(params.getFuzzyDatabaseName()))
                .and(UserDatabasePermissionSpec.dataSourceNameLike(params.getFuzzyDataSourceName()))
                .and(UserDatabasePermissionSpec.typeIn(params.getTypes()))
                .and(UserDatabasePermissionSpec.authorizationTypeEqual(params.getAuthorizationType()));
        Date expireTimeThreshold = getExpireTimeThreshold();
        if (CollectionUtils.isNotEmpty(params.getStatuses())) {
            List<Specification<UserDatabasePermissionEntity>> expireSpecList = new ArrayList<>();
            for (PermissionExpireStatus status : params.getStatuses()) {
                expireSpecList.add(getSpecificationByStatus(status, expireTimeThreshold));
            }
            Specification<UserDatabasePermissionEntity> expireSpec = expireSpecList.get(0);
            for (int i = 1; i < expireSpecList.size(); i++) {
                expireSpec = expireSpec.or(expireSpecList.get(i));
            }
            spec = spec.and(expireSpec);
        }
        return userDatabasePermissionRepository.findAll(spec, pageable).map(
                e -> {
                    UserDatabasePermission permission = mapper.entityToModel(e);
                    Date expireTime = permission.getExpireTime();
                    if (expireTime == null) {
                        permission.setExpireStatus(PermissionExpireStatus.NOT_EXPIRED);
                    } else if (expireTime.before(expireTimeThreshold)) {
                        permission.setExpireStatus(PermissionExpireStatus.EXPIRED);
                    } else if (expireTime.before(DateUtils.addDays(expireTimeThreshold, EXPIRING_DAYS))) {
                        permission.setExpireStatus(PermissionExpireStatus.EXPIRING);
                    } else {
                        permission.setExpireStatus(PermissionExpireStatus.NOT_EXPIRED);
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
        for (Long databaseId : req.getDatabaseIds()) {
            for (DatabasePermissionType permissionType : req.getTypes()) {
                PermissionEntity permissionEntity = new PermissionEntity();
                permissionEntity.setAction(permissionType.getAction());
                permissionEntity.setResourceIdentifier(ResourceType.ODC_DATABASE.name() + ":" + databaseId);
                permissionEntity.setResourceType(ResourceType.ODC_DATABASE);
                permissionEntity.setResourceId(databaseId);
                permissionEntity.setType(PermissionType.PUBLIC_RESOURCE);
                permissionEntity.setCreatorId(creatorId);
                permissionEntity.setOrganizationId(organizationId);
                permissionEntity.setBuiltIn(false);
                permissionEntity.setExpireTime(getFixedExpireTime(req.getExpireTime()));
                permissionEntity.setAuthorizationType(AuthorizationType.USER_AUTHORIZATION);
                permissionEntities.add(permissionEntity);
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

    private void deleteExpiredDBPermission() {
        Date expiredTime = new Date(System.currentTimeMillis() - expiredRetentionTimeSeconds * 1000);
        int count = permissionRepository.deleteByExpireTimeBefore(expiredTime);
        log.info("Clear expired permission, count: {}, expired time: {}", count, expiredTime);
    }

    private Date getExpireTimeThreshold() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return new Date(calendar.getTimeInMillis());
    }

    private Date getFixedExpireTime(Date expireTime) {
        if (expireTime == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(expireTime);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Specification<UserDatabasePermissionEntity> getSpecificationByStatus(PermissionExpireStatus status,
            Date date) {
        switch (status) {
            case EXPIRED:
                return UserDatabasePermissionSpec.expireTimeIsNotNull()
                        .and(UserDatabasePermissionSpec.expireTimeBefore(date));
            case EXPIRING:
                return UserDatabasePermissionSpec.expireTimeIsNotNull()
                        .and(UserDatabasePermissionSpec.expireTimeLate(date))
                        .and(UserDatabasePermissionSpec.expireTimeBefore(DateUtils.addDays(date, EXPIRING_DAYS)));
            case NOT_EXPIRED:
                return UserDatabasePermissionSpec.expireTimeIsNull()
                        .or(UserDatabasePermissionSpec.expireTimeLate(date));
            default:
                throw new IllegalArgumentException("Unknown status: " + status);
        }
    }

}
