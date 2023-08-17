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
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.tools.dbbrowser.editor.oracle.OracleConstraintEditor;
import com.oceanbase.tools.dbbrowser.model.DBConstraintType;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-06-21
 * @since 4.2.0
 */
@Slf4j
public class OBOracleForeignKeyHandler extends BaseForeignKeyHandler {

    public OBOracleForeignKeyHandler(ConnectionSession connectionSession) {
        super(connectionSession, new OracleConstraintEditor());
    }

    @Override
    public void disableForeignKeyCheck(String schemaName, String tableName) {
        List<DBTableConstraint> dbTableConstraints = getDbTableConstraints(schemaName, tableName);
        if (CollectionUtils.isEmpty(dbTableConstraints)) {
            return;
        }
        String fullTableName = getFullTableName(schemaName, tableName);
        dbTableConstraints.forEach(c -> getSyncJdbcExecutor()
                .execute("alter table " + fullTableName + " disable constraint " + c.getName()));
    }

    @Override
    public void enableForeignKeyCheck(String schemaName, String tableName) {
        List<DBTableConstraint> dbTableConstraints = getDbTableConstraints(schemaName, tableName);
        if (CollectionUtils.isEmpty(dbTableConstraints)) {
            return;
        }
        String fullTableName = getFullTableName(schemaName, tableName);
        dbTableConstraints.forEach(c -> getSyncJdbcExecutor()
                .execute("alter table " + fullTableName + " enable constraint " + c.getName()));
    }

    private static String getFullTableName(String schemaName, String tableName) {
        return schemaName + "." + tableName;
    }

    private List<DBTableConstraint> getDbTableConstraints(String schemaName, String tableName) {
        return DBSchemaAccessors.create(connectionSession)
                .listTableConstraints(schemaName, tableName)
                .stream().filter(a -> a.getType() == DBConstraintType.FOREIGN_KEY)
                .collect(Collectors.toList());
    }

    @Override
    protected List<DBTableReferencedInfo> getTableConstraintByReferenced(String schemaName, String tableName) {
        String sql = " SELECT a.owner, a.table_name FROM user_constraints a inner join user_constraints b "
                + " on a.r_constraint_name = b.constraint_name "
                + " WHERE a.constraint_type = 'R' "
                + " and a.owner= b.owner "
                + " and b.owner = ? "
                + " and b.table_name = ?";
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
