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

import com.oceanbase.tools.dbbrowser.editor.DBTableConstraintEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLConstraintEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLLessThan400ConstraintEditor;
import com.oceanbase.tools.dbbrowser.editor.oracle.OBOracleLessThan400ConstraintEditor;
import com.oceanbase.tools.dbbrowser.editor.oracle.OracleConstraintEditor;
import com.oceanbase.tools.dbbrowser.util.VersionUtils;

/**
 * @author jingtian
 * @date 2024/5/22
 */
public class DBTableConstraintEditorGenerator extends DBObjectEditorGenerator<DBTableConstraintEditor> {
    @Override
    public DBTableConstraintEditor createForOBMySQL(String dbVersion) {
        if (VersionUtils.isLessThan(dbVersion, "4.0.0")) {
            return new OBMySQLLessThan400ConstraintEditor();
        }
        return new MySQLConstraintEditor();
    }

    @Override
    public DBTableConstraintEditor createForOBOracle(String dbVersion) {
        if (VersionUtils.isLessThan(dbVersion, "4.0.0")) {
            return new OBOracleLessThan400ConstraintEditor();
        }
        return new OracleConstraintEditor();
    }

    @Override
    public DBTableConstraintEditor createForODPOBMySQL(String dbVersion) {
        return createForOBMySQL(dbVersion);
    }

    @Override
    public DBTableConstraintEditor createForMySQL(String dbVersion) {
        return new MySQLConstraintEditor();
    }

    @Override
    public DBTableConstraintEditor createForOracle(String dbVersion) {
        return new OracleConstraintEditor();
    }

    @Override
    public DBTableConstraintEditor createForDoris(String dbVersion) {
        return createForMySQL(dbVersion);
    }
}
