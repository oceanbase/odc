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
package com.oceanbase.tools.dbbrowser.factory.editor.table;

import com.oceanbase.tools.dbbrowser.editor.DBTableEditor;
import com.oceanbase.tools.dbbrowser.factory.DBBrowserFactories;
import com.oceanbase.tools.dbbrowser.factory.DBBrowserFactory;

public class DBTableEditorFactories implements DBBrowserFactories<DBTableEditor> {
    @Override
    public <Factory extends DBBrowserFactory<DBTableEditor>> Factory forMySQL() {
        return null;
    }

    @Override
    public <Factory extends DBBrowserFactory<DBTableEditor>> Factory forOracle() {
        return null;
    }

    @Override
    public OBMySQLDBTableEditorFactory forOBMySQL() {
        return new OBMySQLDBTableEditorFactory();
    }

    @Override
    public OBOracleDBTableEditorFactory forOBOracle() {
        return new OBOracleDBTableEditorFactory();
    }

    @Override
    public <Factory extends DBBrowserFactory<DBTableEditor>> Factory forDoris() {
        return null;
    }
}
