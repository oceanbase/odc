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
package com.oceanbase.tools.dbbrowser.schema.mysql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.tools.dbbrowser.model.DBColumnTypeDisplay;
import com.oceanbase.tools.dbbrowser.model.DBFunction;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBPLObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBProcedure;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionDefinition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionOption;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionType;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessorSqlMappers;
import com.oceanbase.tools.dbbrowser.schema.constant.Statements;
import com.oceanbase.tools.dbbrowser.schema.constant.StatementsFiles;
import com.oceanbase.tools.dbbrowser.util.DBSchemaAccessorUtil;
import com.oceanbase.tools.dbbrowser.util.StringUtils;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author jingtian
 * 
 *         适用 OB 版本：(~, 1.4.79]
 */
@Slf4j
public class OBMySQLNoGreaterThan1479SchemaAccessor extends BaseOBMySQLLessThan2277SchemaAccessor {

    private final String tenantName;
    protected JdbcOperations sysJdbcOperations;

    public OBMySQLNoGreaterThan1479SchemaAccessor(JdbcOperations jdbcOperations,
            JdbcOperations sysJdbcOperations, String tenantName) {
        super(jdbcOperations, DBSchemaAccessorSqlMappers.get(StatementsFiles.OBMYSQL_1479));
        this.tenantName = tenantName;
        this.sysJdbcOperations = sysJdbcOperations;
    }

    private static final Set<String> SPECIAL_TYPE_NAME = new HashSet<>();

    static {
        SPECIAL_TYPE_NAME.add("bit");
        SPECIAL_TYPE_NAME.add("int");
        SPECIAL_TYPE_NAME.add("tinyint");
        SPECIAL_TYPE_NAME.add("smallint");
        SPECIAL_TYPE_NAME.add("mediumint");
        SPECIAL_TYPE_NAME.add("bigint");
        SPECIAL_TYPE_NAME.add("float");
        SPECIAL_TYPE_NAME.add("double");
    }

