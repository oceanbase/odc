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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.metadb.connection.DatabaseRepository;
import com.oceanbase.odc.metadb.connection.logicaldatabase.LogicalTableEntity;
import com.oceanbase.odc.metadb.connection.logicaldatabase.LogicalTablePhysicalTableEntity;
import com.oceanbase.odc.metadb.connection.logicaldatabase.LogicalTablePhysicalTableRepository;
import com.oceanbase.odc.metadb.connection.logicaldatabase.LogicalTableRepository;
import com.oceanbase.odc.metadb.dbobject.DBObjectRepository;
import com.oceanbase.odc.service.connection.logicaldatabase.core.model.DataNode;
import com.oceanbase.odc.service.connection.logicaldatabase.core.parser.BadLogicalTableExpressionException;
import com.oceanbase.odc.service.connection.logicaldatabase.core.parser.DefaultLogicalTableExpressionParser;
import com.oceanbase.odc.service.connection.logicaldatabase.core.parser.LogicalTableExpressions;
import com.oceanbase.odc.service.connection.logicaldatabase.model.DetailLogicalTableResp;
import com.oceanbase.odc.service.iam.ProjectPermissionValidator;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.tools.sqlparser.SyntaxErrorException;

/**
 * @Author: Lebie
 * @Date: 2024/4/23 11:31
 * @Description: []
 */
@Service
@SkipAuthorize
public class LogicalTableService {
    private final DefaultLogicalTableExpressionParser parser = new DefaultLogicalTableExpressionParser();

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private LogicalTableRepository tableRepository;

    @Autowired
    private LogicalTablePhysicalTableRepository relationRepository;

    @Autowired
    private DBObjectRepository dbObjectRepository;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private ProjectPermissionValidator projectPermissionValidator;

    // TODO: database permission check after @GaoDa's PR merged
    public List<DetailLogicalTableResp> list(@NotNull Long logicalDatabaseId) {
        List<LogicalTableEntity> tableEntities = tableRepository.findByLogicalDatabaseId(logicalDatabaseId);
        Set<Long> logicalTableIds = tableEntities.stream().map(LogicalTableEntity::getId).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(logicalTableIds)) {
            return Collections.emptyList();
        }
        Map<Long, List<LogicalTablePhysicalTableEntity>> logicalTbId2Relations =
                relationRepository.findByLogicalTableIdIn(logicalTableIds).stream()
                        .collect(Collectors.groupingBy(LogicalTablePhysicalTableEntity::getLogicalTableId));


        return tableEntities.stream().map(tableEntity -> {
            DetailLogicalTableResp resp = new DetailLogicalTableResp();
            List<LogicalTablePhysicalTableEntity> relations =
                    logicalTbId2Relations.getOrDefault(tableEntity.getId(), Collections.emptyList());
            resp.setId(tableEntity.getId());
            resp.setName(tableEntity.getName());
            resp.setExpression(tableEntity.getExpression());
            resp.setPhysicalTableCount(relations.size());
            resp.setLastSyncTime(tableEntity.getLastSyncTime());
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
        LogicalTableEntity tableEntity = tableRepository.findById(logicalTableId)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_LOGICAL_TABLE, "id", logicalTableId));
        Verify.equals(tableEntity.getLogicalDatabaseId(), logicalDatabaseId, "logical database id");
        tableRepository.deleteById(logicalTableId);
        relationRepository.deleteByLogicalTableId(logicalTableId);
        return true;
    }

    public Boolean detectConsistency(@NotNull Long logicalDatabaseId, @NotNull Long logicalTableId) {
        LogicalTableEntity tableEntity = tableRepository.findById(logicalTableId)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_LOGICAL_TABLE, "id", logicalTableId));
        Verify.equals(tableEntity.getLogicalDatabaseId(), logicalDatabaseId, "logical database id");
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
