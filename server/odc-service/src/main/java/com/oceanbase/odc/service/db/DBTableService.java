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
package com.oceanbase.odc.service.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.core.shared.model.TableIdentity;
import com.oceanbase.odc.core.sql.parser.AbstractSyntaxTreeFactories;
import com.oceanbase.odc.core.sql.parser.AbstractSyntaxTreeFactory;
import com.oceanbase.odc.core.sql.parser.DropStatement;
import com.oceanbase.odc.plugin.schema.api.TableExtensionPoint;
import com.oceanbase.odc.service.common.util.SqlUtils;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.db.model.GenerateTableDDLResp;
import com.oceanbase.odc.service.db.model.GenerateUpdateTableDDLReq;
import com.oceanbase.odc.service.db.model.UpdateTableDdlCheck;
import com.oceanbase.odc.service.plugin.SchemaPluginUtil;
import com.oceanbase.odc.service.session.ConnectConsoleService;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTable.DBTableOptions;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTable;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTableAction;
import com.oceanbase.tools.sqlparser.statement.createindex.CreateIndex;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SkipAuthorize("inside connect session")
public class DBTableService {
    @Autowired
    private ConnectConsoleService consoleService;

    /**
     * show tables from schemaName like tableName
     *
     * @param session session
     * @param schemaName use session current database if null
     * @param fuzzyTableName show all tables if null or blank
     */
    public List<String> showTablesLike(@NotNull ConnectionSession session, String schemaName, String fuzzyTableName) {
        String tableNameLike = SqlUtils.anyLike(fuzzyTableName);
        List<String> tableNames = session.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<List<String>>) con -> getTableExtensionPoint(session)
                        .showNamesLike(con, schemaName, tableNameLike).stream()
                        .filter(name -> !StringUtils.endsWith(name.toUpperCase(),
                                OdcConstants.VALIDATE_DDL_TABLE_POSTFIX))
                        .collect(Collectors.toList()));
        log.debug("showTablesLike, schemaName={}, tableNameLike={}, tableNamesCount={}",
                schemaName, tableNameLike, tableNames.size());
        return tableNames;
    }

    public DBTable getTable(@NotNull ConnectionSession connectionSession, String schemaName,
            @NotBlank String tableName) {
        DBSchemaAccessor schemaAccessor = DBSchemaAccessors.create(connectionSession);
        PreConditions.validExists(ResourceType.OB_TABLE, "tableName", tableName,
                () -> schemaAccessor.showTables(schemaName).stream().filter(name -> name.equals(tableName))
                        .collect(Collectors.toList()).size() > 0);
        try {
            return connectionSession.getSyncJdbcExecutor(
                    ConnectionSessionConstants.BACKEND_DS_KEY)
                    .execute((ConnectionCallback<DBTable>) con -> getTableExtensionPoint(connectionSession)
                            .getDetail(con, schemaName, tableName));
        } catch (Exception e) {
            throw new UnexpectedException(String
                    .format("Query table information failed, table name=%s, error massage=%s", tableName,
                            e.getMessage()));
        }
    }

    public List<DBTable> listTables(@NotNull ConnectionSession connectionSession, String schemaName,
            @NotEmpty List<String> tableNames) {
        DBSchemaAccessor schemaAccessor = DBSchemaAccessors.create(connectionSession);
        List<DBTable> tables = new ArrayList<>();
        Map<String, List<DBTableColumn>> tableName2Columns = schemaAccessor.listTableColumns(schemaName);
        Map<String, List<DBTableIndex>> tableName2Indexes = schemaAccessor.listTableIndexes(schemaName);
        Map<String, List<DBTableConstraint>> tableName2Constraints = schemaAccessor.listTableConstraints(schemaName);
        Map<String, DBTableOptions> tableName2Options = schemaAccessor.listTableOptions(schemaName);
        for (String tableName : tableNames) {
            if (!tableName2Columns.containsKey(tableName)) {
                continue;
            }
            DBTable table = new DBTable();
            table.setSchemaName(schemaName);
            table.setOwner(schemaName);
            table.setName(tableName);
            table.setColumns(tableName2Columns.getOrDefault(tableName, Lists.newArrayList()));
            table.setIndexes(tableName2Indexes.getOrDefault(tableName, Lists.newArrayList()));
            table.setConstraints(tableName2Constraints.getOrDefault(tableName, Lists.newArrayList()));
            table.setTableOptions(tableName2Options.getOrDefault(tableName, new DBTableOptions()));
            table.setPartition(schemaAccessor.getPartition(schemaName, tableName));
            table.setDDL(schemaAccessor.getTableDDL(schemaName, tableName));
            tables.add(table);
        }
        return tables;
    }

    public List<DBTable> listTables(@NotNull ConnectionSession connectionSession, String schemaName) {
        return connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<List<DBObjectIdentity>>) con -> getTableExtensionPoint(connectionSession)
                        .list(con, schemaName))
                .stream().map(item -> {
                    DBTable table = new DBTable();
                    table.setName(item.getName());
                    table.setSchemaName(schemaName);
                    return table;
                }).collect(Collectors.toList());
    }

    public GenerateTableDDLResp generateCreateDDL(@NotNull ConnectionSession session, @NotNull DBTable table) {
        String ddl = session.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<String>) con -> getTableExtensionPoint(session).generateCreateDDL(con,
                        table));
        return GenerateTableDDLResp.builder()
                .sql(ddl)
                .currentIdentity(TableIdentity.of(table.getSchemaName(), table.getName()))
                .previousIdentity(TableIdentity.of(table.getSchemaName(), table.getName()))
                .build();
    }

    public GenerateTableDDLResp generateUpdateDDL(@NotNull ConnectionSession session,
            @NotNull GenerateUpdateTableDDLReq req) {
        String ddl = session.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<String>) con -> getTableExtensionPoint(session).generateUpdateDDL(con,
                        req.getPrevious(), req.getCurrent()));
        return GenerateTableDDLResp.builder()
                .sql(ddl)
                .currentIdentity(TableIdentity.of(req.getCurrent().getSchemaName(), req.getCurrent().getName()))
                .previousIdentity(TableIdentity.of(req.getPrevious().getSchemaName(), req.getPrevious().getName()))
                .tip(checkUpdateDDL(session.getDialectType(), ddl))
                .build();
    }

    private String checkUpdateDDL(DialectType dialectType, String ddl) {
        boolean createIndex = false;
        boolean dropIndex = false;
        for (String s : SqlUtils.split(dialectType, ddl, ";")) {
            Statement stmt = parseSingleSql(dialectType, s);
            if (stmt == null) {
                continue;
            }
            if (stmt instanceof CreateIndex) {
                createIndex = true;
            } else if (stmt instanceof DropStatement && ((DropStatement) stmt).getObjectType().equals("INDEX")) {
                dropIndex = true;
            } else if (stmt instanceof AlterTable) {
                for (AlterTableAction tableAction : ((AlterTable) stmt).getAlterTableActions()) {
                    if (Objects.nonNull(tableAction.getAddIndex())) {
                        createIndex = true;
                    } else if (Objects.nonNull(tableAction.getDropIndexName())) {
                        dropIndex = true;
                    }
                }
            }
        }
        if (dropIndex && createIndex) {
            return UpdateTableDdlCheck.DROP_AND_CREATE_INDEX.getLocalizedMessage();
        } else if (dropIndex) {
            return UpdateTableDdlCheck.DROP_INDEX.getLocalizedMessage();
        } else if (createIndex) {
            return UpdateTableDdlCheck.CREATE_INDEX.getLocalizedMessage();
        }
        return null;
    }

    private Statement parseSingleSql(DialectType dialectType, String sql) {
        try {
            AbstractSyntaxTreeFactory factory = AbstractSyntaxTreeFactories.getAstFactory(dialectType, 0);
            Validate.notNull(factory, "AbstractSyntaxTreeFactory can not be null");
            return factory.buildAst(sql).getStatement();
        } catch (Exception e) {
            log.warn("parse generated update table sql failed, sql={}, error={}", sql, e.getMessage());
            return null;
        }
    }

    public Boolean isLowerCaseTableName(@NotNull ConnectionSession connectionSession) {
        DBSchemaAccessor schemaAccessor = DBSchemaAccessors.create(connectionSession);
        return schemaAccessor.isLowerCaseTableName();
    }

    private TableExtensionPoint getTableExtensionPoint(@NotNull ConnectionSession connectionSession) {
        return SchemaPluginUtil.getTableExtension(connectionSession.getDialectType());
    }

}
