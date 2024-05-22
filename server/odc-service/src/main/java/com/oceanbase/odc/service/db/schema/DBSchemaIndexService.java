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
package com.oceanbase.odc.service.db.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.google.common.collect.Ordering;
import com.oceanbase.odc.common.jpa.SpecificationUtil;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.dbobject.DBColumnEntity;
import com.oceanbase.odc.metadb.dbobject.DBColumnEntity_;
import com.oceanbase.odc.metadb.dbobject.DBColumnRepository;
import com.oceanbase.odc.metadb.dbobject.DBObjectEntity;
import com.oceanbase.odc.metadb.dbobject.DBObjectEntity_;
import com.oceanbase.odc.metadb.dbobject.DBObjectRepository;
import com.oceanbase.odc.service.collaboration.project.ProjectService;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.schema.model.DBObjectSyncStatus;
import com.oceanbase.odc.service.db.schema.model.OdcDBColumn;
import com.oceanbase.odc.service.db.schema.model.OdcDBObject;
import com.oceanbase.odc.service.db.schema.model.QueryDBObjectParams;
import com.oceanbase.odc.service.db.schema.model.QueryDBObjectResp;
import com.oceanbase.odc.service.db.schema.model.SyncDBObjectReq;
import com.oceanbase.odc.service.iam.ProjectPermissionValidator;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

import lombok.NonNull;

/**
 * @author gaoda.xy
 * @date 2024/3/28 13:41
 */
@Service
@Validated
@SkipAuthorize("permission check inside")
public class DBSchemaIndexService {

    @Autowired
    private ProjectPermissionValidator projectPermissionValidator;
    @Autowired
    private DatabaseService databaseService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private DBColumnRepository dbColumnRepository;
    @Autowired
    private DBObjectRepository dbObjectRepository;
    @Autowired
    private DBSchemaSyncTaskManager dbSchemaSyncTaskManager;

    private static final int MAX_SEARCH_SIZE = 5000;
    private static final int MAX_RETURN_SIZE_PER_TYPE = 200;

