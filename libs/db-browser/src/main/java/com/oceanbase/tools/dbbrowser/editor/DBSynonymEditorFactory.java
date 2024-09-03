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
import com.oceanbase.tools.dbbrowser.editor.oracle.OracleSynonymEditor;
import com.oceanbase.tools.dbbrowser.model.DBSynonym;

public class DBSynonymEditorFactory extends AbstractDBBrowserFactory<DBObjectEditor<DBSynonym>> {

    @Override
    public DBObjectEditor<DBSynonym> buildForDoris() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public DBObjectEditor<DBSynonym> buildForMySQL() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public DBObjectEditor<DBSynonym> buildForOBMySQL() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public DBObjectEditor<DBSynonym> buildForOBOracle() {
        return new OracleSynonymEditor();
    }

    @Override
    public DBObjectEditor<DBSynonym> buildForOracle() {
        return new OracleSynonymEditor();
    }

    @Override
    public DBObjectEditor<DBSynonym> buildForOdpSharding() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public DBObjectEditor<DBSynonym> buildForPostgres() {
        throw new UnsupportedOperationException("Not supported yet");
    }

}
