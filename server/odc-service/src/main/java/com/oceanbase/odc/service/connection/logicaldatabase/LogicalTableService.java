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

import java.io.StringReader;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.metadb.connection.DatabaseEntity;
import com.oceanbase.odc.metadb.connection.DatabaseRepository;
import com.oceanbase.odc.metadb.connection.logicaldatabase.DatabaseMappingEntity;
import com.oceanbase.odc.metadb.connection.logicaldatabase.DatabaseMappingRepository;
import com.oceanbase.odc.metadb.connection.logicaldatabase.TableMappingEntity;
import com.oceanbase.odc.metadb.connection.logicaldatabase.TableMappingRepository;
import com.oceanbase.odc.metadb.dbobject.DBObjectEntity;
import com.oceanbase.odc.metadb.dbobject.DBObjectRepository;
import com.oceanbase.odc.plugin.schema.api.TableExtensionPoint;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.database.model.DatabaseType;
import com.oceanbase.odc.service.connection.logicaldatabase.core.LogicalTableRecognitionUtils;
import com.oceanbase.odc.service.connection.logicaldatabase.core.model.DataNode;
import com.oceanbase.odc.service.connection.logicaldatabase.core.parser.BadLogicalTableExpressionException;
import com.oceanbase.odc.service.connection.logicaldatabase.core.parser.DefaultLogicalTableExpressionParser;
import com.oceanbase.odc.service.connection.logicaldatabase.core.parser.LogicalTableExpressions;
import com.oceanbase.odc.service.connection.logicaldatabase.model.DetailLogicalTableResp;
import com.oceanbase.odc.service.connection.logicaldatabase.model.LogicalTableTopologyResp;
import com.oceanbase.odc.service.iam.ProjectPermissionValidator;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.permission.DBResourcePermissionHelper;
import com.oceanbase.odc.service.plugin.SchemaPluginUtil;
import com.oceanbase.odc.service.session.factory.DruidDataSourceFactory;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.sqlparser.SyntaxErrorException;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2024/4/23 11:31
 * @Description: []
 */
@Service
@SkipAuthorize
@Slf4j
@Validated
public class LogicalTableService {
    private final DefaultLogicalTableExpressionParser parser = new DefaultLogicalTableExpressionParser();

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private DBObjectRepository dbObjectRepository;

    @Autowired
    private TableMappingRepository mappingRepository;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private ProjectPermissionValidator projectPermissionValidator;

    @Autowired
    private LogicalDatabaseSyncManager syncManager;

    @Autowired
    private DBResourcePermissionHelper permissionHelper;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private DatabaseMappingRepository databaseMappingRepository;

    public List<DetailLogicalTableResp> list(@NotNull Long logicalDatabaseId) {
        DatabaseEntity logicalDatabase =
                databaseRepository.findById(logicalDatabaseId).orElseThrow(() -> new NotFoundException(
                        ResourceType.ODC_DATABASE, "id", logicalDatabaseId));
        Verify.equals(DatabaseType.LOGICAL, logicalDatabase.getType(), "database type");
        projectPermissionValidator.checkProjectRole(logicalDatabase.getProjectId(), ResourceRoleName.all());

        List<DBObjectEntity> logicalTables =
                dbObjectRepository.findByDatabaseIdAndType(logicalDatabaseId, DBObjectType.LOGICAL_TABLE);


        Set<Long> logicalTableIds = logicalTables.stream().map(DBObjectEntity::getId).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(logicalTableIds)) {
            return Collections.emptyList();
        }
        Map<Long, List<TableMappingEntity>> logicalTbId2Mappings =
                mappingRepository.findByLogicalTableIdIn(logicalTableIds).stream()
                        .collect(Collectors.groupingBy(TableMappingEntity::getLogicalTableId));


