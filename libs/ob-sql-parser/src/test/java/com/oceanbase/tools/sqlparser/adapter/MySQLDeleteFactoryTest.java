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

import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLDeleteFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Delete_stmtContext;
import com.oceanbase.tools.sqlparser.statement.JoinType;
import com.oceanbase.tools.sqlparser.statement.Operator;
import com.oceanbase.tools.sqlparser.statement.delete.Delete;
import com.oceanbase.tools.sqlparser.statement.delete.DeleteRelation;
import com.oceanbase.tools.sqlparser.statement.delete.MultiDelete;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.expression.CompoundExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;
import com.oceanbase.tools.sqlparser.statement.select.JoinReference;
import com.oceanbase.tools.sqlparser.statement.select.NameReference;
import com.oceanbase.tools.sqlparser.statement.select.OrderBy;
import com.oceanbase.tools.sqlparser.statement.select.PartitionType;
import com.oceanbase.tools.sqlparser.statement.select.PartitionUsage;
import com.oceanbase.tools.sqlparser.statement.select.SortKey;
import com.oceanbase.tools.sqlparser.statement.select.mysql.Limit;

/**
 * {@link MySQLDeleteFactoryTest}
 *
 * @author yh263208
 * @date 2022-12-20 17:22
 * @since ODC_release_4.1.0
 */
public class MySQLDeleteFactoryTest {

