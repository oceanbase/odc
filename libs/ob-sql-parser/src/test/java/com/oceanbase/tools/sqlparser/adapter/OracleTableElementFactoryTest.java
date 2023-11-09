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
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.sqlparser.adapter.oracle.OracleTableElementFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBLexer;
import com.oceanbase.tools.sqlparser.oboracle.OBParser;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Table_elementContext;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.Operator;
import com.oceanbase.tools.sqlparser.statement.common.CharacterType;
import com.oceanbase.tools.sqlparser.statement.common.DataType;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnAttributes;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnDefinition;
import com.oceanbase.tools.sqlparser.statement.createtable.ConstraintState;
import com.oceanbase.tools.sqlparser.statement.createtable.ForeignReference;
import com.oceanbase.tools.sqlparser.statement.createtable.ForeignReference.OnOption;
import com.oceanbase.tools.sqlparser.statement.createtable.GenerateOption;
import com.oceanbase.tools.sqlparser.statement.createtable.GenerateOption.Type;
import com.oceanbase.tools.sqlparser.statement.createtable.InLineCheckConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.InLineConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.InLineForeignConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.IndexOptions;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineCheckConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineForeignConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineIndex;
import com.oceanbase.tools.sqlparser.statement.createtable.SortColumn;
import com.oceanbase.tools.sqlparser.statement.createtable.TableElement;
import com.oceanbase.tools.sqlparser.statement.expression.BoolValue;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.expression.CompoundExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ExpressionParam;
import com.oceanbase.tools.sqlparser.statement.expression.FunctionCall;
import com.oceanbase.tools.sqlparser.statement.expression.RelationReference;
import com.oceanbase.tools.sqlparser.statement.select.SortDirection;
import com.oceanbase.tools.sqlparser.statement.select.oracle.SortNullPosition;
import com.oceanbase.tools.sqlparser.statement.sequence.SequenceOptions;

/**
 * Test cases for {@link OracleTableElementFactory}
 *
 * @author yh263208
 * @date 2023-05-23 10:38
 * @since ODC_release_5.2.0
 */
public class OracleTableElementFactoryTest {

