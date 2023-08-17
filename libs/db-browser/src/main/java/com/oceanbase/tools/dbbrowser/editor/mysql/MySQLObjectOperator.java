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
package com.oceanbase.tools.dbbrowser.editor.mysql;

import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.tools.dbbrowser.editor.DBObjectOperator;
import com.oceanbase.tools.dbbrowser.editor.GeneralSqlStatementBuilder;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;

public class MySQLObjectOperator implements DBObjectOperator {

    protected final JdbcOperations syncJdbcExecutor;

    public MySQLObjectOperator(JdbcOperations syncJdbcExecutor) {
        this.syncJdbcExecutor = syncJdbcExecutor;
    }

    @Override
    public void drop(DBObjectType objectType, String schemaName, String objectName) {
        String sql = GeneralSqlStatementBuilder.drop(new MySQLSqlBuilder(), objectType, schemaName, objectName);
        syncJdbcExecutor.execute(sql);
    }

}
