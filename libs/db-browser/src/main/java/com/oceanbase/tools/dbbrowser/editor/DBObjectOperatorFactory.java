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

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.tools.dbbrowser.AbstractDBBrowserFactory;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLObjectOperator;
import com.oceanbase.tools.dbbrowser.editor.oracle.OracleObjectOperator;

import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Accessors(chain = true)
public class DBObjectOperatorFactory extends AbstractDBBrowserFactory<DBObjectOperator> {

    private DataSource dataSource;
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
        return new MySQLObjectOperator(getJdbcOperations());
    }

    @Override
    public DBObjectOperator buildForOBOracle() {
        return new OracleObjectOperator(getJdbcOperations());
    }

    @Override
    public DBObjectOperator buildForOracle() {
        return buildForOBOracle();
    }

    @Override
    public DBObjectOperator buildForOdpSharding() {
        return buildForOBMySQL();
    }

    private JdbcOperations getJdbcOperations() {
        if (this.jdbcOperations != null) {
            return this.jdbcOperations;
        } else if (this.dataSource != null) {
            return new JdbcTemplate(this.dataSource);
        }
        throw new IllegalArgumentException("Datasource can not be null");
    }

}
