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
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLConstraintEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLLessThan400ConstraintEditor;
import com.oceanbase.tools.dbbrowser.editor.oracle.OBOracleLessThan400ConstraintEditor;
import com.oceanbase.tools.dbbrowser.editor.oracle.OracleConstraintEditor;
import com.oceanbase.tools.dbbrowser.util.VersionUtils;

import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Accessors(chain = true)
public class DBTableConstraintEditorFactory extends AbstractDBBrowserFactory<DBTableConstraintEditor> {

    private String dbVersion;

    @Override
    public DBTableConstraintEditor buildForDoris() {
        return buildForMySQL();
    }

    @Override
    public DBTableConstraintEditor buildForMySQL() {
        return new MySQLConstraintEditor();
    }

    @Override
    public DBTableConstraintEditor buildForOBMySQL() {
        Validate.notNull(this.dbVersion, "DBVersion can not be null");
        if (VersionUtils.isLessThan(this.dbVersion, "4.0.0")) {
            return new OBMySQLLessThan400ConstraintEditor();
        }
        return new MySQLConstraintEditor();
    }

    @Override
    public DBTableConstraintEditor buildForOBOracle() {
        Validate.notNull(this.dbVersion, "DBVersion can not be null");
        if (VersionUtils.isLessThan(this.dbVersion, "4.0.0")) {
            return new OBOracleLessThan400ConstraintEditor();
        }
        return new OracleConstraintEditor();
    }

    @Override
    public DBTableConstraintEditor buildForOracle() {
        return new OracleConstraintEditor();
    }

    @Override
    public DBTableConstraintEditor buildForOdpSharding() {
        return buildForOBMySQL();
    }

    @Override
    public DBTableConstraintEditor buildForPostgres() {
        throw new UnsupportedOperationException("Not supported yet");
    }

}
