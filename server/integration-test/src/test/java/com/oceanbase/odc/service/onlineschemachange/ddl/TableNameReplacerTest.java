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

import java.io.StringReader;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.sqlparser.OBMySQLParser;
import com.oceanbase.tools.sqlparser.OBOracleSQLParser;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineForeignConstraint;

public class TableNameReplacerTest {
    private static final String CREATE_STMT = "create table t1 (id int);";
    private static final String CREATE_QUOTE_STMT = "create table \"t1\" (id int);";
    private static final String CREATE_ACCENT_STMT = "create table `t1` (id int);";
    private static final String ALTER_STMT = "alter table t1 add constraint constraint_t1_id unique (id);";
    private static final String ALTER_QUOTE_STMT = "alter table \"t1\" add constraint constraint_t1_id unique (id);";

    @Test
    public void test_RewriteCreateStmt_Mysql() {
        String newSql = new OBMysqlTableNameReplacer().replaceCreateStmt(CREATE_STMT, getNewMySqlTableName("t1"));
        Assert.assertEquals("create table _t1_osc_new_ (id int);", newSql);
    }

    @Test
    public void test_RewriteCreateStmtWithAccent_Mysql() {
        String newSql =
                new OBMysqlTableNameReplacer().replaceCreateStmt(CREATE_ACCENT_STMT, getNewMySqlTableName("`t1`"));
        Assert.assertEquals("create table `_t1_osc_new_` (id int);", newSql);
    }

    @Test
    public void test_RewriteCreateStmt_Oracle() {
        String newSql = new OBOracleTableNameReplacer().replaceCreateStmt(CREATE_STMT, getNewOracleTableName("t1"));
        Assert.assertEquals("create table t1_osc_new_ (id int);", newSql);
    }

    @Test
    public void test_RewriteCreateStmtWittConstraint_Oracle() {
        String createSql = "CREATE TABLE CHILD_TABLE1 (\n"
                + "COL NUMBER NOT NULL,\n"
                + "COL1 NUMBER NOT NULL,\n"
                + "CONSTRAINT P1 PRIMARY KEY (COL),\n"
                + "CONSTRAINT U1 UNIQUE (COL1),\n"
                + "CONSTRAINT F1 FOREIGN KEY (COL) REFERENCES PARENT_TABLE1 (COL) ON DELETE CASCADE \n"
                + ")";
        String newSql = new OBOracleTableNameReplacer().replaceCreateStmt(createSql, "CHILD_TABLE_NEW");
        Statement statement = new OBOracleSQLParser().parse(new StringReader(newSql));
        Assert.assertTrue(statement instanceof CreateTable);
        CreateTable createTable = (CreateTable) statement;
        List<OutOfLineConstraint> constraints = createTable.getConstraints();
        Optional<OutOfLineConstraint> pk = constraints.stream().filter(OutOfLineConstraint::isPrimaryKey).findFirst();
        Assert.assertTrue(pk.isPresent());
        Assert.assertNotEquals("P1", pk.get().getConstraintName());

        Optional<OutOfLineConstraint> uk = constraints.stream().filter(OutOfLineConstraint::isUniqueKey).findFirst();
        Assert.assertTrue(uk.isPresent());
        Assert.assertNotEquals("U1", uk.get().getConstraintName());

        Optional<OutOfLineForeignConstraint> fk =
                constraints.stream().filter(c -> (c instanceof OutOfLineForeignConstraint))
                        .map(c -> (OutOfLineForeignConstraint) c).findFirst();
        Assert.assertTrue(fk.isPresent());
        Assert.assertNotEquals("F1", pk.get().getConstraintName());
    }



    @Test
    public void test_RewriteCreateStmtWittConstraint_MySql() {
        String createSql = "CREATE TABLE `child_table1` (\n"
                + "`col` int NOT NULL,\n"
                + "`col1` int NOT NULL,\n"
                + "CONSTRAINT `p1` PRIMARY KEY (`col`),\n"
                + "CONSTRAINT `u1` UNIQUE (`col`),\n"
                + "UNIQUE (`col`),\n"
                + "CONSTRAINT `f1` FOREIGN KEY (`col`) REFERENCES `parent_table1` (`col`) ON DELETE CASCADE ON "
                + "UPDATE NO ACTION\n"
                + ")\n";
        String newSql = new OBMysqlTableNameReplacer().replaceCreateStmt(createSql, "`child_table1_new`");
        Statement statement = new OBMySQLParser().parse(new StringReader(newSql));
        Assert.assertTrue(statement instanceof CreateTable);
        CreateTable createTable = (CreateTable) statement;
        List<OutOfLineConstraint> constraints = createTable.getConstraints();
        Optional<OutOfLineConstraint> pk = constraints.stream().filter(OutOfLineConstraint::isPrimaryKey).findFirst();
        Assert.assertTrue(pk.isPresent());
        Assert.assertEquals("`p1`", pk.get().getConstraintName());

        Optional<OutOfLineConstraint> uk = constraints.stream().filter(OutOfLineConstraint::isUniqueKey).findFirst();
        Assert.assertTrue(uk.isPresent());
        Assert.assertEquals("`u1`", uk.get().getConstraintName());

        Optional<OutOfLineForeignConstraint> fk =
                constraints.stream().filter(c -> (c instanceof OutOfLineForeignConstraint))
                        .map(c -> (OutOfLineForeignConstraint) c).findFirst();
        Assert.assertTrue(fk.isPresent());
        Assert.assertNotEquals("`f1`", pk.get().getConstraintName());
    }

    @Test
    public void test_RewriteCreateStmtWithQuote_Oracle() {
        String newSql = new OBOracleTableNameReplacer().replaceCreateStmt(CREATE_QUOTE_STMT,
                getNewOracleTableName("\"t1\""));
        Assert.assertEquals("create table \"t1_osc_new_\" (id int);", newSql);
    }

    @Test
    public void test_RewriteAlterStmt_Mysql() {
        String newSql = new OBMysqlTableNameReplacer().replaceCreateStmt(ALTER_STMT, getNewMySqlTableName("t1"));
        Assert.assertEquals("alter table _t1_osc_new_ add constraint constraint_t1_id unique (id);", newSql);
    }

    @Test
    public void test_RewriteAlterStmt_Oracle() {
        String newSql = new OBOracleTableNameReplacer().replaceAlterStmt(ALTER_STMT, getNewOracleTableName("t1"));
        Assert.assertEquals("alter table t1_osc_new_ add constraint constraint_t1_id unique (id);", newSql);
    }

    @Test
    public void test_RewriteAlterStmtQuote_Oracle() {
        String newSql =
                new OBOracleTableNameReplacer().replaceAlterStmt(ALTER_QUOTE_STMT, getNewOracleTableName("\"t1\""));
        Assert.assertEquals("alter table \"t1_osc_new_\" add constraint constraint_t1_id unique (id);", newSql);

    }

    private String getNewOracleTableName(String tableName) {
        return DdlUtils.getNewNameWithSuffix(tableName,
                DdlConstants.OSC_TABLE_NAME_PREFIX_OB_ORACLE, DdlConstants.NEW_TABLE_NAME_SUFFIX);
    }

    private String getNewMySqlTableName(String tableName) {
        return DdlUtils.getNewNameWithSuffix(tableName,
                DdlConstants.OSC_TABLE_NAME_PREFIX, DdlConstants.NEW_TABLE_NAME_SUFFIX);
    }
}
