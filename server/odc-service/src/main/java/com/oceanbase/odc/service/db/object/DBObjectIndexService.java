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
package com.oceanbase.odc.service.db.object;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.exception.NotImplementedException;
import com.oceanbase.odc.metadb.dbobject.DBColumnEntity;
import com.oceanbase.odc.metadb.dbobject.DBColumnRepository;
import com.oceanbase.odc.metadb.dbobject.DBObjectEntity;
import com.oceanbase.odc.metadb.dbobject.DBObjectRepository;
import com.oceanbase.odc.service.collaboration.project.ProjectService;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.object.model.OdcDBColumn;
import com.oceanbase.odc.service.db.object.model.OdcDBObject;
import com.oceanbase.odc.service.db.object.model.QueryDBObjectParams;
import com.oceanbase.odc.service.db.object.model.QueryDBObjectResp;
import com.oceanbase.odc.service.db.object.model.SyncDBObjectReq;
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
public class DBObjectIndexService {

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

    public QueryDBObjectResp listDatabaseObjects(@NonNull @Valid QueryDBObjectParams params) {
        QueryDBObjectResp resp = new QueryDBObjectResp();
        List<Long> queryDatabaseIds = new ArrayList<>();
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
                queryDatabaseIds.addAll(databases.stream().map(Database::getId).collect(Collectors.toList()));
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
                            .collect(Collectors.toList()));
                } else {
                    queryDatabaseIds.addAll(id2Database.keySet());
                }
            } else {
                Set<Long> projectIds = projectService.getMemberProjectIds(authenticationFacade.currentUserId());
                if (CollectionUtils.isNotEmpty(params.getDatabaseIds())) {
                    queryDatabaseIds.addAll(params.getDatabaseIds().stream()
                            .filter(e -> id2Database.containsKey(e) && id2Database.get(e).getProject() != null
                                    && projectIds.contains(id2Database.get(e).getProject().getId()))
                            .collect(Collectors.toList()));
                } else {
                    queryDatabaseIds.addAll(id2Database.values().stream()
                            .filter(e -> e.getProject() != null && projectIds.contains(e.getProject().getId()))
                            .map(Database::getId).collect(Collectors.toList()));
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
            resp.setDatabases(databases.stream().filter(e -> {
                String name = e.getName().toLowerCase();
                if (key.startsWith("%") && key.endsWith("%")) {
                    return name.contains(key.substring(1, key.length() - 1));
                } else if (key.startsWith("%")) {
                    return name.endsWith(key.substring(1));
                } else if (key.endsWith("%")) {
                    return name.startsWith(key.substring(0, key.length() - 1));
                } else {
                    return e.getName().equals(key);
                }
            }).collect(Collectors.toList()));
        }
        if (CollectionUtils.isEmpty(params.getTypes()) || params.getTypes().contains(DBObjectType.COLUMN)) {
            List<DBColumnEntity> columns = dbColumnRepository.findTop1000ByDatabaseIdInAndNameLike(queryDatabaseIds,
                    params.getSearchKey());
            List<Long> objectIds = columns.stream().map(DBColumnEntity::getObjectId).collect(Collectors.toList());
            List<DBObjectEntity> objects = dbObjectRepository.findByIdIn(objectIds);
            Map<Long, OdcDBObject> id2Object = objectEntitiesToModels(objects, id2Database).stream()
                    .collect(Collectors.toMap(OdcDBObject::getId, e -> e, (e1, e2) -> e1));
            resp.setDbColumns(columnEntitiesToModels(columns, id2Object));
        }
        if (CollectionUtils.isEmpty(params.getTypes())) {
            List<DBObjectEntity> objects =
                    dbObjectRepository.findTop1000ByDatabaseIdInAndNameLike(queryDatabaseIds, params.getSearchKey());
            resp.setDbObjects(objectEntitiesToModels(objects, id2Database));
        } else {
            List<DBObjectEntity> objects =
                    dbObjectRepository.findTop1000ByDatabaseIdInAndTypeInAndNameLike(queryDatabaseIds,
                            params.getTypes().stream().map(DBObjectType::getName).collect(Collectors.toList()),
                            params.getSearchKey());
            resp.setDbObjects(objectEntitiesToModels(objects, id2Database));
        }
        return resp;
    }

    public Boolean syncDatabaseObjects(@NonNull SyncDBObjectReq req) {
        throw new NotImplementedException();
    }

    private List<OdcDBObject> objectEntitiesToModels(List<DBObjectEntity> entities, Map<Long, Database> id2Database) {
        if (CollectionUtils.isEmpty(entities)) {
            return new ArrayList<>();
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
