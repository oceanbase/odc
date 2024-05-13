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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.lang.NonNull;

import com.oceanbase.tools.dbbrowser.model.DBDatabase;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionDefinition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionOption;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionType;
import com.oceanbase.tools.dbbrowser.model.DBView;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessorSqlMappers;
import com.oceanbase.tools.dbbrowser.schema.constant.Statements;
import com.oceanbase.tools.dbbrowser.schema.constant.StatementsFiles;
import com.oceanbase.tools.dbbrowser.util.DBSchemaAccessorUtil;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 *
 * 适用版本：[2.2.77, 4.0.0)
 *
 * @Author: Lebie
 * @Date: 2023/1/31 16:19
 * @Description: []
 */
@Slf4j
public class OBMySQLBetween2277And3XSchemaAccessor extends OBMySQLSchemaAccessor {

    /**
     * 以下对象名为 mysql schema 下的视图，但 table_type='BASE_TYPE'
     */
    protected final List<String> VIEW_AS_BASE_TABLE =
            Arrays.asList("time_zone", "time_zone_name", "time_zone_transition", "time_zone_transition_type");

    public OBMySQLBetween2277And3XSchemaAccessor(@NonNull JdbcOperations jdbcOperations) {
        super(jdbcOperations);
        this.sqlMapper = DBSchemaAccessorSqlMappers.get(StatementsFiles.OBMYSQL_3X);
    }

    @Override
    public DBDatabase getDatabase(String schemaName) {
        DBDatabase database = new DBDatabase();
        MySQLSqlBuilder sb = new MySQLSqlBuilder();
        sb.append("select database_id, database_name from oceanbase.gv$database where database_name=")
                .value(schemaName);
        jdbcOperations.query(sb.toString(), rs -> {
            database.setId(rs.getString("database_id"));
            database.setName(rs.getString("database_name"));
        });
        String sql =
                "SELECT DEFAULT_CHARACTER_SET_NAME, DEFAULT_COLLATION_NAME FROM information_schema.schemata WHERE SCHEMA_NAME ='"
                        + schemaName + "'";
        jdbcOperations.query(sql, rs -> {
            database.setCharset(rs.getString("DEFAULT_CHARACTER_SET_NAME"));
            database.setCollation(rs.getString("DEFAULT_COLLATION_NAME"));
        });
        return database;
    }

    @Override
    protected void fillIndexInfo(List<DBTableIndex> indexList, String schemaName,
            String tableName) {
        setIndexInfoByDDL(indexList, schemaName, tableName);
        if (indexList.stream().anyMatch(idx -> Objects.isNull(idx.getGlobal()))) {
            setIndexRangeByQuery(indexList, schemaName, tableName);
        }
    }

    protected void setIndexRangeByQuery(List<DBTableIndex> indexList, String schemaName, String tableName) {
        try {
            MySQLSqlBuilder sb = new MySQLSqlBuilder();
            sb.append("SELECT `table_id` from oceanbase.gv$table where database_name=");
            sb.value(schemaName);
            sb.append(" and");
            sb.append(" table_name=");
            sb.value(tableName);
            sb.append(" into @table_id");
            jdbcOperations.execute(sb.toString());

            sb = new MySQLSqlBuilder();
            sb.append(
                    "select database_name, table_type, index_type, replace(table_name, CONCAT('__idx_', @table_id, '_'),'') as ");
            sb.append(" index_name, case when index_type < 3 then true else false end as is_local_index"
                    + " from oceanbase.gv$table");
            sb.append(" where database_name=");
            sb.value(schemaName);
            sb.append(" and table_name like");
            sb.append(" CONCAT('__idx_', @table_id, '_%') and table_type=5");

            jdbcOperations.query(sb.toString(), (rs, num) -> {
                String indexName = rs.getString("index_name");
                Boolean isLocal = rs.getBoolean("is_local_index");
                indexList.forEach(dbTableIndex -> {
                    if (org.apache.commons.lang3.StringUtils.isNotBlank(dbTableIndex.getWarning())
                            && org.apache.commons.lang3.StringUtils.equals(dbTableIndex.getName(), indexName)) {
                        dbTableIndex.setGlobal(!isLocal);
                    }
                });
                return indexName;
            });
        } catch (Exception ex) {
            fillWarning(indexList, DBObjectType.INDEX, "query oceanbase.gv$table failed to get index ddl failed");
            log.warn("Failed to fetch indices of table {} by querying `oceanbase.gv$table`", tableName, ex);
        }
    }