    public QueryDBObjectResp listDatabaseObjects(@NonNull @Valid QueryDBObjectParams params) {
        QueryDBObjectResp resp = new QueryDBObjectResp();
        Set<Long> queryDatabaseIds = new HashSet<>();
        if (params.getProjectId() != null && params.getDatasourceId() != null) {
            throw new IllegalArgumentException("projectId and datasourceId cannot be set at the same time");
        }
        if (params.getProjectId() != null) {
            projectPermissionValidator.checkProjectRole(params.getProjectId(), ResourceRoleName.all());
            if (CollectionUtils.isNotEmpty(params.getDatabaseIds())) {
                List<Database> databases = databaseService.listDatabasesByIds(params.getDatabaseIds());
                databases.forEach(e -> {
                    if (Objects.isNull(e.getProject())
                            || !Objects.equals(e.getProject().getId(), params.getProjectId())) {
                        throw new NotFoundException(ResourceType.ODC_DATABASE, "id", e.getId());
                    }
                });
                queryDatabaseIds.addAll(databases.stream().map(Database::getId).collect(Collectors.toSet()));
            } else {
                queryDatabaseIds.addAll(databaseService.listExistDatabaseIdsByProjectId(params.getProjectId()));
            }
        } else if (params.getDatasourceId() != null) {
            Map<Long, Database> id2Database =
                    databaseService.listExistDatabasesByConnectionId(params.getDatasourceId()).stream()
                            .collect(Collectors.toMap(Database::getId, e -> e, (e1, e2) -> e1));
            if (authenticationFacade.currentUser().getOrganizationType() == OrganizationType.INDIVIDUAL) {
                ConnectionConfig config = connectionService.getBasicWithoutPermissionCheck(params.getDatasourceId());
                if (!Objects.equals(authenticationFacade.currentUserId(), config.getCreatorId())) {
                    throw new NotFoundException(ResourceType.ODC_CONNECTION, "id", params.getDatasourceId());
                }
                if (CollectionUtils.isNotEmpty(params.getDatabaseIds())) {
                    queryDatabaseIds.addAll(params.getDatabaseIds().stream().filter(id2Database::containsKey)
                            .collect(Collectors.toSet()));
                } else {
                    queryDatabaseIds.addAll(id2Database.keySet());
                }
            } else {
                Set<Long> projectIds = projectService.getMemberProjectIds(authenticationFacade.currentUserId());
                if (CollectionUtils.isNotEmpty(params.getDatabaseIds())) {
                    queryDatabaseIds.addAll(params.getDatabaseIds().stream()
                            .filter(e -> id2Database.containsKey(e) && id2Database.get(e).getProject() != null
                                    && projectIds.contains(id2Database.get(e).getProject().getId()))
                            .collect(Collectors.toSet()));
                } else {
                    queryDatabaseIds.addAll(id2Database.values().stream()
                            .filter(e -> e.getProject() != null && projectIds.contains(e.getProject().getId()))
                            .map(Database::getId).collect(Collectors.toSet()));
                }
            }
        } else {
            throw new IllegalArgumentException("projectId or datasourceId is required");
        }
        if (CollectionUtils.isEmpty(queryDatabaseIds)) {
            return resp;
        }
        List<Database> databases = databaseService.listDatabasesDetailsByIds(queryDatabaseIds);
        Map<Long, Database> id2Database =
                databases.stream().collect(Collectors.toMap(Database::getId, e -> e, (e1, e2) -> e1));
        if (CollectionUtils.isEmpty(params.getTypes()) || params.getTypes().contains(DBObjectType.SCHEMA)) {
            String key = params.getSearchKey().toLowerCase();
            List<Database> matches = databases.stream().filter(e -> e.getName().toLowerCase().contains(key))
                    .collect(Collectors.toList());
            Ordering<Database> ordering = Ordering.natural().onResultOf(e -> e.getName().length());
            ordering = ordering.compound(Ordering.natural().onResultOf(Database::getName));
            resp.setDatabases(ordering.leastOf(matches, MAX_RETURN_SIZE_PER_TYPE));
        }
        Pageable pageable = PageRequest.of(0, MAX_SEARCH_SIZE);
        if (CollectionUtils.isEmpty(params.getTypes()) || params.getTypes().contains(DBObjectType.COLUMN)) {
            Specification<DBColumnEntity> columnSpec =
                    SpecificationUtil.columnIn(DBColumnEntity_.DATABASE_ID, queryDatabaseIds);
            columnSpec = columnSpec.and(SpecificationUtil.columnLike(DBColumnEntity_.NAME, params.getSearchKey()));
            List<DBColumnEntity> matches = dbColumnRepository.findAll(columnSpec, pageable).getContent();
            Ordering<DBColumnEntity> ordering = Ordering.natural().onResultOf(e -> e.getName().length());
            ordering = ordering.compound(Ordering.natural().onResultOf(DBColumnEntity::getName));
            matches = ordering.leastOf(matches, MAX_RETURN_SIZE_PER_TYPE);
            Set<Long> objectIds = matches.stream().map(DBColumnEntity::getObjectId).collect(Collectors.toSet());
            Map<Long, OdcDBObject> id2Object =
                    objectEntitiesToModels(dbObjectRepository.findByIdIn(objectIds), id2Database).stream()
                            .collect(Collectors.toMap(OdcDBObject::getId, e -> e, (e1, e2) -> e1));
            resp.setDbColumns(columnEntitiesToModels(matches, id2Object));
        }
        Specification<DBObjectEntity> objectSpec =
                SpecificationUtil.columnIn(DBObjectEntity_.DATABASE_ID, queryDatabaseIds);
        objectSpec = objectSpec.and(SpecificationUtil.columnLike(DBObjectEntity_.NAME, params.getSearchKey()));
        if (CollectionUtils.isNotEmpty(params.getTypes())) {
            objectSpec = objectSpec.and(SpecificationUtil.columnIn(DBObjectEntity_.TYPE, params.getTypes()));
        }
        List<DBObjectEntity> matches = dbObjectRepository.findAll(objectSpec, pageable).getContent();
        Map<DBObjectType, List<DBObjectEntity>> type2Objects =
                matches.stream().collect(Collectors.groupingBy(DBObjectEntity::getType));
        Ordering<DBObjectEntity> ordering = Ordering.natural().onResultOf(e -> e.getName().length());
        ordering = ordering.compound(Ordering.natural().onResultOf(DBObjectEntity::getName));
        List<DBObjectEntity> filtered = new ArrayList<>();
        for (Map.Entry<DBObjectType, List<DBObjectEntity>> entry : type2Objects.entrySet()) {
            filtered.addAll(ordering.leastOf(entry.getValue(), MAX_RETURN_SIZE_PER_TYPE));
        }
        resp.setDbObjects(objectEntitiesToModels(filtered, id2Database));
        return resp;
    }

