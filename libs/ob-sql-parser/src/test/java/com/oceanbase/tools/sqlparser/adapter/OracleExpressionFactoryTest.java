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

import com.oceanbase.tools.sqlparser.adapter.oracle.OracleExpressionFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBLexer;
import com.oceanbase.tools.sqlparser.oboracle.OBParser;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Bit_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.ExprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.PredicateContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Xml_functionContext;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.Expression.ReferenceOperator;
import com.oceanbase.tools.sqlparser.statement.Operator;
import com.oceanbase.tools.sqlparser.statement.common.CharacterType;
import com.oceanbase.tools.sqlparser.statement.common.GeneralDataType;
import com.oceanbase.tools.sqlparser.statement.common.NumberType;
import com.oceanbase.tools.sqlparser.statement.common.WindowBody;
import com.oceanbase.tools.sqlparser.statement.common.WindowOffset;
import com.oceanbase.tools.sqlparser.statement.common.WindowOffsetType;
import com.oceanbase.tools.sqlparser.statement.common.WindowSpec;
import com.oceanbase.tools.sqlparser.statement.common.WindowType;
import com.oceanbase.tools.sqlparser.statement.common.oracle.KeepClause;
import com.oceanbase.tools.sqlparser.statement.expression.BoolValue;
import com.oceanbase.tools.sqlparser.statement.expression.CaseWhen;
import com.oceanbase.tools.sqlparser.statement.expression.CollectionExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.expression.CompoundExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ExpressionParam;
import com.oceanbase.tools.sqlparser.statement.expression.FullTextSearch;
import com.oceanbase.tools.sqlparser.statement.expression.FunctionCall;
import com.oceanbase.tools.sqlparser.statement.expression.FunctionParam;
import com.oceanbase.tools.sqlparser.statement.expression.JsonConstraint;
import com.oceanbase.tools.sqlparser.statement.expression.JsonConstraint.ScalarsMode;
import com.oceanbase.tools.sqlparser.statement.expression.JsonConstraint.StrictMode;
import com.oceanbase.tools.sqlparser.statement.expression.JsonConstraint.UniqueMode;
import com.oceanbase.tools.sqlparser.statement.expression.JsonConstraint.WrapperMode;
import com.oceanbase.tools.sqlparser.statement.expression.JsonKeyValue;
import com.oceanbase.tools.sqlparser.statement.expression.JsonOnOption;
import com.oceanbase.tools.sqlparser.statement.expression.JsonOnOption.OnMismatch;
import com.oceanbase.tools.sqlparser.statement.expression.NullExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ParamWithAssign;
import com.oceanbase.tools.sqlparser.statement.expression.RelationReference;
import com.oceanbase.tools.sqlparser.statement.expression.TextSearchMode;
import com.oceanbase.tools.sqlparser.statement.expression.WhenClause;
import com.oceanbase.tools.sqlparser.statement.select.FromReference;
import com.oceanbase.tools.sqlparser.statement.select.NameReference;
import com.oceanbase.tools.sqlparser.statement.select.OrderBy;
import com.oceanbase.tools.sqlparser.statement.select.Projection;
import com.oceanbase.tools.sqlparser.statement.select.Select;
import com.oceanbase.tools.sqlparser.statement.select.SelectBody;
import com.oceanbase.tools.sqlparser.statement.select.SortDirection;
import com.oceanbase.tools.sqlparser.statement.select.SortKey;

/**
 * Test cases for {@link OracleExpressionFactory}
 *
 * @author yh263208
 * @date 2022-12-05 15:19
 * @since ODC_release_4.1.0
 */
public class OracleExpressionFactoryTest {

