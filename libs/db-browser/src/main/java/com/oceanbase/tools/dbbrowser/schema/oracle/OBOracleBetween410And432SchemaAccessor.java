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

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessorSqlMappers;
import com.oceanbase.tools.dbbrowser.schema.constant.StatementsFiles;
import com.oceanbase.tools.dbbrowser.util.OracleDataDictTableNames;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.StringUtils;

/**
 * @description: applicable to OB [4.1.0,4.3.2)
 * @author: zijia.cj
 * @date: 2024/8/27 15:13
 * @since: 4.3.3
 */
public class OBOracleBetween410And432SchemaAccessor extends OBOracleSchemaAccessor {

    public OBOracleBetween410And432SchemaAccessor(JdbcOperations jdbcOperations,
            OracleDataDictTableNames dataDictTableNames) {
        super(jdbcOperations, dataDictTableNames);
        this.sqlMapper = DBSchemaAccessorSqlMappers.get(StatementsFiles.OBORACLE_4_1_x);
    }

    @Override
    public List<String> showExternalTables(String schemaName) {
        throw new UnsupportedOperationException(
                "External table is supported by odc after the 432 version of oceanbase");
    }


    @Override
    public List<String> showExternalTablesLike(String schemaName, String tableNameLike) {
        throw new UnsupportedOperationException(
                "External table is supported by odc after the 432 version of oceanbase");
    }

    @Override
    public List<DBObjectIdentity> listExternalTables(String schemaName, String tableNameLike) {
        throw new UnsupportedOperationException(
                "External table is supported by odc after the 432 version of oceanbase");
    }

    @Override
    public List<String> showTablesLike(String schemaName, String tableNameLike) {
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("SELECT TABLE_NAME FROM ");
        sb.append(dataDictTableNames.TABLES());
        sb.append(" WHERE OWNER=");
        sb.value(schemaName);
        if (StringUtils.isNotBlank(tableNameLike)) {
            sb.append(" AND TABLE_NAME LIKE ");
            sb.value(tableNameLike);
        }
        sb.append(" ORDER BY TABLE_NAME ASC");
        return jdbcOperations.queryForList(sb.toString(), String.class);
    }

    @Override
    public boolean syncExternalTableFiles(String schemaName, String tableName) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public List<DBObjectIdentity> listTables(String schemaName, String tableNameLike) {
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("select OWNER as schema_name, 'TABLE' as type,TABLE_NAME as name");
        sb.append(" from ");
        sb.append(dataDictTableNames.TABLES());
        sb.append(" where 1=1 ");

        if (StringUtils.isNotBlank(schemaName)) {
            sb.append(" AND OWNER=");
            sb.value(schemaName);
        }
        if (StringUtils.isNotBlank(tableNameLike)) {
            sb.append(" AND TABLE_NAME LIKE ");
            sb.value(tableNameLike);
        }
        sb.append(" ORDER BY schema_name, type, name");
        return jdbcOperations.query(sb.toString(), new BeanPropertyRowMapper<>(DBObjectIdentity.class));
    }

    @Override
    public Map<String, List<DBTableColumn>> listBasicExternalTableColumns(String schemaName) {
        throw new UnsupportedOperationException("not support yet");
    }

    @Override
    public List<DBTableColumn> listBasicExternalTableColumns(String schemaName, String externalTableName) {
        throw new UnsupportedOperationException("not support yet");
    }


}
