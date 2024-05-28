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

import com.oceanbase.tools.dbbrowser.editor.DBTableIndexEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLNoLessThan5700IndexEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLIndexEditor;
import com.oceanbase.tools.dbbrowser.editor.oracle.OBOracleIndexEditor;
import com.oceanbase.tools.dbbrowser.editor.oracle.OracleIndexEditor;

/**
 * @author jingtian
 * @date 2024/5/22
 * @since
 */
public class DBTableIndexEditorGenerator {
    public static DBTableIndexEditor createForOBMySQL(String dbVersion) {
        return new OBMySQLIndexEditor();
    }

    public static DBTableIndexEditor createForOBOracle(String dbVersion) {
        return new OBOracleIndexEditor();
    }

    public static DBTableIndexEditor createForODPOBMySQL(String dbVersion) {
        return createForOBMySQL(dbVersion);
    }

    public static DBTableIndexEditor createForMySQL(String dbVersion) {
        return new MySQLNoLessThan5700IndexEditor();
    }

    public static DBTableIndexEditor createForOracle(String dbVersion) {
        return new OracleIndexEditor();
    }

    public static DBTableIndexEditor createForDoris(String dbVersion) {
        return createForMySQL(dbVersion);
    }
}
