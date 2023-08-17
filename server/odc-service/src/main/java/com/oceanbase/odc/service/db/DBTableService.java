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
import java.util.stream.Collectors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.RequestTimeoutException;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.core.shared.model.TableIdentity;
import com.oceanbase.odc.service.common.util.SqlUtils;
import com.oceanbase.odc.service.db.browser.DBObjectEditorFactory;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.db.browser.DBStatsAccessors;
import com.oceanbase.odc.service.db.browser.DBTableEditorFactory;
import com.oceanbase.odc.service.db.model.GenerateTableDDLResp;
import com.oceanbase.odc.service.db.model.GenerateUpdateTableDDLReq;
import com.oceanbase.odc.service.session.ConnectConsoleService;
import com.oceanbase.tools.dbbrowser.editor.DBTableEditor;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTable.DBTableOptions;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.dbbrowser.stats.DBStatsAccessor;

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
        DBSchemaAccessor schemaAccessor = DBSchemaAccessors.create(session);
        String tableNameLike = SqlUtils.anyLike(fuzzyTableName);
        List<String> tableNames = schemaAccessor.showTablesLike(schemaName, tableNameLike).stream()
                .filter(name -> !StringUtils.endsWith(name.toUpperCase(), OdcConstants.VALIDATE_DDL_TABLE_POSTFIX))
                .collect(Collectors.toList());
        log.debug("showTablesLike, schemaName={}, tableNameLike={}, tableNamesCount={}",
                schemaName, tableNameLike, tableNames.size());
        return tableNames;
    }

    public DBTable getTable(@NotNull ConnectionSession connectionSession, String schemaName,
            @NotBlank String tableName) {
        DBSchemaAccessor schemaAccessor = DBSchemaAccessors.create(connectionSession);
        DBStatsAccessor statsAccessor = DBStatsAccessors.create(connectionSession);
        return buildTable(schemaAccessor, statsAccessor, schemaName, tableName);
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
        DBSchemaAccessor accessor = DBSchemaAccessors.create(connectionSession);
        return accessor.showTables(schemaName).stream()
                .filter(name -> !StringUtils.endsWithIgnoreCase(name, OdcConstants.VALIDATE_DDL_TABLE_POSTFIX))
                .map(item -> {
                    DBTable table = new DBTable();
                    table.setName(item);
                    table.setSchemaName(schemaName);
                    return table;
                }).collect(Collectors.toList());
    }

    public GenerateTableDDLResp generateCreateDDL(@NotNull ConnectionSession session, @NotNull DBTable table) {
        DBObjectEditorFactory<DBTableEditor> tableEditorFactory = new DBTableEditorFactory(
                session.getConnectType(), ConnectionSessionUtil.getVersion(session));
        return innerGenerateCreateDDL(tableEditorFactory, table);
    }

    public GenerateTableDDLResp generateUpdateDDL(@NotNull ConnectionSession session,
            @NotNull GenerateUpdateTableDDLReq req) {
        DBObjectEditorFactory<DBTableEditor> tableEditorFactory =
                new DBTableEditorFactory(session.getConnectType(), ConnectionSessionUtil.getVersion(session));
        return innerGenerateUpdateDDL(tableEditorFactory, req);
    }

    public GenerateTableDDLResp generateUpdateDDLWithoutRenaming(@NotNull ConnectionSession connectionSession,
            @NotNull GenerateUpdateTableDDLReq req) {
        DBObjectEditorFactory<DBTableEditor> tableEditorFactory =
                new DBTableEditorFactory(connectionSession.getConnectType(),
                        ConnectionSessionUtil.getVersion(connectionSession));
        DBTableEditor tableEditor = tableEditorFactory.create();
        String ddl = tableEditor.generateUpdateObjectDDLWithoutRenaming(req.getPrevious(), req.getCurrent());
        return GenerateTableDDLResp.builder()
                .sql(ddl)
                .currentIdentity(TableIdentity.of(req.getCurrent().getSchemaName(), req.getCurrent().getName()))
                .previousIdentity(TableIdentity.of(req.getPrevious().getSchemaName(), req.getPrevious().getName()))
                .build();
    }

    private GenerateTableDDLResp innerGenerateUpdateDDL(
            @NotNull DBObjectEditorFactory<DBTableEditor> tableEditorFactory,
            @NotNull GenerateUpdateTableDDLReq req) {
        DBTableEditor tableEditor = tableEditorFactory.create();
        String ddl = tableEditor.generateUpdateObjectDDL(req.getPrevious(), req.getCurrent());
        return GenerateTableDDLResp.builder()
                .sql(ddl)
                .currentIdentity(TableIdentity.of(req.getCurrent().getSchemaName(), req.getCurrent().getName()))
                .previousIdentity(TableIdentity.of(req.getPrevious().getSchemaName(), req.getPrevious().getName()))
                .build();
    }

    private GenerateTableDDLResp innerGenerateCreateDDL(
            @NotNull DBObjectEditorFactory<DBTableEditor> tableEditorFactory,
            @NotNull DBTable table) {
        DBTableEditor tableEditor = tableEditorFactory.create();
        String ddl = tableEditor.generateCreateObjectDDL(table);
        return GenerateTableDDLResp.builder()
                .sql(ddl)
                .currentIdentity(TableIdentity.of(table.getSchemaName(), table.getName()))
                .previousIdentity(TableIdentity.of(table.getSchemaName(), table.getName()))
                .build();
    }

    public Boolean isLowerCaseTableName(@NotNull ConnectionSession connectionSession) {
        DBSchemaAccessor schemaAccessor = DBSchemaAccessors.create(connectionSession);
        return schemaAccessor.isLowerCaseTableName();
    }

    private DBTable buildTable(@NotNull DBSchemaAccessor schemaAccessor, @NotNull DBStatsAccessor statsAccessor,
            String schemaName, @NotBlank String tableName) {
        PreConditions.validExists(ResourceType.OB_TABLE, "tableName", tableName,
                () -> schemaAccessor.showTables(schemaName).stream().filter(name -> name.equals(tableName))
                        .collect(Collectors.toList()).size() > 0);
        try {
            DBTable table = new DBTable();
            table.setSchemaName(schemaName);
            table.setOwner(schemaName);
            table.setName(schemaAccessor.isLowerCaseTableName() ? tableName.toLowerCase() : tableName);
            table.setColumns(schemaAccessor.listTableColumns(schemaName, tableName));
            table.setConstraints(schemaAccessor.listTableConstraints(schemaName, tableName));
            try {
                table.setPartition(schemaAccessor.getPartition(schemaName, tableName));
            } catch (Exception e) {
                DBTablePartition partition = new DBTablePartition();
                partition.setWarning(e.getMessage());
                table.setPartition(partition);
            }
            table.setIndexes(schemaAccessor.listTableIndexes(schemaName, tableName));
            table.setDDL(schemaAccessor.getTableDDL(schemaName, tableName));
            table.setTableOptions(schemaAccessor.getTableOptions(schemaName, tableName));
            table.setStats(statsAccessor.getTableStats(schemaName, tableName));
            return table;
        } catch (TransientDataAccessResourceException e1) {
            throw new RequestTimeoutException(String
                    .format("Query table information timeout, table name=%s, error massage=%s", tableName,
                            e1.getMessage()));
        } catch (Exception e) {
            throw new UnexpectedException(String
                    .format("Query table information failed, table name=%s, error massage=%s", tableName,
                            e.getMessage()));
        }
    }

}
