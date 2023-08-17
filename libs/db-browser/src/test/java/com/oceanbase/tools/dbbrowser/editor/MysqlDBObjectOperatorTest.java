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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLObjectOperator;
import com.oceanbase.tools.dbbrowser.env.BaseTestEnv;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

/**
 * {@link MysqlDBObjectOperatorTest}
 *
 * @author yh263208
 * @date 2023-01-11 16:09
 * @since ODC_release_4.1.0
 */
public class MysqlDBObjectOperatorTest extends BaseTestEnv {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(getOBMySQLDataSource());
        String createTable = "CREATE TABLE ABCD(id varchar(64))";
        jdbcTemplate.execute(createTable);
    }

    @Test
    public void drop_dropTableInOracleMode_dropSucceed() {
        DBObjectOperator operator = new MySQLObjectOperator(new JdbcTemplate(getOBMySQLDataSource()));
        operator.drop(DBObjectType.TABLE, getOBMySQLDataBaseName(), "ABCD");

        JdbcTemplate executor = new JdbcTemplate(getOBMySQLDataSource());
        thrown.expect(BadSqlGrammarException.class);
        thrown.expectMessage("doesn't exist");
        executor.execute("select count(1) from ABCD");
    }

}
