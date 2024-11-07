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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLInsertFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Insert_stmtContext;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;
import com.oceanbase.tools.sqlparser.statement.expression.ArrayExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;
import com.oceanbase.tools.sqlparser.statement.insert.Insert;
import com.oceanbase.tools.sqlparser.statement.insert.InsertTable;
import com.oceanbase.tools.sqlparser.statement.insert.mysql.SetColumn;
import com.oceanbase.tools.sqlparser.statement.select.NameReference;
import com.oceanbase.tools.sqlparser.statement.select.PartitionType;
import com.oceanbase.tools.sqlparser.statement.select.PartitionUsage;
import com.oceanbase.tools.sqlparser.statement.select.Projection;
import com.oceanbase.tools.sqlparser.statement.select.Select;
import com.oceanbase.tools.sqlparser.statement.select.SelectBody;

/**
 * {@link MySQLInsertFactoryTest}
 *
 * @author yh263208
 * @date 2023-11-09 15:19
 * @since ODC_release_4.2.3
 */
public class MySQLInsertFactoryTest {

    @Test
    public void generate_simpleInsert_succeed() {
        StatementFactory<Insert> factory = new MySQLInsertFactory(
                getInsertContext("insert a.b values(1,default)"));
        Insert actual = factory.generate();

        RelationFactor factor = new RelationFactor("b");
        factor.setSchema("a");
        InsertTable insertTable = new InsertTable(factor);
        List<List<Expression>> values = new ArrayList<>();
        values.add(Arrays.asList(new ConstExpression("1"), new ConstExpression("default")));
        insertTable.setValues(values);
        Insert expect = new Insert(Collections.singletonList(insertTable), null);
        Assert.assertEquals(actual, expect);
    }

    @Test
    public void generate_insertWithHighPriority_succeed() {
        StatementFactory<Insert> factory = new MySQLInsertFactory(
                getInsertContext("insert high_priority overwrite a.b values(1,default)"));
        Insert actual = factory.generate();

        RelationFactor factor = new RelationFactor("b");
        factor.setSchema("a");
        InsertTable insertTable = new InsertTable(factor);
        List<List<Expression>> values = new ArrayList<>();
        values.add(Arrays.asList(new ConstExpression("1"), new ConstExpression("default")));
        insertTable.setValues(values);
        Insert expect = new Insert(Collections.singletonList(insertTable), null);
        expect.setHighPriority(true);
        expect.setOverwrite(true);
        Assert.assertEquals(actual, expect);
    }

    @Test
    public void generate_simpleReplace_succeed() {
        StatementFactory<Insert> factory = new MySQLInsertFactory(getInsertContext(
                "replace a.b() select col.* abc from dual"));
        Insert actual = factory.generate();

        RelationFactor factor = new RelationFactor("b");
        factor.setSchema("a");
        InsertTable insertTable = new InsertTable(factor);
        List<List<Expression>> values = new ArrayList<>();
        ColumnReference r = new ColumnReference(null, "col", "*");
        Projection p = new Projection(r, "abc");
        NameReference from = new NameReference(null, "dual", null);
        values.add(Collections.singletonList(new Select(new SelectBody(
                Collections.singletonList(p), Collections.singletonList(from)))));
        insertTable.setValues(values);
        Insert expect = new Insert(Collections.singletonList(insertTable), null);
        expect.setReplace(true);
        Assert.assertEquals(actual, expect);
    }

    @Test
    public void generate_onDuplicateKey_succeed() {
        StatementFactory<Insert> factory = new MySQLInsertFactory(getInsertContext("insert ignore into a.b "
                + "partition (p1,p2)(col1, col2) values(1,2),(3,5) on duplicate key update tb.col1='abcd'"));
        Insert actual = factory.generate();

        RelationFactor factor = new RelationFactor("b");
        factor.setSchema("a");
        InsertTable insertTable = new InsertTable(factor);
        List<List<Expression>> values = new ArrayList<>();
        values.add(Arrays.asList(new ConstExpression("1"), new ConstExpression("2")));
        values.add(Arrays.asList(new ConstExpression("3"), new ConstExpression("5")));
        insertTable.setValues(values);
        insertTable.setColumns(Arrays.asList(new ColumnReference(null, null, "col1"),
                new ColumnReference(null, null, "col2")));
        insertTable.setPartitionUsage(new PartitionUsage(PartitionType.PARTITION, Arrays.asList("p1", "p2")));
        Insert expect = new Insert(Collections.singletonList(insertTable), null);
        expect.setIgnore(true);
        SetColumn setColumn = new SetColumn(new ColumnReference(null, "tb", "col1"), new ConstExpression("'abcd'"));
        expect.setOnDuplicateKeyUpdateColumns(Collections.singletonList(setColumn));
        Assert.assertEquals(actual, expect);
    }

