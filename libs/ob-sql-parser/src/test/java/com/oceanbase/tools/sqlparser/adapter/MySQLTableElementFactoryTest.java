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

import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLTableElementFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Table_elementContext;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.Operator;
import com.oceanbase.tools.sqlparser.statement.common.CharacterType;
import com.oceanbase.tools.sqlparser.statement.common.ColumnGroupElement;
import com.oceanbase.tools.sqlparser.statement.common.DataType;
import com.oceanbase.tools.sqlparser.statement.common.GeneralDataType;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnAttributes;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnDefinition;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnDefinition.Location;
import com.oceanbase.tools.sqlparser.statement.createtable.ConstraintState;
import com.oceanbase.tools.sqlparser.statement.createtable.ForeignReference;
import com.oceanbase.tools.sqlparser.statement.createtable.ForeignReference.MatchOption;
import com.oceanbase.tools.sqlparser.statement.createtable.ForeignReference.OnOption;
import com.oceanbase.tools.sqlparser.statement.createtable.GenerateOption;
import com.oceanbase.tools.sqlparser.statement.createtable.GenerateOption.Type;
import com.oceanbase.tools.sqlparser.statement.createtable.HashPartition;
import com.oceanbase.tools.sqlparser.statement.createtable.InLineCheckConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.InLineConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.IndexOptions;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineCheckConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineForeignConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineIndex;
import com.oceanbase.tools.sqlparser.statement.createtable.RangePartition;
import com.oceanbase.tools.sqlparser.statement.createtable.SortColumn;
import com.oceanbase.tools.sqlparser.statement.createtable.TableElement;
import com.oceanbase.tools.sqlparser.statement.expression.CaseWhen;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.expression.CompoundExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ExpressionParam;
import com.oceanbase.tools.sqlparser.statement.expression.FunctionCall;
import com.oceanbase.tools.sqlparser.statement.expression.WhenClause;
import com.oceanbase.tools.sqlparser.statement.select.SortDirection;

/**
 * Test cases for {@link MySQLTableElementFactory}
 *
 * @author yh263208
 * @date 2023-05-23 19:57
 * @since ODC_release_4.2.0
 */
public class MySQLTableElementFactoryTest {

