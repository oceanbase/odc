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
package com.oceanbase.odc.service.connection.logicaldatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.connection.ConnectionConfigRepository;
import com.oceanbase.odc.metadb.connection.ConnectionEntity;
import com.oceanbase.odc.metadb.connection.DatabaseEntity;
import com.oceanbase.odc.metadb.connection.DatabaseRepository;
import com.oceanbase.odc.metadb.connection.logicaldatabase.DatabaseMappingEntity;
import com.oceanbase.odc.metadb.connection.logicaldatabase.DatabaseMappingRepository;
import com.oceanbase.odc.metadb.connection.logicaldatabase.TableMappingRepository;
import com.oceanbase.odc.metadb.dbobject.DBObjectRepository;
import com.oceanbase.odc.service.collaboration.environment.EnvironmentService;
import com.oceanbase.odc.service.collaboration.environment.model.Environment;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.database.model.DatabaseSyncStatus;
import com.oceanbase.odc.service.connection.database.model.DatabaseType;
import com.oceanbase.odc.service.connection.logicaldatabase.model.CreateLogicalDatabaseReq;
import com.oceanbase.odc.service.connection.logicaldatabase.model.DetailLogicalDatabaseResp;
import com.oceanbase.odc.service.db.schema.model.DBObjectSyncStatus;
import com.oceanbase.odc.service.iam.ProjectPermissionValidator;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.permission.DBResourcePermissionHelper;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2024/5/8 17:33
 * @Description: []
 */
@Service
@SkipAuthorize
@Slf4j
@Validated
public class LogicalDatabaseService {

    @Autowired
    private ProjectPermissionValidator projectPermissionValidator;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private DatabaseMappingRepository databaseMappingRepository;

    @Autowired
    private DBObjectRepository dbObjectRepository;

    @Autowired
    private TableMappingRepository tableMappingRepository;

    @Autowired
    private ConnectionConfigRepository connectionRepository;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private LogicalTableService tableService;

    @Autowired
    private LogicalDatabaseSyncManager syncManager;

    @Autowired
    private DBResourcePermissionHelper permissionHelper;


    @Transactional(rollbackFor = Exception.class)
    public Boolean create(@Valid CreateLogicalDatabaseReq req) {
        preCheck(req);

        Long organizationId = authenticationFacade.currentOrganizationId();
        DatabaseEntity basePhysicalDatabase = databaseRepository
                .findById(req.getPhysicalDatabaseIds().iterator().next()).orElseThrow(() -> new NotFoundException(
                        ResourceType.ODC_DATABASE, "id", req.getPhysicalDatabaseIds().iterator().next()));
        ConnectionEntity baseConnection = connectionRepository.findById(basePhysicalDatabase.getConnectionId())
                .orElseThrow(() -> new NotFoundException(
                        ResourceType.ODC_CONNECTION, "id", basePhysicalDatabase.getConnectionId()));
        DatabaseEntity logicalDatabase = new DatabaseEntity();
        logicalDatabase.setProjectId(req.getProjectId());
        logicalDatabase.setName(req.getName());
        logicalDatabase.setAlias(req.getAlias());
        logicalDatabase.setEnvironmentId(basePhysicalDatabase.getEnvironmentId());
        logicalDatabase.setDatabaseId(StringUtils.uuid());
        logicalDatabase.setType(DatabaseType.LOGICAL);
        logicalDatabase.setDialectType(baseConnection.getDialectType());
        logicalDatabase.setSyncStatus(DatabaseSyncStatus.INITIALIZED);
        logicalDatabase.setObjectSyncStatus(DBObjectSyncStatus.INITIALIZED);
        logicalDatabase.setExisted(true);
        logicalDatabase.setOrganizationId(organizationId);
        DatabaseEntity savedLogicalDatabase = databaseRepository.saveAndFlush(logicalDatabase);

        List<DatabaseMappingEntity> mappings = new ArrayList<>();
        req.getPhysicalDatabaseIds().stream().forEach(physicalDatabaseId -> {
            DatabaseMappingEntity relation = new DatabaseMappingEntity();
            relation.setLogicalDatabaseId(savedLogicalDatabase.getId());
            relation.setPhysicalDatabaseId(physicalDatabaseId);
            relation.setOrganizationId(organizationId);
            mappings.add(relation);
        });
        databaseMappingRepository.batchCreate(mappings);

        return true;
    }

