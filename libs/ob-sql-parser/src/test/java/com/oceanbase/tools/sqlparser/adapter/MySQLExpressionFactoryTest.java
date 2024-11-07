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

import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLExpressionFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Bit_exprContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.ExprContext;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.Operator;
import com.oceanbase.tools.sqlparser.statement.common.BraceBlock;
import com.oceanbase.tools.sqlparser.statement.common.CharacterType;
import com.oceanbase.tools.sqlparser.statement.common.GeneralDataType;
import com.oceanbase.tools.sqlparser.statement.common.NumberType;
import com.oceanbase.tools.sqlparser.statement.common.WindowBody;
import com.oceanbase.tools.sqlparser.statement.common.WindowOffset;
import com.oceanbase.tools.sqlparser.statement.common.WindowOffsetType;
import com.oceanbase.tools.sqlparser.statement.common.WindowSpec;
import com.oceanbase.tools.sqlparser.statement.common.WindowType;
import com.oceanbase.tools.sqlparser.statement.expression.*;
import com.oceanbase.tools.sqlparser.statement.select.OrderBy;
import com.oceanbase.tools.sqlparser.statement.select.SortDirection;
import com.oceanbase.tools.sqlparser.statement.select.SortKey;

/**
 * {@link MySQLExpressionFactoryTest}
 *
 * @author yh263208
 * @date 2022-12-10 22:24
 * @since ODC_release_4.1.0
 */
public class MySQLExpressionFactoryTest {

