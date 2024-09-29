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

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
import com.oceanbase.odc.service.connection.logicaldatabase.core.model.DataNode;
import com.oceanbase.odc.service.connection.logicaldatabase.core.parser.LogicalTableExpressionParseUtils;
import com.oceanbase.odc.service.session.util.DBSchemaExtractor;
import com.oceanbase.odc.service.session.util.DBSchemaExtractor.DBSchemaIdentity;
import com.oceanbase.tools.dbbrowser.parser.constant.SqlType;

/**
 * @Author: Lebie
 * @Date: 2024/9/9 14:38
 * @Description: []
 */
public class LogicalDatabaseUtils {
    public static Set<DataNode> getDataNodesFromCreateTable(String sql, DialectType dialectType,
            Map<String, DataNode> databaseName2DataNodes) {
        Map<DBSchemaIdentity, Set<SqlType>> identity2SqlTypes = DBSchemaExtractor.listDBSchemasWithSqlTypes(
                Arrays.asList(SqlTuple.newTuple(sql)), dialectType, null);
        DBSchemaIdentity identity = identity2SqlTypes.keySet().iterator().next();
        Set<DataNode> dataNodesToExecute =
                LogicalTableExpressionParseUtils.resolve(identity.getTable()).stream().collect(
                        Collectors.toSet());
        dataNodesToExecute.forEach(dataNode -> {
            if (!databaseName2DataNodes.containsKey(dataNode.getSchemaName())) {
                throw new BadRequestException("physical database not found, database name=" + dataNode.getSchemaName());
            }
            dataNode.setDatabaseId(databaseName2DataNodes.get(dataNode.getSchemaName()).getDatabaseId());
        });
        return dataNodesToExecute;
    }

    public static Set<DataNode> getDataNodesFromNotCreateTable(String sql, DialectType dialectType,
            Map<String, Set<DataNode>> logicalTableName2DataNodes, String logicalDatabaseName) {
        Map<DBSchemaIdentity, Set<SqlType>> identity2SqlTypes = DBSchemaExtractor.listDBSchemasWithSqlTypes(
                Arrays.asList(SqlTuple.newTuple(sql)), dialectType, logicalDatabaseName);
        DBSchemaIdentity identity = identity2SqlTypes.keySet().iterator().next();
        if (!logicalTableName2DataNodes.containsKey(identity.getTable())) {
            throw new BadRequestException("logical table not found, logical table name=" + identity.getTable());
        }
        return logicalTableName2DataNodes.get(identity.getTable());
    }
}
