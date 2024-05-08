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
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.connection.ConnectionConfigRepository;
import com.oceanbase.odc.metadb.connection.ConnectionEntity;
import com.oceanbase.odc.metadb.connection.DatabaseEntity;
import com.oceanbase.odc.metadb.connection.DatabaseRepository;
import com.oceanbase.odc.metadb.connection.logicaldatabase.LogicalDBPhysicalDBEntity;
import com.oceanbase.odc.metadb.connection.logicaldatabase.LogicalDBPhysicalDBRepository;
import com.oceanbase.odc.metadb.connection.logicaldatabase.LogicalDatabaseMetaEntity;
import com.oceanbase.odc.metadb.connection.logicaldatabase.LogicalDatabaseMetaRepository;
import com.oceanbase.odc.service.connection.database.model.DatabaseSyncStatus;
import com.oceanbase.odc.service.connection.database.model.DatabaseType;
import com.oceanbase.odc.service.connection.logicaldatabase.model.CreateLogicalDatabaseReq;
import com.oceanbase.odc.service.iam.ProjectPermissionValidator;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

/**
 * @Author: Lebie
 * @Date: 2024/5/8 17:33
 * @Description: []
 */
@Service
public class LogicalDatabaseService {

    @Autowired
    private ProjectPermissionValidator projectPermissionValidator;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private LogicalDatabaseMetaRepository logicalDatabaseMetaRepository;

    @Autowired
    private LogicalDBPhysicalDBRepository logicalDBPhysicalDBRepository;

    @Autowired
    private ConnectionConfigRepository connectionRepository;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    public Boolean create(CreateLogicalDatabaseReq req) {
        preCheck(req);

        Long organizationId = authenticationFacade.currentOrganizationId();
        DatabaseEntity basePhysicalDatabase = databaseRepository
                .findById(req.getPhysicalDatabaseIds().iterator().next()).orElseThrow(() -> new NotFoundException(
                        ResourceType.ODC_DATABASE, "id", req.getPhysicalDatabaseIds().iterator().next()));
        DatabaseEntity logicalDatabase = new DatabaseEntity();
        logicalDatabase.setProjectId(req.getProjectId());
        logicalDatabase.setAlias(req.getAlias());
        logicalDatabase.setEnvironmentId(basePhysicalDatabase.getEnvironmentId());
        logicalDatabase.setDatabaseId(StringUtils.uuid());
        logicalDatabase.setType(DatabaseType.LOGICAL);
        logicalDatabase.setSyncStatus(DatabaseSyncStatus.INITIALIZED);
        logicalDatabase.setExisted(true);
        logicalDatabase.setOrganizationId(organizationId);
        DatabaseEntity savedLogicalDatabase = databaseRepository.save(logicalDatabase);

        ConnectionEntity baseConnection = connectionRepository.findById(basePhysicalDatabase.getConnectionId())
                .orElseThrow(() -> new NotFoundException(
                        ResourceType.ODC_CONNECTION, "id", basePhysicalDatabase.getConnectionId()));
        LogicalDatabaseMetaEntity logicalDatabaseMeta = new LogicalDatabaseMetaEntity();
        logicalDatabaseMeta.setDatabaseId(savedLogicalDatabase.getId());
        logicalDatabaseMeta.setEnvironmentId(savedLogicalDatabase.getEnvironmentId());
        logicalDatabaseMeta.setDialectType(baseConnection.getDialectType());
        logicalDatabaseMeta.setOrganizationId(organizationId);
        logicalDatabaseMetaRepository.save(logicalDatabaseMeta);

        List<LogicalDBPhysicalDBEntity> relations = new ArrayList<>();
        req.getPhysicalDatabaseIds().stream().forEach(physicalDatabaseId -> {
            LogicalDBPhysicalDBEntity relation = new LogicalDBPhysicalDBEntity();
            relation.setLogicalDatabaseId(savedLogicalDatabase.getId());
            relation.setPhysicalDatabaseId(physicalDatabaseId);
            relation.setOrganizationId(organizationId);
            relations.add(relation);
        });
        logicalDBPhysicalDBRepository.batchCreate(relations);

        return true;
    }



    private void preCheck(CreateLogicalDatabaseReq req) {
        projectPermissionValidator.checkProjectRole(req.getProjectId(),
                Arrays.asList(ResourceRoleName.OWNER, ResourceRoleName.DBA));
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