    @Override
    public DBTablePartition getPartition(String schemaName, String tableName) {
        String sql = sqlMapper.getSql(Statements.GET_PARTITION);
        List<Map<String, Object>> result = jdbcOperations.query(sql, ps -> {
            ps.setString(1, schemaName);
            ps.setString(2, tableName);
        }, (rs, rowNum) -> {
            Map<String, Object> row = new HashMap<>();
            row.put("partition_method", rs.getString("partition_method"));
            row.put("part_num", rs.getInt("part_num"));
            row.put("part_func_expr", rs.getString("part_func_expr"));
            row.put("is_sub_part_template", rs.getInt("is_sub_part_template"));
            row.put("part_name", rs.getString("part_name"));
            row.put("part_id", rs.getInt("part_id"));
            row.put("list_val", rs.getString("list_val"));
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
                    return jdbcOperations.query(sql, new Object[] {schemaName}, (rs, rowNum) -> {
                        Map<String, Object> row = new HashMap<>();
                        row.put("table_name", rs.getString("table_name"));
                        row.put("partition_method", rs.getString("partition_method"));
                        row.put("part_num", rs.getInt("part_num"));
                        row.put("part_func_expr", rs.getString("part_func_expr"));
                        row.put("is_sub_part_template", rs.getInt("is_sub_part_template"));
                        row.put("part_name", rs.getString("part_name"));
                        row.put("part_id", rs.getInt("part_id"));
                        row.put("list_val", rs.getString("list_val"));
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
        DBTablePartitionOption partitionOption = new DBTablePartitionOption();
        partitionOption.setType(DBTablePartitionType.NOT_PARTITIONED);
        partition.setPartitionOption(partitionOption);
        partition.setSchemaName(schemaName);
        partition.setTableName(tableName);
        subPartition.setSchemaName(schemaName);

        DBTablePartitionOption subPartitionOption = new DBTablePartitionOption();
        subPartitionOption.setType(DBTablePartitionType.NOT_PARTITIONED);
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
            if (partitionDefinition.getType() == DBTablePartitionType.LIST
                    || partitionDefinition.getType() == DBTablePartitionType.LIST_COLUMNS) {
                description = (String) row.get("list_val");
            } else if (partitionDefinition.getType() == DBTablePartitionType.RANGE
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
                partition.setWarning("Only support HASH/KEY subpartition currently");
            }
        }
        return partition;
    }

    @Override
    public List<DBObjectIdentity> listSequences(String schemaName) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public List<DBTableColumn> listTableColumns(String schemaName, String tableName) {
        if ("mysql".equals(schemaName) && VIEW_AS_BASE_TABLE.contains(tableName)) {
            DBView view = new DBView();
            view.setSchemaName("mysql");
            view.setViewName(tableName);
            return fillColumnInfoByDesc(view).getColumns();
        }
        return super.listTableColumns(schemaName, tableName);
    }

    @Override
    public Map<String, List<DBTableColumn>> listBasicTableColumns(String schemaName) {
        String sql = sqlMapper.getSql(Statements.LIST_BASIC_SCHEMA_TABLE_COLUMNS);
        List<DBTableColumn> tableColumns =
                jdbcOperations.query(sql, new Object[] {schemaName}, listBasicTableColumnRowMapper());
        return tableColumns.stream().collect(Collectors.groupingBy(DBTableColumn::getTableName));
    }

    @Override
    public Map<String, List<DBTableColumn>> listBasicViewColumns(String schemaName) {
        MySQLSqlBuilder sb = new MySQLSqlBuilder();
        sb.append("select table_name from information_schema.views where table_schema=");
        sb.value(schemaName);
        List<String> viewNames = jdbcOperations.query(sb.toString(), (rs, rowNum) -> rs.getString("table_name"));
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
            MySQLSqlBuilder sb = new MySQLSqlBuilder();
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
