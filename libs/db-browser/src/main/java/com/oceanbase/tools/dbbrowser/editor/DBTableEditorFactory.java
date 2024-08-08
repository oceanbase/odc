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
package com.oceanbase.tools.dbbrowser.editor;

import org.apache.commons.lang3.Validate;

import com.oceanbase.tools.dbbrowser.AbstractDBBrowserFactory;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLTableEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLLessThan400TableEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLTableEditor;
import com.oceanbase.tools.dbbrowser.editor.oracle.OracleTableEditor;
import com.oceanbase.tools.dbbrowser.util.VersionUtils;

import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Accessors(chain = true)
public class DBTableEditorFactory extends AbstractDBBrowserFactory<DBTableEditor> {

    private String dbVersion;

    @Override
    public DBTableEditor buildForDoris() {
        return buildForMySQL();
    }

    @Override
    public DBTableEditor buildForMySQL() {
        return new MySQLTableEditor(getTableIndexEditor(),
                getTableColumnEditor(),
                getTableConstraintEditor(),
                getTablePartitionEditor());
    }

    @Override
    public DBTableEditor buildForOBMySQL() {
        Validate.notNull(this.dbVersion, "DBVersion can not be null");
        DBTableIndexEditor indexEditor = getTableIndexEditor();
        DBTableColumnEditor columnEditor = getTableColumnEditor();
        DBTableConstraintEditor constraintEditor = getTableConstraintEditor();
        DBTablePartitionEditor partitionEditor = getTablePartitionEditor();
        if (VersionUtils.isLessThan(this.dbVersion, "4.0.0")) {
            return new OBMySQLLessThan400TableEditor(indexEditor,
                    columnEditor, constraintEditor, partitionEditor);
        }
        return new OBMySQLTableEditor(indexEditor, columnEditor, constraintEditor, partitionEditor);
    }

    @Override
    public DBTableEditor buildForOBOracle() {
        return new OracleTableEditor(getTableIndexEditor(),
                getTableColumnEditor(),
                getTableConstraintEditor(),
                getTablePartitionEditor());
    }

    @Override
    public DBTableEditor buildForOracle() {
        return buildForOBOracle();
    }

    @Override
    public DBTableEditor buildForOdpSharding() {
        return buildForOBMySQL();
    }

    @Override
    public DBTableEditor buildForPostgres() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    private DBTableIndexEditor getTableIndexEditor() {
        DBTableIndexEditorFactory indexFactory = new DBTableIndexEditorFactory();
        indexFactory.setType(this.type);
        return indexFactory.create();
    }

    private DBTableColumnEditor getTableColumnEditor() {
        DBTableColumnEditorFactory columnFactory = new DBTableColumnEditorFactory();
        columnFactory.setType(this.type);
        return columnFactory.create();
    }

    private DBTablePartitionEditor getTablePartitionEditor() {
        Validate.notNull(this.dbVersion, "DBVersion can not be null");
        DBTablePartitionEditorFactory partitionFactory = new DBTablePartitionEditorFactory();
        partitionFactory.setType(this.type);
        partitionFactory.setDbVersion(this.dbVersion);
        return partitionFactory.create();
    }

    private DBTableConstraintEditor getTableConstraintEditor() {
        Validate.notNull(this.dbVersion, "DBVersion can not be null");
        DBTableConstraintEditorFactory constraintFactory = new DBTableConstraintEditorFactory();
        constraintFactory.setType(this.type);
        constraintFactory.setDbVersion(this.dbVersion);
        return constraintFactory.create();
    }

}
