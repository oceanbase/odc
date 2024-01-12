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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
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
    public List<DBObjectIdentity> listTables(String schemaName, String tableNameLike) {
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("select\n"
                + "  t1.DATABASE_NAME as schema_name,\n"
                + "  t2.TABLE_NAME as name,\n"
                + "  'TABLE' as type \n"
                + "from \n"
                + "  SYS.ALL_VIRTUAL_DATABASE_REAL_AGENT t1,\n"
                + "  SYS.ALL_VIRTUAL_TABLE_REAL_AGENT t2\n"
                + "where \n"
                + "  t1.database_id=t2.database_id\n"
                + "  and t2.table_type = 3\n");

        if (StringUtils.isNotBlank(schemaName)) {
            sb.append("  and t1.database_name = ").value(schemaName);
        }
        if (StringUtils.isNotBlank(tableNameLike)) {
            sb.append("  and t2.table_name LIKE ").value(tableNameLike);
        }
        sb.append(" order by t1.database_name, t2.table_name");
        return jdbcOperations.query(sb.toString(), new BeanPropertyRowMapper<>(DBObjectIdentity.class));
    }

    @Override
    public DBTableOptions getTableOptions(String schemaName, String tableName) {
        DBTableOptions tableOptions = new DBTableOptions();
        obtainTableCharset(Collections.singletonList(tableOptions));
        obtainTableCollation(Collections.singletonList(tableOptions));
        String sql = this.sqlMapper.getSql(Statements.GET_TABLE_OPTION);
        try {
            this.jdbcOperations.query(sql.toString(), new Object[] {schemaName, tableName},
                    rs -> {
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
        DBTablePartition partition = new DBTablePartition();
        partition.setPartitionOption(new DBTablePartitionOption());
        partition.setPartitionDefinitions(new ArrayList<>());

        String sql = this.sqlMapper.getSql(Statements.GET_TABLE_PARTITION);

        try {
            this.jdbcOperations.query(sql.toString(), new Object[] {schemaName, tableName},
                    rs -> {
                        DBTablePartitionOption option = partition.getPartitionOption();
                        option.setType(DBTablePartitionType.fromValue(rs.getString("PARTITION_METHOD")));
                        option.setPartitionsNum(rs.getInt("PART_NUM"));
                        String expression = rs.getString("EXPRESSION");
                        if (option.getType().supportExpression()) {
                            option.setExpression(expression);
                        } else {
                            option.setColumnNames(Arrays.asList(expression.split(",")));
                        }
                        DBTablePartitionDefinition partitionDefinition = new DBTablePartitionDefinition();
                        partitionDefinition.setName(rs.getString("PART_NAME"));
                        partitionDefinition.setOrdinalPosition(rs.getInt("PART_ID"));
                        partitionDefinition.setType(option.getType());
                        String maxValue = rs.getString("MAX_VALUE");
                        String listValue = rs.getString("LIST_VALUE");
                        partitionDefinition.fillValues(StringUtils.isNotEmpty(maxValue) ? maxValue : listValue);

                        partition.getPartitionDefinitions().add(partitionDefinition);
                    });
        } catch (Exception ex) {
            log.warn("get table partitions failed, schema={}, table={}, reason:", schemaName, tableName, ex);
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
