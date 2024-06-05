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
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLDBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLDBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLLessThan2277PartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLLessThan400DBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.oracle.OBOracleLessThan400DBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.oracle.OracleDBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.util.VersionUtils;

import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Accessors(chain = true)
public class DBTablePartitionEditorFactory extends AbstractDBBrowserFactory<DBTablePartitionEditor> {

    private String dbVersion;

    @Override
    public DBTablePartitionEditor buildForDoris() {
        return buildForMySQL();
    }

    @Override
    public DBTablePartitionEditor buildForMySQL() {
        return new MySQLDBTablePartitionEditor();
    }

    @Override
    public DBTablePartitionEditor buildForOBMySQL() {
        Validate.notNull(this.dbVersion, "DBVersion can not be null");
        if (VersionUtils.isLessThan(this.dbVersion, "2.2.77")) {
            return new OBMySQLLessThan2277PartitionEditor();
        } else if (VersionUtils.isLessThan(this.dbVersion, "4.0.0")) {
            return new OBMySQLLessThan400DBTablePartitionEditor();
        } else {
            return new OBMySQLDBTablePartitionEditor();
        }
    }

    @Override
    public DBTablePartitionEditor buildForOBOracle() {
        Validate.notNull(this.dbVersion, "DBVersion can not be null");
        if (VersionUtils.isLessThan(this.dbVersion, "4.0.0")) {
            return new OBOracleLessThan400DBTablePartitionEditor();
        }
        return new OracleDBTablePartitionEditor();
    }

    @Override
    public DBTablePartitionEditor buildForOracle() {
        return new OracleDBTablePartitionEditor();
    }

    @Override
    public DBTablePartitionEditor buildForOdpSharding() {
        return new MySQLDBTablePartitionEditor();
    }

}
