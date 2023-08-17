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
package com.oceanbase.tools.sqlparser.adapter.mysql;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Bit_exprContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Bool_priContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Case_exprContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Complex_func_exprContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Cur_date_funcContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Cur_time_funcContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Cur_timestamp_funcContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Date_paramsContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.ExprContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Expr_listContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.In_exprContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Json_value_exprContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Parameterized_trimContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.PredicateContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Select_no_parensContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Simple_exprContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Simple_func_exprContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.String_val_listContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Sysdate_funcContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Utc_time_funcContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Utc_timestamp_funcContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Win_fun_first_last_paramsContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Window_functionContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Ws_nweightsContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.Operator;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.common.CharacterType;
import com.oceanbase.tools.sqlparser.statement.common.GeneralDataType;
import com.oceanbase.tools.sqlparser.statement.common.WindowSpec;
import com.oceanbase.tools.sqlparser.statement.expression.BoolValue;
import com.oceanbase.tools.sqlparser.statement.expression.CaseWhen;
import com.oceanbase.tools.sqlparser.statement.expression.CollectionExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.expression.CompoundExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;
import com.oceanbase.tools.sqlparser.statement.expression.DefaultExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ExpressionParam;
import com.oceanbase.tools.sqlparser.statement.expression.FullTextSearch;
import com.oceanbase.tools.sqlparser.statement.expression.FunctionCall;
import com.oceanbase.tools.sqlparser.statement.expression.FunctionParam;
import com.oceanbase.tools.sqlparser.statement.expression.GroupConcat;
import com.oceanbase.tools.sqlparser.statement.expression.IntervalExpression;
import com.oceanbase.tools.sqlparser.statement.expression.NullExpression;
import com.oceanbase.tools.sqlparser.statement.expression.TextSearchMode;
import com.oceanbase.tools.sqlparser.statement.expression.WhenClause;
import com.oceanbase.tools.sqlparser.statement.expression.WindowFunction;
import com.oceanbase.tools.sqlparser.statement.select.OrderBy;

import lombok.NonNull;

/**
 * {@link MySQLExpressionFactory}
 *
 * @author yh263208
 * @date 2022-12-08 12:01
 * @see StatementFactory
 * @since ODC_release_4.1.0
 */
public class MySQLExpressionFactory extends OBParserBaseVisitor<Expression> implements StatementFactory<Expression> {

    private final ExprContext exprContext;
    private final Bit_exprContext bitExprContext;

    public MySQLExpressionFactory(@NonNull ExprContext exprContext) {
        this.bitExprContext = null;
        this.exprContext = exprContext;
    }

    public MySQLExpressionFactory(@NonNull Bit_exprContext bitExprContext) {
        this.exprContext = null;
        this.bitExprContext = bitExprContext;
    }

    @Override
    public Expression generate() {
        if (this.exprContext != null) {
            return visit(this.exprContext);
        }
        return visit(this.bitExprContext);
    }

    @Override
    public Expression visitExpr(ExprContext ctx) {
        if (ctx.bool_pri() != null) {
            Expression left = visit(ctx.bool_pri());
            if (ctx.IS() == null) {
                return left;
            }
            Operator operator = ctx.not() == null ? Operator.EQ : Operator.NE;
            Expression right;
            if (ctx.BOOL_VALUE() != null) {
                right = new BoolValue(ctx.BOOL_VALUE());
            } else {
                right = new ConstExpression(ctx.UNKNOWN().getText());
            }
            return new CompoundExpression(ctx, left, right, operator);
        }
        Expression left = visit(ctx.expr(0));
        if (ctx.expr().size() == 1) {
            if (ctx.NOT() != null) {
                return new CompoundExpression(ctx, left, null, Operator.NOT);
            } else if (ctx.USER_VARIABLE() != null) {
                return new CompoundExpression(ctx, new ConstExpression(ctx.USER_VARIABLE()), left, Operator.SET_VAR);
            }
            return left;
        }
        Expression right = visit(ctx.expr(1));
        Operator operator = null;
        if (ctx.AND() != null || ctx.AND_OP() != null) {
            operator = Operator.AND;
        } else if (ctx.CNNOP() != null) {
            operator = Operator.CNNOP;
        } else if (ctx.OR() != null) {
            operator = Operator.OR;
        } else if (ctx.XOR() != null) {
            operator = Operator.XOR;
        }
        if (operator == null) {
            throw new IllegalStateException("Missing operator");
        }
        return new CompoundExpression(ctx, left, right, operator);
    }

