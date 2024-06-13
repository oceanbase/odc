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
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLColumnEditor;
import com.oceanbase.tools.dbbrowser.editor.oracle.OracleColumnEditor;

public class DBTableColumnEditorFactory extends AbstractDBBrowserFactory<DBTableColumnEditor> {

    @Override
    public DBTableColumnEditor buildForDoris() {
        return buildForMySQL();
    }

    @Override
    public DBTableColumnEditor buildForMySQL() {
        return new MySQLColumnEditor();
    }

    @Override
    public DBTableColumnEditor buildForOBMySQL() {
        return buildForMySQL();
    }

    @Override
    public DBTableColumnEditor buildForOBOracle() {
        return buildForOracle();
    }

    @Override
    public DBTableColumnEditor buildForOracle() {
        return new OracleColumnEditor();
    }

    @Override
    public DBTableColumnEditor buildForOdpSharding() {
        return buildForMySQL();
    }

}
