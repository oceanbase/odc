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
import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.tools.dbbrowser.AbstractDBBrowserFactory;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLObjectOperator;
import com.oceanbase.tools.dbbrowser.editor.oracle.OracleObjectOperator;

import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Accessors(chain = true)
public class DBObjectOperatorFactory extends AbstractDBBrowserFactory<DBObjectOperator> {

    private JdbcOperations jdbcOperations;

    @Override
    public DBObjectOperator buildForDoris() {
        return buildForOBMySQL();
    }

    @Override
    public DBObjectOperator buildForMySQL() {
        return buildForOBMySQL();
    }

    @Override
    public DBObjectOperator buildForOBMySQL() {
        Validate.notNull(this.jdbcOperations, "Datasource can not be null");
        return new MySQLObjectOperator(this.jdbcOperations);
    }

    @Override
    public DBObjectOperator buildForOBOracle() {
        Validate.notNull(this.jdbcOperations, "Datasource can not be null");
        return new OracleObjectOperator(this.jdbcOperations);
    }

    @Override
    public DBObjectOperator buildForOracle() {
        return buildForOBOracle();
    }

    @Override
    public DBObjectOperator buildForOdpSharding() {
        return buildForOBMySQL();
    }

}
