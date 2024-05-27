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
package com.oceanbase.tools.dbbrowser.factory.editor.index;

import com.oceanbase.tools.dbbrowser.editor.DBTableIndexEditor;
import com.oceanbase.tools.dbbrowser.factory.DBBrowserFactories;
import com.oceanbase.tools.dbbrowser.factory.DBBrowserFactory;

import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Accessors(chain = true)
public class DBTableIndexEditorFactories implements DBBrowserFactories<DBTableIndexEditor> {

    @Override
    public DBBrowserFactory<DBTableIndexEditor> mysql() {
        return null;
    }

    @Override
    public DBBrowserFactory<DBTableIndexEditor> obmysql() {
        return new OBMySQLDBTableIndexEditorFactory();
    }

    @Override
    public DBBrowserFactory<DBTableIndexEditor> oboracle() {
        return new OBOracleDBTableIndexEditorFactory();
    }

    @Override
    public DBBrowserFactory<DBTableIndexEditor> oracle() {
        return null;
    }

    @Override
    public DBBrowserFactory<DBTableIndexEditor> doris() {
        return null;
    }

}
