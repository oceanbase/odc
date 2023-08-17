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
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.Operator;
import com.oceanbase.tools.sqlparser.statement.common.CharacterType;
import com.oceanbase.tools.sqlparser.statement.common.WindowBody;
import com.oceanbase.tools.sqlparser.statement.common.WindowOffset;
import com.oceanbase.tools.sqlparser.statement.common.WindowOffsetType;
import com.oceanbase.tools.sqlparser.statement.common.WindowSpec;
import com.oceanbase.tools.sqlparser.statement.common.WindowType;
import com.oceanbase.tools.sqlparser.statement.expression.BoolValue;
import com.oceanbase.tools.sqlparser.statement.expression.CaseWhen;
import com.oceanbase.tools.sqlparser.statement.expression.CollectionExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.expression.CompoundExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ExpressionParam;
import com.oceanbase.tools.sqlparser.statement.expression.FullTextSearch;
import com.oceanbase.tools.sqlparser.statement.expression.FunctionAccess;
import com.oceanbase.tools.sqlparser.statement.expression.FunctionCall;
import com.oceanbase.tools.sqlparser.statement.expression.FunctionParam;
import com.oceanbase.tools.sqlparser.statement.expression.NullExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ParamWithAssign;
import com.oceanbase.tools.sqlparser.statement.expression.RelationReference;
import com.oceanbase.tools.sqlparser.statement.expression.TextSearchMode;
import com.oceanbase.tools.sqlparser.statement.expression.WhenClause;
import com.oceanbase.tools.sqlparser.statement.expression.WindowFunction;
import com.oceanbase.tools.sqlparser.statement.select.OrderBy;
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
    public void generate_sum_generateBoolValueSucceed() {
        Bit_exprContext context = getBitExprContext("sum(tab.col)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        RelationReference r = new RelationReference("tab", new RelationReference("col", null));
        FunctionCall expect = new FunctionCall("sum", Collections.singletonList(new ExpressionParam(r)));
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
        expect.setParamsFlag("ALL");
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
        expect.setParamsFlag("distinct");
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
        expect.setParamsFlag("unique");
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
        expect.setParamsFlag("ALL");
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
        FunctionAccess expect =
                new FunctionAccess("function", Collections.singletonList(p), Collections.singletonList(af));
        expect.setParamsFlag("ALL");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_accessFunctionWithTableIndexFunctionAccess_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("function(ALL 1)(1)(2)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        ExpressionParam p = new ExpressionParam(new ConstExpression("1"));
        FunctionAccess expect = new FunctionAccess("function", Collections.singletonList(p),
                Arrays.asList(new ConstExpression("1"), new ConstExpression("2")));
        expect.setParamsFlag("ALL");
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
        params.add(new ExpressionParam(new ConstExpression("'avc'")));
        params.add(new ExpressionParam(new ConstExpression("'abc'")));
        FunctionCall expect = new FunctionCall("trim", params);
        expect.setParamsFlag("both from");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_translateFunctionCall_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("translate('abc' using char_cs)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("'abc'")));
        FunctionCall expect = new FunctionCall("translate", params);
        expect.addParamsOption(new ConstExpression("char_cs"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_extractFunctionCall_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("extract(year from 'abc')");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("'abc'")));
        FunctionCall expect = new FunctionCall("extract", params);
        expect.addParamsOption(new ConstExpression("year"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_castFunctionCall_generateFunctionCallSucceed() {
        Bit_exprContext context = getBitExprContext("cast('abc' as varchar2(64))");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("'abc'")));
        FunctionCall expect = new FunctionCall("cast", params);
        expect.addParamsOption(new CharacterType("varchar2", new BigDecimal("64")));
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
        expect.setParamsFlag("all");
        expect.addParamsOption(new ConstExpression("first"));
        SortKey s = new SortKey(new RelationReference("col", null), SortDirection.DESC);
        expect.addParamsOption(new OrderBy(Collections.singletonList(s)));
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
        expect.setParamsFlag("distinct");
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
        WindowFunction expect = new WindowFunction("count", params);
        WindowSpec window = new WindowSpec();
        expect.setWindow(window);
        expect.setParamsFlag("all");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_distinctBitExprWithoutWinBody_generateSucceed() {
        Bit_exprContext context = getBitExprContext("count(distinct 56) over (partition by (1,2) order by col desc)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("56")));
        WindowFunction expect = new WindowFunction("count", params);
        WindowSpec window = new WindowSpec();
        CollectionExpression p = new CollectionExpression();
        p.addExpression(new ConstExpression("1"));
        p.addExpression(new ConstExpression("2"));
        window.setPartitionBy(Collections.singletonList(p));
        SortKey s = new SortKey(new RelationReference("col", null), SortDirection.DESC);
        OrderBy orderBy = new OrderBy(Collections.singletonList(s));
        window.setOrderBy(orderBy);
        expect.setWindow(window);
        expect.setParamsFlag("distinct");
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
        WindowFunction expect = new WindowFunction("APPROX_COUNT_DISTINCT_SYNOPSIS", params);
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
    public void generate_uniqueBitExprWithWinBody_generateSucceed() {
        Bit_exprContext context = getBitExprContext(
                "min(unique 5) over (partition by (1,2) order by col desc rows between current row and 123 FOLLOWING)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("5")));
        WindowFunction expect = new WindowFunction("min", params);
        expect.setParamsFlag("unique");
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
        params.add(new ExpressionParam(new ConstExpression("5")));
        WindowFunction expect = new WindowFunction("FIRST_VALUE", params);
        expect.addParamsOption(new ConstExpression("respect"));
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
    public void generate_leadWithWinBody_generateSucceed() {
        Bit_exprContext context = getBitExprContext(
                "LEAD (5 respect nulls, 1,2) over (partition by (1,2) order by col desc RANGE 123 FOLLOWING)");
        StatementFactory<Expression> factory = new OracleExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("5")));
        params.add(new ExpressionParam(new ConstExpression("1")));
        params.add(new ExpressionParam(new ConstExpression("2")));
        WindowFunction expect = new WindowFunction("LEAD", params);
        expect.addParamsOption(new ConstExpression("respect"));
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
        WindowFunction expect = new WindowFunction("function_name", params);
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
        WindowFunction expect = new WindowFunction("NTH_VALUE", params);
        expect.addParamsOption(new ConstExpression("first"));
        expect.addParamsOption(new ConstExpression("respect"));
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
        WindowFunction expect = new WindowFunction("max", params);
        expect.addParamsOption(new ConstExpression("first"));
        SortKey s0 = new SortKey(new RelationReference("col", null), SortDirection.ASC);
        expect.addParamsOption(new OrderBy(Collections.singletonList(s0)));
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
        WindowFunction expect = new WindowFunction("WMSYS.WM_CONCAT", params);
        expect.addParamsOption(new ConstExpression("first"));
        SortKey s0 = new SortKey(new RelationReference("col", null), SortDirection.ASC);
        expect.addParamsOption(new OrderBy(Collections.singletonList(s0)));
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

    private Bit_exprContext getBitExprContext(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.bit_expr();
    }

    private ExprContext getExprContext(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.expr();
    }

}
