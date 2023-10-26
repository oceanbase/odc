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

import com.oceanbase.tools.sqlparser.adapter.oracle.OracleWithTableFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBLexer;
import com.oceanbase.tools.sqlparser.oboracle.OBParser;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Common_table_exprContext;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;
import com.oceanbase.tools.sqlparser.statement.expression.RelationReference;
import com.oceanbase.tools.sqlparser.statement.select.NameReference;
import com.oceanbase.tools.sqlparser.statement.select.OrderBy;
import com.oceanbase.tools.sqlparser.statement.select.Projection;
import com.oceanbase.tools.sqlparser.statement.select.SelectBody;
import com.oceanbase.tools.sqlparser.statement.select.SortDirection;
import com.oceanbase.tools.sqlparser.statement.select.SortKey;
import com.oceanbase.tools.sqlparser.statement.select.WithTable;
import com.oceanbase.tools.sqlparser.statement.select.oracle.Fetch;
import com.oceanbase.tools.sqlparser.statement.select.oracle.FetchAddition;
import com.oceanbase.tools.sqlparser.statement.select.oracle.FetchDirection;
import com.oceanbase.tools.sqlparser.statement.select.oracle.FetchType;
import com.oceanbase.tools.sqlparser.statement.select.oracle.SearchMode;
import com.oceanbase.tools.sqlparser.statement.select.oracle.SetValue;

/**
 * {@link OracleWithTableFactoryTest}
 *
 * @author yh263208
 * @date 2022-12-07 17:42
 * @since ODC_release_4.1.0
 */
public class OracleWithTableFactoryTest {

    @Test
    public void generate_withoutAliasList_generateWithTableSucceed() {
        Common_table_exprContext context =
                getTableExprContext(
                        "WITH relation_name as (select * from dual order by abc desc fetch next 12 rows only) select 2 from dual");
        StatementFactory<WithTable> factory = new OracleWithTableFactory(context);
        WithTable actual = factory.generate();

        SelectBody selectBody = getDefaultSelect();
        SortKey s1 = new SortKey(new RelationReference("abc", null), SortDirection.DESC, null);
        OrderBy orderBy = new OrderBy(false, Collections.singletonList(s1));
        selectBody.setOrderBy(orderBy);
        Fetch fetch =
                new Fetch(new ConstExpression("12"), FetchDirection.NEXT, FetchType.COUNT, FetchAddition.ONLY, null);
        selectBody.setFetch(fetch);
        WithTable expect = new WithTable("relation_name", selectBody);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_withAliasList_generateWithTableSucceed() {
        Common_table_exprContext context =
                getTableExprContext("WITH relation_name (col1, col2) as (select * from dual) select 2 from dual");
        StatementFactory<WithTable> factory = new OracleWithTableFactory(context);
        WithTable actual = factory.generate();

        WithTable expect = new WithTable("relation_name", getDefaultSelect());
        expect.setAliasList(Arrays.asList("col1", "col2"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_depthFirstSearch_generateWithTableSucceed() {
        Common_table_exprContext context = getTableExprContext(
                "WITH relation_name (col1, col2) as (select * from dual) search depth first by col2 desc, col3 asc set varname select 2 from dual");
        StatementFactory<WithTable> factory = new OracleWithTableFactory(context);
        WithTable actual = factory.generate();

        WithTable expect = new WithTable("relation_name", getDefaultSelect());
        expect.setAliasList(Arrays.asList("col1", "col2"));
        expect.setSearchMode(SearchMode.DEPTH_FIRST);
        SortKey s1 = new SortKey(new RelationReference("col2", null), SortDirection.DESC, null);
        SortKey s2 = new SortKey(new RelationReference("col3", null), SortDirection.ASC, null);
        expect.setSearchSortKeyList(Arrays.asList(s1, s2));
        expect.setSearchValueSet(new SetValue("varname", null, null));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_breadthFirstSearch_generateWithTableSucceed() {
        Common_table_exprContext context = getTableExprContext(
                "WITH relation_name (col1, col2) as (select * from dual) search breadth first by col2 desc, col3 asc set varname select 2 from dual");
        StatementFactory<WithTable> factory = new OracleWithTableFactory(context);
        WithTable actual = factory.generate();

        WithTable expect = new WithTable("relation_name", getDefaultSelect());
        expect.setAliasList(Arrays.asList("col1", "col2"));
        expect.setSearchMode(SearchMode.BREADTH_FIRST);
        SortKey s1 = new SortKey(new RelationReference("col2", null), SortDirection.DESC, null);
        SortKey s2 = new SortKey(new RelationReference("col3", null), SortDirection.ASC, null);
        expect.setSearchSortKeyList(Arrays.asList(s1, s2));
        expect.setSearchValueSet(new SetValue("varname", null, null));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_withCycle_generateWithTableSucceed() {
        Common_table_exprContext context = getTableExprContext(
                "WITH relation_name (col1, col2) as (select * from dual) search breadth first by col2 desc, col3 asc set varname cycle col2,col3 set cyclename to 'abcd' default '1234' select 2 from dual");
        StatementFactory<WithTable> factory = new OracleWithTableFactory(context);
        WithTable actual = factory.generate();

        WithTable expect = new WithTable("relation_name", getDefaultSelect());
        expect.setAliasList(Arrays.asList("col1", "col2"));
        expect.setSearchMode(SearchMode.BREADTH_FIRST);
        SortKey s1 = new SortKey(new RelationReference("col2", null), SortDirection.DESC, null);
        SortKey s2 = new SortKey(new RelationReference("col3", null), SortDirection.ASC, null);
        expect.setSearchSortKeyList(Arrays.asList(s1, s2));
        expect.setSearchValueSet(new SetValue("varname", null, null));
        expect.setCycleAliasList(Arrays.asList("col2", "col3"));
        expect.setCycleValueSet(new SetValue("cyclename", "'abcd'", "'1234'"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_withCycleAliasWithoutAlias_generateWithTableSucceed() {
        Common_table_exprContext context = getTableExprContext(
                "WITH relation_name as (select * from dual) search breadth first by col2 desc, col3 asc set varname cycle col2,col3 set cyclename to 'abcd' default '1234' select 2 from dual");
        StatementFactory<WithTable> factory = new OracleWithTableFactory(context);
        WithTable actual = factory.generate();

        WithTable expect = new WithTable("relation_name", getDefaultSelect());
        expect.setSearchMode(SearchMode.BREADTH_FIRST);
        SortKey s1 = new SortKey(new RelationReference("col2", null), SortDirection.DESC, null);
        SortKey s2 = new SortKey(new RelationReference("col3", null), SortDirection.ASC, null);
        expect.setSearchSortKeyList(Arrays.asList(s1, s2));
        expect.setSearchValueSet(new SetValue("varname", null, null));
        expect.setCycleAliasList(Arrays.asList("col2", "col3"));
        expect.setCycleValueSet(new SetValue("cyclename", "'abcd'", "'1234'"));
        Assert.assertEquals(expect, actual);
    }

    private SelectBody getDefaultSelect() {
        Projection p = new Projection();
        NameReference r = new NameReference(null, "dual", null);
        return new SelectBody(Collections.singletonList(p), Collections.singletonList(r));
    }

    private Common_table_exprContext getTableExprContext(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.select_stmt().subquery().with_select().with_clause().with_list().common_table_expr(0);
    }

}
