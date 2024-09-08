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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
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
import com.oceanbase.odc.service.common.util.SqlUtils;
import com.oceanbase.odc.service.connection.database.DatabaseMapper;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.database.model.DatabaseSyncStatus;
import com.oceanbase.odc.service.connection.database.model.DatabaseType;
import com.oceanbase.odc.service.connection.logicaldatabase.core.model.DataNode;
import com.oceanbase.odc.service.connection.logicaldatabase.core.parser.LogicalTableExpressionParseUtils;
import com.oceanbase.odc.service.connection.logicaldatabase.core.rewrite.RelationFactorRewriter;
import com.oceanbase.odc.service.connection.logicaldatabase.core.rewrite.RewriteContext;
import com.oceanbase.odc.service.connection.logicaldatabase.core.rewrite.RewriteResult;
import com.oceanbase.odc.service.connection.logicaldatabase.core.rewrite.SqlRewriter;
import com.oceanbase.odc.service.connection.logicaldatabase.model.CreateLogicalDatabaseReq;
import com.oceanbase.odc.service.connection.logicaldatabase.model.DetailLogicalDatabaseResp;
import com.oceanbase.odc.service.connection.logicaldatabase.model.DetailLogicalTableResp;
import com.oceanbase.odc.service.connection.logicaldatabase.model.PreviewSqlReq;
import com.oceanbase.odc.service.connection.logicaldatabase.model.PreviewSqlResp;
import com.oceanbase.odc.service.db.schema.model.DBObjectSyncStatus;
import com.oceanbase.odc.service.iam.ProjectPermissionValidator;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.permission.DBResourcePermissionHelper;
import com.oceanbase.odc.service.session.util.DBSchemaExtractor;
import com.oceanbase.odc.service.session.util.DBSchemaExtractor.DBSchemaIdentity;
import com.oceanbase.tools.dbbrowser.parser.SqlParser;
import com.oceanbase.tools.dbbrowser.parser.constant.SqlType;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;