    @Override
    public Expression visitBool_pri(Bool_priContext ctx) {
        if (ctx.bool_pri() == null && ctx.predicate() != null) {
            return visit(ctx.predicate());
        }
        Operator operator = null;
        if (ctx.COMP_EQ() != null) {
            operator = Operator.EQ;
        } else if (ctx.COMP_GE() != null) {
            operator = Operator.GE;
        } else if (ctx.COMP_GT() != null) {
            operator = Operator.GT;
        } else if (ctx.COMP_LE() != null) {
            operator = Operator.LE;
        } else if (ctx.COMP_LT() != null) {
            operator = Operator.LT;
        } else if (ctx.COMP_NE() != null) {
            operator = Operator.NE;
        } else if (ctx.COMP_NSEQ() != null) {
            operator = Operator.NSEQ;
        } else if (ctx.IS() != null) {
            operator = Operator.EQ;
            if (ctx.not() != null) {
                operator = Operator.NE;
            }
        }
        Expression left = null;
        Expression right = null;
        if (ctx.bool_pri() != null && ctx.predicate() != null) {
            left = visit(ctx.bool_pri());
            right = visit(ctx.predicate());
        } else if (ctx.NULLX() != null) {
            left = visit(ctx.bool_pri());
            right = new NullExpression(ctx.bool_pri());
        } else if (ctx.bool_pri() != null && ctx.select_no_parens() != null) {
            left = visit(ctx.bool_pri());
            right = visit(ctx.select_no_parens());
        }
        if (left == null || right == null || operator == null) {
            throw new IllegalStateException("Unable to build expression, some syntax modules are missing");
        }
        return new CompoundExpression(ctx, left, right, operator);
    }

    @Override
    public Expression visitPredicate(PredicateContext ctx) {
        List<Bit_exprContext> bitExprList = ctx.bit_expr();
        if (CollectionUtils.isEmpty(bitExprList)) {
            throw new IllegalStateException("Missing expression");
        }
        Operator operator = null;
        boolean notExist = ctx.not() != null;
        if (ctx.IN() != null) {
            operator = notExist ? Operator.NOT_IN : Operator.IN;
        } else if (ctx.BETWEEN() != null) {
            operator = notExist ? Operator.NOT_BETWEEN : Operator.BETWEEN;
        } else if (ctx.LIKE() != null) {
            operator = notExist ? Operator.NOT_LIKE : Operator.LIKE;
        } else if (ctx.REGEXP() != null) {
            operator = notExist ? Operator.NOT_REGEXP : Operator.REGEXP;
        } else if (ctx.MEMBER() != null) {
            operator = notExist ? Operator.NOT_MEMBER_OF : Operator.MEMBER_OF;
        }
        if (operator == null) {
            if (bitExprList.size() != 1) {
                throw new IllegalStateException("Unknown error");
            }
            return visit(bitExprList.get(0));
        }
        Expression left = visit(bitExprList.get(0));
        Expression right = null;
        if (ctx.in_expr() != null) {
            right = visit(ctx.in_expr());
        } else if (ctx.AND() != null && ctx.predicate() != null) {
            right = new CompoundExpression(ctx, visit(bitExprList.get(1)), visit(ctx.predicate()), Operator.AND);
        } else if (ctx.LIKE() != null) {
            List<Expression> strValList = ctx.string_val_list().stream().map(this::visit).collect(Collectors.toList());
            List<Expression> simpleExprs = ctx.simple_expr().stream().map(this::visit).collect(Collectors.toList());
            if (ctx.ESCAPE() == null) {
                if (strValList.size() == 1) {
                    right = strValList.get(0);
                } else if (simpleExprs.size() == 1) {
                    right = simpleExprs.get(0);
                } else {
                    throw new IllegalStateException("Parser error");
                }
            } else {
                List<Integer> strValIndexes = new ArrayList<>();
                List<Integer> simpleExprIndexes = new ArrayList<>();
                for (int i = 0; i < ctx.getChildCount(); i++) {
                    ParseTree parseTree = ctx.getChild(i);
                    if (parseTree instanceof String_val_listContext) {
                        strValIndexes.add(i);
                    } else if (parseTree instanceof Simple_exprContext) {
                        simpleExprIndexes.add(i);
                    }
                }
                if (simpleExprs.size() == 2) {
                    right = new CompoundExpression(ctx, simpleExprs.get(0), simpleExprs.get(1), Operator.ESCAPE);
                } else if (strValList.size() == 2) {
                    right = new CompoundExpression(ctx, strValList.get(0), strValList.get(1), Operator.ESCAPE);
                } else if (strValList.size() == 1 && simpleExprs.size() == 1) {
                    Integer strValIndex = strValIndexes.get(0);
                    Integer simpleExprIndex = simpleExprIndexes.get(0);
                    Expression first = strValIndex < simpleExprIndex ? strValList.get(0) : simpleExprs.get(0);
                    Expression second = strValIndex < simpleExprIndex ? simpleExprs.get(0) : strValList.get(0);
                    right = new CompoundExpression(ctx, first, second, Operator.ESCAPE);
                } else {
                    throw new IllegalStateException("Missing expression");
                }
            }
        } else if (ctx.REGEXP() != null) {
            if (ctx.string_val_list(0) != null) {
                right = visit(ctx.string_val_list(0));
            } else if (ctx.bit_expr(1) != null) {
                right = visit(ctx.bit_expr(1));
            } else {
                throw new IllegalStateException("Missing expression");
            }
        } else if (ctx.MEMBER() != null && ctx.simple_expr(0) != null) {
            right = visit(ctx.simple_expr(0));
        }
        if (right == null) {
            throw new IllegalStateException("Unknown branch");
        }
        return new CompoundExpression(ctx, left, right, operator);
    }