    @Test
    public void generate_TrueExpressionInput_generateBoolValueSucceed() {
        Bit_exprContext context = getBitExprContext("True");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();
        BoolValue expect = new BoolValue(true);

        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_plSqlVariable_generateBoolValueSucceed() {
        Bit_exprContext context = getBitExprContext("$$abcd");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();
        Expression expect = new ConstExpression("$$abcd");

        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_sum_generateBoolValueSucceed() {
        Bit_exprContext context = getBitExprContext("sum(tab.col)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        RelationReference r = new RelationReference("tab", new RelationReference("col", null));
        FunctionCall expect = new FunctionCall("sum", Collections.singletonList(new ExpressionParam(r)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_multisetSelect_generateSucceed() {
        Bit_exprContext context = getBitExprContext("multiset(select 1 from dual)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        Projection projection = new Projection(new ConstExpression("1"), null);
        FromReference from = new NameReference(null, "dual", null);
        SelectBody selectBody = new SelectBody(Collections.singletonList(projection), Collections.singletonList(from));

        FunctionCall expect = new FunctionCall("multiset", Collections.singletonList(new ExpressionParam(selectBody)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_notationPath_generateSucceed() {
        Bit_exprContext context = getBitExprContext("a.b[*, 1, 1 to 2]");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        RelationReference expect = new RelationReference("a", null);
        CollectionExpression exprs = new CollectionExpression();
        exprs.addExpression(new ConstExpression("*"));
        exprs.addExpression(new ConstExpression("1"));
        exprs.addExpression(new CompoundExpression(new ConstExpression("1"), new ConstExpression("2"), Operator.TO));
        expect.reference(new RelationReference("b", null), ReferenceOperator.DOT)
                .reference(exprs, ReferenceOperator.BRACKET);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_notationPathTwice_generateSucceed() {
        Bit_exprContext context = getBitExprContext("a.b[*, 1, 1 to 2][1,2]");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        RelationReference expect = new RelationReference("a", null);
        CollectionExpression exprs = new CollectionExpression();
        exprs.addExpression(new ConstExpression("*"));
        exprs.addExpression(new ConstExpression("1"));
        exprs.addExpression(new CompoundExpression(new ConstExpression("1"), new ConstExpression("2"), Operator.TO));
        CollectionExpression exprs1 = new CollectionExpression();
        exprs1.addExpression(new ConstExpression("1"));
        exprs1.addExpression(new ConstExpression("2"));
        expect.reference(new RelationReference("b", null), ReferenceOperator.DOT)
                .reference(exprs, ReferenceOperator.BRACKET).reference(exprs1, ReferenceOperator.BRACKET);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_notationPathTwiceObjAccess_generateSucceed() {
        Bit_exprContext context = getBitExprContext("a.b[*, 1, 1 to 2][1,2].ab.v");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        RelationReference expect = new RelationReference("a", null);
        CollectionExpression exprs = new CollectionExpression();
        exprs.addExpression(new ConstExpression("*"));
        exprs.addExpression(new ConstExpression("1"));
        exprs.addExpression(new CompoundExpression(new ConstExpression("1"), new ConstExpression("2"), Operator.TO));
        CollectionExpression exprs1 = new CollectionExpression();
        exprs1.addExpression(new ConstExpression("1"));
        exprs1.addExpression(new ConstExpression("2"));
        expect.reference(new RelationReference("b", null), ReferenceOperator.DOT)
                .reference(exprs, ReferenceOperator.BRACKET).reference(exprs1, ReferenceOperator.BRACKET)
                .reference(new RelationReference("ab", null), ReferenceOperator.DOT)
                .reference(new RelationReference("v", null), ReferenceOperator.DOT);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_notationPathObjAccess_generateSucceed() {
        Bit_exprContext context = getBitExprContext("a.b[*, 1, 1 to 2].ab.v");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        RelationReference expect = new RelationReference("a", null);
        CollectionExpression exprs = new CollectionExpression();
        exprs.addExpression(new ConstExpression("*"));
        exprs.addExpression(new ConstExpression("1"));
        exprs.addExpression(new CompoundExpression(new ConstExpression("1"), new ConstExpression("2"), Operator.TO));
        expect.reference(new RelationReference("b", null), ReferenceOperator.DOT)
                .reference(exprs, ReferenceOperator.BRACKET)
                .reference(new RelationReference("ab", null), ReferenceOperator.DOT)
                .reference(new RelationReference("v", null), ReferenceOperator.DOT);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_exists_generateBoolValueSucceed() {
        Bit_exprContext context = getBitExprContext("exists(tab.col)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        RelationReference r = new RelationReference("tab", new RelationReference("col", null));
        FunctionCall expect = new FunctionCall("exists", Collections.singletonList(new ExpressionParam(r)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_dblinkFunc_generateSucceed() {
        Bit_exprContext context = getBitExprContext("a@abc()");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionCall expect = new FunctionCall("a", Collections.emptyList());
        expect.setUserVariable("@abc");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_dblinkFunc1_generateSucceed() {
        Bit_exprContext context = getBitExprContext("a.b.c@abc(1,2)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionCall fCall = new FunctionCall("c", Arrays.asList(
                new ExpressionParam(new ConstExpression("1")),
                new ExpressionParam(new ConstExpression("2"))));
        fCall.setUserVariable("@abc");
        RelationReference expect = new RelationReference("a", new RelationReference("b", fCall));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_FalseExpressionInput_generateBoolValueSucceed() {
        Bit_exprContext context = getBitExprContext("false");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();
        BoolValue expect = new BoolValue(false);

        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_refColumn_generateRelationReferenceSucceed() {
        Bit_exprContext context = getBitExprContext("col");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();
        RelationReference expect = new RelationReference("col", null);

        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_refColumnStar_generateRelationReferenceSucceed() {
        Bit_exprContext context = getBitExprContext("col.*");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();
        RelationReference expect = new RelationReference("col", new RelationReference("*", null));

        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_refSchemaColumn_generateRelationReferenceSucceed() {
        Bit_exprContext context = getBitExprContext("tableName.colName");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();
        RelationReference expect = new RelationReference("tableName", new RelationReference("colName", null));

        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_refColumnFIRST_generateRelationReferenceSucceed() {
        Bit_exprContext context = getBitExprContext("colName.first()");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionCall functionCall = new FunctionCall("first", new ArrayList<>());
        RelationReference expect = new RelationReference("colName", functionCall);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_refColumnLast_generateRelationReferenceSucceed() {
        Bit_exprContext context = getBitExprContext("colName.Last()");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionCall functionCall = new FunctionCall("Last", new ArrayList<>());
        RelationReference expect = new RelationReference("colName", functionCall);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessCOUNTStartExists_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("COUNT(*)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        ExpressionParam param = new ExpressionParam(new ConstExpression("*"));
        FunctionCall expect = new FunctionCall("COUNT", Collections.singletonList(param));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessCOUNTColumnRefExists_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("COUNT(tableName.colName)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        RelationReference ref = new RelationReference("tableName", new RelationReference("colName", null));
        ExpressionParam param = new ExpressionParam(ref);
        FunctionCall expect = new FunctionCall("COUNT", Collections.singletonList(param));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessCOUNTAllColumnRefExists_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("COUNT(ALL tableName.colName)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        RelationReference ref = new RelationReference("tableName", new RelationReference("colName", null));
        ExpressionParam param = new ExpressionParam(ref);
        FunctionCall expect = new FunctionCall("COUNT", Collections.singletonList(param));
        expect.addOption(new ConstExpression("ALL"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessCOUNTDistinctColumnRefExists_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("COUNT(distinct tableName.colName)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        RelationReference ref = new RelationReference("tableName", new RelationReference("colName", null));
        ExpressionParam param = new ExpressionParam(ref);
        FunctionCall expect = new FunctionCall("COUNT", Collections.singletonList(param));
        expect.addOption(new ConstExpression("distinct"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessCOUNTUniqueColumnRefExists_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("COUNT(unique tableName.colName)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        RelationReference ref = new RelationReference("tableName", new RelationReference("colName", null));
        ExpressionParam param = new ExpressionParam(ref);
        FunctionCall expect = new FunctionCall("COUNT", Collections.singletonList(param));
        expect.addOption(new ConstExpression("unique"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessFunctionWithoutParams_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("function()");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionCall expect = new FunctionCall("function", new ArrayList<>());
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessFunctionWithTrueParamExpr_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("function(true)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        ExpressionParam param = new ExpressionParam(new BoolValue(true));
        FunctionCall expect = new FunctionCall("function", Collections.singletonList(param));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessFunctionWithServalLiteralExprs_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("function(a,3,4,5,6)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        ExpressionParam param1 = new ExpressionParam(new RelationReference("a", null));
        ExpressionParam param2 = new ExpressionParam(new ConstExpression("3"));
        ExpressionParam param3 = new ExpressionParam(new ConstExpression("4"));
        ExpressionParam param4 = new ExpressionParam(new ConstExpression("5"));
        ExpressionParam param5 = new ExpressionParam(new ConstExpression("6"));
        FunctionCall expect = new FunctionCall("function", Arrays.asList(param1, param2, param3, param4, param5));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessFunctionWithIsNullPredicate_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("function(a is null, b is not nan_value)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        ExpressionParam param1 = new ExpressionParam(
                new CompoundExpression(new RelationReference("a", null), new NullExpression(), Operator.EQ));
        ExpressionParam param2 = new ExpressionParam(
                new CompoundExpression(new RelationReference("b", null), new NullExpression(), Operator.NE));
        FunctionCall expect = new FunctionCall("function", Arrays.asList(param1, param2));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessFunctionWitha_EQ_bPredicate_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("function(a=b)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        ExpressionParam param1 = new ExpressionParam(
                new CompoundExpression(new RelationReference("a", null), new RelationReference("b", null),
                        Operator.EQ));
        FunctionCall expect = new FunctionCall("function", Collections.singletonList(param1));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessFunctionWitha_NE_bPredicate_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("function(a!=b)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        ExpressionParam param1 = new ExpressionParam(
                new CompoundExpression(new RelationReference("a", null), new RelationReference("b", null),
                        Operator.NE));
        FunctionCall expect = new FunctionCall("function", Collections.singletonList(param1));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessFunctionWitha_LT__GT_bPredicate_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("function(a <   > b)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        ExpressionParam param1 = new ExpressionParam(
                new CompoundExpression(new RelationReference("a", null), new RelationReference("b", null),
                        Operator.NE));
        FunctionCall expect = new FunctionCall("function", Collections.singletonList(param1));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessFunctionWitha_Caret__EQ_bPredicate_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("function(a ^   = b)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        ExpressionParam param1 = new ExpressionParam(
                new CompoundExpression(new RelationReference("a", null), new RelationReference("b", null),
                        Operator.NE));
        FunctionCall expect = new FunctionCall("function", Collections.singletonList(param1));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessFunctionWitha_Not__EQ_bPredicate_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("function(a !   = b)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        ExpressionParam param1 = new ExpressionParam(
                new CompoundExpression(new RelationReference("a", null), new RelationReference("b", null),
                        Operator.NE));
        FunctionCall expect = new FunctionCall("function", Collections.singletonList(param1));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessFunctionWitha_NE_PL_bPredicate_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("function(a ~= b)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        ExpressionParam param1 = new ExpressionParam(
                new CompoundExpression(new RelationReference("a", null), new RelationReference("b", null),
                        Operator.NE_PL));
        FunctionCall expect = new FunctionCall("function", Collections.singletonList(param1));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessFunctionWitha_GT__EQ_bPredicate_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("function(a >  = b)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        ExpressionParam param1 = new ExpressionParam(
                new CompoundExpression(new RelationReference("a", null), new RelationReference("b", null),
                        Operator.GE));
        FunctionCall expect = new FunctionCall("function", Collections.singletonList(param1));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessFunctionWitha_LT__EQ_bPredicate_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("function(a <  = b)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        ExpressionParam param1 = new ExpressionParam(
                new CompoundExpression(new RelationReference("a", null), new RelationReference("b", null),
                        Operator.LE));
        FunctionCall expect = new FunctionCall("function", Collections.singletonList(param1));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessFunctionWitha_GE_bPredicate_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("function(a >= b)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        ExpressionParam param1 = new ExpressionParam(
                new CompoundExpression(new RelationReference("a", null), new RelationReference("b", null),
                        Operator.GE));
        FunctionCall expect = new FunctionCall("function", Collections.singletonList(param1));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessFunctionWitha_LE_bPredicate_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("function(a <= b)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        ExpressionParam param1 = new ExpressionParam(
                new CompoundExpression(new RelationReference("a", null), new RelationReference("b", null),
                        Operator.LE));
        FunctionCall expect = new FunctionCall("function", Collections.singletonList(param1));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessFuncWith_a_in_expr_list_Pri_generate_FunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("function(a in (1,2,3,4))");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        CollectionExpression exprs = new CollectionExpression();
        exprs.addExpression(new ConstExpression("1"));
        exprs.addExpression(new ConstExpression("2"));
        exprs.addExpression(new ConstExpression("3"));
        exprs.addExpression(new ConstExpression("4"));
        ExpressionParam param1 = new ExpressionParam(
                new CompoundExpression(new RelationReference("a", null), exprs, Operator.IN));
        FunctionCall expect = new FunctionCall("function", Collections.singletonList(param1));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessFuncWith_a_not_in_expr_list_Pri_generate_FunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("function(a not in (1,2,3,4))");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        CollectionExpression exprs = new CollectionExpression();
        exprs.addExpression(new ConstExpression("1"));
        exprs.addExpression(new ConstExpression("2"));
        exprs.addExpression(new ConstExpression("3"));
        exprs.addExpression(new ConstExpression("4"));
        ExpressionParam param1 = new ExpressionParam(
                new CompoundExpression(new RelationReference("a", null), exprs, Operator.NOT_IN));
        FunctionCall expect = new FunctionCall("function", Collections.singletonList(param1));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessFuncWith_a_between_b_and_c_Pri_generate_FunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("function(a between c and b)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        CompoundExpression right = new CompoundExpression(
                new RelationReference("c", null), new RelationReference("b", null), Operator.AND);
        ExpressionParam param1 = new ExpressionParam(
                new CompoundExpression(new RelationReference("a", null), right, Operator.BETWEEN));
        FunctionCall expect = new FunctionCall("function", Collections.singletonList(param1));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessFuncWith_a_not_like_b_Pri_generate_FunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("function(a not like b)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        ExpressionParam param1 = new ExpressionParam(
                new CompoundExpression(new RelationReference("a", null), new RelationReference("b", null),
                        Operator.NOT_LIKE));
        FunctionCall expect = new FunctionCall("function", Collections.singletonList(param1));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessFuncWith_a_not_like_b_escape_c_Pri_generate_FunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("function(a like b escape c)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        CompoundExpression right = new CompoundExpression(
                new RelationReference("b", null), new RelationReference("c", null), Operator.ESCAPE);
        ExpressionParam param1 = new ExpressionParam(
                new CompoundExpression(new RelationReference("a", null), right, Operator.LIKE));
        FunctionCall expect = new FunctionCall("function", Collections.singletonList(param1));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessFuncWith_REGEXP_LIKE_Pri_generate_FunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("function(REGEXP_LIKE('regexp_str', 'value', 'value1'))");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p1 = new ExpressionParam(new ConstExpression("'regexp_str'"));
        FunctionParam p2 = new ExpressionParam(new ConstExpression("'value'"));
        FunctionParam p3 = new ExpressionParam(new ConstExpression("'value1'"));
        FunctionCall pCall = new FunctionCall("REGEXP_LIKE", Arrays.asList(p1, p2, p3));
        ExpressionParam param1 = new ExpressionParam(pCall);
        FunctionCall expect = new FunctionCall("function", Collections.singletonList(param1));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessFuncWith_UPDATING_Pri_generate_FunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("function(UPDATING('regexp_str', 'value', 'value1'))");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p1 = new ExpressionParam(new ConstExpression("'regexp_str'"));
        FunctionParam p2 = new ExpressionParam(new ConstExpression("'value'"));
        FunctionParam p3 = new ExpressionParam(new ConstExpression("'value1'"));
        FunctionCall pCall = new FunctionCall("UPDATING", Arrays.asList(p1, p2, p3));
        ExpressionParam param1 = new ExpressionParam(pCall);
        FunctionCall expect = new FunctionCall("function", Collections.singletonList(param1));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessFuncWith_a_member_of_b_Pri_generate_FunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("function(a member of b)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        CompoundExpression expr = new CompoundExpression(new RelationReference("a", null),
                new RelationReference("b", null), Operator.MEMBER_OF);
        ExpressionParam param1 = new ExpressionParam(expr);
        FunctionCall expect = new FunctionCall("function", Collections.singletonList(param1));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessFuncWith_exprs_is_not_empty_Pri_generate_FunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("function((1,2,3) is not empty)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        CollectionExpression exprs = new CollectionExpression();
        exprs.addExpression(new ConstExpression("1"));
        exprs.addExpression(new ConstExpression("2"));
        exprs.addExpression(new ConstExpression("3"));
        CompoundExpression expr = new CompoundExpression(exprs, null, Operator.IS_NOT_EMPTY);
        ExpressionParam param1 = new ExpressionParam(expr);
        FunctionCall expect = new FunctionCall("function", Collections.singletonList(param1));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessFunctionWithNOT_a_LE_bPredicate_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("function(not a <= b)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        Expression left =
                new CompoundExpression(new RelationReference("a", null), new RelationReference("b", null), Operator.LE);
        ExpressionParam param1 = new ExpressionParam(new CompoundExpression(left, null, Operator.NOT));
        FunctionCall expect = new FunctionCall("function", Collections.singletonList(param1));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessFunctionWith_paren_a_LE_b_parenPredicate_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("function((not a <= b))");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        Expression left =
                new CompoundExpression(new RelationReference("a", null), new RelationReference("b", null), Operator.LE);
        ExpressionParam param1 = new ExpressionParam(new CompoundExpression(left, null, Operator.NOT));
        FunctionCall expect = new FunctionCall("function", Collections.singletonList(param1));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessFunctionWith_paren_a_LE_b_paren_and_other_pri_Predicate_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("function((not a <= b) and (c != d))");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        Expression left = new CompoundExpression(
                new CompoundExpression(new RelationReference("a", null), new RelationReference("b", null), Operator.LE),
                null, Operator.NOT);
        Expression right =
                new CompoundExpression(new RelationReference("c", null), new RelationReference("d", null), Operator.NE);
        ExpressionParam param1 = new ExpressionParam(new CompoundExpression(left, right, Operator.AND));
        FunctionCall expect = new FunctionCall("function", Collections.singletonList(param1));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessFunctionWithAssign_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("function(p => (not a <= b) and (c != d))");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        Expression left = new CompoundExpression(
                new CompoundExpression(new RelationReference("a", null), new RelationReference("b", null), Operator.LE),
                null, Operator.NOT);
        Expression right =
                new CompoundExpression(new RelationReference("c", null), new RelationReference("d", null), Operator.NE);
        ParamWithAssign param1 = new ParamWithAssign("p", new CompoundExpression(left, right, Operator.AND));
        FunctionCall expect = new FunctionCall("function", Collections.singletonList(param1));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessFunctionWithMultiParams_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("function(p => (not a <= b) and (c != d), 1+3)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        Expression left = new CompoundExpression(
                new CompoundExpression(new RelationReference("a", null), new RelationReference("b", null), Operator.LE),
                null, Operator.NOT);
        Expression right =
                new CompoundExpression(new RelationReference("c", null), new RelationReference("d", null), Operator.NE);
        ParamWithAssign param1 = new ParamWithAssign("p", new CompoundExpression(left, right, Operator.AND));

        ExpressionParam param2 = new ExpressionParam(
                new CompoundExpression(new ConstExpression("1"), new ConstExpression("3"), Operator.ADD));
        FunctionCall expect = new FunctionCall("function", Arrays.asList(param1, param2));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessFunctionWithMultiParamsAndParamFlag_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("function(ALL p => (not a <= b) and (c != d), 1+3)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        Expression left = new CompoundExpression(
                new CompoundExpression(new RelationReference("a", null), new RelationReference("b", null), Operator.LE),
                null, Operator.NOT);
        Expression right =
                new CompoundExpression(new RelationReference("c", null), new RelationReference("d", null), Operator.NE);
        ParamWithAssign param1 = new ParamWithAssign("p", new CompoundExpression(left, right, Operator.AND));

        ExpressionParam param2 = new ExpressionParam(
                new CompoundExpression(new ConstExpression("1"), new ConstExpression("3"), Operator.ADD));
        FunctionCall expect = new FunctionCall("function", Arrays.asList(param1, param2));
        expect.addOption(new ConstExpression("ALL"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessFunctionWithExists_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("function(Exists())");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionCall pCall = new FunctionCall("Exists", new ArrayList<>());
        ExpressionParam p = new ExpressionParam(pCall);
        FunctionCall expect = new FunctionCall("function", Collections.singletonList(p));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessFunctionWithObject_accessFunctionAccess_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("function(ALL 1).obj_access(2)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionCall af = new FunctionCall("obj_access",
                Collections.singletonList(new ExpressionParam(new ConstExpression("2"))));
        ExpressionParam p = new ExpressionParam(new ConstExpression("1"));
        FunctionCall expect = new FunctionCall("function", Collections.singletonList(p));
        expect.addOption(new ConstExpression("ALL"));
        expect.reference(af, ReferenceOperator.DOT);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_questionmark_generateSucceed() {
        Bit_exprContext context = getBitExprContext("?(1)(2).b");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        ConstExpression expect = new ConstExpression("?");
        expect.reference(new ConstExpression("1"), ReferenceOperator.PAREN)
                .reference(new ConstExpression("2"), ReferenceOperator.PAREN)
                .reference(new RelationReference("b", null), ReferenceOperator.DOT);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_size_generateSucceed() {
        Bit_exprContext context = getBitExprContext("size()");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        Expression expect = new FunctionCall("size", Collections.emptyList());
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessFunctionWithTableIndexFunctionAccess_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("function(ALL 1)(1)(2)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        ExpressionParam p = new ExpressionParam(new ConstExpression("1"));
        FunctionCall expect = new FunctionCall("function", Collections.singletonList(p));
        expect.addOption(new ConstExpression("ALL"));
        expect.reference(new ConstExpression("1"), ReferenceOperator.PAREN).reference(new ConstExpression("2"),
                ReferenceOperator.PAREN);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_exprConst_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("15");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        Expression expect = new ConstExpression("15");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_exprWithParen_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("(a)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        Expression expect = new RelationReference("a", null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_exprList_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("(a,b,f)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        CollectionExpression expect = new CollectionExpression();
        expect.addExpression(new RelationReference("a", null));
        expect.addExpression(new RelationReference("b", null));
        expect.addExpression(new RelationReference("f", null));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_userVariable_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("(@abc, @ebf)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        CollectionExpression expect = new CollectionExpression();
        expect.addExpression(new ConstExpression("@abc"));
        expect.addExpression(new ConstExpression("@ebf"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_colDbLink_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("a.b@link");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        RelationReference expect = new RelationReference("a", null);
        RelationReference second = new RelationReference("b", null);
        second.setUserVariable("@link");
        expect.reference(second, ReferenceOperator.DOT);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_colDbLink2_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("schema.a.b@link");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        RelationReference expect = new RelationReference("schema", null);
        RelationReference second = new RelationReference("a", null);
        RelationReference third = new RelationReference("b", null);
        third.setUserVariable("@link");
        expect.reference(second, ReferenceOperator.DOT).reference(third, ReferenceOperator.DOT);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_priorExpr_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("prior a");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        CompoundExpression expect = new CompoundExpression(new RelationReference("a", null), null, Operator.PRIOR);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_connectByrootExpr_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("connect_by_root a");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        CompoundExpression expect =
                new CompoundExpression(new RelationReference("a", null), null, Operator.CONNECT_BY_ROOT);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_setExpr_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("set(1,2,3,4)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        ExpressionParam p1 = new ExpressionParam(new ConstExpression("1"));
        ExpressionParam p2 = new ExpressionParam(new ConstExpression("2"));
        ExpressionParam p3 = new ExpressionParam(new ConstExpression("3"));
        ExpressionParam p4 = new ExpressionParam(new ConstExpression("4"));
        FunctionCall expect = new FunctionCall("set", Arrays.asList(p1, p2, p3, p4));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_unKnownExpr_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("CASE WHEN id IN (1,2,3) THEN 1 ELSE 0 END");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        Expression left = new RelationReference("id", null);
        CollectionExpression right = new CollectionExpression();
        right.addExpression(new ConstExpression("1"));
        right.addExpression(new ConstExpression("2"));
        right.addExpression(new ConstExpression("3"));
        WhenClause whenClause =
                new WhenClause(new CompoundExpression(left, right, Operator.IN), new ConstExpression("1"));
        CaseWhen expect = new CaseWhen(Collections.singletonList(whenClause));
        expect.setCaseDefault(new ConstExpression("0"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_minusExpr_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("-a");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        Expression expect = new CompoundExpression(new RelationReference("a", null), null, Operator.SUB);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_fullTextSearch_generateFunctionCallSucceed() {
        Bit_exprContext context =
                getBitExprContext("match(a.b.c, a.b, a) against ('against_value' in natural language mode)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        ExpressionParam p1 = new ExpressionParam(new ColumnReference("a", "b", "c"));
        ExpressionParam p2 = new ExpressionParam(new ColumnReference(null, "a", "b"));
        ExpressionParam p3 = new ExpressionParam(new ColumnReference(null, null, "a"));
        FullTextSearch expect = new FullTextSearch(Arrays.asList(p1, p2, p3), "'against_value'");
        expect.setSearchMode(TextSearchMode.NATURAL_LANGUAGE_MODE);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_aAddB_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("a+b");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        Expression expect = new CompoundExpression(new RelationReference("a", null), new RelationReference("b", null),
                Operator.ADD);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_trimFunctionCall_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("trim(both 'avc' from 'abc')");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        FunctionParam p = new ExpressionParam(new ConstExpression("'avc'"));
        p.addOption(new ConstExpression("'abc'"));
        params.add(p);
        FunctionCall expect = new FunctionCall("trim", params);
        expect.addOption(new ConstExpression("both"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_translateFunctionCall_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("translate('abc' using char_cs)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        FunctionParam p1 = new ExpressionParam(new ConstExpression("'abc'"));
        p1.addOption(new ConstExpression("char_cs"));
        params.add(p1);
        FunctionCall expect = new FunctionCall("translate", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_extractFunctionCall_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("extract(year from 'abc')");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        FunctionParam p1 = new ExpressionParam(new ConstExpression("year"));
        p1.addOption(new ConstExpression("'abc'"));
        params.add(p1);
        FunctionCall expect = new FunctionCall("extract", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_castFunctionCall_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("cast('abc' as varchar2(64))");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        FunctionParam p1 = new ExpressionParam(new ConstExpression("'abc'"));
        p1.addOption(new CharacterType("varchar2", new BigDecimal("64")));
        params.add(p1);
        FunctionCall expect = new FunctionCall("cast", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_userFunctionCall_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("user");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        FunctionCall expect = new FunctionCall("user", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_notLnnvl_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("not lnnvl(1)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("1")));
        FunctionCall call = new FunctionCall("lnnvl", params);
        Expression expect = new CompoundExpression(call, null, Operator.NOT);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_notInExpr_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("id not in (1,2,3)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        Expression left = new RelationReference("id", null);
        CollectionExpression right = new CollectionExpression();
        right.addExpression(new ConstExpression("1"));
        right.addExpression(new ConstExpression("2"));
        right.addExpression(new ConstExpression("3"));
        Expression expect = new CompoundExpression(left, right, Operator.NOT_IN);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_varSet_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("@user_var := 12");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        Expression left = new ConstExpression("@user_var");
        Expression right = new ConstExpression("12");
        Expression expect = new CompoundExpression(left, right, Operator.SET_VAR);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_bitExpr_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("1");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        Expression expect = new ConstExpression("1");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_andOperation_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("true and false");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        Expression expect = new CompoundExpression(new BoolValue(true), new BoolValue(false), Operator.AND);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_WMSYS_Dot_WM_CONCAT_generateFunctionCallSucceed() {
        Bit_exprContext context =
                getBitExprContext("wmsys.wm_concat(all col) keep (DENSE_RANK first order by col desc)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionCall expect = new FunctionCall("wmsys.wm_concat",
                Collections.singletonList(new ExpressionParam(new RelationReference("col", null))));
        expect.addOption(new ConstExpression("all"));
        SortKey s = new SortKey(new RelationReference("col", null), SortDirection.DESC);
        expect.setKeep(new KeepClause("first", new OrderBy(Collections.singletonList(s))));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_PERCENTILE_DISC_generateFunctionCallSucceed() {
        Bit_exprContext context =
                getBitExprContext("PERCENTILE_DISC(all col) within group(order by col desc)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionCall expect = new FunctionCall("PERCENTILE_DISC",
                Collections.singletonList(new ExpressionParam(new RelationReference("col", null))));
        expect.addOption(new ConstExpression("all"));
        SortKey s = new SortKey(new RelationReference("col", null), SortDirection.DESC);
        expect.setWithinGroup(new OrderBy(Collections.singletonList(s)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_listagg_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("listagg(distinct 1,2,3)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("1")));
        params.add(new ExpressionParam(new ConstExpression("2")));
        params.add(new ExpressionParam(new ConstExpression("3")));
        FunctionCall expect = new FunctionCall("listagg", params);
        expect.addOption(new ConstExpression("distinct"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_xmlagg_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("xmlagg(1 order by col desc)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionCall expect = new FunctionCall("xmlagg", Collections.singletonList(
                new ExpressionParam(new ConstExpression("1"))));
        SortKey s = new SortKey(new RelationReference("col", null), SortDirection.DESC);
        expect.addOption(new OrderBy(Collections.singletonList(s)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_sysdate_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("sysdate");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        FunctionCall expect = new FunctionCall("sysdate", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_insert_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("insert(12,13,14,15)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("12")));
        params.add(new ExpressionParam(new ConstExpression("13")));
        params.add(new ExpressionParam(new ConstExpression("14")));
        params.add(new ExpressionParam(new ConstExpression("15")));
        FunctionCall expect = new FunctionCall("insert", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_default_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("default(chz.tab.col)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ColumnReference("chz", "tab", "col")));
        FunctionCall expect = new FunctionCall("default", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_countStarNameWin_generateSucceed() {
        Bit_exprContext context = getBitExprContext("count(all *) over ()");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("*")));
        FunctionCall expect = new FunctionCall("count", params);
        WindowSpec window = new WindowSpec();
        expect.setWindow(window);
        expect.addOption(new ConstExpression("all"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_countKeep_generateSucceed() {
        Bit_exprContext context = getBitExprContext("count(all *) keep (DENSE_RANK first order by col desc) ");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("*")));
        FunctionCall expect = new FunctionCall("count", params);
        SortKey s0 = new SortKey(new RelationReference("col", null), SortDirection.DESC);
        expect.setKeep(new KeepClause("first", new OrderBy(Collections.singletonList(s0))));
        expect.addOption(new ConstExpression("all"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_distinctBitExprWithoutWinBody_generateSucceed() {
        Bit_exprContext context = getBitExprContext("count(distinct 56) over (partition by (1,2) order by col desc)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("56")));
        FunctionCall expect = new FunctionCall("count", params);
        WindowSpec window = new WindowSpec();
        CollectionExpression p = new CollectionExpression();
        p.addExpression(new ConstExpression("1"));
        p.addExpression(new ConstExpression("2"));
        window.setPartitionBy(Collections.singletonList(p));
        SortKey s = new SortKey(new RelationReference("col", null), SortDirection.DESC);
        OrderBy orderBy = new OrderBy(Collections.singletonList(s));
        window.setOrderBy(orderBy);
        expect.setWindow(window);
        expect.addOption(new ConstExpression("distinct"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_exprListWithWinBody_generateSucceed() {
        Bit_exprContext context = getBitExprContext(
                "APPROX_COUNT_DISTINCT_SYNOPSIS(1,2,3) over (partition by (1,2) order by col desc rows between current row and 123 PRECEDING)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("1")));
        params.add(new ExpressionParam(new ConstExpression("2")));
        params.add(new ExpressionParam(new ConstExpression("3")));
        FunctionCall expect = new FunctionCall("APPROX_COUNT_DISTINCT_SYNOPSIS", params);
        WindowSpec window = new WindowSpec();
        CollectionExpression p = new CollectionExpression();
        p.addExpression(new ConstExpression("1"));
        p.addExpression(new ConstExpression("2"));
        window.setPartitionBy(Collections.singletonList(p));
        SortKey s = new SortKey(new RelationReference("col", null), SortDirection.DESC);
        OrderBy orderBy = new OrderBy(Collections.singletonList(s));
        window.setOrderBy(orderBy);
        WindowOffset begin = new WindowOffset(WindowOffsetType.CURRENT_ROW);
        WindowOffset end = new WindowOffset(WindowOffsetType.PRECEDING);
        end.setInterval(new ConstExpression("123"));
        WindowBody body = new WindowBody(WindowType.ROWS, begin, end);
        window.setBody(body);
        expect.setWindow(window);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_listaggwithingroup_generateSucceed() {
        Bit_exprContext context = getBitExprContext(
                "listagg(1,2,3) within group (order by col desc) over (partition by (1,2) order by col desc rows between current row and 123 PRECEDING)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("1")));
        params.add(new ExpressionParam(new ConstExpression("2")));
        params.add(new ExpressionParam(new ConstExpression("3")));
        FunctionCall expect = new FunctionCall("listagg", params);
        WindowSpec window = new WindowSpec();
        CollectionExpression p = new CollectionExpression();
        p.addExpression(new ConstExpression("1"));
        p.addExpression(new ConstExpression("2"));
        window.setPartitionBy(Collections.singletonList(p));
        SortKey s = new SortKey(new RelationReference("col", null), SortDirection.DESC);
        OrderBy orderBy = new OrderBy(Collections.singletonList(s));
        window.setOrderBy(orderBy);
        expect.setWithinGroup(orderBy);
        WindowOffset begin = new WindowOffset(WindowOffsetType.CURRENT_ROW);
        WindowOffset end = new WindowOffset(WindowOffsetType.PRECEDING);
        end.setInterval(new ConstExpression("123"));
        WindowBody body = new WindowBody(WindowType.ROWS, begin, end);
        window.setBody(body);
        expect.setWindow(window);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_uniqueBitExprWithWinBody_generateSucceed() {
        Bit_exprContext context = getBitExprContext(
                "min(unique 5) over (partition by (1,2) order by col desc rows between current row and 123 FOLLOWING)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("5")));
        FunctionCall expect = new FunctionCall("min", params);
        expect.addOption(new ConstExpression("unique"));
        WindowSpec window = new WindowSpec();
        CollectionExpression p = new CollectionExpression();
        p.addExpression(new ConstExpression("1"));
        p.addExpression(new ConstExpression("2"));
        window.setPartitionBy(Collections.singletonList(p));
        SortKey s = new SortKey(new RelationReference("col", null), SortDirection.DESC);
        OrderBy orderBy = new OrderBy(Collections.singletonList(s));
        window.setOrderBy(orderBy);
        WindowOffset begin = new WindowOffset(WindowOffsetType.CURRENT_ROW);
        WindowOffset end = new WindowOffset(WindowOffsetType.FOLLOWING);
        end.setInterval(new ConstExpression("123"));
        WindowBody body = new WindowBody(WindowType.ROWS, begin, end);
        window.setBody(body);
        expect.setWindow(window);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_firstValueWithWinBody_generateSucceed() {
        Bit_exprContext context = getBitExprContext(
                "FIRST_VALUE (5 respect nulls) over (partition by (1,2) order by col desc RANGE 123 FOLLOWING)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        FunctionParam p1 = new ExpressionParam(new ConstExpression("5"));
        params.add(p1);
        FunctionCall expect = new FunctionCall("FIRST_VALUE", params);
        WindowSpec window = new WindowSpec();
        CollectionExpression p = new CollectionExpression();
        p.addExpression(new ConstExpression("1"));
        p.addExpression(new ConstExpression("2"));
        window.setPartitionBy(Collections.singletonList(p));
        SortKey s = new SortKey(new RelationReference("col", null), SortDirection.DESC);
        OrderBy orderBy = new OrderBy(Collections.singletonList(s));
        window.setOrderBy(orderBy);
        WindowOffset offset = new WindowOffset(WindowOffsetType.FOLLOWING);
        offset.setInterval(new ConstExpression("123"));
        WindowBody body = new WindowBody(WindowType.RANGE, offset);
        window.setBody(body);
        expect.setWindow(window);
        expect.addOption(new ConstExpression("respect nulls"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_leadWithWinBody_generateSucceed() {
        Bit_exprContext context = getBitExprContext(
                "LEAD (5 respect nulls, 1,2) over (partition by (1,2) order by col desc RANGE 123 FOLLOWING)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        FunctionParam p1 = new ExpressionParam(new ConstExpression("5"));
        params.add(p1);
        params.add(new ExpressionParam(new ConstExpression("1")));
        params.add(new ExpressionParam(new ConstExpression("2")));
        FunctionCall expect = new FunctionCall("LEAD", params);
        WindowSpec window = new WindowSpec();
        CollectionExpression p = new CollectionExpression();
        p.addExpression(new ConstExpression("1"));
        p.addExpression(new ConstExpression("2"));
        window.setPartitionBy(Collections.singletonList(p));
        SortKey s = new SortKey(new RelationReference("col", null), SortDirection.DESC);
        OrderBy orderBy = new OrderBy(Collections.singletonList(s));
        window.setOrderBy(orderBy);
        WindowOffset offset = new WindowOffset(WindowOffsetType.FOLLOWING);
        offset.setInterval(new ConstExpression("123"));
        WindowBody body = new WindowBody(WindowType.RANGE, offset);
        window.setBody(body);
        expect.setWindow(window);
        expect.addOption(new ConstExpression("respect nulls"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_leadWithWinBody1_generateSucceed() {
        Bit_exprContext context = getBitExprContext(
                "LEAD (5,1,2) respect nulls over (partition by (1,2) order by col desc RANGE 123 FOLLOWING)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("5")));
        params.add(new ExpressionParam(new ConstExpression("1")));
        params.add(new ExpressionParam(new ConstExpression("2")));
        FunctionCall expect = new FunctionCall("LEAD", params);
        expect.addOption(new ConstExpression("respect nulls"));
        WindowSpec window = new WindowSpec();
        CollectionExpression p = new CollectionExpression();
        p.addExpression(new ConstExpression("1"));
        p.addExpression(new ConstExpression("2"));
        window.setPartitionBy(Collections.singletonList(p));
        SortKey s = new SortKey(new RelationReference("col", null), SortDirection.DESC);
        OrderBy orderBy = new OrderBy(Collections.singletonList(s));
        window.setOrderBy(orderBy);
        WindowOffset offset = new WindowOffset(WindowOffsetType.FOLLOWING);
        offset.setInterval(new ConstExpression("123"));
        WindowBody body = new WindowBody(WindowType.RANGE, offset);
        window.setBody(body);
        expect.setWindow(window);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_functionNameWinBody_generateSucceed() {
        Bit_exprContext context = getBitExprContext(
                "function_name(5, 1,2) over (partition by (1,2) order by col desc RANGE 123 FOLLOWING)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("5")));
        params.add(new ExpressionParam(new ConstExpression("1")));
        params.add(new ExpressionParam(new ConstExpression("2")));
        FunctionCall expect = new FunctionCall("function_name", params);
        WindowSpec window = new WindowSpec();
        CollectionExpression p = new CollectionExpression();
        p.addExpression(new ConstExpression("1"));
        p.addExpression(new ConstExpression("2"));
        window.setPartitionBy(Collections.singletonList(p));
        SortKey s = new SortKey(new RelationReference("col", null), SortDirection.DESC);
        OrderBy orderBy = new OrderBy(Collections.singletonList(s));
        window.setOrderBy(orderBy);
        WindowOffset offset = new WindowOffset(WindowOffsetType.FOLLOWING);
        offset.setInterval(new ConstExpression("123"));
        WindowBody body = new WindowBody(WindowType.RANGE, offset);
        window.setBody(body);
        expect.setWindow(window);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_nthValueWithWinBody_generateSucceed() {
        Bit_exprContext context = getBitExprContext(
                "NTH_VALUE(5,6) from first respect nulls over (partition by (1,2) order by col desc RANGE 123 FOLLOWING)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("5")));
        params.add(new ExpressionParam(new ConstExpression("6")));
        FunctionCall expect = new FunctionCall("NTH_VALUE", params);
        expect.addOption(new ConstExpression("from first"));
        expect.addOption(new ConstExpression("respect nulls"));
        WindowSpec window = new WindowSpec();
        CollectionExpression p = new CollectionExpression();
        p.addExpression(new ConstExpression("1"));
        p.addExpression(new ConstExpression("2"));
        window.setPartitionBy(Collections.singletonList(p));
        SortKey s = new SortKey(new RelationReference("col", null), SortDirection.DESC);
        OrderBy orderBy = new OrderBy(Collections.singletonList(s));
        window.setOrderBy(orderBy);
        WindowOffset offset = new WindowOffset(WindowOffsetType.FOLLOWING);
        offset.setInterval(new ConstExpression("123"));
        WindowBody body = new WindowBody(WindowType.RANGE, offset);
        window.setBody(body);
        expect.setWindow(window);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_maxWithWinBody_generateSucceed() {
        Bit_exprContext context = getBitExprContext(
                "max(1) keep (DENSE_RANK first order by col asc) over (partition by (1,2) order by col desc RANGE 123 FOLLOWING)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("1")));
        FunctionCall expect = new FunctionCall("max", params);
        SortKey s0 = new SortKey(new RelationReference("col", null), SortDirection.ASC);
        expect.setKeep(new KeepClause("first", new OrderBy(Collections.singletonList(s0))));
        WindowSpec window = new WindowSpec();
        CollectionExpression p = new CollectionExpression();
        p.addExpression(new ConstExpression("1"));
        p.addExpression(new ConstExpression("2"));
        window.setPartitionBy(Collections.singletonList(p));
        SortKey s = new SortKey(new RelationReference("col", null), SortDirection.DESC);
        OrderBy orderBy = new OrderBy(Collections.singletonList(s));
        window.setOrderBy(orderBy);
        WindowOffset offset = new WindowOffset(WindowOffsetType.FOLLOWING);
        offset.setInterval(new ConstExpression("123"));
        WindowBody body = new WindowBody(WindowType.RANGE, offset);
        window.setBody(body);
        expect.setWindow(window);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_WMSYS_Dot_WM_CONCATWithWinBody_generateSucceed() {
        Bit_exprContext context = getBitExprContext(
                "WMSYS.WM_CONCAT(1) keep (DENSE_RANK first order by col asc) over (partition by (1,2) order by col desc RANGE 123 FOLLOWING)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("1")));
        FunctionCall expect = new FunctionCall("WMSYS.WM_CONCAT", params);
        SortKey s0 = new SortKey(new RelationReference("col", null), SortDirection.ASC);
        expect.setKeep(new KeepClause("first", new OrderBy(Collections.singletonList(s0))));
        WindowSpec window = new WindowSpec();
        CollectionExpression p = new CollectionExpression();
        p.addExpression(new ConstExpression("1"));
        p.addExpression(new ConstExpression("2"));
        window.setPartitionBy(Collections.singletonList(p));
        SortKey s = new SortKey(new RelationReference("col", null), SortDirection.DESC);
        OrderBy orderBy = new OrderBy(Collections.singletonList(s));
        window.setOrderBy(orderBy);
        WindowOffset offset = new WindowOffset(WindowOffsetType.FOLLOWING);
        offset.setInterval(new ConstExpression("123"));
        WindowBody body = new WindowBody(WindowType.RANGE, offset);
        window.setBody(body);
        expect.setWindow(window);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_value_generateCaseWhenSucceed() {
        ExprContext context = getExprContext("CASE a WHEN 1 THEN 11 WHEN 2 THEN 22 ELSE 33 END");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        List<WhenClause> whenClauses = new ArrayList<>();
        whenClauses.add(new WhenClause(new ConstExpression("1"), new ConstExpression("11")));
        whenClauses.add(new WhenClause(new ConstExpression("2"), new ConstExpression("22")));
        CaseWhen expect = new CaseWhen(whenClauses);
        expect.setCaseValue(new RelationReference("a", null));
        expect.setCaseDefault(new ConstExpression("33"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_condition_generateCaseWhenSucceed() {
        ExprContext context = getExprContext("CASE WHEN a < 1 THEN 11 WHEN a < 2 THEN 22 ELSE 33 END");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        List<WhenClause> whenClauses = new ArrayList<>();
        RelationReference relationReference = new RelationReference("a", null);
        whenClauses.add(new WhenClause(new CompoundExpression(relationReference, new ConstExpression("1"), Operator.LT),
                new ConstExpression("11")));
        whenClauses.add(new WhenClause(new CompoundExpression(relationReference, new ConstExpression("2"), Operator.LT),
                new ConstExpression("22")));
        CaseWhen expect = new CaseWhen(whenClauses);
        expect.setCaseDefault(new ConstExpression("33"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_xmlParse_generateSucceed() {
        ExprContext context = getExprContext("xmlparse(document 'aaa')");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionCall expect = new FunctionCall("xmlparse",
                Collections.singletonList(new ExpressionParam(new ConstExpression("'aaa'"))));
        expect.addOption(new ConstExpression("document"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_deleteXml_generateSucceed() {
        Xml_functionContext context = getXmlExprContext("deletexml(1,2,3)");
        OracleExpressionFactory factory = new OracleExpressionFactory();
        Expression actual = factory.visit(context);

        FunctionCall expect = new FunctionCall("deletexml", Arrays.asList(
                new ExpressionParam(new ConstExpression("1")),
                new ExpressionParam(new ConstExpression("2")),
                new ExpressionParam(new ConstExpression("3"))));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_insertChildXml_generateSucceed() {
        Xml_functionContext context = getXmlExprContext("insertchildxml(1,2,3,4)");
        OracleExpressionFactory factory = new OracleExpressionFactory();
        Expression actual = factory.visit(context);

        FunctionCall expect = new FunctionCall("insertchildxml", Arrays.asList(
                new ExpressionParam(new ConstExpression("1")),
                new ExpressionParam(new ConstExpression("2")),
                new ExpressionParam(new ConstExpression("3")),
                new ExpressionParam(new ConstExpression("4"))));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_xmlSequence_generateSucceed() {
        Xml_functionContext context = getXmlExprContext("xmlsequence(1)");
        OracleExpressionFactory factory = new OracleExpressionFactory();
        Expression actual = factory.visit(context);

        FunctionCall expect = new FunctionCall("xmlsequence",
                Collections.singletonList(new ExpressionParam(new ConstExpression("1"))));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_xmlParseWellformed_generateSucceed() {
        ExprContext context = getExprContext("xmlparse(document 'aaa' wellformed)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p = new ExpressionParam(new ConstExpression("'aaa'"));
        p.addOption(new ConstExpression("wellformed"));
        FunctionCall expect = new FunctionCall("xmlparse", Collections.singletonList(p));
        expect.addOption(new ConstExpression("document"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_xmlElement_generateSucceed() {
        ExprContext context = getExprContext("xmlelement(ENTITYESCAPING name abc)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p = new ExpressionParam(new ColumnReference(null, null, "abc"));
        p.addOption(new ConstExpression("name"));
        FunctionCall expect = new FunctionCall("xmlelement", Collections.singletonList(p));
        expect.addOption(new ConstExpression("ENTITYESCAPING"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_xmlElementNoEntity_generateSucceed() {
        ExprContext context = getExprContext("xmlelement(NOENTITYESCAPING evalname 1 || 3)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p = new ExpressionParam(new CompoundExpression(
                new ConstExpression("1"), new ConstExpression("3"), Operator.CNNOP));
        p.addOption(new ConstExpression("evalname"));
        FunctionCall expect = new FunctionCall("xmlelement", Collections.singletonList(p));
        expect.addOption(new ConstExpression("NOENTITYESCAPING"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_xmlElementXmlAttrs_generateSucceed() {
        ExprContext context = getExprContext("xmlelement(NOENTITYESCAPING evalname 1 || 3, "
                + "xmlattributes('aaa' as evalname 12, 'err' as ancd))");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p = new ExpressionParam(new CompoundExpression(
                new ConstExpression("1"), new ConstExpression("3"), Operator.CNNOP));
        p.addOption(new ConstExpression("evalname"));
        FunctionParam p1 = new ExpressionParam(new ConstExpression("'aaa'"));
        p1.addOption(new ConstExpression("12"));
        FunctionParam p2 = new ExpressionParam(new ConstExpression("'err'"));
        p2.addOption(new RelationReference("ancd", null));
        FunctionCall xmlAttrs = new FunctionCall("xmlattributes", Arrays.asList(p1, p2));
        FunctionCall expect = new FunctionCall("xmlelement", Arrays.asList(p, new ExpressionParam(xmlAttrs)));
        expect.addOption(new ConstExpression("NOENTITYESCAPING"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_xmlElementXmlAttrsEntity_generateSucceed() {
        ExprContext context = getExprContext("xmlelement(NOENTITYESCAPING evalname 1 || 3, "
                + "xmlattributes(ENTITYESCAPING SCHEMACHECK 'aaa' as evalname 12, 'err' as ancd))");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p = new ExpressionParam(new CompoundExpression(
                new ConstExpression("1"), new ConstExpression("3"), Operator.CNNOP));
        p.addOption(new ConstExpression("evalname"));
        FunctionParam p1 = new ExpressionParam(new ConstExpression("'aaa'"));
        p1.addOption(new ConstExpression("12"));
        FunctionParam p2 = new ExpressionParam(new ConstExpression("'err'"));
        p2.addOption(new RelationReference("ancd", null));
        FunctionCall xmlAttrs = new FunctionCall("xmlattributes", Arrays.asList(p1, p2));
        xmlAttrs.addOption(new ConstExpression("ENTITYESCAPING"));
        xmlAttrs.addOption(new ConstExpression("SCHEMACHECK"));
        FunctionCall expect = new FunctionCall("xmlelement", Arrays.asList(p, new ExpressionParam(xmlAttrs)));
        expect.addOption(new ConstExpression("NOENTITYESCAPING"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_xmlElementXmlAttrsNoEntity_generateSucceed() {
        ExprContext context = getExprContext("xmlelement(NOENTITYESCAPING evalname 1 || 3, "
                + "xmlattributes(NOENTITYESCAPING NOSCHEMACHECK 'aaa' as evalname 12, 'err' as ancd))");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p = new ExpressionParam(new CompoundExpression(
                new ConstExpression("1"), new ConstExpression("3"), Operator.CNNOP));
        p.addOption(new ConstExpression("evalname"));
        FunctionParam p1 = new ExpressionParam(new ConstExpression("'aaa'"));
        p1.addOption(new ConstExpression("12"));
        FunctionParam p2 = new ExpressionParam(new ConstExpression("'err'"));
        p2.addOption(new RelationReference("ancd", null));
        FunctionCall xmlAttrs = new FunctionCall("xmlattributes", Arrays.asList(p1, p2));
        xmlAttrs.addOption(new ConstExpression("NOENTITYESCAPING"));
        xmlAttrs.addOption(new ConstExpression("NOSCHEMACHECK"));
        FunctionCall expect = new FunctionCall("xmlelement", Arrays.asList(p, new ExpressionParam(xmlAttrs)));
        expect.addOption(new ConstExpression("NOENTITYESCAPING"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_xmlElementXmlAttrsXmlClause_generateSucceed() {
        ExprContext context = getExprContext("xmlelement(NOENTITYESCAPING evalname 1 || 3, "
                + "xmlattributes('aaa' as evalname 12, 'err' as ancd), 1, 2 shs)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p = new ExpressionParam(new CompoundExpression(
                new ConstExpression("1"), new ConstExpression("3"), Operator.CNNOP));
        p.addOption(new ConstExpression("evalname"));
        FunctionParam p1 = new ExpressionParam(new ConstExpression("'aaa'"));
        p1.addOption(new ConstExpression("12"));
        FunctionParam p2 = new ExpressionParam(new ConstExpression("'err'"));
        p2.addOption(new RelationReference("ancd", null));
        FunctionCall xmlAttrs = new FunctionCall("xmlattributes", Arrays.asList(p1, p2));
        FunctionParam p3 = new ExpressionParam(new ConstExpression("1"));
        FunctionParam p4 = new ExpressionParam(new ConstExpression("2"));
        p4.addOption(new RelationReference("shs", null));
        FunctionCall expect = new FunctionCall("xmlelement", Arrays.asList(p, new ExpressionParam(xmlAttrs), p3, p4));
        expect.addOption(new ConstExpression("NOENTITYESCAPING"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_xmlSerialize_generateSucceed() {
        ExprContext context = getExprContext("xmlSerialize(content 'aaa')");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p1 = new ExpressionParam(new ConstExpression("'aaa'"));
        FunctionCall expect = new FunctionCall("xmlSerialize", Collections.singletonList(p1));
        expect.addOption(new ConstExpression("content"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_xmlSerializeEncodingVersion_generateSucceed() {
        ExprContext context =
                getExprContext("xmlSerialize(content 'aaa' as int encoding 'aaa' version 12 no indent show defaults)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p1 = new ExpressionParam(new ConstExpression("'aaa'"));
        p1.addOption(new NumberType("int", null, null));
        FunctionCall expect = new FunctionCall("xmlSerialize", Collections.singletonList(p1));
        expect.addOption(new ConstExpression("content"));
        expect.addOption(new ConstExpression("encoding 'aaa'"));
        expect.addOption(new ConstExpression("version 12"));
        expect.addOption(new ConstExpression("no indent"));
        expect.addOption(new ConstExpression("show defaults"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_xmlSerializeEncodingVersion1_generateSucceed() {
        ExprContext context = getExprContext(
                "xmlSerialize(content 'aaa' as int encoding 'aaa' version 12 indent size=12 hide defaults)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p1 = new ExpressionParam(new ConstExpression("'aaa'"));
        p1.addOption(new NumberType("int", null, null));
        FunctionCall expect = new FunctionCall("xmlSerialize", Collections.singletonList(p1));
        expect.addOption(new ConstExpression("content"));
        expect.addOption(new ConstExpression("encoding 'aaa'"));
        expect.addOption(new ConstExpression("version 12"));
        expect.addOption(
                new CompoundExpression(new ConstExpression("indent size"), new ConstExpression("12"), Operator.EQ));
        expect.addOption(new ConstExpression("hide defaults"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_xmlSerializeEncodingVersion2_generateSucceed() {
        ExprContext context = getExprContext(
                "xmlSerialize(content 'aaa' as int encoding 'aaa' version 12 indent size=-12 hide defaults)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p1 = new ExpressionParam(new ConstExpression("'aaa'"));
        p1.addOption(new NumberType("int", null, null));
        FunctionCall expect = new FunctionCall("xmlSerialize", Collections.singletonList(p1));
        expect.addOption(new ConstExpression("content"));
        expect.addOption(new ConstExpression("encoding 'aaa'"));
        expect.addOption(new ConstExpression("version 12"));
        expect.addOption(new CompoundExpression(new ConstExpression("indent size"),
                new CompoundExpression(new ConstExpression("12"), null, Operator.SUB), Operator.EQ));
        expect.addOption(new ConstExpression("hide defaults"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_xmlSerializeEncodingVersion3_generateSucceed() {
        ExprContext context = getExprContext(
                "xmlSerialize(content 'aaa' as int encoding 'aaa' version 12 indent)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p1 = new ExpressionParam(new ConstExpression("'aaa'"));
        p1.addOption(new NumberType("int", null, null));
        FunctionCall expect = new FunctionCall("xmlSerialize", Collections.singletonList(p1));
        expect.addOption(new ConstExpression("content"));
        expect.addOption(new ConstExpression("encoding 'aaa'"));
        expect.addOption(new ConstExpression("version 12"));
        expect.addOption(new ConstExpression("indent"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_xmlCast_generateSucceed() {
        ExprContext context = getExprContext("xmlcast('aaa' as int)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p1 = new ExpressionParam(new ConstExpression("'aaa'"));
        p1.addOption(new NumberType("int", null, null));
        FunctionCall expect = new FunctionCall("xmlcast", Collections.singletonList(p1));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_xmlFunction_generateSucceed() {
        ExprContext context = getExprContext("xmlcast('aaa' as int).\"aaa\".count(*)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p1 = new ExpressionParam(new ConstExpression("'aaa'"));
        p1.addOption(new NumberType("int", null, null));
        FunctionCall expect = new FunctionCall("xmlcast", Collections.singletonList(p1));
        expect.reference(new RelationReference("\"aaa\"", null), ReferenceOperator.DOT)
                .reference(new FunctionCall("count", Collections.singletonList(
                        new ExpressionParam(new ConstExpression("*")))), ReferenceOperator.DOT);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_xmlFunctionCount_generateSucceed() {
        ExprContext context = getExprContext("xmlcast('aaa' as int).count(*)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p1 = new ExpressionParam(new ConstExpression("'aaa'"));
        p1.addOption(new NumberType("int", null, null));
        FunctionCall expect = new FunctionCall("xmlcast", Collections.singletonList(p1));
        expect.reference(new FunctionCall("count", Collections.singletonList(
                new ExpressionParam(new ConstExpression("*")))), ReferenceOperator.DOT);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_xmlFunctionPrior_generateSucceed() {
        ExprContext context = getExprContext("xmlcast('aaa' as int).prior(1)(2)(3)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p1 = new ExpressionParam(new ConstExpression("'aaa'"));
        p1.addOption(new NumberType("int", null, null));
        FunctionCall expect = new FunctionCall("xmlcast", Collections.singletonList(p1));
        expect.reference(new FunctionCall("prior", Collections.singletonList(
                new ExpressionParam(new ConstExpression("1")))), ReferenceOperator.DOT)
                .reference(new ConstExpression("2"), ReferenceOperator.PAREN)
                .reference(new ConstExpression("3"), ReferenceOperator.PAREN);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_xmlFunction2_generateSucceed() {
        ExprContext context = getExprContext("xmlcast('aaa' as int)(2)(3)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p1 = new ExpressionParam(new ConstExpression("'aaa'"));
        p1.addOption(new NumberType("int", null, null));
        FunctionCall expect = new FunctionCall("xmlcast", Collections.singletonList(p1));
        expect.reference(new ConstExpression("2"), ReferenceOperator.PAREN)
                .reference(new ConstExpression("3"), ReferenceOperator.PAREN);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_treatFunction_generateSucceed() {
        ExprContext context = getExprContext("treat('aaa' as json)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p1 = new ExpressionParam(new ConstExpression("'aaa'"));
        p1.addOption(new GeneralDataType("json", null));
        FunctionCall expect = new FunctionCall("treat", Collections.singletonList(p1));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_newOB_generateSucceed() {
        ExprContext context = getExprContext("new \"ob\"()");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionCall expect = new FunctionCall("new \"ob\"", Collections.emptyList());
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_simpleExpr_generateSucceed() {
        ExprContext context = getExprContext("abcd collate 'aaa'");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        Expression expect = new RelationReference("abcd", null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_cursorSelect_generateSucceed() {
        ExprContext context = getExprContext("cursor(select 1 from dual)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        Projection p = new Projection(new ConstExpression("1"), null);
        NameReference from = new NameReference(null, "dual", null);
        FunctionCall expect = new FunctionCall("cursor", Collections.singletonList(
                new ExpressionParam(new Select(new SelectBody(Collections.singletonList(p),
                        Collections.singletonList(from))))));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_regExp_generateSucceed() {
        PredicateContext context = getPredicateContext("regexp_like(1,2)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionCall expect = new FunctionCall("regexp_like", Arrays.asList(
                new ExpressionParam(new ConstExpression("1")),
                new ExpressionParam(new ConstExpression("2"))));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_lnnvl_generateSucceed() {
        PredicateContext context = getPredicateContext("lnnvl(col = '12')");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionCall expect = new FunctionCall("lnnvl", Collections.singletonList(
                new ExpressionParam(new CompoundExpression(new RelationReference("col", null),
                        new ConstExpression("'12'"), Operator.EQ))));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_existSelect_generateSucceed() {
        PredicateContext context = getPredicateContext("exists(select 1 from dual)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        Projection p = new Projection(new ConstExpression("1"), null);
        NameReference from = new NameReference(null, "dual", null);
        FunctionCall expect = new FunctionCall("exists", Collections.singletonList(
                new ExpressionParam(new SelectBody(Collections.singletonList(p),
                        Collections.singletonList(from)))));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_updating_generateSucceed() {
        PredicateContext context = getPredicateContext("updating(\"aaa\")");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionCall expect = new FunctionCall("updating", Collections.singletonList(
                new ExpressionParam(new ConstExpression("\"aaa\""))));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonConstrain_generateSucceed() {
        ExprContext context = getExprContext("'aaa' is json lax allow scalars with unique keys");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        Expression left = new ConstExpression("'aaa'");
        JsonConstraint right = new JsonConstraint();
        right.setStrictMode(StrictMode.LAX);
        right.setScalarsMode(ScalarsMode.ALLOW_SCALARS);
        right.setUniqueMode(UniqueMode.WITH_UNIQUE_KEYS);
        Expression expect = new CompoundExpression(left, right, Operator.EQ);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonConstrain1_generateSucceed() {
        ExprContext context = getExprContext("'aaa' is json");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        Expression left = new ConstExpression("'aaa'");
        JsonConstraint right = new JsonConstraint();
        Expression expect = new CompoundExpression(left, right, Operator.EQ);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonExists_generateSucceed() {
        ExprContext context = getExprContext("json_exists(12 format json,12)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p1 = new ExpressionParam(new ConstExpression("12"));
        FunctionParam p2 = new ExpressionParam(new ConstExpression("12"));
        p1.addOption(new ConstExpression("format json"));
        FunctionCall expect = new FunctionCall("json_exists", Arrays.asList(p1, p2));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonExists1_generateSucceed() {
        ExprContext context = getExprContext("json_exists(12 format json,12 passing 123 as \"aaa\", 456 as \"bbb\")");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p1 = new ExpressionParam(new ConstExpression("12"));
        FunctionParam p2 = new ExpressionParam(new ConstExpression("12"));
        p1.addOption(new ConstExpression("format json"));
        FunctionCall expect = new FunctionCall("json_exists", Arrays.asList(p1, p2));
        expect.addOption(new ExpressionParam(new ConstExpression("123"), "\"aaa\""));
        expect.addOption(new ExpressionParam(new ConstExpression("456"), "\"bbb\""));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessFunc_generateSucceed() {
        ExprContext context = getExprContext("func(12,13 passing 123 as \"aaa\", 456 as \"bbb\")");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p1 = new ExpressionParam(new ConstExpression("12"));
        FunctionParam p2 = new ExpressionParam(new ConstExpression("13"));
        FunctionCall expect = new FunctionCall("func", Arrays.asList(p1, p2));
        expect.addOption(new ExpressionParam(new ConstExpression("123"), "\"aaa\""));
        expect.addOption(new ExpressionParam(new ConstExpression("456"), "\"bbb\""));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonExists2_generateSucceed() {
        ExprContext context = getExprContext("json_exists(12 format json,12 true on error_p error_p on empty)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p1 = new ExpressionParam(new ConstExpression("12"));
        FunctionParam p2 = new ExpressionParam(new ConstExpression("12"));
        p1.addOption(new ConstExpression("format json"));
        FunctionCall expect = new FunctionCall("json_exists", Arrays.asList(p1, p2));
        JsonOnOption jsonOnOption = new JsonOnOption();
        jsonOnOption.setOnError(new BoolValue(true));
        jsonOnOption.setOnEmpty(new ConstExpression("error_p"));
        expect.addOption(jsonOnOption);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessFunc1_generateSucceed() {
        ExprContext context = getExprContext("func(12, 12 true on error_p)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p1 = new ExpressionParam(new ConstExpression("12"));
        FunctionParam p2 = new ExpressionParam(new ConstExpression("12"));
        FunctionCall expect = new FunctionCall("func", Arrays.asList(p1, p2));
        JsonOnOption jsonOnOption = new JsonOnOption();
        jsonOnOption.setOnError(new BoolValue(true));
        expect.addOption(jsonOnOption);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessFunc3_generateSucceed() {
        ExprContext context = getExprContext("func(12, 12 error_p on error_p)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p1 = new ExpressionParam(new ConstExpression("12"));
        FunctionParam p2 = new ExpressionParam(new ConstExpression("12"));
        FunctionCall expect = new FunctionCall("func", Arrays.asList(p1, p2));
        JsonOnOption jsonOnOption = new JsonOnOption();
        jsonOnOption.setOnError(new ConstExpression("error_p"));
        expect.addOption(jsonOnOption);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonArrayAgg_generateSucceed() {
        ExprContext context = getExprContext("json_arrayagg(12)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p1 = new ExpressionParam(new ConstExpression("12"));
        FunctionCall expect = new FunctionCall("json_arrayagg", Collections.singletonList(p1));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonArrayAgg1_generateSucceed() {
        ExprContext context = getExprContext(
                "json_arrayagg(all 12 format json order by col desc absent on null returning raw(12) strict)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p1 = new ExpressionParam(new ConstExpression("12"));
        FunctionCall expect = new FunctionCall("json_arrayagg", Collections.singletonList(p1));
        expect.addOption(new ConstExpression("all"));
        expect.addOption(new ConstExpression("format json"));
        SortKey s = new SortKey(new RelationReference("col", null), SortDirection.DESC);
        expect.addOption(new OrderBy(Collections.singletonList(s)));
        JsonOnOption jsonOnOption = new JsonOnOption();
        jsonOnOption.setOnNull(new ConstExpression("absent"));
        expect.addOption(jsonOnOption);
        expect.addOption(new GeneralDataType("raw", Collections.singletonList("12")));
        JsonConstraint c = new JsonConstraint();
        c.setStrictMode(StrictMode.STRICT);
        expect.addOption(c);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonObjAgg_generateSucceed() {
        ExprContext context = getExprContext("json_objectagg(key 1 value 2)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p1 = new ExpressionParam(new JsonKeyValue(new ConstExpression("1"), new ConstExpression("2")));
        FunctionCall expect = new FunctionCall("json_objectagg", Collections.singletonList(p1));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonObjAgg3_generateSucceed() {
        ExprContext context = getExprContext("json_objectagg(1,2)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p1 = new ExpressionParam(new ConstExpression("1"));
        FunctionParam p2 = new ExpressionParam(new ConstExpression("2"));
        FunctionCall expect = new FunctionCall("json_objectagg", Arrays.asList(p1, p2));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonObjAgg1_generateSucceed() {
        ExprContext context = getExprContext(
                "json_objectagg(1 value 2 format json null on null returning nvarchar2(12) strict with unique keys)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p1 = new ExpressionParam(new JsonKeyValue(new ConstExpression("1"), new ConstExpression("2")));
        FunctionCall expect = new FunctionCall("json_objectagg", Collections.singletonList(p1));
        expect.addOption(new ConstExpression("format json"));
        JsonOnOption jsonOnOption = new JsonOnOption();
        jsonOnOption.setOnNull(new NullExpression());
        expect.addOption(jsonOnOption);
        expect.addOption(new CharacterType("nvarchar2", new BigDecimal("12")));
        JsonConstraint c = new JsonConstraint();
        c.setStrictMode(StrictMode.STRICT);
        c.setUniqueMode(UniqueMode.WITH_UNIQUE_KEYS);
        expect.addOption(c);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonExists3_generateSucceed() {
        ExprContext context = getExprContext(
                "json_exists(12 format json,12 passing 123 as \"aaa\", 456 as \"bbb\" true on error_p error_p on empty)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p1 = new ExpressionParam(new ConstExpression("12"));
        FunctionParam p2 = new ExpressionParam(new ConstExpression("12"));
        p1.addOption(new ConstExpression("format json"));
        FunctionCall expect = new FunctionCall("json_exists", Arrays.asList(p1, p2));
        expect.addOption(new ExpressionParam(new ConstExpression("123"), "\"aaa\""));
        expect.addOption(new ExpressionParam(new ConstExpression("456"), "\"bbb\""));
        JsonOnOption jsonOnOption = new JsonOnOption();
        jsonOnOption.setOnError(new BoolValue(true));
        jsonOnOption.setOnEmpty(new ConstExpression("error_p"));
        expect.addOption(jsonOnOption);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonConstrain2_generateSucceed() {
        ExprContext context = getExprContext("'aaa' is json strict disallow scalars without unique keys");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        Expression left = new ConstExpression("'aaa'");
        JsonConstraint right = new JsonConstraint();
        right.setStrictMode(StrictMode.STRICT);
        right.setScalarsMode(ScalarsMode.DISALLOW_SCALARS);
        right.setUniqueMode(UniqueMode.WITHOUT_UNIQUE_KEYS);
        Expression expect = new CompoundExpression(left, right, Operator.EQ);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonObject_generateSucceed() {
        ExprContext context = getExprContext("json {}");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        Expression expect = new FunctionCall("json_object", Collections.emptyList());
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonObject1_generateSucceed() {
        ExprContext context = getExprContext("json_object(absent on null)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionCall expect = new FunctionCall("json_object", Collections.emptyList());
        JsonOnOption jsonOnOption = new JsonOnOption();
        jsonOnOption.setOnNull(new ConstExpression("absent"));
        expect.addOption(jsonOnOption);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonObject2_generateSucceed() {
        ExprContext context = getExprContext("json_object(returning json)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionCall expect = new FunctionCall("json_object", Collections.emptyList());
        expect.addOption(new GeneralDataType("json", null));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonObject3_generateSucceed() {
        ExprContext context = getExprContext("json_object(null on null returning json strict with unique keys)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionCall expect = new FunctionCall("json_object", Collections.emptyList());
        JsonOnOption jsonOnOption = new JsonOnOption();
        jsonOnOption.setOnNull(new NullExpression());
        expect.addOption(jsonOnOption);
        expect.addOption(new GeneralDataType("json", null));
        JsonConstraint jc = new JsonConstraint();
        jc.setStrictMode(StrictMode.STRICT);
        jc.setUniqueMode(UniqueMode.WITH_UNIQUE_KEYS);
        expect.addOption(jc);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonObject4_generateSucceed() {
        ExprContext context = getExprContext("json_object(* null on null returning json strict with unique keys)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p = new ExpressionParam(new ConstExpression("*"));
        FunctionCall expect = new FunctionCall("json_object", Collections.singletonList(p));
        JsonOnOption jsonOnOption = new JsonOnOption();
        jsonOnOption.setOnNull(new NullExpression());
        expect.addOption(jsonOnOption);
        expect.addOption(new GeneralDataType("json", null));
        JsonConstraint jc = new JsonConstraint();
        jc.setStrictMode(StrictMode.STRICT);
        jc.setUniqueMode(UniqueMode.WITH_UNIQUE_KEYS);
        expect.addOption(jc);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonObject5_generateSucceed() {
        ExprContext context =
                getExprContext(
                        "json{key 1 value 2, '111', 1:'222', 'abc':1234 format json,    '  asdasdas  '     :col3 strict}");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p = new ExpressionParam(new JsonKeyValue(new ConstExpression("1"), new ConstExpression("2")));
        FunctionParam p1 = new ExpressionParam(new ConstExpression("'111'"));
        FunctionParam p2 =
                new ExpressionParam(new JsonKeyValue(new ConstExpression("1"), new ConstExpression("'222'")));
        FunctionParam p3 =
                new ExpressionParam(new JsonKeyValue(new ConstExpression("'abc'"), new ConstExpression("1234")));
        p3.addOption(new ConstExpression("format json"));
        FunctionParam p4 =
                new ExpressionParam(
                        new JsonKeyValue(new ConstExpression("'  asdasdas  '"), new RelationReference("col3", null)));
        FunctionCall expect = new FunctionCall("json_object", Arrays.asList(p, p1, p2, p3, p4));
        JsonConstraint jc = new JsonConstraint();
        jc.setStrictMode(StrictMode.STRICT);
        expect.addOption(jc);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonObject6_generateSucceed() {
        ExprContext context = getExprContext("json{ with unique keys}");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionCall expect = new FunctionCall("json_object", Collections.emptyList());
        JsonConstraint jc = new JsonConstraint();
        jc.setUniqueMode(UniqueMode.WITH_UNIQUE_KEYS);
        expect.addOption(jc);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonQuery_generateSucceed() {
        ExprContext context = getExprContext("json_query(2 format json, 'aaa')");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p2 = new ExpressionParam(new ConstExpression("2"));
        p2.addOption(new ConstExpression("format json"));
        FunctionParam p3 = new ExpressionParam(new ConstExpression("'aaa'"));
        FunctionCall expect = new FunctionCall("json_query", Arrays.asList(p2, p3));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonQuery1_generateSucceed() {
        ExprContext context = getExprContext("json_query(2 format json, 'aaa' "
                + "returning json truncate ALLOW SCALARS pretty ascii WITH CONDITIONAL WRAPPER "
                + "empty on empty null on error_p dot on mismatch)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p2 = new ExpressionParam(new ConstExpression("2"));
        p2.addOption(new ConstExpression("format json"));
        FunctionParam p3 = new ExpressionParam(new ConstExpression("'aaa'"));
        FunctionCall expect = new FunctionCall("json_query", Arrays.asList(p2, p3));
        expect.addOption(new GeneralDataType("json", null));
        expect.addOption(new ConstExpression("truncate"));
        expect.addOption(new ConstExpression("pretty"));
        expect.addOption(new ConstExpression("ascii"));
        JsonConstraint jsonConstraint = new JsonConstraint();
        jsonConstraint.setScalarsMode(ScalarsMode.ALLOW_SCALARS);
        jsonConstraint.setWrapperMode(WrapperMode.WITH_CONDITIONAL_WRAPPER);
        expect.addOption(jsonConstraint);
        JsonOnOption jsonOnOption = new JsonOnOption();
        jsonOnOption.setOnMismatches(Collections.singletonList(new OnMismatch(new ConstExpression("dot"), null)));
        jsonOnOption.setOnEmpty(new ConstExpression("empty"));
        jsonOnOption.setOnError(new NullExpression());
        expect.addOption(jsonOnOption);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonQuery2_generateSucceed() {
        String[] wrapper = new String[] {
                "WITHOUT WRAPPER",
                "WITHOUT ARRAY WRAPPER",
                "WITH WRAPPER",
                "WITH ARRAY WRAPPER",
                "WITH UNCONDITIONAL WRAPPER",
                "WITH CONDITIONAL WRAPPER",
                "WITH UNCONDITIONAL ARRAY WRAPPER",
                "WITH CONDITIONAL ARRAY WRAPPER"
        };
        for (String s : wrapper) {
            ExprContext context = getExprContext(
                    "json_query(2 format json, 'aaa' " + s + " null on mismatch)");
            StatementFactory<Expression> factory = new OracleExpressionFactory(context);
            Expression actual = factory.generate();

            FunctionParam p2 = new ExpressionParam(new ConstExpression("2"));
            p2.addOption(new ConstExpression("format json"));
            FunctionParam p3 = new ExpressionParam(new ConstExpression("'aaa'"));
            FunctionCall expect = new FunctionCall("json_query", Arrays.asList(p2, p3));
            JsonConstraint jsonConstraint = new JsonConstraint();
            jsonConstraint.setWrapperMode(WrapperMode.valueOf(s.replace(" ", "_")));
            expect.addOption(jsonConstraint);
            JsonOnOption jsonOnOption = new JsonOnOption();
            jsonOnOption.setOnMismatches(Collections.singletonList(new OnMismatch(new NullExpression(), null)));
            expect.addOption(jsonOnOption);
            Assert.assertEquals(expect, actual);
        }
    }

    @Test
    public void generate_jsonMergepatch_generateSucceed() {
        ExprContext context = getExprContext("json_mergepatch('a',2 PRETTY ASCII)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p2 = new ExpressionParam(new ConstExpression("'a'"));
        FunctionParam p3 = new ExpressionParam(new ConstExpression("2"));
        FunctionCall expect = new FunctionCall("json_mergepatch", Arrays.asList(p2, p3));
        expect.addOption(new ConstExpression("PRETTY"));
        expect.addOption(new ConstExpression("ASCII"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonMergepatch1_generateSucceed() {
        ExprContext context = getExprContext("json_mergepatch('a',2 returning json PRETTY ASCII null on error_p)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p2 = new ExpressionParam(new ConstExpression("'a'"));
        FunctionParam p3 = new ExpressionParam(new ConstExpression("2"));
        FunctionCall expect = new FunctionCall("json_mergepatch", Arrays.asList(p2, p3));
        expect.addOption(new GeneralDataType("json", null));
        expect.addOption(new ConstExpression("PRETTY"));
        expect.addOption(new ConstExpression("ASCII"));
        JsonOnOption jsonOnOption = new JsonOnOption();
        jsonOnOption.setOnError(new NullExpression());
        expect.addOption(jsonOnOption);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonMergepatch2_generateSucceed() {
        ExprContext context =
                getExprContext(
                        "json_mergepatch('a',2 returning varchar2(12) binary TRUNCATE PRETTY ASCII error_p on error_p)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p2 = new ExpressionParam(new ConstExpression("'a'"));
        FunctionParam p3 = new ExpressionParam(new ConstExpression("2"));
        FunctionCall expect = new FunctionCall("json_mergepatch", Arrays.asList(p2, p3));
        CharacterType characterType = new CharacterType("varchar2", new BigDecimal("12"));
        characterType.setBinary(true);
        expect.addOption(characterType);
        expect.addOption(new ConstExpression("TRUNCATE"));
        expect.addOption(new ConstExpression("PRETTY"));
        expect.addOption(new ConstExpression("ASCII"));
        JsonOnOption jsonOnOption = new JsonOnOption();
        jsonOnOption.setOnError(new ConstExpression("error_p"));
        expect.addOption(jsonOnOption);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonArray_generateSucceed() {
        ExprContext context = getExprContext("json_array()");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionCall expect = new FunctionCall("json_array", Collections.emptyList());
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonArray1_generateSucceed() {
        ExprContext context = getExprContext("json['a', 1 format json]");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p2 = new ExpressionParam(new ConstExpression("'a'"));
        FunctionParam p3 = new ExpressionParam(new ConstExpression("1"));
        p3.addOption(new ConstExpression("format json"));
        FunctionCall expect = new FunctionCall("json_array", Arrays.asList(p2, p3));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonArray2_generateSucceed() {
        ExprContext context = getExprContext("json['a', 1 format json absent on null returning json]");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p2 = new ExpressionParam(new ConstExpression("'a'"));
        FunctionParam p3 = new ExpressionParam(new ConstExpression("1"));
        p3.addOption(new ConstExpression("format json"));
        FunctionCall expect = new FunctionCall("json_array", Arrays.asList(p2, p3));
        JsonOnOption jsonOnOption = new JsonOnOption();
        jsonOnOption.setOnNull(new ConstExpression("absent"));
        expect.addOption(jsonOnOption);
        expect.addOption(new GeneralDataType("json", null));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonArray3_generateSucceed() {
        ExprContext context = getExprContext("json['a', 1 format json null on null strict]");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p2 = new ExpressionParam(new ConstExpression("'a'"));
        FunctionParam p3 = new ExpressionParam(new ConstExpression("1"));
        p3.addOption(new ConstExpression("format json"));
        FunctionCall expect = new FunctionCall("json_array", Arrays.asList(p2, p3));
        JsonOnOption jsonOnOption = new JsonOnOption();
        jsonOnOption.setOnNull(new NullExpression());
        expect.addOption(jsonOnOption);
        JsonConstraint jc = new JsonConstraint();
        jc.setStrictMode(StrictMode.STRICT);
        expect.addOption(jc);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonValue_generateSucceed() {
        ExprContext context = getExprContext("json_value(1 format json, 'aaa')");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p2 = new ExpressionParam(new ConstExpression("1"));
        p2.addOption(new ConstExpression("format json"));
        FunctionParam p3 = new ExpressionParam(new ConstExpression("'aaa'"));
        FunctionCall expect = new FunctionCall("json_value", Arrays.asList(p2, p3));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonValue1_generateSucceed() {
        ExprContext context = getExprContext("json_value(1 format json, 'aaa' returning nchar(2) "
                + "truncate ascii default 1 on error_p null on empty ignore on mismatch(MISSING DATA) null on mismatch())");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p2 = new ExpressionParam(new ConstExpression("1"));
        p2.addOption(new ConstExpression("format json"));
        FunctionParam p3 = new ExpressionParam(new ConstExpression("'aaa'"));
        FunctionCall expect = new FunctionCall("json_value", Arrays.asList(p2, p3));
        expect.addOption(new CharacterType("nchar", new BigDecimal("2")));
        expect.addOption(new ConstExpression("truncate"));
        expect.addOption(new ConstExpression("ascii"));
        JsonOnOption jsonOnOption = new JsonOnOption();
        jsonOnOption.setOnError(new ConstExpression("1"));
        jsonOnOption.setOnEmpty(new NullExpression());
        jsonOnOption.setOnMismatches(Arrays.asList(new OnMismatch(new ConstExpression("ignore"),
                Collections.singletonList("MISSING DATA")),
                new OnMismatch(new NullExpression(), Collections.emptyList())));
        expect.addOption(jsonOnOption);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonValue2_generateSucceed() {
        ExprContext context = getExprContext("json_value(1 format json, 'aaa' returning varchar2(2) binary "
                + "truncate ascii error_p on error_p default 1 on empty ignore on mismatch(MISSING DATA) null on mismatch())");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p2 = new ExpressionParam(new ConstExpression("1"));
        p2.addOption(new ConstExpression("format json"));
        FunctionParam p3 = new ExpressionParam(new ConstExpression("'aaa'"));
        FunctionCall expect = new FunctionCall("json_value", Arrays.asList(p2, p3));
        CharacterType characterType = new CharacterType("varchar2", new BigDecimal("2"));
        characterType.setBinary(true);
        expect.addOption(characterType);
        expect.addOption(new ConstExpression("truncate"));
        expect.addOption(new ConstExpression("ascii"));
        JsonOnOption jsonOnOption = new JsonOnOption();
        jsonOnOption.setOnEmpty(new ConstExpression("1"));
        jsonOnOption.setOnError(new ConstExpression("error_p"));
        jsonOnOption.setOnMismatches(Arrays.asList(new OnMismatch(new ConstExpression("ignore"),
                Collections.singletonList("MISSING DATA")),
                new OnMismatch(new NullExpression(), Collections.emptyList())));
        expect.addOption(jsonOnOption);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonValue4_generateSucceed() {
        ExprContext context = getExprContext("json_value(1 format json, 'aaa' returning char "
                + "truncate ascii error_p on error_p default 1 on empty ignore on mismatch(MISSING DATA) null on mismatch())");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p2 = new ExpressionParam(new ConstExpression("1"));
        p2.addOption(new ConstExpression("format json"));
        FunctionParam p3 = new ExpressionParam(new ConstExpression("'aaa'"));
        FunctionCall expect = new FunctionCall("json_value", Arrays.asList(p2, p3));
        CharacterType characterType = new CharacterType("char", null);
        expect.addOption(characterType);
        expect.addOption(new ConstExpression("truncate"));
        expect.addOption(new ConstExpression("ascii"));
        JsonOnOption jsonOnOption = new JsonOnOption();
        jsonOnOption.setOnEmpty(new ConstExpression("1"));
        jsonOnOption.setOnError(new ConstExpression("error_p"));
        jsonOnOption.setOnMismatches(Arrays.asList(new OnMismatch(new ConstExpression("ignore"),
                Collections.singletonList("MISSING DATA")),
                new OnMismatch(new NullExpression(), Collections.emptyList())));
        expect.addOption(jsonOnOption);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonValue5_generateSucceed() {
        ExprContext context = getExprContext("json_value(1 format json, 'aaa' returning raw "
                + "truncate ascii error_p on error_p default 1 on empty ignore on mismatch(MISSING DATA) null on mismatch())");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p2 = new ExpressionParam(new ConstExpression("1"));
        p2.addOption(new ConstExpression("format json"));
        FunctionParam p3 = new ExpressionParam(new ConstExpression("'aaa'"));
        FunctionCall expect = new FunctionCall("json_value", Arrays.asList(p2, p3));
        expect.addOption(new GeneralDataType("raw", null));
        expect.addOption(new ConstExpression("truncate"));
        expect.addOption(new ConstExpression("ascii"));
        JsonOnOption jsonOnOption = new JsonOnOption();
        jsonOnOption.setOnEmpty(new ConstExpression("1"));
        jsonOnOption.setOnError(new ConstExpression("error_p"));
        jsonOnOption.setOnMismatches(Arrays.asList(new OnMismatch(new ConstExpression("ignore"),
                Collections.singletonList("MISSING DATA")),
                new OnMismatch(new NullExpression(), Collections.emptyList())));
        expect.addOption(jsonOnOption);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonValue3_generateSucceed() {
        ExprContext context = getExprContext("json_value(1 format json, 'aaa' returning nvarchar2 "
                + "truncate ascii error_p on error_p default 1 on empty ignore on mismatch(MISSING DATA) null on mismatch())");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p2 = new ExpressionParam(new ConstExpression("1"));
        p2.addOption(new ConstExpression("format json"));
        FunctionParam p3 = new ExpressionParam(new ConstExpression("'aaa'"));
        FunctionCall expect = new FunctionCall("json_value", Arrays.asList(p2, p3));
        CharacterType characterType = new CharacterType("nvarchar2", null);
        expect.addOption(characterType);
        expect.addOption(new ConstExpression("truncate"));
        expect.addOption(new ConstExpression("ascii"));
        JsonOnOption jsonOnOption = new JsonOnOption();
        jsonOnOption.setOnEmpty(new ConstExpression("1"));
        jsonOnOption.setOnError(new ConstExpression("error_p"));
        jsonOnOption.setOnMismatches(Arrays.asList(new OnMismatch(new ConstExpression("ignore"),
                Collections.singletonList("MISSING DATA")),
                new OnMismatch(new NullExpression(), Collections.emptyList())));
        expect.addOption(jsonOnOption);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonTable_generateSucceed() {
        ExprContext context = getExprContext("json_table('123' columns \"abcd\" FOR ORDINALITY)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p1 = new ExpressionParam(new ConstExpression("'123'"));
        FunctionCall expect = new FunctionCall("json_table", Collections.singletonList(p1));
        FunctionParam p2 = new ExpressionParam(new ColumnReference(null, null, "\"abcd\""));
        p2.addOption(new ConstExpression("FOR ORDINALITY"));
        expect.addOption(p2);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonTable1_generateSucceed() {
        ExprContext context = getExprContext("json_table('123' format json, 'aaa' columns "
                + "\"abcd\" FOR ORDINALITY, "
                + "col1 exists, "
                + "col2 json, "
                + "col3 format json, "
                + "col4, "
                + "nested path 123 columns(col5))");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p1 = new ExpressionParam(new ConstExpression("'123'"));
        p1.addOption(new ConstExpression("format json"));
        FunctionParam p2 = new ExpressionParam(new ConstExpression("'aaa'"));
        FunctionCall expect = new FunctionCall("json_table", Arrays.asList(p1, p2));

        FunctionParam op1 = new ExpressionParam(new ColumnReference(null, null, "\"abcd\""));
        op1.addOption(new ConstExpression("FOR ORDINALITY"));
        expect.addOption(op1);
        FunctionParam op2 = new ExpressionParam(new ColumnReference(null, null, "col1"));
        op2.addOption(new ConstExpression("exists"));
        expect.addOption(op2);
        FunctionParam op3 = new ExpressionParam(new ColumnReference(null, null, "col2"));
        op3.addOption(new GeneralDataType("json", null));
        expect.addOption(op3);
        FunctionParam op4 = new ExpressionParam(new ColumnReference(null, null, "col3"));
        op4.addOption(new ConstExpression("format json"));
        expect.addOption(op4);
        FunctionParam op5 = new ExpressionParam(new ColumnReference(null, null, "col4"));
        expect.addOption(op5);
        FunctionParam op6 = new ExpressionParam(new ConstExpression("nested path"));
        op6.addOption(new ConstExpression("123"));
        op6.addOption(new ExpressionParam(new ColumnReference(null, null, "col5")));
        expect.addOption(op6);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonTable2_generateSucceed() {
        ExprContext context = getExprContext("json_table('123' format json, 'aaa' "
                + "error_p on error_p null on empty columns "
                + "\"abcd\" FOR ORDINALITY, "
                + "col1 int truncate exists path 123 asis true on empty, "
                + "col2 json DISALLOW SCALARS WITH CONDITIONAL ARRAY WRAPPER path col21 asis empty on empty, "
                + "col3 blob format json truncate allow SCALARS WITH ARRAY WRAPPER path col31 asis empty on empty, "
                + "col4 nchar(12) truncate path col41[*] asis default -3 on empty, "
                + "nested path 123 columns(col5))");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p1 = new ExpressionParam(new ConstExpression("'123'"));
        p1.addOption(new ConstExpression("format json"));
        FunctionParam p2 = new ExpressionParam(new ConstExpression("'aaa'"));
        FunctionCall expect = new FunctionCall("json_table", Arrays.asList(p1, p2));
        JsonOnOption onOption = new JsonOnOption();
        onOption.setOnError(new ConstExpression("error_p"));
        onOption.setOnEmpty(new NullExpression());
        expect.addOption(onOption);

        FunctionParam op1 = new ExpressionParam(new ColumnReference(null, null, "\"abcd\""));
        op1.addOption(new ConstExpression("FOR ORDINALITY"));
        expect.addOption(op1);
        FunctionParam op2 = new ExpressionParam(new ColumnReference(null, null, "col1"));
        op2.addOption(new NumberType("int", null, null));
        op2.addOption(new ConstExpression("truncate"));
        op2.addOption(new ConstExpression("exists"));
        op2.addOption(new ConstExpression("123"));
        onOption = new JsonOnOption();
        onOption.setOnEmpty(new BoolValue(true));
        op2.addOption(onOption);
        expect.addOption(op2);
        FunctionParam op3 = new ExpressionParam(new ColumnReference(null, null, "col2"));
        op3.addOption(new GeneralDataType("json", null));
        JsonConstraint jc = new JsonConstraint();
        jc.setScalarsMode(ScalarsMode.DISALLOW_SCALARS);
        jc.setWrapperMode(WrapperMode.WITH_CONDITIONAL_ARRAY_WRAPPER);
        op3.addOption(jc);
        op3.addOption(new ColumnReference(null, null, "col21"));
        onOption = new JsonOnOption();
        onOption.setOnEmpty(new ConstExpression("empty"));
        op3.addOption(onOption);
        expect.addOption(op3);

        FunctionParam op4 = new ExpressionParam(new ColumnReference(null, null, "col3"));
        op4.addOption(new GeneralDataType("blob", null));
        op4.addOption(new ConstExpression("format json"));
        op4.addOption(new ConstExpression("truncate"));
        jc = new JsonConstraint();
        jc.setScalarsMode(ScalarsMode.ALLOW_SCALARS);
        jc.setWrapperMode(WrapperMode.WITH_ARRAY_WRAPPER);
        op4.addOption(jc);
        op4.addOption(new ColumnReference(null, null, "col31"));
        op4.addOption(onOption);
        expect.addOption(op4);

        FunctionParam op5 = new ExpressionParam(new ColumnReference(null, null, "col4"));
        op5.addOption(new CharacterType("nchar", new BigDecimal("12")));
        op5.addOption(new ConstExpression("truncate"));
        ColumnReference rc = new ColumnReference(null, null, "col41");
        CollectionExpression es = new CollectionExpression();
        es.addExpression(new ConstExpression("*"));
        rc.reference(es, ReferenceOperator.BRACKET);
        op5.addOption(rc);
        onOption = new JsonOnOption();
        onOption.setOnEmpty(new CompoundExpression(new ConstExpression("3"), null, Operator.SUB));
        op5.addOption(onOption);
        expect.addOption(op5);
        FunctionParam op6 = new ExpressionParam(new ConstExpression("nested path"));
        op6.addOption(new ConstExpression("123"));
        op6.addOption(new ExpressionParam(new ColumnReference(null, null, "col5")));
        expect.addOption(op6);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonTable3_generateSucceed() {
        ExprContext context = getExprContext("json_table('123' format json, 'aaa' "
                + "error_p on error_p default -5 on empty columns "
                + "\"abcd\" FOR ORDINALITY, "
                + "col1 int truncate exists path 123 asis true on empty, "
                + "col2 json WITH CONDITIONAL ARRAY WRAPPER path col21 asis empty on empty, "
                + "col3 blob format json truncate allow SCALARS path col31 asis empty on empty, "
                + "col4 nchar(12) truncate path col41[*] asis default -3 on empty, "
                + "nested path 123 columns(col5))");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionParam p1 = new ExpressionParam(new ConstExpression("'123'"));
        p1.addOption(new ConstExpression("format json"));
        FunctionParam p2 = new ExpressionParam(new ConstExpression("'aaa'"));
        FunctionCall expect = new FunctionCall("json_table", Arrays.asList(p1, p2));
        JsonOnOption onOption = new JsonOnOption();
        onOption.setOnError(new ConstExpression("error_p"));
        onOption.setOnEmpty(new CompoundExpression(new ConstExpression("5"), null, Operator.SUB));
        expect.addOption(onOption);

        FunctionParam op1 = new ExpressionParam(new ColumnReference(null, null, "\"abcd\""));
        op1.addOption(new ConstExpression("FOR ORDINALITY"));
        expect.addOption(op1);
        FunctionParam op2 = new ExpressionParam(new ColumnReference(null, null, "col1"));
        op2.addOption(new NumberType("int", null, null));
        op2.addOption(new ConstExpression("truncate"));
        op2.addOption(new ConstExpression("exists"));
        op2.addOption(new ConstExpression("123"));
        onOption = new JsonOnOption();
        onOption.setOnEmpty(new BoolValue(true));
        op2.addOption(onOption);
        expect.addOption(op2);
        FunctionParam op3 = new ExpressionParam(new ColumnReference(null, null, "col2"));
        op3.addOption(new GeneralDataType("json", null));
        JsonConstraint jc = new JsonConstraint();
        jc.setWrapperMode(WrapperMode.WITH_CONDITIONAL_ARRAY_WRAPPER);
        op3.addOption(jc);
        op3.addOption(new ColumnReference(null, null, "col21"));
        onOption = new JsonOnOption();
        onOption.setOnEmpty(new ConstExpression("empty"));
        op3.addOption(onOption);
        expect.addOption(op3);

        FunctionParam op4 = new ExpressionParam(new ColumnReference(null, null, "col3"));
        op4.addOption(new GeneralDataType("blob", null));
        op4.addOption(new ConstExpression("format json"));
        op4.addOption(new ConstExpression("truncate"));
        jc = new JsonConstraint();
        jc.setScalarsMode(ScalarsMode.ALLOW_SCALARS);
        op4.addOption(jc);
        op4.addOption(new ColumnReference(null, null, "col31"));
        op4.addOption(onOption);
        expect.addOption(op4);

        FunctionParam op5 = new ExpressionParam(new ColumnReference(null, null, "col4"));
        op5.addOption(new CharacterType("nchar", new BigDecimal("12")));
        op5.addOption(new ConstExpression("truncate"));
        ColumnReference rc = new ColumnReference(null, null, "col41");
        CollectionExpression es = new CollectionExpression();
        es.addExpression(new ConstExpression("*"));
        rc.reference(es, ReferenceOperator.BRACKET);
        op5.addOption(rc);
        onOption = new JsonOnOption();
        onOption.setOnEmpty(new CompoundExpression(new ConstExpression("3"), null, Operator.SUB));
        op5.addOption(onOption);
        expect.addOption(op5);
        FunctionParam op6 = new ExpressionParam(new ConstExpression("nested path"));
        op6.addOption(new ConstExpression("123"));
        op6.addOption(new ExpressionParam(new ColumnReference(null, null, "col5")));
        expect.addOption(op6);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonEqualExpr_generateSucceed() {
        ExprContext context = getExprContext("json_equal('[1,]', '[1]' false on error_p)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        ExpressionParam p1 = new ExpressionParam(new ConstExpression("'[1,]'"));
        ExpressionParam p2 = new ExpressionParam(new ConstExpression("'[1]'"));
        FunctionCall expect = new FunctionCall("json_equal", Arrays.asList(p1, p2));
        JsonOnOption jsonOnOption = new JsonOnOption();
        jsonOnOption.setOnError(new BoolValue(false));
        expect.addOption(jsonOnOption);
        Assert.assertEquals(expect, actual);
    }

    private Bit_exprContext getBitExprContext(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.bit_expr();
    }

    private PredicateContext getPredicateContext(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.predicate();
    }

    private ExprContext getExprContext(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.expr();
    }

    private Xml_functionContext getXmlExprContext(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.xml_function();
    }

}
