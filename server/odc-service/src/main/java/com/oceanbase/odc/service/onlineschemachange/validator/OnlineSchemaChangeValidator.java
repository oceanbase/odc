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
package com.oceanbase.odc.service.onlineschemachange.validator;

import static com.oceanbase.tools.dbbrowser.model.DBIndexType.UNIQUE;

import java.io.StringReader;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.LimitMetric;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.BadArgumentException;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.service.common.util.SqlUtils;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.onlineschemachange.ddl.DdlUtils;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeSqlType;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.sqlparser.OBMySQLParser;
import com.oceanbase.tools.sqlparser.OBOracleSQLParser;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTable;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;

/**
 * @author yaobin
 * @date 2023-05-23
 * @since 4.2.0
 */
@Component
public class OnlineSchemaChangeValidator {
    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private OscConnectionConfigValidator oscConnectionConfigValidator;

    public void validate(CreateFlowInstanceReq createReq) {
        OnlineSchemaChangeParameters parameter = (OnlineSchemaChangeParameters) createReq.getParameters();
        ConnectionConfig connectionConfig =
                connectionService.getForConnectionSkipPermissionCheck(createReq.getConnectionId());
        connectionConfig.setDefaultSchema(createReq.getDatabaseName());
        ConnectionSession session = new DefaultConnectSessionFactory(connectionConfig).generateSession();

        oscConnectionConfigValidator.valid(connectionConfig);

        try {
            List<String> sqls =
                    SqlUtils.split(connectionConfig.getDialectType(), parameter.getSqlContent(),
                            parameter.getDelimiter());
            for (String sql : sqls) {
                Statement statement =
                        (connectionConfig.getDialectType().isMysql() ? new OBMySQLParser() : new OBOracleSQLParser())
                                .parse(new StringReader(sql));

                validateType(sql, getSqlType(statement), parameter.getSqlType());

                String database = createReq.getDatabaseName();
                String tableName;
                if (parameter.getSqlType() == OnlineSchemaChangeSqlType.CREATE) {
                    CreateTable create = (CreateTable) statement;
                    tableName = create.getTableName();
                    if (create.getSchema() != null) {
                        validateSchema(create.getSchema(), database, connectionConfig.getDialectType());
                    }
                } else {
                    AlterTable alter = (AlterTable) statement;
                    tableName = alter.getTableName();
                    if (alter.getSchema() != null) {
                        validateSchema(alter.getSchema(), database, connectionConfig.getDialectType());
                    }
                }

                validateTableNameLength(tableName, connectionConfig.getDialectType());

                validateOriginTableExists(database, tableName, session);
                validateOldTableNotExists(database, tableName, session);
                validateTableConstraints(database, tableName, session);
            }
        } finally {
            session.expire();
        }

        // todo

        // valid in support scope

        // valid database user has privilege to create table
        // 数据库账号要有ALL PRIVILEGES权限或以下读写权限：
        // ALTER、CREATE、DELETE、DROP、INDEX、INSERT、SELECT、TRIGGER、UPDATE

    }

    private void validateSchema(String currentSchema, String expectedSchema, DialectType dialectType) {
        currentSchema = dialectType.isMysql() ? StringUtils.unquoteMySqlIdentifier(currentSchema)
                : StringUtils.unquoteOracleIdentifier(currentSchema);
        if (!StringUtils.equalsIgnoreCase(currentSchema, expectedSchema)) {
            throw new UnsupportedException(
                    String.format("The schema %s is different from expected schema %s", currentSchema, expectedSchema));
        }
    }

    private void validateTableNameLength(String tableName, DialectType dialectType) {
        PreConditions.lessThanOrEqualTo("table name", LimitMetric.TABLE_NAME_LENGTH, tableName.length(),
                dialectType.isMysql() ? 54 : 118);
    }

    private void validateOriginTableExists(String database, String tableName, ConnectionSession session) {
        DBSchemaAccessor accessor = DBSchemaAccessors.create(session);
        List<String> tables = accessor.showTablesLike(database, DdlUtils.getUnwrappedName(tableName));
        PreConditions.validExists(ResourceType.OB_TABLE, "tableName", tableName,
                () -> CollectionUtils.isNotEmpty(tables));
    }

    private void validateOldTableNotExists(String database, String tableName, ConnectionSession session) {
        DBSchemaAccessor accessor = DBSchemaAccessors.create(session);
        List<String> tables = accessor.showTablesLike(database, DdlUtils.getRenamedTableName(tableName));
        PreConditions.validNoDuplicated(ResourceType.OB_TABLE, "tableName",
                DdlUtils.getRenamedTableName(tableName), () -> CollectionUtils.isNotEmpty(tables));
    }

    private void validateTableConstraints(String database, String tableName, ConnectionSession session) {
        DBSchemaAccessor accessor = DBSchemaAccessors.create(session);
        List<DBTableIndex> indexes = accessor.listTableIndexes(database, DdlUtils.getUnwrappedName(tableName));
        if (indexes.stream().noneMatch(index -> index.getType() == UNIQUE)) {
            throw new UnsupportedException(ErrorCodes.NoUniqueKeyExists, new Object[] {tableName},
                    "There is no primary key or unique key exists in table " + tableName);
        }
    }

    private void validateType(String sql, OnlineSchemaChangeSqlType actualType,
            OnlineSchemaChangeSqlType expectedType) {
        if (actualType != expectedType) {
            throw new BadArgumentException(ErrorCodes.OscSqlTypeInconsistent,
                    new Object[] {actualType, expectedType, sql},
                    ErrorCodes.OscSqlTypeInconsistent
                            .getLocalizedMessage(new Object[] {actualType, expectedType, sql}));
        }
    }


    private OnlineSchemaChangeSqlType getSqlType(Statement statement) {
        return statement instanceof CreateTable ? OnlineSchemaChangeSqlType.CREATE : OnlineSchemaChangeSqlType.ALTER;
    }
}
