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
import java.util.Map;

import org.pf4j.Extension;

import com.oceanbase.odc.plugin.schema.api.ColumnExtensionPoint;
import com.oceanbase.odc.plugin.schema.obmysql.utils.DBAccessorUtil;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

/**
 * @author gaoda.xy
 * @date 2024/4/21 21:04
 */
@Extension
public class OBMySQLColumnExtension implements ColumnExtensionPoint {

    @Override
    public Map<String, List<DBTableColumn>> listBasicTableColumns(Connection connection, String schemaName) {
        return getSchemaAccessor(connection).listBasicTableColumns(schemaName);
    }

    @Override
    public Map<String, List<DBTableColumn>> listBasicViewColumns(Connection connection, String schemaName) {
        return getSchemaAccessor(connection).listBasicViewColumns(schemaName);
    }

    protected DBSchemaAccessor getSchemaAccessor(Connection connection) {
        return DBAccessorUtil.getSchemaAccessor(connection);
    }

}
