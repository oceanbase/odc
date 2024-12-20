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

import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.core.sql.split.OffsetString;
import com.oceanbase.odc.service.common.util.SqlUtils;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.onlineschemachange.ddl.DdlUtils;
import com.oceanbase.odc.service.onlineschemachange.ddl.OscFactoryWrapper;
import com.oceanbase.odc.service.onlineschemachange.ddl.OscFactoryWrapperGenerator;
import com.oceanbase.odc.service.onlineschemachange.ddl.TableNameDescriptor;
import com.oceanbase.odc.service.onlineschemachange.ddl.TableNameDescriptorFactory;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeSqlType;
import com.oceanbase.odc.service.onlineschemachange.rename.LockTableSupportDecider;
import com.oceanbase.odc.service.onlineschemachange.rename.OscDBUserUtil;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.tools.dbbrowser.model.DBConstraintType;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.sqlparser.OBMySQLParser;
import com.oceanbase.tools.sqlparser.OBOracleSQLParser;
import com.oceanbase.tools.sqlparser.SQLParser;
import com.oceanbase.tools.sqlparser.SyntaxErrorException;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTable;
import com.oceanbase.tools.sqlparser.statement.createindex.CreateIndex;
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
        PreConditions.notEmpty(parameter.getSqlContent(), "Input sql cant not bee empty");

        ConnectionConfig connectionConfig =
                connectionService.getForConnectionSkipPermissionCheck(createReq.getConnectionId());
        connectionConfig.setDefaultSchema(createReq.getDatabaseName());
        List<String> sqls = SqlUtils.splitWithOffset(connectionConfig.getDialectType(),
                parameter.getSqlContent() + "\n",
                parameter.getDelimiter(), true).stream().map(OffsetString::getStr).collect(
                        Collectors.toList());

        PreConditions.notEmpty(sqls, "Parser sqls is empty");
        oscConnectionConfigValidator.valid(connectionConfig);

        List<Statement> statements = parseStatements(parameter, connectionConfig, sqls);

        ConnectionSession session = new DefaultConnectSessionFactory(connectionConfig).generateSession();
        try {
            for (Statement statement : statements) {
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
                OscFactoryWrapper oscFactoryWrapper = OscFactoryWrapperGenerator.generate(session.getDialectType());
                TableNameDescriptorFactory tableNameDescriptorFactory =
                        oscFactoryWrapper.getTableNameDescriptorFactory();
                TableNameDescriptor tableNameDescriptor = tableNameDescriptorFactory.getTableNameDescriptor(tableName);

                validateTableNameLength(tableName, connectionConfig.getDialectType());
                validateOriginTableExists(database, tableName, session);
                // valid check ghost table and renamed table not exists
                validateTableNotExists(database, tableNameDescriptor.getNewTableNameUnWrapped(), session);
                validateTableNotExists(database, tableNameDescriptor.getRenamedTableNameUnWrapped(), session);
                // valid constraints
                validateForeignKeyTable(database, tableName, session);
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

    private List<Statement> parseStatements(OnlineSchemaChangeParameters parameter,
            ConnectionConfig connectionConfig, List<String> sqls) {
        List<Statement> statements = null;
        try {
            SQLParser sqlParser =
                    connectionConfig.getDialectType().isMysql() ? new OBMySQLParser() : new OBOracleSQLParser();
            statements = sqls.stream().map(sql -> {
                Statement statement = sqlParser.parse(new StringReader(sql));
                // skip valid type when statement is "create index"
                if (statement instanceof CreateTable || statement instanceof AlterTable) {
                    validateType(sql, getSqlType(statement), parameter.getSqlType());
                } else {
                    PreConditions.validArgumentState(statement instanceof CreateIndex,
                            ErrorCodes.OscSqlTypeInconsistent, new Object[] {sql}, "Unsupported sql type");
                }
                return statement;
            }).filter(statement -> !(statement instanceof CreateIndex)).collect(Collectors.toList());
        } catch (SyntaxErrorException ex) {
            throw new BadArgumentException(ErrorCodes.ObPreCheckDdlFailed, ex.getLocalizedMessage());
        }
        return statements;
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

    private void validateTableNotExists(String database, String tableName, ConnectionSession session) {
        DBSchemaAccessor accessor = DBSchemaAccessors.create(session);
        List<String> tables = accessor.showTablesLike(database, DdlUtils.getUnwrappedName(tableName));
        PreConditions.validNoDuplicated(ResourceType.OB_TABLE, "tableName",
                tableName, () -> CollectionUtils.isNotEmpty(tables));
    }

    private void validateForeignKeyTable(String database, String tableName, ConnectionSession session) {
        DBSchemaAccessor accessor = DBSchemaAccessors.create(session);
        List<DBTableConstraint> constraints =
                accessor.listTableConstraints(database, DdlUtils.getUnwrappedName(tableName));

        if (constraints.stream().anyMatch(index -> index.getType() == DBConstraintType.FOREIGN_KEY)) {
            throw new UnsupportedException(ErrorCodes.OscUnsupportedForeignKeyTable, new Object[] {tableName},
                    "Unsupported foreign key table " + tableName);
        }
    }

    private void validateTableConstraints(String database, String tableName, ConnectionSession session) {
        DBSchemaAccessor accessor = DBSchemaAccessors.create(session);
        List<DBTableConstraint> constraints =
                accessor.listTableConstraints(database, DdlUtils.getUnwrappedName(tableName));
        if (CollectionUtils.isEmpty(constraints)) {
            throw new UnsupportedException(ErrorCodes.NoUniqueKeyExists, new Object[] {tableName},
                    "There is no primary key or not nullable unique key in table " + tableName);
        }
        if (constraints.stream().anyMatch(index -> index.getType() == DBConstraintType.PRIMARY_KEY)) {
            return;
        }
        // Check unique key reference columns is not null
        List<DBTableConstraint> uniques = constraints.stream()
                .filter(c -> c.getType() == DBConstraintType.UNIQUE_KEY).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(uniques)) {
            throw new UnsupportedException(ErrorCodes.NoUniqueKeyExists, new Object[] {tableName},
                    "There is no primary key or not nullable unique key in table " + tableName);
        }

        validateUniqueKeyIsConstraintNullable(database, tableName, session, uniques);
    }

    private void validateUniqueKeyIsConstraintNullable(String database, String tableName, ConnectionSession session,
            List<DBTableConstraint> uniques) {
        Map<String, DBTableColumn> dbTableColumns =
                DBSchemaAccessors.create(session).listTableColumns(database,
                        DdlUtils.getUnwrappedName(tableName)).stream().collect(
                                Collectors.toMap(DBTableColumn::getName, v -> v));
        boolean existsNullableColumnUk = uniques.stream().anyMatch(
                uk -> uk.getColumnNames().stream().noneMatch(column -> dbTableColumns.get(column).getNullable()));

        if (!existsNullableColumnUk) {
            throw new UnsupportedException(ErrorCodes.NoUniqueKeyExists, new Object[] {tableName},
                    "There is no primary key or not nullable unique key in table " + tableName);
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

    private void validateLockUser(DialectType dialectType, String obVersion, List<String> lockUsers) {
        if (OscDBUserUtil.isLockUserRequired(dialectType, () -> obVersion,
                () -> LockTableSupportDecider.DEFAULT_LOCK_TABLE_DECIDER) && CollectionUtils.isEmpty(lockUsers)) {
            throw new BadRequestException(ErrorCodes.OscLockUserRequired, new Object[] {lockUsers},
                    "Current db version should lock user required, but parameters do not contains user to lock.");
        }
    }

    private OnlineSchemaChangeSqlType getSqlType(Statement statement) {
        return statement instanceof CreateTable ? OnlineSchemaChangeSqlType.CREATE : OnlineSchemaChangeSqlType.ALTER;
    }
}
