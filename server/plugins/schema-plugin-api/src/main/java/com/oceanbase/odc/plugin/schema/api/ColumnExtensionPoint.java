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
package com.oceanbase.odc.plugin.schema.api;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import org.pf4j.ExtensionPoint;

import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

/**
 * @author gaoda.xy
 * @date 2024/4/19 17:25
 */
public interface ColumnExtensionPoint extends ExtensionPoint {

    /**
     * List all table columns of the specified schema
     * 
     * @param schemaName schema name
     * @return table columns, only basic information (schemaName, tableName, name, comment) is included
     */
    Map<String, List<DBTableColumn>> listBasicTableColumns(Connection connection, String schemaName);

    /**
     * List all view columns of the specified schema
     * 
     * @param schemaName schema name
     * @return view columns, only basic information (schemaName, viewName, name, comment) is included
     */
    Map<String, List<DBTableColumn>> listBasicViewColumns(Connection connection, String schemaName);

}