    public Boolean syncDatabaseObjects(@NonNull @Valid SyncDBObjectReq req) {
        Set<Database> databases = new HashSet<>();
        if (req.getResourceType() == ResourceType.ODC_CONNECTION) {
            Set<Database> dbs = new HashSet<>(databaseService.listExistDatabasesByConnectionId(req.getResourceId()));
            if (authenticationFacade.currentUser().getOrganizationType() == OrganizationType.INDIVIDUAL) {
                ConnectionConfig config = connectionService.getBasicWithoutPermissionCheck(req.getResourceId());
                if (!Objects.equals(authenticationFacade.currentUserId(), config.getCreatorId())) {
                    throw new NotFoundException(ResourceType.ODC_CONNECTION, "id", req.getResourceId());
                }
                databases.addAll(dbs);
            } else {
                Set<Long> projectIds = projectService.getMemberProjectIds(authenticationFacade.currentUserId());
                databases.addAll(dbs.stream()
                        .filter(e -> e.getProject() != null && projectIds.contains(e.getProject().getId()))
                        .collect(Collectors.toSet()));
            }
        } else if (req.getResourceType() == ResourceType.ODC_PROJECT) {
            projectPermissionValidator.checkProjectRole(req.getResourceId(), ResourceRoleName.all());
            databases.addAll(databaseService.listExistDatabasesByProjectId(req.getResourceId()));
        } else if (req.getResourceType() == ResourceType.ODC_DATABASE) {
            List<Database> dbs = databaseService.listDatabasesByIds(Collections.singleton(req.getResourceId()));
            if (authenticationFacade.currentUser().getOrganizationType() == OrganizationType.INDIVIDUAL) {
                ConnectionConfig config =
                        connectionService.getBasicWithoutPermissionCheck(dbs.get(0).getDataSource().getCreatorId());
                if (!Objects.equals(authenticationFacade.currentUserId(), config.getCreatorId())) {
                    throw new NotFoundException(ResourceType.ODC_DATABASE, "id", req.getResourceId());
                }
                databases.addAll(dbs);
            } else {
                Set<Long> projectIds = projectService.getMemberProjectIds(authenticationFacade.currentUserId());
                if (CollectionUtils.isEmpty(dbs) || dbs.get(0).getProject() == null
                        || !projectIds.contains(dbs.get(0).getProject().getId())) {
                    throw new NotFoundException(ResourceType.ODC_DATABASE, "id", req.getResourceId());
                }
                databases.addAll(dbs);
            }
        } else {
            throw new IllegalArgumentException("Unsupported resource type: " + req.getResourceType());
        }
        databases.removeIf(e -> Boolean.FALSE.equals(e.getExisted())
                || e.getObjectSyncStatus() == DBObjectSyncStatus.PENDING);
        dbSchemaSyncTaskManager.submitTaskByDatabases(databases);
        return true;
    }

    private List<OdcDBObject> objectEntitiesToModels(List<DBObjectEntity> entities, Map<Long, Database> id2Database) {
        List<OdcDBObject> result = new ArrayList<>();
        if (CollectionUtils.isEmpty(entities)) {
            return result;
        }
        return entities.stream().map(e -> {
            OdcDBObject object = OdcDBObject.fromEntity(e);
            if (e.getDatabaseId() != null && id2Database.containsKey(e.getDatabaseId())) {
                object.setDatabase(id2Database.get(e.getDatabaseId()));
            }
            return object;
        }).collect(Collectors.toList());
    }

    private List<OdcDBColumn> columnEntitiesToModels(List<DBColumnEntity> entities, Map<Long, OdcDBObject> id2Object) {
        if (CollectionUtils.isEmpty(entities)) {
            return new ArrayList<>();
        }
        return entities.stream().map(e -> {
            OdcDBColumn column = OdcDBColumn.fromEntity(e);
            if (e.getObjectId() != null && id2Object.containsKey(e.getObjectId())) {
                column.setDbObject(id2Object.get(e.getObjectId()));
            }
            return column;
        }).collect(Collectors.toList());
    }

}