    public DetailLogicalDatabaseResp detail(@NotNull Long id) {
        DatabaseEntity logicalDatabase = databaseRepository.findById(id).orElseThrow(() -> new NotFoundException(
                ResourceType.ODC_DATABASE, "id", id));
        Verify.equals(DatabaseType.LOGICAL, logicalDatabase.getType(), "database type");
        projectPermissionValidator.checkProjectRole(logicalDatabase.getProjectId(), ResourceRoleName.all());

        Environment environment = environmentService.detailSkipPermissionCheck(logicalDatabase.getEnvironmentId());

        Set<Long> physicalDBIds =
                databaseMappingRepository.findByLogicalDatabaseId(logicalDatabase.getId()).stream()
                        .map(DatabaseMappingEntity::getPhysicalDatabaseId).collect(Collectors.toSet());
        List<Database> physicalDatabases = databaseService.listDatabasesByIds(physicalDBIds);

        DetailLogicalDatabaseResp resp = new DetailLogicalDatabaseResp();
        resp.setId(logicalDatabase.getId());
        resp.setName(logicalDatabase.getName());
        resp.setAlias(logicalDatabase.getAlias());
        resp.setDialectType(logicalDatabase.getDialectType());
        resp.setEnvironment(environment);
        resp.setPhysicalDatabases(physicalDatabases);
        resp.setLogicalTables(tableService.list(logicalDatabase.getId()));

        return resp;
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(@NotNull Long id) {
        DatabaseEntity logicalDatabase = databaseRepository.findById(id).orElseThrow(() -> new NotFoundException(
                ResourceType.ODC_DATABASE, "id", id));
        Verify.equals(DatabaseType.LOGICAL, logicalDatabase.getType(), "database type");
        projectPermissionValidator.checkProjectRole(logicalDatabase.getProjectId(),
                Arrays.asList(ResourceRoleName.DBA, ResourceRoleName.OWNER));

        databaseRepository.deleteById(id);
        Set<Long> physicalDBIds = databaseMappingRepository.findByLogicalDatabaseId(id).stream()
                .map(DatabaseMappingEntity::getId).collect(
                        Collectors.toSet());
        databaseMappingRepository.deleteByLogicalDatabaseId(id);
        dbObjectRepository.deleteByDatabaseIdIn(Collections.singleton(id));
        tableMappingRepository.deleteByPhysicalDatabaseIds(physicalDBIds);
        return true;
    }

    public boolean extractLogicalTables(@NotNull Long logicalDatabaseId) {
        Database logicalDatabase =
                databaseService.getBasicSkipPermissionCheck(logicalDatabaseId);
        Verify.equals(logicalDatabase.getType(), DatabaseType.LOGICAL, "database type");
        projectPermissionValidator.checkProjectRole(logicalDatabase.getProject().getId(),
                Arrays.asList(ResourceRoleName.DBA, ResourceRoleName.OWNER));
        try {
            syncManager.submitExtractLogicalTablesTask(logicalDatabase);
        } catch (TaskRejectedException ex) {
            log.warn("submit extract logical tables task rejected, logical database id={}", logicalDatabaseId);
            return false;
        }
        return true;
    }

    protected void preCheck(CreateLogicalDatabaseReq req) {
        projectPermissionValidator.checkProjectRole(req.getProjectId(),
                Arrays.asList(ResourceRoleName.DBA, ResourceRoleName.OWNER));
        List<DatabaseEntity> databases = databaseRepository.findByIdIn(req.getPhysicalDatabaseIds());
        Verify.equals(databases.size(), req.getPhysicalDatabaseIds().size(), "physical database");

        if (!databases.stream().allMatch(database -> Objects.equals(req.getProjectId(), database.getProjectId()))) {
            throw new BadRequestException(
                    "physical databases not all in the same project, project id=" + req.getProjectId());
        }

        if (databases.stream().map(DatabaseEntity::getEnvironmentId).collect(Collectors.toSet()).size() > 1) {
            throw new BadRequestException("physical databases are not all the same environment");
        }

        List<ConnectionEntity> connections = connectionRepository
                .findByIdIn(databases.stream().map(DatabaseEntity::getConnectionId).collect(Collectors.toSet()));
        if (connections.stream().map(ConnectionEntity::getDialectType).collect(Collectors.toSet()).size() > 1) {
            throw new BadRequestException("physical databases are not all the same dialect type");
        }

        Set<String> aliasNames = databaseRepository.findByProjectId(req.getProjectId()).stream()
                .map(DatabaseEntity::getAlias).collect(Collectors.toSet());
        if (aliasNames.contains(req.getAlias())) {
            throw new BadRequestException("alias name already exists, alias=" + req.getAlias());
        }
    }
}