    @Override
    public Expression visitSimple_expr(Simple_exprContext ctx) {
        List<Simple_exprContext> simpleExprContexts = ctx.simple_expr();
        if (CollectionUtils.isNotEmpty(simpleExprContexts)) {
            Expression left = visit(simpleExprContexts.get(0));
            Operator operator = null;
            if (ctx.CNNOP() != null) {
                operator = Operator.CNNOP;
            } else if (ctx.BINARY() != null) {
                operator = Operator.BINARY;
            } else if (ctx.Plus() != null) {
                operator = Operator.ADD;
            } else if (ctx.Minus() != null) {
                operator = Operator.SUB;
            } else if (ctx.Tilde() != null) {
                operator = Operator.TILDE;
            } else if (ctx.Not() != null || ctx.NOT() != null) {
                operator = Operator.NOT;
            }
            if (simpleExprContexts.size() == 1) {
                if (operator == null) {
                    return left;
                }
                return new CompoundExpression(ctx, left, null, operator);
            }
            return new CompoundExpression(ctx, left, visit(simpleExprContexts.get(1)), Operator.CNNOP);
        } else if (ctx.column_ref() != null) {
            StatementFactory<ColumnReference> factory = new MySQLColumnRefFactory(ctx.column_ref());
            return factory.generate();
        } else if (ctx.expr_const() != null) {
            return new ConstExpression(ctx.expr_const());
        } else if (ctx.expr_list() != null) {
            if (ctx.ROW() == null) {
                return visit(ctx.expr_list());
            }
            List<FunctionParam> params = ctx.expr_list().expr().stream()
                    .map(e -> new ExpressionParam(visit(e))).collect(Collectors.toList());
            return new FunctionCall(ctx, ctx.ROW().getText(), params);
        } else if (ctx.select_with_parens() != null) {
            MySQLSelectBodyFactory factory = new MySQLSelectBodyFactory(ctx.select_with_parens());
            if (ctx.EXISTS() == null) {
                return factory.generate();
            }
            return new CompoundExpression(ctx, factory.generate(), null, Operator.EXISTS);
        } else if (ctx.MATCH() != null) {
            List<FunctionParam> params = ctx.column_list().column_definition_ref().stream().map(c -> {
                StatementFactory<ColumnReference> factory = new MySQLColumnRefFactory(c);
                return new ExpressionParam(factory.generate());
            }).collect(Collectors.toList());
            TextSearchMode searchMode = null;
            if (ctx.BOOLEAN() != null) {
                searchMode = TextSearchMode.BOOLEAN_MODE;
            } else if (ctx.NATURAL() != null) {
                searchMode = TextSearchMode.NATURAL_LANGUAGE_MODE;
            }
            FullTextSearch f = new FullTextSearch(ctx, params, ctx.STRING_VALUE().getText());
            f.setSearchMode(searchMode);
            return f;
        } else if (ctx.func_expr() != null) {
            return visit(ctx.func_expr());
        } else if (ctx.window_function() != null) {
            return visit(ctx.window_function());
        } else if (ctx.USER_VARIABLE() != null) {
            return new ConstExpression(ctx.USER_VARIABLE());
        } else if (ctx.column_definition_ref() != null) {
            StatementFactory<ColumnReference> factory = new MySQLColumnRefFactory(ctx.column_definition_ref());
            Operator operator = ctx.JSON_EXTRACT() == null ? Operator.JSON_EXTRACT_UNQUOTED : Operator.JSON_EXTRACT;
            return new CompoundExpression(ctx, factory.generate(),
                    new ConstExpression(ctx.complex_string_literal()), operator);
        } else if (ctx.case_expr() != null) {
            return visit(ctx.case_expr());
        }
        return new DefaultExpression(ctx);
    }

