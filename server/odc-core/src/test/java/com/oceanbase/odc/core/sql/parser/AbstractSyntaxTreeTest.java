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

package com.oceanbase.odc.core.sql.parser;

import java.math.BigDecimal;
import java.util.Collections;

import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.parser.constant.SqlType;
import com.oceanbase.tools.dbbrowser.parser.result.BasicResult;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Update_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Select_stmtContext;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.common.CharacterType;
import com.oceanbase.tools.sqlparser.statement.common.DataType;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnDefinition;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.expression.RelationReference;
import com.oceanbase.tools.sqlparser.statement.select.NameReference;
import com.oceanbase.tools.sqlparser.statement.select.Projection;
import com.oceanbase.tools.sqlparser.statement.select.Select;
import com.oceanbase.tools.sqlparser.statement.select.SelectBody;

/**
 * Test cases for {@link AbstractSyntaxTree}
 *
 * @author yh263208
 * @date 2023-11-20 14:30
 * @since ODC_release_4.2.3
 */
public class AbstractSyntaxTreeTest {

    @Test
    public void getRoot_selectOracleMode_getSelectStmtContext() {
        AbstractSyntaxTreeFactory factory = AbstractSyntaxTreeFactories.getAstFactory(DialectType.OB_ORACLE, 0);
        AbstractSyntaxTree ast = factory.buildAst("select * from show_tbl");
        ParseTree expect = ast.getRoot();
        Assert.assertEquals(Select_stmtContext.class, expect.getClass());
    }

    @Test
    public void getRoot_updateMysqlMode_getUpdateStmtContext() {
        AbstractSyntaxTreeFactory factory = AbstractSyntaxTreeFactories.getAstFactory(DialectType.OB_MYSQL, 0);
        AbstractSyntaxTree ast = factory.buildAst("update show_tbl set a='1'");
        ParseTree expect = ast.getRoot();
        Assert.assertEquals(Update_stmtContext.class, expect.getClass());
    }

    @Test
    public void getParseResult_plOracle_buildSucceed() {
        AbstractSyntaxTreeFactory factory = AbstractSyntaxTreeFactories.getAstFactory(DialectType.OB_ORACLE, 0);
        AbstractSyntaxTree ast = factory.buildAst("create or replace procedure test_proc(p1 in integer) as\n"
                + "begin\n"
                + "dbms_output.put_line('asdasd');\n"
                + "end;");
        BasicResult expect = ast.getParseResult();
        Assert.assertTrue(expect.isPlDdl());
        Assert.assertEquals(SqlType.CREATE, expect.getSqlType());
        Assert.assertEquals(DBObjectType.PROCEDURE, expect.getDbObjectType());
    }

    @Test
    public void getParseResult_plMysql_buildSucceed() {
        AbstractSyntaxTreeFactory factory = AbstractSyntaxTreeFactories.getAstFactory(DialectType.OB_MYSQL, 0);
        AbstractSyntaxTree ast = factory.buildAst("create procedure `test1_proc` () BEGIN\n"
                + "select * from t1_osc_new_;\n"
                + "END;");
        BasicResult expect = ast.getParseResult();
        Assert.assertTrue(expect.isPlDdl());
        Assert.assertEquals(SqlType.CREATE, expect.getSqlType());
        Assert.assertEquals(DBObjectType.PROCEDURE, expect.getDbObjectType());
    }

    @Test
    public void getStatement_selectOracleMode_getSelectStmt() {
        AbstractSyntaxTreeFactory factory = AbstractSyntaxTreeFactories.getAstFactory(DialectType.OB_ORACLE, 0);
        AbstractSyntaxTree ast = factory.buildAst("select col.* abc from dual");
        Statement actual = ast.getStatement();
        RelationReference r = new RelationReference("col", new RelationReference("*", null));
        Projection p = new Projection(r, "abc");
        NameReference from = new NameReference(null, "dual", null);
        Select expect = new Select(new SelectBody(Collections.singletonList(p), Collections.singletonList(from)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void getStatement_createTableOracleMode_getCreateTableStmt() {
        AbstractSyntaxTreeFactory factory = AbstractSyntaxTreeFactories.getAstFactory(DialectType.OB_MYSQL, 0);
        AbstractSyntaxTree ast = factory.buildAst("create table abcd (id varchar(64))");
        Statement actual = ast.getStatement();
        CreateTable expect = new CreateTable("abcd");
        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        expect.setTableElements(
                Collections.singletonList(new ColumnDefinition(new ColumnReference(null, null, "id"), dataType)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void getStatement_plMysql_getNull() {
        AbstractSyntaxTreeFactory factory = AbstractSyntaxTreeFactories.getAstFactory(DialectType.OB_MYSQL, 0);
        AbstractSyntaxTree ast = factory.buildAst("create procedure `test1_proc` () BEGIN\n"
                + "select * from t1_osc_new_;\n"
                + "END;");
        CreateStatement expect = new CreateStatement(new RelationFactor("`test1_proc`"));
        Assert.assertEquals(expect, ast.getStatement());
    }

    @Test
    public void getStatement_explainMysql_getNull() {
        AbstractSyntaxTreeFactory factory = AbstractSyntaxTreeFactories.getAstFactory(DialectType.OB_MYSQL, 0);
        AbstractSyntaxTree ast = factory.buildAst("explain abcd;");
        Assert.assertNull(ast.getStatement());
    }

    @Test
    public void getStatement_plOracle_getNull() {
        AbstractSyntaxTreeFactory factory = AbstractSyntaxTreeFactories.getAstFactory(DialectType.OB_ORACLE, 0);
        AbstractSyntaxTree ast = factory.buildAst("create or replace procedure test_proc(p1 in integer) as\n"
                + "begin\n"
                + "dbms_output.put_line('asdasd');\n"
                + "end;");
        CreateStatement expect = new CreateStatement(new RelationFactor("test_proc"));
        Assert.assertEquals(expect, ast.getStatement());
    }

    @Test
    public void getStatement_explainOracle_getNull() {
        AbstractSyntaxTreeFactory factory = AbstractSyntaxTreeFactories.getAstFactory(DialectType.OB_ORACLE, 0);
        AbstractSyntaxTree ast = factory.buildAst("explain xxx");
        Assert.assertNull(ast.getStatement());
    }

}
