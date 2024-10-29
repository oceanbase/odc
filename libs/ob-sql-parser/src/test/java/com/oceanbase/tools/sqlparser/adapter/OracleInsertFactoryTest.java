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

import java.util.*;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.sqlparser.adapter.oracle.OracleInsertFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBLexer;
import com.oceanbase.tools.sqlparser.oboracle.OBParser;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Insert_stmtContext;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;
import com.oceanbase.tools.sqlparser.statement.common.oracle.LogErrors;
import com.oceanbase.tools.sqlparser.statement.common.oracle.Returning;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;
import com.oceanbase.tools.sqlparser.statement.expression.RelationReference;
import com.oceanbase.tools.sqlparser.statement.insert.ConditionalInsert;
import com.oceanbase.tools.sqlparser.statement.insert.Insert;
import com.oceanbase.tools.sqlparser.statement.insert.InsertCondition;
import com.oceanbase.tools.sqlparser.statement.insert.InsertTable;
import com.oceanbase.tools.sqlparser.statement.select.NameReference;
import com.oceanbase.tools.sqlparser.statement.select.OrderBy;
import com.oceanbase.tools.sqlparser.statement.select.PartitionType;
import com.oceanbase.tools.sqlparser.statement.select.PartitionUsage;
import com.oceanbase.tools.sqlparser.statement.select.Projection;
import com.oceanbase.tools.sqlparser.statement.select.Select;
import com.oceanbase.tools.sqlparser.statement.select.SelectBody;
import com.oceanbase.tools.sqlparser.statement.select.SortDirection;
import com.oceanbase.tools.sqlparser.statement.select.SortKey;
import com.oceanbase.tools.sqlparser.statement.select.oracle.Fetch;
import com.oceanbase.tools.sqlparser.statement.select.oracle.FetchAddition;
import com.oceanbase.tools.sqlparser.statement.select.oracle.FetchDirection;
import com.oceanbase.tools.sqlparser.statement.select.oracle.FetchType;

/**
 * {@link OracleInsertFactoryTest}
 *
 * @author yh263208
 * @date 2023-11-09 16:08
 * @since ODC_release_4.2.3
 */
public class OracleInsertFactoryTest {

    @Test
    public void generate_simpleInsert_succeed() {
        StatementFactory<Insert> factory = new OracleInsertFactory(
                getInsertContext("insert into a.b partition(p1, p2) alias values(1,default)"));
        Insert actual = factory.generate();

        RelationFactor factor = new RelationFactor("b");
        factor.setSchema("a");
        InsertTable insertTable = new InsertTable(factor);
        insertTable.setPartitionUsage(new PartitionUsage(PartitionType.PARTITION, Arrays.asList("p1", "p2")));
        insertTable.setAlias("alias");
        List<List<Expression>> values = new ArrayList<>();
        values.add(Arrays.asList(new ConstExpression("1"), new ConstExpression("default")));
        insertTable.setValues(values);
        Insert expect = new Insert(Collections.singletonList(insertTable), null);
        Assert.assertEquals(actual, expect);
    }

    @Test
    public void generate_simpleInsertWithPartition_succeed() {
        StatementFactory<Insert> factory = new OracleInsertFactory(
                getInsertContext("insert into a.b partition(col='111', col2=121) alias values(1,default)"));
        Insert actual = factory.generate();

        RelationFactor factor = new RelationFactor("b");
        factor.setSchema("a");
        InsertTable insertTable = new InsertTable(factor);
        Map<String, Expression> parti = new HashMap<>();
        parti.put("col", new ConstExpression("'111'"));
        parti.put("col2", new ConstExpression("121"));
        insertTable.setPartitionUsage(new PartitionUsage(PartitionType.PARTITION, parti));
        insertTable.setAlias("alias");
        List<List<Expression>> values = new ArrayList<>();
        values.add(Arrays.asList(new ConstExpression("1"), new ConstExpression("default")));
        insertTable.setValues(values);
        Insert expect = new Insert(Collections.singletonList(insertTable), null);
        Assert.assertEquals(actual, expect);
    }

