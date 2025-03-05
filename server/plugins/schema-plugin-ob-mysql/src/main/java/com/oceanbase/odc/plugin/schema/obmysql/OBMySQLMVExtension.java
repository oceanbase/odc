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
import com.oceanbase.odc.plugin.schema.api.MVExtensionPoint;
import com.oceanbase.odc.plugin.schema.obmysql.utils.DBAccessorUtil;
import com.oceanbase.tools.dbbrowser.DBBrowser;
import com.oceanbase.tools.dbbrowser.editor.DBObjectOperator;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLObjectOperator;
import com.oceanbase.tools.dbbrowser.model.DBMVSyncDataParameter;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBView;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.dbbrowser.template.DBObjectTemplate;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/4 16:15
 * @since: 4.3.4
 */
@Extension
public class OBMySQLMVExtension implements MVExtensionPoint {
    @Override
    public List<DBObjectIdentity> list(Connection connection, String schemaName) {
        return getSchemaAccessor(connection).listMVs(schemaName);
    }

    @Override
    public DBView getDetail(Connection connection, String schemaName, String viewName) {
        return getSchemaAccessor(connection).getMV(schemaName, viewName);
    }

    @Override
    public void drop(Connection connection, String schemaName, String viewName) {
        getOperator(connection).drop(DBObjectType.MATERIALIZED_VIEW, null, viewName);
    }

    @Override
    public String generateCreateTemplate(DBView view) {
        return getTemplate().generateCreateObjectTemplate(view);
    }

    @Override
    public Boolean syncMVData(Connection connection, DBMVSyncDataParameter parameter) {
        return getSchemaAccessor(connection).syncMVData(parameter);
    }

    protected DBSchemaAccessor getSchemaAccessor(Connection connection) {
        return DBAccessorUtil.getSchemaAccessor(connection);
    }

    protected DBObjectOperator getOperator(Connection connection) {
        return new MySQLObjectOperator(JdbcOperationsUtil.getJdbcOperations(connection));
    }

    protected DBObjectTemplate<DBView> getTemplate() {
        return DBBrowser.objectTemplate().viewTemplate()
                .setType(DialectType.OB_MYSQL.getDBBrowserDialectTypeName()).create();
    }
}
