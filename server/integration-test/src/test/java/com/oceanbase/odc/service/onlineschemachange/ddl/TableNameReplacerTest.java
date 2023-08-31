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
package com.oceanbase.odc.service.onlineschemachange.ddl;

import org.junit.Assert;
import org.junit.Test;

public class TableNameReplacerTest {
    private static final String CREATE_STMT = "create table t1 (id int);";
    private static final String CREATE_QUOTE_STMT = "create table \"t1\" (id int);";
    private static final String CREATE_ACCENT_STMT = "create table `t1` (id int);";
    private static final String ALTER_STMT = "alter table t1 add constraint constraint_t1_id unique (id);";
    private static final String ALTER_QUOTE_STMT = "alter table \"t1\" add constraint constraint_t1_id unique (id);";

    @Test
    public void test_RewriteCreateStmt_Mysql() {
        String newSql = new OBMysqlTableNameReplacer().replaceCreateStmt(CREATE_STMT, DdlUtils.getNewTableName("t1"));
        Assert.assertEquals("create table _t1_osc_new_ (id int);", newSql);
    }

    @Test
    public void test_RewriteCreateStmtWithAccent_Mysql() {
        String newSql =
                new OBMysqlTableNameReplacer().replaceCreateStmt(CREATE_ACCENT_STMT, DdlUtils.getNewTableName("`t1`"));
        Assert.assertEquals("create table `_t1_osc_new_` (id int);", newSql);
    }

    @Test
    public void test_RewriteCreateStmt_Oracle() {
        String newSql = new OBOracleTableNameReplacer().replaceCreateStmt(CREATE_STMT, DdlUtils.getNewTableName("t1"));
        Assert.assertEquals("create table _t1_osc_new_ (id int);", newSql);
    }

    @Test
    public void test_RewriteCreateStmtWithQuote_Oracle() {
        String newSql = new OBOracleTableNameReplacer().replaceCreateStmt(CREATE_QUOTE_STMT,
                DdlUtils.getNewTableName("\"t1\""));
        Assert.assertEquals("create table \"_t1_osc_new_\" (id int);", newSql);
    }

    @Test
    public void test_RewriteAlterStmt_Mysql() {
        String newSql = new OBMysqlTableNameReplacer().replaceCreateStmt(ALTER_STMT, DdlUtils.getNewTableName("t1"));
        Assert.assertEquals("alter table _t1_osc_new_ add constraint constraint_t1_id unique (id);", newSql);
    }

    @Test
    public void test_RewriteAlterStmt_Oracle() {
        String newSql = new OBOracleTableNameReplacer().replaceAlterStmt(ALTER_STMT, DdlUtils.getNewTableName("t1"));
        Assert.assertEquals("alter table _t1_osc_new_ add constraint constraint_t1_id unique (id);", newSql);
    }

    @Test
    public void test_RewriteAlterStmtQuote_Oracle() {
        String newSql =
                new OBOracleTableNameReplacer().replaceAlterStmt(ALTER_QUOTE_STMT, DdlUtils.getNewTableName("\"t1\""));
        Assert.assertEquals("alter table \"_t1_osc_new_\" add constraint constraint_t1_id unique (id);", newSql);

    }

}
