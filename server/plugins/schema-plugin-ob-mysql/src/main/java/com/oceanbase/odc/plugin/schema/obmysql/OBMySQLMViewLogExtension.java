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
import com.oceanbase.odc.plugin.schema.api.MViewLogExtensionPoint;
import com.oceanbase.odc.plugin.schema.obmysql.utils.DBAccessorUtil;
import com.oceanbase.tools.dbbrowser.editor.DBObjectOperator;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLObjectOperator;
import com.oceanbase.tools.dbbrowser.model.DBMViewLogPurgeParameter;
import com.oceanbase.tools.dbbrowser.model.DBMaterializedViewLog;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.extern.slf4j.Slf4j;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/7/15 08:29
 * @since: 4.4.0
 */
@Extension
@Slf4j
public class OBMySQLMViewLogExtension implements MViewLogExtensionPoint {
    @Override
    public List<DBObjectIdentity> list(Connection connection, String schemaName) {
        return getSchemaAccessor(connection).listMViewLogs(schemaName);
    }

    @Override
    public DBMaterializedViewLog getDetail(Connection connection, String schemaName, String mViewName) {
        DBSchemaAccessor schemaAccessor = getSchemaAccessor(connection);
        DBMaterializedViewLog mViewLog = schemaAccessor.getMViewLog(schemaName, mViewName);
        mViewLog.setColumns(schemaAccessor.listTableColumns(schemaName, mViewName));
        return mViewLog;
    }

    @Override
    public void drop(Connection connection, String schemaName, String mViewName) {
        getOperator(connection).drop(DBObjectType.MATERIALIZED_VIEW_LOG, null, mViewName);
    }

    @Override
    public Boolean purge(Connection connection, DBMViewLogPurgeParameter parameter) {
        return getSchemaAccessor(connection).purgeMViewLog(parameter);
    }

    protected DBSchemaAccessor getSchemaAccessor(Connection connection) {
        return DBAccessorUtil.getSchemaAccessor(connection);
    }

    protected DBObjectOperator getOperator(Connection connection) {
        return new MySQLObjectOperator(JdbcOperationsUtil.getJdbcOperations(connection));
    }

}
