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
import java.util.List;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.sqlparser.adapter.oracle.OracleFromReferenceFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBLexer;
import com.oceanbase.tools.sqlparser.oboracle.OBParser;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Table_referenceContext;
import com.oceanbase.tools.sqlparser.statement.JoinType;
import com.oceanbase.tools.sqlparser.statement.Operator;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.expression.CompoundExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ExpressionParam;
import com.oceanbase.tools.sqlparser.statement.expression.FunctionCall;
import com.oceanbase.tools.sqlparser.statement.expression.RelationReference;
import com.oceanbase.tools.sqlparser.statement.select.ExpressionReference;
import com.oceanbase.tools.sqlparser.statement.select.FlashBackType;
import com.oceanbase.tools.sqlparser.statement.select.FlashbackUsage;
import com.oceanbase.tools.sqlparser.statement.select.FromReference;
import com.oceanbase.tools.sqlparser.statement.select.JoinCondition;
import com.oceanbase.tools.sqlparser.statement.select.JoinReference;
import com.oceanbase.tools.sqlparser.statement.select.NameReference;
import com.oceanbase.tools.sqlparser.statement.select.OnJoinCondition;
import com.oceanbase.tools.sqlparser.statement.select.PartitionType;
import com.oceanbase.tools.sqlparser.statement.select.PartitionUsage;
import com.oceanbase.tools.sqlparser.statement.select.Projection;
import com.oceanbase.tools.sqlparser.statement.select.SelectBody;
import com.oceanbase.tools.sqlparser.statement.select.UsingJoinCondition;
import com.oceanbase.tools.sqlparser.statement.select.oracle.Pivot;
import com.oceanbase.tools.sqlparser.statement.select.oracle.Pivot.ExpressionItem;
import com.oceanbase.tools.sqlparser.statement.select.oracle.Pivot.FunctionItem;
import com.oceanbase.tools.sqlparser.statement.select.oracle.UnPivot;
import com.oceanbase.tools.sqlparser.statement.select.oracle.UnPivot.InItem;

/**
 * {@link OracleFromReferenceFactoryTest}
 *
 * @author yh263208
 * @date 2022-12-06 16:30
 * @since ODC_release_4.1.0
 */
public class OracleFromReferenceFactoryTest {

