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

import com.oceanbase.tools.dbbrowser.model.DBIndexRangeType;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.parser.constant.SqlType;
import com.oceanbase.tools.dbbrowser.parser.result.ParseSqlResult;
import com.oceanbase.tools.sqlparser.SyntaxErrorException;

/**
 * @author wenniu.ly
 * @date 2021/8/26
 */
public class SqlParserTest {

    @Test
    public void testParseMysqlSelect() {
        String sql = "select * from test;";
        ParseSqlResult result = SqlParser.parseMysql(sql);
        Assert.assertEquals(true, result.isSupportLimit());

        sql = "select * from test for update;";
        result = SqlParser.parseMysql(sql);
        Assert.assertEquals(false, result.isSupportLimit());

        sql = "create table t_test(c1 int)";
        result = SqlParser.parseMysql(sql);
        Assert.assertEquals(false, result.isSupportLimit());

        sql = "insert into t_test values(1);";
        result = SqlParser.parseMysql(sql);
        Assert.assertEquals(false, result.isSupportLimit());

        sql = " update t_test set c1=100 where c1 in (select c1 from t);";
        result = SqlParser.parseMysql(sql);
        Assert.assertEquals(false, result.isSupportLimit());
    }


    @Test
    public void testParseOracleSelect() {
        String sql = "select * from test;";
        ParseSqlResult result = SqlParser.parseOracle(sql);
        Assert.assertEquals(true, result.isSupportLimit());

        sql = "select test.\"ROWID\", test.* from test;";
        result = SqlParser.parseOracle(sql);
        Assert.assertEquals(true, result.isSupportLimit());

        sql = "select * from test for update;";
        result = SqlParser.parseOracle(sql);
        Assert.assertEquals(false, result.isSupportLimit());

        sql = "create table t_test(c1 int)";
        result = SqlParser.parseOracle(sql);
        Assert.assertEquals(false, result.isSupportLimit());

        sql = "insert into t_test values(1);";
        result = SqlParser.parseOracle(sql);
        Assert.assertEquals(false, result.isSupportLimit());

        sql = " update t_test set c1=100 where c1 in (select c1 from t);";
        result = SqlParser.parseOracle(sql);
        Assert.assertEquals(false, result.isSupportLimit());

    }

    @Test
    public void testParseOracleSelectWithFetch() {
        String sql = "select * from test fetch first 3 ROWS ONLY;";
        ParseSqlResult result = SqlParser.parseOracle(sql);
        Assert.assertEquals(true, result.isFetchClause());

        sql = "select * from test where rownum<10;";
        result = SqlParser.parseOracle(sql);
        Assert.assertEquals(false, result.isFetchClause());
    }

    @Test
    public void testParseMysqlSelectWithLimit() {
        String sql = "select * from test limit 3;";
        ParseSqlResult result = SqlParser.parseMysql(sql);
        Assert.assertEquals(true, result.isLimitClause());

        sql = "select t.a,s.c from test t join stest s on t.b=s.b limit 5;";
        result = SqlParser.parseMysql(sql);
        Assert.assertEquals(true, result.isLimitClause());

        sql = "select * from (select * from nopart_nopri limit 10);";
        result = SqlParser.parseMysql(sql);
        Assert.assertEquals(true, result.isLimitClause());
    }

    @Test
    public void testParseOracleSelectWithWhere() {
        String sql = "select * from test fetch where id < 3;";
        ParseSqlResult result = SqlParser.parseOracle(sql);
        Assert.assertEquals(true, result.isWhereClause());

        sql = "select * from (select * from tt where id < 10);";
        result = SqlParser.parseOracle(sql);
        Assert.assertEquals(true, result.isWhereClause());

        sql = "select * from (select * from tt where id < 10) where rownum<10;";
        result = SqlParser.parseOracle(sql);
        Assert.assertEquals(true, result.isWhereClause());
    }

