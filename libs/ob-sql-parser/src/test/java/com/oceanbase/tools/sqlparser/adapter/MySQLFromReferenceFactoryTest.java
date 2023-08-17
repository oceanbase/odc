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
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLFromReferenceFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Table_referenceContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Table_referencesContext;
import com.oceanbase.tools.sqlparser.statement.JoinType;
import com.oceanbase.tools.sqlparser.statement.Operator;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.expression.CompoundExpression;
import com.oceanbase.tools.sqlparser.statement.select.FlashBackType;
import com.oceanbase.tools.sqlparser.statement.select.FlashbackUsage;
import com.oceanbase.tools.sqlparser.statement.select.FromReference;
import com.oceanbase.tools.sqlparser.statement.select.JoinCondition;
import com.oceanbase.tools.sqlparser.statement.select.JoinReference;
import com.oceanbase.tools.sqlparser.statement.select.NameReference;
import com.oceanbase.tools.sqlparser.statement.select.OnJoinCondition;
import com.oceanbase.tools.sqlparser.statement.select.PartitionType;
import com.oceanbase.tools.sqlparser.statement.select.PartitionUsage;
import com.oceanbase.tools.sqlparser.statement.select.UsingJoinCondition;

/**
 * {@link MySQLFromReferenceFactoryTest}
 *
 * @author yh263208
 * @date 2022-12-12 01:14
 * @since ODC_release_4.1.0
 */
public class MySQLFromReferenceFactoryTest {

