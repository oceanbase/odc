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
import java.util.Collections;
import java.util.List;

import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.tools.dbbrowser.model.DBDatabase;
import com.oceanbase.tools.dbbrowser.model.DBFunction;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBPLObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBProcedure;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.StringUtils;

/**
 * @author jingtian
 */
public class ODPOBMySQLSchemaAccessor extends MySQLNoGreaterThan5740SchemaAccessor {

    public ODPOBMySQLSchemaAccessor(JdbcOperations jdbcOperations) {
        super(jdbcOperations);
    }

    @Override
    public List<DBDatabase> listDatabases() {
        List<DBDatabase> dbDatabases = new ArrayList<>();
        String sql = "show databases";
        List<String> dbNames = jdbcOperations.queryForList(sql, String.class);
        for (String dbName : dbNames) {
            DBDatabase database = new DBDatabase();
            jdbcOperations.execute("use " + dbName);
            database.setName(dbName);
            database.setId(dbName);
            database.setCharset(
                    jdbcOperations.queryForObject("show variables like 'character_set_database';",
                            (rs, num) -> rs.getString(2)));
            database.setCollation(
                    jdbcOperations.queryForObject("show variables like 'collation_database';",
                            (rs, num) -> rs.getString(2)));
            dbDatabases.add(database);
        }
        return dbDatabases;
    }

    @Override
    public List<String> showTables(String schemaName) {
        MySQLSqlBuilder sb = new MySQLSqlBuilder();
        sb.append("SHOW TABLES ");
        if (StringUtils.isNotBlank(schemaName)) {
            sb.append("FROM ");
            sb.identifier(schemaName);
            sb.append(" ");
        }
        String sql = sb.toString();
        return jdbcOperations.queryForList(sql, String.class);
    }

    @Override
    public List<DBObjectIdentity> listTables(String schemaName, String tableNameLike) {
        String sql = "select database()";
        String currentSchema = jdbcOperations.queryForObject(sql, (rs, rowNum) -> rs.getString(1));
        List<DBObjectIdentity> results = new ArrayList<>();
        MySQLSqlBuilder builder = new MySQLSqlBuilder();
        builder.append("show full tables");
        if (StringUtils.isNotBlank(schemaName)) {
            builder.append(" from ").identifier(schemaName);
        }
        builder.append(" where Table_type='BASE TABLE'");
        List<String> tables = jdbcOperations.query(builder.toString(), (rs, rowNum) -> rs.getString(1));
        tables.forEach(name -> results.add(DBObjectIdentity
                .of(StringUtils.isBlank(schemaName) ? currentSchema : schemaName, DBObjectType.TABLE, name)));
        return results;
    }

    @Override
    public List<String> showTablesLike(String schemaName, String tableNameLike) {
        MySQLSqlBuilder sb = new MySQLSqlBuilder();
        if (tableNameLike == null) {
            sb.append("show tables");
        } else {
            sb.append("show tables like ").value("%" + tableNameLike + "%");
        }
        return jdbcOperations.queryForList(sb.toString(), String.class);
    }

    @Override
    public List<DBObjectIdentity> listViews(String schemaName) {
        return Collections.emptyList();
    }

    @Override
    public List<DBObjectIdentity> listAllViews(String viewNameLike) {
        return Collections.emptyList();
    }

    @Override
    public List<DBObjectIdentity> listAllUserViews() {
        String sql = "select database()";
        String currentSchema = jdbcOperations.queryForObject(sql, (rs, rowNum) -> rs.getString(1));
        List<DBObjectIdentity> results = new ArrayList<>();
        sql = "show full tables where Table_type='VIEW'";
        List<String> views = jdbcOperations.query(sql, (rs, rowNum) -> rs.getString(1));
        views.forEach(name -> results.add(DBObjectIdentity.of(currentSchema, DBObjectType.VIEW, name)));
        return results;
    }

    @Override
    public List<DBObjectIdentity> listAllSystemViews() {
        return Collections.emptyList();
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