    @Test
    public void testParseMysqlWithBackQuote() {
        String sql = "select * from ```test```;";
        ParseSqlResult result = SqlParser.parseMysql(sql);
        Assert.assertEquals(true, result.isSupportLimit());
    }

    @Test(expected = SyntaxErrorException.class)
    public void testParseMysqlWithError() {
        String sql =
                "CREATE TABLE `alltype2` (`EMPNO` int(11) NOT NULL DEFAULT '1' COMMENT '1',`ENAME` varchar(10) default not null COMMENT '员工号')";
        ParseSqlResult result = SqlParser.parseMysql(sql);
    }

    @Test
    public void testParseOracleCreateIndex() {
        String sql = "create index idx_test on t_test_create_index(c2)";
        ParseSqlResult result = SqlParser.parseOracle(sql);
        Assert.assertEquals(SqlType.CREATE, result.getSqlType());
        Assert.assertEquals(DBObjectType.INDEX, result.getDbObjectType());
    }

    @Test
    public void testParseOracleCreateuser() {
        String sql = "create user u1 identified by 123456";
        ParseSqlResult result = SqlParser.parseOracle(sql);
        Assert.assertEquals(SqlType.CREATE, result.getSqlType());
        Assert.assertEquals(DBObjectType.USER, result.getDbObjectType());
    }

    @Test
    public void test_parse_oracle_index_global_range() {
        String sql = "create index idx_name on aa(col1,col2,col3) global;";
        ParseSqlResult result = SqlParser.parseOracle(sql);
        Assert.assertEquals(1, result.getIndexes().size());
        Assert.assertEquals(DBIndexRangeType.GLOBAL, result.getIndexes().get(0).getRange());
        Assert.assertEquals("idx_name", result.getIndexes().get(0).getName());
    }

    @Test
    public void test_parse_oracle_index_local_range() {
        String sql = "create index idx_name on aa(col1,col2,col3) local;";
        ParseSqlResult result = SqlParser.parseOracle(sql);
        Assert.assertEquals(1, result.getIndexes().size());
        Assert.assertEquals(DBIndexRangeType.LOCAL, result.getIndexes().get(0).getRange());
        Assert.assertEquals("idx_name", result.getIndexes().get(0).getName());
    }

    @Test
    public void test_parse_mysql_index_range() {
        String sql = "CREATE TABLE `test` (\n"
                + "  `id` int(11) DEFAULT NULL,\n"
                + "  KEY `idx_1` (`id`) BLOCK_SIZE 16384 LOCAL\n"
                + ")";
        ParseSqlResult result = SqlParser.parseMysql(sql);
        Assert.assertEquals(1, result.getIndexes().size());
        Assert.assertEquals(DBIndexRangeType.LOCAL, result.getIndexes().get(0).getRange());
        Assert.assertEquals("idx_1", result.getIndexes().get(0).getName());
    }

    @Test
    public void test_parse_mysql_key_index_range() {
        String sql = "CREATE TABLE `putong_index` (\n"
                + "  `name` varchar(20) NOT NULL,\n"
                + "  `age` int(10) NOT NULL,\n"
                + "  `address` varchar(20) NOT NULL,\n"
                + "  `sex` float NOT NULL,\n"
                + "  `class` int(10) NOT NULL,\n"
                + "  PRIMARY KEY (`name`),\n"
                + "  KEY `hehe` (`age`) BLOCK_SIZE 16384 LOCAL\n"
                + ")";
        ParseSqlResult result = SqlParser.parseMysql(sql);
        Assert.assertEquals(1, result.getIndexes().size());
        Assert.assertEquals(DBIndexRangeType.LOCAL, result.getIndexes().get(0).getRange());
        Assert.assertEquals("hehe", result.getIndexes().get(0).getName());
    }

