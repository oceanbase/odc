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
package com.oceanbase.tools.sqlparser.adapter;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.sqlparser.adapter.oracle.OracleDeleteFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBLexer;
import com.oceanbase.tools.sqlparser.oboracle.OBParser;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Delete_stmtContext;
import com.oceanbase.tools.sqlparser.statement.Operator;
import com.oceanbase.tools.sqlparser.statement.delete.Delete;
import com.oceanbase.tools.sqlparser.statement.expression.CompoundExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;
import com.oceanbase.tools.sqlparser.statement.expression.RelationReference;
import com.oceanbase.tools.sqlparser.statement.select.NameReference;

/**
 * {@link OracleDeleteFactoryTest}
 *
 * @author yh263208
 * @date 2022-12-20 17:13
 * @since ODC_release_4.1.0
 */
public class OracleDeleteFactoryTest {

    @Test
    public void generate_deleteWithWhereClause_generateSucceed() {
        Delete_stmtContext context = getDeleteContext("delete from tab where col=100");
        StatementFactory<Delete> factory = new OracleDeleteFactory(context);
        Delete actual = factory.generate();

        NameReference nameReference = new NameReference(null, "tab", null);
        Delete expect = new Delete(context, nameReference);
        RelationReference left = new RelationReference("col", null);
        ConstExpression right = new ConstExpression("100");
        expect.setWhere(new CompoundExpression(left, right, Operator.EQ));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_deleteWithWhereClauseCursor_generateSucceed() {
        Delete_stmtContext context = getDeleteContext("delete from tab where current of tab.col");
        StatementFactory<Delete> factory = new OracleDeleteFactory(context);
        Delete actual = factory.generate();

        NameReference nameReference = new NameReference(null, "tab", null);
        Delete expect = new Delete(context, nameReference);
        RelationReference left = new RelationReference("tab", new RelationReference("col", null));
        expect.setWhere(left);
        expect.setCursor(true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_deleteWithoutWhereClause_generateSucceed() {
        Delete_stmtContext context = getDeleteContext("delete from tab");
        StatementFactory<Delete> factory = new OracleDeleteFactory(context);
        Delete actual = factory.generate();

        NameReference nameReference = new NameReference(null, "tab", null);
        Delete expect = new Delete(context, nameReference);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_deleteWithoutWhereClause_1_generateSucceed() {
        Delete_stmtContext context = getDeleteContext("delete tab");
        StatementFactory<Delete> factory = new OracleDeleteFactory(context);
        Delete actual = factory.generate();

        NameReference nameReference = new NameReference(null, "tab", null);
        Delete expect = new Delete(context, nameReference);
        Assert.assertEquals(expect, actual);
    }

    private Delete_stmtContext getDeleteContext(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.delete_stmt();
    }
}