    @Test
    public void generate_setColumn_succeed() {
        StatementFactory<Insert> factory = new MySQLInsertFactory(
                getInsertContext(
                        "insert ignore into a.b partition (p1,p2) set tb.col2=default on duplicate key update tb.col1='abcd'"));
        Insert actual = factory.generate();

        RelationFactor factor = new RelationFactor("b");
        factor.setSchema("a");
        InsertTable insertTable = new InsertTable(factor);
        insertTable.setSetColumns(Collections.singletonList(
                new SetColumn(new ColumnReference(null, "tb", "col2"), new ConstExpression("default"))));
        insertTable.setPartitionUsage(new PartitionUsage(PartitionType.PARTITION, Arrays.asList("p1", "p2")));
        Insert expect = new Insert(Collections.singletonList(insertTable), null);
        expect.setIgnore(true);
        SetColumn setColumn = new SetColumn(new ColumnReference(null, "tb", "col1"), new ConstExpression("'abcd'"));
        expect.setOnDuplicateKeyUpdateColumns(Collections.singletonList(setColumn));
        Assert.assertEquals(actual, expect);
    }

    @Test
    public void generate_valuesWithAlias_succeed() {
        StatementFactory<Insert> factory = new MySQLInsertFactory(
                getInsertContext("insert a.b values(1,default) as new(a,b)"));
        Insert actual = factory.generate();

        RelationFactor factor = new RelationFactor("b");
        factor.setSchema("a");
        InsertTable insertTable = new InsertTable(factor);
        List<List<Expression>> values = new ArrayList<>();
        values.add(Arrays.asList(new ConstExpression("1"), new ConstExpression("default")));
        insertTable.setValues(values);
        insertTable.setAlias("new");
        insertTable.setAliasColumns(Arrays.asList("a", "b"));
        Insert expect = new Insert(Collections.singletonList(insertTable), null);
        Assert.assertEquals(actual, expect);
    }

    @Test
    public void generate_setColumnWithAlias_succeed() {
        StatementFactory<Insert> factory = new MySQLInsertFactory(
                getInsertContext(
                        "insert ignore into a.b partition (p1,p2) set tb.col2=default as new(c,d) on duplicate key update tb.col1='abcd'"));
        Insert actual = factory.generate();

        RelationFactor factor = new RelationFactor("b");
        factor.setSchema("a");
        InsertTable insertTable = new InsertTable(factor);
        insertTable.setAlias("new");
        insertTable.setAliasColumns(Arrays.asList("c", "d"));
        insertTable.setSetColumns(Collections.singletonList(
                new SetColumn(new ColumnReference(null, "tb", "col2"), new ConstExpression("default"))));
        insertTable.setPartitionUsage(new PartitionUsage(PartitionType.PARTITION, Arrays.asList("p1", "p2")));
        Insert expect = new Insert(Collections.singletonList(insertTable), null);
        expect.setIgnore(true);
        SetColumn setColumn = new SetColumn(new ColumnReference(null, "tb", "col1"), new ConstExpression("'abcd'"));
        expect.setOnDuplicateKeyUpdateColumns(Collections.singletonList(setColumn));
        Assert.assertEquals(actual, expect);
    }

    @Test
    public void generate_InsertVectorValues_Succeed() {
        StatementFactory<Insert> factory = new MySQLInsertFactory(
                getInsertContext(
                        "insert into any_schema.t_vec values(41, [0.735541,0.670776,0.903237]);"));
        Insert actual = factory.generate();

        RelationFactor factor = new RelationFactor("t_vec");
        factor.setSchema("any_schema");
        InsertTable insertTable = new InsertTable(factor);
        List<List<Expression>> values = new ArrayList<>();
        ArrayExpression arrayExpression = new ArrayExpression(Arrays.asList(
                new ConstExpression("0.735541"), new ConstExpression("0.670776"), new ConstExpression("0.903237")));
        values.add(Arrays.asList(new ConstExpression("41"), arrayExpression));

        insertTable.setValues(values);
        Insert expect = new Insert(Collections.singletonList(insertTable), null);
        Assert.assertEquals(actual, expect);
    }

    private Insert_stmtContext getInsertContext(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.insert_stmt();
    }

}