    @Test
    public void test_parse_mysql_unique_index_range() {
        String sql = "CREATE TABLE `weiyi_index` (\n"
                + "  `name` varchar(20) NOT NULL,\n"
                + "  `age` int(10) NOT NULL,\n"
                + "  `address` varchar(20) NOT NULL,\n"
                + "  `sex` float NOT NULL,\n"
                + "  `class` int(10) NOT NULL,\n"
                + "  PRIMARY KEY (`name`),\n"
                + "  UNIQUE KEY `hehe` (`age`) BLOCK_SIZE 16384 GLOBAL\n"
                + ")";
        ParseSqlResult result = SqlParser.parseMysql(sql);
        Assert.assertEquals(1, result.getIndexes().size());
        Assert.assertEquals(DBIndexRangeType.GLOBAL, result.getIndexes().get(0).getRange());
        Assert.assertEquals("hehe", result.getIndexes().get(0).getName());
    }

    @Test
    public void test_parse_mysql_fulltext_index_range() {
        String sql = "CREATE TABLE `quanwen_index` (\n"
                + "  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,\n"
                + "  `title` varchar(200) DEFAULT NULL,\n"
                + "  `content` text DEFAULT NULL,\n"
                + "  PRIMARY KEY (`id`), \n"
                + " FULLTEXT KEY `title` (`title`, `content`) CTXCAT(`title`, `content`) WITH PARSER 'TAOBAO_CHN' BLOCK_SIZE 16384\n"
                + ") ";
        ParseSqlResult result = SqlParser.parseMysql(sql);
        Assert.assertEquals(1, result.getIndexes().size());
        Assert.assertEquals(DBIndexRangeType.GLOBAL, result.getIndexes().get(0).getRange());
        Assert.assertEquals("title", result.getIndexes().get(0).getName());
    }

    @Test
    public void testParseOracleReference() {
        String sql = "create table test (\n"
                + "  id int,\n"
                + "  name varchar(30),\n"
                + "  constraint fk_test_id foreign key (id) references t1(id),\n"
                + "  constraint fk_test_name foreign key (name) references sys.t2(name)\n"
                + ");";
        ParseSqlResult parseSqlResult = SqlParser.parseOracle(sql);
        Assert.assertEquals("sys", parseSqlResult.getForeignConstraint().get(1).getReferenceSchemaName());
    }

    @Test
    public void testParseMysqlReference() {
        String sql = "create table test (\n"
                + "  id int,\n"
                + "  name varchar(30),\n"
                + "  constraint fk_test_id foreign key (id) references t1(id),\n"
                + "  constraint fk_test_name foreign key (name) references sys.t2(name)\n"
                + ");";
        ParseSqlResult parseSqlResult = SqlParser.parseMysql(sql);
        Assert.assertEquals("sys", parseSqlResult.getForeignConstraint().get(1).getReferenceSchemaName());
    }

    @Test
    public void parseMysql_commitStmt_getSqlTypeSucceed() {
        ParseSqlResult actual = SqlParser.parseMysql("commit");
        Assert.assertEquals(DBObjectType.OTHERS, actual.getDbObjectType());
        Assert.assertEquals(SqlType.COMMIT, actual.getSqlType());
    }

    @Test
    public void parseMysql_rollbackStmt_getSqlTypeSucceed() {
        ParseSqlResult actual = SqlParser.parseMysql("rollback");
        Assert.assertEquals(DBObjectType.OTHERS, actual.getDbObjectType());
        Assert.assertEquals(SqlType.ROLLBACK, actual.getSqlType());
    }

    @Test
    public void parseOracle_commitStmt_getSqlTypeSucceed() {
        ParseSqlResult actual = SqlParser.parseOracle("commit");
        Assert.assertEquals(DBObjectType.OTHERS, actual.getDbObjectType());
        Assert.assertEquals(SqlType.COMMIT, actual.getSqlType());
    }

    @Test
    public void parseOracle_rollbackStmt_getSqlTypeSucceed() {
        ParseSqlResult actual = SqlParser.parseOracle("rollback");
        Assert.assertEquals(DBObjectType.OTHERS, actual.getDbObjectType());
        Assert.assertEquals(SqlType.ROLLBACK, actual.getSqlType());
    }

