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
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLMViewEditor;
import com.oceanbase.tools.dbbrowser.editor.oracle.OBOracleMViewEditor;

import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/4/2 14:06
 * @since: 4.3.4
 */

@Setter
@Accessors(chain = true)
public class DBMViewEditorFactory extends AbstractDBBrowserFactory<DBMViewEditor> {

    private String dbVersion;

    @Override
    public DBMViewEditor buildForDoris() {
        throw new UnsupportedOperationException("not support yet");
    }

    @Override
    public DBMViewEditor buildForMySQL() {
        throw new UnsupportedOperationException("not support yet");
    }

    @Override
    public DBMViewEditor buildForOBMySQL() {
        return new OBMySQLMViewEditor(getMViewIndexEditor());
    }

    @Override
    public DBMViewEditor buildForOBOracle() {
        return new OBOracleMViewEditor(getMViewIndexEditor());
    }

    @Override
    public DBMViewEditor buildForOracle() {
        return null;
    }

    @Override
    public DBMViewEditor buildForOdpSharding() {
        return null;
    }

    @Override
    public DBMViewEditor buildForPostgres() {
        return null;
    }

    private DBTableIndexEditor getMViewIndexEditor() {
        DBMViewIndexEditorFactory indexFactory = new DBMViewIndexEditorFactory();
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
