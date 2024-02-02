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
package com.oceanbase.tools.dbbrowser.schema.oracle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.tools.dbbrowser.model.DBSynonymType;
import com.oceanbase.tools.dbbrowser.model.DBTable.DBTableOptions;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionDefinition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionOption;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionType;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessorSqlMappers;
import com.oceanbase.tools.dbbrowser.schema.constant.Statements;
import com.oceanbase.tools.dbbrowser.schema.constant.StatementsFiles;
import com.oceanbase.tools.dbbrowser.util.OracleDataDictTableNames;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.StringUtils;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/3/7 15:42
 * @Description: 适用于的 OB 版本：[2270, 400)
 */
@Slf4j
public class OBOracleLessThan400SchemaAccessor extends OBOracleSchemaAccessor {

    public OBOracleLessThan400SchemaAccessor(JdbcOperations jdbcOperations,
            OracleDataDictTableNames dataDictTableNames) {
        super(jdbcOperations, dataDictTableNames);
        this.sqlMapper = DBSchemaAccessorSqlMappers.get(StatementsFiles.OBORACLE_3_x);
    }

    @Override
    public DBTableOptions getTableOptions(String schemaName, String tableName) {
        DBTableOptions tableOptions = new DBTableOptions();
        obtainTableCharset(Collections.singletonList(tableOptions));
        obtainTableCollation(Collections.singletonList(tableOptions));
        String sql = this.sqlMapper.getSql(Statements.GET_TABLE_OPTION);
        try {
            this.jdbcOperations.query(sql, new Object[] {schemaName, tableName}, rs -> {
                tableOptions.setCreateTime(rs.getTimestamp("GMT_CREATE"));
                tableOptions.setUpdateTime(rs.getTimestamp("GMT_MODIFIED"));
                tableOptions.setComment(rs.getString("COMMENT"));
                tableOptions.setTabletSize(rs.getLong("TABLET_SIZE"));
            });
        } catch (Exception ex) {
            log.warn("get table options failed, schema={}, table={}, reason:", schemaName, tableName, ex);
        }
        return tableOptions;
    }

