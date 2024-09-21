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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
import com.oceanbase.odc.service.connection.logicaldatabase.core.model.DataNode;
import com.oceanbase.odc.service.connection.logicaldatabase.core.parser.LogicalTableExpressionParseUtils;
import com.oceanbase.odc.service.connection.logicaldatabase.model.DetailLogicalDatabaseResp;
import com.oceanbase.odc.service.connection.logicaldatabase.model.DetailLogicalTableResp;
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
            Set<DataNode> allDataNodes) {
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
        Set<DataNode> dataNodesToExecute =
                LogicalTableExpressionParseUtils.resolve(logicalTableExpression).stream().collect(
                        Collectors.toSet());
        dataNodesToExecute.forEach(dataNode -> dataNode.setDatabaseId(
                databaseName2DataNodes.getOrDefault(dataNode.getSchemaName(), dataNode).getDatabaseId()));
        return dataNodesToExecute;
    }

    public static Set<DataNode> getDataNodesFromNotCreateTable(String sql, DialectType dialectType,
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