    @Test
    public void generate_columnDef_generateSuccees() {
        StatementFactory<TableElement> factory =
                new MySQLTableElementFactory(getTableElementContext("tb.col varchar(64)"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefFirst_generateSuccees() {
        StatementFactory<TableElement> factory =
                new MySQLTableElementFactory(getTableElementContext("tb.col varchar(64) first"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        expect.setLocation(new Location("first", null));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefBefore_generateSuccees() {
        StatementFactory<TableElement> factory =
                new MySQLTableElementFactory(getTableElementContext("tb.col varchar(64) before col"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        expect.setLocation(new Location("before", new ColumnReference(null, null, "col")));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefAfter_generateSuccees() {
        StatementFactory<TableElement> factory =
                new MySQLTableElementFactory(getTableElementContext("tb.col varchar(64) after col"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        expect.setLocation(new Location("after", new ColumnReference(null, null, "col")));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefNotNull_generateSuccees() {
        StatementFactory<TableElement> factory =
                new MySQLTableElementFactory(getTableElementContext("tb.col varchar(64) not null"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ColumnAttributes attributes = new ColumnAttributes();
        InLineConstraint attribute = new InLineConstraint(null, null);
        attribute.setNullable(false);
        attributes.setConstraints(Collections.singletonList(attribute));
        expect.setColumnAttributes(attributes);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefNull_generateSuccees() {
        StatementFactory<TableElement> factory =
                new MySQLTableElementFactory(getTableElementContext("tb.col varchar(64) null"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ColumnAttributes attributes = new ColumnAttributes();
        InLineConstraint attribute = new InLineConstraint(null, null);
        attribute.setNullable(true);
        attributes.setConstraints(Collections.singletonList(attribute));
        expect.setColumnAttributes(attributes);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefPrimaryKey_generateSuccees() {
        StatementFactory<TableElement> factory =
                new MySQLTableElementFactory(getTableElementContext("tb.col varchar(64) primary key"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ColumnAttributes attributes = new ColumnAttributes();
        InLineConstraint attribute = new InLineConstraint(null, null);
        attribute.setPrimaryKey(true);
        attributes.setConstraints(Collections.singletonList(attribute));
        expect.setColumnAttributes(attributes);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefUniqueKey_generateSuccees() {
        StatementFactory<TableElement> factory =
                new MySQLTableElementFactory(getTableElementContext("tb.col varchar(64) unique"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ColumnAttributes attributes = new ColumnAttributes();
        InLineConstraint attribute = new InLineConstraint(null, null);
        attribute.setUniqueKey(true);
        attributes.setConstraints(Collections.singletonList(attribute));
        expect.setColumnAttributes(attributes);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefDefaultExpr_generateSuccees() {
        StatementFactory<TableElement> factory =
                new MySQLTableElementFactory(getTableElementContext("tb.col varchar(64) default 1"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ColumnAttributes attributes = new ColumnAttributes();
        attributes.setDefaultValue(new ConstExpression("1"));
        expect.setColumnAttributes(attributes);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefKey_generateSuccees() {
        StatementFactory<TableElement> factory =
                new MySQLTableElementFactory(getTableElementContext("tb.col varchar(64) key"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ColumnAttributes attributes = new ColumnAttributes();
        InLineConstraint constraint = new InLineConstraint(null, null);
        constraint.setPrimaryKey(true);
        attributes.setConstraints(Collections.singletonList(constraint));
        expect.setColumnAttributes(attributes);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefOrigDefaultExpr_generateSuccees() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext("tb.col varchar(64) orig_default current_timestamp(1)"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ColumnAttributes attributes = new ColumnAttributes();
        FunctionCall expr = new FunctionCall("current_timestamp",
                Collections.singletonList(new ExpressionParam(new ConstExpression("1"))));
        attributes.setOrigDefault(expr);
        expect.setColumnAttributes(attributes);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefOrigDefaultExpr1_generateSuccees() {
        StatementFactory<TableElement> factory =
                new MySQLTableElementFactory(getTableElementContext("tb.col varchar(64) orig_default -12"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ColumnAttributes attributes = new ColumnAttributes();
        attributes.setOrigDefault(new CompoundExpression(new ConstExpression("12"), null, Operator.SUB));
        expect.setColumnAttributes(attributes);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefId_generateSuccees() {
        StatementFactory<TableElement> factory =
                new MySQLTableElementFactory(getTableElementContext("tb.col varchar(64) id 12"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ColumnAttributes attributes = new ColumnAttributes();
        attributes.setId(12);
        expect.setColumnAttributes(attributes);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefsrId_generateSuccees() {
        StatementFactory<TableElement> factory =
                new MySQLTableElementFactory(getTableElementContext("tb.col GEOMETRYCOLLECTION srid 12"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new GeneralDataType("GEOMETRYCOLLECTION", null);
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ColumnAttributes attributes = new ColumnAttributes();
        attributes.setSrid(12);
        expect.setColumnAttributes(attributes);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefAutoIncrement_generateSuccees() {
        StatementFactory<TableElement> factory =
                new MySQLTableElementFactory(getTableElementContext("tb.col varchar(64) auto_increment"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ColumnAttributes attributes = new ColumnAttributes();
        attributes.setAutoIncrement(true);
        expect.setColumnAttributes(attributes);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefComment_generateSuccees() {
        StatementFactory<TableElement> factory =
                new MySQLTableElementFactory(getTableElementContext("tb.col varchar(64) comment 'abcd'"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ColumnAttributes attributes = new ColumnAttributes();
        attributes.setComment("'abcd'");
        expect.setColumnAttributes(attributes);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefCollate_generateSuccees() {
        StatementFactory<TableElement> factory =
                new MySQLTableElementFactory(getTableElementContext("tb.col point collate 'abcd'"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new GeneralDataType("point", null);
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ColumnAttributes attributes = new ColumnAttributes();
        attributes.setCollation("'abcd'");
        expect.setColumnAttributes(attributes);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefOnUpdate_generateSuccees() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext("tb.col varchar(64) on update current_timestamp(1)"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ColumnAttributes attributes = new ColumnAttributes();
        FunctionCall expr = new FunctionCall("current_timestamp",
                Collections.singletonList(new ExpressionParam(new ConstExpression("1"))));
        attributes.setOnUpdate(expr);
        expect.setColumnAttributes(attributes);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefMultiColumnAttrs_generateSuccees() {
        StatementFactory<TableElement> factory =
                new MySQLTableElementFactory(getTableElementContext("tb.col varchar(64) not null primary key"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        InLineConstraint first = new InLineConstraint(null, null);
        first.setNullable(false);
        InLineConstraint second = new InLineConstraint(null, null);
        second.setPrimaryKey(true);
        ColumnAttributes attributes = new ColumnAttributes();
        attributes.setConstraints(Arrays.asList(first, second));
        expect.setColumnAttributes(attributes);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefCheck_generateSuccees() {
        StatementFactory<TableElement> factory =
                new MySQLTableElementFactory(getTableElementContext("tb.col varchar(64) check(false)"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        InLineConstraint first = new InLineCheckConstraint(null, null, new ConstExpression("false"));
        ColumnAttributes attributes = new ColumnAttributes();
        attributes.setConstraints(Collections.singletonList(first));
        expect.setColumnAttributes(attributes);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefCheck1_generateSuccees() {
        StatementFactory<TableElement> factory =
                new MySQLTableElementFactory(getTableElementContext("tb.col varchar(64) constraint check(false)"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        InLineConstraint first = new InLineCheckConstraint(null, null, new ConstExpression("false"));
        ColumnAttributes attributes = new ColumnAttributes();
        attributes.setConstraints(Collections.singletonList(first));
        expect.setColumnAttributes(attributes);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefCheck2_generateSuccees() {
        StatementFactory<TableElement> factory =
                new MySQLTableElementFactory(getTableElementContext("tb.col varchar(64) constraint abcd check(false)"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        InLineConstraint first = new InLineCheckConstraint("abcd", null, new ConstExpression("false"));
        ColumnAttributes attributes = new ColumnAttributes();
        attributes.setConstraints(Collections.singletonList(first));
        expect.setColumnAttributes(attributes);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefCheck3_generateSuccees() {
        StatementFactory<TableElement> factory =
                new MySQLTableElementFactory(
                        getTableElementContext("tb.col varchar(64) constraint abcd check(false) not enforced"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ConstraintState state = new ConstraintState();
        state.setEnforced(false);
        InLineConstraint first = new InLineCheckConstraint("abcd", state, new ConstExpression("false"));
        ColumnAttributes attributes = new ColumnAttributes();
        attributes.setConstraints(Collections.singletonList(first));
        expect.setColumnAttributes(attributes);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_generatedColumnDefAsExpr_generateSuccees() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext("tb.col varchar(64) generated always as (tb.col+1) virtual"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ColumnReference r1 = new ColumnReference(null, "tb", "col");
        Expression e = new CompoundExpression(r1, new ConstExpression("1"), Operator.ADD);
        GenerateOption option = new GenerateOption(e);
        option.setType(Type.VIRTUAL);
        option.setGenerateOption("always");
        expect.setGenerateOption(option);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_generatedColumnDefAsExprStore_generateSuccees() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext("tb.col varchar(64) generated always as (tb.col+1) stored"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ColumnReference r1 = new ColumnReference(null, "tb", "col");
        Expression e = new CompoundExpression(r1, new ConstExpression("1"), Operator.ADD);
        GenerateOption option = new GenerateOption(e);
        option.setType(Type.STORED);
        option.setGenerateOption("always");
        expect.setGenerateOption(option);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_generatedColumnDefAsExprPri_generateSuccees() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext("tb.col varchar(64) generated always as (tb.col+1) virtual primary key"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ColumnReference r1 = new ColumnReference(null, "tb", "col");
        Expression e = new CompoundExpression(r1, new ConstExpression("1"), Operator.ADD);
        GenerateOption option = new GenerateOption(e);
        option.setType(Type.VIRTUAL);
        option.setGenerateOption("always");
        ColumnAttributes attributes = new ColumnAttributes();
        InLineConstraint attribute = new InLineConstraint(null, null);
        attribute.setPrimaryKey(true);
        attributes.setConstraints(Collections.singletonList(attribute));
        expect.setColumnAttributes(attributes);
        expect.setGenerateOption(option);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_generatedColumnDefAsExprUni_generateSuccees() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext("tb.col varchar(64) generated always as (tb.col+1) virtual unique"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ColumnReference r1 = new ColumnReference(null, "tb", "col");
        Expression e = new CompoundExpression(r1, new ConstExpression("1"), Operator.ADD);
        GenerateOption option = new GenerateOption(e);
        option.setType(Type.VIRTUAL);
        option.setGenerateOption("always");
        ColumnAttributes attributes = new ColumnAttributes();
        InLineConstraint attribute = new InLineConstraint(null, null);
        attribute.setUniqueKey(true);
        attributes.setConstraints(Collections.singletonList(attribute));
        expect.setColumnAttributes(attributes);
        expect.setGenerateOption(option);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_generatedColumnDefAsExprKey_generateSuccees() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext("tb.col varchar(64) generated always as (tb.col+1) virtual key"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ColumnReference r1 = new ColumnReference(null, "tb", "col");
        Expression e = new CompoundExpression(r1, new ConstExpression("1"), Operator.ADD);
        GenerateOption option = new GenerateOption(e);
        option.setType(Type.VIRTUAL);
        option.setGenerateOption("always");
        ColumnAttributes attributes = new ColumnAttributes();
        InLineConstraint constraint = new InLineConstraint(null, null);
        constraint.setPrimaryKey(true);
        attributes.setConstraints(Collections.singletonList(constraint));
        expect.setColumnAttributes(attributes);
        expect.setGenerateOption(option);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_generatedColumnDefAsComment_generateSuccees() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext("tb.col varchar(64) generated always as (tb.col+1) virtual comment 'aaaaa'"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ColumnReference r1 = new ColumnReference(null, "tb", "col");
        Expression e = new CompoundExpression(r1, new ConstExpression("1"), Operator.ADD);
        GenerateOption option = new GenerateOption(e);
        option.setType(Type.VIRTUAL);
        option.setGenerateOption("always");
        ColumnAttributes attributes = new ColumnAttributes();
        attributes.setComment("'aaaaa'");
        expect.setColumnAttributes(attributes);
        expect.setGenerateOption(option);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_generatedColumnDefAsID_generateSuccees() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext("tb.col varchar(64) generated always as (tb.col+1) virtual id 12"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ColumnReference r1 = new ColumnReference(null, "tb", "col");
        Expression e = new CompoundExpression(r1, new ConstExpression("1"), Operator.ADD);
        GenerateOption option = new GenerateOption(e);
        option.setType(Type.VIRTUAL);
        option.setGenerateOption("always");
        ColumnAttributes attributes = new ColumnAttributes();
        attributes.setId(12);
        expect.setColumnAttributes(attributes);
        expect.setGenerateOption(option);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_generatedColumnDefAsIDMultiAttrs_generateSuccees() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext("tb.col varchar(64) generated always as (tb.col+1) virtual not null id 12"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ColumnReference r1 = new ColumnReference(null, "tb", "col");
        Expression e = new CompoundExpression(r1, new ConstExpression("1"), Operator.ADD);
        GenerateOption option = new GenerateOption(e);
        option.setType(Type.VIRTUAL);
        option.setGenerateOption("always");
        InLineConstraint first = new InLineConstraint(null, null);
        first.setNullable(false);
        ColumnAttributes attributes = new ColumnAttributes();
        attributes.setConstraints(Collections.singletonList(first));
        attributes.setId(12);
        expect.setColumnAttributes(attributes);
        expect.setGenerateOption(option);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_generatedColumnDefAsSRIDMultiAttrs_generateSuccees() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext("tb.col varchar(64) generated always as (tb.col+1) virtual not null srid 12"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ColumnReference r1 = new ColumnReference(null, "tb", "col");
        Expression e = new CompoundExpression(r1, new ConstExpression("1"), Operator.ADD);
        GenerateOption option = new GenerateOption(e);
        option.setType(Type.VIRTUAL);
        option.setGenerateOption("always");
        InLineConstraint first = new InLineConstraint(null, null);
        first.setNullable(false);
        ColumnAttributes attributes = new ColumnAttributes();
        attributes.setConstraints(Collections.singletonList(first));
        attributes.setSrid(12);
        expect.setColumnAttributes(attributes);
        expect.setGenerateOption(option);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefCheckConstraint_generateSuccees() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext("tb.col varchar(64) generated always as (tb.col+1) stored check(true)"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ColumnReference r1 = new ColumnReference(null, "tb", "col");
        Expression e = new CompoundExpression(r1, new ConstExpression("1"), Operator.ADD);
        GenerateOption option = new GenerateOption(e);
        option.setType(Type.STORED);
        option.setGenerateOption("always");
        InLineConstraint first = new InLineCheckConstraint(null, null, new ConstExpression("true"));
        ColumnAttributes attributes = new ColumnAttributes();
        attributes.setConstraints(Collections.singletonList(first));
        expect.setColumnAttributes(attributes);
        expect.setGenerateOption(option);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefCheckConstraint1_generateSuccees() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext(
                        "tb.col varchar(64) generated always as (tb.col+1) stored constraint abcd check(true) enforced"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ColumnReference r1 = new ColumnReference(null, "tb", "col");
        Expression e = new CompoundExpression(r1, new ConstExpression("1"), Operator.ADD);
        GenerateOption option = new GenerateOption(e);
        option.setType(Type.STORED);
        option.setGenerateOption("always");
        ConstraintState state = new ConstraintState();
        state.setEnforced(true);
        InLineConstraint first = new InLineCheckConstraint("abcd", state, new ConstExpression("true"));
        ColumnAttributes attributes = new ColumnAttributes();
        attributes.setConstraints(Collections.singletonList(first));
        expect.setColumnAttributes(attributes);
        expect.setGenerateOption(option);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_indexBtree_succeed() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext("index idx_name using btree (col, col1) global with parser 'aaaa'"));
        OutOfLineIndex actual = (OutOfLineIndex) factory.generate();

        SortColumn s1 = new SortColumn(new ColumnReference(null, null, "col"));
        SortColumn s2 = new SortColumn(new ColumnReference(null, null, "col1"));
        OutOfLineIndex expect = new OutOfLineIndex("idx_name", Arrays.asList(s1, s2));
        IndexOptions indexOptions = new IndexOptions();
        indexOptions.setUsingBtree(true);
        indexOptions.setGlobal(true);
        indexOptions.setWithParser("'aaaa'");
        expect.setIndexOptions(indexOptions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_exprIndexBtree_succeed() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext(
                        "index idx_name using btree ((CASE a WHEN 1 THEN 11 WHEN 2 THEN 22 ELSE 33 END)) global with parser 'aaaa'"));
        OutOfLineIndex actual = (OutOfLineIndex) factory.generate();

        List<WhenClause> whenClauses = new ArrayList<>();
        whenClauses.add(new WhenClause(new ConstExpression("1"), new ConstExpression("11")));
        whenClauses.add(new WhenClause(new ConstExpression("2"), new ConstExpression("22")));
        CaseWhen caseWhen = new CaseWhen(whenClauses);
        caseWhen.setCaseValue(new ColumnReference(null, null, "a"));
        caseWhen.setCaseDefault(new ConstExpression("33"));
        SortColumn s = new SortColumn(caseWhen);
        OutOfLineIndex expect = new OutOfLineIndex("idx_name", Collections.singletonList(s));
        IndexOptions indexOptions = new IndexOptions();
        indexOptions.setUsingBtree(true);
        indexOptions.setGlobal(true);
        indexOptions.setWithParser("'aaaa'");
        expect.setIndexOptions(indexOptions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_indexHash_succeed() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext("index idx_name using hash (col, col1) local block_size=30"));
        OutOfLineIndex actual = (OutOfLineIndex) factory.generate();

        SortColumn s1 = new SortColumn(new ColumnReference(null, null, "col"));
        SortColumn s2 = new SortColumn(new ColumnReference(null, null, "col1"));
        OutOfLineIndex expect = new OutOfLineIndex("idx_name", Arrays.asList(s1, s2));
        IndexOptions indexOptions = new IndexOptions();
        indexOptions.setUsingHash(true);
        indexOptions.setGlobal(false);
        indexOptions.setBlockSize(30);
        expect.setIndexOptions(indexOptions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_indexHashPartitioned_succeed() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext("index idx_name (col, col1) partition by hash(col)"));
        OutOfLineIndex actual = (OutOfLineIndex) factory.generate();

        SortColumn s1 = new SortColumn(new ColumnReference(null, null, "col"));
        SortColumn s2 = new SortColumn(new ColumnReference(null, null, "col1"));
        OutOfLineIndex expect = new OutOfLineIndex("idx_name", Arrays.asList(s1, s2));
        HashPartition p =
                new HashPartition(Collections.singletonList(new ColumnReference(null, null, "col")), null, null, null);
        expect.setPartition(p);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_indexAutoPartitioned_succeed() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext(
                        "index idx_name (col, col1) partition by range columns(a,b) partition size 'auto' PARTITIONS AUTO"));
        OutOfLineIndex actual = (OutOfLineIndex) factory.generate();

        SortColumn s1 = new SortColumn(new ColumnReference(null, null, "col"));
        SortColumn s2 = new SortColumn(new ColumnReference(null, null, "col1"));
        OutOfLineIndex expect = new OutOfLineIndex("idx_name", Arrays.asList(s1, s2));
        RangePartition p = new RangePartition(Arrays.asList(
                new ColumnReference(null, null, "a"),
                new ColumnReference(null, null, "b")), null, null, null, true);
        p.setAuto(true);
        p.setPartitionSize(new ConstExpression("'auto'"));
        expect.setPartition(p);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_noNameIndex_succeed() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext("index (col, col1) data_table_id=12 virtual_column_id=13 max_used_part_id=14"));
        OutOfLineIndex actual = (OutOfLineIndex) factory.generate();

        SortColumn s1 = new SortColumn(new ColumnReference(null, null, "col"));
        SortColumn s2 = new SortColumn(new ColumnReference(null, null, "col1"));
        OutOfLineIndex expect = new OutOfLineIndex(null, Arrays.asList(s1, s2));
        IndexOptions indexOptions = new IndexOptions();
        indexOptions.setDataTableId(12);
        indexOptions.setVirtualColumnId(13);
        indexOptions.setMaxUsedPartId(14);
        expect.setIndexOptions(indexOptions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_indexColumnDescLenId_succeed() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext("index (col(13) desc id 16, col1) comment 'abcd' storing(a,b) noparallel"));
        OutOfLineIndex actual = (OutOfLineIndex) factory.generate();

        SortColumn s1 = new SortColumn(new ColumnReference(null, null, "col"));
        s1.setId(16);
        s1.setDirection(SortDirection.DESC);
        s1.setLength(13);
        SortColumn s2 = new SortColumn(new ColumnReference(null, null, "col1"));
        OutOfLineIndex expect = new OutOfLineIndex(null, Arrays.asList(s1, s2));
        IndexOptions indexOptions = new IndexOptions();
        indexOptions.setComment("'abcd'");
        indexOptions.setNoParallel(true);
        indexOptions
                .setStoring(Arrays.asList(new ColumnReference(null, null, "a"), new ColumnReference(null, null, "b")));
        expect.setIndexOptions(indexOptions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_indexColumnDescLen_succeed() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext("index (col(13) desc, col1) with rowid PARALLEL=12"));
        OutOfLineIndex actual = (OutOfLineIndex) factory.generate();

        SortColumn s1 = new SortColumn(new ColumnReference(null, null, "col"));
        s1.setDirection(SortDirection.DESC);
        s1.setLength(13);
        SortColumn s2 = new SortColumn(new ColumnReference(null, null, "col1"));
        OutOfLineIndex expect = new OutOfLineIndex(null, Arrays.asList(s1, s2));
        IndexOptions indexOptions = new IndexOptions();
        indexOptions.setWithRowId(true);
        indexOptions.setParallel(12);
        expect.setIndexOptions(indexOptions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_indexColumnAscId_succeed() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext("index (col asc id 16, col1) using btree visible"));
        OutOfLineIndex actual = (OutOfLineIndex) factory.generate();

        SortColumn s1 = new SortColumn(new ColumnReference(null, null, "col"));
        s1.setId(16);
        s1.setDirection(SortDirection.ASC);
        SortColumn s2 = new SortColumn(new ColumnReference(null, null, "col1"));
        OutOfLineIndex expect = new OutOfLineIndex(null, Arrays.asList(s1, s2));
        IndexOptions indexOptions = new IndexOptions();
        indexOptions.setUsingBtree(true);
        indexOptions.setVisible(true);
        expect.setIndexOptions(indexOptions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_indexPrimaryKey_succeed() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext("primary key `aaaa` using hash (col, col1) comment 'abcd'"));
        TableElement actual = factory.generate();

        SortColumn s1 = new SortColumn(new ColumnReference(null, null, "col"));
        SortColumn s2 = new SortColumn(new ColumnReference(null, null, "col1"));
        ConstraintState state = new ConstraintState();
        IndexOptions indexOptions = new IndexOptions();
        indexOptions.setUsingHash(true);
        indexOptions.setComment("'abcd'");
        state.setIndexOptions(indexOptions);
        OutOfLineConstraint expect = new OutOfLineConstraint(state, Arrays.asList(s1, s2));
        expect.setPrimaryKey(true);
        expect.setIndexName("`aaaa`");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_indexPrimaryKeyNoUsingHash_succeed() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext("constraint abcd primary key oop (col, col1) comment 'abcd'"));
        TableElement actual = factory.generate();

        SortColumn s1 = new SortColumn(new ColumnReference(null, null, "col"));
        SortColumn s2 = new SortColumn(new ColumnReference(null, null, "col1"));
        ConstraintState state = new ConstraintState();
        IndexOptions indexOptions = new IndexOptions();
        indexOptions.setComment("'abcd'");
        state.setIndexOptions(indexOptions);
        OutOfLineConstraint expect = new OutOfLineConstraint(state, Arrays.asList(s1, s2));
        expect.setPrimaryKey(true);
        expect.setConstraintName("abcd");
        expect.setIndexName("oop");
        Assert.assertEquals(expect, actual);
    }


    @Test
    public void generate_indexPrimaryKeyNoyAlgorithmAndComment_succeed() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext("primary key (col, col1)"));
        TableElement actual = factory.generate();

        SortColumn s1 = new SortColumn(new ColumnReference(null, null, "col"));
        SortColumn s2 = new SortColumn(new ColumnReference(null, null, "col1"));
        OutOfLineConstraint expect = new OutOfLineConstraint(null, Arrays.asList(s1, s2));
        expect.setPrimaryKey(true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_uniqueIndexColumnAscId_succeed() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext(
                        "unique index idx_name using btree (col asc id 16, col1) global with parser 'aaaa'"));
        TableElement actual = factory.generate();

        SortColumn s1 = new SortColumn(new ColumnReference(null, null, "col"));
        s1.setId(16);
        s1.setDirection(SortDirection.ASC);
        SortColumn s2 = new SortColumn(new ColumnReference(null, null, "col1"));
        ConstraintState state = new ConstraintState();
        IndexOptions indexOptions = new IndexOptions();
        indexOptions.setUsingBtree(true);
        indexOptions.setGlobal(true);
        indexOptions.setWithParser("'aaaa'");
        state.setIndexOptions(indexOptions);
        OutOfLineConstraint expect = new OutOfLineConstraint(state, Arrays.asList(s1, s2));
        expect.setUniqueKey(true);
        expect.setIndexName("idx_name");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_uniqueIndexHashPartition_succeed() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext("unique index idx_name (col asc id 16, col1) partition by hash(col)"));
        TableElement actual = factory.generate();

        SortColumn s1 = new SortColumn(new ColumnReference(null, null, "col"));
        s1.setId(16);
        s1.setDirection(SortDirection.ASC);
        SortColumn s2 = new SortColumn(new ColumnReference(null, null, "col1"));
        ConstraintState state = new ConstraintState();
        HashPartition p =
                new HashPartition(Collections.singletonList(new ColumnReference(null, null, "col")), null, null, null);
        state.setPartition(p);
        OutOfLineConstraint expect = new OutOfLineConstraint(state, Arrays.asList(s1, s2));
        expect.setUniqueKey(true);
        expect.setIndexName("idx_name");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_uniqueIndexAutoPartition_succeed() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext(
                        "unique index idx_name (col asc id 16, col1) partition by range columns(a,b) partition size 'auto' PARTITIONS AUTO"));
        TableElement actual = factory.generate();

        SortColumn s1 = new SortColumn(new ColumnReference(null, null, "col"));
        s1.setId(16);
        s1.setDirection(SortDirection.ASC);
        SortColumn s2 = new SortColumn(new ColumnReference(null, null, "col1"));
        ConstraintState state = new ConstraintState();
        RangePartition p = new RangePartition(Arrays.asList(
                new ColumnReference(null, null, "a"),
                new ColumnReference(null, null, "b")), null, null, null, true);
        p.setAuto(true);
        p.setPartitionSize(new ConstExpression("'auto'"));
        state.setPartition(p);
        OutOfLineConstraint expect = new OutOfLineConstraint(state, Arrays.asList(s1, s2));
        expect.setUniqueKey(true);
        expect.setIndexName("idx_name");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_uniqueIndexColumnAscIdNoIndexOps_succeed() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext("unique index idx_name (col asc id 16, col1) global with parser 'aaaa'"));
        TableElement actual = factory.generate();

        SortColumn s1 = new SortColumn(new ColumnReference(null, null, "col"));
        s1.setId(16);
        s1.setDirection(SortDirection.ASC);
        SortColumn s2 = new SortColumn(new ColumnReference(null, null, "col1"));
        ConstraintState state = new ConstraintState();
        IndexOptions indexOptions = new IndexOptions();
        indexOptions.setGlobal(true);
        indexOptions.setWithParser("'aaaa'");
        state.setIndexOptions(indexOptions);
        OutOfLineConstraint expect = new OutOfLineConstraint(state, Arrays.asList(s1, s2));
        expect.setUniqueKey(true);
        expect.setIndexName("idx_name");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_uniqueIndexColumnAscIdNoUsingBtree_succeed() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext("unique index (col asc id 16, col1)"));
        TableElement actual = factory.generate();

        SortColumn s1 = new SortColumn(new ColumnReference(null, null, "col"));
        s1.setId(16);
        s1.setDirection(SortDirection.ASC);
        SortColumn s2 = new SortColumn(new ColumnReference(null, null, "col1"));
        OutOfLineConstraint expect = new OutOfLineConstraint(null, Arrays.asList(s1, s2));
        expect.setUniqueKey(true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_fullTextIndexColumnAscId_succeed() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext("fulltext index using btree (col asc id 16, col1) invisible"));
        OutOfLineIndex actual = (OutOfLineIndex) factory.generate();

        SortColumn s1 = new SortColumn(new ColumnReference(null, null, "col"));
        s1.setId(16);
        s1.setDirection(SortDirection.ASC);
        SortColumn s2 = new SortColumn(new ColumnReference(null, null, "col1"));
        OutOfLineIndex expect = new OutOfLineIndex(null, Arrays.asList(s1, s2));
        IndexOptions indexOptions = new IndexOptions();
        indexOptions.setUsingBtree(true);
        indexOptions.setVisible(false);
        expect.setIndexOptions(indexOptions);
        expect.setFullText(true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_spatialIndexColumnAscId_succeed() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext("spatial index using btree (col asc id 16, col1) invisible"));
        OutOfLineIndex actual = (OutOfLineIndex) factory.generate();

        SortColumn s1 = new SortColumn(new ColumnReference(null, null, "col"));
        s1.setId(16);
        s1.setDirection(SortDirection.ASC);
        SortColumn s2 = new SortColumn(new ColumnReference(null, null, "col1"));
        OutOfLineIndex expect = new OutOfLineIndex(null, Arrays.asList(s1, s2));
        IndexOptions indexOptions = new IndexOptions();
        indexOptions.setUsingBtree(true);
        indexOptions.setVisible(false);
        expect.setIndexOptions(indexOptions);
        expect.setSpatial(true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_checkKey_succeed() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext("constraint ck_name check(col > 12)"));
        TableElement actual = factory.generate();

        ColumnReference r = new ColumnReference(null, null, "col");
        ConstExpression c1 = new ConstExpression("12");
        CompoundExpression checkExpr = new CompoundExpression(r, c1, Operator.GT);
        OutOfLineCheckConstraint expect = new OutOfLineCheckConstraint(null, checkExpr);
        expect.setConstraintName("ck_name");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_checkKeyEnforced_succeed() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext("constraint ck_name check(col > 12) enforced"));
        TableElement actual = factory.generate();

        ColumnReference r = new ColumnReference(null, null, "col");
        ConstExpression c1 = new ConstExpression("12");
        CompoundExpression checkExpr = new CompoundExpression(r, c1, Operator.GT);
        ConstraintState state = new ConstraintState();
        state.setEnforced(true);
        OutOfLineCheckConstraint expect = new OutOfLineCheckConstraint(state, checkExpr);
        expect.setConstraintName("ck_name");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_checkKeyNotEnforced_succeed() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext("constraint ck_name check(col > 12) not enforced"));
        TableElement actual = factory.generate();

        ColumnReference r = new ColumnReference(null, null, "col");
        ConstExpression c1 = new ConstExpression("12");
        CompoundExpression checkExpr = new CompoundExpression(r, c1, Operator.GT);
        ConstraintState state = new ConstraintState();
        state.setEnforced(false);
        OutOfLineCheckConstraint expect = new OutOfLineCheckConstraint(state, checkExpr);
        expect.setConstraintName("ck_name");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_foreignKeyIndexName_succeed() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext(
                        "foreign key fk_name(col, col1) references a.b (col2, col3) match simple on delete cascade on update set default"));
        TableElement actual = factory.generate();

        SortColumn s1 = new SortColumn(new ColumnReference(null, null, "col"));
        SortColumn s2 = new SortColumn(new ColumnReference(null, null, "col1"));
        ColumnReference r1 = new ColumnReference(null, null, "col2");
        ColumnReference r2 = new ColumnReference(null, null, "col3");
        ForeignReference reference = new ForeignReference("a", "b", Arrays.asList(r1, r2));
        reference.setDeleteOption(OnOption.CASCADE);
        reference.setUpdateOption(OnOption.SET_DEFAULT);
        reference.setMatchOption(MatchOption.SIMPLE);
        OutOfLineForeignConstraint expect = new OutOfLineForeignConstraint(null, Arrays.asList(s1, s2), reference);
        expect.setIndexName("fk_name");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_simpleForeignKey_succeed() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext("foreign key (col, col1) references a.b (col2, col3)"));
        TableElement actual = factory.generate();

        SortColumn s1 = new SortColumn(new ColumnReference(null, null, "col"));
        SortColumn s2 = new SortColumn(new ColumnReference(null, null, "col1"));
        ColumnReference r1 = new ColumnReference(null, null, "col2");
        ColumnReference r2 = new ColumnReference(null, null, "col3");
        ForeignReference reference = new ForeignReference("a", "b", Arrays.asList(r1, r2));
        OutOfLineForeignConstraint expect = new OutOfLineForeignConstraint(null, Arrays.asList(s1, s2), reference);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_indexWithColumnGroup_allColumns_succeed() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(getTableElementContext(
                "index idx_name (col) with column group(all columns)"));

        OutOfLineIndex actual = (OutOfLineIndex) factory.generate();

        SortColumn s1 = new SortColumn(new ColumnReference(null, null, "col"));
        OutOfLineIndex expect = new OutOfLineIndex("idx_name", Collections.singletonList(s1));
        expect.setColumnGroupElements(Collections.singletonList(new ColumnGroupElement(true, false)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_uniqueIndexWithColumnGroup_allColumns_succeed() {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(
                getTableElementContext(
                        "unique index idx_name (col) with column group(all columns)"));
        TableElement actual = factory.generate();

        SortColumn s1 = new SortColumn(new ColumnReference(null, null, "col"));
        OutOfLineConstraint expect = new OutOfLineConstraint(null, Collections.singletonList(s1));
        expect.setUniqueKey(true);
        expect.setIndexName("idx_name");
        Assert.assertEquals(expect, actual);
    }

    private Table_elementContext getTableElementContext(String str) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(str));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.table_element();
    }

}