    @Override
    public DBTablePartition getPartition(String schemaName, String tableName) {
        String sql = this.sqlMapper.getSql(Statements.GET_PARTITION);
        List<Map<String, Object>> queryResult =
                this.jdbcOperations.query(sql, new Object[] {schemaName, tableName}, (rs, num) -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("PARTITION_METHOD", rs.getString("PARTITION_METHOD"));
                    result.put("PART_NUM", rs.getInt("PART_NUM"));
                    result.put("EXPRESSION", rs.getString("EXPRESSION"));
                    result.put("PART_NAME", rs.getString("PART_NAME"));
                    result.put("PART_ID", rs.getInt("PART_ID"));
                    result.put("MAX_VALUE", rs.getString("MAX_VALUE"));
                    result.put("LIST_VALUE", rs.getString("LIST_VALUE"));
                    return result;
                });
        return getFromResultSet(queryResult, schemaName, tableName);
    }

    @Override
    public Map<String, DBTablePartition> listTablePartitions(@NonNull String schemaName, List<String> candidates) {
        String sql = filterByValues(sqlMapper.getSql(Statements.LIST_PARTITIONS), "TABLE_NAME", candidates);
        List<Map<String, Object>> queryResult = jdbcOperations.query(sql, new Object[] {schemaName}, (rs, rowNum) -> {
            Map<String, Object> row = new HashMap<>();
            row.put("TABLE_NAME", rs.getString("TABLE_NAME"));
            row.put("PARTITION_METHOD", rs.getString("PARTITION_METHOD"));
            row.put("PART_NUM", rs.getInt("PART_NUM"));
            row.put("EXPRESSION", rs.getString("EXPRESSION"));
            row.put("PART_NAME", rs.getString("PART_NAME"));
            row.put("PART_ID", rs.getInt("PART_ID"));
            row.put("MAX_VALUE", rs.getString("MAX_VALUE"));
            row.put("LIST_VALUE", rs.getString("LIST_VALUE"));
            return row;
        });
        Map<String, List<Map<String, Object>>> tableName2Rows = queryResult.stream()
                .collect(Collectors.groupingBy(m -> (String) m.get("TABLE_NAME")));
        return tableName2Rows.entrySet().stream().collect(Collectors.toMap(Entry::getKey,
                e -> getFromResultSet(e.getValue(), schemaName, e.getKey())));
    }

    private DBTablePartition getFromResultSet(List<Map<String, Object>> rows, String schemaName, String tableName) {
        DBTablePartition partition = new DBTablePartition();
        partition.setPartitionOption(new DBTablePartitionOption());
        partition.setPartitionDefinitions(new ArrayList<>());
        partition.setSchemaName(schemaName);
        partition.setTableName(tableName);
        for (Map<String, Object> row : rows) {
            DBTablePartitionOption option = partition.getPartitionOption();
            option.setType(DBTablePartitionType.fromValue((String) row.get("PARTITION_METHOD")));
            option.setPartitionsNum((Integer) row.get("PART_NUM"));
            String expression = (String) row.get("EXPRESSION");
            if (option.getType().supportExpression()) {
                option.setExpression(expression);
            } else {
                option.setColumnNames(Arrays.asList(expression.split(",")));
            }
            DBTablePartitionDefinition partitionDefinition = new DBTablePartitionDefinition();
            partitionDefinition.setName((String) row.get("PART_NAME"));
            partitionDefinition.setOrdinalPosition((Integer) row.get("PART_ID"));
            partitionDefinition.setType(option.getType());
            String maxValue = (String) row.get("MAX_VALUE");
            String listValue = (String) row.get("LIST_VALUE");
            partitionDefinition.fillValues(StringUtils.isNotEmpty(maxValue) ? maxValue : listValue);
            partition.getPartitionDefinitions().add(partitionDefinition);
        }
        return partition;
    }

    @Override
    protected String getSynonymOwnerSymbol(DBSynonymType synonymType, String schemaName) {
        if (synonymType.equals(DBSynonymType.PUBLIC)) {
            return "PUBLIC";
        } else if (synonymType.equals(DBSynonymType.COMMON)) {
            return schemaName;
        } else {
            throw new UnsupportedOperationException("Not supported Synonym type");
        }
    }

    @Override
    public Map<String, List<DBTableColumn>> listBasicTableColumns(String schemaName) {
        String sql = sqlMapper.getSql(Statements.LIST_BASIC_SCHEMA_TABLE_COLUMNS);
        List<DBTableColumn> tableColumns =
                jdbcOperations.query(sql, new Object[] {schemaName}, listBasicColumnsRowMapper());
        return tableColumns.stream().collect(Collectors.groupingBy(DBTableColumn::getTableName));
    }

    @Override
    public Map<String, List<DBTableColumn>> listBasicViewColumns(String schemaName) {
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("select view_name from all_views where owner = ").value(schemaName);
        List<String> viewNames = jdbcOperations.query(sb.toString(), (rs, rowNum) -> rs.getString("view_name"));
        List<DBTableColumn> columns = new ArrayList<>();
        for (String viewName : viewNames) {
            columns.addAll(fillViewColumnInfoByDesc(schemaName, viewName));
        }
        return columns.stream().collect(Collectors.groupingBy(DBTableColumn::getTableName));
    }

    @Override
    public List<DBTableColumn> listBasicViewColumns(String schemaName, String viewName) {
        return fillViewColumnInfoByDesc(schemaName, viewName);
    }

    protected List<DBTableColumn> fillViewColumnInfoByDesc(String schemaName, String viewName) {
        List<DBTableColumn> columns = new ArrayList<>();
        try {
            OracleSqlBuilder sb = new OracleSqlBuilder();
            sb.append("desc ");
            if (StringUtils.isNotBlank(schemaName)) {
                sb.identifier(schemaName).append(".");
            }
            sb.identifier(viewName);
            columns = jdbcOperations.query(sb.toString(), (rs, rowNum) -> {
                DBTableColumn column = new DBTableColumn();
                column.setSchemaName(schemaName);
                column.setTableName(viewName);
                column.setName(rs.getString(1));
                column.setTypeName(rs.getString(2).split("\\(")[0]);
                return column;
            });
        } catch (Exception e) {
            log.warn("fail to get view column info, message={}", e.getMessage());
        }
        return columns;
    }

}
