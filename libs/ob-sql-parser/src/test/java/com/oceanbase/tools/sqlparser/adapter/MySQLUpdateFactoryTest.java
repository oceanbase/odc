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

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLUpdateFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Update_stmtContext;
import com.oceanbase.tools.sqlparser.statement.Operator;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.expression.CompoundExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;
import com.oceanbase.tools.sqlparser.statement.select.FromReference;
import com.oceanbase.tools.sqlparser.statement.select.NameReference;
import com.oceanbase.tools.sqlparser.statement.select.OrderBy;
import com.oceanbase.tools.sqlparser.statement.select.PartitionType;
import com.oceanbase.tools.sqlparser.statement.select.PartitionUsage;
import com.oceanbase.tools.sqlparser.statement.select.SortKey;
import com.oceanbase.tools.sqlparser.statement.select.mysql.Limit;
import com.oceanbase.tools.sqlparser.statement.update.Update;
import com.oceanbase.tools.sqlparser.statement.update.UpdateAssign;

/**
 * {@link MySQLUpdateFactoryTest}
 *
 * @author yh263208
 * @date 2022-12-20 16:11
 * @since ODC_release_4.1.0
 */
public class MySQLUpdateFactoryTest {

    @Test
    public void generate_updateWithWhereClause_generateSucceed() {
        Update_stmtContext context = getUpdateContext("update tab set col=1 where col=100");
        StatementFactory<Update> factory = new MySQLUpdateFactory(context);
        Update actual = factory.generate();

        FromReference tableReference = new NameReference(null, "tab", null);
        Update expect = new Update(Arrays.asList(tableReference), Arrays.asList(new UpdateAssign(
                Arrays.asList(new ColumnReference(null, null, "col")),
                new ConstExpression("1"), false)));
        ColumnReference left = new ColumnReference(null, null, "col");
        ConstExpression right = new ConstExpression("100");
        expect.setWhere(new CompoundExpression(left, right, Operator.EQ));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_updateWithoutWhereClause_generateSucceed() {
        Update_stmtContext context = getUpdateContext("update tab set col=1");
        StatementFactory<Update> factory = new MySQLUpdateFactory(context);
        Update actual = factory.generate();

        FromReference tableReference = new NameReference(null, "tab", null);
        Update expect = new Update(Arrays.asList(tableReference), Arrays.asList(new UpdateAssign(
                Arrays.asList(new ColumnReference(null, null, "col")),
                new ConstExpression("1"), false)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_updateWithOrderByAndLimit_generateSucceed() {
        Update_stmtContext context = getUpdateContext(
                "update tab set tab.col=1 order by chz.tab.col limit 4");
        StatementFactory<Update> factory = new MySQLUpdateFactory(context);
        Update actual = factory.generate();

        FromReference tableReference = new NameReference(null, "tab", null);
        Update expect = new Update(Arrays.asList(tableReference), Arrays.asList(new UpdateAssign(
                Arrays.asList(new ColumnReference(null, "tab", "col")),
                new ConstExpression("1"), false)));
        SortKey s1 = new SortKey(new ColumnReference("chz", "tab", "col"), null);
        expect.setLimit(new Limit(new ConstExpression("4")));
        expect.setOrderBy(new OrderBy(Arrays.asList(s1)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_updateWithTableReferences_generateSucceed() {
        Update_stmtContext context =
                getUpdateContext("UPDATE chz.t1, chz.t2 PARTITION(p2, p2) SET t1.c2 = 100, t2.c2 = 200");
        StatementFactory<Update> factory = new MySQLUpdateFactory(context);
        Update actual = factory.generate();

        NameReference nameReference1 = new NameReference("chz", "t1", null);
        NameReference nameReference2 = new NameReference("chz", "t2", null);
        nameReference2.setPartitionUsage(new PartitionUsage(PartitionType.PARTITION, Arrays.asList("p2", "p2")));
        Update expect = new Update(Arrays.asList(nameReference1, nameReference2), Arrays.asList(
                new UpdateAssign(
                        Arrays.asList(new ColumnReference(null, "t1", "c2")),
                        new ConstExpression("100"), false),
                new UpdateAssign(
                        Arrays.asList(new ColumnReference(null, "t2", "c2")),
                        new ConstExpression("200"), false)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_updateAssignmentUseDefault_generateSucceed() {
        Update_stmtContext context = getUpdateContext("update tab set col=DEFAULT, col2=1");
        StatementFactory<Update> factory = new MySQLUpdateFactory(context);
        Update actual = factory.generate();

        FromReference tableReference = new NameReference(null, "tab", null);
        Update expect = new Update(Arrays.asList(tableReference),
                Arrays.asList(new UpdateAssign(Arrays.asList(new ColumnReference(null, null, "col")), null, true),
                        new UpdateAssign(Arrays.asList(new ColumnReference(null, null, "col2")),
                                new ConstExpression("1"),
                                false)));
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
