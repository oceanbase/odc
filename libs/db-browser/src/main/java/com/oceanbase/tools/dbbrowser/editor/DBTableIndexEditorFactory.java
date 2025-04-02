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

import com.oceanbase.tools.dbbrowser.AbstractDBBrowserFactory;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLNoLessThan5700IndexEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLDBMViewIndexEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLIndexEditor;
import com.oceanbase.tools.dbbrowser.editor.oracle.OBOracleIndexEditor;
import com.oceanbase.tools.dbbrowser.editor.oracle.OracleIndexEditor;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Accessors(chain = true)
public class DBTableIndexEditorFactory extends AbstractDBBrowserFactory<DBTableIndexEditor> {

    private DBObjectType dbObjectType;

    @Override
    public DBTableIndexEditor buildForDoris() {
        return buildForMySQL();
    }

    @Override
    public DBTableIndexEditor buildForMySQL() {
        return new MySQLNoLessThan5700IndexEditor();
    }

    @Override
    public DBTableIndexEditor buildForOBMySQL() {
        if (dbObjectType == DBObjectType.MATERIALIZED_VIEW) {
            return new OBMySQLDBMViewIndexEditor();
        }
        return new OBMySQLIndexEditor();
    }

    @Override
    public DBTableIndexEditor buildForOBOracle() {
        return new OBOracleIndexEditor();
    }

    @Override
    public DBTableIndexEditor buildForOracle() {
        return new OracleIndexEditor();
    }

    @Override
    public DBTableIndexEditor buildForOdpSharding() {
        return buildForOBMySQL();
    }

    @Override
    public DBTableIndexEditor buildForPostgres() {
        throw new UnsupportedOperationException("Not supported yet");
    }

}
