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
package com.oceanbase.tools.dbbrowser.parser;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.parser.constant.GeneralSqlType;
import com.oceanbase.tools.dbbrowser.parser.constant.SqlType;
import com.oceanbase.tools.dbbrowser.parser.result.BasicResult;

public class ParserUtilTest {

    @Test
    public void test_sql_visit_select() {
        String sql = "select * from db.t_test";
        BasicResult result = ParserUtil.parseMysqlType(sql);
        Assert.assertEquals(SqlType.SELECT, result.getSqlType());
    }

    @Test
    public void test_sql_visit_drop_database() {
        String sql = "DROP database db1";
        BasicResult result = ParserUtil.parseMysqlType(sql);
        Assert.assertEquals(SqlType.DROP, result.getSqlType());
        Assert.assertEquals(DBObjectType.DATABASE, result.getDbObjectType());
        Assert.assertEquals("db1", result.getDbObjectNameList().get(0));
    }

    @Test
    public void test_sql_visit_drop_table() {
        String sql = "drop table tb1";
        BasicResult result = ParserUtil.parseMysqlType(sql);
        Assert.assertEquals(SqlType.DROP, result.getSqlType());
        Assert.assertEquals(DBObjectType.TABLE, result.getDbObjectType());
        Assert.assertEquals("tb1", result.getDbObjectNameList().get(0));
    }

    @Test
    public void test_sql_drop_view() {
        String sql = "drop view v1";
        BasicResult result = ParserUtil.parseMysqlType(sql);
        Assert.assertEquals(SqlType.DROP, result.getSqlType());
        Assert.assertEquals(DBObjectType.VIEW, result.getDbObjectType());
        Assert.assertEquals("v1", result.getDbObjectNameList().get(0));
    }

    @Test
    public void test_sql_drop_procedure() {
        String sql = "drop procedure p1";
        BasicResult result = ParserUtil.parseMysqlType(sql);
        Assert.assertEquals(SqlType.DROP, result.getSqlType());
        Assert.assertEquals(DBObjectType.PROCEDURE, result.getDbObjectType());
        Assert.assertEquals("p1", result.getDbObjectNameList().get(0));
    }

    @Test
    public void test_sql_visit_drop_function() {
        String sql = "drop function f1";
        BasicResult result = ParserUtil.parseMysqlType(sql);
        Assert.assertEquals(SqlType.DROP, result.getSqlType());
        Assert.assertEquals(DBObjectType.FUNCTION, result.getDbObjectType());
        Assert.assertEquals("f1", result.getDbObjectNameList().get(0));
    }

    @Test
    public void test_sql_visit_create_database() {
        String sql = "create database db1";
        BasicResult result = ParserUtil.parseMysqlType(sql);
        Assert.assertEquals(SqlType.CREATE, result.getSqlType());
        Assert.assertEquals(DBObjectType.DATABASE, result.getDbObjectType());
        Assert.assertEquals("db1", result.getDbObjectNameList().get(0));
    }

    @Test
    public void test_sql_visit_create_table() {
        String sql = "create table test(c1 int, c2 int)";
        BasicResult result = ParserUtil.parseMysqlType(sql);
        Assert.assertEquals(SqlType.CREATE, result.getSqlType());
        Assert.assertEquals(DBObjectType.TABLE, result.getDbObjectType());
        Assert.assertEquals("test", result.getDbObjectNameList().get(0));
    }

    @Test
    public void test_sql_visit_create_table_with_quote() {
        String sql = "create table \"test\"(c1 int, c2 int)";
        BasicResult result = ParserUtil.parseOracleType(sql);
        Assert.assertEquals(SqlType.CREATE, result.getSqlType());
        Assert.assertEquals(DBObjectType.TABLE, result.getDbObjectType());
        Assert.assertEquals("test", result.getDbObjectNameList().get(0));
    }

    @Test
    public void test_sql_visit_create_view() {
        String sql = "create view v1 as select * from test";
        BasicResult result = ParserUtil.parseMysqlType(sql);
        Assert.assertEquals(SqlType.CREATE, result.getSqlType());
        Assert.assertEquals(DBObjectType.VIEW, result.getDbObjectType());
        Assert.assertEquals("v1", result.getDbObjectNameList().get(0));
    }

    @Test
    public void test_DQL() {
        String sql = "select * from t";
        BasicResult result = ParserUtil.parseMysqlType(sql);
        Assert.assertEquals(GeneralSqlType.DQL, ParserUtil.getGeneralSqlType(result));
    }

    @Test
    public void test_DDL() {
        String sql = "create table t(i int)";
        BasicResult result = ParserUtil.parseMysqlType(sql);
        Assert.assertEquals(GeneralSqlType.DDL, ParserUtil.getGeneralSqlType(result));
    }

    @Test
    public void test_Mysql_DML2() {
        String sql = "insert into t_test values(1);";
        BasicResult result = ParserUtil.parseMysqlType(sql);
        Assert.assertEquals(GeneralSqlType.DML, ParserUtil.getGeneralSqlType(result));
    }

