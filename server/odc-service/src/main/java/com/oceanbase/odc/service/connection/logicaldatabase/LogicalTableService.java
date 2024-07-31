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
import com.oceanbase.odc.metadb.connection.logicaldatabase.TableMappingEntity;
import com.oceanbase.odc.metadb.connection.logicaldatabase.TableMappingRepository;
import com.oceanbase.odc.metadb.dbobject.DBObjectEntity;
import com.oceanbase.odc.metadb.dbobject.DBObjectRepository;
import com.oceanbase.odc.service.connection.database.model.DatabaseType;
import com.oceanbase.odc.service.connection.logicaldatabase.core.model.DataNode;
import com.oceanbase.odc.service.connection.logicaldatabase.core.parser.BadLogicalTableExpressionException;
import com.oceanbase.odc.service.connection.logicaldatabase.core.parser.DefaultLogicalTableExpressionParser;
import com.oceanbase.odc.service.connection.logicaldatabase.core.parser.LogicalTableExpressions;
import com.oceanbase.odc.service.connection.logicaldatabase.model.DetailLogicalTableResp;
import com.oceanbase.odc.service.iam.ProjectPermissionValidator;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.permission.DBResourcePermissionHelper;
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