        return logicalTables.stream().map(tableEntity -> {
            DetailLogicalTableResp resp = new DetailLogicalTableResp();
            List<TableMappingEntity> relations =
                    logicalTbId2Mappings.getOrDefault(tableEntity.getId(), Collections.emptyList());
            resp.setId(tableEntity.getId());
            resp.setName(tableEntity.getName());
            resp.setExpression(relations.isEmpty() ? StringUtils.EMPTY : relations.get(0).getExpression());
            resp.setPhysicalTableCount(relations.size());
            List<DataNode> inconsistentPhysicalTables = new ArrayList<>();
            relations.stream().filter(relation -> !relation.getConsistent()).forEach(relation -> {
                DataNode dataNode = new DataNode();
                dataNode.setSchemaName(relation.getPhysicalDatabaseName());
                dataNode.setTableName(relation.getPhysicalTableName());
                inconsistentPhysicalTables.add(dataNode);
            });
            resp.setInconsistentPhysicalTables(inconsistentPhysicalTables);
            return resp;
        }).collect(Collectors.toList());
    }

    public List<LogicalTableTopologyResp> previewLogicalTableTopologies(@NotNull Long logicalDatabaseId,
            @NotNull String expression) {
        DatabaseEntity logicalDatabase =
                databaseRepository.findById(logicalDatabaseId).orElseThrow(() -> new NotFoundException(
                        ResourceType.ODC_DATABASE, "id", logicalDatabaseId));
        Verify.equals(DatabaseType.LOGICAL, logicalDatabase.getType(), "database type");
        projectPermissionValidator.checkProjectRole(logicalDatabase.getProjectId(), ResourceRoleName.all());
        List<Long> physicalDatabaseIds = databaseMappingRepository.findByLogicalDatabaseId(logicalDatabaseId).stream()
                .map(DatabaseMappingEntity::getPhysicalDatabaseId).collect(
                        Collectors.toList());
        Map<String, List<Database>> name2Databases = databaseService.listDatabasesByIds(physicalDatabaseIds).stream()
                .collect(Collectors.groupingBy(Database::getName));
        Map<String, List<DataNode>> schemaName2DataNodes =
                resolve(expression).stream().collect(Collectors.groupingBy(DataNode::getSchemaName));
        Verify.verify(name2Databases.entrySet().containsAll(schemaName2DataNodes.entrySet()),
                "The expression contains physical databases that not belong to the logical database");
        return schemaName2DataNodes.entrySet().stream().map(entry -> {
            LogicalTableTopologyResp resp = new LogicalTableTopologyResp();
            resp.setPhysicalDatabase(name2Databases.get(entry.getKey()).get(0));
            resp.setTableCount(entry.getValue().size());
            resp.setExpression(LogicalTableRecognitionUtils.recognizeLogicalTablesWithExpression(entry.getValue())
                    .get(0).getFullNameExpression());
            return resp;
        }).collect(Collectors.toList());
    }

    public List<LogicalTableTopologyResp> listLogicalTableTopologies(@NotNull Long logicalDatabaseId,
            @NotNull Long logicalTableId) {
        Map<Long, List<TableMappingEntity>> physicalDBId2Tables = mappingRepository.findByLogicalTableId(logicalTableId)
                .stream().collect(Collectors.groupingBy(TableMappingEntity::getPhysicalDatabaseId));
        Map<Long, List<Database>> id2Databases =
                databaseService.listDatabasesByIds(physicalDBId2Tables.keySet()).stream().collect(Collectors.groupingBy(
                        Database::getId));
        return physicalDBId2Tables.entrySet().stream().map(entry -> {
            List<TableMappingEntity> tables = entry.getValue();
            LogicalTableTopologyResp resp = new LogicalTableTopologyResp();
            resp.setPhysicalDatabase(id2Databases.get(entry.getKey()).get(0));
            resp.setTableCount(tables.size());
            resp.setExpression(
                    LogicalTableRecognitionUtils.recognizeLogicalTablesWithExpression(tables.stream().map(table -> {
                        DataNode dataNode = new DataNode();
                        dataNode.setSchemaName(table.getPhysicalDatabaseName());
                        dataNode.setTableName(table.getPhysicalTableName());
                        return dataNode;
                    }).collect(Collectors.toList())).get(0).getFullNameExpression());
            return resp;
        }).collect(Collectors.toList());
    }

    public DetailLogicalTableResp detail(@NotNull Long logicalDatabaseId, @NotNull Long logicalTableId) {
        DatabaseEntity logicalDatabase =
                databaseRepository.findById(logicalDatabaseId).orElseThrow(() -> new NotFoundException(
                        ResourceType.ODC_DATABASE, "id", logicalDatabaseId));
        Verify.equals(DatabaseType.LOGICAL, logicalDatabase.getType(), "database type");
        projectPermissionValidator.checkProjectRole(logicalDatabase.getProjectId(), ResourceRoleName.all());

        DBObjectEntity logicalTable =
                dbObjectRepository.findById(logicalTableId).orElseThrow(() -> new NotFoundException(
                        ResourceType.ODC_LOGICAL_TABLE, "id", logicalTableId));
        Verify.equals(logicalTable.getDatabaseId(), logicalDatabaseId, "logical database id");
        Verify.equals(logicalTable.getType(), DBObjectType.LOGICAL_TABLE, "logical table type");

        List<TableMappingEntity> relations = mappingRepository.findByLogicalTableId(logicalTableId);
        DetailLogicalTableResp resp = new DetailLogicalTableResp();

        resp.setId(logicalTable.getId());
        resp.setName(logicalTable.getName());
        resp.setExpression(relations.isEmpty() ? StringUtils.EMPTY : relations.get(0).getExpression());
        resp.setPhysicalTableCount(relations.size());
        List<DataNode> inconsistentPhysicalTables = new ArrayList<>();
        relations.stream().filter(relation -> !relation.getConsistent()).forEach(relation -> {
            DataNode dataNode = new DataNode();
            dataNode.setSchemaName(relation.getPhysicalDatabaseName());
            dataNode.setTableName(relation.getPhysicalTableName());
            inconsistentPhysicalTables.add(dataNode);
        });
        resp.setInconsistentPhysicalTables(inconsistentPhysicalTables);
        resp.setTopologies(listLogicalTableTopologies(logicalDatabaseId, logicalTableId));

        TableMappingEntity baseTable = relations.stream().filter(relation -> relation.getConsistent()).findFirst()
                .orElseThrow(() -> new NotFoundException(
                        ResourceType.ODC_LOGICAL_TABLE, "id", logicalTableId));
        DatabaseEntity physicalDatabase =
                databaseRepository.findById(baseTable.getPhysicalDatabaseId()).orElseThrow(() -> new NotFoundException(
                        ResourceType.ODC_DATABASE, "id", baseTable.getPhysicalDatabaseId()));

        TableExtensionPoint tableExtensionPoint =
                SchemaPluginUtil.getTableExtension(logicalDatabase.getConnectType().getDialectType());
        if (tableExtensionPoint == null) {
            throw new UnsupportedOperationException(
                    "Unsupported dialect " + logicalDatabase.getConnectType().getDialectType());
        }
        try (Connection connection = new DruidDataSourceFactory(
                connectionService.getForConnectionSkipPermissionCheck(physicalDatabase.getConnectionId()))
                        .getDataSource().getConnection()) {
            resp.setBasePhysicalTable(tableExtensionPoint.getDetail(connection, physicalDatabase.getName(),
                    baseTable.getPhysicalTableName()));
        } catch (Exception e) {
            log.error("failed to get table structure, logical table id={}", logicalTableId, e);
        }
        return resp;
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(@NotNull Long logicalDatabaseId, @NotNull Long logicalTableId) {
        DBObjectEntity logicalTable = dbObjectRepository.findById(logicalTableId)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_LOGICAL_TABLE, "id", logicalTableId));
        Verify.equals(logicalTable.getDatabaseId(), logicalDatabaseId, "logical database id");
        DatabaseEntity logicalDatabase =
                databaseRepository.findById(logicalDatabaseId).orElseThrow(() -> new NotFoundException(
                        ResourceType.ODC_DATABASE, "id", logicalDatabaseId));
        projectPermissionValidator.checkProjectRole(logicalDatabase.getProjectId(),
                Arrays.asList(ResourceRoleName.DBA, ResourceRoleName.OWNER));

        dbObjectRepository.deleteById(logicalTableId);
        mappingRepository.deleteByLogicalTableId(logicalTableId);
        return true;
    }

    public Boolean checkStructureConsistency(@NotNull Long logicalDatabaseId, @NotNull Long logicalTableId) {
        DBObjectEntity table = dbObjectRepository.findById(logicalTableId)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_LOGICAL_TABLE, "id", logicalTableId));
        Verify.equals(table.getDatabaseId(), logicalDatabaseId, "logical database id");
        try {
            syncManager.submitCheckConsistencyTask(logicalTableId);
        } catch (TaskRejectedException ex) {
            log.warn("submit check logical table structure consistency task rejected, logical table id={}",
                    logicalTableId);
            return false;
        }
        return true;
    }


    public List<DataNode> resolve(String expression) {
        PreConditions.notEmpty(expression, "expression");
        LogicalTableExpressions logicalTableExpression;
        try {
            logicalTableExpression = (LogicalTableExpressions) parser.parse(new StringReader(expression));
        } catch (SyntaxErrorException e) {
            throw new BadLogicalTableExpressionException(e);
        } catch (Exception e) {
            throw new UnexpectedException("failed to parse logical table expression", e);
        }
        return logicalTableExpression.evaluate().stream().map(name -> {
            String[] parts = name.split("\\.");
            if (parts.length != 2) {
                throw new UnexpectedException("invalid logical table expression");
            }
            return new DataNode(parts[0], parts[1]);
        }).collect(Collectors.toList());
    }
}
