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
package com.oceanbase.odc.plugin.schema.odpsharding.obmysql;

import java.sql.Connection;

import org.pf4j.Extension;

import com.oceanbase.odc.common.util.JdbcOperationsUtil;
import com.oceanbase.odc.plugin.schema.obmysql.OBMySQLTableExtension;
import com.oceanbase.tools.dbbrowser.editor.DBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLDBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.mysql.ODPOBMySQLSchemaAccessor;
import com.oceanbase.tools.dbbrowser.stats.DBStatsAccessor;
import com.oceanbase.tools.dbbrowser.stats.mysql.ODPOBMySQLStatsAccessor;

/**
 * @author jingtian
 * @date 2023/6/30
 * @since 4.2.0
 */
@Extension
public class ODPShardingOBMySQLTableExtension extends OBMySQLTableExtension {
    @Override
    protected DBSchemaAccessor getSchemaAccessor(Connection connection) {
        return new ODPOBMySQLSchemaAccessor(JdbcOperationsUtil.getJdbcOperations(connection));
    }

    @Override
    protected DBStatsAccessor getStatsAccessor(Connection consoleConnection) {
        // only use DBStatsAccessor.getTableStats() method in plugin, so we do not use connectionId
        return new ODPOBMySQLStatsAccessor("");
    }

    @Override
    protected DBTablePartitionEditor getDBTablePartitionEditor(Connection connection) {
        return new MySQLDBTablePartitionEditor();
    }
}