    @Override
    public Expression visitCase_expr(Case_exprContext ctx) {
        List<WhenClause> whenClauses = ctx.when_clause_list().when_clause().stream().map(
                e -> new WhenClause(e, visit(e.expr(0)), visit(e.expr(1)))).collect(Collectors.toList());
        CaseWhen caseWhen = new CaseWhen(ctx, whenClauses);
        if (ctx.expr() != null) {
            caseWhen.setCaseValue(visit(ctx.expr()));
        }
        if (ctx.case_default() != null) {
            caseWhen.setCaseDefault(visit(ctx.case_default().expr()));
        }
        return caseWhen;
    }

    @Override
    public Expression visitSimple_func_expr(Simple_func_exprContext ctx) {
        String funcName;
        if (ctx.func_name != null) {
            funcName = ctx.func_name.getText();
        } else if (ctx.function_name() != null && ctx.relation_name() == null) {
            funcName = ctx.function_name().getText();
        } else if (ctx.function_name() != null && ctx.relation_name() != null) {
            funcName = ctx.relation_name().getText() + "." + ctx.function_name().getText();
        } else {
            throw new IllegalStateException("Function name is missing");
        }
        List<FunctionParam> params = new ArrayList<>();
        if (ctx.Star() != null) {
            params.add(new ExpressionParam(new ConstExpression(ctx.Star())));
        } else {
            if (CollectionUtils.isNotEmpty(ctx.expr())) {
                params.addAll(ctx.expr().stream()
                        .map(e -> wrap(e, null)).collect(Collectors.toList()));
            } else if (ctx.expr_list() != null) {
                params.addAll(ctx.expr_list().expr().stream()
                        .map(e -> wrap(e, null)).collect(Collectors.toList()));
            } else if (CollectionUtils.isNotEmpty(ctx.bit_expr())) {
                params.addAll(ctx.bit_expr().stream()
                        .map(e -> wrap(e, null)).collect(Collectors.toList()));
            } else if (ctx.column_definition_ref() != null) {
                StatementFactory<ColumnReference> factory =
                        new MySQLColumnRefFactory(ctx.column_definition_ref());
                params.add(new ExpressionParam(factory.generate()));
            } else if (ctx.expr_as_list() != null) {
                params.addAll(ctx.expr_as_list().expr_with_opt_alias().stream().map(e -> {
                    if (e.column_label() == null && e.STRING_VALUE() == null) {
                        return wrap(e.expr(), null);
                    } else if (e.column_label() != null) {
                        return wrap(e.expr(), e.column_label().getText());
                    }
                    return wrap(e.expr(), e.STRING_VALUE().getText());
                }).collect(Collectors.toList()));
            }
        }
        FunctionCall fCall = new FunctionCall(ctx, funcName, params);
        fCall.setParamsFlag(getParamFlags(ctx));
        return fCall;
    }

