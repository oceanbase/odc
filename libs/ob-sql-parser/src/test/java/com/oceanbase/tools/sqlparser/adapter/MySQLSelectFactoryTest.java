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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLSelectFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Select_stmtContext;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.Operator;
import com.oceanbase.tools.sqlparser.statement.common.Window;
import com.oceanbase.tools.sqlparser.statement.common.WindowBody;
import com.oceanbase.tools.sqlparser.statement.common.WindowOffset;
import com.oceanbase.tools.sqlparser.statement.common.WindowOffsetType;
import com.oceanbase.tools.sqlparser.statement.common.WindowSpec;
import com.oceanbase.tools.sqlparser.statement.common.WindowType;
import com.oceanbase.tools.sqlparser.statement.expression.ArrayExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.expression.CompoundExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;
import com.oceanbase.tools.sqlparser.statement.expression.IntervalExpression;
import com.oceanbase.tools.sqlparser.statement.select.ExpressionReference;
import com.oceanbase.tools.sqlparser.statement.select.FlashBackType;
import com.oceanbase.tools.sqlparser.statement.select.FlashbackUsage;
import com.oceanbase.tools.sqlparser.statement.select.ForUpdate;
import com.oceanbase.tools.sqlparser.statement.select.NameReference;
import com.oceanbase.tools.sqlparser.statement.select.OrderBy;
import com.oceanbase.tools.sqlparser.statement.select.Projection;
import com.oceanbase.tools.sqlparser.statement.select.RelatedSelectBody;
import com.oceanbase.tools.sqlparser.statement.select.RelationType;
import com.oceanbase.tools.sqlparser.statement.select.Select;
import com.oceanbase.tools.sqlparser.statement.select.SelectBody;
import com.oceanbase.tools.sqlparser.statement.select.SortDirection;
import com.oceanbase.tools.sqlparser.statement.select.SortKey;
import com.oceanbase.tools.sqlparser.statement.select.WaitOption;
import com.oceanbase.tools.sqlparser.statement.select.WithTable;
import com.oceanbase.tools.sqlparser.statement.select.mysql.Limit;

/**
 * {@link MySQLSelectFactoryTest}
 *
 * @author yh263208
 * @date 2022-12-13 12:02
 * @since ODC_release_4.1.0
 */
public class MySQLSelectFactoryTest {

