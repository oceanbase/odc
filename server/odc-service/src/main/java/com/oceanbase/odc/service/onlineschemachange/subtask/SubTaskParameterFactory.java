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
import com.oceanbase.odc.service.onlineschemachange.ddl.OscFactoryWrapper;
import com.oceanbase.odc.service.onlineschemachange.ddl.ReplaceResult;
import com.oceanbase.odc.service.onlineschemachange.ddl.TableNameDescriptor;
import com.oceanbase.odc.service.onlineschemachange.ddl.TableNameDescriptorFactory;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeSqlType;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.tools.sqlparser.SQLParser;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTable;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;

public class SubTaskParameterFactory implements AutoCloseable {

    private final Long connectionId;
    private final ConnectionConfig connectionConfig;
    protected final ConnectionSession session;
    private final String schema;
    private final TableNameDescriptorFactory tableNameDescriptorFactory;
    private final SQLParser sqlParser;

    public SubTaskParameterFactory(ConnectionConfig connectionConfig, String schema,
            OscFactoryWrapper oscFactoryWrapper) {
        this.connectionId = connectionConfig.id();
        this.connectionConfig = connectionConfig;
        this.session = new DefaultConnectSessionFactory(connectionConfig).generateSession();
        ConnectionSessionUtil.setCurrentSchema(session, schema);
        this.schema = schema;
        this.tableNameDescriptorFactory = oscFactoryWrapper.getTableNameDescriptorFactory();
        this.sqlParser = oscFactoryWrapper.getSqlParser();
    }

    public OnlineSchemaChangeScheduleTaskParameters generate(String sql, OnlineSchemaChangeSqlType sqlType,
            Statement statement)
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
        if (sqlType == OnlineSchemaChangeSqlType.ALTER) {
            AlterTable statement = (AlterTable) parse(sql);
            String tableName = statement.getTableName();
            TableNameDescriptor tableNameDescriptor = tableNameDescriptorFactory.getTableNameDescriptor(tableName);
            String originTableCreateDdl = DdlUtils.queryOriginTableCreateDdl(session, tableName);
            taskParameter.setOriginTableCreateDdl(originTableCreateDdl);
            ReplaceResult replaceResult = DdlUtils.replaceTableName(originTableCreateDdl,
                    tableNameDescriptor.getNewTableName(), session.getDialectType(), OnlineSchemaChangeSqlType.CREATE);
            taskParameter.setNewTableCreateDdl(replaceResult.getNewSql());
            taskParameter.setReplaceResult(replaceResult);

            populateTaskParameter(tableNameDescriptor, taskParameter, tableName);
        } else {
            CreateTable statement = (CreateTable) parse(sql);
            String tableName = statement.getTableName();
            String originTableCreateDdl = DdlUtils.queryOriginTableCreateDdl(session, tableName);
            taskParameter.setOriginTableCreateDdl(originTableCreateDdl);
            TableNameDescriptor tableNameDescriptor = tableNameDescriptorFactory.getTableNameDescriptor(tableName);
            ReplaceResult replaceResult = DdlUtils.replaceTableName(sql,
                    tableNameDescriptor.getNewTableName(), session.getDialectType(), OnlineSchemaChangeSqlType.CREATE);
            taskParameter.setNewTableCreateDdl(replaceResult.getNewSql());
            taskParameter.setNewTableCreateDdlForDisplay(sql);
            taskParameter.setReplaceResult(replaceResult);

            populateTaskParameter(tableNameDescriptor, taskParameter, tableName);
        }
        return taskParameter;
    }

    private void populateTaskParameter(TableNameDescriptor tableNameDescriptor,
            OnlineSchemaChangeScheduleTaskParameters taskParameter,
            String tableName) {
        taskParameter.setOriginTableName(tableName);
        taskParameter.setNewTableName(tableNameDescriptor.getNewTableName());
        taskParameter.setRenamedTableName(tableNameDescriptor.getRenamedTableName());
        taskParameter.setNewTableNameUnwrapped(tableNameDescriptor.getNewTableNameUnWrapped());
        taskParameter.setOriginTableNameUnwrapped(tableNameDescriptor.getOriginTableNameUnwrapped());
        taskParameter.setRenamedTableNameUnwrapped(tableNameDescriptor.getRenamedTableNameUnWrapped());


    }

    private Statement parse(String sql) {
        return sqlParser.parse(new StringReader(sql));
    }


    private String unquote(String value) {
        return session.getDialectType().isMysql() ? StringUtils.unquoteMySqlIdentifier(value)
                : StringUtils.unquoteOracleIdentifier(value);
    }
}
