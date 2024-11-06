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
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.tools.dbbrowser.editor.DBTableConstraintEditor;
import com.oceanbase.tools.dbbrowser.model.DBConstraintType;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-06-21
 * @since 4.2.0
 */
@Slf4j
public abstract class BaseForeignKeyHandler implements ForeignKeyHandler {

    protected final ConnectionSession connectionSession;
    protected final DBTableConstraintEditor dbTableConstraintEditor;

    public BaseForeignKeyHandler(ConnectionSession connectionSession, DBTableConstraintEditor dbObjectEditor) {
        this.connectionSession = connectionSession;
        this.dbTableConstraintEditor = dbObjectEditor;
    }

    @Override
    public void dropAllForeignKeysOnTable(String schemaName, String tableName) {

        /**
         * Old child table reference to a parent a table, will cause a new parent table can't execute dml.
         * So we remove foreign key from old child table
         */
        List<DBTableConstraint> dbTableConstraints =
                DBSchemaAccessors.create(connectionSession, ConnectionSessionConstants.CONSOLE_DS_KEY)
                        .listTableConstraints(schemaName, tableName)
                        .stream().filter(a -> a.getType() == DBConstraintType.FOREIGN_KEY)
                        .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(dbTableConstraints)) {
            return;
        }

        dbTableConstraints.forEach(constraint -> {
            String dropOldConstraint = dbTableConstraintEditor.generateDropObjectDDL(constraint);
            getSyncJdbcExecutor().execute(dropOldConstraint);
        });

    }

    @Override
    public void alterTableForeignKeyReference(String schemaName, String oldTableName, String newTableName) {
        /**
         * When rename a parent table, eg: rename to a to a_gho, the child table's foreign key reference to
         * parent table will rename, so we should alter it reference to origin table
         */
        List<DBTableReferencedInfo> referencedConstraint = getTableConstraintByReferenced(schemaName, oldTableName);

        if (CollectionUtils.isEmpty(referencedConstraint)) {
            return;
        }
        DBSchemaAccessor accessor =
                DBSchemaAccessors.create(connectionSession, ConnectionSessionConstants.CONSOLE_DS_KEY);
        referencedConstraint.forEach(constraint -> {
            doAlterTableForeignKeyReference(schemaName, oldTableName, newTableName, accessor, constraint);
        });

        log.info("Alter table {} foreign key reference to {} ", oldTableName, newTableName);
    }

    private void doAlterTableForeignKeyReference(String schemaName, String oldTableName,
            String newTableName, DBSchemaAccessor accessor, DBTableReferencedInfo constraint) {

        // Get constraint info from constraint table reference to old table
        List<DBTableConstraint> dbTableConstraints = accessor
                .listTableConstraints(constraint.getReferenceFromSchemaName(), constraint.getReferenceFromTableName())
                .stream().filter(a -> a.getType() == DBConstraintType.FOREIGN_KEY)
                .filter(a -> Objects.equals(a.getReferenceTableName(), oldTableName))
                .filter(a -> Objects.equals(a.getReferenceSchemaName(), schemaName))
                .collect(Collectors.toList());

        // Drop old constraint on old table and create new constraint on new table
        dbTableConstraints.forEach(c -> {
            String dropOldConstraint = dbTableConstraintEditor.generateDropObjectDDL(c);
            DBTableConstraint newConstraint = new DBTableConstraint();
            BeanUtils.copyProperties(c, newConstraint);
            newConstraint.setReferenceTableName(newTableName);

            String createNewConstraint = dbTableConstraintEditor.generateCreateObjectDDL(newConstraint);
            getSyncJdbcExecutor().execute(dropOldConstraint);
            getSyncJdbcExecutor().execute(createNewConstraint);

        });
    }

    protected abstract List<DBTableReferencedInfo> getTableConstraintByReferenced(String schemaName, String tableName);

    protected SyncJdbcExecutor getSyncJdbcExecutor() {
        return connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
    }

    @Setter
    @Getter
    protected static class DBTableReferencedInfo {
        private String referenceFromSchemaName;
        private String referenceFromTableName;
    }

}
