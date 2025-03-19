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
package com.oceanbase.odc.plugin.schema.obmysql;

import java.sql.Connection;
import java.util.List;
import java.util.Objects;

import org.pf4j.Extension;

import com.oceanbase.odc.common.util.JdbcOperationsUtil;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.plugin.schema.api.MViewExtensionPoint;
import com.oceanbase.odc.plugin.schema.obmysql.parser.OBMySQLGetDBTableByParser;
import com.oceanbase.odc.plugin.schema.obmysql.utils.DBAccessorUtil;
import com.oceanbase.tools.dbbrowser.DBBrowser;
import com.oceanbase.tools.dbbrowser.editor.DBObjectOperator;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLObjectOperator;
import com.oceanbase.tools.dbbrowser.model.DBMViewRefreshParameter;
import com.oceanbase.tools.dbbrowser.model.DBMaterializedView;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.parser.SqlParser;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.dbbrowser.template.DBObjectTemplate;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.creatematerializedview.CreateMaterializedView;

import lombok.extern.slf4j.Slf4j;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/4 16:15
 * @since: 4.3.4
 */
@Extension
@Slf4j
public class OBMySQLMVExtension implements MViewExtensionPoint {
    @Override
    public List<DBObjectIdentity> list(Connection connection, String schemaName) {
        return getSchemaAccessor(connection).listMViews(schemaName);
    }

    @Override
    public DBMaterializedView getDetail(Connection connection, String schemaName, String mViewName) {
        DBSchemaAccessor schemaAccessor = getSchemaAccessor(connection);
        DBMaterializedView mView = schemaAccessor.getMView(schemaName, mViewName);
        String ddl = schemaAccessor.getTableDDL(schemaName, mViewName);
        CreateMaterializedView createMaterializedView = parseTableDDL(ddl);
        if (Objects.nonNull(createMaterializedView.getCreateMaterializedViewOpts())
                && Objects.nonNull(
                        createMaterializedView.getCreateMaterializedViewOpts().getMaterializedViewRefreshOpts())
                && Objects.nonNull(createMaterializedView.getCreateMaterializedViewOpts()
                        .getMaterializedViewRefreshOpts().getRefreshInterval())) {
            mView.setRefreshInterval(createMaterializedView.getCreateMaterializedViewOpts()
                    .getMaterializedViewRefreshOpts().getRefreshInterval());
        }
        mView.setSchemaName(schemaName);
        mView.setName(mViewName);
        mView.setColumns(schemaAccessor.listTableColumns(schemaName, mViewName));
        mView.setConstraints(schemaAccessor.listMViewConstraints(schemaName, mViewName));
        mView.setIndexes(schemaAccessor.listTableIndexes(schemaName, mViewName));
        OBMySQLGetDBTableByParser parser = new OBMySQLGetDBTableByParser(ddl);
        if (Objects.nonNull(createMaterializedView.getPartition())) {
            mView.setPartition(parser.getPartition(createMaterializedView.getPartition()));
        }
        mView.setDdl(ddl);
        try {
            mView.setColumnGroups(schemaAccessor.listTableColumnGroups(schemaName, mViewName));
        } catch (Exception e) {
            // eat the exception
        }
        return mView;
    }

    @Override
    public void drop(Connection connection, String schemaName, String mViewName) {
        getOperator(connection).drop(DBObjectType.MATERIALIZED_VIEW, null, mViewName);
    }

    @Override
    public String generateCreateTemplate(DBMaterializedView mView) {
        return getTemplate().generateCreateObjectTemplate(mView);
    }

    @Override
    public Boolean refresh(Connection connection, DBMViewRefreshParameter parameter) {
        return getSchemaAccessor(connection).refreshMVData(parameter);
    }

    private CreateMaterializedView parseTableDDL(String ddl) {
        CreateMaterializedView statement = null;
        try {
            Statement value = SqlParser.parseMysqlStatement(ddl);
            if (value instanceof CreateMaterializedView) {
                statement = (CreateMaterializedView) value;
            }
        } catch (Exception e) {
            log.warn("Failed to parse materialized view ddl, error message={}", e.getMessage());
        }
        return statement;
    }


    protected DBSchemaAccessor getSchemaAccessor(Connection connection) {
        return DBAccessorUtil.getSchemaAccessor(connection);
    }

    protected DBObjectOperator getOperator(Connection connection) {
        return new MySQLObjectOperator(JdbcOperationsUtil.getJdbcOperations(connection));
    }

    protected DBObjectTemplate<DBMaterializedView> getTemplate() {
        return DBBrowser.objectTemplate().mViewTemplate()
                .setType(DialectType.OB_MYSQL.getDBBrowserDialectTypeName()).create();
    }
}