    @Override
    public Expression visitComplex_func_expr(Complex_func_exprContext ctx) {
        if (ctx.GROUP_CONCAT() != null) {
            List<FunctionParam> params = ctx.expr_list().expr().stream()
                    .map(e -> wrap(e, null)).collect(Collectors.toList());
            GroupConcat fCall = new GroupConcat(ctx, params);
            if (ctx.SEPARATOR() != null) {
                fCall.setSeparator(ctx.STRING_VALUE().getText());
            }
            if (ctx.order_by() != null) {
                StatementFactory<OrderBy> factory = new MySQLOrderByFactory(ctx.order_by());
                fCall.setOrderBy(factory.generate());
            }
            fCall.setParamsFlag(getParamFlags(ctx));
            return fCall;
        } else if (ctx.CAST() != null) {
            FunctionCall fCall = new FunctionCall(ctx, ctx.CAST().getText(),
                    Collections.singletonList(wrap(ctx.expr(), null)));
            fCall.addParamsOption(new MySQLDataTypeFactory(ctx.cast_data_type()).generate());
            return fCall;
        } else if (ctx.CONVERT() != null) {
            FunctionCall fCall = new FunctionCall(ctx, ctx.CONVERT().getText(),
                    Collections.singletonList(wrap(ctx.expr(), null)));
            if (ctx.cast_data_type() != null) {
                fCall.addParamsOption(new MySQLDataTypeFactory(ctx.cast_data_type()).generate());
            }
            if (ctx.charset_name() != null) {
                fCall.addParamsOption(new ConstExpression(ctx.charset_name()));
            }
            return fCall;
        } else if (ctx.POSITION() != null) {
            List<FunctionParam> params = new ArrayList<>();
            params.add(new ExpressionParam(new CompoundExpression(ctx,
                    visit(ctx.bit_expr()), visit(ctx.expr()), Operator.IN)));
            return new FunctionCall(ctx, ctx.POSITION().getText(), params);
        } else if (ctx.substr_or_substring() != null) {
            String funcName = ctx.substr_or_substring().getText();
            List<FunctionParam> params = ctx.substr_params().expr().stream()
                    .map(e -> wrap(e, null)).collect(Collectors.toList());
            return new FunctionCall(ctx, funcName, params);
        } else if (ctx.TRIM() != null) {
            List<FunctionParam> params = ctx.parameterized_trim().expr().stream()
                    .map(e -> wrap(e, null)).collect(Collectors.toList());
            FunctionCall fCall = new FunctionCall(ctx, ctx.TRIM().getText(), params);
            List<String> paramFlags = new ArrayList<>();
            Parameterized_trimContext trim = ctx.parameterized_trim();
            for (int i = 0; i < trim.getChildCount(); i++) {
                ParseTree p = trim.getChild(i);
                if (p instanceof TerminalNode) {
                    paramFlags.add(p.getText());
                }
            }
            if (!paramFlags.isEmpty()) {
                fCall.setParamsFlag(String.join(" ", paramFlags));
            }
            return fCall;
        } else if (ctx.GET_FORMAT() != null) {
            List<FunctionParam> params = new ArrayList<>();
            params.add(new ExpressionParam(new ConstExpression(ctx.get_format_unit())));
            params.add(new ExpressionParam(visit(ctx.expr())));
            return new FunctionCall(ctx, ctx.GET_FORMAT().getText(), params);
        } else if (ctx.DATE_ADD() != null || ctx.DATE_SUB() != null || ctx.ADDDATE() != null || ctx.SUBDATE() != null) {
            String funcName;
            if (ctx.DATE_ADD() != null) {
                funcName = ctx.DATE_ADD().getText();
            } else if (ctx.DATE_SUB() != null) {
                funcName = ctx.DATE_SUB().getText();
            } else if (ctx.ADDDATE() != null) {
                funcName = ctx.ADDDATE().getText();
            } else {
                funcName = ctx.SUBDATE().getText();
            }
            Date_paramsContext p = ctx.date_params();
            List<FunctionParam> params = new ArrayList<>();
            params.add(wrap(p.expr(0), null));
            params.add(new ExpressionParam(new IntervalExpression(p,
                    visit(p.expr(1)), p.date_unit().getText())));
            FunctionCall fCall = new FunctionCall(ctx, funcName, params);
            if (ctx.date_params().date_unit() != null) {
                fCall.addParamsOption(new ConstExpression(ctx.date_params().date_unit()));
            }
            return fCall;
        } else if (ctx.TIMESTAMPDIFF() != null || ctx.TIMESTAMPADD() != null) {
            String funcName;
            if (ctx.TIMESTAMPDIFF() != null) {
                funcName = ctx.TIMESTAMPDIFF().getText();
            } else {
                funcName = ctx.TIMESTAMPADD().getText();
            }
            List<FunctionParam> params = new ArrayList<>();
            params.add(new ExpressionParam(new ConstExpression(ctx.timestamp_params().date_unit())));
            params.addAll(ctx.timestamp_params().expr().stream()
                    .map(e -> wrap(e, null)).collect(Collectors.toList()));
            return new FunctionCall(ctx, funcName, params);
        } else if (ctx.EXTRACT() != null) {
            FunctionCall fCall = new FunctionCall(ctx, ctx.EXTRACT().getText(),
                    Collections.singletonList(wrap(ctx.expr(), null)));
            fCall.addParamsOption(new ConstExpression(ctx.date_unit()));
            return fCall;
        } else if (ctx.CHARACTER() != null && ctx.WEIGHT_STRING() == null) {
            List<FunctionParam> params = ctx.expr_list().expr().stream()
                    .map(e -> wrap(e, null)).collect(Collectors.toList());
            FunctionCall fCall = new FunctionCall(ctx, ctx.CHARACTER().getText(), params);
            fCall.addParamsOption(new ConstExpression(ctx.charset_name()));
            return fCall;
        } else if (ctx.WEIGHT_STRING() != null) {
            String funcName = ctx.WEIGHT_STRING().getText();
            List<FunctionParam> params = new ArrayList<>();
            params.add(new ExpressionParam(visit(ctx.expr())));
            params.addAll(ctx.INTNUM().stream().map(t -> new ExpressionParam(new ConstExpression(t)))
                    .collect(Collectors.toList()));
            FunctionCall fCall = new FunctionCall(ctx, funcName, params);
            if (ctx.ws_nweights() == null) {
                return fCall;
            }
            Ws_nweightsContext weights = ctx.ws_nweights();
            String arg = weights.INTNUM().getText();
            if (ctx.CHARACTER() != null) {
                fCall.addParamsOption(new CharacterType(weights, ctx.CHARACTER().getText(), new BigDecimal(arg)));
            } else if (ctx.BINARY() != null) {
                fCall.addParamsOption(new GeneralDataType(weights, ctx.BINARY().getText(),
                        Collections.singletonList(arg)));
            }
            return fCall;
        } else if (ctx.json_value_expr() != null) {
            Json_value_exprContext jsonValue = ctx.json_value_expr();
            List<FunctionParam> params = new ArrayList<>();
            params.add(new ExpressionParam(visit(jsonValue.simple_expr())));
            params.add(new ExpressionParam(new ConstExpression(jsonValue.complex_string_literal())));
            FunctionCall fCall = new FunctionCall(ctx, jsonValue.JSON_VALUE().getText(), params);
            fCall.addParamsOption(new MySQLDataTypeFactory(jsonValue.cast_data_type()).generate());
            return fCall;
        }
        String funcName = null;
        List<FunctionParam> params = new ArrayList<>();
        if (ctx.cur_timestamp_func() != null) {
            Cur_timestamp_funcContext curTime = ctx.cur_timestamp_func();
            if (curTime.INTNUM() != null) {
                params.add(new ExpressionParam(new ConstExpression(curTime.INTNUM())));
            }
            funcName = curTime.NOW() == null ? curTime.now_synonyms_func().getText() : curTime.NOW().getText();
        } else if (ctx.sysdate_func() != null) {
            Sysdate_funcContext sysdate = ctx.sysdate_func();
            if (sysdate.INTNUM() != null) {
                params.add(new ExpressionParam(new ConstExpression(sysdate.INTNUM())));
            }
            funcName = sysdate.SYSDATE().getText();
        } else if (ctx.cur_time_func() != null) {
            Cur_time_funcContext curTime = ctx.cur_time_func();
            if (curTime.INTNUM() != null) {
                params.add(new ExpressionParam(new ConstExpression(curTime.INTNUM())));
            }
            funcName = curTime.CURTIME() == null ? curTime.CURRENT_TIME().getText() : curTime.CURTIME().getText();
        } else if (ctx.cur_date_func() != null) {
            Cur_date_funcContext curTime = ctx.cur_date_func();
            funcName = curTime.CURDATE() == null ? curTime.CURRENT_DATE().getText() : curTime.CURDATE().getText();
        } else if (ctx.utc_timestamp_func() != null) {
            Utc_timestamp_funcContext utcdate = ctx.utc_timestamp_func();
            if (utcdate.INTNUM() != null) {
                params.add(new ExpressionParam(new ConstExpression(utcdate.INTNUM())));
            }
            funcName = utcdate.UTC_TIMESTAMP().getText();
        } else if (ctx.utc_time_func() != null) {
            Utc_time_funcContext utctime = ctx.utc_time_func();
            if (utctime.INTNUM() != null) {
                params.add(new ExpressionParam(new ConstExpression(utctime.INTNUM())));
            }
            funcName = utctime.UTC_TIME().getText();
        } else if (ctx.utc_date_func() != null) {
            funcName = ctx.utc_date_func().UTC_DATE().getText();
        } else if (ctx.sys_interval_func() != null) {
            params = ctx.sys_interval_func().expr()
                    .stream().map(e -> wrap(e, null)).collect(Collectors.toList());
            funcName = ctx.sys_interval_func().INTERVAL().getText();
        }
        if (funcName == null) {
            throw new IllegalStateException("Missing function name");
        }
        return new FunctionCall(ctx, funcName, params);
    }

