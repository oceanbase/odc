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
import java.util.Arrays;
import java.util.Collections;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.sqlparser.adapter.oracle.OracleSelectFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBLexer;
import com.oceanbase.tools.sqlparser.oboracle.OBParser;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Select_stmtContext;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.Operator;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.expression.CompoundExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;
import com.oceanbase.tools.sqlparser.statement.expression.RelationReference;
import com.oceanbase.tools.sqlparser.statement.select.ExpressionReference;
import com.oceanbase.tools.sqlparser.statement.select.FlashBackType;
import com.oceanbase.tools.sqlparser.statement.select.FlashbackUsage;
import com.oceanbase.tools.sqlparser.statement.select.ForUpdate;
import com.oceanbase.tools.sqlparser.statement.select.GroupBy;
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
import com.oceanbase.tools.sqlparser.statement.select.oracle.Fetch;
import com.oceanbase.tools.sqlparser.statement.select.oracle.FetchAddition;
import com.oceanbase.tools.sqlparser.statement.select.oracle.FetchDirection;
import com.oceanbase.tools.sqlparser.statement.select.oracle.FetchType;
import com.oceanbase.tools.sqlparser.statement.select.oracle.GeneralGroupBy;
import com.oceanbase.tools.sqlparser.statement.select.oracle.GroupingSetsGroupBy;
import com.oceanbase.tools.sqlparser.statement.select.oracle.SearchMode;
import com.oceanbase.tools.sqlparser.statement.select.oracle.SetValue;

/**
 * {@link OracleSelectFactoryTest}
 *
 * @author yh263208
 * @date 2022-12-07 12:24
 * @since ODC_release_4.1.0
 */
public class OracleSelectFactoryTest {

