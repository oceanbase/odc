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

import org.pf4j.Extension;

import com.oceanbase.odc.common.util.JdbcOperationsUtil;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.plugin.schema.api.MViewExtensionPoint;
import com.oceanbase.odc.plugin.schema.obmysql.parser.OBMySQLGetDBTableByParser;
import com.oceanbase.odc.plugin.schema.obmysql.utils.DBAccessorUtil;
import com.oceanbase.tools.dbbrowser.DBBrowser;
import com.oceanbase.tools.dbbrowser.editor.DBObjectOperator;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLObjectOperator;
import com.oceanbase.tools.dbbrowser.model.DBMView;
import com.oceanbase.tools.dbbrowser.model.DBMViewSyncDataParameter;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.dbbrowser.template.DBObjectTemplate;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/4 16:15
 * @since: 4.3.4
 */
@Extension
public class OBMySQLMVExtension implements MViewExtensionPoint {
    @Override
    public List<DBObjectIdentity> list(Connection connection, String schemaName) {
        return getSchemaAccessor(connection).listMVs(schemaName);
    }

    @Override
    public DBMView getDetail(Connection connection, String schemaName, String mViewName) {
        DBSchemaAccessor schemaAccessor = getSchemaAccessor(connection);
        DBMView mView = schemaAccessor.getMView(schemaName, mViewName);
        String ddl = schemaAccessor.getTableDDL(schemaName, mViewName);
        OBMySQLGetDBTableByParser parser = new OBMySQLGetDBTableByParser(ddl);
        mView.setSchemaName(schemaName);
        mView.setName(mViewName);
        mView.setColumns(schemaAccessor.listTableColumns(schemaName, mViewName));
        // TODO: syntax does not match
        mView.setConstraints(schemaAccessor.listTableConstraints(schemaName, mViewName));
        mView.setIndexes(schemaAccessor.listTableIndexes(schemaName, mViewName));
        mView.setType(DBObjectType.MATERIALIZED_VIEW);
        // TODO: parser failed to parse
        mView.setPartition(parser.getPartition());
        mView.setDdl(ddl);
        try {
            // TODO: parser failed to parse
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
    public String generateCreateTemplate(DBMView mView) {
        return getTemplate().generateCreateObjectTemplate(mView);
    }

    @Override
    public Boolean syncMVData(Connection connection, DBMViewSyncDataParameter parameter) {
        return getSchemaAccessor(connection).syncMVData(parameter);
    }

    protected DBSchemaAccessor getSchemaAccessor(Connection connection) {
        return DBAccessorUtil.getSchemaAccessor(connection);
    }

    protected DBObjectOperator getOperator(Connection connection) {
        return new MySQLObjectOperator(JdbcOperationsUtil.getJdbcOperations(connection));
    }

    protected DBObjectTemplate<DBMView> getTemplate() {
        return DBBrowser.objectTemplate().mViewTemplate()
                .setType(DialectType.OB_MYSQL.getDBBrowserDialectTypeName()).create();
    }
}
