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
package com.oceanbase.odc.service.onlineschemachange.subtask;

import java.io.StringReader;
import java.sql.SQLException;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.onlineschemachange.ddl.DdlUtils;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeSqlType;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.tools.sqlparser.OBMySQLParser;
import com.oceanbase.tools.sqlparser.OBOracleSQLParser;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTable;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;

public class SubTaskParameterFactory implements AutoCloseable {

    private final Long connectionId;
    private final ConnectionConfig connectionConfig;
    protected final ConnectionSession session;
    private final String schema;

    public SubTaskParameterFactory(ConnectionConfig connectionConfig, String schema) {
        this.connectionId = connectionConfig.id();
        this.connectionConfig = connectionConfig;
        this.session = new DefaultConnectSessionFactory(connectionConfig).generateSession();
        ConnectionSessionUtil.setCurrentSchema(session, schema);
        this.schema = schema;
    }

    public OnlineSchemaChangeScheduleTaskParameters generate(String sql, OnlineSchemaChangeSqlType sqlType)
            throws SQLException {
        OnlineSchemaChangeScheduleTaskParameters taskParameter = createNewParameter(sql, sqlType);
        taskParameter.setDialectType(connectionConfig.getDialectType());
        taskParameter.setConnectionId(connectionId);
        taskParameter.setDatabaseName(schema);
        return taskParameter;
    }

    @Override
    public void close() throws Exception {
        if (session != null) {
            session.expire();
        }
    }

    private OnlineSchemaChangeScheduleTaskParameters createNewParameter(String sql, OnlineSchemaChangeSqlType sqlType)
            throws SQLException {
        OnlineSchemaChangeScheduleTaskParameters taskParameter = new OnlineSchemaChangeScheduleTaskParameters();
        taskParameter.setNewTableName(DdlUtils.getNewTableName(taskParameter.getOriginTableName()));
        taskParameter.setRenamedTableName(DdlUtils.getRenamedTableName(taskParameter.getOriginTableName()));
        if (sqlType == OnlineSchemaChangeSqlType.ALTER) {
            AlterTable statement = (AlterTable) parse(sql);
            String tableName = statement.getTableName();
            taskParameter.setOriginTableName(tableName);

            String originTableCreateDdl = DdlUtils.queryOriginTableCreateDdl(session, tableName);
            taskParameter.setOriginTableCreateDdl(originTableCreateDdl);
            taskParameter.setNewTableCreateDdl(DdlUtils.replaceTableName(originTableCreateDdl,
                    DdlUtils.getNewTableName(tableName), session.getDialectType(), sqlType));

            taskParameter.setNewTableCreateDdlForDisplay("");
        } else {
            CreateTable statement = (CreateTable) parse(sql);
            String tableName = statement.getTableName();
            taskParameter.setOriginTableName(tableName);

            taskParameter.setOriginTableCreateDdl(DdlUtils.queryOriginTableCreateDdl(session, tableName));
            taskParameter.setNewTableCreateDdl(DdlUtils.replaceTableName(sql,
                    DdlUtils.getNewTableName(tableName), session.getDialectType(), sqlType));

            taskParameter.setNewTableCreateDdlForDisplay(sql);
        }
        return taskParameter;
    }

    private Statement parse(String sql) {
        return (session.getDialectType().isMysql() ? new OBMySQLParser() : new OBOracleSQLParser())
                .parse(new StringReader(sql));
    }

    private String unquote(String value) {
        return session.getDialectType().isMysql() ? StringUtils.unquoteMySqlIdentifier(value)
                : StringUtils.unquoteOracleIdentifier(value);
    }
}