import lombok.NonNull;
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
    private final DatabaseMapper databaseMapper = DatabaseMapper.INSTANCE;

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
    public Database create(@Valid CreateLogicalDatabaseReq req) {
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
        logicalDatabase.setConnectType(baseConnection.getType());
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

        return databaseMapper.entityToModel(savedLogicalDatabase);
    }

    public DetailLogicalDatabaseResp detail(@NotNull Long id) {
        DatabaseEntity logicalDatabase = databaseRepository.findById(id).orElseThrow(() -> new NotFoundException(
                ResourceType.ODC_DATABASE, "id", id));
        Verify.equals(DatabaseType.LOGICAL, logicalDatabase.getType(), "database type");
        projectPermissionValidator.checkProjectRole(logicalDatabase.getProjectId(), ResourceRoleName.all());

        Environment environment = environmentService.detailSkipPermissionCheck(logicalDatabase.getEnvironmentId());

        List<Database> physicalDatabases = listPhysicalDatabases(logicalDatabase.getId());
        DetailLogicalDatabaseResp resp = new DetailLogicalDatabaseResp();
        resp.setId(logicalDatabase.getId());
        resp.setName(logicalDatabase.getName());
        resp.setAlias(logicalDatabase.getAlias());
        resp.setDialectType(logicalDatabase.getConnectType().getDialectType());
        resp.setEnvironment(environment);
        resp.setPhysicalDatabases(physicalDatabases);
        resp.setLogicalTables(tableService.list(logicalDatabase.getId()));

        return resp;
    }

    public List<Database> listPhysicalDatabases(@NotNull Long logicalDatabaseId) {
        Set<Long> physicalDBIds =
                databaseMappingRepository.findByLogicalDatabaseId(logicalDatabaseId).stream()
                        .map(DatabaseMappingEntity::getPhysicalDatabaseId).collect(Collectors.toSet());
        return databaseService.listDatabasesByIds(physicalDBIds);
    }

    public Set<Long> listDataSourceIds(@NotNull Long logicalDatabaseId) {
        Set<Long> physicalDBIds =
                databaseMappingRepository.findByLogicalDatabaseId(logicalDatabaseId).stream()
                        .map(DatabaseMappingEntity::getPhysicalDatabaseId).collect(Collectors.toSet());
        return databaseRepository.findAllById(physicalDBIds).stream().map(DatabaseEntity::getConnectionId)
                .collect(Collectors.toSet());
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
                .map(DatabaseMappingEntity::getPhysicalDatabaseId).collect(
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

    public List<PreviewSqlResp> preview(@NonNull Long logicalDatabaseId, @NonNull PreviewSqlReq req) {
        DetailLogicalDatabaseResp logicalDatabase = detail(logicalDatabaseId);
        Set<DataNode> allDataNodes = logicalDatabase.getLogicalTables().stream()
                .map(DetailLogicalTableResp::getAllPhysicalTables).flatMap(List::stream)
                .collect(Collectors.toSet());
        Map<Long, List<String>> databaseId2Sqls = new HashMap<>();
        String delimiter = StringUtils.isEmpty(req.getDelimiter()) ? ";" : req.getDelimiter();
        List<String> sqls =
                SqlUtils.split(logicalDatabase.getDialectType(), req.getSql(), delimiter);
        SqlRewriter sqlRewriter = new RelationFactorRewriter();
        for (String sql : sqls) {
            Statement statement = SqlParser.parseMysqlStatement(sql);
            Set<DataNode> dataNodesToExecute;
            if (statement instanceof CreateTable) {
                dataNodesToExecute = getDataNodesFromCreateTable(sql, logicalDatabase.getDialectType(), allDataNodes);
            } else {
                dataNodesToExecute =
                        getDataNodesFromNotCreateTable(sql, logicalDatabase.getDialectType(), logicalDatabase);
            }
            RewriteResult rewriteResult = sqlRewriter.rewrite(
                    new RewriteContext(statement, logicalDatabase.getDialectType(), dataNodesToExecute));
            for (Map.Entry<DataNode, String> result : rewriteResult.getSqls().entrySet()) {
                Long databaseId = result.getKey().getDatabaseId();
                databaseId2Sqls.computeIfAbsent(databaseId, k -> new ArrayList<>()).add(result.getValue());
            }
        }
        Map<Long, Database> id2Database = databaseService.listDatabasesByIds(databaseId2Sqls.keySet()).stream()
                .collect(Collectors.toMap(Database::getId, db -> db));
        return databaseId2Sqls.entrySet().stream()
                .map(entry -> PreviewSqlResp.builder().database(id2Database.getOrDefault(entry.getKey(), null))
                        .sql(StringUtils.join(entry.getValue(), delimiter)).build())
                .collect(Collectors.toList());
    }

    private Set<DataNode> getDataNodesFromCreateTable(String sql, DialectType dialectType, Set<DataNode> allDataNodes) {
        Map<String, DataNode> databaseName2DataNodes = allDataNodes.stream()
                .collect(Collectors.toMap(dataNode -> dataNode.getSchemaName(), dataNode -> dataNode,
                        (value1, value2) -> value1));
        Map<DBSchemaIdentity, Set<SqlType>> identity2SqlTypes = DBSchemaExtractor.listDBSchemasWithSqlTypes(
                Arrays.asList(SqlTuple.newTuple(sql)), dialectType, null);
        DBSchemaIdentity identity = identity2SqlTypes.keySet().iterator().next();
        String logicalTableExpression = "";
        if (StringUtils.isNotEmpty(identity.getSchema())) {
            logicalTableExpression = identity.getSchema() + ".";
        }
        logicalTableExpression += identity.getTable();
        log.info("logical table expression = {}", logicalTableExpression);
        Set<DataNode> dataNodesToExecute =
                LogicalTableExpressionParseUtils.resolve(logicalTableExpression).stream().collect(
                        Collectors.toSet());
        dataNodesToExecute.forEach(dataNode -> dataNode.setDatabaseId(
                databaseName2DataNodes.getOrDefault(dataNode.getSchemaName(), dataNode).getDatabaseId()));
        log.info("data nodes to execute = {}", dataNodesToExecute);
        return dataNodesToExecute;
    }

    private Set<DataNode> getDataNodesFromNotCreateTable(String sql, DialectType dialectType,
            DetailLogicalDatabaseResp detailLogicalDatabaseResp) {
        List<DetailLogicalTableResp> logicalTables = detailLogicalDatabaseResp.getLogicalTables();
        Map<String, Set<DataNode>> logicalTableName2DataNodes = logicalTables.stream()
                .collect(Collectors.toMap(DetailLogicalTableResp::getName,
                        resp -> resp.getAllPhysicalTables().stream().collect(Collectors.toSet())));
        Map<DBSchemaIdentity, Set<SqlType>> identity2SqlTypes = DBSchemaExtractor.listDBSchemasWithSqlTypes(
                Arrays.asList(SqlTuple.newTuple(sql)), dialectType, detailLogicalDatabaseResp.getName());
        DBSchemaIdentity identity = identity2SqlTypes.keySet().iterator().next();
        return logicalTableName2DataNodes.getOrDefault(identity.getTable(), Collections.emptySet());
    }
}