    @Test
    public void generate_columnDef_generateSuccees() {
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext("tb.col varchar2(64)"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar2", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefVisible_generateSuccees() {
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext("tb.col varchar2(64) visible"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar2", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        expect.setVisible(true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefInvisible_generateSuccees() {
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext("tb.col varchar2(64) invisible"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar2", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        expect.setVisible(false);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefNotNull_generateSuccees() {
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext("tb.col varchar2(64) not null"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar2", new BigDecimal("64"));
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
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext("tb.col varchar2(64) null"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar2", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ColumnAttributes attributes = new ColumnAttributes();
        InLineConstraint attribute = new InLineConstraint(null, null);
        attribute.setNullable(true);
        attributes.setConstraints(Collections.singletonList(attribute));
        expect.setColumnAttributes(attributes);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefConstraintName_generateSuccees() {
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext("tb.col varchar2(64) constraint abcd not null"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar2", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ColumnAttributes attributes = new ColumnAttributes();
        InLineConstraint attribute = new InLineConstraint("abcd", null);
        attribute.setNullable(false);
        attributes.setConstraints(Collections.singletonList(attribute));
        expect.setColumnAttributes(attributes);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefConstraintState_generateSuccees() {
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext(
                        "tb.col varchar2(64) constraint abcd not null rely using index global storing(a,b) enable novalidate"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar2", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ColumnAttributes attributes = new ColumnAttributes();
        ConstraintState state = new ConstraintState();
        state.setRely(true);
        state.setUsingIndexFlag(true);
        IndexOptions indexOptions = new IndexOptions();
        indexOptions
                .setStoring(Arrays.asList(new ColumnReference(null, null, "a"), new ColumnReference(null, null, "b")));
        indexOptions.setGlobal(true);
        state.setIndexOptions(indexOptions);
        state.setEnable(true);
        state.setValidate(false);
        InLineConstraint attribute = new InLineConstraint("abcd", state);
        attribute.setNullable(false);
        attributes.setConstraints(Collections.singletonList(attribute));
        expect.setColumnAttributes(attributes);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefConstraintState1_generateSuccees() {
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext(
                        "tb.col varchar2(64) constraint abcd not null norely using index disable validate"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar2", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ColumnAttributes attributes = new ColumnAttributes();
        ConstraintState state = new ConstraintState();
        state.setRely(false);
        state.setUsingIndexFlag(true);
        state.setEnable(false);
        state.setValidate(true);
        InLineConstraint attribute = new InLineConstraint("abcd", state);
        attribute.setNullable(false);
        attributes.setConstraints(Collections.singletonList(attribute));
        expect.setColumnAttributes(attributes);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefPrimaryKey_generateSuccees() {
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext("tb.col varchar2(64) primary key"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar2", new BigDecimal("64"));
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
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext("tb.col varchar2(64) constraint abcd unique"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar2", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ColumnAttributes attributes = new ColumnAttributes();
        InLineConstraint attribute = new InLineConstraint("abcd", null);
        attribute.setUniqueKey(true);
        attributes.setConstraints(Collections.singletonList(attribute));
        expect.setColumnAttributes(attributes);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefCheckConstraint_generateSuccees() {
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext(
                        "tb.col varchar2(64) constraint abcd check(true) rely using index global storing(a,b) enable novalidate"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar2", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ColumnAttributes attributes = new ColumnAttributes();
        ConstraintState state = new ConstraintState();
        state.setRely(true);
        state.setUsingIndexFlag(true);
        IndexOptions indexOptions = new IndexOptions();
        indexOptions
                .setStoring(Arrays.asList(new ColumnReference(null, null, "a"), new ColumnReference(null, null, "b")));
        indexOptions.setGlobal(true);
        state.setIndexOptions(indexOptions);
        state.setEnable(true);
        state.setValidate(false);
        InLineConstraint attribute = new InLineCheckConstraint("abcd", state, new BoolValue(true));
        attributes.setConstraints(Collections.singletonList(attribute));
        expect.setColumnAttributes(attributes);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefForeignConstraint_generateSuccees() {
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext(
                        "tb.col varchar2(64) references a.b (a,b,c) rely using index local block_size=12 enable novalidate"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar2", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ColumnAttributes attributes = new ColumnAttributes();
        ConstraintState state = new ConstraintState();
        state.setRely(true);
        state.setUsingIndexFlag(true);
        IndexOptions indexOptions = new IndexOptions();
        indexOptions.setBlockSize(12);
        indexOptions.setGlobal(false);
        state.setIndexOptions(indexOptions);
        state.setEnable(true);
        state.setValidate(false);
        ColumnReference r1 = new ColumnReference(null, null, "a");
        ColumnReference r2 = new ColumnReference(null, null, "b");
        ColumnReference r3 = new ColumnReference(null, null, "c");
        ForeignReference reference = new ForeignReference("a", "b", Arrays.asList(r1, r2, r3));
        InLineConstraint attribute = new InLineForeignConstraint(null, state, reference);
        attributes.setConstraints(Collections.singletonList(attribute));
        expect.setColumnAttributes(attributes);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefForeignConstraintDeleteCascade_generateSuccees() {
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext(
                        "tb.col varchar2(64) references a.b(a,b,c) on delete cascade rely using index comment 'abcd' with rowid enable novalidate"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar2", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ColumnAttributes attributes = new ColumnAttributes();
        ConstraintState state = new ConstraintState();
        state.setRely(true);
        state.setUsingIndexFlag(true);
        IndexOptions indexOptions = new IndexOptions();
        indexOptions.setComment("'abcd'");
        indexOptions.setWithRowId(true);
        state.setIndexOptions(indexOptions);
        state.setEnable(true);
        state.setValidate(false);
        ColumnReference r1 = new ColumnReference(null, null, "a");
        ColumnReference r2 = new ColumnReference(null, null, "b");
        ColumnReference r3 = new ColumnReference(null, null, "c");
        ForeignReference reference = new ForeignReference("a", "b", Arrays.asList(r1, r2, r3));
        reference.setDeleteOption(OnOption.CASCADE);
        InLineConstraint attribute = new InLineForeignConstraint(null, state, reference);
        attributes.setConstraints(Collections.singletonList(attribute));
        expect.setColumnAttributes(attributes);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefForeignConstraintDeleteSetNull_generateSuccees() {
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext(
                        "tb.col varchar2(64) references a.b(a,b,c) on delete set null rely using index with parser 'aaa' using btree enable novalidate"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar2", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ColumnAttributes attributes = new ColumnAttributes();
        ConstraintState state = new ConstraintState();
        state.setRely(true);
        state.setUsingIndexFlag(true);
        IndexOptions indexOptions = new IndexOptions();
        indexOptions.setWithParser("'aaa'");
        indexOptions.setUsingBtree(true);
        state.setIndexOptions(indexOptions);
        state.setEnable(true);
        state.setValidate(false);
        ColumnReference r1 = new ColumnReference(null, null, "a");
        ColumnReference r2 = new ColumnReference(null, null, "b");
        ColumnReference r3 = new ColumnReference(null, null, "c");
        ForeignReference reference = new ForeignReference("a", "b", Arrays.asList(r1, r2, r3));
        reference.setDeleteOption(OnOption.SET_NULL);
        InLineConstraint attribute = new InLineForeignConstraint(null, state, reference);
        attributes.setConstraints(Collections.singletonList(attribute));
        expect.setColumnAttributes(attributes);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefDefaultExpr_generateSuccees() {
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext("tb.col varchar2(64) default 1"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar2", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ColumnAttributes attributes = new ColumnAttributes();
        attributes.setDefaultValue(new ConstExpression("1"));
        expect.setColumnAttributes(attributes);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefOrigDefaultExpr_generateSuccees() {
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext("tb.col varchar2(64) orig_default((current_timestamp(1)))"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar2", new BigDecimal("64"));
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
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext("tb.col varchar2(64) orig_default(-12)"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar2", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ColumnAttributes attributes = new ColumnAttributes();
        attributes.setOrigDefault(new CompoundExpression(new ConstExpression("12"), null, Operator.SUB));
        expect.setColumnAttributes(attributes);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefId_generateSuccees() {
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext("tb.col varchar2(64) id 12"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar2", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        ColumnAttributes attributes = new ColumnAttributes();
        attributes.setId(12);
        expect.setColumnAttributes(attributes);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnDefMultiColumnAttrs_generateSuccees() {
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext("tb.col varchar2(64) not null primary key"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar2", new BigDecimal("64"));
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
    public void generate_generatedColumnDefAsExpr_generateSuccees() {
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext("tb.col varchar2(64) generated always as (tb.col+1) virtual"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar2", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        RelationReference r1 = new RelationReference("tb", new RelationReference("col", null));
        Expression e = new CompoundExpression(r1, new ConstExpression("1"), Operator.ADD);
        GenerateOption option = new GenerateOption(e);
        option.setType(Type.VIRTUAL);
        option.setGenerateOption("always");
        expect.setGenerateOption(option);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_generatedColumnCheck_generateSuccees() {
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext("tb.col varchar2(64) generated always as (tb.col+1) virtual check(1)"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar2", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        RelationReference r1 = new RelationReference("tb", new RelationReference("col", null));
        Expression e = new CompoundExpression(r1, new ConstExpression("1"), Operator.ADD);
        GenerateOption option = new GenerateOption(e);
        option.setType(Type.VIRTUAL);
        option.setGenerateOption("always");
        expect.setGenerateOption(option);
        ColumnAttributes attributes = new ColumnAttributes();
        attributes.setConstraints(
                Collections.singletonList(new InLineCheckConstraint(null, null, new ConstExpression("1"))));
        expect.setColumnAttributes(attributes);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_generatedColumnDefAsExprConstraint_generateSuccees() {
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext(
                        "tb.col varchar2(64) generated always as (tb.col+1) virtual constraint abc not null rely"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar2", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        RelationReference r1 = new RelationReference("tb", new RelationReference("col", null));
        Expression e = new CompoundExpression(r1, new ConstExpression("1"), Operator.ADD);
        GenerateOption option = new GenerateOption(e);
        option.setType(Type.VIRTUAL);
        option.setGenerateOption("always");
        ColumnAttributes attributes = new ColumnAttributes();
        ConstraintState state = new ConstraintState();
        state.setRely(true);
        InLineConstraint attribute = new InLineConstraint("abc", state);
        attribute.setNullable(false);
        attributes.setConstraints(Collections.singletonList(attribute));
        expect.setColumnAttributes(attributes);
        expect.setGenerateOption(option);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_generatedColumnDefAsExprPri_generateSuccees() {
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext("tb.col varchar2(64) generated always as (tb.col+1) virtual primary key"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar2", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        RelationReference r1 = new RelationReference("tb", new RelationReference("col", null));
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
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext("tb.col varchar2(64) generated always as (tb.col+1) virtual unique"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar2", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        RelationReference r1 = new RelationReference("tb", new RelationReference("col", null));
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
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext("tb.col varchar2(64) generated always as (tb.col+1) virtual key"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar2", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        RelationReference r1 = new RelationReference("tb", new RelationReference("col", null));
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
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext("tb.col varchar2(64) generated always as (tb.col+1) virtual comment 'aaaaa'"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar2", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        RelationReference r1 = new RelationReference("tb", new RelationReference("col", null));
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
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext("tb.col varchar2(64) generated always as (tb.col+1) virtual id 12"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar2", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        RelationReference r1 = new RelationReference("tb", new RelationReference("col", null));
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
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext("tb.col varchar2(64) generated always as (tb.col+1) virtual not null id 12"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar2", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        RelationReference r1 = new RelationReference("tb", new RelationReference("col", null));
        Expression e = new CompoundExpression(r1, new ConstExpression("1"), Operator.ADD);
        GenerateOption option = new GenerateOption(e);
        option.setType(Type.VIRTUAL);
        option.setGenerateOption("always");
        InLineConstraint first = new InLineConstraint(null, null);
        first.setNullable(false);
        ColumnAttributes attributes = new ColumnAttributes();
        attributes.setId(12);
        attributes.setConstraints(Collections.singletonList(first));
        expect.setColumnAttributes(attributes);
        expect.setGenerateOption(option);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_generatedColumnDefAsIdentity_generateSuccees() {
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext("tb.col varchar2(64) generated always as identity "
                        + "cycle minvalue -12 increment by 13 maxvalue 15 start with 1 "
                        + "nomaxvalue nominvalue cycle nocycle cache 13 "
                        + "nocache order noorder"));
        ColumnDefinition actual = (ColumnDefinition) factory.generate();

        DataType dataType = new CharacterType("varchar2", new BigDecimal("64"));
        ColumnDefinition expect = new ColumnDefinition(new ColumnReference(null, "tb", "col"), dataType);
        SequenceOptions options = new SequenceOptions();
        options.setCycle(true);
        options.setMinValue(new BigDecimal("-12"));
        options.setIncrementBy(new BigDecimal("13"));
        options.setMaxValue(new BigDecimal("15"));
        options.setStartWith(new BigDecimal("1"));
        options.setNoMaxValue(true);
        options.setNoMinValue(true);
        options.setCycle(true);
        options.setNoCycle(true);
        options.setCache(new BigDecimal("13"));
        options.setNoCache(true);
        options.setOrder(true);
        options.setNoOrder(true);
        GenerateOption option = new GenerateOption(options);
        option.setGenerateOption("always");
        expect.setGenerateOption(option);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_indexBtree_succeed() {
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext(
                        "index idx_name using btree (col, col1) visible data_table_id 14 index_table_id=15 max_used_part_id=16"));
        OutOfLineIndex actual = (OutOfLineIndex) factory.generate();

        SortColumn s1 = new SortColumn(new RelationReference("col", null));
        SortColumn s2 = new SortColumn(new RelationReference("col1", null));
        OutOfLineIndex expect = new OutOfLineIndex("idx_name", Arrays.asList(s1, s2));
        IndexOptions indexOptions = new IndexOptions();
        indexOptions.setVisible(true);
        indexOptions.setUsingBtree(true);
        indexOptions.setDataTableId(14);
        indexOptions.setIndexTableId(15);
        indexOptions.setMaxUsedPartId(16);
        expect.setIndexOptions(indexOptions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_indexHash_succeed() {
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext("index idx_name using hash (col, col1) reverse parallel=12"));
        OutOfLineIndex actual = (OutOfLineIndex) factory.generate();

        SortColumn s1 = new SortColumn(new RelationReference("col", null));
        SortColumn s2 = new SortColumn(new RelationReference("col1", null));
        OutOfLineIndex expect = new OutOfLineIndex("idx_name", Arrays.asList(s1, s2));
        IndexOptions indexOptions = new IndexOptions();
        indexOptions.setUsingHash(true);
        indexOptions.setParallel(12);
        indexOptions.setReverse(true);
        expect.setIndexOptions(indexOptions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_noNameIndex_succeed() {
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext("index (col, col1) noparallel pctfree 12"));
        OutOfLineIndex actual = (OutOfLineIndex) factory.generate();

        SortColumn s1 = new SortColumn(new RelationReference("col", null));
        SortColumn s2 = new SortColumn(new RelationReference("col1", null));
        OutOfLineIndex expect = new OutOfLineIndex(null, Arrays.asList(s1, s2));
        IndexOptions indexOptions = new IndexOptions();
        indexOptions.setNoParallel(true);
        indexOptions.setPctFree(12);
        expect.setIndexOptions(indexOptions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_indexColumnDescNullsLast_succeed() {
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext("index (col desc nulls last id 15, col1) pctused 14 tablespace abcd"));
        OutOfLineIndex actual = (OutOfLineIndex) factory.generate();

        SortColumn s1 = new SortColumn(new RelationReference("col", null));
        s1.setId(15);
        s1.setDirection(SortDirection.DESC);
        s1.setNullPosition(SortNullPosition.LAST);
        SortColumn s2 = new SortColumn(new RelationReference("col1", null));
        OutOfLineIndex expect = new OutOfLineIndex(null, Arrays.asList(s1, s2));
        IndexOptions indexOptions = new IndexOptions();
        indexOptions.setPctUsed(14);
        indexOptions.setTableSpace("abcd");
        expect.setIndexOptions(indexOptions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_indexColumnAscNullsLast_succeed() {
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext(
                        "index (col asc nulls last id 15, col1) initrans 13 maxtrans 14 storage(next 10 initial 13)"));
        OutOfLineIndex actual = (OutOfLineIndex) factory.generate();

        SortColumn s1 = new SortColumn(new RelationReference("col", null));
        s1.setId(15);
        s1.setDirection(SortDirection.ASC);
        s1.setNullPosition(SortNullPosition.LAST);
        SortColumn s2 = new SortColumn(new RelationReference("col1", null));
        OutOfLineIndex expect = new OutOfLineIndex(null, Arrays.asList(s1, s2));
        IndexOptions indexOptions = new IndexOptions();
        indexOptions.setIniTrans(13);
        indexOptions.setMaxTrans(14);
        indexOptions.setStorage(Arrays.asList("next 10", "initial 13"));
        expect.setIndexOptions(indexOptions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_indexPrimaryKey_succeed() {
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext("primary key (col, col1) using index using btree comment 'abcd'"));
        OutOfLineConstraint actual = (OutOfLineConstraint) factory.generate();

        SortColumn s1 = new SortColumn(new ColumnReference(null, null, "col"));
        SortColumn s2 = new SortColumn(new ColumnReference(null, null, "col1"));
        ConstraintState state = new ConstraintState();
        IndexOptions indexOptions = new IndexOptions();
        indexOptions.setUsingBtree(true);
        indexOptions.setComment("'abcd'");
        state.setIndexOptions(indexOptions);
        state.setUsingIndexFlag(true);
        OutOfLineConstraint expect = new OutOfLineConstraint(state, Arrays.asList(s1, s2));
        expect.setPrimaryKey(true);
        Assert.assertEquals(expect, actual);
    }

    @Test(expected = ParseCancellationException.class)
    public void generate_indexPrimaryKeyWithoutUsingIndex_wrong() {
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext("primary key (col, col1) using btree comment 'abcd'"));
        factory.generate();
    }

    @Test
    public void generate_indexPrimaryKeyWithConstraintName_succeed() {
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext("constraint pk_name primary key (col, col1) using index"));
        OutOfLineConstraint actual = (OutOfLineConstraint) factory.generate();

        SortColumn s1 = new SortColumn(new ColumnReference(null, null, "col"));
        SortColumn s2 = new SortColumn(new ColumnReference(null, null, "col1"));
        ConstraintState state = new ConstraintState();
        state.setUsingIndexFlag(true);
        OutOfLineConstraint expect = new OutOfLineConstraint(state, Arrays.asList(s1, s2));
        expect.setConstraintName("pk_name");
        expect.setPrimaryKey(true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_indexPrimaryKeyWithoutIndex_succeed() {
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext("constraint pk_name primary key (col, col1)"));
        OutOfLineConstraint actual = (OutOfLineConstraint) factory.generate();

        SortColumn s1 = new SortColumn(new ColumnReference(null, null, "col"));
        SortColumn s2 = new SortColumn(new ColumnReference(null, null, "col1"));
        OutOfLineConstraint expect = new OutOfLineConstraint(null, Arrays.asList(s1, s2));
        expect.setConstraintName("pk_name");
        expect.setPrimaryKey(true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_indexPrimaryKeyNoyAlgorithmAndComment_succeed() {
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext("primary key (col, col1)"));
        OutOfLineConstraint actual = (OutOfLineConstraint) factory.generate();

        SortColumn s1 = new SortColumn(new ColumnReference(null, null, "col"));
        SortColumn s2 = new SortColumn(new ColumnReference(null, null, "col1"));
        OutOfLineConstraint expect = new OutOfLineConstraint(null, Arrays.asList(s1, s2));
        expect.setPrimaryKey(true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_uniqueIndexColumnAscId_succeed() {
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext(
                        "unique (col asc id 16, col1) using index using btree global with parser 'aaaa'"));
        OutOfLineConstraint actual = (OutOfLineConstraint) factory.generate();

        SortColumn s1 = new SortColumn(new RelationReference("col", null));
        s1.setId(16);
        s1.setDirection(SortDirection.ASC);
        SortColumn s2 = new SortColumn(new RelationReference("col1", null));
        ConstraintState state = new ConstraintState();
        IndexOptions indexOptions = new IndexOptions();
        indexOptions.setUsingBtree(true);
        indexOptions.setGlobal(true);
        indexOptions.setWithParser("'aaaa'");
        state.setIndexOptions(indexOptions);
        state.setUsingIndexFlag(true);
        OutOfLineConstraint expect = new OutOfLineConstraint(state, Arrays.asList(s1, s2));
        expect.setUniqueKey(true);
        Assert.assertEquals(expect, actual);
    }

    @Test(expected = ParseCancellationException.class)
    public void generate_uniqueIndexWithoutUsingIndex_wrong() {
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext("unique (col asc id 16, col1) using btree global with parser 'aaaa'"));
        factory.generate();
    }

    @Test
    public void generate_foreignKey_succeed() {
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext(
                        "constraint fk_name foreign key (col, col1) references tb_name (col2, col3) on delete cascade using index using btree global with parser 'aaaa'"));
        TableElement actual = factory.generate();

        SortColumn s1 = new SortColumn(new ColumnReference(null, null, "col"));
        SortColumn s2 = new SortColumn(new ColumnReference(null, null, "col1"));
        ColumnReference r1 = new ColumnReference(null, null, "col2");
        ColumnReference r2 = new ColumnReference(null, null, "col3");
        ForeignReference reference = new ForeignReference(null, "tb_name", Arrays.asList(r1, r2));
        reference.setDeleteOption(OnOption.CASCADE);
        ConstraintState state = new ConstraintState();
        IndexOptions indexOptions = new IndexOptions();
        indexOptions.setUsingBtree(true);
        indexOptions.setGlobal(true);
        indexOptions.setWithParser("'aaaa'");
        state.setIndexOptions(indexOptions);
        state.setUsingIndexFlag(true);
        OutOfLineForeignConstraint expect = new OutOfLineForeignConstraint(state, Arrays.asList(s1, s2), reference);
        expect.setConstraintName("fk_name");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_checkKey_succeed() {
        StatementFactory<TableElement> factory = new OracleTableElementFactory(
                getTableElementContext(
                        "constraint ck_name check(col > 12) using index using btree global with parser 'aaaa'"));
        TableElement actual = factory.generate();

        RelationReference r = new RelationReference("col", null);
        ConstExpression c1 = new ConstExpression("12");
        CompoundExpression checkExpr = new CompoundExpression(r, c1, Operator.GT);
        ConstraintState state = new ConstraintState();
        IndexOptions indexOptions = new IndexOptions();
        indexOptions.setUsingBtree(true);
        indexOptions.setGlobal(true);
        indexOptions.setWithParser("'aaaa'");
        state.setIndexOptions(indexOptions);
        state.setUsingIndexFlag(true);
        OutOfLineCheckConstraint expect = new OutOfLineCheckConstraint(state, checkExpr);
        expect.setConstraintName("ck_name");
        Assert.assertEquals(expect, actual);
    }

    private Table_elementContext getTableElementContext(String columnDef) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(columnDef));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.table_element();
    }

}
