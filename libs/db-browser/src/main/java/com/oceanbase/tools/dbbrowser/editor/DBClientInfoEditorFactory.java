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

import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.tools.dbbrowser.AbstractDBBrowserFactory;
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMysqlNoLessThan400ClientInfoEditor;
import com.oceanbase.tools.dbbrowser.editor.oracle.OBOracleNoLess400ClientInfoEditor;
import com.oceanbase.tools.dbbrowser.editor.oracle.OracleClientInfoEditor;
import com.oceanbase.tools.dbbrowser.model.DbClientInfo;
import com.oceanbase.tools.dbbrowser.util.VersionUtils;

import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Accessors(chain = true)
public class DBClientInfoEditorFactory extends AbstractDBBrowserFactory<DBClientInfoEditor> {

    private String dbVersion;
    private JdbcOperations jdbcOperations;

    @Override
    public DBClientInfoEditor buildForDoris() {
        return new NotSupportDBClientInfoEditor();
    }

    @Override
    public DBClientInfoEditor buildForMySQL() {
        return new NotSupportDBClientInfoEditor();
    }

    @Override
    public DBClientInfoEditor buildForOBMySQL() {
        if (VersionUtils.isGreaterThanOrEqualsTo(this.dbVersion, "4.0.0")) {
            return new OBMysqlNoLessThan400ClientInfoEditor(jdbcOperations);
        }
        return new NotSupportDBClientInfoEditor();
    }

    @Override
    public DBClientInfoEditor buildForOBOracle() {
        if (VersionUtils.isGreaterThanOrEqualsTo(this.dbVersion, "4.0.0")) {
            return new OBOracleNoLess400ClientInfoEditor(jdbcOperations);
        }
        return new NotSupportDBClientInfoEditor();
    }

    @Override
    public DBClientInfoEditor buildForOracle() {
        return new OracleClientInfoEditor(jdbcOperations);
    }

    @Override
    public DBClientInfoEditor buildForOdpSharding() {
        return new NotSupportDBClientInfoEditor();
    }

    @Override
    public DBClientInfoEditor buildForPostgres() {
        return new NotSupportDBClientInfoEditor();
    }

    static class NotSupportDBClientInfoEditor implements DBClientInfoEditor {

        @Override
        public boolean setClientInfo(DbClientInfo clientInfo) {
            return false;
        }
    }
}
