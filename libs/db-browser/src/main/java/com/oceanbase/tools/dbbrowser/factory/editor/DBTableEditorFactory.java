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
package com.oceanbase.tools.dbbrowser.factory.editor;

import com.oceanbase.tools.dbbrowser.editor.DBTableEditor;
import com.oceanbase.tools.dbbrowser.factory.AbstractDBBrowserFactory;

import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Accessors(chain = true)
public class DBTableEditorFactory extends AbstractDBBrowserFactory<DBTableEditor> {

    @Override
    public DBTableEditor buildForMysql() {
        return null;
    }

    @Override
    public DBTableEditor buildForOBMysql() {
        return null;
    }

    @Override
    public DBTableEditor buildForOBOracle() {
        return null;
    }

    @Override
    public DBTableEditor buildForOracle() {
        return null;
    }

    @Override
    public DBTableEditor buildForDoris() {
        return null;
    }

}
