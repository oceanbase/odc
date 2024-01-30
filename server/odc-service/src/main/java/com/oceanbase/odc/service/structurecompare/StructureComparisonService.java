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
package com.oceanbase.odc.service.structurecompare;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.AccessDeniedException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.connection.DatabaseEntity;
import com.oceanbase.odc.metadb.connection.DatabaseRepository;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.structurecompare.StructureComparisonEntity;
import com.oceanbase.odc.metadb.structurecompare.StructureComparisonEntitySpecs;
import com.oceanbase.odc.metadb.structurecompare.StructureComparisonRepository;
import com.oceanbase.odc.metadb.structurecompare.StructureComparisonTaskEntity;
import com.oceanbase.odc.metadb.structurecompare.StructureComparisonTaskRepository;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.util.ConnectionMapper;
import com.oceanbase.odc.service.flow.ApprovalPermissionService;
import com.oceanbase.odc.service.flow.task.model.DBObjectStructureComparisonResp;
import com.oceanbase.odc.service.flow.task.model.DBStructureComparisonParameter;
import com.oceanbase.odc.service.flow.task.model.DBStructureComparisonParameter.ComparisonScope;
import com.oceanbase.odc.service.flow.task.model.DBStructureComparisonParameter.DBStructureComparisonMapper;
import com.oceanbase.odc.service.flow.task.model.DBStructureComparisonResp;
import com.oceanbase.odc.service.flow.task.model.DBStructureComparisonResp.ObjectComparisonResult;
import com.oceanbase.odc.service.flow.task.model.DBStructureComparisonResp.OperationType;
import com.oceanbase.odc.service.flow.task.model.DBStructureComparisonTaskResult;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.objectstorage.ObjectStorageFacade;
import com.oceanbase.odc.service.session.factory.DruidDataSourceFactory;
import com.oceanbase.odc.service.structurecompare.model.DBStructureComparisonConfig;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author jingtian
 * @date 2024/1/18
 * @since ODC_release_4.2.4
 */
@Slf4j
@Service
@SkipAuthorize("permission check inside")
public class StructureComparisonService {
    @Autowired
    @Qualifier("structureComparisonTaskExecutor")
    private ThreadPoolTaskExecutor executor;
    @Autowired
    private StructureComparisonTaskRepository structureComparisonTaskRepository;
    @Autowired
    private DatabaseRepository databaseRepository;
    @Autowired
    private StructureComparisonRepository structureComparisonRepository;
    @Autowired
    private ObjectStorageFacade objectStorageFacade;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private ApprovalPermissionService approvalPermissionService;
    private final ConnectionMapper connectionMapper = ConnectionMapper.INSTANCE;


    public StructureComparisonContext create(@NonNull DBStructureComparisonParameter parameters, @NonNull Long taskId,
            @NonNull Long creatorId, @NonNull Long flowInstanceId) {
        StructureComparisonTaskEntity structureComparisonTaskEntity = structureComparisonTaskRepository.saveAndFlush(
                DBStructureComparisonMapper.ofTaskEntity(parameters, creatorId, flowInstanceId));

        log.info("StructureComparisonService create a new structure comparison task entity, id={}",
                structureComparisonTaskEntity.getId());
        DBStructureComparisonConfig srcConfig = initSourceComparisonConfig(parameters);
        DBStructureComparisonConfig tgtConfig = initTargetComparisonConfig(parameters);

        StructureComparisonTask task =
                new StructureComparisonTask(srcConfig, tgtConfig, taskId, structureComparisonTaskEntity.getId(),
                        authenticationFacade.currentUser(), structureComparisonTaskRepository,
                        structureComparisonRepository, objectStorageFacade);
        Future<DBStructureComparisonTaskResult> resultFuture = executor.submit(task);

        return new StructureComparisonContext(task, resultFuture);
    }

    private DBStructureComparisonConfig initSourceComparisonConfig(DBStructureComparisonParameter parameters) {
        DBStructureComparisonConfig srcConfig = new DBStructureComparisonConfig();

        DatabaseEntity database = getDatabaseEntityByDatabaseId(parameters.getSourceDatabaseId());
        ConnectionConfig connectionConfig = getConnectionConfigByDatabaseEntity(database);

        srcConfig.setSchemaName(database.getName());
        srcConfig.setConnectType(connectionConfig.getType());
        srcConfig.setDataSource(new DruidDataSourceFactory(connectionConfig).getDataSource());
        srcConfig.setToComparedObjectTypes(Collections.singleton(DBObjectType.TABLE));
        if (parameters.getComparisonScope() == ComparisonScope.PART) {
            Map<DBObjectType, Set<String>> blackListMap = new HashMap<>();
            blackListMap.put(DBObjectType.TABLE, new HashSet<>(parameters.getTableNamesToBeCompared()));
            srcConfig.setBlackListMap(blackListMap);
        }

        return srcConfig;
    }

