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

import com.oceanbase.tools.dbbrowser.editor.DBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLDBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLDBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLLessThan2277PartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLLessThan400DBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.oracle.OBOracleLessThan400DBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.oracle.OracleDBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.util.VersionUtils;

/**
 * @author jingtian
 * @date 2024/5/22
 * @since
 */
public class DBTablePartitionEditorGenerator {
    public static DBTablePartitionEditor createForOBMySQL(String dbVersion) {
        if (VersionUtils.isLessThan(dbVersion, "2.2.77")) {
            return new OBMySQLLessThan2277PartitionEditor();
        } else if (VersionUtils.isLessThan(dbVersion, "4.0.0")) {
            return new OBMySQLLessThan400DBTablePartitionEditor();
        } else {
            return new OBMySQLDBTablePartitionEditor();
        }
    }

    public static DBTablePartitionEditor createForOBOracle(String dbVersion) {
        if (VersionUtils.isLessThan(dbVersion, "4.0.0")) {
            return new OBOracleLessThan400DBTablePartitionEditor();
        }
        return new OracleDBTablePartitionEditor();
    }

    public static DBTablePartitionEditor createForODPOBMySQL(String dbVersion) {
        return new MySQLDBTablePartitionEditor();
    }

    public static DBTablePartitionEditor createForMySQL(String dbVersion) {
        return new MySQLDBTablePartitionEditor();
    }

    public static DBTablePartitionEditor createForOracle(String dbVersion) {
        return new OracleDBTablePartitionEditor();
    }

    public static DBTablePartitionEditor createForDoris(String dbVersion) {
        return createForMySQL(dbVersion);
    }
}
