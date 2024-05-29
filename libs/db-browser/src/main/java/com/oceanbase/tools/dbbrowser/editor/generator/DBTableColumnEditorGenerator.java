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
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLColumnEditor;
import com.oceanbase.tools.dbbrowser.editor.oracle.OracleColumnEditor;

/**
 * @author jingtian
 * @date 2024/5/22
 */
public class DBTableColumnEditorGenerator {
    public static DBTableColumnEditor createForOBMySQL(String dbVersion) {
        return createForMySQL(dbVersion);
    }

    public static DBTableColumnEditor createForOBOracle(String dbVersion) {
        return createForOracle(dbVersion);
    }

    public static DBTableColumnEditor createForODPOBMySQL(String dbVersion) {
        return createForMySQL(dbVersion);
    }

    public static DBTableColumnEditor createForMySQL(String dbVersion) {
        return new MySQLColumnEditor();
    }

    public static DBTableColumnEditor createForOracle(String dbVersion) {
        return new OracleColumnEditor();
    }

    public static DBTableColumnEditor createForDoris(String dbVersion) {
        return createForMySQL(dbVersion);
    }
}
