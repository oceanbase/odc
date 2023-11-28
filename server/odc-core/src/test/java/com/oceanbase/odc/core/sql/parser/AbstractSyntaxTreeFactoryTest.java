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

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.parser.constant.SqlType;
import com.oceanbase.tools.dbbrowser.parser.result.BasicResult;
import com.oceanbase.tools.sqlparser.SyntaxErrorException;

/**
 * {@link AbstractSyntaxTreeFactoryTest}
 *
 * @author yh263208
 * @date 2023-11-20 14:04
 * @since ODC_release_4.2.3
 */
public class AbstractSyntaxTreeFactoryTest {

    @Test
    public void buildAst_rightSqlOracle_buildSucceed() {
        AbstractSyntaxTreeFactory factory = AbstractSyntaxTreeFactories.getAstFactory(DialectType.OB_ORACLE, 0);
        AbstractSyntaxTree ast = factory.buildAst("select * from tb");
        BasicResult expect = ast.getParseResult();
        Assert.assertFalse(expect.isPlDdl());
        Assert.assertEquals(SqlType.SELECT, expect.getSqlType());
    }

    @Test
    public void buildAst_rightSqlMysql_buildSucceed() {
        AbstractSyntaxTreeFactory factory = AbstractSyntaxTreeFactories.getAstFactory(DialectType.OB_MYSQL, 0);
        AbstractSyntaxTree ast = factory.buildAst("select * from tb");
        BasicResult expect = ast.getParseResult();
        Assert.assertFalse(expect.isPlDdl());
        Assert.assertEquals(SqlType.SELECT, expect.getSqlType());
    }

    @Test(expected = SyntaxErrorException.class)
    public void buildAst_syntaxErrorSqlOracle_expThrown() {
        AbstractSyntaxTreeFactory factory = AbstractSyntaxTreeFactories.getAstFactory(DialectType.OB_ORACLE, 0);
        factory.buildAst("select * fro tb");
    }

    @Test(expected = SyntaxErrorException.class)
    public void buildAst_syntaxErrorSqlMysql_expThrown() {
        AbstractSyntaxTreeFactory factory = AbstractSyntaxTreeFactories.getAstFactory(DialectType.OB_MYSQL, 0);
        factory.buildAst("select * fro tb");
    }

    @Test
    public void buildAst_plOracle_buildSucceed() {
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
    public void buildAst_plMysql_buildSucceed() {
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
    public void buildAst_pkg_buildSucceed() {
        AbstractSyntaxTreeFactory factory = AbstractSyntaxTreeFactories.getAstFactory(DialectType.OB_ORACLE, 0);
        AbstractSyntaxTree ast = factory.buildAst("create or replace package test_pl_debug_pkg AS\n"
                + " procedure pkg_inner_proc(p1 in integer);\n"
                + " function pkg_inner_func(p1 in integer) return varchar2;\n"
                + "end;");
        BasicResult expect = ast.getParseResult();
        Assert.assertTrue(expect.isPlDdl());
        Assert.assertEquals(SqlType.CREATE, expect.getSqlType());
        Assert.assertEquals(DBObjectType.PACKAGE, expect.getDbObjectType());
    }

}