    @Test
    public void test_Mysql_DML() {
        String sql = "delete from t where id=1";
        BasicResult result = ParserUtil.parseMysqlType(sql);
        Assert.assertEquals(GeneralSqlType.DML, ParserUtil.getGeneralSqlType(result));
    }

    @Test
    public void test_Oracle_DML() {
        String sql = "delete from t where id=1";
        BasicResult result = ParserUtil.parseOracleType(sql);
        Assert.assertEquals(GeneralSqlType.DML, ParserUtil.getGeneralSqlType(result));
    }

    @Test
    public void test_OTHER() {
        String sql = "commit";
        BasicResult result = ParserUtil.parseOracleType(sql);
        Assert.assertEquals(GeneralSqlType.OTHER, ParserUtil.getGeneralSqlType(result));
    }

    @Test
    public void test_alter_table_with_rename() {
        String sql = "alter table test001 rename test002";
        BasicResult result = ParserUtil.parseOracleType(sql);
        Assert.assertEquals(SqlType.ALTER, result.getSqlType());
        Assert.assertEquals(GeneralSqlType.DDL, ParserUtil.getGeneralSqlType(result));
    }

    @Test
    public void test_create_trigger() {
        String sql = "create or replace TRIGGER \"tri2\" AFTER\n"
                + "\n"
                + "  DELETE ON \"T_COLUMN\"\n"
                + "\n"
                + "  FOR EACH ROW\n"
                + "\n"
                + "  DISABLE\n"
                + "\n"
                + "BEGIN\n"
                + "\n"
                + " --your trigger body\n"
                + "\n"
                + "  select 1 from dual;\n"
                + "\n"
                + "END;";
        BasicResult result = ParserUtil.parseOracleType(sql);
        Assert.assertEquals(SqlType.CREATE, result.getSqlType());
        Assert.assertEquals(DBObjectType.TRIGGER, result.getDbObjectType());
    }

    @Test
    public void test_create_type() {
        String sql = "CREATE OR REPLACE TYPE \"CHZ\".\"TYPE2\"  IS OBJECT ( \n"
                + "\n"
                + "  \"ID\" number(38));";
        BasicResult result = ParserUtil.parseOracleType(sql);
        Assert.assertEquals(SqlType.CREATE, result.getSqlType());
        Assert.assertEquals(DBObjectType.TYPE, result.getDbObjectType());
    }

    @Test
    public void test_create_synonym() {
        String sql = "CREATE OR REPLACE SYNONYM \"Syn2\" FOR \"CHZ\".\"A1_COPY1\" ;";
        BasicResult result = ParserUtil.parseOracleType(sql);
        Assert.assertEquals(SqlType.CREATE, result.getSqlType());
        Assert.assertEquals(DBObjectType.SYNONYM, result.getDbObjectType());
    }

    @Test
    public void test_drop_trigger() {
        String sql = "drop trigger tri2;";
        BasicResult result = ParserUtil.parseOracleType(sql);
        Assert.assertEquals(SqlType.DROP, result.getSqlType());
        Assert.assertEquals(DBObjectType.TRIGGER, result.getDbObjectType());
    }

    @Test
    public void test_drop_function() {
        String sql = "drop function f2;";
        BasicResult result = ParserUtil.parseOracleType(sql);
        Assert.assertEquals(SqlType.DROP, result.getSqlType());
        Assert.assertEquals(DBObjectType.FUNCTION, result.getDbObjectType());
    }

    @Test
    public void test_drop_package() {
        String sql = "drop package oooooo;";
        BasicResult result = ParserUtil.parseOracleType(sql);
        Assert.assertEquals(SqlType.DROP, result.getSqlType());
        Assert.assertEquals(DBObjectType.PACKAGE, result.getDbObjectType());
    }

    @Test
    public void test_drop_type() {
        String sql = "drop type type2;";
        BasicResult result = ParserUtil.parseOracleType(sql);
        Assert.assertEquals(SqlType.DROP, result.getSqlType());
        Assert.assertEquals(DBObjectType.TYPE, result.getDbObjectType());
    }

    @Test
    public void test_drop_synonym() {
        String sql = "drop synonym \"Syn2\";";
        BasicResult result = ParserUtil.parseOracleType(sql);
        Assert.assertEquals(SqlType.DROP, result.getSqlType());
        Assert.assertEquals(DBObjectType.SYNONYM, result.getDbObjectType());
    }

    @Test
    public void test_oracle_syntax_error_1() {
        String sql = "select * f rom table;";
        BasicResult result = ParserUtil.parseOracleType(sql);
        Assert.assertTrue(result.getSyntaxError());
    }

    @Test
    public void test_oracle_syntax_error_2() {
        String sql = "abc";
        BasicResult result = ParserUtil.parseOracleType(sql);
        Assert.assertTrue(result.getSyntaxError());
    }

    @Test
    public void test_oracle_pl_syntax_error() {
        String sql = "CREATE1 OR REPLACE TYPE \"CHZ\".\"TYPE2\"  IS OBJECT ( \n"
                + "\n"
                + "  \"ID\" number(38));";
        BasicResult result = ParserUtil.parseOracleType(sql);
        Assert.assertTrue(result.getSyntaxError());
    }

