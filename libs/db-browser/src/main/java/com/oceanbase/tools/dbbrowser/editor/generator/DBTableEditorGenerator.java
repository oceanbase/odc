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
package com.oceanbase.tools.dbbrowser.editor.generator;

import com.oceanbase.tools.dbbrowser.editor.DBTableColumnEditor;
import com.oceanbase.tools.dbbrowser.editor.DBTableConstraintEditor;
import com.oceanbase.tools.dbbrowser.editor.DBTableEditor;
import com.oceanbase.tools.dbbrowser.editor.DBTableIndexEditor;
import com.oceanbase.tools.dbbrowser.editor.DBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLTableEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLLessThan400TableEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLTableEditor;
import com.oceanbase.tools.dbbrowser.editor.oracle.OracleTableEditor;
import com.oceanbase.tools.dbbrowser.util.VersionUtils;

/**
 * @author jingtian
 * @date 2024/5/22
 */
public class DBTableEditorGenerator {
    public static DBTableEditor createForOBMySQL(String dbVersion) {
        DBTableIndexEditor indexEditor = DBTableIndexEditorGenerator.createForOBMySQL(dbVersion);
        DBTableColumnEditor columnEditor = DBTableColumnEditorGenerator.createForOBMySQL(dbVersion);
        DBTableConstraintEditor constraintEditor = DBTableConstraintEditorGenerator.createForOBMySQL(dbVersion);
        DBTablePartitionEditor partitionEditor = DBTablePartitionEditorGenerator.createForOBMySQL(dbVersion);
        if (VersionUtils.isLessThan(dbVersion, "4.0.0")) {
            return new OBMySQLLessThan400TableEditor(indexEditor, columnEditor, constraintEditor, partitionEditor);
        }
        return new OBMySQLTableEditor(indexEditor, columnEditor, constraintEditor, partitionEditor);
    }

    public static DBTableEditor createForOBOracle(String dbVersion) {
        return createForOracle(dbVersion);
    }

    public static DBTableEditor createForODPOBMySQL(String dbVersion) {
        return createForOBMySQL(dbVersion);
    }

    public static DBTableEditor createForMySQL(String dbVersion) {
        return new MySQLTableEditor(new DBTableIndexEditorGenerator().createForMySQL(dbVersion),
                DBTableColumnEditorGenerator.createForMySQL(dbVersion),
                DBTableConstraintEditorGenerator.createForMySQL(dbVersion),
                DBTablePartitionEditorGenerator.createForMySQL(dbVersion));
    }

    public static DBTableEditor createForOracle(String dbVersion) {
        return new OracleTableEditor(new DBTableIndexEditorGenerator().createForOracle(dbVersion),
                DBTableColumnEditorGenerator.createForOracle(dbVersion),
                DBTableConstraintEditorGenerator.createForOracle(dbVersion),
                DBTablePartitionEditorGenerator.createForOracle(dbVersion));
    }

    public static DBTableEditor createForDoris(String dbVersion) {
        return createForMySQL(dbVersion);
    }
}