    @Test
    public void generate_onlyIncludeSelectItemsAndWheres_generateSelectSucceed() {
        Select_stmtContext context = getSelectContext("select col.* abc from dual");
        StatementFactory<Select> factory = new MySQLSelectFactory(context);
        Select actual = factory.generate();

        ColumnReference r = new ColumnReference(null, "col", "*");
        Projection p = new Projection(r, "abc");
        NameReference from = new NameReference(null, "dual", null);
        Select expect = new Select(new SelectBody(Collections.singletonList(p), Collections.singletonList(from)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_unionDistinctDualTable_generateSelectSucceed() {
        Select_stmtContext context = getSelectContext("select col.* abc from dual union distinct select * from dual");
        StatementFactory<Select> factory = new MySQLSelectFactory(context);
        Select actual = factory.generate();

        ColumnReference r = new ColumnReference(null, "col", "*");
        Projection p = new Projection(r, "abc");
        NameReference from = new NameReference(null, "dual", null);
        SelectBody selectBody = new SelectBody(Collections.singletonList(p), Collections.singletonList(from));
        selectBody.setRelatedSelect(new RelatedSelectBody(getDefaultSelectSimple(), RelationType.UNION_DISTINCT));
        Select expect = new Select(selectBody);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_parentSelectUnionDual_generateSelectSucceed() {
        Select_stmtContext context = getSelectContext("(select col.* abc from dual) union distinct select * from dual");
        StatementFactory<Select> factory = new MySQLSelectFactory(context);
        Select actual = factory.generate();

        ColumnReference r = new ColumnReference(null, "col", "*");
        Projection p = new Projection(r, "abc");
        NameReference from = new NameReference(null, "dual", null);
        SelectBody selectBody = new SelectBody(Collections.singletonList(p), Collections.singletonList(from));
        selectBody.setRelatedSelect(new RelatedSelectBody(getDefaultSelectSimple(), RelationType.UNION_DISTINCT));
        Select expect = new Select(selectBody);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_orderAndLimitUnion_generateSelectSucceed() {
        Select_stmtContext context = getSelectContext(
                "select col.* abc from tab order by col desc approx limit 3 union distinct select * from dual");
        StatementFactory<Select> factory = new MySQLSelectFactory(context);
        Select actual = factory.generate();

        ColumnReference r = new ColumnReference(null, "col", "*");
        Projection p = new Projection(r, "abc");
        NameReference from = new NameReference(null, "tab", null);
        SelectBody selectBody = new SelectBody(Collections.singletonList(p), Collections.singletonList(from));
        selectBody.setApproximate(true);
        SortKey s = new SortKey(new ColumnReference(null, null, "col"), SortDirection.DESC);
        selectBody.setOrderBy(new OrderBy(Collections.singletonList(s)));
        selectBody.setLimit(new Limit(new ConstExpression("3")));
        selectBody.setRelatedSelect(new RelatedSelectBody(getDefaultSelectSimple(), RelationType.UNION_DISTINCT));
        Select expect = new Select(selectBody);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_multiUnion_generateSelectSucceed() {
        Select_stmtContext context = getSelectContext(
                "select col.* abc from tab order by col desc limit 3 union distinct select * from dual union unique select * from dual");
        StatementFactory<Select> factory = new MySQLSelectFactory(context);
        Select actual = factory.generate();

        ColumnReference r = new ColumnReference(null, "col", "*");
        Projection p = new Projection(r, "abc");
        NameReference from = new NameReference(null, "tab", null);
        SelectBody selectBody = new SelectBody(Collections.singletonList(p), Collections.singletonList(from));
        SortKey s = new SortKey(new ColumnReference(null, null, "col"), SortDirection.DESC);
        selectBody.setOrderBy(new OrderBy(Collections.singletonList(s)));
        selectBody.setLimit(new Limit(new ConstExpression("3")));
        SelectBody second = getDefaultSelectSimple();
        selectBody.setRelatedSelect(new RelatedSelectBody(second, RelationType.UNION_DISTINCT));
        second.setRelatedSelect(new RelatedSelectBody(getDefaultSelectSimple(), RelationType.UNION_UNIQUE));
        Select expect = new Select(selectBody);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_multiUnionWithOrderByAndLimit_generateSelectSucceed() {
        Select_stmtContext context = getSelectContext(
                "select col.* abc from tab order by col desc limit 3 union select * from dual order by col1 desc limit 5 union unique select * from dual");
        StatementFactory<Select> factory = new MySQLSelectFactory(context);
        Select actual = factory.generate();

        ColumnReference r = new ColumnReference(null, "col", "*");
        Projection p = new Projection(r, "abc");
        NameReference from = new NameReference(null, "tab", null);
        SelectBody selectBody = new SelectBody(Collections.singletonList(p), Collections.singletonList(from));
        SortKey s = new SortKey(new ColumnReference(null, null, "col"), SortDirection.DESC);
        selectBody.setOrderBy(new OrderBy(Collections.singletonList(s)));
        selectBody.setLimit(new Limit(new ConstExpression("3")));
        SelectBody second = getDefaultSelectSimple();

        RelatedSelectBody related = new RelatedSelectBody(second, RelationType.UNION);
        s = new SortKey(new ColumnReference(null, null, "col1"), SortDirection.DESC);
        second.setOrderBy(new OrderBy(Collections.singletonList(s)));
        second.setLimit(new Limit(new ConstExpression("5")));
        selectBody.setRelatedSelect(related);
        second.setRelatedSelect(new RelatedSelectBody(getDefaultSelectSimple(), RelationType.UNION_UNIQUE));
        Select expect = new Select(selectBody);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_fromList_generateSelectSucceed() {
        Select_stmtContext context = getSelectContext("select col.* abc from a,(b),((c,d),e),f");
        StatementFactory<Select> factory = new MySQLSelectFactory(context);
        Select actual = factory.generate();

        ColumnReference r = new ColumnReference(null, "col", "*");
        Projection p = new Projection(r, "abc");
        NameReference n1 = new NameReference(null, "a", null);
        NameReference n2 = new NameReference(null, "b", null);
        NameReference n3 = new NameReference(null, "c", null);
        NameReference n4 = new NameReference(null, "d", null);
        NameReference n5 = new NameReference(null, "e", null);
        NameReference n6 = new NameReference(null, "f", null);
        Select expect = new Select(new SelectBody(Collections.singletonList(p), Arrays.asList(n1, n2, n3, n4, n5, n6)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_withNamedWindow_generateSelectSucceed() {
        Select_stmtContext context = getSelectContext(
                "select col.* abc from dual window name_w as (name_spec_1 partition by 1,2,3 order by col desc rows interval 1 day FOLLOWING), name_w2 as (range 1 PRECEDING)");
        StatementFactory<Select> factory = new MySQLSelectFactory(context);
        Select actual = factory.generate();

        ColumnReference r = new ColumnReference(null, "col", "*");
        Projection p = new Projection(r, "abc");
        NameReference from = new NameReference(null, "dual", null);
        SelectBody selectBody = new SelectBody(Collections.singletonList(p), Collections.singletonList(from));
        WindowSpec s1 = new WindowSpec();
        s1.setName("name_spec_1");
        s1.getPartitionBy().add(new ConstExpression("1"));
        s1.getPartitionBy().add(new ConstExpression("2"));
        s1.getPartitionBy().add(new ConstExpression("3"));
        SortKey sortKey = new SortKey(new ColumnReference(null, null, "col"), SortDirection.DESC);
        s1.setOrderBy(new OrderBy(Collections.singletonList(sortKey)));
        WindowOffset offset = new WindowOffset(WindowOffsetType.FOLLOWING);
        offset.setInterval(new IntervalExpression(new ConstExpression("1"), "day"));
        WindowBody windowBody = new WindowBody(WindowType.ROWS, offset);
        s1.setBody(windowBody);
        Window w1 = new Window("name_w", s1);

        WindowSpec s2 = new WindowSpec();
        offset = new WindowOffset(WindowOffsetType.PRECEDING);
        offset.setInterval(new ConstExpression("1"));
        windowBody = new WindowBody(WindowType.RANGE, offset);
        s2.setBody(windowBody);
        Window w2 = new Window("name_w2", s2);
        selectBody.getWindows().add(w1);
        selectBody.getWindows().add(w2);
        Select expect = new Select(selectBody);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_fromSelectStatment_generateSelectSucceed() {
        Select_stmtContext context =
                getSelectContext(
                        "select abc.* from lateral (select * from tab order by col1 desc) as of snapshot 1 abc");
        StatementFactory<Select> factory = new MySQLSelectFactory(context);
        Select actual = factory.generate();

        ColumnReference r = new ColumnReference(null, "abc", "*");
        Projection p = new Projection(r, null);
        SelectBody fromBody = new SelectBody(Collections.singletonList(
                new Projection()), Collections.singletonList(new NameReference(null, "tab", null)));
        OrderBy orderBy = new OrderBy(false, Collections.singletonList(
                new SortKey(new ColumnReference(null, null, "col1"), SortDirection.DESC, null)));
        fromBody.setOrderBy(orderBy);
        FlashbackUsage flashbackUsage = new FlashbackUsage(FlashBackType.AS_OF_SNAPSHOT, new ConstExpression("1"));
        ExpressionReference from = new ExpressionReference(fromBody, "abc");
        from.setLateral(true);
        from.setFlashbackUsage(flashbackUsage);
        Select expect = new Select(new SelectBody(Collections.singletonList(p), Collections.singletonList(from)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_whereClauseExists_generateSelectSucceed() {
        Select_stmtContext context = getSelectContext("select * from tab where tab.col='abcd'");
        StatementFactory<Select> factory = new MySQLSelectFactory(context);
        Select actual = factory.generate();

        Projection p = new Projection();
        NameReference from = new NameReference(null, "tab", null);
        SelectBody body = new SelectBody(Collections.singletonList(p), Collections.singletonList(from));
        Expression e1 = new ColumnReference(null, "tab", "col");
        Expression e2 = new ConstExpression("'abcd'");
        CompoundExpression where = new CompoundExpression(e1, e2, Operator.EQ);
        body.setWhere(where);
        Select expect = new Select(body);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_whereClauseAndGroupByClauseExists_generateSelectSucceed() {
        Select_stmtContext context =
                getSelectContext("select * from tab where tab.col='abcd' group by tab.col1 asc,tab2.col2 with rollup");
        StatementFactory<Select> factory = new MySQLSelectFactory(context);
        Select actual = factory.generate();

        Projection p = new Projection();
        NameReference from = new NameReference(null, "tab", null);
        SelectBody body = new SelectBody(Collections.singletonList(p), Collections.singletonList(from));
        Expression e1 = new ColumnReference(null, "tab", "col");
        Expression e2 = new ConstExpression("'abcd'");
        CompoundExpression where = new CompoundExpression(e1, e2, Operator.EQ);
        body.setWhere(where);
        SortKey g1 = new SortKey(new ColumnReference(null, "tab", "col1"), SortDirection.ASC);
        SortKey g2 = new SortKey(new ColumnReference(null, "tab2", "col2"), null);
        body.setGroupBy(Arrays.asList(g1, g2));
        body.setWithRollUp(true);
        Select expect = new Select(body);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_whereClauseGroupByClauseHavingClauseExists_generateSelectSucceed() {
        Select_stmtContext context =
                getSelectContext("select * from tab where tab.col='abcd' group by tab.col1 having tab.col3=123");
        StatementFactory<Select> factory = new MySQLSelectFactory(context);
        Select actual = factory.generate();

        Projection p = new Projection();
        NameReference from = new NameReference(null, "tab", null);
        SelectBody body = new SelectBody(Collections.singletonList(p), Collections.singletonList(from));
        Expression e1 = new ColumnReference(null, "tab", "col");
        Expression e2 = new ConstExpression("'abcd'");
        CompoundExpression where = new CompoundExpression(e1, e2, Operator.EQ);
        body.setWhere(where);
        body.getGroupBy().add(new SortKey(new ColumnReference(null, "tab", "col1"), null));
        Expression e3 = new ColumnReference(null, "tab", "col3");
        Expression e4 = new ConstExpression("123");
        CompoundExpression having = new CompoundExpression(e3, e4, Operator.EQ);
        body.setHaving(having);
        Select expect = new Select(body);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_withForUpdate_generateSelectSucceed() {
        Select_stmtContext context = getSelectContext("select * from dual for update nowait");
        StatementFactory<Select> factory = new MySQLSelectFactory(context);
        Select actual = factory.generate();

        Projection p = new Projection();
        NameReference from = new NameReference(null, "dual", null);
        SelectBody body = new SelectBody(Collections.singletonList(p), Collections.singletonList(from));
        body.setForUpdate(new ForUpdate(new ArrayList<>(), WaitOption.NOWAIT, null));
        Select expect = new Select(body);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_orderByLimitForUpdate_generateSelectSucceed() {
        String sql = "select all * from dual "
                + "where tab.col='abcd' "
                + "group by tab.col1 "
                + "having tab.col3=123 "
                + "order by col desc,col1 asc "
                + "limit 12 offset 67 "
                + "for update wait 12";
        Select_stmtContext context = getSelectContext(sql);
        StatementFactory<Select> factory = new MySQLSelectFactory(context);
        Select actual = factory.generate();

        Projection p = new Projection();
        NameReference from = new NameReference(null, "dual", null);
        SelectBody body = new SelectBody(Collections.singletonList(p), Collections.singletonList(from));
        body.setQueryOptions("all");
        Expression e1 = new ColumnReference(null, "tab", "col");
        Expression e2 = new ConstExpression("'abcd'");
        CompoundExpression where = new CompoundExpression(e1, e2, Operator.EQ);
        body.setWhere(where);
        body.getGroupBy().add(new SortKey(new ColumnReference(null, "tab", "col1"), null));
        Expression e3 = new ColumnReference(null, "tab", "col3");
        Expression e4 = new ConstExpression("123");
        CompoundExpression having = new CompoundExpression(e3, e4, Operator.EQ);
        body.setHaving(having);
        SortKey s1 = new SortKey(new ColumnReference(null, null, "col"), SortDirection.DESC, null);
        SortKey s2 = new SortKey(new ColumnReference(null, null, "col1"), SortDirection.ASC, null);
        OrderBy orderBy = new OrderBy(false, Arrays.asList(s1, s2));
        body.setOrderBy(orderBy);
        Limit limit = new Limit(new ConstExpression("12"));
        limit.setOffset(new ConstExpression("67"));
        body.setLimit(limit);
        body.setForUpdate(new ForUpdate(new ArrayList<>(), WaitOption.WAIT, BigDecimal.valueOf(12)));

        Select expect = new Select(body);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_unionOtherSelect_generateSelectSucceed() {
        String sql = "select all * from dual "
                + "where tab.col='abcd' "
                + "group by tab.col1 "
                + "having tab.col3=123 union all select col.* from tab "
                + "order by col desc,col1 asc "
                + "limit 12 offset 67 "
                + "for update wait 12";
        Select_stmtContext context = getSelectContext(sql);
        StatementFactory<Select> factory = new MySQLSelectFactory(context);
        Select actual = factory.generate();

        Projection p = new Projection();
        NameReference from = new NameReference(null, "dual", null);
        SelectBody body = new SelectBody(Collections.singletonList(p), Collections.singletonList(from));
        body.setQueryOptions("all");
        Expression e1 = new ColumnReference(null, "tab", "col");
        Expression e2 = new ConstExpression("'abcd'");
        CompoundExpression where = new CompoundExpression(e1, e2, Operator.EQ);
        body.setWhere(where);
        body.getGroupBy().add(new SortKey(new ColumnReference(null, "tab", "col1"), null));
        Expression e3 = new ColumnReference(null, "tab", "col3");
        Expression e4 = new ConstExpression("123");
        CompoundExpression having = new CompoundExpression(e3, e4, Operator.EQ);
        body.setHaving(having);

        Projection p1 = new Projection(new ColumnReference(null, "col", "*"), null);
        NameReference from1 = new NameReference(null, "tab", null);
        SelectBody other = new SelectBody(Collections.singletonList(p1), Collections.singletonList(from1));
        RelatedSelectBody selectBody = new RelatedSelectBody(other, RelationType.UNION_ALL);
        SortKey s1 = new SortKey(new ColumnReference(null, null, "col"), SortDirection.DESC, null);
        SortKey s2 = new SortKey(new ColumnReference(null, null, "col1"), SortDirection.ASC, null);
        OrderBy orderBy = new OrderBy(false, Arrays.asList(s1, s2));
        other.setOrderBy(orderBy);
        Limit limit = new Limit(new ConstExpression("12"));
        limit.setOffset(new ConstExpression("67"));
        other.setLimit(limit);
        other.setForUpdate(new ForUpdate(new ArrayList<>(), WaitOption.WAIT, BigDecimal.valueOf(12)));
        body.setRelatedSelect(selectBody);

        Select expect = new Select(body);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_onlyHaving_generateSelectSucceed() {
        String sql = "select all * from dual having tab.col3=123 lock in share mode";
        Select_stmtContext context = getSelectContext(sql);
        StatementFactory<Select> factory = new MySQLSelectFactory(context);
        Select actual = factory.generate();

        Projection p = new Projection();
        NameReference from = new NameReference(null, "dual", null);
        SelectBody body = new SelectBody(Collections.singletonList(p), Collections.singletonList(from));
        body.setQueryOptions("all");
        body.setLockInShareMode(true);
        Expression e3 = new ColumnReference(null, "tab", "col3");
        Expression e4 = new ConstExpression("123");
        CompoundExpression having = new CompoundExpression(e3, e4, Operator.EQ);
        body.setHaving(having);
        Select expect = new Select(body);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_valuesStatement_generateSelectSucceed() {
        String sql = "values row(1, '2'), row(2, '3')";
        Select_stmtContext context = getSelectContext(sql);
        StatementFactory<Select> factory = new MySQLSelectFactory(context);
        Select actual = factory.generate();

        List<List<Expression>> values = new ArrayList<>();
        values.add(Arrays.asList(new ConstExpression("1"), new ConstExpression("'2'")));
        values.add(Arrays.asList(new ConstExpression("2"), new ConstExpression("'3'")));
        SelectBody body = new SelectBody(values);
        Select expect = new Select(body);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_valuesStatementOrderByLimit_generateSelectSucceed() {
        String sql = "values row(1, '2'), row(2, '3') order by 1 desc limit 3";
        Select_stmtContext context = getSelectContext(sql);
        StatementFactory<Select> factory = new MySQLSelectFactory(context);
        Select actual = factory.generate();

        List<List<Expression>> values = new ArrayList<>();
        values.add(Arrays.asList(new ConstExpression("1"), new ConstExpression("'2'")));
        values.add(Arrays.asList(new ConstExpression("2"), new ConstExpression("'3'")));
        SelectBody body = new SelectBody(values);
        SortKey s1 = new SortKey(new ConstExpression("1"), SortDirection.DESC, null);
        OrderBy orderBy = new OrderBy(false, Collections.singletonList(s1));
        body.setOrderBy(orderBy);
        Limit limit = new Limit(new ConstExpression("3"));
        body.setLimit(limit);
        Select expect = new Select(body);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_valuesStatementUnion_generateSelectSucceed() {
        String sql = "values row(1, '2'), row(2, '3') union values row(1, '2')";
        Select_stmtContext context = getSelectContext(sql);
        StatementFactory<Select> factory = new MySQLSelectFactory(context);
        Select actual = factory.generate();

        List<List<Expression>> values = new ArrayList<>();
        values.add(Arrays.asList(new ConstExpression("1"), new ConstExpression("'2'")));
        values.add(Arrays.asList(new ConstExpression("2"), new ConstExpression("'3'")));
        SelectBody body = new SelectBody(values);

        values = new ArrayList<>();
        values.add(Arrays.asList(new ConstExpression("1"), new ConstExpression("'2'")));
        SelectBody second = new SelectBody(values);
        body.setRelatedSelect(new RelatedSelectBody(second, RelationType.UNION));
        Select expect = new Select(body);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_withParentSelect_generateSelectSucceed() {
        String sql = "(select all * from dual "
                + "where tab.col='abcd' "
                + "group by tab.col1 "
                + "having tab.col3=123 INTERSECT select col.* from tab order by col4 desc limit 12 offset 67)"
                + "order by col desc,col1 asc "
                + "limit 34 offset 67 "
                + "for update wait 12";
        Select_stmtContext context = getSelectContext(sql);
        StatementFactory<Select> factory = new MySQLSelectFactory(context);
        Select actual = factory.generate();

        Projection p = new Projection();
        NameReference from = new NameReference(null, "dual", null);
        SelectBody body = new SelectBody(Collections.singletonList(p), Collections.singletonList(from));
        body.setQueryOptions("all");
        Expression e1 = new ColumnReference(null, "tab", "col");
        Expression e2 = new ConstExpression("'abcd'");
        CompoundExpression where = new CompoundExpression(e1, e2, Operator.EQ);
        body.setWhere(where);
        body.getGroupBy().add(new SortKey(new ColumnReference(null, "tab", "col1"), null));
        Expression e3 = new ColumnReference(null, "tab", "col3");
        Expression e4 = new ConstExpression("123");
        CompoundExpression having = new CompoundExpression(e3, e4, Operator.EQ);
        body.setHaving(having);

        Projection p1 = new Projection(new ColumnReference(null, "col", "*"), null);
        NameReference from1 = new NameReference(null, "tab", null);
        SelectBody other = new SelectBody(Collections.singletonList(p1), Collections.singletonList(from1));
        RelatedSelectBody selectBody = new RelatedSelectBody(other, RelationType.INTERSECT);
        SortKey s1 = new SortKey(new ColumnReference(null, null, "col"), SortDirection.DESC, null);
        SortKey s2 = new SortKey(new ColumnReference(null, null, "col1"), SortDirection.ASC, null);
        OrderBy orderBy = new OrderBy(false, Arrays.asList(s1, s2));
        other.setOrderBy(orderBy);
        Limit limit = new Limit(new ConstExpression("34"));
        limit.setOffset(new ConstExpression("67"));
        other.setLimit(limit);
        other.setForUpdate(new ForUpdate(new ArrayList<>(), WaitOption.WAIT, BigDecimal.valueOf(12)));
        body.setRelatedSelect(selectBody);

        Select expect = new Select(body);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_selectWithClause_generateSelectSucceed() {
        String sql =
                "with RECURSIVE w1 (w1_col1,w1_col2) as (select * from dual order by col5 desc limit 4), "
                        + "w2 (w2_col1,w2_col2) as (select * from dual),"
                        + "w3 (w3_col1,w3_col2) as (select * from dual) "
                        + "(select all * from dual "
                        + "where tab.col='abcd' "
                        + "group by tab.col1 "
                        + "having tab.col3=123 union all (select col.* from tab order by col4 desc limit 12 offset 56))"
                        + "order by col desc,col1 asc "
                        + "for update wait 12";
        Select_stmtContext context = getSelectContext(sql);
        StatementFactory<Select> factory = new MySQLSelectFactory(context);
        Select actual = factory.generate();

        Projection p = new Projection();
        NameReference from = new NameReference(null, "dual", null);
        SelectBody body = new SelectBody(Collections.singletonList(p), Collections.singletonList(from));
        body.setQueryOptions("all");
        Expression e1 = new ColumnReference(null, "tab", "col");
        Expression e2 = new ConstExpression("'abcd'");
        CompoundExpression where = new CompoundExpression(e1, e2, Operator.EQ);
        body.setWhere(where);
        body.getGroupBy().add(new SortKey(new ColumnReference(null, "tab", "col1"), null));
        Expression e3 = new ColumnReference(null, "tab", "col3");
        Expression e4 = new ConstExpression("123");
        CompoundExpression having = new CompoundExpression(e3, e4, Operator.EQ);
        body.setHaving(having);

        WithTable withTable = new WithTable("w1", getDefaultSelect());
        withTable.setAliasList(Arrays.asList("w1_col1", "w1_col2"));
        WithTable withTable1 = new WithTable("w2", getDefaultSelectSimple());
        withTable1.setAliasList(Arrays.asList("w2_col1", "w2_col2"));
        WithTable withTable2 = new WithTable("w3", getDefaultSelectSimple());
        withTable2.setAliasList(Arrays.asList("w3_col1", "w3_col2"));
        body.setWith(Arrays.asList(withTable, withTable1, withTable2));
        body.setRecursive(true);

        Projection p1 = new Projection(new ColumnReference(null, "col", "*"), null);
        NameReference from1 = new NameReference(null, "tab", null);
        SelectBody other = new SelectBody(Collections.singletonList(p1), Collections.singletonList(from1));
        SortKey s0 = new SortKey(new ColumnReference(null, null, "col4"), SortDirection.DESC, null);
        other.setOrderBy(new OrderBy(false, Collections.singletonList(s0)));
        Limit limit = new Limit(new ConstExpression("12"));
        limit.setOffset(new ConstExpression("56"));
        other.setLimit(limit);
        RelatedSelectBody selectBody = new RelatedSelectBody(other, RelationType.UNION_ALL);
        SortKey s1 = new SortKey(new ColumnReference(null, null, "col"), SortDirection.DESC, null);
        SortKey s2 = new SortKey(new ColumnReference(null, null, "col1"), SortDirection.ASC, null);
        OrderBy orderBy = new OrderBy(false, Arrays.asList(s1, s2));
        other.setOrderBy(orderBy);
        other.setForUpdate(new ForUpdate(new ArrayList<>(), WaitOption.WAIT, BigDecimal.valueOf(12)));
        body.setRelatedSelect(selectBody);

        Select expect = new Select(body);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_parentSelectWithClause_generateSelectSucceed() {
        String sql =
                "(with RECURSIVE w1 (w1_col1,w1_col2) as (select * from dual order by col5 desc limit 4), "
                        + "w2 (w2_col1,w2_col2) as (select * from dual),"
                        + "w3 (w3_col1,w3_col2) as (select * from dual) "
                        + "(select all * from dual "
                        + "where tab.col='abcd' "
                        + "group by tab.col1 "
                        + "having tab.col3=123 union all (select col.* from tab order by col4 desc limit 12 offset 56))"
                        + "order by col desc,col1 asc "
                        + "for update wait 12)";
        Select_stmtContext context = getSelectContext(sql);
        StatementFactory<Select> factory = new MySQLSelectFactory(context);
        Select actual = factory.generate();

        Projection p = new Projection();
        NameReference from = new NameReference(null, "dual", null);
        SelectBody body = new SelectBody(Collections.singletonList(p), Collections.singletonList(from));
        body.setQueryOptions("all");
        Expression e1 = new ColumnReference(null, "tab", "col");
        Expression e2 = new ConstExpression("'abcd'");
        CompoundExpression where = new CompoundExpression(e1, e2, Operator.EQ);
        body.setWhere(where);
        body.getGroupBy().add(new SortKey(new ColumnReference(null, "tab", "col1"), null));
        Expression e3 = new ColumnReference(null, "tab", "col3");
        Expression e4 = new ConstExpression("123");
        CompoundExpression having = new CompoundExpression(e3, e4, Operator.EQ);
        body.setHaving(having);

        WithTable withTable = new WithTable("w1", getDefaultSelect());
        withTable.setAliasList(Arrays.asList("w1_col1", "w1_col2"));
        WithTable withTable1 = new WithTable("w2", getDefaultSelectSimple());
        withTable1.setAliasList(Arrays.asList("w2_col1", "w2_col2"));
        WithTable withTable2 = new WithTable("w3", getDefaultSelectSimple());
        withTable2.setAliasList(Arrays.asList("w3_col1", "w3_col2"));
        body.setWith(Arrays.asList(withTable, withTable1, withTable2));
        body.setRecursive(true);

        Projection p1 = new Projection(new ColumnReference(null, "col", "*"), null);
        NameReference from1 = new NameReference(null, "tab", null);
        SelectBody other = new SelectBody(Collections.singletonList(p1), Collections.singletonList(from1));
        SortKey s0 = new SortKey(new ColumnReference(null, null, "col4"), SortDirection.DESC, null);
        other.setOrderBy(new OrderBy(false, Collections.singletonList(s0)));
        Limit limit = new Limit(new ConstExpression("12"));
        limit.setOffset(new ConstExpression("56"));
        other.setLimit(limit);
        RelatedSelectBody selectBody = new RelatedSelectBody(other, RelationType.UNION_ALL);
        SortKey s1 = new SortKey(new ColumnReference(null, null, "col"), SortDirection.DESC, null);
        SortKey s2 = new SortKey(new ColumnReference(null, null, "col1"), SortDirection.ASC, null);
        OrderBy orderBy = new OrderBy(false, Arrays.asList(s1, s2));
        other.setOrderBy(orderBy);
        other.setForUpdate(new ForUpdate(new ArrayList<>(), WaitOption.WAIT, BigDecimal.valueOf(12)));
        body.setRelatedSelect(selectBody);

        Select expect = new Select(body);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_SelectArray_Succeed() {
        Select_stmtContext context = getSelectContext("select [[1,2],[3,4],[5,6]];");
        StatementFactory<Select> factory = new MySQLSelectFactory(context);
        Select actual = factory.generate();

        ArrayExpression expression1 =
                new ArrayExpression(Arrays.asList(new ConstExpression("1"), new ConstExpression("2")));
        ArrayExpression expression2 =
                new ArrayExpression(Arrays.asList(new ConstExpression("3"), new ConstExpression("4")));
        ArrayExpression expression3 =
                new ArrayExpression(Arrays.asList(new ConstExpression("5"), new ConstExpression("6")));

        ArrayExpression column = new ArrayExpression(Arrays.asList(expression1, expression2, expression3));
        Projection projection = new Projection(column, null);
        SelectBody body = new SelectBody(Collections.singletonList(projection), Collections.emptyList());
        Select expected = new Select(body);
        Assert.assertEquals(expected, actual);

    }

    private SelectBody getDefaultSelect() {
        Projection p = new Projection();
        NameReference r = new NameReference(null, "dual", null);
        SelectBody body = new SelectBody(Collections.singletonList(p), Collections.singletonList(r));
        SortKey s1 = new SortKey(new ColumnReference(null, null, "col5"), SortDirection.DESC, null);
        OrderBy orderBy = new OrderBy(false, Collections.singletonList(s1));
        body.setOrderBy(orderBy);
        Limit limit = new Limit(new ConstExpression("4"));
        body.setLimit(limit);
        return body;
    }

    private SelectBody getDefaultSelectSimple() {
        Projection p = new Projection();
        NameReference r = new NameReference(null, "dual", null);
        return new SelectBody(Collections.singletonList(p), Collections.singletonList(r));
    }

    private Select_stmtContext getSelectContext(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.select_stmt();
    }
}