    @Override
    public Expression visitWindow_function(Window_functionContext ctx) {
        if (ctx.func_name == null) {
            throw new IllegalStateException("Missing function name");
        }
        String funcName = ctx.func_name.getText();
        String paramsFlag = null;
        StatementFactory<WindowSpec> factory = new MySQLWindowSpecFactory(ctx.new_generalized_window_clause());
        WindowSpec window = factory.generate();
        if (ctx.ALL() != null) {
            paramsFlag = ctx.ALL().getText();
        } else if (ctx.DISTINCT() != null) {
            paramsFlag = ctx.DISTINCT().getText();
        } else if (ctx.UNIQUE() != null) {
            paramsFlag = ctx.UNIQUE().getText();
        }
        List<FunctionParam> params = new ArrayList<>();
        List<Statement> paramsOpts = new ArrayList<>();
        if (ctx.Star() != null) {
            params.add(new ExpressionParam(new ConstExpression(ctx.Star())));
        } else if (CollectionUtils.isNotEmpty(ctx.expr())) {
            params.addAll(ctx.expr().stream().map(e -> new ExpressionParam(visit(e))).collect(Collectors.toList()));
        } else if (ctx.expr_list() != null) {
            params.addAll(ctx.expr_list().expr().stream().map(e -> new ExpressionParam(visit(e)))
                    .collect(Collectors.toList()));
        } else if (CollectionUtils.isNotEmpty(ctx.bit_expr())) {
            params.addAll(ctx.bit_expr().stream().map(e -> new ExpressionParam(visit(e))).collect(Collectors.toList()));
        } else if (ctx.win_fun_first_last_params() != null) {
            Win_fun_first_last_paramsContext c = ctx.win_fun_first_last_params();
            params.add(new ExpressionParam(visit(c.expr())));
            if (c.respect_or_ignore() != null) {
                paramsOpts.add(new ConstExpression(c.respect_or_ignore()));
            }
        }
        if (ctx.NTH_VALUE() != null) {
            if (ctx.first_or_last() != null) {
                paramsOpts.add(new ConstExpression(ctx.first_or_last()));
            }
            if (ctx.respect_or_ignore() != null) {
                paramsOpts.add(new ConstExpression(ctx.respect_or_ignore()));
            }
        }
        if (ctx.GROUP_CONCAT() != null || ctx.LISTAGG() != null) {
            OrderBy orderBy = null;
            if (ctx.order_by() != null) {
                StatementFactory<OrderBy> order = new MySQLOrderByFactory(ctx.order_by());
                orderBy = order.generate();
                paramsOpts.add(orderBy);
            }
            String separator = null;
            if (ctx.SEPARATOR() != null) {
                separator = ctx.STRING_VALUE().getText();
                paramsOpts.add(new ConstExpression(ctx.STRING_VALUE()));
            }
            if (ctx.GROUP_CONCAT() != null) {
                GroupConcat groupConcat = new GroupConcat(ctx, params);
                groupConcat.setParamsFlag(paramsFlag);
                groupConcat.setSeparator(separator);
                groupConcat.setOrderBy(orderBy);
                groupConcat.setWindow(window);
                return groupConcat;
            }
        }
        WindowFunction fCall = new WindowFunction(ctx, funcName, params);
        fCall.setParamsFlag(paramsFlag);
        fCall.setWindow(window);
        paramsOpts.forEach(fCall::addParamsOption);
        return fCall;
    }