    @Test
    public void generate_nameRefWithoutAny_generateNameRefSucceed() {
        Table_referenceContext context = getTableReferenceContext("select a from tab");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference expect = new NameReference(null, "tab", null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_nameRefWithSchema_generateNameRefSucceed() {
        Table_referenceContext context = getTableReferenceContext("select a from oracle.tab");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference expect = new NameReference("oracle", "tab", null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_nameRefWithPartition_generateNameRefSucceed() {
        Table_referenceContext context = getTableReferenceContext("select a from oracle.tab partition (col1,col2)");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference expect = new NameReference("oracle", "tab", null);
        expect.setPartitionUsage(new PartitionUsage(PartitionType.PARTITION, Arrays.asList("col1", "col2")));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_nameRefWithSubPartition_generateNameRefSucceed() {
        Table_referenceContext context = getTableReferenceContext("select a from oracle.tab SUBPARTITION (col1,col2)");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference expect = new NameReference("oracle", "tab", null);
        expect.setPartitionUsage(new PartitionUsage(PartitionType.SUB_PARTITION, Arrays.asList("col1", "col2")));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_nameRefWithSCNFlashback_generateNameRefSucceed() {
        Table_referenceContext context =
                getTableReferenceContext("select a from oracle.tab SUBPARTITION (col1,col2) as of scn chz");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference expect = new NameReference("oracle", "tab", null);
        expect.setPartitionUsage(new PartitionUsage(PartitionType.SUB_PARTITION, Arrays.asList("col1", "col2")));
        expect.setFlashbackUsage(new FlashbackUsage(FlashBackType.AS_OF_SCN, new RelationReference("chz", null)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_nameRefWithTIMESTAMPFlashback_generateNameRefSucceed() {
        Table_referenceContext context =
                getTableReferenceContext("select a from oracle.tab SUBPARTITION (col1,col2) as of timestamp chz");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference expect = new NameReference("oracle", "tab", null);
        expect.setPartitionUsage(new PartitionUsage(PartitionType.SUB_PARTITION, Arrays.asList("col1", "col2")));
        expect.setFlashbackUsage(new FlashbackUsage(FlashBackType.AS_OF_TIMESTAMP, new RelationReference("chz", null)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_nameRefWithAlias_generateNameRefSucceed() {
        Table_referenceContext context =
                getTableReferenceContext("select a from oracle.tab SUBPARTITION (col1,col2) as of timestamp chz alias");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference expect = new NameReference("oracle", "tab", "alias");
        expect.setPartitionUsage(new PartitionUsage(PartitionType.SUB_PARTITION, Arrays.asList("col1", "col2")));
        expect.setFlashbackUsage(new FlashbackUsage(FlashBackType.AS_OF_TIMESTAMP, new RelationReference("chz", null)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_nameRefWithUserVariable_generateNameRefSucceed() {
        Table_referenceContext context = getTableReferenceContext("select a from tab@userVar");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference expect = new NameReference(null, "tab", null);
        expect.setUserVariable("@userVar");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_nameRefWithDual_generateNameRefSucceed() {
        Table_referenceContext context = getTableReferenceContext("select a from dual abc");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference expect = new NameReference(null, "dual", "abc");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_nameRefWithTabRef_generateNameRefSucceed() {
        Table_referenceContext context = getTableReferenceContext("select a from ((oracle.chz))");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference expect = new NameReference("oracle", "chz", null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_nameRefWithExpr_generateNameRefSucceed() {
        Table_referenceContext context = getTableReferenceContext("select a from TABLE ((1+3*5)) abc");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        CompoundExpression left =
                new CompoundExpression(new ConstExpression("1"), new ConstExpression("3"), Operator.ADD);
        ConstExpression right = new ConstExpression("5");
        ExpressionReference expect = new ExpressionReference(new CompoundExpression(left, right, Operator.MUL), "abc");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_joinedTableWithFullOuterJoinOnCondition_generateJoinRefSucceed() {
        Table_referenceContext context = getTableReferenceContext(
                "select a from chz.tab1 full outer join gsh.tab2 on chz.tab1.col1=gsh.tab2.col2");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference left = new NameReference("chz", "tab1", null);
        NameReference right = new NameReference("gsh", "tab2", null);
        RelationReference col1 = new RelationReference("col1", null);
        RelationReference tab1 = new RelationReference("tab1", col1);
        RelationReference chz = new RelationReference("chz", tab1);
        RelationReference col2 = new RelationReference("col2", null);
        RelationReference tab2 = new RelationReference("tab2", col2);
        RelationReference gsh = new RelationReference("gsh", tab2);
        JoinCondition condition = new OnJoinCondition(new CompoundExpression(chz, gsh, Operator.EQ));
        JoinReference expect = new JoinReference(left, right, JoinType.FULL_OUTER_JOIN, condition);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_joinedTableWithFullJoinOnCondition_generateJoinRefSucceed() {
        Table_referenceContext context = getTableReferenceContext(
                "select a from chz.tab1 full join gsh.tab2 using (chz.tab1.col1, gsh.tab2.col2)");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference left = new NameReference("chz", "tab1", null);
        NameReference right = new NameReference("gsh", "tab2", null);
        ColumnReference c1 = new ColumnReference("chz", "tab1", "col1");
        ColumnReference c2 = new ColumnReference("gsh", "tab2", "col2");
        JoinCondition condition = new UsingJoinCondition(Arrays.asList(c1, c2));
        JoinReference expect = new JoinReference(left, right, JoinType.FULL_JOIN, condition);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_joinedTableWithLeftOuterJoinOnCondition_generateJoinRefSucceed() {
        Table_referenceContext context = getTableReferenceContext(
                "select a from chz.tab1 left outer join gsh.tab2 on chz.tab1.col1=gsh.tab2.col2");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference left = new NameReference("chz", "tab1", null);
        NameReference right = new NameReference("gsh", "tab2", null);
        RelationReference col1 = new RelationReference("col1", null);
        RelationReference tab1 = new RelationReference("tab1", col1);
        RelationReference chz = new RelationReference("chz", tab1);
        RelationReference col2 = new RelationReference("col2", null);
        RelationReference tab2 = new RelationReference("tab2", col2);
        RelationReference gsh = new RelationReference("gsh", tab2);
        JoinCondition condition = new OnJoinCondition(new CompoundExpression(chz, gsh, Operator.EQ));
        JoinReference expect = new JoinReference(left, right, JoinType.LEFT_OUTER_JOIN, condition);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_joinedTableWithLeftJoinOnCondition_generateJoinRefSucceed() {
        Table_referenceContext context = getTableReferenceContext(
                "select a from chz.tab1 Left join gsh.tab2 using (chz.tab1.col1, gsh.tab2.col2)");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference left = new NameReference("chz", "tab1", null);
        NameReference right = new NameReference("gsh", "tab2", null);
        ColumnReference c1 = new ColumnReference("chz", "tab1", "col1");
        ColumnReference c2 = new ColumnReference("gsh", "tab2", "col2");
        JoinCondition condition = new UsingJoinCondition(Arrays.asList(c1, c2));
        JoinReference expect = new JoinReference(left, right, JoinType.LEFT_JOIN, condition);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_joinedTableWithRightOuterJoinOnCondition_generateJoinRefSucceed() {
        Table_referenceContext context = getTableReferenceContext(
                "select a from chz.tab1 right outer join gsh.tab2 on chz.tab1.col1=gsh.tab2.col2");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference left = new NameReference("chz", "tab1", null);
        NameReference right = new NameReference("gsh", "tab2", null);
        RelationReference col1 = new RelationReference("col1", null);
        RelationReference tab1 = new RelationReference("tab1", col1);
        RelationReference chz = new RelationReference("chz", tab1);
        RelationReference col2 = new RelationReference("col2", null);
        RelationReference tab2 = new RelationReference("tab2", col2);
        RelationReference gsh = new RelationReference("gsh", tab2);
        JoinCondition condition = new OnJoinCondition(new CompoundExpression(chz, gsh, Operator.EQ));
        JoinReference expect = new JoinReference(left, right, JoinType.RIGHT_OUTER_JOIN, condition);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_joinedTableWithRightJoinOnCondition_generateJoinRefSucceed() {
        Table_referenceContext context = getTableReferenceContext(
                "select a from chz.tab1 right join gsh.tab2 using (chz.tab1.col1, gsh.tab2.col2)");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference left = new NameReference("chz", "tab1", null);
        NameReference right = new NameReference("gsh", "tab2", null);
        ColumnReference c1 = new ColumnReference("chz", "tab1", "col1");
        ColumnReference c2 = new ColumnReference("gsh", "tab2", "col2");
        JoinCondition condition = new UsingJoinCondition(Arrays.asList(c1, c2));
        JoinReference expect = new JoinReference(left, right, JoinType.RIGHT_JOIN, condition);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_joinedTableWithJoinOnCondition_generateJoinRefSucceed() {
        Table_referenceContext context =
                getTableReferenceContext("select a from chz.tab1 join gsh.tab2 on chz.tab1.col1=gsh.tab2.col2");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference left = new NameReference("chz", "tab1", null);
        NameReference right = new NameReference("gsh", "tab2", null);
        RelationReference col1 = new RelationReference("col1", null);
        RelationReference tab1 = new RelationReference("tab1", col1);
        RelationReference chz = new RelationReference("chz", tab1);
        RelationReference col2 = new RelationReference("col2", null);
        RelationReference tab2 = new RelationReference("tab2", col2);
        RelationReference gsh = new RelationReference("gsh", tab2);
        JoinCondition condition = new OnJoinCondition(new CompoundExpression(chz, gsh, Operator.EQ));
        JoinReference expect = new JoinReference(left, right, JoinType.JOIN, condition);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_joinedTableWithInnerJoinOnCondition_generateJoinRefSucceed() {
        Table_referenceContext context =
                getTableReferenceContext("select a from chz.tab1 a inner join gsh.tab2 on chz.tab1.col1=gsh.tab2.col2");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference left = new NameReference("chz", "tab1", "a");
        NameReference right = new NameReference("gsh", "tab2", null);
        RelationReference col1 = new RelationReference("col1", null);
        RelationReference tab1 = new RelationReference("tab1", col1);
        RelationReference chz = new RelationReference("chz", tab1);
        RelationReference col2 = new RelationReference("col2", null);
        RelationReference tab2 = new RelationReference("tab2", col2);
        RelationReference gsh = new RelationReference("gsh", tab2);
        JoinCondition condition = new OnJoinCondition(new CompoundExpression(chz, gsh, Operator.EQ));
        JoinReference expect = new JoinReference(left, right, JoinType.INNER_JOIN, condition);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_joinedTableWithJoinUsingCondition_generateJoinRefSucceed() {
        Table_referenceContext context =
                getTableReferenceContext("select a from chz.tab1 join gsh.tab2 using (chz.tab1.col1, gsh.tab2.col2)");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference left = new NameReference("chz", "tab1", null);
        NameReference right = new NameReference("gsh", "tab2", null);
        ColumnReference c1 = new ColumnReference("chz", "tab1", "col1");
        ColumnReference c2 = new ColumnReference("gsh", "tab2", "col2");
        JoinCondition condition = new UsingJoinCondition(Arrays.asList(c1, c2));
        JoinReference expect = new JoinReference(left, right, JoinType.JOIN, condition);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_joinedTableWithInnerJoinUsingCondition_generateJoinRefSucceed() {
        Table_referenceContext context = getTableReferenceContext(
                "select a from chz.tab1 a inner join gsh.tab2 using (chz.tab1.col1, gsh.tab2.col2)");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference left = new NameReference("chz", "tab1", "a");
        NameReference right = new NameReference("gsh", "tab2", null);
        ColumnReference c1 = new ColumnReference("chz", "tab1", "col1");
        ColumnReference c2 = new ColumnReference("gsh", "tab2", "col2");
        JoinCondition condition = new UsingJoinCondition(Arrays.asList(c1, c2));
        JoinReference expect = new JoinReference(left, right, JoinType.INNER_JOIN, condition);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_joinedTableWithCrossJoinUsingCondition_generateJoinRefSucceed() {
        Table_referenceContext context = getTableReferenceContext("select a from chz.tab1 a cross join gsh.tab2");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference left = new NameReference("chz", "tab1", "a");
        NameReference right = new NameReference("gsh", "tab2", null);
        JoinReference expect = new JoinReference(left, right, JoinType.CROSS_JOIN, null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_joinedTableWithNaturalJoinUsingCondition_generateJoinRefSucceed() {
        Table_referenceContext context = getTableReferenceContext("select a from chz.tab1 a natural join gsh.tab2");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference left = new NameReference("chz", "tab1", "a");
        NameReference right = new NameReference("gsh", "tab2", null);
        JoinReference expect = new JoinReference(left, right, JoinType.NATURAL_JOIN, null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_joinedTableWithNaturalInnerJoinUsingCondition_generateJoinRefSucceed() {
        Table_referenceContext context =
                getTableReferenceContext("select a from chz.tab1 a natural inner join gsh.tab2");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference left = new NameReference("chz", "tab1", "a");
        NameReference right = new NameReference("gsh", "tab2", null);
        JoinReference expect = new JoinReference(left, right, JoinType.NATURAL_INNER_JOIN, null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_joinedTableWithNaturalFullJoinUsingCondition_generateJoinRefSucceed() {
        Table_referenceContext context =
                getTableReferenceContext("select a from chz.tab1 a natural full join gsh.tab2");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference left = new NameReference("chz", "tab1", "a");
        NameReference right = new NameReference("gsh", "tab2", null);
        JoinReference expect = new JoinReference(left, right, JoinType.NATURAL_FULL_JOIN, null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_joinedTableWithNaturalFullOuterJoinUsingCondition_generateJoinRefSucceed() {
        Table_referenceContext context =
                getTableReferenceContext("select a from chz.tab1 a natural full outer join gsh.tab2");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference left = new NameReference("chz", "tab1", "a");
        NameReference right = new NameReference("gsh", "tab2", null);
        JoinReference expect = new JoinReference(left, right, JoinType.NATURAL_FULL_OUTER_JOIN, null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_joinedTableWithNaturalLeftJoinUsingCondition_generateJoinRefSucceed() {
        Table_referenceContext context =
                getTableReferenceContext("select a from chz.tab1 a natural left join gsh.tab2");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference left = new NameReference("chz", "tab1", "a");
        NameReference right = new NameReference("gsh", "tab2", null);
        JoinReference expect = new JoinReference(left, right, JoinType.NATURAL_LEFT_JOIN, null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_joinedTableWithNaturalLeftOuterJoinUsingCondition_generateJoinRefSucceed() {
        Table_referenceContext context =
                getTableReferenceContext("select a from chz.tab1 a natural left outer join gsh.tab2");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference left = new NameReference("chz", "tab1", "a");
        NameReference right = new NameReference("gsh", "tab2", null);
        JoinReference expect = new JoinReference(left, right, JoinType.NATURAL_LEFT_OUTER_JOIN, null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_joinedTableWithNaturalRightJoinUsingCondition_generateJoinRefSucceed() {
        Table_referenceContext context =
                getTableReferenceContext("select a from chz.tab1 a natural right join gsh.tab2");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference left = new NameReference("chz", "tab1", "a");
        NameReference right = new NameReference("gsh", "tab2", null);
        JoinReference expect = new JoinReference(left, right, JoinType.NATURAL_RIGHT_JOIN, null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_joinedTableWithNaturalRightOuterJoinUsingCondition_generateJoinRefSucceed() {
        Table_referenceContext context =
                getTableReferenceContext("select a from chz.tab1 a natural right outer join gsh.tab2");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference left = new NameReference("chz", "tab1", "a");
        NameReference right = new NameReference("gsh", "tab2", null);
        JoinReference expect = new JoinReference(left, right, JoinType.NATURAL_RIGHT_OUTER_JOIN, null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_joinedTableWithNaturalRightOuterJoinUsingConditionNRJoinoinOtherTable_generateJoinRefSucceed() {
        Table_referenceContext context = getTableReferenceContext(
                "select a from chz.tab1 a natural right outer join gsh.tab2 natural right join sl.tab3");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference left = new NameReference("chz", "tab1", "a");
        NameReference right = new NameReference("gsh", "tab2", null);
        JoinReference leftJoin = new JoinReference(left, right, JoinType.NATURAL_RIGHT_OUTER_JOIN, null);
        NameReference right1 = new NameReference("sl", "tab3", null);
        JoinReference expect = new JoinReference(leftJoin, right1, JoinType.NATURAL_RIGHT_JOIN, null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_joinedTableWithNaturalRightOuterJoinUsingConditionOtherCrossJoinOtherTable_generateJoinRefSucceed() {
        Table_referenceContext context = getTableReferenceContext(
                "select a from chz.tab1 a natural right outer join gsh.tab2 cross join sl.tab3");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference left = new NameReference("chz", "tab1", "a");
        NameReference right = new NameReference("gsh", "tab2", null);
        JoinReference leftJoin = new JoinReference(left, right, JoinType.NATURAL_RIGHT_OUTER_JOIN, null);
        NameReference right1 = new NameReference("sl", "tab3", null);
        JoinReference expect = new JoinReference(leftJoin, right1, JoinType.CROSS_JOIN, null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_joinedTableWithInnerJoinOnConditionFulljoinTable_generateJoinRefSucceed() {
        Table_referenceContext context = getTableReferenceContext(
                "select a from chz.tab1 a inner join gsh.tab2 on chz.tab1.col1=gsh.tab2.col2 full join sl.tab3 on chz.tab1.col1=gsh.tab2.col2");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference left = new NameReference("chz", "tab1", "a");
        NameReference right = new NameReference("gsh", "tab2", null);
        RelationReference col1 = new RelationReference("col1", null);
        RelationReference tab1 = new RelationReference("tab1", col1);
        RelationReference chz = new RelationReference("chz", tab1);
        RelationReference col2 = new RelationReference("col2", null);
        RelationReference tab2 = new RelationReference("tab2", col2);
        RelationReference gsh = new RelationReference("gsh", tab2);
        JoinCondition condition = new OnJoinCondition(new CompoundExpression(chz, gsh, Operator.EQ));
        JoinReference leftJoin = new JoinReference(left, right, JoinType.INNER_JOIN, condition);
        NameReference right1 = new NameReference("sl", "tab3", null);
        JoinCondition condition1 = new OnJoinCondition(new CompoundExpression(chz, gsh, Operator.EQ));
        JoinReference expect = new JoinReference(leftJoin, right1, JoinType.FULL_JOIN, condition1);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_joinedTableWithJoinUsingConditionJoinTable_generateJoinRefSucceed() {
        Table_referenceContext context = getTableReferenceContext(
                "select a from chz.tab1 join gsh.tab2 using (chz.tab1.col1, gsh.tab2.col2) join sl.tab3 on chz.tab1.col1=gsh.tab2.col2");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference left = new NameReference("chz", "tab1", null);
        NameReference right = new NameReference("gsh", "tab2", null);
        ColumnReference c1 = new ColumnReference("chz", "tab1", "col1");
        ColumnReference c2 = new ColumnReference("gsh", "tab2", "col2");
        JoinCondition condition = new UsingJoinCondition(Arrays.asList(c1, c2));
        JoinReference leftJoin = new JoinReference(left, right, JoinType.JOIN, condition);
        NameReference right1 = new NameReference("sl", "tab3", null);
        RelationReference col1 = new RelationReference("col1", null);
        RelationReference tab1 = new RelationReference("tab1", col1);
        RelationReference chz = new RelationReference("chz", tab1);
        RelationReference col2 = new RelationReference("col2", null);
        RelationReference tab2 = new RelationReference("tab2", col2);
        RelationReference gsh = new RelationReference("gsh", tab2);
        JoinCondition condition1 = new OnJoinCondition(new CompoundExpression(chz, gsh, Operator.EQ));
        JoinReference expect = new JoinReference(leftJoin, right1, JoinType.JOIN, condition1);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_joinedTableWithJoinUsingConditionInnerJoinTable_generateJoinRefSucceed() {
        Table_referenceContext context = getTableReferenceContext(
                "select a from chz.tab1 join gsh.tab2 using (chz.tab1.col1, gsh.tab2.col2) inner join sl.tab3 on chz.tab1.col1=gsh.tab2.col2");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference left = new NameReference("chz", "tab1", null);
        NameReference right = new NameReference("gsh", "tab2", null);
        ColumnReference c1 = new ColumnReference("chz", "tab1", "col1");
        ColumnReference c2 = new ColumnReference("gsh", "tab2", "col2");
        JoinCondition condition = new UsingJoinCondition(Arrays.asList(c1, c2));
        JoinReference leftJoin = new JoinReference(left, right, JoinType.JOIN, condition);
        NameReference right1 = new NameReference("sl", "tab3", null);
        RelationReference col1 = new RelationReference("col1", null);
        RelationReference tab1 = new RelationReference("tab1", col1);
        RelationReference chz = new RelationReference("chz", tab1);
        RelationReference col2 = new RelationReference("col2", null);
        RelationReference tab2 = new RelationReference("tab2", col2);
        RelationReference gsh = new RelationReference("gsh", tab2);
        JoinCondition condition1 = new OnJoinCondition(new CompoundExpression(chz, gsh, Operator.EQ));
        JoinReference expect = new JoinReference(leftJoin, right1, JoinType.INNER_JOIN, condition1);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_joinedTableWithJoinUsingConditionJoinTableUsing_generateJoinRefSucceed() {
        Table_referenceContext context = getTableReferenceContext(
                "select a from chz.tab1 join gsh.tab2 using (chz.tab1.col1, gsh.tab2.col2) join sl.tab3 using (chz.tab1.col1,gsh.tab2.col2)");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference left = new NameReference("chz", "tab1", null);
        NameReference right = new NameReference("gsh", "tab2", null);
        ColumnReference c1 = new ColumnReference("chz", "tab1", "col1");
        ColumnReference c2 = new ColumnReference("gsh", "tab2", "col2");
        JoinCondition condition = new UsingJoinCondition(Arrays.asList(c1, c2));
        JoinReference leftJoin = new JoinReference(left, right, JoinType.JOIN, condition);
        NameReference right1 = new NameReference("sl", "tab3", null);
        JoinReference expect = new JoinReference(leftJoin, right1, JoinType.JOIN, condition);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_joinedTableWithJoinUsingConditionInnerJoinTableUsing_generateJoinRefSucceed() {
        Table_referenceContext context = getTableReferenceContext(
                "select a from chz.tab1 join gsh.tab2 using (chz.tab1.col1, gsh.tab2.col2) inner join sl.tab3 using (chz.tab1.col1,gsh.tab2.col2)");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference left = new NameReference("chz", "tab1", null);
        NameReference right = new NameReference("gsh", "tab2", null);
        ColumnReference c1 = new ColumnReference("chz", "tab1", "col1");
        ColumnReference c2 = new ColumnReference("gsh", "tab2", "col2");
        JoinCondition condition = new UsingJoinCondition(Arrays.asList(c1, c2));
        JoinReference leftJoin = new JoinReference(left, right, JoinType.JOIN, condition);
        NameReference right1 = new NameReference("sl", "tab3", null);
        JoinReference expect = new JoinReference(leftJoin, right1, JoinType.INNER_JOIN, condition);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_subQueryWithPivot_generatePivotSucceed() {
        Table_referenceContext context = getTableReferenceContext(
                "select a from (select 1 from dual) tmp_select pivot(count(*) as alias_1, APPROX_COUNT_DISTINCT(1,2) for(col1,col2) in (col2 as alias_4, col2 alias_3, col3))");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        Projection projection = new Projection(new ConstExpression("1"), null);
        NameReference from = new NameReference(null, "dual", null);
        SelectBody selectBody = new SelectBody(Collections.singletonList(projection), Collections.singletonList(from));
        ExpressionReference expect = new ExpressionReference(selectBody, "tmp_select");

        ExpressionParam p1 = new ExpressionParam(new ConstExpression("*"));
        FunctionCall f1 = new FunctionCall("count", Collections.singletonList(p1));
        FunctionItem i1 = new FunctionItem(f1, "alias_1");
        ExpressionParam p2 = new ExpressionParam(new ConstExpression("1"));
        ExpressionParam p3 = new ExpressionParam(new ConstExpression("2"));
        FunctionCall f2 = new FunctionCall("APPROX_COUNT_DISTINCT", Arrays.asList(p2, p3));
        FunctionItem i2 = new FunctionItem(f2, null);
        ExpressionItem i3 = new ExpressionItem(new RelationReference("col2", null), "alias_4");
        ExpressionItem i4 = new ExpressionItem(new RelationReference("col2", null), "alias_3");
        ExpressionItem i5 = new ExpressionItem(new RelationReference("col3", null), null);
        Pivot pivot = new Pivot(Arrays.asList(i1, i2),
                Arrays.asList(new ColumnReference(null, null, "col1"), new ColumnReference(null, null, "col2")),
                Arrays.asList(i3, i4, i5));
        expect.setPivot(pivot);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_subQueryWithPivotAndAlias_generatePivotSucceed() {
        Table_referenceContext context = getTableReferenceContext(
                "select a from (select 1 from dual) tmp_select pivot(count(*) as alias_1, APPROX_COUNT_DISTINCT(1,2) for col1 in (col2 as alias_4, col2 alias_3, col3)) ooo");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        Projection projection = new Projection(new ConstExpression("1"), null);
        NameReference from = new NameReference(null, "dual", null);
        SelectBody selectBody = new SelectBody(Collections.singletonList(projection), Collections.singletonList(from));
        ExpressionReference expect = new ExpressionReference(selectBody, "tmp_select");

        ExpressionParam p1 = new ExpressionParam(new ConstExpression("*"));
        FunctionCall f1 = new FunctionCall("count", Collections.singletonList(p1));
        FunctionItem i1 = new FunctionItem(f1, "alias_1");
        ExpressionParam p2 = new ExpressionParam(new ConstExpression("1"));
        ExpressionParam p3 = new ExpressionParam(new ConstExpression("2"));
        FunctionCall f2 = new FunctionCall("APPROX_COUNT_DISTINCT", Arrays.asList(p2, p3));
        FunctionItem i2 = new FunctionItem(f2, null);
        ExpressionItem i3 = new ExpressionItem(new RelationReference("col2", null), "alias_4");
        ExpressionItem i4 = new ExpressionItem(new RelationReference("col2", null), "alias_3");
        ExpressionItem i5 = new ExpressionItem(new RelationReference("col3", null), null);
        Pivot pivot = new Pivot(Arrays.asList(i1, i2),
                Collections.singletonList(new ColumnReference(null, null, "col1")), Arrays.asList(i3, i4, i5));
        pivot.setAlias("ooo");
        expect.setPivot(pivot);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_tableNameWithUnPivot_generateUnPivotSucceed() {
        Table_referenceContext context = getTableReferenceContext(
                "select a from abc tmp unpivot exclude nulls ((a,b) for c in ((d,f) as col, e as p, q))");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        List<ColumnReference> unpivotColumns =
                Arrays.asList(new ColumnReference(null, null, "a"), new ColumnReference(null, null, "b"));
        List<ColumnReference> forColumns = Collections.singletonList(new ColumnReference(null, null, "c"));
        InItem i1 =
                new InItem(Arrays.asList(new ColumnReference(null, null, "d"), new ColumnReference(null, null, "f")),
                        new RelationReference("col", null));
        InItem i2 = new InItem(Collections.singletonList(new ColumnReference(null, null, "e")),
                new RelationReference("p", null));
        InItem i3 = new InItem(Collections.singletonList(new ColumnReference(null, null, "q")), null);
        UnPivot unPivot = new UnPivot(false, unpivotColumns, forColumns, Arrays.asList(i1, i2, i3));
        NameReference expect = new NameReference(null, "abc", "tmp");
        expect.setUnPivot(unPivot);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_tableNameWithUnPivotAndAlias_generateUnPivotSucceed() {
        Table_referenceContext context = getTableReferenceContext(
                "select a from abc tmp unpivot (a for (c, k) in ((d,f) as col, e as p, q)) aaas");
        StatementFactory<FromReference> factory = new OracleFromReferenceFactory(context);
        FromReference actual = factory.generate();

        List<ColumnReference> unpivotColumns = Collections.singletonList(new ColumnReference(null, null, "a"));
        List<ColumnReference> forColumns =
                Arrays.asList(new ColumnReference(null, null, "c"), new ColumnReference(null, null, "k"));
        InItem i1 =
                new InItem(Arrays.asList(new ColumnReference(null, null, "d"), new ColumnReference(null, null, "f")),
                        new RelationReference("col", null));
        InItem i2 = new InItem(Collections.singletonList(new ColumnReference(null, null, "e")),
                new RelationReference("p", null));
        InItem i3 = new InItem(Collections.singletonList(new ColumnReference(null, null, "q")), null);
        UnPivot unPivot = new UnPivot(false, unpivotColumns, forColumns, Arrays.asList(i1, i2, i3));
        unPivot.setAlias("aaas");
        NameReference expect = new NameReference(null, "abc", "tmp");
        expect.setUnPivot(unPivot);
        Assert.assertEquals(expect, actual);
    }

    private Table_referenceContext getTableReferenceContext(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.simple_select().from_list().table_references().table_reference(0);
    }

}