    @Test
    public void generate_deleteWithWhereClause_generateSucceed() {
        Delete_stmtContext context = getDeleteContext("delete from tab where col=100");
        StatementFactory<Delete> factory = new MySQLDeleteFactory(context);
        Delete actual = factory.generate();

        NameReference nameReference = new NameReference(null, "tab", null);
        Delete expect = new Delete(context, nameReference);
        ColumnReference left = new ColumnReference(null, null, "col");
        ConstExpression right = new ConstExpression("100");
        expect.setWhere(new CompoundExpression(left, right, Operator.EQ));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_deleteWithoutWhereClause_generateSucceed() {
        Delete_stmtContext context = getDeleteContext("delete from tab");
        StatementFactory<Delete> factory = new MySQLDeleteFactory(context);
        Delete actual = factory.generate();

        NameReference nameReference = new NameReference(null, "tab", null);
        Delete expect = new Delete(context, nameReference);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_deleteSingleTableWithPartition_generateSucceed() {
        Delete_stmtContext context =
                getDeleteContext("DELETE FROM tab PARTITION(p1) WHERE user = 'test' ORDER BY col LIMIT 100;");
        StatementFactory<Delete> factory = new MySQLDeleteFactory(context);
        Delete actual = factory.generate();

        NameReference nameReference = new NameReference(null, "tab", null);
        nameReference.setPartitionUsage(new PartitionUsage(PartitionType.PARTITION, Arrays.asList("p1")));
        Delete expect = new Delete(context, nameReference);
        ColumnReference left = new ColumnReference(null, null, "user");
        ConstExpression right = new ConstExpression("'test'");
        expect.setWhere(new CompoundExpression(left, right, Operator.EQ));
        expect.setLimit(new Limit(new ConstExpression("100")));
        SortKey s1 = new SortKey(new ColumnReference(null, null, "col"), null);
        expect.setOrderBy(new OrderBy(Arrays.asList(s1)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_deleteMultiTableWithoutUSING_generateSucceed() {
        Delete_stmtContext context =
                getDeleteContext("DELETE t1, t2 FROM t1 INNER JOIN t2 INNER JOIN t3 WHERE t1.id=t2.id;");
        StatementFactory<Delete> factory = new MySQLDeleteFactory(context);
        Delete actual = factory.generate();

        NameReference left = new NameReference(null, "t1", null);
        NameReference right = new NameReference(null, "t2", null);
        JoinReference joinLeft = new JoinReference(left, right, JoinType.INNER_JOIN, null);
        NameReference joinRight = new NameReference(null, "t3", null);
        JoinReference joinReference = new JoinReference(joinLeft, joinRight, JoinType.INNER_JOIN, null);
        MultiDelete multiDelete =
                new MultiDelete(
                        Arrays.asList(new DeleteRelation(null, "t1", false), new DeleteRelation(null, "t2", false)),
                        false, Arrays.asList(joinReference));
        Delete expect = new Delete(context, multiDelete);

        ColumnReference colLeft = new ColumnReference(null, "t1", "id");
        ColumnReference colRight = new ColumnReference(null, "t2", "id");
        CompoundExpression expression = new CompoundExpression(colLeft, colRight, Operator.EQ);
        expect.setWhere(expression);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_deleteMultiTableWithUSING_generateSucceed() {
        Delete_stmtContext context =
                getDeleteContext("DELETE FROM t1, t2 USING t1 INNER JOIN t2 INNER JOIN t3 WHERE t1.id=t2.id;");
        StatementFactory<Delete> factory = new MySQLDeleteFactory(context);
        Delete actual = factory.generate();

        NameReference left = new NameReference(null, "t1", null);
        NameReference right = new NameReference(null, "t2", null);
        JoinReference joinLeft = new JoinReference(left, right, JoinType.INNER_JOIN, null);
        NameReference joinRight = new NameReference(null, "t3", null);
        JoinReference joinReference = new JoinReference(joinLeft, joinRight, JoinType.INNER_JOIN, null);
        MultiDelete multiDelete =
                new MultiDelete(
                        Arrays.asList(new DeleteRelation(null, "t1", false), new DeleteRelation(null, "t2", false)),
                        true, Arrays.asList(joinReference));
        Delete expect = new Delete(context, multiDelete);

        ColumnReference colLeft = new ColumnReference(null, "t1", "id");
        ColumnReference colRight = new ColumnReference(null, "t2", "id");
        CompoundExpression expression = new CompoundExpression(colLeft, colRight, Operator.EQ);
        expect.setWhere(expression);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_deleteSingeTableWithMultiTableReference_generateSucceed() {
        Delete_stmtContext context = getDeleteContext("DELETE t2 FROM t1,t2 PARTITION(p2) WHERE t1.c1 = t2.c1;");
        StatementFactory<Delete> factory = new MySQLDeleteFactory(context);
        Delete actual = factory.generate();

        NameReference nameReference1 = new NameReference(null, "t1", null);
        NameReference nameReference2 = new NameReference(null, "t2", null);
        nameReference2.setPartitionUsage(new PartitionUsage(PartitionType.PARTITION, Arrays.asList("p2")));
        MultiDelete multiDelete =
                new MultiDelete(Arrays.asList(new DeleteRelation(null, "t2", false)), false,
                        Arrays.asList(nameReference1, nameReference2));
        Delete expect = new Delete(context, multiDelete);

        ColumnReference colLeft = new ColumnReference(null, "t1", "c1");
        ColumnReference colRight = new ColumnReference(null, "t2", "c1");
        CompoundExpression expression = new CompoundExpression(colLeft, colRight, Operator.EQ);
        expect.setWhere(expression);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_deleteMultiTableUsingAlias_generateSucceed() {
        Delete_stmtContext context =
                getDeleteContext("DELETE a1, a2 FROM t1 AS a1 INNER JOIN t2 AS a2 WHERE a1.id=a2.id;");
        StatementFactory<Delete> factory = new MySQLDeleteFactory(context);
        Delete actual = factory.generate();

        NameReference left = new NameReference(null, "t1", "a1");
        NameReference right = new NameReference(null, "t2", "a2");
        JoinReference joinReference = new JoinReference(left, right, JoinType.INNER_JOIN, null);
        MultiDelete multiDelete =
                new MultiDelete(
                        Arrays.asList(new DeleteRelation(null, "a1", false), new DeleteRelation(null, "a2", false)),
                        false, Arrays.asList(joinReference));
        Delete expect = new Delete(context, multiDelete);

        ColumnReference colLeft = new ColumnReference(null, "a1", "id");
        ColumnReference colRight = new ColumnReference(null, "a2", "id");
        CompoundExpression expression = new CompoundExpression(colLeft, colRight, Operator.EQ);
        expect.setWhere(expression);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_deleteMultiTableWithFullDeleteRelation_generateSucceed() {
        Delete_stmtContext context =
                getDeleteContext(
                        "DELETE FROM test.t1.*, test.t2.* USING test.t1,test.t2 WHERE test.t1.c1 = test.t2.c1;");
        StatementFactory<Delete> factory = new MySQLDeleteFactory(context);
        Delete actual = factory.generate();

        NameReference nameReference1 = new NameReference("test", "t1", null);
        NameReference nameReference2 = new NameReference("test", "t2", null);
        MultiDelete multiDelete =
                new MultiDelete(
                        Arrays.asList(new DeleteRelation("test", "t1", true), new DeleteRelation("test", "t2", true)),
                        true, Arrays.asList(nameReference1, nameReference2));
        Delete expect = new Delete(context, multiDelete);

        ColumnReference colLeft = new ColumnReference("test", "t1", "c1");
        ColumnReference colRight = new ColumnReference("test", "t2", "c1");
        CompoundExpression expression = new CompoundExpression(colLeft, colRight, Operator.EQ);
        expect.setWhere(expression);
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
