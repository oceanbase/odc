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

import org.pf4j.ExtensionPoint;

import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBTable;

/**
 * {@link TableExtensionPoint}
 *
 * @author jingtian
 * @date 2023/6/14
 * @since 4.2.0
 */
public interface TableExtensionPoint extends ExtensionPoint {

    List<DBObjectIdentity> list(Connection connection, String schemaName, DBObjectType tableType);

    List<String> showNamesLike(Connection connection, String schemaName, String tableNameLike);

    DBTable getDetail(Connection connection, String schemaName, String tableName);

    void drop(Connection connection, String schemaName, String tableName);

    String generateCreateDDL(Connection connection, DBTable table);

    String generateUpdateDDL(Connection connection, DBTable oldTable, DBTable newTable);

    boolean syncExternalTableFiles(Connection connection, String schemaName, String tableName);
}
