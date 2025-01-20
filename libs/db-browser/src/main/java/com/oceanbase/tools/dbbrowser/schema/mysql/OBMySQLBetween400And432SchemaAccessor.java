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
package com.oceanbase.tools.dbbrowser.schema.mysql;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

/**
 * @description: applicable to OB [4.0.0,4.3.2)
 * @author: zijia.cj
 * @date: 2024/8/27 14:55
 * @since: 4.3.3
 */
public class OBMySQLBetween400And432SchemaAccessor extends OBMySQLSchemaAccessor {


    public OBMySQLBetween400And432SchemaAccessor(JdbcOperations jdbcOperations) {
        super(jdbcOperations);
    }

    @Override
    public List<String> showExternalTables(String schemaName) {
        throw new UnsupportedOperationException(
                "External table is supported by odc after the 432 version of oceanbase");
    }

    @Override
    public List<String> showExternalTablesLike(String schemaName, String tableNameLike) {
        throw new UnsupportedOperationException(
                "External table is supported by odc after the 432 version of oceanbase");
    }

    @Override
    public List<DBObjectIdentity> listExternalTables(String schemaName, String tableNameLike) {
        throw new UnsupportedOperationException(
                "External table is supported by odc after the 432 version of oceanbase");
    }

    @Override
    public boolean isExternalTable(String schemaName, String tableName) {
        return false;
    }

    @Override
    public Map<String, List<DBTableColumn>> listBasicExternalTableColumns(String schemaName) {
        throw new UnsupportedOperationException("not support yet");
    }

    @Override
    public List<DBTableColumn> listBasicExternalTableColumns(String schemaName, String externalTableName) {
        throw new UnsupportedOperationException("not support yet");
    }

}