    private DBStructureComparisonConfig initTargetComparisonConfig(DBStructureComparisonParameter parameters) {
        DBStructureComparisonConfig tgtConfig = new DBStructureComparisonConfig();

        DatabaseEntity database = getDatabaseEntityByDatabaseId(parameters.getTargetDatabaseId());
        ConnectionConfig connectionConfig = getConnectionConfigByDatabaseEntity(database);
        tgtConfig.setSchemaName(database.getName());
        tgtConfig.setConnectType(connectionConfig.getType());
        tgtConfig.setDataSource(new DruidDataSourceFactory(connectionConfig).getDataSource());
        return tgtConfig;
    }

    private DatabaseEntity getDatabaseEntityByDatabaseId(Long databaseId) {
        return databaseRepository.findById(databaseId).orElseThrow(
                () -> new NotFoundException(ResourceType.ODC_DATABASE, "source database id", databaseId));
    }

    private ConnectionConfig getConnectionConfigByDatabaseEntity(DatabaseEntity databaseEntity) {
        Long connectionId = databaseEntity.getConnectionId();
        ConnectionConfig connectionConfig = connectionService.getForConnectionSkipPermissionCheck(
                connectionId);
        Verify.notNull(connectionConfig, "ConnectionConfig");
        return connectionConfig;
    }

    public DBStructureComparisonResp getDBStructureComparisonResult(@NonNull Long id, OperationType operationType,
            String dbObjectName,
            @NotNull Pageable pageable) {
        StructureComparisonTaskEntity taskEntity = structureComparisonTaskRepository.findById(id).orElseThrow(
                () -> new NotFoundException(ResourceType.ODC_STRUCTURE_COMPARISON_TASK, "id", id));
        checkPermission(taskEntity);

        Page<StructureComparisonEntity> entities;
        Specification<StructureComparisonEntity> specification =
                Specification.where(StructureComparisonEntitySpecs.comparisonTaskIdEquals(id));
        if (operationType != null) {
            specification = specification
                    .and(StructureComparisonEntitySpecs.comparisonResultEquals(operationType.getComparisonResult()));
        }
        if (dbObjectName != null) {
            specification = specification.and(StructureComparisonEntitySpecs.dbObjectNameLike(dbObjectName));
        }
        entities = structureComparisonRepository.findAll(specification, pageable);

        DBStructureComparisonResp resp = new DBStructureComparisonResp();
        resp.setId(id);
        resp.setTotalChangeScript(taskEntity.getTotalChangeSqlScript());
        resp.setStorageObjectId(taskEntity.getStorageObjectId());
        if (!entities.isEmpty()) {
            resp.setComparisonResults(Responses.paginated(entities.map(ObjectComparisonResult::fromEntity)));
        }

        return resp;
    }

    public DBObjectStructureComparisonResp getDBObjectStructureComparisonResult(@NonNull Long comparisonTaskId,
            @NonNull Long id) {
        StructureComparisonTaskEntity taskEntity =
                structureComparisonTaskRepository.findById(comparisonTaskId).orElseThrow(
                        () -> new NotFoundException(ResourceType.ODC_STRUCTURE_COMPARISON_TASK, "id", id));
        checkPermission(taskEntity);
        StructureComparisonEntity entity =
                structureComparisonRepository.findByIdAndComparisonTaskId(id, comparisonTaskId);
        return DBObjectStructureComparisonResp.fromEntity(entity);
    }

    /**
     * Permission verification, the following two situations allow you to view the structure comparison
     * results: 1. The current user is the creator of the structure comparison task 2. The current user
     * is the approver of the structure comparison task
     */
    private void checkPermission(StructureComparisonTaskEntity taskEntity) {
        if (currentUserId().equals(taskEntity.getCreatorId())) {
            return;
        }
        Map<Long, Set<UserEntity>> flowInstanceId2Users = approvalPermissionService
                .getApproverByFlowInstanceIds(Collections.singleton(taskEntity.getFlowInstanceId()));
        Set<Long> approvalUserIds =
                flowInstanceId2Users.get(taskEntity.getFlowInstanceId()).stream().filter(Objects::nonNull)
                        .map(UserEntity::getId).collect(
                                Collectors.toSet());
        if (approvalUserIds.contains(currentUserId())) {
            return;
        }
        throw new AccessDeniedException("The permission verification of the structure comparison task failed.");
    }

    private Long currentUserId() {
        return authenticationFacade.currentUserId();
    }

}