    @Test
    public void test_mysql_syntax_error_1() {
        String sql = "selec t * from table;";
        BasicResult result = ParserUtil.parseMysqlType(sql);
        Assert.assertTrue(result.getSyntaxError());
    }

    @Test
    public void test_mysql_syntax_error_2() {
        String sql = "abc";
        BasicResult result = ParserUtil.parseMysqlType(sql);
        Assert.assertTrue(result.getSyntaxError());
    }

    @Test
    public void test_mysql_pl_syntax_error() {
        String sql = "CREATE1 OR REPLACE TYPE \"CHZ\".\"TYPE2\"  IS OBJECT ( \n"
                + "\n"
                + "  \"ID\" number(38));";
        BasicResult result = ParserUtil.parseOracleType(sql);
        Assert.assertTrue(result.getSyntaxError());
    }

    @Test
    public void test_mysql_set_session() {
        String sql = "SET SESSION time_zone = '+00:00';";
        BasicResult result = ParserUtil.parseMysqlType(sql);
        Assert.assertEquals(SqlType.SET_SESSION, result.getSqlType());
        Assert.assertEquals(SqlType.SET, result.getSqlType().getParentType());
        Assert.assertEquals(DBObjectType.SESSION_VARIABLE, result.getDbObjectType());
    }

    @Test
    public void test_mysql_set_global() {
        String sql = "SET GLOBAL time_zone = '+00:00';";
        BasicResult result = ParserUtil.parseMysqlType(sql);
        Assert.assertEquals(SqlType.SET, result.getSqlType());
        Assert.assertEquals(DBObjectType.GLOBAL_VARIABLE, result.getDbObjectType());
    }

    @Test
    public void test_oracle_set_global() {
        String sql = "SET GLOBAL time_zone = '+00:00';";
        BasicResult result = ParserUtil.parseMysqlType(sql);
        Assert.assertEquals(SqlType.SET, result.getSqlType());
        Assert.assertEquals(DBObjectType.GLOBAL_VARIABLE, result.getDbObjectType());
    }

    @Test
    public void test_oracle_alter_system() {
        String sql = "ALTER SYSTEM SET time_zone = '+00:00';";
        BasicResult result = ParserUtil.parseOracleType(sql);
        Assert.assertEquals(SqlType.ALTER, result.getSqlType());
        Assert.assertEquals(DBObjectType.OTHERS, result.getDbObjectType());
    }

    @Test
    public void test_mysql_set() {
        String sql = "SET time_zone = '+00:00';";
        BasicResult result = ParserUtil.parseMysqlType(sql);
        Assert.assertEquals(SqlType.SET, result.getSqlType());
        Assert.assertEquals(DBObjectType.SYSTEM_VARIABLE, result.getDbObjectType());
    }

    @Test
    public void test_mysql_call() {
        String sql = "call proc()";
        BasicResult result = ParserUtil.parseMysqlType(sql);
        Assert.assertEquals(SqlType.CALL, result.getSqlType());
        Assert.assertEquals(DBObjectType.PROCEDURE, result.getDbObjectType());
    }

    @Test
    public void test_oracle_call() {
        String sql = "call proc()";
        BasicResult result = ParserUtil.parseOracleType(sql);
        Assert.assertEquals(SqlType.CALL, result.getSqlType());
        Assert.assertEquals(DBObjectType.PROCEDURE, result.getDbObjectType());
    }

    @Test
    public void test_oracle_alterSession() {
        String sql = "alter SESSION set ob_query_timeout=6000000000;";
        BasicResult result = ParserUtil.parseOracleType(sql);
        Assert.assertEquals(SqlType.ALTER_SESSION, result.getSqlType());
        Assert.assertEquals(SqlType.ALTER, result.getSqlType().getParentType());
        Assert.assertEquals(DBObjectType.OTHERS, result.getDbObjectType());
    }

    @Test
    public void test_oracle_setSession() {
        String sql = "set SESSION ob_query_timeout=6000000000;";
        BasicResult result = ParserUtil.parseOracleType(sql);
        Assert.assertEquals(SqlType.SET_SESSION, result.getSqlType());
        Assert.assertEquals(SqlType.SET, result.getSqlType().getParentType());
        Assert.assertEquals(DBObjectType.SESSION_VARIABLE, result.getDbObjectType());
    }

    @Test
    public void test_oracle_comment_on_table() {
        String sql = "comment on table t is 'abc'";
        BasicResult result = ParserUtil.parseOracleType(sql);
        Assert.assertEquals(SqlType.COMMENT_ON, result.getSqlType());
        Assert.assertEquals(DBObjectType.TABLE, result.getDbObjectType());
    }

    @Test
    public void test_oracle_comment_on_column() {
        String sql = "comment on column t is 'abc'";
        BasicResult result = ParserUtil.parseOracleType(sql);
        Assert.assertEquals(SqlType.COMMENT_ON, result.getSqlType());
        Assert.assertEquals(DBObjectType.COLUMN, result.getDbObjectType());
    }

}