    @Test
    public void generate_onlyIncludeSelectItemsAndWheres_generateSelectSucceed() {
        Select_stmtContext context = getSelectContext("select col.* abc from dual");
        StatementFactory<Select> factory = new OracleSelectFactory(context);
        Select actual = factory.generate();

        RelationReference r = new RelationReference("col", new RelationReference("*", null));
        Projection p = new Projection(r, "abc");
        NameReference from = new NameReference(null, "dual", null);
        Select expect = new Select(new SelectBody(Collections.singletonList(p), Collections.singletonList(from)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_fromSelectStatment_generateSelectSucceed() {
        Select_stmtContext context =
                getSelectContext("select abc.* from (select * from tab order by col1 desc) as of scn 1 abc");
        StatementFactory<Select> factory = new OracleSelectFactory(context);
        Select actual = factory.generate();

        RelationReference r = new RelationReference("abc", new RelationReference("*", null));
        Projection p = new Projection(r, null);
        SelectBody fromBody = new SelectBody(Collections.singletonList(
                new Projection()), Collections.singletonList(new NameReference(null, "tab", null)));
        OrderBy orderBy = new OrderBy(false, Collections.singletonList(
                new SortKey(new RelationReference("col1", null), SortDirection.DESC, null)));
        fromBody.setOrderBy(orderBy);
        FlashbackUsage flashbackUsage = new FlashbackUsage(FlashBackType.AS_OF_SCN, new ConstExpression("1"));
        ExpressionReference from = new ExpressionReference(fromBody, "abc");
        from.setFlashbackUsage(flashbackUsage);
        Select expect = new Select(new SelectBody(Collections.singletonList(p), Collections.singletonList(from)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_whereClauseExists_generateSelectSucceed() {
        Select_stmtContext context = getSelectContext("select * from tab where tab.col='abcd'");
        StatementFactory<Select> factory = new OracleSelectFactory(context);
        Select actual = factory.generate();

        Projection p = new Projection();
        NameReference from = new NameReference(null, "tab", null);
        SelectBody body = new SelectBody(Collections.singletonList(p), Collections.singletonList(from));
        Expression e1 = new RelationReference("tab", new RelationReference("col", null));
        Expression e2 = new ConstExpression("'abcd'");
        CompoundExpression where = new CompoundExpression(e1, e2, Operator.EQ);
        body.setWhere(where);
        Select expect = new Select(body);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_whereClauseAndGroupByClauseExists_generateSelectSucceed() {
        Select_stmtContext context = getSelectContext(
                "select * from tab where tab.col='abcd' group by tab.col1,grouping sets(tab.col2)");
        StatementFactory<Select> factory = new OracleSelectFactory(context);
        Select actual = factory.generate();

        Projection p = new Projection();
        NameReference from = new NameReference(null, "tab", null);
        SelectBody body = new SelectBody(Collections.singletonList(p), Collections.singletonList(from));
        Expression e1 = new RelationReference("tab", new RelationReference("col", null));
        Expression e2 = new ConstExpression("'abcd'");
        CompoundExpression where = new CompoundExpression(e1, e2, Operator.EQ);
        body.setWhere(where);
        GroupBy g1 = new GeneralGroupBy(new RelationReference("tab", new RelationReference("col1", null)));
        GroupBy g2 = new GeneralGroupBy(new RelationReference("tab", new RelationReference("col2", null)));
        GroupBy g3 = new GroupingSetsGroupBy(Collections.singletonList(g2));
        body.setGroupBy(Arrays.asList(g1, g3));
        Select expect = new Select(body);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_whereClauseGroupByClauseHavingClauseExists_generateSelectSucceed() {
        Select_stmtContext context = getSelectContext(
                "select * from tab where tab.col='abcd' group by tab.col1,grouping sets(tab.col2) having tab.col3=123");
        StatementFactory<Select> factory = new OracleSelectFactory(context);
        Select actual = factory.generate();

        Projection p = new Projection();
        NameReference from = new NameReference(null, "tab", null);
        SelectBody body = new SelectBody(Collections.singletonList(p), Collections.singletonList(from));
        Expression e1 = new RelationReference("tab", new RelationReference("col", null));
        Expression e2 = new ConstExpression("'abcd'");
        CompoundExpression where = new CompoundExpression(e1, e2, Operator.EQ);
        body.setWhere(where);
        GroupBy g1 = new GeneralGroupBy(new RelationReference("tab", new RelationReference("col1", null)));
        GroupBy g2 = new GeneralGroupBy(new RelationReference("tab", new RelationReference("col2", null)));
        GroupBy g3 = new GroupingSetsGroupBy(Collections.singletonList(g2));
        body.setGroupBy(Arrays.asList(g1, g3));
        Expression e3 = new RelationReference("tab", new RelationReference("col3", null));
        Expression e4 = new ConstExpression("123");
        CompoundExpression having = new CompoundExpression(e3, e4, Operator.EQ);
        body.setHaving(having);
        Select expect = new Select(body);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_withStartWith_generateSelectSucceed() {
        Select_stmtContext context = getSelectContext("select 1 from dual start with @abc:=1+3 connect by col");
        StatementFactory<Select> factory = new OracleSelectFactory(context);
        Select actual = factory.generate();

        Projection p = new Projection(new ConstExpression("1"), null);
        NameReference from = new NameReference(null, "dual", null);
        SelectBody body = new SelectBody(Collections.singletonList(p), Collections.singletonList(from));
        Expression right = new CompoundExpression(new ConstExpression("1"), new ConstExpression("3"), Operator.ADD);
        Expression sw = new CompoundExpression(new ConstExpression("@abc"), right, Operator.SET_VAR);
        body.setStartWith(sw);
        Expression cb = new RelationReference("col", null);
        body.setConnectBy(cb);
        Select expect = new Select(body);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_withForUpdate_generateSelectSucceed() {
        Select_stmtContext context = getSelectContext("select * from dual for update of col1, col2 nowait");
        StatementFactory<Select> factory = new OracleSelectFactory(context);
        Select actual = factory.generate();

        Projection p = new Projection();
        NameReference from = new NameReference(null, "dual", null);
        Select expect = new Select(new SelectBody(Collections.singletonList(p), Collections.singletonList(from)));
        ColumnReference c1 = new ColumnReference(null, null, "col1");
        ColumnReference c2 = new ColumnReference(null, null, "col2");
        expect.setForUpdate(new ForUpdate(Arrays.asList(c1, c2), WaitOption.NOWAIT, null));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_orderByFetchNextForUpdate_generateSelectSucceed() {
        String sql = "select all * from dual "
                + "where tab.col='abcd' "
                + "start with @abc:=1+3 "
                + "connect by col "
                + "group by tab.col1,grouping sets(tab.col2) "
                + "having tab.col3=123 "
                + "order by col desc,col1 asc "
                + "offset 12 rows fetch first 12 rows only "
                + "for update of col1, col2 wait 12";
        Select_stmtContext context = getSelectContext(sql);
        StatementFactory<Select> factory = new OracleSelectFactory(context);
        Select actual = factory.generate();

        Projection p = new Projection();
        NameReference from = new NameReference(null, "dual", null);
        SelectBody body = new SelectBody(Collections.singletonList(p), Collections.singletonList(from));
        body.setQueryOptions("all");
        Expression e1 = new RelationReference("tab", new RelationReference("col", null));
        Expression e2 = new ConstExpression("'abcd'");
        CompoundExpression where = new CompoundExpression(e1, e2, Operator.EQ);
        body.setWhere(where);
        Expression right = new CompoundExpression(new ConstExpression("1"), new ConstExpression("3"), Operator.ADD);
        Expression sw = new CompoundExpression(new ConstExpression("@abc"), right, Operator.SET_VAR);
        body.setStartWith(sw);
        Expression cb = new RelationReference("col", null);
        body.setConnectBy(cb);
        GroupBy g1 = new GeneralGroupBy(new RelationReference("tab", new RelationReference("col1", null)));
        GroupBy g2 = new GeneralGroupBy(new RelationReference("tab", new RelationReference("col2", null)));
        GroupBy g3 = new GroupingSetsGroupBy(Collections.singletonList(g2));
        body.setGroupBy(Arrays.asList(g1, g3));
        Expression e3 = new RelationReference("tab", new RelationReference("col3", null));
        Expression e4 = new ConstExpression("123");
        CompoundExpression having = new CompoundExpression(e3, e4, Operator.EQ);
        body.setHaving(having);

        Select expect = new Select(body);
        SortKey s1 = new SortKey(new RelationReference("col", null), SortDirection.DESC, null);
        SortKey s2 = new SortKey(new RelationReference("col1", null), SortDirection.ASC, null);
        OrderBy orderBy = new OrderBy(false, Arrays.asList(s1, s2));
        expect.setOrderBy(orderBy);
        Fetch fetch = new Fetch(new ConstExpression("12"), FetchDirection.FIRST, FetchType.COUNT, FetchAddition.ONLY,
                new ConstExpression("12"));
        expect.setFetch(fetch);
        ColumnReference c1 = new ColumnReference(null, null, "col1");
        ColumnReference c2 = new ColumnReference(null, null, "col2");
        expect.setForUpdate(new ForUpdate(Arrays.asList(c1, c2), WaitOption.WAIT, BigDecimal.valueOf(12)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_unionOtherSelect_generateSelectSucceed() {
        String sql = "select all * from dual "
                + "where tab.col='abcd' "
                + "start with @abc:=1+3 "
                + "connect by col "
                + "group by tab.col1,grouping sets(tab.col2) "
                + "having tab.col3=123 union all select col.* from tab "
                + "order by col desc,col1 asc "
                + "offset 12 rows fetch first 12 rows only "
                + "for update of col1, col2 wait 12";
        Select_stmtContext context = getSelectContext(sql);
        StatementFactory<Select> factory = new OracleSelectFactory(context);
        Select actual = factory.generate();

        Projection p = new Projection();
        NameReference from = new NameReference(null, "dual", null);
        SelectBody body = new SelectBody(Collections.singletonList(p), Collections.singletonList(from));
        body.setQueryOptions("all");
        Expression e1 = new RelationReference("tab", new RelationReference("col", null));
        Expression e2 = new ConstExpression("'abcd'");
        CompoundExpression where = new CompoundExpression(e1, e2, Operator.EQ);
        body.setWhere(where);
        Expression right = new CompoundExpression(new ConstExpression("1"), new ConstExpression("3"), Operator.ADD);
        Expression sw = new CompoundExpression(new ConstExpression("@abc"), right, Operator.SET_VAR);
        body.setStartWith(sw);
        Expression cb = new RelationReference("col", null);
        body.setConnectBy(cb);
        GroupBy g1 = new GeneralGroupBy(new RelationReference("tab", new RelationReference("col1", null)));
        GroupBy g2 = new GeneralGroupBy(new RelationReference("tab", new RelationReference("col2", null)));
        GroupBy g3 = new GroupingSetsGroupBy(Collections.singletonList(g2));
        body.setGroupBy(Arrays.asList(g1, g3));
        Expression e3 = new RelationReference("tab", new RelationReference("col3", null));
        Expression e4 = new ConstExpression("123");
        CompoundExpression having = new CompoundExpression(e3, e4, Operator.EQ);
        body.setHaving(having);
        Projection pp = new Projection(new RelationReference("col", new RelationReference("*", null)), null);
        NameReference f1 = new NameReference(null, "tab", null);
        SelectBody related = new SelectBody(Collections.singletonList(pp), Collections.singletonList(f1));
        body.setRelatedSelect(new RelatedSelectBody(related, RelationType.UNION_ALL));

        Select expect = new Select(body);
        SortKey s1 = new SortKey(new RelationReference("col", null), SortDirection.DESC, null);
        SortKey s2 = new SortKey(new RelationReference("col1", null), SortDirection.ASC, null);
        OrderBy orderBy = new OrderBy(false, Arrays.asList(s1, s2));
        expect.setOrderBy(orderBy);
        Fetch fetch = new Fetch(new ConstExpression("12"), FetchDirection.FIRST, FetchType.COUNT, FetchAddition.ONLY,
                new ConstExpression("12"));
        expect.setFetch(fetch);
        ColumnReference c1 = new ColumnReference(null, null, "col1");
        ColumnReference c2 = new ColumnReference(null, null, "col2");
        expect.setForUpdate(new ForUpdate(Arrays.asList(c1, c2), WaitOption.WAIT, BigDecimal.valueOf(12)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_withParentSelect_generateSelectSucceed() {
        String sql = "(select all * from dual "
                + "where tab.col='abcd' "
                + "start with @abc:=1+3 "
                + "connect by col "
                + "group by tab.col1,grouping sets(tab.col2) "
                + "having tab.col3=123 union all select col.* from tab order by col4 desc offset 12 rows fetch first 12 percent rows with ties)"
                + "order by col desc,col1 asc "
                + "offset 12 rows fetch first 12 rows only "
                + "for update of col1, col2 wait 12";
        Select_stmtContext context = getSelectContext(sql);
        StatementFactory<Select> factory = new OracleSelectFactory(context);
        Select actual = factory.generate();

        Projection p = new Projection();
        NameReference from = new NameReference(null, "dual", null);
        SelectBody body = new SelectBody(Collections.singletonList(p), Collections.singletonList(from));
        body.setQueryOptions("all");
        Expression e1 = new RelationReference("tab", new RelationReference("col", null));
        Expression e2 = new ConstExpression("'abcd'");
        CompoundExpression where = new CompoundExpression(e1, e2, Operator.EQ);
        body.setWhere(where);
        Expression right = new CompoundExpression(new ConstExpression("1"), new ConstExpression("3"), Operator.ADD);
        Expression sw = new CompoundExpression(new ConstExpression("@abc"), right, Operator.SET_VAR);
        body.setStartWith(sw);
        Expression cb = new RelationReference("col", null);
        body.setConnectBy(cb);
        GroupBy g1 = new GeneralGroupBy(new RelationReference("tab", new RelationReference("col1", null)));
        GroupBy g2 = new GeneralGroupBy(new RelationReference("tab", new RelationReference("col2", null)));
        GroupBy g3 = new GroupingSetsGroupBy(Collections.singletonList(g2));
        body.setGroupBy(Arrays.asList(g1, g3));
        Expression e3 = new RelationReference("tab", new RelationReference("col3", null));
        Expression e4 = new ConstExpression("123");
        CompoundExpression having = new CompoundExpression(e3, e4, Operator.EQ);
        body.setHaving(having);
        Projection pp = new Projection(new RelationReference("col", new RelationReference("*", null)), null);
        NameReference f1 = new NameReference(null, "tab", null);
        SelectBody related = new SelectBody(Collections.singletonList(pp), Collections.singletonList(f1));
        SortKey s = new SortKey(new RelationReference("col4", null), SortDirection.DESC, null);
        RelatedSelectBody body1 = new RelatedSelectBody(related, RelationType.UNION_ALL);
        body1.setFetch(new Fetch(new ConstExpression("12"), FetchDirection.FIRST, FetchType.PERCENT,
                FetchAddition.WITH_TIES, new ConstExpression("12")));
        body1.setOrderBy(new OrderBy(false, Collections.singletonList(s)));
        body.setRelatedSelect(body1);

        Select expect = new Select(body);
        SortKey s1 = new SortKey(new RelationReference("col", null), SortDirection.DESC, null);
        SortKey s2 = new SortKey(new RelationReference("col1", null), SortDirection.ASC, null);
        OrderBy orderBy = new OrderBy(false, Arrays.asList(s1, s2));
        expect.setOrderBy(orderBy);
        Fetch fetch = new Fetch(new ConstExpression("12"), FetchDirection.FIRST, FetchType.COUNT, FetchAddition.ONLY,
                new ConstExpression("12"));
        expect.setFetch(fetch);
        ColumnReference c1 = new ColumnReference(null, null, "col1");
        ColumnReference c2 = new ColumnReference(null, null, "col2");
        expect.setForUpdate(new ForUpdate(Arrays.asList(c1, c2), WaitOption.WAIT, BigDecimal.valueOf(12)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_selectWithClause_generateSelectSucceed() {
        String sql =
                "with relation_name (col1,col2) as (select * from dual order by col5 desc fetch first 21 percent rows with ties) "
                        + "search depth first by col1 desc set search_var_name "
                        + "cycle col2, col3 set cycle_name to '123' default '432', "
                        + "relation_name (col1,col2) as (select * from dual) "
                        + "search depth first by col1 desc set search_var_name, "
                        + "relation_name (col1,col2) as (select * from dual) "
                        + "cycle col2, col3 set cycle_name to '123' default '432'"
                        + "(select all * from dual "
                        + "where tab.col='abcd' "
                        + "start with @abc:=1+3 "
                        + "connect by col "
                        + "group by tab.col1,grouping sets(tab.col2) "
                        + "having tab.col3=123 union all select col.* from tab order by col4 desc offset 12 rows fetch first 12 percent rows with ties)"
                        + "order by col desc,col1 asc "
                        + "offset 12 rows fetch first 12 rows only "
                        + "for update of col1, col2 wait 12";
        Select_stmtContext context = getSelectContext(sql);
        StatementFactory<Select> factory = new OracleSelectFactory(context);
        Select actual = factory.generate();

        Projection p = new Projection();
        NameReference from = new NameReference(null, "dual", null);
        SelectBody body = new SelectBody(Collections.singletonList(p), Collections.singletonList(from));
        body.setQueryOptions("all");
        Expression e1 = new RelationReference("tab", new RelationReference("col", null));
        Expression e2 = new ConstExpression("'abcd'");
        CompoundExpression where = new CompoundExpression(e1, e2, Operator.EQ);
        body.setWhere(where);
        Expression right = new CompoundExpression(new ConstExpression("1"), new ConstExpression("3"), Operator.ADD);
        Expression sw = new CompoundExpression(new ConstExpression("@abc"), right, Operator.SET_VAR);
        body.setStartWith(sw);
        Expression cb = new RelationReference("col", null);
        body.setConnectBy(cb);
        GroupBy g1 = new GeneralGroupBy(new RelationReference("tab", new RelationReference("col1", null)));
        GroupBy g2 = new GeneralGroupBy(new RelationReference("tab", new RelationReference("col2", null)));
        GroupBy g3 = new GroupingSetsGroupBy(Collections.singletonList(g2));
        body.setGroupBy(Arrays.asList(g1, g3));
        Expression e3 = new RelationReference("tab", new RelationReference("col3", null));
        Expression e4 = new ConstExpression("123");
        CompoundExpression having = new CompoundExpression(e3, e4, Operator.EQ);
        body.setHaving(having);
        Projection pp = new Projection(new RelationReference("col", new RelationReference("*", null)), null);
        NameReference f1 = new NameReference(null, "tab", null);
        SelectBody related = new SelectBody(Collections.singletonList(pp), Collections.singletonList(f1));
        SortKey s = new SortKey(new RelationReference("col4", null), SortDirection.DESC, null);
        RelatedSelectBody selectBody = new RelatedSelectBody(related, RelationType.UNION_ALL);
        selectBody.setFetch(new Fetch(new ConstExpression("12"), FetchDirection.FIRST, FetchType.PERCENT,
                FetchAddition.WITH_TIES, new ConstExpression("12")));
        selectBody.setOrderBy(new OrderBy(false, Collections.singletonList(s)));
        body.setRelatedSelect(selectBody);
        WithTable withTable = new WithTable("relation_name", getDefaultSelect());
        withTable.setAliasList(Arrays.asList("col1", "col2"));
        withTable.setSearchMode(SearchMode.DEPTH_FIRST);
        SortKey s0 = new SortKey(new RelationReference("col1", null), SortDirection.DESC, null);
        withTable.setSearchSortKeyList(Collections.singletonList(s0));
        withTable.setSearchValueSet(new SetValue("search_var_name", null, null));
        withTable.setCycleAliasList(Arrays.asList("col2", "col3"));
        withTable.setCycleValueSet(new SetValue("cycle_name", "'123'", "'432'"));

        WithTable withTable1 = new WithTable("relation_name", getDefaultSelectSimple());
        withTable1.setAliasList(Arrays.asList("col1", "col2"));
        withTable1.setSearchMode(SearchMode.DEPTH_FIRST);
        SortKey ss0 = new SortKey(new RelationReference("col1", null), SortDirection.DESC, null);
        withTable1.setSearchSortKeyList(Collections.singletonList(ss0));
        withTable1.setSearchValueSet(new SetValue("search_var_name", null, null));

        WithTable withTable2 = new WithTable("relation_name", getDefaultSelectSimple());
        withTable2.setAliasList(Arrays.asList("col1", "col2"));
        withTable2.setCycleAliasList(Arrays.asList("col2", "col3"));
        withTable2.setCycleValueSet(new SetValue("cycle_name", "'123'", "'432'"));
        body.setWith(Arrays.asList(withTable, withTable1, withTable2));

        Select expect = new Select(body);
        SortKey s1 = new SortKey(new RelationReference("col", null), SortDirection.DESC, null);
        SortKey s2 = new SortKey(new RelationReference("col1", null), SortDirection.ASC, null);
        OrderBy orderBy = new OrderBy(false, Arrays.asList(s1, s2));
        expect.setOrderBy(orderBy);
        Fetch fetch = new Fetch(new ConstExpression("12"), FetchDirection.FIRST, FetchType.COUNT, FetchAddition.ONLY,
                new ConstExpression("12"));
        expect.setFetch(fetch);
        ColumnReference c1 = new ColumnReference(null, null, "col1");
        ColumnReference c2 = new ColumnReference(null, null, "col2");
        expect.setForUpdate(new ForUpdate(Arrays.asList(c1, c2), WaitOption.WAIT, BigDecimal.valueOf(12)));
        Assert.assertEquals(expect, actual);
    }

    private SelectBody getDefaultSelect() {
        Projection p = new Projection();
        NameReference r = new NameReference(null, "dual", null);
        SelectBody body = new SelectBody(Collections.singletonList(p), Collections.singletonList(r));
        SortKey s1 = new SortKey(new RelationReference("col5", null), SortDirection.DESC, null);
        OrderBy orderBy = new OrderBy(false, Collections.singletonList(s1));
        body.setOrderBy(orderBy);
        Fetch fetch = new Fetch(new ConstExpression("21"), FetchDirection.FIRST, FetchType.PERCENT,
                FetchAddition.WITH_TIES, null);
        body.setFetch(fetch);
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