    @Override
    public List<DBObjectIdentity> listTables(String schemaName, String tableNameLike) {
        // OceanBase less than 1479 can not use 'show full table from oceanbase'
        // which could cause duplicate result
        try {
            return listBaseTables(schemaName, tableNameLike);
        } catch (Exception e) {
            log.warn("List tables failed, reason={}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public DBTablePartition getPartition(String schemaName, String tableName) {
        if (this.sysJdbcOperations == null) {
            throw new IllegalStateException(
                    "Query table partition info failed, please confirm that you have sys read permission");
        }
        String sql = sqlMapper.getSql(Statements.GET_PARTITION);
        List<Map<String, Object>> result = sysJdbcOperations.query(sql, ps -> {
            ps.setString(1, tenantName);
            ps.setString(2, schemaName);
            ps.setString(3, tableName);
        }, (rs, rowNum) -> {
            Map<String, Object> row = new HashMap<>();
            row.put("partition_method", rs.getString("partition_method"));
            row.put("part_num", rs.getInt("part_num"));
            row.put("part_func_expr", rs.getString("part_func_expr"));
            row.put("is_sub_part_template", rs.getInt("is_sub_part_template"));
            row.put("part_name", rs.getString("part_name"));
            row.put("part_id", rs.getInt("part_id"));
            row.put("high_bound_val", rs.getString("high_bound_val"));
            row.put("subpartition_method", rs.getString("subpartition_method"));
            row.put("sub_part_func_expr", rs.getString("sub_part_func_expr"));
            row.put("sub_part_num", rs.getInt("sub_part_num"));
            return row;
        });
        return getFromResultSet(result, schemaName, tableName);
    }

    @Override
    public Map<String, DBTablePartition> listTablePartitions(@NonNull String schemaName, List<String> tableNames) {
        List<Map<String, Object>> queryResult = DBSchemaAccessorUtil.partitionFind(tableNames,
                DBSchemaAccessorUtil.OB_MAX_IN_SIZE, names -> {
                    String sql = filterByValues(sqlMapper.getSql(Statements.LIST_PARTITIONS), "table_name", names);
                    return jdbcOperations.query(sql, new Object[] {tenantName, schemaName}, (rs, rowNum) -> {
                        Map<String, Object> row = new HashMap<>();
                        row.put("table_name", rs.getString("table_name"));
                        row.put("partition_method", rs.getString("partition_method"));
                        row.put("part_num", rs.getInt("part_num"));
                        row.put("part_func_expr", rs.getString("part_func_expr"));
                        row.put("is_sub_part_template", rs.getInt("is_sub_part_template"));
                        row.put("part_name", rs.getString("part_name"));
                        row.put("part_id", rs.getInt("part_id"));
                        row.put("high_bound_val", rs.getString("high_bound_val"));
                        row.put("subpartition_method", rs.getString("subpartition_method"));
                        row.put("sub_part_func_expr", rs.getString("sub_part_func_expr"));
                        row.put("sub_part_num", rs.getInt("sub_part_num"));
                        return row;
                    });
                });
        Map<String, List<Map<String, Object>>> tableName2Rows = queryResult.stream()
                .collect(Collectors.groupingBy(m -> (String) m.get("table_name")));
        return tableName2Rows.entrySet().stream().collect(Collectors.toMap(Entry::getKey,
                e -> getFromResultSet(e.getValue(), schemaName, e.getKey())));
    }

    private DBTablePartition getFromResultSet(List<Map<String, Object>> rows, String schemaName, String tableName) {
        DBTablePartition partition = new DBTablePartition();
        DBTablePartition subPartition = new DBTablePartition();
        partition.setSubpartition(subPartition);
        partition.setSchemaName(schemaName);
        partition.setTableName(tableName);
        subPartition.setSchemaName(schemaName);

        DBTablePartitionOption partitionOption = new DBTablePartitionOption();
        partitionOption.setType(DBTablePartitionType.NOT_PARTITIONED);
        partition.setPartitionOption(partitionOption);

        DBTablePartitionOption subPartitionOption = new DBTablePartitionOption();
        partitionOption.setType(DBTablePartitionType.NOT_PARTITIONED);
        subPartition.setPartitionOption(subPartitionOption);

        List<DBTablePartitionDefinition> partitionDefinitions = new ArrayList<>();
        partition.setPartitionDefinitions(partitionDefinitions);
        List<DBTablePartitionDefinition> subPartitionDefinitions = new ArrayList<>();
        subPartition.setPartitionDefinitions(subPartitionDefinitions);

        for (Map<String, Object> row : rows) {
            partitionOption.setType(DBTablePartitionType.fromValue((String) row.get("partition_method")));
            partitionOption.setPartitionsNum((Integer) row.get("part_num"));
            String expression = (String) row.get("part_func_expr");
            if (StringUtils.isNotEmpty(expression)) {
                if (partitionOption.getType().supportExpression()) {
                    partitionOption.setExpression(expression);
                } else {
                    partitionOption.setColumnNames(Arrays.asList(expression.split(",")));
                }
            }
            partition.setSubpartitionTemplated((Integer) row.get("is_sub_part_template") == 1);

            DBTablePartitionDefinition partitionDefinition = new DBTablePartitionDefinition();
            partitionDefinition.setName((String) row.get("part_name"));
            partitionDefinition.setOrdinalPosition((Integer) row.get("part_id"));
            partitionDefinition.setType(DBTablePartitionType.fromValue((String) row.get("partition_method")));
            String description = null;
            if (partitionDefinition.getType() == DBTablePartitionType.RANGE
                    || partitionDefinition.getType() == DBTablePartitionType.RANGE_COLUMNS) {
                description = (String) row.get("high_bound_val");
            }
            partitionDefinition.fillValues(description);
            partitionDefinitions.add(partitionDefinition);


            DBTablePartitionType subPartType = DBTablePartitionType.fromValue((String) row.get("subpartition_method"));
            String subPartExpression = (String) row.get("sub_part_func_expr");

            // TODO 目前 ODC 仅支持 HASH/KEY 模板化二级分区, 其它类型后续需补充
            // OB 1479 bug，不是二级分区表，内部表里也会有值，需要判断 sub_part_func_expr 是否为空来确定是否有二级分区
            if ((subPartType == DBTablePartitionType.HASH || subPartType == DBTablePartitionType.KEY)
                    && partition.getSubpartitionTemplated() && StringUtils.isNotBlank(subPartExpression)) {
                subPartitionOption.setType(subPartType);
                subPartitionOption.setPartitionsNum((Integer) row.get("sub_part_num"));
                if (subPartType.supportExpression()) {
                    subPartitionOption.setExpression(subPartExpression);
                } else {
                    subPartitionOption.setColumnNames(Arrays.asList(subPartExpression.split(",")));
                }
            } else {
                partition.setWarning("Only support HASH/KEY subpartition currently, please check comparing ddl");
            }
        }
        return partition;
    }

    @Override
    public List<DBTableColumn> listTableColumns(String schemaName, String tableName) {
        List<DBTableColumn> columns = super.listTableColumns(schemaName, tableName);
        if (CollectionUtils.isNotEmpty(columns)) {
            columns.forEach(this::fillPrecisionAndScale);
        }
        return columns;
    }

    @Override
    public Map<String, List<DBTableColumn>> listTableColumns(String schemaName, List<String> tableNames) {
        Map<String, List<DBTableColumn>> tableName2Columns = super.listTableColumns(schemaName, tableNames);
        for (List<DBTableColumn> columnList : tableName2Columns.values()) {
            if (CollectionUtils.isNotEmpty(columnList)) {
                columnList.forEach(this::fillPrecisionAndScale);
            }
        }
        return tableName2Columns;
    }

    @Override
    protected void fillIndexInfo(List<DBTableIndex> indexList, String schemaName, String tableName) {
        /**
         * OBMySQL 1479 不支持 GLOBAL 索引，这里把所有 range 设置为 LOCAL
         */
        indexList.forEach(index -> index.setGlobal(false));
    }

    private void fillPrecisionAndScale(DBTableColumn column) {
        if (SPECIAL_TYPE_NAME.contains(column.getTypeName())) {
            String precisionAndScale = DBSchemaAccessorUtil.parsePrecisionAndScale(column.getFullTypeName());
            if (StringUtils.isBlank(precisionAndScale)) {
                return;
            }
            DBColumnTypeDisplay display = DBColumnTypeDisplay.fromName(column.getTypeName());
            if (precisionAndScale.contains(",")) {
                String[] seg = precisionAndScale.split(",");
                if (seg.length == 2) {
                    if (display.displayPrecision()) {
                        column.setPrecision(Long.parseLong(seg[0]));
                    }
                    if (display.displayScale()) {
                        column.setScale(Integer.parseInt(seg[1]));
                    }
                }
            } else {
                if (display.displayPrecision()) {
                    column.setPrecision(Long.parseLong(precisionAndScale));
                }
            }
        }
    }

    @Override
    public List<DBPLObjectIdentity> listFunctions(String schemaName) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public List<DBPLObjectIdentity> listProcedures(String schemaName) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public DBFunction getFunction(String schemaName, String functionName) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public DBProcedure getProcedure(String schemaName, String procedureName) {
        throw new UnsupportedOperationException("Not supported yet");
    }
}