    @Override
    public Expression visitString_val_list(String_val_listContext c) {
        List<Expression> exprs = c.STRING_VALUE().stream().map(ConstExpression::new).collect(Collectors.toList());
        return new CollectionExpression(c, exprs);
    }

    @Override
    public Expression visitIn_expr(In_exprContext ctx) {
        if (ctx.select_with_parens() != null) {
            return new MySQLSelectBodyFactory(ctx.select_with_parens()).generate();
        }
        return visit(ctx.expr_list());
    }

    @Override
    public Expression visitExpr_list(Expr_listContext ctx) {
        List<Expression> expressions = ctx.expr().stream().map(this::visit).collect(Collectors.toList());
        return new CollectionExpression(ctx, expressions);
    }

    @Override
    public Expression visitSelect_no_parens(Select_no_parensContext ctx) {
        return new MySQLSelectBodyFactory(ctx).generate();
    }

    @Override
    public Expression visitBit_expr(Bit_exprContext ctx) {
        if (ctx.simple_expr() != null) {
            return visit(ctx.simple_expr());
        }
        Expression left = visit(ctx.bit_expr(0));
        Expression right = null;
        Operator operator = null;
        if (ctx.And() != null) {
            operator = Operator.AND;
        } else if (ctx.Caret() != null) {
            operator = Operator.NOT;
        } else if (ctx.DIV() != null || ctx.Div() != null) {
            operator = Operator.DIV;
        } else if (ctx.MOD() != null || ctx.Mod() != null) {
            operator = Operator.MOD;
        } else if (ctx.Minus() != null) {
            operator = Operator.SUB;
        } else if (ctx.Or() != null) {
            operator = Operator.OR;
        } else if (ctx.Plus() != null) {
            operator = Operator.ADD;
        } else if (ctx.SHIFT_LEFT() != null) {
            operator = Operator.SHIFT_LEFT;
        } else if (ctx.SHIFT_RIGHT() != null) {
            operator = Operator.SHIFT_RIGHT;
        } else if (ctx.Star() != null) {
            operator = Operator.MUL;
        }
        if (operator == null) {
            throw new IllegalStateException("Missing operator");
        }
        if (ctx.bit_expr().size() == 2) {
            right = visit(ctx.bit_expr(1));
        } else if (ctx.expr() != null) {
            right = new IntervalExpression(ctx, visit(ctx.expr()), ctx.date_unit().getText());
        }
        if (right == null) {
            throw new IllegalStateException("Missing expression");
        }
        return new CompoundExpression(ctx, left, right, operator);
    }

    private String getParamFlags(Simple_func_exprContext ctx) {
        if (ctx.ALL() != null) {
            return ctx.ALL().getText();
        } else if (ctx.DISTINCT() != null) {
            return ctx.DISTINCT().getText();
        } else if (ctx.UNIQUE() != null) {
            return ctx.UNIQUE().getText();
        }
        return null;
    }

    private ExpressionParam wrap(ParserRuleContext expr, String alias) {
        ExpressionParam param = new ExpressionParam(visit(expr));
        param.setAlias(alias);
        return param;
    }

    private String getParamFlags(Complex_func_exprContext ctx) {
        if (ctx.DISTINCT() != null) {
            return ctx.DISTINCT().getText();
        } else if (ctx.UNIQUE() != null) {
            return ctx.UNIQUE().getText();
        }
        return null;
    }

}