    @Test
    public void parseOracle_commentOnTable_getSqlTypeSucceed() {
        ParseSqlResult actual = SqlParser.parseOracle("comment on table a is 'xxx'");
        Assert.assertEquals(DBObjectType.TABLE, actual.getDbObjectType());
        Assert.assertEquals(SqlType.COMMENT_ON, actual.getSqlType());
    }

    @Test
    public void parseOracle_commentOnColumn_getSqlTypeSucceed() {
        ParseSqlResult actual = SqlParser.parseOracle("comment on column a is 'xxx'");
        Assert.assertEquals(DBObjectType.COLUMN, actual.getDbObjectType());
        Assert.assertEquals(SqlType.COMMENT_ON, actual.getSqlType());
    }

    @Test
    public void parseOracle_call_getSqlTypeSucceed() {
        ParseSqlResult actual = SqlParser.parseOracle("call proc()");
        Assert.assertEquals(DBObjectType.PROCEDURE, actual.getDbObjectType());
        Assert.assertEquals(SqlType.CALL, actual.getSqlType());
    }


    @Test
    public void parseMysql_setSession_getSqlTypeSucceed() {
        ParseSqlResult actual = SqlParser.parseMysql("SET SESSION time_zone = '+00:00';");
        Assert.assertEquals(DBObjectType.SESSION_VARIABLE, actual.getDbObjectType());
        Assert.assertEquals(SqlType.SET_SESSION, actual.getSqlType());
        Assert.assertEquals(SqlType.SET, actual.getSqlType().getParentType());
    }

    @Test
    public void parseMysql_setGlobal_getSqlTypeSucceed() {
        ParseSqlResult actual = SqlParser.parseMysql("SET GLOBAL time_zone = '+00:00';");
        Assert.assertEquals(DBObjectType.GLOBAL_VARIABLE, actual.getDbObjectType());
        Assert.assertEquals(SqlType.SET, actual.getSqlType());
    }

    @Test
    public void parseMysql_set_getSqlTypeSucceed() {
        ParseSqlResult actual = SqlParser.parseMysql("SET time_zone = '+00:00';");
        Assert.assertEquals(DBObjectType.SYSTEM_VARIABLE, actual.getDbObjectType());
        Assert.assertEquals(SqlType.SET, actual.getSqlType());
    }

    @Test
    public void parseOracle_alterSession_getSqlTypeSucceed() {
        ParseSqlResult actual = SqlParser.parseOracle("alter SESSION set ob_query_timeout=6000000000;");
        Assert.assertEquals(SqlType.ALTER_SESSION, actual.getSqlType());
        Assert.assertEquals(SqlType.ALTER, actual.getSqlType().getParentType());
        Assert.assertEquals(DBObjectType.OTHERS, actual.getDbObjectType());
    }

    @Test
    public void parseOracle_setSession_getSqlTypeSucceed() {
        ParseSqlResult actual = SqlParser.parseOracle("set SESSION ob_query_timeout=6000000000;");
        Assert.assertEquals(SqlType.SET_SESSION, actual.getSqlType());
        Assert.assertEquals(SqlType.SET, actual.getSqlType().getParentType());
        Assert.assertEquals(DBObjectType.SESSION_VARIABLE, actual.getDbObjectType());
    }

    @Test
    public void parseOracle_setGlobal_getSqlTypeSucceed() {
        ParseSqlResult actual = SqlParser.parseOracle("SET GLOBAL time_zone = '+00:00';");
        Assert.assertEquals(DBObjectType.GLOBAL_VARIABLE, actual.getDbObjectType());
        Assert.assertEquals(SqlType.SET, actual.getSqlType());
    }

    @Test
    public void parseOracle_alterSystem_getSqlTypeSucceed() {
        ParseSqlResult actual = SqlParser.parseOracle("alter system set time_zone = '+00:00';");
        Assert.assertEquals(DBObjectType.OTHERS, actual.getDbObjectType());
        Assert.assertEquals(SqlType.ALTER, actual.getSqlType());
    }


}