    @Test
    public void generate_nameRefWithoutAny_generateNameRefSucceed() {
        Table_referenceContext context = getTableReferenceContext("select a from tab");
        StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference expect = new NameReference(null, "tab", null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_relationDotKeyWord_generateNameRefSucceed() {
        Table_referenceContext context = getTableReferenceContext("select a from tab.ADD");
        StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference expect = new NameReference("tab", "ADD", null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_dotRelation_generateNameRefSucceed() {
        Table_referenceContext context = getTableReferenceContext("select a from .tab");
        StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference expect = new NameReference(null, "tab", null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_dotKeyWord_generateNameRefSucceed() {
        Table_referenceContext context = getTableReferenceContext("select a from .ADD");
        StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference expect = new NameReference(null, "ADD", null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_nameRefWithSchema_generateNameRefSucceed() {
        Table_referenceContext context = getTableReferenceContext("select a from mysql.tab");
        StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference expect = new NameReference("mysql", "tab", null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_nameRefWithPartition_generateNameRefSucceed() {
        Table_referenceContext context = getTableReferenceContext("select a from mysql.tab partition (col1,col2)");
        StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference expect = new NameReference("mysql", "tab", null);
        expect.setPartitionUsage(new PartitionUsage(PartitionType.PARTITION, Arrays.asList("col1", "col2")));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_nameRefWithFlashback_generateNameRefSucceed() {
        Table_referenceContext context =
                getTableReferenceContext("select a from mysql.tab PARTITION (col1,col2) as of SNAPSHOT chz");
        StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference expect = new NameReference("mysql", "tab", null);
        expect.setPartitionUsage(new PartitionUsage(PartitionType.PARTITION, Arrays.asList("col1", "col2")));
        expect.setFlashbackUsage(
                new FlashbackUsage(FlashBackType.AS_OF_SNAPSHOT, new ColumnReference(null, null, "chz")));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_nameRefWithAlias_generateNameRefSucceed() {
        Table_referenceContext context =
                getTableReferenceContext("select a from oracle.tab PARTITION (col1,col2) as of snapshot chz alias");
        StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference expect = new NameReference("oracle", "tab", "alias");
        expect.setPartitionUsage(new PartitionUsage(PartitionType.PARTITION, Arrays.asList("col1", "col2")));
        expect.setFlashbackUsage(
                new FlashbackUsage(FlashBackType.AS_OF_SNAPSHOT, new ColumnReference(null, null, "chz")));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_nameRefWithTabRef_generateNameRefSucceed() {
        Table_referenceContext context = getTableReferenceContext("select a from ((mysql.chz), opy),((ouy), ppo)");
        StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference expect = new NameReference("mysql", "chz", null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_joinedTableWithFullOuterJoinOnCondition_generateJoinRefSucceed() {
        Table_referenceContext context = getTableReferenceContext(
                "select a from chz.tab1 full outer join gsh.tab2 on chz.tab1.col1=gsh.tab2.col2");
        StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference left = new NameReference("chz", "tab1", null);
        NameReference right = new NameReference("gsh", "tab2", null);
        ColumnReference chz = new ColumnReference("chz", "tab1", "col1");
        ColumnReference gsh = new ColumnReference("gsh", "tab2", "col2");
        JoinCondition condition = new OnJoinCondition(new CompoundExpression(chz, gsh, Operator.EQ));
        JoinReference expect = new JoinReference(left, right, JoinType.FULL_OUTER_JOIN, condition);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_joinedTableWithFullJoinOnCondition_generateJoinRefSucceed() {
        Table_referenceContext context = getTableReferenceContext(
                "select a from chz.tab1 a full join gsh.tab2 using (chz.tab1.col1, gsh.tab2.col2)");
        StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference left = new NameReference("chz", "tab1", "a");
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
        StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference left = new NameReference("chz", "tab1", null);
        NameReference right = new NameReference("gsh", "tab2", null);
        ColumnReference chz = new ColumnReference("chz", "tab1", "col1");
        ColumnReference gsh = new ColumnReference("gsh", "tab2", "col2");
        JoinCondition condition = new OnJoinCondition(new CompoundExpression(chz, gsh, Operator.EQ));
        JoinReference expect = new JoinReference(left, right, JoinType.LEFT_OUTER_JOIN, condition);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_joinedTableWithLeftJoinOnCondition_generateJoinRefSucceed() {
        Table_referenceContext context = getTableReferenceContext(
                "select a from chz.tab1 Left join gsh.tab2 using (chz.tab1.col1, gsh.tab2.col2)");
        StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(context);
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
        StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference left = new NameReference("chz", "tab1", null);
        NameReference right = new NameReference("gsh", "tab2", null);
        ColumnReference chz = new ColumnReference("chz", "tab1", "col1");
        ColumnReference gsh = new ColumnReference("gsh", "tab2", "col2");
        JoinCondition condition = new OnJoinCondition(new CompoundExpression(chz, gsh, Operator.EQ));
        JoinReference expect = new JoinReference(left, right, JoinType.RIGHT_OUTER_JOIN, condition);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_joinedTableWithRightJoinOnCondition_generateJoinRefSucceed() {
        Table_referenceContext context = getTableReferenceContext(
                "select a from chz.tab1 right join gsh.tab2 using (chz.tab1.col1, gsh.tab2.col2)");
        StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(context);
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
        StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference left = new NameReference("chz", "tab1", null);
        NameReference right = new NameReference("gsh", "tab2", null);
        ColumnReference chz = new ColumnReference("chz", "tab1", "col1");
        ColumnReference gsh = new ColumnReference("gsh", "tab2", "col2");
        JoinCondition condition = new OnJoinCondition(new CompoundExpression(chz, gsh, Operator.EQ));
        JoinReference expect = new JoinReference(left, right, JoinType.JOIN, condition);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_joinedTableWithInnerJoinOnCondition_generateJoinRefSucceed() {
        Table_referenceContext context =
                getTableReferenceContext("select a from chz.tab1 a inner join gsh.tab2 on chz.tab1.col1=gsh.tab2.col2");
        StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference left = new NameReference("chz", "tab1", "a");
        NameReference right = new NameReference("gsh", "tab2", null);
        ColumnReference chz = new ColumnReference("chz", "tab1", "col1");
        ColumnReference gsh = new ColumnReference("gsh", "tab2", "col2");
        JoinCondition condition = new OnJoinCondition(new CompoundExpression(chz, gsh, Operator.EQ));
        JoinReference expect = new JoinReference(left, right, JoinType.INNER_JOIN, condition);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_joinedTableWithJoinUsingCondition_generateJoinRefSucceed() {
        Table_referenceContext context =
                getTableReferenceContext("select a from chz.tab1 join gsh.tab2 using (chz.tab1.col1, gsh.tab2.col2)");
        StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(context);
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
        StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(context);
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
        StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference left = new NameReference("chz", "tab1", "a");
        NameReference right = new NameReference("gsh", "tab2", null);
        JoinReference expect = new JoinReference(left, right, JoinType.CROSS_JOIN, null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_joinedTableWithNaturalJoinUsingCondition_generateJoinRefSucceed() {
        Table_referenceContext context = getTableReferenceContext("select a from chz.tab1 a natural join gsh.tab2");
        StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(context);
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
        StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(context);
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
        StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(context);
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
        StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(context);
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
        StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(context);
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
        StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(context);
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
        StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(context);
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
        StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(context);
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
        StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(context);
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
        StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(context);
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
        StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference left = new NameReference("chz", "tab1", "a");
        NameReference right = new NameReference("gsh", "tab2", null);
        ColumnReference chz = new ColumnReference("chz", "tab1", "col1");
        ColumnReference gsh = new ColumnReference("gsh", "tab2", "col2");
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
        StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference left = new NameReference("chz", "tab1", null);
        NameReference right = new NameReference("gsh", "tab2", null);
        ColumnReference c1 = new ColumnReference("chz", "tab1", "col1");
        ColumnReference c2 = new ColumnReference("gsh", "tab2", "col2");
        JoinCondition condition = new UsingJoinCondition(Arrays.asList(c1, c2));
        JoinReference leftJoin = new JoinReference(left, right, JoinType.JOIN, condition);
        NameReference right1 = new NameReference("sl", "tab3", null);
        ColumnReference chz = new ColumnReference("chz", "tab1", "col1");
        ColumnReference gsh = new ColumnReference("gsh", "tab2", "col2");
        JoinCondition condition1 = new OnJoinCondition(new CompoundExpression(chz, gsh, Operator.EQ));
        JoinReference expect = new JoinReference(leftJoin, right1, JoinType.JOIN, condition1);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_joinedTableWithJoinUsingConditionInnerJoinTable_generateJoinRefSucceed() {
        Table_referenceContext context = getTableReferenceContext(
                "select a from chz.tab1 join gsh.tab2 using (chz.tab1.col1, gsh.tab2.col2) inner join sl.tab3 on chz.tab1.col1=gsh.tab2.col2");
        StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(context);
        FromReference actual = factory.generate();

        NameReference left = new NameReference("chz", "tab1", null);
        NameReference right = new NameReference("gsh", "tab2", null);
        ColumnReference c1 = new ColumnReference("chz", "tab1", "col1");
        ColumnReference c2 = new ColumnReference("gsh", "tab2", "col2");
        JoinCondition condition = new UsingJoinCondition(Arrays.asList(c1, c2));
        JoinReference leftJoin = new JoinReference(left, right, JoinType.JOIN, condition);
        NameReference right1 = new NameReference("sl", "tab3", null);
        ColumnReference chz = new ColumnReference("chz", "tab1", "col1");
        ColumnReference gsh = new ColumnReference("gsh", "tab2", "col2");
        JoinCondition condition1 = new OnJoinCondition(new CompoundExpression(chz, gsh, Operator.EQ));
        JoinReference expect = new JoinReference(leftJoin, right1, JoinType.INNER_JOIN, condition1);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_joinedTableWithJoinUsingConditionJoinTableUsing_generateJoinRefSucceed() {
        Table_referenceContext context = getTableReferenceContext(
                "select a from chz.tab1 join gsh.tab2 using (chz.tab1.col1, gsh.tab2.col2) join sl.tab3 using (chz.tab1.col1,gsh.tab2.col2)");
        StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(context);
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
        StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(context);
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

    private Table_referenceContext getTableReferenceContext(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        Table_referencesContext tableRef = parser.simple_select().from_list().table_references();
        if (CollectionUtils.isNotEmpty(tableRef.table_reference())) {
            return tableRef.table_reference(0);
        }
        return tableRef.table_references_paren(0).table_reference(0);
    }

}