    @Test
    public void generate_overwriteInsert_succeed() {
        StatementFactory<Insert> factory = new OracleInsertFactory(
                getInsertContext("insert overwrite a.b values(1,default)"));
        Insert actual = factory.generate();

        RelationFactor factor = new RelationFactor("b");
        factor.setSchema("a");
        InsertTable insertTable = new InsertTable(factor);
        List<List<Expression>> values = new ArrayList<>();
        values.add(Arrays.asList(new ConstExpression("1"), new ConstExpression("default")));
        insertTable.setValues(values);
        Insert expect = new Insert(Collections.singletonList(insertTable), null);
        expect.setOverwrite(true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_insertSelect_succeed() {
        StatementFactory<Insert> factory = new OracleInsertFactory(
                getInsertContext("insert into (select col.* abc from dual) abcd nologging () values(1,default)"));
        Insert actual = factory.generate();

        RelationReference r = new RelationReference("col", new RelationReference("*", null));
        Projection p = new Projection(r, "abc");
        NameReference from = new NameReference(null, "dual", null);
        InsertTable insertTable = new InsertTable(
                new SelectBody(Collections.singletonList(p), Collections.singletonList(from)));
        insertTable.setAlias("abcd");
        insertTable.setNologging(true);
        List<List<Expression>> values = new ArrayList<>();
        values.add(Arrays.asList(new ConstExpression("1"), new ConstExpression("default")));
        insertTable.setValues(values);
        Insert expect = new Insert(Collections.singletonList(insertTable), null);
        Assert.assertEquals(actual, expect);
    }

    @Test
    public void generate_insertSelectColumns_succeed() {
        StatementFactory<Insert> factory = new OracleInsertFactory(
                getInsertContext("insert into (select col.* abc from dual order by col desc) "
                        + "abcd nologging (col1, col2) values(1,default), (1,2) "));
        Insert actual = factory.generate();

        RelationReference r = new RelationReference("col", new RelationReference("*", null));
        Projection p = new Projection(r, "abc");
        NameReference from = new NameReference(null, "dual", null);
        SortKey s1 = new SortKey(new RelationReference("col", null), SortDirection.DESC, null);
        OrderBy orderBy = new OrderBy(false, Collections.singletonList(s1));
        SelectBody selectBody = new SelectBody(Collections.singletonList(p), Collections.singletonList(from));
        selectBody.setOrderBy(orderBy);
        InsertTable insertTable = new InsertTable(selectBody);
        insertTable.setColumns(Arrays.asList(new ColumnReference(null, null, "col1"),
                new ColumnReference(null, null, "col2")));
        insertTable.setAlias("abcd");
        insertTable.setNologging(true);
        List<List<Expression>> values = new ArrayList<>();
        values.add(Arrays.asList(new ConstExpression("1"), new ConstExpression("default")));
        values.add(Arrays.asList(new ConstExpression("1"), new ConstExpression("2")));
        insertTable.setValues(values);
        Insert expect = new Insert(Collections.singletonList(insertTable), null);
        Assert.assertEquals(actual, expect);
    }

    @Test
    public void generate_insertSelect1_succeed() {
        StatementFactory<Insert> factory = new OracleInsertFactory(
                getInsertContext("insert into (select col.* abc from dual order by col desc "
                        + "offset 12 rows fetch first 12 rows only with check option) "
                        + "abcd nologging (col1, col2) values a.b returning a,b BULK COLLECT into @ab,a.b,:12"));
        Insert actual = factory.generate();

        RelationReference r = new RelationReference("col", new RelationReference("*", null));
        Projection p = new Projection(r, "abc");
        NameReference from = new NameReference(null, "dual", null);
        SortKey s1 = new SortKey(new RelationReference("col", null), SortDirection.DESC, null);
        OrderBy orderBy = new OrderBy(false, Collections.singletonList(s1));
        SelectBody selectBody = new SelectBody(Collections.singletonList(p), Collections.singletonList(from));
        selectBody.setOrderBy(orderBy);
        selectBody.setFetch(new Fetch(new ConstExpression("12"),
                FetchDirection.FIRST, FetchType.COUNT, FetchAddition.ONLY, new ConstExpression("12")));
        selectBody.setWithCheckOption(true);
        InsertTable insertTable = new InsertTable(selectBody);
        insertTable.setColumns(Arrays.asList(new ColumnReference(null, null, "col1"),
                new ColumnReference(null, null, "col2")));
        insertTable.setAlias("abcd");
        insertTable.setNologging(true);
        List<List<Expression>> values = new ArrayList<>();
        values.add(Collections.singletonList(new RelationReference("a", new RelationReference("b", null))));
        insertTable.setValues(values);
        Insert expect = new Insert(Collections.singletonList(insertTable), null);
        Returning returning = new Returning(Arrays.asList(new ConstExpression("@ab"),
                new RelationReference("a", new RelationReference("b", null)),
                new ConstExpression(":12")),
                Arrays.asList(new Projection(new RelationReference("a", null), null),
                        new Projection(new RelationReference("b", null), null)));
        returning.setBulkCollect(true);
        expect.setReturning(returning);
        Assert.assertEquals(actual, expect);
    }

    @Test
    public void generate_logErrors_succeed() {
        StatementFactory<Insert> factory = new OracleInsertFactory(
                getInsertContext("insert into a.b values(1,default) log errors"));
        Insert actual = factory.generate();

        RelationFactor factor = new RelationFactor("b");
        factor.setSchema("a");
        InsertTable insertTable = new InsertTable(factor);
        List<List<Expression>> values = new ArrayList<>();
        values.add(Arrays.asList(new ConstExpression("1"), new ConstExpression("default")));
        insertTable.setValues(values);
        Insert expect = new Insert(Collections.singletonList(insertTable), null);
        expect.setLogErrors(new LogErrors());
        Assert.assertEquals(actual, expect);
    }

    @Test
    public void generate_fullLogErrors_succeed() {
        StatementFactory<Insert> factory = new OracleInsertFactory(getInsertContext(
                "insert into a.b select col.* abc from dual order by col desc "
                        + "offset 12 rows fetch first 12 rows only "
                        + "log errors into b (123) REJECT LIMIT 12"));
        Insert actual = factory.generate();

        RelationFactor factor = new RelationFactor("b");
        factor.setSchema("a");
        InsertTable insertTable = new InsertTable(factor);
        List<List<Expression>> values = new ArrayList<>();
        RelationReference r = new RelationReference("col", new RelationReference("*", null));
        Projection p = new Projection(r, "abc");
        NameReference from = new NameReference(null, "dual", null);
        SortKey s1 = new SortKey(new RelationReference("col", null), SortDirection.DESC, null);
        OrderBy orderBy = new OrderBy(false, Collections.singletonList(s1));
        Select select = new Select(new SelectBody(Collections.singletonList(p), Collections.singletonList(from)));
        select.setOrderBy(orderBy);
        select.setFetch(new Fetch(new ConstExpression("12"),
                FetchDirection.FIRST, FetchType.COUNT, FetchAddition.ONLY, new ConstExpression("12")));
        values.add(Collections.singletonList(select));
        insertTable.setValues(values);
        Insert expect = new Insert(Collections.singletonList(insertTable), null);
        LogErrors logErrors = new LogErrors();
        logErrors.setInto(new RelationFactor("b"));
        logErrors.setExpression(new ConstExpression("123"));
        logErrors.setRejectLimit(12);
        logErrors.setUnlimitedReject(false);
        expect.setLogErrors(logErrors);
        Assert.assertEquals(actual, expect);
    }

    @Test
    public void generate_multiInsert_succeed() {
        StatementFactory<Insert> factory = new OracleInsertFactory(getInsertContext(
                "insert all into a partition(p1, p2) "
                        + "into b (c1, c2) "
                        + "into c values (1, default) "
                        + "into d (c3, c4) values(1, 2) "
                        + "select col.* abc from dual order by col desc offset 12 rows fetch first 12 rows only"));
        Insert actual = factory.generate();

        InsertTable t1 = new InsertTable(new RelationFactor("a"));
        t1.setPartitionUsage(new PartitionUsage(PartitionType.PARTITION, Arrays.asList("p1", "p2")));
        InsertTable t2 = new InsertTable(new RelationFactor("b"));
        t2.setColumns(Arrays.asList(new ColumnReference(null, null, "c1"),
                new ColumnReference(null, null, "c2")));
        InsertTable t3 = new InsertTable(new RelationFactor("c"));
        List<List<Expression>> values = new ArrayList<>();
        values.add(Arrays.asList(new ConstExpression("1"), new ConstExpression("default")));
        t3.setValues(values);
        InsertTable t4 = new InsertTable(new RelationFactor("d"));
        values = new ArrayList<>();
        values.add(Arrays.asList(new ConstExpression("1"), new ConstExpression("2")));
        t4.setValues(values);
        t4.setColumns(Arrays.asList(new ColumnReference(null, null, "c3"),
                new ColumnReference(null, null, "c4")));
        RelationReference r = new RelationReference("col", new RelationReference("*", null));
        Projection p = new Projection(r, "abc");
        NameReference from = new NameReference(null, "dual", null);
        SortKey s1 = new SortKey(new RelationReference("col", null), SortDirection.DESC, null);
        OrderBy orderBy = new OrderBy(false, Collections.singletonList(s1));
        Select select = new Select(new SelectBody(Collections.singletonList(p), Collections.singletonList(from)));
        select.setOrderBy(orderBy);
        select.setFetch(new Fetch(new ConstExpression("12"),
                FetchDirection.FIRST, FetchType.COUNT, FetchAddition.ONLY, new ConstExpression("12")));
        Insert expect = new Insert(Arrays.asList(t1, t2, t3, t4), null);
        expect.setAll(true);
        expect.setSelect(select);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_conditionalInsert_succeed() {
        StatementFactory<Insert> factory = new OracleInsertFactory(getInsertContext(
                "insert all when 1 then into a partition(p1, p2) "
                        + "into b (c1, c2) "
                        + "when 2 then into c values (1, default) "
                        + "into d (c3, c4) values(1, 2) "
                        + "select col.* abc from dual order by col desc offset 12 rows fetch first 12 rows only"));
        Insert actual = factory.generate();

        InsertTable t1 = new InsertTable(new RelationFactor("a"));
        t1.setPartitionUsage(new PartitionUsage(PartitionType.PARTITION, Arrays.asList("p1", "p2")));
        InsertTable t2 = new InsertTable(new RelationFactor("b"));
        t2.setColumns(Arrays.asList(new ColumnReference(null, null, "c1"),
                new ColumnReference(null, null, "c2")));
        InsertCondition i1 = new InsertCondition(new ConstExpression("1"), Arrays.asList(t1, t2));
        InsertTable t3 = new InsertTable(new RelationFactor("c"));
        List<List<Expression>> values = new ArrayList<>();
        values.add(Arrays.asList(new ConstExpression("1"), new ConstExpression("default")));
        t3.setValues(values);
        InsertTable t4 = new InsertTable(new RelationFactor("d"));
        values = new ArrayList<>();
        values.add(Arrays.asList(new ConstExpression("1"), new ConstExpression("2")));
        t4.setValues(values);
        t4.setColumns(Arrays.asList(new ColumnReference(null, null, "c3"),
                new ColumnReference(null, null, "c4")));
        InsertCondition i2 = new InsertCondition(new ConstExpression("2"), Arrays.asList(t3, t4));
        ConditionalInsert conditionalInsert = new ConditionalInsert(Arrays.asList(i1, i2));
        RelationReference r = new RelationReference("col", new RelationReference("*", null));
        Projection p = new Projection(r, "abc");
        NameReference from = new NameReference(null, "dual", null);
        SortKey s1 = new SortKey(new RelationReference("col", null), SortDirection.DESC, null);
        OrderBy orderBy = new OrderBy(false, Collections.singletonList(s1));
        Select select = new Select(new SelectBody(Collections.singletonList(p), Collections.singletonList(from)));
        select.setOrderBy(orderBy);
        select.setFetch(new Fetch(new ConstExpression("12"),
                FetchDirection.FIRST, FetchType.COUNT, FetchAddition.ONLY, new ConstExpression("12")));
        Insert expect = new Insert(null, conditionalInsert);
        expect.setSelect(select);
        expect.setAll(true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_elseConditionalInsert_succeed() {
        StatementFactory<Insert> factory = new OracleInsertFactory(getInsertContext(
                "insert first when 1 then into a partition(p1, p2) "
                        + "when 2 then into b (c1, c2) "
                        + "else into c values (1, default) "
                        + "into d (c3, c4) values(1, 2) "
                        + "select col.* abc from dual order by col desc offset 12 rows fetch first 12 rows only"));
        Insert actual = factory.generate();

        InsertTable t1 = new InsertTable(new RelationFactor("a"));
        t1.setPartitionUsage(new PartitionUsage(PartitionType.PARTITION, Arrays.asList("p1", "p2")));
        InsertCondition i1 = new InsertCondition(new ConstExpression("1"), Collections.singletonList(t1));
        InsertTable t2 = new InsertTable(new RelationFactor("b"));
        t2.setColumns(Arrays.asList(new ColumnReference(null, null, "c1"),
                new ColumnReference(null, null, "c2")));
        InsertCondition i2 = new InsertCondition(new ConstExpression("2"), Collections.singletonList(t2));
        InsertTable t3 = new InsertTable(new RelationFactor("c"));
        List<List<Expression>> values = new ArrayList<>();
        values.add(Arrays.asList(new ConstExpression("1"), new ConstExpression("default")));
        t3.setValues(values);
        InsertTable t4 = new InsertTable(new RelationFactor("d"));
        values = new ArrayList<>();
        values.add(Arrays.asList(new ConstExpression("1"), new ConstExpression("2")));
        t4.setValues(values);
        t4.setColumns(Arrays.asList(new ColumnReference(null, null, "c3"),
                new ColumnReference(null, null, "c4")));
        ConditionalInsert conditionalInsert = new ConditionalInsert(Arrays.asList(i1, i2));
        conditionalInsert.setElseClause(Arrays.asList(t3, t4));
        RelationReference r = new RelationReference("col", new RelationReference("*", null));
        Projection p = new Projection(r, "abc");
        NameReference from = new NameReference(null, "dual", null);
        SortKey s1 = new SortKey(new RelationReference("col", null), SortDirection.DESC, null);
        OrderBy orderBy = new OrderBy(false, Collections.singletonList(s1));
        Select select = new Select(new SelectBody(Collections.singletonList(p), Collections.singletonList(from)));
        select.setOrderBy(orderBy);
        select.setFetch(new Fetch(new ConstExpression("12"),
                FetchDirection.FIRST, FetchType.COUNT, FetchAddition.ONLY, new ConstExpression("12")));
        Insert expect = new Insert(null, conditionalInsert);
        expect.setSelect(select);
        expect.setFirst(true);
        Assert.assertEquals(expect, actual);
    }

    private Insert_stmtContext getInsertContext(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.insert_stmt();
    }

}
