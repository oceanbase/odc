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

import java.util.Arrays;
import java.util.Collections;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.sqlparser.adapter.oracle.OracleUpdateFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBLexer;
import com.oceanbase.tools.sqlparser.oboracle.OBParser;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Update_stmtContext;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.Operator;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.expression.CompoundExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;
import com.oceanbase.tools.sqlparser.statement.expression.RelationReference;
import com.oceanbase.tools.sqlparser.statement.select.ExpressionReference;
import com.oceanbase.tools.sqlparser.statement.select.FromReference;
import com.oceanbase.tools.sqlparser.statement.select.NameReference;
import com.oceanbase.tools.sqlparser.statement.select.PartitionType;
import com.oceanbase.tools.sqlparser.statement.select.PartitionUsage;
import com.oceanbase.tools.sqlparser.statement.select.Projection;
import com.oceanbase.tools.sqlparser.statement.select.SelectBody;
import com.oceanbase.tools.sqlparser.statement.update.Update;
import com.oceanbase.tools.sqlparser.statement.update.UpdateAssign;

/**
 * {@link OracleUpdateFactoryTest}
 *
 * @author yh263208
 * @date 2022-12-20 16:16
 * @since ODC_release_4.1.0
 */
public class OracleUpdateFactoryTest {

    @Test
    public void generate_updateWithWhereClause_generateSucceed() {
        Update_stmtContext context = getUpdateContext("update tab set col=1 where col=100");
        StatementFactory<Update> factory = new OracleUpdateFactory(context);
        Update actual = factory.generate();

        NameReference nameReference = new NameReference(null, "tab", null);
        Update expect = new Update(Arrays.asList(nameReference), Arrays.asList(new UpdateAssign(
                Arrays.asList(new ColumnReference(null, null, "col")),
                new ConstExpression("1"), false)));
        RelationReference left = new RelationReference("col", null);
        ConstExpression right = new ConstExpression("100");
        expect.setWhere(new CompoundExpression(left, right, Operator.EQ));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_updateWithWhereClauseCursor_generateSucceed() {
        Update_stmtContext context = getUpdateContext("update tab set col=1 where current of tab.col");
        StatementFactory<Update> factory = new OracleUpdateFactory(context);
        Update actual = factory.generate();

        NameReference nameReference = new NameReference(null, "tab", null);
        Update expect = new Update(Arrays.asList(nameReference), Arrays.asList(new UpdateAssign(
                Arrays.asList(new ColumnReference(null, null, "col")),
                new ConstExpression("1"), false)));
        RelationReference left = new RelationReference("tab", new RelationReference("col", null));
        expect.setWhere(left);
        expect.setCursor(true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_updateWithoutWhereClause_generateSucceed() {
        Update_stmtContext context = getUpdateContext("update tab set col=1");
        StatementFactory<Update> factory = new OracleUpdateFactory(context);
        Update actual = factory.generate();

        NameReference nameReference = new NameReference(null, "tab", null);
        Update expect = new Update(Arrays.asList(nameReference),
                Arrays.asList(new UpdateAssign(Arrays.asList(new ColumnReference(null, null, "col")),
                        new ConstExpression("1"), false)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_updateWithPartitionUsage_generateSucceed() {
        Update_stmtContext context = getUpdateContext("UPDATE schema.tab PARTITION(p1,p2) SET col=1;");
        StatementFactory<Update> factory = new OracleUpdateFactory(context);
        Update actual = factory.generate();

        NameReference nameReference = new NameReference("schema", "tab", null);
        nameReference.setPartitionUsage(new PartitionUsage(PartitionType.PARTITION, Arrays.asList("p1", "p2")));
        Update expect = new Update(Arrays.asList(nameReference),
                Arrays.asList(new UpdateAssign(Arrays.asList(new ColumnReference(null, null, "col")),
                        new ConstExpression("1"), false)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_updateAssignmentWithSubquery_generateSucceed() {
        Update_stmtContext context =
                getUpdateContext("UPDATE tab1 SET col1 = (SELECT tab2.col1 from tab2 where tab2.col2='abcd');");
        StatementFactory<Update> factory = new OracleUpdateFactory(context);
        Update actual = factory.generate();

        NameReference nameReference = new NameReference(null, "tab1", null);
        RelationReference relationReference = new RelationReference("tab2", new RelationReference("col1", null));
        Projection p = new Projection(relationReference, null);
        NameReference from = new NameReference(null, "tab2", null);
        SelectBody body = new SelectBody(Collections.singletonList(p), Collections.singletonList(from));
        Expression e1 = new RelationReference("tab2", new RelationReference("col2", null));
        Expression e2 = new ConstExpression("'abcd'");
        CompoundExpression where = new CompoundExpression(e1, e2, Operator.EQ);
        body.setWhere(where);
        Update expect = new Update(Arrays.asList(nameReference),
                Arrays.asList(new UpdateAssign(Arrays.asList(new ColumnReference(null, null, "col1")), body, false)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_updateAssignmentWithColumnList_generateSucceed() {
        Update_stmtContext context =
                getUpdateContext("UPDATE tab1 SET (col1,col2) = (SELECT tab2.col1 from tab2 where tab2.col2='abcd');");
        StatementFactory<Update> factory = new OracleUpdateFactory(context);
        Update actual = factory.generate();

        NameReference nameReference = new NameReference(null, "tab1", null);
        RelationReference relationReference = new RelationReference("tab2", new RelationReference("col1", null));
        Projection p = new Projection(relationReference, null);
        NameReference from = new NameReference(null, "tab2", null);
        SelectBody body = new SelectBody(Collections.singletonList(p), Collections.singletonList(from));
        Expression e1 = new RelationReference("tab2", new RelationReference("col2", null));
        Expression e2 = new ConstExpression("'abcd'");
        CompoundExpression where = new CompoundExpression(e1, e2, Operator.EQ);
        body.setWhere(where);
        Update expect = new Update(Arrays.asList(nameReference),
                Arrays.asList(new UpdateAssign(
                        Arrays.asList(new ColumnReference(null, null, "col1"), new ColumnReference(null, null, "col2")),
                        body, false)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_updateAssignmentUseDefault_generateSucceed() {
        Update_stmtContext context = getUpdateContext("update tab set col=DEFAULT, col2=1");
        StatementFactory<Update> factory = new OracleUpdateFactory(context);
        Update actual = factory.generate();

        FromReference tableReference = new NameReference(null, "tab", null);
        Update expect = new Update(Arrays.asList(tableReference),
                Arrays.asList(new UpdateAssign(Arrays.asList(new ColumnReference(null, null, "col")), null, true),
                        new UpdateAssign(Arrays.asList(new ColumnReference(null, null, "col2")),
                                new ConstExpression("1"), false)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_updateWithSelectParens_generateSucceed() {
        Update_stmtContext context =
                getUpdateContext("UPDATE (SELECT * FROM tab) v SET v.col2 = 10 WHERE v.col1 = 1;");
        StatementFactory<Update> factory = new OracleUpdateFactory(context);
        Update actual = factory.generate();

        NameReference from = new NameReference(null, "tab", null);
        SelectBody selectBody =
                new SelectBody(Collections.singletonList(new Projection()), Collections.singletonList(from));
        ExpressionReference updateTable = new ExpressionReference(selectBody, "v");
        Update expect = new Update(Arrays.asList(updateTable),
                Arrays.asList(new UpdateAssign(Arrays.asList(new ColumnReference(null, "v", "col2")),
                        new ConstExpression("10"), false)));
        RelationReference left = new RelationReference("v", new RelationReference("col1", null));
        ConstExpression right = new ConstExpression("1");
        expect.setWhere(new CompoundExpression(left, right, Operator.EQ));
        Assert.assertEquals(expect, actual);
    }

    private Update_stmtContext getUpdateContext(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.update_stmt();
    }

}