    @Test
    public void generate_dotKeywordKeyword_generateSucceed() {
        ExprContext context = getExprContext(".BEFORE.BEFORE");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        ColumnReference expect = new ColumnReference(null, "BEFORE", "BEFORE");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_dotRelationColumn_generateSucceed() {
        ExprContext context = getExprContext(".tab.col");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        ColumnReference expect = new ColumnReference(null, "tab", "col");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_dotRelationKeyword_generateSucceed() {
        ExprContext context = getExprContext(".tab.BEFORE");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        ColumnReference expect = new ColumnReference(null, "tab", "BEFORE");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_relationRelationColumn_generateSucceed() {
        ExprContext context = getExprContext("chz.tab.col");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        ColumnReference expect = new ColumnReference("chz", "tab", "col");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_relationRelationKeyword_generateSucceed() {
        ExprContext context = getExprContext("chz.tab.BEFORE");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        ColumnReference expect = new ColumnReference("chz", "tab", "BEFORE");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_relationRelationStar_generateSucceed() {
        ExprContext context = getExprContext("chz.tab.*");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        ColumnReference expect = new ColumnReference("chz", "tab", "*");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_constExpr_generateSucceed() {
        ExprContext context = getExprContext("12");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        ConstExpression expect = new ConstExpression("12");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_cnnop_generateSucceed() {
        ExprContext context = getExprContext("12 || 13");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        ConstExpression left = new ConstExpression("12");
        ConstExpression right = new ConstExpression("13");
        Expression expect = new CompoundExpression(left, right, Operator.CNNOP);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_minusExpr_generateSucceed() {
        ExprContext context = getExprContext("-13");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        ConstExpression left = new ConstExpression("13");
        Expression expect = new CompoundExpression(left, null, Operator.SUB);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_exprList_generateSucceed() {
        ExprContext context = getExprContext("(12,13,14)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        CollectionExpression expect = new CollectionExpression();
        expect.addExpression(new ConstExpression("12"));
        expect.addExpression(new ConstExpression("13"));
        expect.addExpression(new ConstExpression("14"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_rowExprList_generateSucceed() {
        ExprContext context = getExprContext("row(12,13,14)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("12")));
        params.add(new ExpressionParam(new ConstExpression("13")));
        params.add(new ExpressionParam(new ConstExpression("14")));
        FunctionCall expect = new FunctionCall("row", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_matchWithoutMode_generateSucceed() {
        ExprContext context = getExprContext("match(col,tab.col,chz.tab.col) against ('abc')");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ColumnReference(null, null, "col")));
        params.add(new ExpressionParam(new ColumnReference(null, "tab", "col")));
        params.add(new ExpressionParam(new ColumnReference("chz", "tab", "col")));
        FullTextSearch expect = new FullTextSearch(params, "'abc'");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_matchNaturalMode_generateSucceed() {
        ExprContext context = getExprContext("match(col,tab.col,chz.tab.col) against ('abc' IN NATURAL LANGUAGE MODE)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ColumnReference(null, null, "col")));
        params.add(new ExpressionParam(new ColumnReference(null, "tab", "col")));
        params.add(new ExpressionParam(new ColumnReference("chz", "tab", "col")));
        FullTextSearch expect = new FullTextSearch(params, "'abc'");
        expect.setSearchMode(TextSearchMode.NATURAL_LANGUAGE_MODE);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_matchNaturalModeWithQueryExpansion_generateSucceed() {
        ExprContext context = getExprContext(
                "match(col,tab.col,chz.tab.col) against ('abc' IN NATURAL LANGUAGE MODE WITH QUERY EXPANSION)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ColumnReference(null, null, "col")));
        params.add(new ExpressionParam(new ColumnReference(null, "tab", "col")));
        params.add(new ExpressionParam(new ColumnReference("chz", "tab", "col")));
        FullTextSearch expect = new FullTextSearch(params, "'abc'");
        expect.setWithQueryExpansion(true);
        expect.setSearchMode(TextSearchMode.NATURAL_LANGUAGE_MODE);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_matchBooleanMode_generateSucceed() {
        ExprContext context = getExprContext("match(col,tab.col,chz.tab.col) against ('abc' IN BOOLEAN MODE)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ColumnReference(null, null, "col")));
        params.add(new ExpressionParam(new ColumnReference(null, "tab", "col")));
        params.add(new ExpressionParam(new ColumnReference("chz", "tab", "col")));
        FullTextSearch expect = new FullTextSearch(params, "'abc'");
        expect.setSearchMode(TextSearchMode.BOOLEAN_MODE);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_userVariable_generateSucceed() {
        ExprContext context = getExprContext("@user_var");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        Expression expect = new ConstExpression("@user_var");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnUserVariables_generateSucceed() {
        ExprContext context = getExprContext("a.b@user_var");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        ColumnReference expect = new ColumnReference(null, "a", "b");
        expect.setUserVariable("@user_var");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnUserVariables1_generateSucceed() {
        ExprContext context = getExprContext("db.a.b@user_var");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        ColumnReference expect = new ColumnReference("db", "a", "b");
        expect.setUserVariable("@user_var");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_complexStringLiteral_generateSucceed() {
        ExprContext context = getExprContext("tab.col -> _UTF8 'str'");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        Expression left = new ColumnReference(null, "tab", "col");
        Expression right = new ConstExpression("_UTF8 'str'");
        Expression expect = new CompoundExpression(left, right, Operator.JSON_EXTRACT);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_complexStringLiteralExtract_generateSucceed() {
        ExprContext context = getExprContext("tab.col ->> _UTF8 'str'");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        Expression left = new ColumnReference(null, "tab", "col");
        Expression right = new ConstExpression("_UTF8 'str'");
        Expression expect = new CompoundExpression(left, right, Operator.JSON_EXTRACT_UNQUOTED);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_countAllStar_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("count(all *)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("*")));
        FunctionCall expect = new FunctionCall("count", params);
        expect.addOption(new ConstExpression("all"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_countAllExprList_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("count(unique a,b,c)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ColumnReference(null, null, "a")));
        params.add(new ExpressionParam(new ColumnReference(null, null, "b")));
        params.add(new ExpressionParam(new ColumnReference(null, null, "c")));
        FunctionCall expect = new FunctionCall("count", params);
        expect.addOption(new ConstExpression("unique"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_st_asmvt_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("_st_asmvt(a.b, 'abcd', 12, -34, null)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ColumnReference(null, "a", "b")));
        params.add(new ExpressionParam(new ConstExpression("'abcd'")));
        params.add(new ExpressionParam(new ConstExpression("12")));
        params.add(new ExpressionParam(new CompoundExpression(new ConstExpression("34"), null, Operator.SUB)));
        params.add(new ExpressionParam(new NullExpression()));

        FunctionCall expect = new FunctionCall("_st_asmvt", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_lastRefreshScn_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("last_refresh_scn(123)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionCall expect = new FunctionCall("last_refresh_scn",
                Collections.singletonList(new ExpressionParam(new ConstExpression("123"))));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_sumOpnsize_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("sum_Opnsize(123)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        FunctionCall expect = new FunctionCall("sum_Opnsize",
                Collections.singletonList(new ExpressionParam(new ConstExpression("123"))));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_stddevPopExpr_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("STDDEV_POP(all 1)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("1")));
        FunctionCall expect = new FunctionCall("STDDEV_POP", params);
        expect.addOption(new ConstExpression("all"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_topKFreHistBitExpr_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("TOP_K_FRE_HIST(1, 1+2, tab.col)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("1")));
        Expression left = new ConstExpression("1");
        Expression right = new ConstExpression("2");
        params.add(new ExpressionParam(new CompoundExpression(left, right, Operator.ADD)));
        params.add(new ExpressionParam(new ColumnReference(null, "tab", "col")));
        FunctionCall expect = new FunctionCall("TOP_K_FRE_HIST", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_defaultColumnRef_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("default(tab.col)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ColumnReference(null, "tab", "col")));
        FunctionCall expect = new FunctionCall("default", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_functionNameExprAsList_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("function_name(tab.col as new_label)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        ExpressionParam p = new ExpressionParam(new ColumnReference(null, "tab", "col"));
        p.addOption(new ConstExpression("new_label"));
        params.add(p);
        FunctionCall expect = new FunctionCall("function_name", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_functionNameExprAsListAsString_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("function_name(tab.col as 'new_label')");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        ExpressionParam p = new ExpressionParam(new ColumnReference(null, "tab", "col"));
        p.addOption(new ConstExpression("'new_label'"));
        params.add(p);
        FunctionCall expect = new FunctionCall("function_name", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_functionNameExprAsListNoAlias_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("function_name(tab.col)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ColumnReference(null, "tab", "col")));
        FunctionCall expect = new FunctionCall("function_name", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_relationNameFunctionNameExprAsListNoAlias_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("relation_name.function_name(tab.col)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ColumnReference(null, "tab", "col")));
        FunctionCall expect = new FunctionCall("relation_name.function_name", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_groupConcat_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("GROUP_CONCAT(distinct tab.col, col order by col desc SEPARATOR ',')");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ColumnReference(null, "tab", "col")));

        FunctionParam p = new ExpressionParam(new ColumnReference(null, null, "col"));
        params.add(p);
        GroupConcat expect = new GroupConcat(params);
        expect.addOption(new ConstExpression("distinct"));
        SortKey sortKey = new SortKey(new ColumnReference(null, null, "col"), SortDirection.DESC);
        OrderBy orderBy = new OrderBy(Collections.singletonList(sortKey));
        expect.addOption(orderBy);
        expect.addOption(new ConstExpression("SEPARATOR ','"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_castAsChar_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("cast('abc' as character(15) binary array)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        FunctionParam p = new ExpressionParam(new ConstExpression("'abc'"));
        CharacterType type = new CharacterType("character", new BigDecimal(15));
        type.setBinary(true);
        p.addOption(type);
        p.addOption(new ConstExpression("array"));
        params.add(p);
        FunctionCall expect = new FunctionCall("cast", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_castAsNumber_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("cast('abc' as numeric(3,2))");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        FunctionParam p = new ExpressionParam(new ConstExpression("'abc'"));
        NumberType type = new NumberType("numeric", new BigDecimal(3), new BigDecimal(2));
        p.addOption(type);
        params.add(p);
        FunctionCall expect = new FunctionCall("cast", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_castAsFloat_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("cast('abc' as float(2))");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        FunctionParam p = new ExpressionParam(new ConstExpression("'abc'"));
        NumberType type = new NumberType("float", new BigDecimal("2"), null);
        p.addOption(type);
        params.add(p);
        FunctionCall expect = new FunctionCall("cast", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_castAsJson_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("cast('abc' as json)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        FunctionParam p = new ExpressionParam(new ConstExpression("'abc'"));
        p.addOption(new GeneralDataType("json", null));
        params.add(p);
        FunctionCall expect = new FunctionCall("cast", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_castAsYear_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("cast('abc' as year(3))");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        FunctionParam ppp = new ExpressionParam(new ConstExpression("'abc'"));
        GeneralDataType type = new GeneralDataType("year", Collections.singletonList("3"));
        ppp.addOption(type);
        params.add(ppp);
        FunctionCall expect = new FunctionCall("cast", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_castAsDatetime_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("cast('abc' as datetime(3))");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        FunctionParam p = new ExpressionParam(new ConstExpression("'abc'"));
        GeneralDataType type = new GeneralDataType("datetime", Collections.singletonList("3"));
        p.addOption(type);
        params.add(p);
        FunctionCall expect = new FunctionCall("cast", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_convertAsBinary_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("convert('abc', binary(12))");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        FunctionParam p = new ExpressionParam(new ConstExpression("'abc'"));
        p.addOption(new GeneralDataType("binary", Collections.singletonList("12")));
        params.add(p);
        FunctionCall expect = new FunctionCall("convert", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_convertAsUnsignedInteger_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("convert('123', unsigned integer)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        FunctionParam p = new ExpressionParam(new ConstExpression("'123'"));
        p.addOption(new GeneralDataType("unsigned integer", Collections.emptyList()));
        params.add(p);
        FunctionCall expect = new FunctionCall("convert", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_convertUsingUtf8_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("convert('123' using utf8)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        FunctionParam p = new ExpressionParam(new ConstExpression("'123'"));
        p.addOption(new ConstExpression("utf8"));
        params.add(p);
        FunctionCall expect = new FunctionCall("convert", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_position_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("position('123' in (1,col.tab))");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        Expression left = new ConstExpression("'123'");
        CollectionExpression right = new CollectionExpression();
        right.addExpression(new ConstExpression("1"));
        right.addExpression(new ColumnReference(null, "col", "tab"));
        params.add(new ExpressionParam(new CompoundExpression(left, right, Operator.IN)));
        FunctionCall expect = new FunctionCall("position", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_substring_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("SUBSTRING('123' from 'abc' for 123)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("'123'")));
        params.add(new ExpressionParam(new ConstExpression("'abc'")));
        params.add(new ExpressionParam(new ConstExpression("123")));
        FunctionCall expect = new FunctionCall("SUBSTRING", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_trim_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("trim(both '123' from 'abc')");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        FunctionParam p = new ExpressionParam(new ConstExpression("'123'"));
        p.addOption(new ConstExpression("'abc'"));
        params.add(p);
        FunctionCall expect = new FunctionCall("trim", params);
        expect.addOption(new ConstExpression("both"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_trim1_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("trim(both from 'abc')");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        FunctionParam p = new ExpressionParam(new ConstExpression("'abc'"));
        params.add(p);
        FunctionCall expect = new FunctionCall("trim", params);
        expect.addOption(new ConstExpression("both"));
        expect.addOption(new ConstExpression("from"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_getFormat_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("GET_FORMAT(datetime, '2022')");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("datetime")));
        params.add(new ExpressionParam(new ConstExpression("'2022'")));
        FunctionCall expect = new FunctionCall("GET_FORMAT", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_dateAdd_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("DATE_ADD(tab, INTERVAL '12' DAY_HOUR)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ColumnReference(null, null, "tab")));
        params.add(new ExpressionParam(new IntervalExpression(new ConstExpression("'12'"), "DAY_HOUR")));
        FunctionCall expect = new FunctionCall("DATE_ADD", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_timestampDiff_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("TIMESTAMPDIFF(DAY, tab,'12')");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("DAY")));
        params.add(new ExpressionParam(new ColumnReference(null, null, "tab")));
        params.add(new ExpressionParam(new ConstExpression("'12'")));
        FunctionCall expect = new FunctionCall("TIMESTAMPDIFF", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_extract_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("extract(day from 123)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        FunctionParam p = new ExpressionParam(new ConstExpression("day"));
        p.addOption(new ConstExpression("123"));
        params.add(p);
        FunctionCall expect = new FunctionCall("extract", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_characterUsingUtf8_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("character('123', 'abc' using utf8)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("'123'")));
        FunctionParam p = new ExpressionParam(new ConstExpression("'abc'"));
        params.add(p);
        FunctionCall expect = new FunctionCall("character", params);
        expect.addOption(new ConstExpression("using utf8"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_weightString_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("WEIGHT_STRING(123 AS CHARACTER (12))");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        FunctionParam p = new ExpressionParam(new ConstExpression("123"));
        p.addOption(new CharacterType("CHARACTER", new BigDecimal("12")));
        params.add(p);
        FunctionCall expect = new FunctionCall("WEIGHT_STRING", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_weightStringBinary_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("WEIGHT_STRING(123 AS Binary (12))");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        FunctionParam p = new ExpressionParam(new ConstExpression("123"));
        p.addOption(new GeneralDataType("Binary", Collections.singletonList("12")));
        params.add(p);
        FunctionCall expect = new FunctionCall("WEIGHT_STRING", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_weightString3Int_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("WEIGHT_STRING('123', 1,2,3,4)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("'123'")));
        params.add(new ExpressionParam(new ConstExpression("1")));
        params.add(new ExpressionParam(new ConstExpression("2")));
        params.add(new ExpressionParam(new ConstExpression("3")));
        params.add(new ExpressionParam(new ConstExpression("4")));
        FunctionCall expect = new FunctionCall("WEIGHT_STRING", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonQueryExpr_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("JSON_QUERY('123', _utf8 'abc')");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("'123'")));
        params.add(new ExpressionParam(new ConstExpression("_utf8 'abc'")));
        FunctionCall expect = new FunctionCall("JSON_QUERY", params);
        expect.addOption(new JsonOption());
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonQueryExprFullOpts_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("JSON_QUERY('123', _utf8 'abc' " +
                "returning double " +
                "TRUNCATE " +
                "allow scalars " +
                "pretty " +
                "ASCII " +
                "with unconditional array wrapper " +
                "asis " +
                "empty on empty empty array on error_p error_p on mismatch " +
                "MULTIVALUE)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("'123'")));
        params.add(new ExpressionParam(new ConstExpression("_utf8 'abc'")));
        FunctionCall expect = new FunctionCall("JSON_QUERY", params);
        expect.addOption(new NumberType("double", null, null));
        JsonOption jsonOpt = new JsonOption();
        jsonOpt.setTruncate(true);
        jsonOpt.setScalarsMode(JsonOption.ScalarsMode.ALLOW_SCALARS);
        jsonOpt.setPretty(true);
        jsonOpt.setAscii(true);
        jsonOpt.setMultiValue(true);
        jsonOpt.setWrapperMode(JsonOption.WrapperMode.WITH_UNCONDITIONAL_ARRAY_WRAPPER);
        jsonOpt.setAsis(true);
        JsonOnOption jsonOnOption = new JsonOnOption();
        jsonOnOption.setOnEmpty(new ConstExpression("empty"));
        jsonOnOption.setOnError(new ConstExpression("empty array"));
        jsonOnOption.setOnMismatches(Collections.singletonList(
                new JsonOnOption.OnMismatch(new ConstExpression("error_p"), null)));
        jsonOpt.setOnOption(jsonOnOption);
        expect.addOption(jsonOpt);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonQueryExprFullOpts2_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("JSON_QUERY('123', _utf8 'abc' " +
                "returning double " +
                "empty array on empty empty array on error_p error_p on mismatch)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("'123'")));
        params.add(new ExpressionParam(new ConstExpression("_utf8 'abc'")));
        FunctionCall expect = new FunctionCall("JSON_QUERY", params);
        expect.addOption(new NumberType("double", null, null));
        JsonOption jsonOpt = new JsonOption();
        JsonOnOption jsonOnOption = new JsonOnOption();
        jsonOnOption.setOnEmpty(new ConstExpression("empty array"));
        jsonOnOption.setOnError(new ConstExpression("empty array"));
        jsonOnOption.setOnMismatches(Collections.singletonList(
                new JsonOnOption.OnMismatch(new ConstExpression("error_p"), null)));
        jsonOpt.setOnOption(jsonOnOption);
        expect.addOption(jsonOpt);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonQueryExprFullOpts4_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("JSON_QUERY('123', _utf8 'abc' " +
                "returning double " +
                "empty object on empty empty array on error_p error_p on mismatch)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("'123'")));
        params.add(new ExpressionParam(new ConstExpression("_utf8 'abc'")));
        FunctionCall expect = new FunctionCall("JSON_QUERY", params);
        expect.addOption(new NumberType("double", null, null));
        JsonOption jsonOpt = new JsonOption();
        JsonOnOption jsonOnOption = new JsonOnOption();
        jsonOnOption.setOnEmpty(new ConstExpression("empty object"));
        jsonOnOption.setOnError(new ConstExpression("empty array"));
        jsonOnOption.setOnMismatches(Collections.singletonList(
                new JsonOnOption.OnMismatch(new ConstExpression("error_p"), null)));
        jsonOpt.setOnOption(jsonOnOption);
        expect.addOption(jsonOpt);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonQueryExpr1_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("JSON_QUERY('123', _utf8 'abc' " +
                "returning double " +
                "with array wrapper)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("'123'")));
        params.add(new ExpressionParam(new ConstExpression("_utf8 'abc'")));
        FunctionCall expect = new FunctionCall("JSON_QUERY", params);
        expect.addOption(new NumberType("double", null, null));
        JsonOption jsonOpt = new JsonOption();
        jsonOpt.setWrapperMode(JsonOption.WrapperMode.WITH_ARRAY_WRAPPER);
        expect.addOption(jsonOpt);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonQueryExpr2_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("JSON_QUERY('123', _utf8 'abc' " +
                "returning double " +
                "with conditional wrapper)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("'123'")));
        params.add(new ExpressionParam(new ConstExpression("_utf8 'abc'")));
        FunctionCall expect = new FunctionCall("JSON_QUERY", params);
        expect.addOption(new NumberType("double", null, null));
        JsonOption jsonOpt = new JsonOption();
        jsonOpt.setWrapperMode(JsonOption.WrapperMode.WITH_CONDITIONAL_WRAPPER);
        expect.addOption(jsonOpt);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonQueryExpr3_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("JSON_QUERY('123', _utf8 'abc' " +
                "returning double " +
                "with unconditional wrapper)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("'123'")));
        params.add(new ExpressionParam(new ConstExpression("_utf8 'abc'")));
        FunctionCall expect = new FunctionCall("JSON_QUERY", params);
        expect.addOption(new NumberType("double", null, null));
        JsonOption jsonOpt = new JsonOption();
        jsonOpt.setWrapperMode(JsonOption.WrapperMode.WITH_UNCONDITIONAL_WRAPPER);
        expect.addOption(jsonOpt);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonQueryExpr4_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("JSON_QUERY('123', _utf8 'abc' " +
                "returning double " +
                "with wrapper)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("'123'")));
        params.add(new ExpressionParam(new ConstExpression("_utf8 'abc'")));
        FunctionCall expect = new FunctionCall("JSON_QUERY", params);
        expect.addOption(new NumberType("double", null, null));
        JsonOption jsonOpt = new JsonOption();
        jsonOpt.setWrapperMode(JsonOption.WrapperMode.WITH_WRAPPER);
        expect.addOption(jsonOpt);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonQueryExpr5_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("JSON_QUERY('123', _utf8 'abc' " +
                "returning double " +
                "without wrapper)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("'123'")));
        params.add(new ExpressionParam(new ConstExpression("_utf8 'abc'")));
        FunctionCall expect = new FunctionCall("JSON_QUERY", params);
        expect.addOption(new NumberType("double", null, null));
        JsonOption jsonOpt = new JsonOption();
        jsonOpt.setWrapperMode(JsonOption.WrapperMode.WITHOUT_WRAPPER);
        expect.addOption(jsonOpt);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonQueryExpr6_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("JSON_QUERY('123', _utf8 'abc' " +
                "returning double " +
                "without array wrapper)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("'123'")));
        params.add(new ExpressionParam(new ConstExpression("_utf8 'abc'")));
        FunctionCall expect = new FunctionCall("JSON_QUERY", params);
        expect.addOption(new NumberType("double", null, null));
        JsonOption jsonOpt = new JsonOption();
        jsonOpt.setWrapperMode(JsonOption.WrapperMode.WITHOUT_ARRAY_WRAPPER);
        expect.addOption(jsonOpt);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonQueryExprFullOpts1_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("JSON_QUERY('123', _utf8 'abc' " +
                "returning double " +
                "TRUNCATE " +
                "disallow scalars " +
                "pretty " +
                "ASCII " +
                "with conditional array wrapper " +
                "asis " +
                "error_p on empty empty object on error_p dot on mismatch " +
                "MULTIVALUE)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("'123'")));
        params.add(new ExpressionParam(new ConstExpression("_utf8 'abc'")));
        FunctionCall expect = new FunctionCall("JSON_QUERY", params);
        expect.addOption(new NumberType("double", null, null));
        JsonOption jsonOpt = new JsonOption();
        jsonOpt.setTruncate(true);
        jsonOpt.setScalarsMode(JsonOption.ScalarsMode.DISALLOW_SCALARS);
        jsonOpt.setPretty(true);
        jsonOpt.setAscii(true);
        jsonOpt.setWrapperMode(JsonOption.WrapperMode.WITH_CONDITIONAL_ARRAY_WRAPPER);
        jsonOpt.setAsis(true);
        jsonOpt.setMultiValue(true);
        JsonOnOption jsonOnOption = new JsonOnOption();
        jsonOnOption.setOnEmpty(new ConstExpression("error_p"));
        jsonOnOption.setOnError(new ConstExpression("empty object"));
        jsonOnOption.setOnMismatches(Collections.singletonList(
                new JsonOnOption.OnMismatch(new ConstExpression("dot"), null)));
        jsonOpt.setOnOption(jsonOnOption);
        expect.addOption(jsonOpt);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonValueExpr_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("JSON_VALUE('123', _utf8 'abc' returning double TRUNCATE ASCII)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("'123'")));
        FunctionParam p = new ExpressionParam(new ConstExpression("_utf8 'abc'"));
        params.add(p);
        FunctionCall expect = new FunctionCall("JSON_VALUE", params);
        expect.addOption(new NumberType("double", null, null));
        JsonOption jsonOpt = new JsonOption();
        jsonOpt.setTruncate(true);
        jsonOpt.setAscii(true);
        expect.addOption(jsonOpt);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonValueExprOnEmpty_generateFunctionCallSucceed() {
        ExprContext context =
                getExprContext("JSON_VALUE('123', _utf8 'abc' returning double TRUNCATE ASCII error_p on empty)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("'123'")));
        FunctionParam p = new ExpressionParam(new ConstExpression("_utf8 'abc'"));
        params.add(p);
        FunctionCall expect = new FunctionCall("JSON_VALUE", params);
        expect.addOption(new NumberType("double", null, null));
        JsonOption jsonOpt = new JsonOption();
        jsonOpt.setTruncate(true);
        jsonOpt.setAscii(true);
        JsonOnOption jsonOnOption = new JsonOnOption();
        jsonOnOption.setOnEmpty(new ConstExpression("error_p"));
        jsonOpt.setOnOption(jsonOnOption);
        expect.addOption(jsonOpt);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonValueExprOnError_generateFunctionCallSucceed() {
        ExprContext context =
                getExprContext("JSON_VALUE('123', _utf8 'abc' returning double TRUNCATE ASCII null on error_p)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("'123'")));
        FunctionParam p = new ExpressionParam(new ConstExpression("_utf8 'abc'"));
        params.add(p);
        FunctionCall expect = new FunctionCall("JSON_VALUE", params);
        expect.addOption(new NumberType("double", null, null));
        JsonOption jsonOpt = new JsonOption();
        jsonOpt.setTruncate(true);
        jsonOpt.setAscii(true);
        JsonOnOption jsonOnOption = new JsonOnOption();
        jsonOnOption.setOnError(new NullExpression());
        jsonOpt.setOnOption(jsonOnOption);
        expect.addOption(jsonOpt);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonValueExprOnErrorEmpty_generateFunctionCallSucceed() {
        ExprContext context = getExprContext(
                "JSON_VALUE('123', _utf8 'abc' returning double TRUNCATE ASCII default 12 on empty null on error_p)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("'123'")));
        FunctionParam p = new ExpressionParam(new ConstExpression("_utf8 'abc'"));
        params.add(p);
        FunctionCall expect = new FunctionCall("JSON_VALUE", params);
        expect.addOption(new NumberType("double", null, null));
        JsonOption jsonOpt = new JsonOption();
        jsonOpt.setTruncate(true);
        jsonOpt.setAscii(true);
        JsonOnOption jsonOnOption = new JsonOnOption();
        jsonOnOption.setOnError(new NullExpression());
        jsonOnOption.setOnEmpty(new ConstExpression("12"));
        jsonOpt.setOnOption(jsonOnOption);
        expect.addOption(jsonOpt);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_jsonValueExprNoOpt_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("JSON_VALUE('123', _utf8 'abc' returning double)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("'123'")));
        FunctionParam p = new ExpressionParam(new ConstExpression("_utf8 'abc'"));
        params.add(p);
        FunctionCall expect = new FunctionCall("JSON_VALUE", params);
        expect.addOption(new NumberType("double", null, null));
        expect.addOption(new JsonOption());
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_cur_timestamp_func_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("CURRENT_TIMESTAMP(123)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("123")));
        FunctionCall expect = new FunctionCall("CURRENT_TIMESTAMP", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_sysdate_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("sysdate(123)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("123")));
        FunctionCall expect = new FunctionCall("sysdate", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_cur_time_func_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("CURRENT_TIME(123)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("123")));
        FunctionCall expect = new FunctionCall("CURRENT_TIME", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_cur_date_func_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("CURRENT_DATE()");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        FunctionCall expect = new FunctionCall("CURRENT_DATE", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_utc_timestamp_func_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("UTC_TIMESTAMP(12)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("12")));
        FunctionCall expect = new FunctionCall("UTC_TIMESTAMP", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_sys_interval_func_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("INTERVAL(12, 13,15)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("12")));
        params.add(new ExpressionParam(new ConstExpression("13")));
        params.add(new ExpressionParam(new ConstExpression("15")));
        FunctionCall expect = new FunctionCall("INTERVAL", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_sys_interval_func_check_generateFunctionCallSucceed() {
        ExprContext context = getExprContext("CHECK(12)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("12")));
        FunctionCall expect = new FunctionCall("CHECK", params);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_boolPriIsTrue_generateSucceed() {
        ExprContext context = getExprContext("abc is true");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        Expression left = new ColumnReference(null, null, "abc");
        Expression right = new BoolValue(true);
        Expression expect = new CompoundExpression(left, right, Operator.EQ);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_boolPriIsNotUnknown_generateSucceed() {
        ExprContext context = getExprContext("abc is not unknown");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        Expression left = new ColumnReference(null, null, "abc");
        Expression right = new ConstExpression("unknown");
        Expression expect = new CompoundExpression(left, right, Operator.NE);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_notPri_generateSucceed() {
        ExprContext context = getExprContext("not abc");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        Expression left = new ColumnReference(null, null, "abc");
        Expression expect = new CompoundExpression(left, null, Operator.NOT);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_userVarSet_generateSucceed() {
        ExprContext context = getExprContext("@user_var := abc");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        Expression right = new ColumnReference(null, null, "abc");
        Expression left = new ConstExpression("@user_var");
        Expression expect = new CompoundExpression(left, right, Operator.SET_VAR);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_trueAndFalse_generateSucceed() {
        ExprContext context = getExprContext("true && false");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        Expression left = new ConstExpression("true");
        Expression right = new ConstExpression("false");
        Expression expect = new CompoundExpression(left, right, Operator.AND);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_boolPriAddBoolPri_generateSucceed() {
        ExprContext context = getExprContext("1>2+3");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        Expression left = new ConstExpression("1");
        Expression right = new CompoundExpression(new ConstExpression("2"), new ConstExpression("3"), Operator.ADD);
        Expression expect = new CompoundExpression(left, right, Operator.GT);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_colIsNotNull_generateSucceed() {
        ExprContext context = getExprContext("tab.col is not null");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        Expression left = new ColumnReference(null, "tab", "col");
        Expression right = new NullExpression();
        Expression expect = new CompoundExpression(left, right, Operator.NE);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_predicateNotIn_generateSucceed() {
        ExprContext context = getExprContext("tab.col not in ('abc', 'bcd')");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        Expression left = new ColumnReference(null, "tab", "col");
        CollectionExpression right = new CollectionExpression();
        right.addExpression(new ConstExpression("'abc'"));
        right.addExpression(new ConstExpression("'bcd'"));
        Expression expect = new CompoundExpression(left, right, Operator.NOT_IN);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_predicateNotBetween_generateSucceed() {
        ExprContext context = getExprContext("tab.col not between col and col1");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        Expression left = new ColumnReference(null, "tab", "col");
        Expression right = new CompoundExpression(new ColumnReference(null, null, "col"),
                new ColumnReference(null, null, "col1"), Operator.AND);
        Expression expect = new CompoundExpression(left, right, Operator.NOT_BETWEEN);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_predicateNotLike_generateSucceed() {
        ExprContext context = getExprContext("tab.col not like 'abc' 'bcd'");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        Expression left = new ColumnReference(null, "tab", "col");
        CollectionExpression right = new CollectionExpression();
        right.addExpression(new ConstExpression("'abc'"));
        right.addExpression(new ConstExpression("'bcd'"));
        Expression expect = new CompoundExpression(left, right, Operator.NOT_LIKE);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_predicateNotLikeSimpleExpr_generateSucceed() {
        ExprContext context = getExprContext("tab.col not like abc");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        Expression left = new ColumnReference(null, "tab", "col");
        Expression right = new ColumnReference(null, null, "abc");
        Expression expect = new CompoundExpression(left, right, Operator.NOT_LIKE);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_predicateNotLikeEscapeSimpleExpr_generateSucceed() {
        ExprContext context = getExprContext("tab.col not like abc escape bcd");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        Expression left = new ColumnReference(null, "tab", "col");
        Expression right = new CompoundExpression(new ColumnReference(null, null, "abc"),
                new ColumnReference(null, null, "bcd"), Operator.ESCAPE);
        Expression expect = new CompoundExpression(left, right, Operator.NOT_LIKE);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_predicateNotLikeEscapeStringList_generateSucceed() {
        ExprContext context = getExprContext("tab.col not like 'abc' escape 'bcd' 'abcde'");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        Expression left = new ColumnReference(null, "tab", "col");
        Expression e1 = new ConstExpression("'abc'");
        CollectionExpression e2 = new CollectionExpression();
        e2.addExpression(new ConstExpression("'bcd'"));
        e2.addExpression(new ConstExpression("'abcde'"));
        Expression right = new CompoundExpression(e1, e2, Operator.ESCAPE);
        Expression expect = new CompoundExpression(left, right, Operator.NOT_LIKE);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_predicateNotLikeEscapeMix_generateSucceed() {
        ExprContext context = getExprContext("tab.col not like 'abc' escape col");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        Expression left = new ColumnReference(null, "tab", "col");
        Expression e1 = new ConstExpression("'abc'");
        Expression e2 = new ColumnReference(null, null, "col");
        Expression right = new CompoundExpression(e1, e2, Operator.ESCAPE);
        Expression expect = new CompoundExpression(left, right, Operator.NOT_LIKE);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_notRegExp_generateSucceed() {
        ExprContext context = getExprContext("tab.col not regexp col");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        Expression left = new ColumnReference(null, "tab", "col");
        Expression right = new ColumnReference(null, null, "col");
        Expression expect = new CompoundExpression(left, right, Operator.NOT_REGEXP);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_notRegExpStringList_generateSucceed() {
        ExprContext context = getExprContext("tab.col not regexp 'col'");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        Expression left = new ColumnReference(null, "tab", "col");
        CollectionExpression right = new CollectionExpression();
        right.addExpression(new ConstExpression("'col'"));
        Expression expect = new CompoundExpression(left, right, Operator.NOT_REGEXP);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_memberOf_generateSucceed() {
        ExprContext context = getExprContext("tab.col member of(col)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        Expression left = new ColumnReference(null, "tab", "col");
        Expression right = new ColumnReference(null, null, "col");
        Expression expect = new CompoundExpression(left, right, Operator.MEMBER_OF);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_bitExprInterval_generateSucceed() {
        Bit_exprContext context = getBitExprContext("1+ interval 4 day");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        Expression left = new ConstExpression("1");
        Expression right = new IntervalExpression(new ConstExpression("4"), "day");
        Expression expect = new CompoundExpression(left, right, Operator.ADD);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_intervalBitExpr_generateSucceed() {
        Bit_exprContext context = getBitExprContext("interval 4 day + 1");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        Expression right = new ConstExpression("1");
        Expression left = new IntervalExpression(new ConstExpression("4"), "day");
        Expression expect = new CompoundExpression(left, right, Operator.ADD);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_countStarNameWin_generateSucceed() {
        Bit_exprContext context = getBitExprContext("count(all *) over name_ob");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("*")));
        FunctionCall expect = new FunctionCall("count", params);
        WindowSpec window = new WindowSpec();
        window.setName("name_ob");
        expect.setWindow(window);
        expect.addOption(new ConstExpression("all"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_distinctExprListWithoutWinBody_generateSucceed() {
        Bit_exprContext context =
                getBitExprContext("count(distinct 5,6) over (name_ob partition by (1,2) order by col desc)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("5")));
        params.add(new ExpressionParam(new ConstExpression("6")));
        FunctionCall expect = new FunctionCall("count", params);
        WindowSpec window = new WindowSpec();
        window.setName("name_ob");
        CollectionExpression p = new CollectionExpression();
        p.addExpression(new ConstExpression("1"));
        p.addExpression(new ConstExpression("2"));
        window.setPartitionBy(Collections.singletonList(p));
        SortKey s = new SortKey(new ColumnReference(null, null, "col"), SortDirection.DESC);
        OrderBy orderBy = new OrderBy(Collections.singletonList(s));
        window.setOrderBy(orderBy);
        expect.setWindow(window);
        expect.addOption(new ConstExpression("distinct"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_uniqueGroupConcatExprListWithWinBody_generateSucceed() {
        Bit_exprContext context = getBitExprContext(
                "GROUP_CONCAT(unique 5,6 order by col1 asc SEPARATOR 'mmm') over (name_ob partition by (1,2) order by col desc rows between "
                        + "current row and 123 PRECEDING)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("5")));
        FunctionParam pppp = new ExpressionParam(new ConstExpression("6"));
        params.add(pppp);
        GroupConcat expect = new GroupConcat(params);
        expect.addOption(new ConstExpression("unique"));
        WindowSpec window = new WindowSpec();
        window.setName("name_ob");
        CollectionExpression p = new CollectionExpression();
        p.addExpression(new ConstExpression("1"));
        p.addExpression(new ConstExpression("2"));
        window.setPartitionBy(Collections.singletonList(p));
        SortKey s = new SortKey(new ColumnReference(null, null, "col"), SortDirection.DESC);
        OrderBy orderBy = new OrderBy(Collections.singletonList(s));
        window.setOrderBy(orderBy);
        WindowOffset begin = new WindowOffset(WindowOffsetType.CURRENT_ROW);
        WindowOffset end = new WindowOffset(WindowOffsetType.PRECEDING);
        end.setInterval(new ConstExpression("123"));
        WindowBody body = new WindowBody(WindowType.ROWS, begin, end);
        window.setBody(body);
        expect.setWindow(window);
        SortKey s0 = new SortKey(new ColumnReference(null, null, "col1"), SortDirection.ASC);
        expect.addOption(new OrderBy(Collections.singletonList(s0)));
        expect.addOption(new ConstExpression("SEPARATOR 'mmm'"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_uniqueListaggExprListWithWinBody_generateSucceed() {
        Bit_exprContext context = getBitExprContext(
                "LISTAGG(unique 5,6 order by col1 asc SEPARATOR 'mmm') over (name_ob partition by (1,2) order by col desc rows between "
                        + "current row and interval 123 day FOLLOWING)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("5")));
        FunctionParam ppp = new ExpressionParam(new ConstExpression("6"));
        params.add(ppp);
        FunctionCall expect = new FunctionCall("LISTAGG", params);
        expect.addOption(new ConstExpression("unique"));
        WindowSpec window = new WindowSpec();
        window.setName("name_ob");
        CollectionExpression p = new CollectionExpression();
        p.addExpression(new ConstExpression("1"));
        p.addExpression(new ConstExpression("2"));
        window.setPartitionBy(Collections.singletonList(p));
        SortKey s = new SortKey(new ColumnReference(null, null, "col"), SortDirection.DESC);
        OrderBy orderBy = new OrderBy(Collections.singletonList(s));
        window.setOrderBy(orderBy);
        WindowOffset begin = new WindowOffset(WindowOffsetType.CURRENT_ROW);
        WindowOffset end = new WindowOffset(WindowOffsetType.FOLLOWING);
        end.setInterval(new IntervalExpression(new ConstExpression("123"), "day"));
        WindowBody body = new WindowBody(WindowType.ROWS, begin, end);
        window.setBody(body);
        expect.setWindow(window);
        SortKey s0 = new SortKey(new ColumnReference(null, null, "col1"), SortDirection.ASC);
        expect.addOption(new OrderBy(Collections.singletonList(s0)));
        expect.addOption(new ConstExpression("SEPARATOR 'mmm'"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_firstValueWithWinBody_generateSucceed() {
        Bit_exprContext context = getBitExprContext(
                "FIRST_VALUE (5 respect nulls) over (name_ob partition by (1,2) order by col desc RANGE interval 123 day FOLLOWING)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        FunctionParam p1 = new ExpressionParam(new ConstExpression("5"));
        params.add(p1);
        FunctionCall expect = new FunctionCall("FIRST_VALUE", params);
        WindowSpec window = new WindowSpec();
        window.setName("name_ob");
        CollectionExpression p = new CollectionExpression();
        p.addExpression(new ConstExpression("1"));
        p.addExpression(new ConstExpression("2"));
        window.setPartitionBy(Collections.singletonList(p));
        SortKey s = new SortKey(new ColumnReference(null, null, "col"), SortDirection.DESC);
        OrderBy orderBy = new OrderBy(Collections.singletonList(s));
        window.setOrderBy(orderBy);
        WindowOffset offset = new WindowOffset(WindowOffsetType.FOLLOWING);
        offset.setInterval(new IntervalExpression(new ConstExpression("123"), "day"));
        WindowBody body = new WindowBody(WindowType.RANGE, offset);
        window.setBody(body);
        expect.setWindow(window);
        expect.addOption(new ConstExpression("respect nulls"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_nthValueWithWinBody_generateSucceed() {
        Bit_exprContext context = getBitExprContext(
                "NTH_VALUE(5,6) from first respect nulls over (name_ob partition by (1,2) order by col desc RANGE interval 123 day FOLLOWING)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("5")));
        params.add(new ExpressionParam(new ConstExpression("6")));
        FunctionCall expect = new FunctionCall("NTH_VALUE", params);
        expect.addOption(new ConstExpression("from first"));
        expect.addOption(new ConstExpression("respect nulls"));
        WindowSpec window = new WindowSpec();
        window.setName("name_ob");
        CollectionExpression p = new CollectionExpression();
        p.addExpression(new ConstExpression("1"));
        p.addExpression(new ConstExpression("2"));
        window.setPartitionBy(Collections.singletonList(p));
        SortKey s = new SortKey(new ColumnReference(null, null, "col"), SortDirection.DESC);
        OrderBy orderBy = new OrderBy(Collections.singletonList(s));
        window.setOrderBy(orderBy);
        WindowOffset offset = new WindowOffset(WindowOffsetType.FOLLOWING);
        offset.setInterval(new IntervalExpression(new ConstExpression("123"), "day"));
        WindowBody body = new WindowBody(WindowType.RANGE, offset);
        window.setBody(body);
        expect.setWindow(window);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_top_k_fre_histWithWinBody_generateSucceed() {
        Bit_exprContext context = getBitExprContext(
                "TOP_K_FRE_HIST(5,6,7) over (name_ob partition by (1,2) order by col desc RANGE interval 123 day FOLLOWING)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ConstExpression("5")));
        params.add(new ExpressionParam(new ConstExpression("6")));
        params.add(new ExpressionParam(new ConstExpression("7")));
        FunctionCall expect = new FunctionCall("TOP_K_FRE_HIST", params);
        WindowSpec window = new WindowSpec();
        window.setName("name_ob");
        CollectionExpression p = new CollectionExpression();
        p.addExpression(new ConstExpression("1"));
        p.addExpression(new ConstExpression("2"));
        window.setPartitionBy(Collections.singletonList(p));
        SortKey s = new SortKey(new ColumnReference(null, null, "col"), SortDirection.DESC);
        OrderBy orderBy = new OrderBy(Collections.singletonList(s));
        window.setOrderBy(orderBy);
        WindowOffset offset = new WindowOffset(WindowOffsetType.FOLLOWING);
        offset.setInterval(new IntervalExpression(new ConstExpression("123"), "day"));
        WindowBody body = new WindowBody(WindowType.RANGE, offset);
        window.setBody(body);
        expect.setWindow(window);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_neExpression_generateSucceed() {
        ExprContext context = getExprContext("b+2<>20");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        ColumnReference c1 = new ColumnReference(null, null, "b");
        ConstExpression c2 = new ConstExpression("2");
        CompoundExpression left = new CompoundExpression(c1, c2, Operator.ADD);
        ConstExpression right = new ConstExpression("20");
        CompoundExpression expect = new CompoundExpression(left, right, Operator.NE);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_braceExpr_succeed() {
        ExprContext context = getExprContext("{abcd 'aaaa'}");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        BraceBlock expect = new BraceBlock("abcd", new ConstExpression("'aaaa'"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_value_generateCaseWhenSucceed() {
        ExprContext context = getExprContext("CASE a WHEN 1 THEN 11 WHEN 2 THEN 22 ELSE 33 END");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<WhenClause> whenClauses = new ArrayList<>();
        whenClauses.add(new WhenClause(new ConstExpression("1"), new ConstExpression("11")));
        whenClauses.add(new WhenClause(new ConstExpression("2"), new ConstExpression("22")));
        CaseWhen expect = new CaseWhen(whenClauses);
        expect.setCaseValue(new ColumnReference(null, null, "a"));
        expect.setCaseDefault(new ConstExpression("33"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_condition_generateCaseWhenSucceed() {
        ExprContext context = getExprContext("CASE WHEN a < 1 THEN 11 WHEN a < 2 THEN 22 ELSE 33 END");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<WhenClause> whenClauses = new ArrayList<>();
        ColumnReference columnReference = new ColumnReference(null, null, "a");
        whenClauses.add(new WhenClause(new CompoundExpression(columnReference, new ConstExpression("1"), Operator.LT),
                new ConstExpression("11")));
        whenClauses.add(new WhenClause(new CompoundExpression(columnReference, new ConstExpression("2"), Operator.LT),
                new ConstExpression("22")));
        CaseWhen expect = new CaseWhen(whenClauses);
        expect.setCaseDefault(new ConstExpression("33"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_vectorDistanceExpr_Succeed() {
        ExprContext context = getExprContext("VECTOR_DISTANCE(vector1, vector2)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ColumnReference(null, null, "vector1")));
        params.add(new ExpressionParam(new ColumnReference(null, null, "vector2")));
        FunctionCall expected = new FunctionCall("VECTOR_DISTANCE", params);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void generate_vectorDistanceExpr1_Succeed() {
        ExprContext context = getExprContext("VECTOR_DISTANCE(vector1, vector2, COSINE)");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();

        List<FunctionParam> params = new ArrayList<>();
        params.add(new ExpressionParam(new ColumnReference(null, null, "vector1")));
        params.add(new ExpressionParam(new ColumnReference(null, null, "vector2")));
        params.add(new ExpressionParam(new ConstExpression("COSINE")));
        FunctionCall expected = new FunctionCall("VECTOR_DISTANCE", params);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void generate_AnyArrayExpr_1_Succeed() {
        ExprContext context = getExprContext("\"hel\" = ANY([\"hello\", \"hi\"])");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();
        ConstExpression left = new ConstExpression("\"hel\"");
        CollectionExpression right = new CollectionExpression();
        ArrayExpression arrayExpression =
                new ArrayExpression(Arrays.asList(new ConstExpression("\"hello\""), new ConstExpression("\"hi\"")));
        right.addExpression(arrayExpression);
        CompoundExpression expected = new CompoundExpression(left, right, Operator.EQ);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void generate_AnyArrayExpr_2_Succeed() {
        ExprContext context = getExprContext("[3,4] = ANY([[1,2],[3,4]])");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();
        ArrayExpression left = new ArrayExpression(Arrays.asList(new ConstExpression("3"), new ConstExpression("4")));
        CollectionExpression right = new CollectionExpression();
        ArrayExpression arrayExpression1 =
                new ArrayExpression(Arrays.asList(new ConstExpression("1"), new ConstExpression("2")));
        ArrayExpression arrayExpression2 =
                new ArrayExpression(Arrays.asList(new ConstExpression("3"), new ConstExpression("4")));
        ArrayExpression arrayExpression = new ArrayExpression(Arrays.asList(arrayExpression1, arrayExpression2));
        right.addExpression(arrayExpression);
        CompoundExpression expected = new CompoundExpression(left, right, Operator.EQ);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void generate_AnyArrayExprNested_Succeed() {
        ExprContext context =
                getExprContext("[\"are You?\"] = ANY([[\"hello\", \"world\"], [\"hi\", \"what\"], [\"are you?\"]]);");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();
        ArrayExpression left = new ArrayExpression(Arrays.asList(new ConstExpression("\"are You?\"")));
        CollectionExpression right = new CollectionExpression();
        ArrayExpression arrayExpression1 =
                new ArrayExpression(Arrays.asList(new ConstExpression("\"hello\""), new ConstExpression("\"world\"")));
        ArrayExpression arrayExpression2 =
                new ArrayExpression(Arrays.asList(new ConstExpression("\"hi\""), new ConstExpression("\"what\"")));
        ArrayExpression arrayExpression3 =
                new ArrayExpression(Arrays.asList(new ConstExpression("\"are you?\"")));
        ArrayExpression arrayExpression =
                new ArrayExpression(Arrays.asList(arrayExpression1, arrayExpression2, arrayExpression3));
        right.addExpression(arrayExpression);
        CompoundExpression expected = new CompoundExpression(left, right, Operator.EQ);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void generate_ArrayExpr_1_Succeed() {
        ExprContext context = getExprContext("(array(1,2,3))");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();
        ArrayExpression expected =
                new ArrayExpression(
                        Arrays.asList(new ConstExpression("1"), new ConstExpression("2"), new ConstExpression("3")));
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void generate_ArrayExpr_2_Succeed() {
        ExprContext context = getExprContext("([1,2,3])");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();
        ArrayExpression expected =
                new ArrayExpression(
                        Arrays.asList(new ConstExpression("1"), new ConstExpression("2"), new ConstExpression("3")));
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void generate_ArrayExpr_3_Succeed() {
        ExprContext context = getExprContext("(\"[1,2,3]\")");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();
        ConstExpression expected = new ConstExpression("\"[1,2,3]\"");
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void generate_ArrayContains_Succeed() {
        ExprContext context = getExprContext("array_contains([1,2,3], 2);");
        StatementFactory<Expression> factory = new MySQLExpressionFactory(context);
        Expression actual = factory.generate();
        List<FunctionParam> params = new ArrayList<>();
        ArrayExpression arrayExpression = new ArrayExpression(
                Arrays.asList(new ConstExpression("1"), new ConstExpression("2"), new ConstExpression("3")));
        ConstExpression constExpression = new ConstExpression("2");
        params.add(new ExpressionParam(arrayExpression));
        params.add(new ExpressionParam(constExpression));
        FunctionCall expected = new FunctionCall("array_contains", params);
        Assert.assertEquals(expected, actual);
    }



    private ExprContext getExprContext(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.expr();
    }

    private Bit_exprContext getBitExprContext(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.bit_expr();
    }

}
