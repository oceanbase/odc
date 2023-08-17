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
package com.oceanbase.odc.service.onlineschemachange.foreignkey;

import java.util.List;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLConstraintEditor;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-06-21
 * @since 4.2.0
 */
@Slf4j
public class OBMySQLForeignKeyHandler extends BaseForeignKeyHandler {

    public OBMySQLForeignKeyHandler(ConnectionSession connectionSession) {
        super(connectionSession, new MySQLConstraintEditor());
    }

    @Override
    public void disableForeignKeyCheck(String schemaName, String tableName) {
        getSyncJdbcExecutor().execute("set session foreign_key_checks = 0");
    }

    @Override
    public void enableForeignKeyCheck(String schemaName, String tableName) {
        getSyncJdbcExecutor().execute("set session foreign_key_checks = 1");
    }

    @Override
    public List<DBTableReferencedInfo> getTableConstraintByReferenced(String schemaName, String tableName) {
        // Get foreign key reference to old table name
        String sql = "SELECT TABLE_SCHEMA, TABLE_NAME from information_schema.KEY_COLUMN_USAGE "
                + "where referenced_table_schema = ? and referenced_table_name = ?";

        return getSyncJdbcExecutor().query(sql,
                (rs, rowNum) -> {
                    DBTableReferencedInfo dbTableConstraint = new DBTableReferencedInfo();
                    dbTableConstraint.setReferenceFromSchemaName(rs.getString(1));
                    dbTableConstraint.setReferenceFromTableName(rs.getString(2));
                    return dbTableConstraint;
                },
                schemaName,
                tableName);
    }

}
