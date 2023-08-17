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
package com.oceanbase.tools.sqlparser.adapter.oracle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Access_func_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Access_func_expr_countContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Aggregate_functionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Bit_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Bool_priContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Bool_pri_in_pl_funcContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Case_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Character_functionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Collection_predicate_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Conversion_functionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Cur_timestamp_funcContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.ExprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Func_access_refContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Func_paramContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Func_param_with_assignContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Function_nameContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.In_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Obj_access_refContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Parameterized_trimContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.PredicateContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Simple_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Single_row_functionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Special_func_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Table_element_access_listContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Table_indexContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Unary_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Updating_paramsContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Win_fun_first_last_paramsContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Win_fun_lead_lag_paramsContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Window_functionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.Operator;
import com.oceanbase.tools.sqlparser.statement.Statement;
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

import lombok.NonNull;

/**
 * {@link OracleExpressionFactory}
 *
 * @author yh263208
 * @date 2022-11-25 17:21
 * @see StatementFactory
 * @since ODC_release_4.1.0
 */
public class OracleExpressionFactory extends OBParserBaseVisitor<Expression> implements StatementFactory<Expression> {

    private final ParserRuleContext parserRuleContext;

    public OracleExpressionFactory(@NonNull ExprContext context) {
        this.parserRuleContext = context;
    }

    public OracleExpressionFactory() {
        this.parserRuleContext = null;
    }

    public OracleExpressionFactory(@NonNull Bit_exprContext context) {
        this.parserRuleContext = context;
    }

    public OracleExpressionFactory(@NonNull Simple_exprContext context) {
        this.parserRuleContext = context;
    }

    public OracleExpressionFactory(@NonNull Obj_access_refContext context) {
        this.parserRuleContext = context;
    }

    @Override
    public Expression generate() {
        return this.parserRuleContext == null ? null : visit(this.parserRuleContext);
    }

    @Override
    public Expression visitExpr(ExprContext ctx) {
        if (ctx.bool_pri() != null) {
            return visit(ctx.bool_pri());
        }
        if (ctx.bit_expr() != null) {
            Expression right = visit(ctx.bit_expr());
            if (ctx.USER_VARIABLE() == null) {
                return right;
            }
            return new CompoundExpression(ctx, new ConstExpression(ctx.USER_VARIABLE()), right, Operator.SET_VAR);
        }
        if (ctx.NOT() != null) {
            return new CompoundExpression(ctx, visit(ctx.expr(0)), null, Operator.NOT);
        }
        if (ctx.AND() != null || ctx.OR() != null) {
            Operator operator = ctx.AND() == null ? Operator.OR : Operator.AND;
            return new CompoundExpression(ctx, visit(ctx.expr(0)), visit(ctx.expr(1)), operator);
        }
        return visit(ctx.expr(0));
    }

    @Override
    public Expression visitBit_expr(Bit_exprContext ctx) {
        if (ctx.BOOL_VALUE() != null) {
            return new BoolValue(ctx.BOOL_VALUE());
        } else if (ctx.unary_expr() != null) {
            return visit(ctx.unary_expr());
        }
        Operator operator = null;
        if (ctx.Plus() != null) {
            operator = Operator.ADD;
        } else if (ctx.Minus() != null) {
            operator = Operator.SUB;
        } else if (ctx.Star() != null) {
            operator = Operator.MUL;
        } else if (ctx.Div() != null) {
            operator = Operator.DIV;
        } else if (ctx.CNNOP() != null) {
            operator = Operator.CNNOP;
        } else if (ctx.AT() != null) {
            if (ctx.TIME() != null) {
                operator = Operator.AT_TIME_ZONE;
            } else if (ctx.LOCAL() != null) {
                operator = Operator.AT_LOCAL;
            }
        } else if (ctx.MULTISET_OP() != null) {
            operator = Operator.MULTISET_OP;
            if (ctx.ALL() != null) {
                operator = Operator.MULTISET_OP_ALL;
            } else if (ctx.DISTINCT() != null) {
                operator = Operator.MULTISET_OP_DISTINCT;
            }
        } else if (ctx.POW_PL() != null) {
            operator = Operator.POW_PL;
        } else if (ctx.MOD() != null) {
            operator = Operator.MOD;
        }
        if (operator == null) {
            throw new IllegalStateException("Missing operator");
        }
        Expression left = visit(ctx.bit_expr(0));
        Expression right = ctx.bit_expr(1) == null ? null : visit(ctx.bit_expr(1));
        return new CompoundExpression(ctx, left, right, operator);
    }

    @Override
    public Expression visitUnary_expr(Unary_exprContext ctx) {
        Operator operator = null;
        if (ctx.Minus() != null) {
            operator = Operator.SUB;
        } else if (ctx.Plus() != null) {
            operator = Operator.ADD;
        }
        Expression first = visit(ctx.simple_expr());
        if (operator == null) {
            return first;
        }
        return new CompoundExpression(ctx, first, null, operator);
    }

    @Override
    public Expression visitSimple_expr(Simple_exprContext ctx) {
        if (ctx.simple_expr() != null) {
            return visit(ctx.simple_expr());
        } else if (ctx.obj_access_ref() != null) {
            return visit(ctx.obj_access_ref());
        } else if (ctx.expr_const() != null) {
            return new ConstExpression(ctx.expr_const());
        } else if (ctx.select_with_parens() != null) {
            OracleSelectBodyFactory selectFactory = new OracleSelectBodyFactory(ctx.select_with_parens());
            return selectFactory.generate();
        } else if (ctx.select_stmt() != null) {
            OracleSelectFactory selectFactory = new OracleSelectFactory(ctx.select_stmt());
            FunctionParam p = new ExpressionParam(selectFactory.generate());
            return new FunctionCall(ctx, ctx.CURSOR().getText(), Collections.singletonList(p));
        } else if (ctx.bit_expr() != null) {
            if (ctx.expr_list() != null) {
                CollectionExpression exprs = new CollectionExpression(ctx);
                for (Bit_exprContext context : ctx.expr_list().bit_expr()) {
                    exprs.addExpression(visit(context));
                }
                exprs.addExpression(visit(ctx.bit_expr()));
                return exprs;
            }
            if (ctx.SET() != null) {
                CollectionExpression exprs = new CollectionExpression(ctx);
                exprs.addExpression(visit(ctx.bit_expr()));
                return exprs;
            }
            return visit(ctx.bit_expr());
        } else if (ctx.USER_VARIABLE() != null) {
            return new ConstExpression(ctx.USER_VARIABLE());
        } else if (ctx.unary_expr() != null) {
            Operator operator = ctx.PRIOR() == null ? Operator.CONNECT_BY_ROOT : Operator.PRIOR;
            return new CompoundExpression(ctx, visit(ctx.unary_expr()), null, operator);
        } else if (ctx.MATCH() != null) {
            List<FunctionParam> params = ctx.column_list().column_definition_ref().stream().map(c -> {
                StatementFactory<ColumnReference> factory = new OracleColumnRefFactory(c);
                return new ExpressionParam(factory.generate());
            }).collect(Collectors.toList());
            TextSearchMode searchMode = null;
            if (ctx.NATURAL() != null) {
                searchMode = TextSearchMode.NATURAL_LANGUAGE_MODE;
            } else if (ctx.BOOLEAN() != null) {
                searchMode = TextSearchMode.BOOLEAN_MODE;
            }
            FullTextSearch f = new FullTextSearch(ctx, params, ctx.STRING_VALUE().getText());
            f.setSearchMode(searchMode);
            return f;
        } else if (ctx.sql_function() != null) {
            return visit(ctx.sql_function());
        } else if (ctx.window_function() != null) {
            return visit(ctx.window_function());
        } else if (ctx.case_expr() != null) {
            return visit(ctx.case_expr());
        }
        return new DefaultExpression(ctx);
    }

    @Override
    public Expression visitCase_expr(Case_exprContext ctx) {
        List<WhenClause> whenClauses;
        if (ctx.bool_when_clause_list() != null) {
            whenClauses = ctx.bool_when_clause_list().bool_when_clause().stream().map(
                    e -> new WhenClause(e, visit(e.expr()), visit(e.bit_expr()))).collect(Collectors.toList());
        } else {
            whenClauses = ctx.simple_when_clause_list().simple_when_clause().stream().map(
                    e -> new WhenClause(e, visit(e.bit_expr(0)), visit(e.bit_expr(1)))).collect(Collectors.toList());
        }
        CaseWhen caseWhen = new CaseWhen(ctx, whenClauses);
        if (ctx.bit_expr() != null) {
            caseWhen.setCaseValue(visit(ctx.bit_expr()));
        }
        if (ctx.case_default().bit_expr() != null) {
            caseWhen.setCaseDefault(visit(ctx.case_default().bit_expr()));
        }
        return caseWhen;
    }

    @Override
    public Expression visitObj_access_ref(Obj_access_refContext ctx) {
        if (ctx.column_ref() != null) {
            return visitColumnRef(ctx);
        } else if (ctx.access_func_expr() != null) {
            return visitAccessFunctionExpr(ctx);
        }
        return new DefaultExpression(ctx);
    }

    @Override
    public Expression visitBool_pri_in_pl_func(Bool_pri_in_pl_funcContext ctx) {
        if (ctx.bool_pri() != null) {
            return visitBool_pri(ctx.bool_pri());
        }
        List<Bool_pri_in_pl_funcContext> contexts = ctx.bool_pri_in_pl_func();
        if (contexts.size() == 1) {
            Expression left = visit(contexts.get(0));
            return ctx.NOT() == null ? left : new CompoundExpression(ctx, left, null, Operator.NOT);
        }
        Operator operator = ctx.OR() == null ? Operator.AND : Operator.OR;
        return new CompoundExpression(ctx, visit(contexts.get(0)), visit(contexts.get(1)), operator);
    }

    @Override
    public Expression visitBool_pri(Bool_priContext boolPri) {
        Bit_exprContext left = boolPri.bit_expr(0);
        Bit_exprContext right = boolPri.bit_expr(1);
        Operator operator = null;
        if (left != null && right == null) {
            // is null or is not null 表达式
            operator = Operator.EQ;
            if (boolPri.not() != null) {
                operator = Operator.NE;
            }
            Expression rightExpr;
            if (boolPri.NULLX() == null) {
                rightExpr = new NullExpression(boolPri.is_nan_inf_value());
            } else {
                rightExpr = new NullExpression(boolPri.NULLX());
            }
            return new CompoundExpression(boolPri, visit(left), rightExpr, operator);
        } else if (left != null) {
            if (boolPri.Caret() != null || boolPri.Not() != null) {
                if (boolPri.COMP_EQ() == null) {
                    throw new IllegalStateException("Missing operator");
                }
                operator = Operator.NE;
            } else if ((boolPri.COMP_LT() != null && boolPri.COMP_GT() != null) || boolPri.COMP_NE() != null) {
                operator = Operator.NE;
            } else if ((boolPri.COMP_LT() != null && boolPri.COMP_EQ() != null) || boolPri.COMP_LE() != null) {
                operator = Operator.LE;
            } else if ((boolPri.COMP_GT() != null && boolPri.COMP_EQ() != null) || boolPri.COMP_GE() != null) {
                operator = Operator.GE;
            } else if (boolPri.COMP_EQ() != null) {
                operator = Operator.EQ;
            } else if (boolPri.COMP_LT() != null) {
                operator = Operator.LT;
            } else if (boolPri.COMP_GT() != null) {
                operator = Operator.GT;
            } else if (boolPri.COMP_NE_PL() != null) {
                operator = Operator.NE_PL;
            }
            if (operator == null) {
                throw new IllegalStateException("Missing operator");
            }
            return new CompoundExpression(boolPri, visit(left), visit(right), operator);
        }
        // left == null && right == null
        PredicateContext predicate = boolPri.predicate();
        if (predicate == null) {
            throw new IllegalStateException("Missing predicate");
        }
        return visit(predicate);
    }

    @Override
    public Expression visitPredicate(PredicateContext predicate) {
        if (predicate.bool_pri() != null) {
            List<FunctionParam> params = new ArrayList<>();
            params.add(new ExpressionParam(visitBool_pri(predicate.bool_pri())));
            return new FunctionCall(predicate, predicate.LNNVL().getText(), params);
        }
        Operator operator = null;
        List<Bit_exprContext> bitExprs = predicate.bit_expr();
        if (CollectionUtils.isNotEmpty(bitExprs)) {
            In_exprContext inExpr = predicate.in_expr();
            if (bitExprs.size() == 1 && inExpr != null) {
                operator = Operator.IN;
                if (predicate.not() != null) {
                    operator = Operator.NOT_IN;
                }
                // in or not in (xxx,xxx,...)
                return new CompoundExpression(predicate, visit(bitExprs.get(0)),
                        visit(inExpr.bit_expr()), operator);
            }
            if (bitExprs.size() == 3 && predicate.ESCAPE() == null) {
                operator = Operator.BETWEEN;
                if (predicate.not() != null) {
                    operator = Operator.NOT_BETWEEN;
                }
                Expression rightExpr = new CompoundExpression(predicate,
                        visit(predicate.bit_expr(1)), visit(predicate.bit_expr(2)), Operator.AND);
                return new CompoundExpression(predicate,
                        visit(predicate.bit_expr(0)), rightExpr, operator);
            }
            operator = Operator.LIKE;
            if (predicate.not() != null) {
                operator = Operator.NOT_LIKE;
            }
            Expression rightExpr = visit(predicate.bit_expr(1));
            if (predicate.ESCAPE() != null) {
                rightExpr = new CompoundExpression(predicate, rightExpr,
                        visit(predicate.bit_expr(2)), Operator.ESCAPE);
            }
            return new CompoundExpression(predicate,
                    visit(predicate.bit_expr(0)), rightExpr, operator);
        }
        if (predicate.collection_predicate_expr() != null) {
            Collection_predicate_exprContext collectionPredicate = predicate.collection_predicate_expr();
            Expression left = visit(collectionPredicate.bit_expr(0));
            Expression right = collectionPredicate.bit_expr(1) == null ? null : visit(collectionPredicate.bit_expr(1));
            if (collectionPredicate.MEMBER() != null) {
                if (collectionPredicate.NOT() == null) {
                    operator = Operator.MEMBER_OF;
                } else {
                    operator = Operator.NOT_MEMBER_OF;
                }
            } else if (collectionPredicate.SUBMULTISET() != null) {
                if (collectionPredicate.NOT() == null) {
                    operator = Operator.SUBMULTISET_OF;
                } else {
                    operator = Operator.NOT_SUBMULTISET_OF;
                }
            } else if (collectionPredicate.SET() != null) {
                if (collectionPredicate.NOT() == null) {
                    operator = Operator.IS_A_SET;
                } else {
                    operator = Operator.IS_NOT_A_SET;
                }
            } else if (collectionPredicate.EMPTY() != null) {
                if (collectionPredicate.NOT() == null) {
                    operator = Operator.IS_EMPTY;
                } else {
                    operator = Operator.IS_NOT_EMPTY;
                }
            }
            if (operator == null) {
                throw new IllegalStateException("Missing operator");
            }
            return new CompoundExpression(collectionPredicate, left, right, operator);
        }
        List<FunctionParam> params = new ArrayList<>();
        String funcName;
        if (predicate.REGEXP_LIKE() != null) {
            for (Bit_exprContext context : predicate.substr_params().bit_expr()) {
                params.add(new ExpressionParam(visit(context)));
            }
            funcName = predicate.REGEXP_LIKE().getText();
        } else if (predicate.exists_function_name() != null) {
            funcName = predicate.exists_function_name().EXISTS().getText();
            OracleSelectBodyFactory selectFactory = new OracleSelectBodyFactory(predicate.select_with_parens());
            params.add(new ExpressionParam(selectFactory.generate()));
        } else if (predicate.updating_func() != null) {
            funcName = predicate.updating_func().UPDATING().getText();
            Updating_paramsContext updatingParams = predicate.updating_func().updating_params();
            params.add(new ExpressionParam(new ConstExpression(updatingParams)));
        } else {
            throw new IllegalStateException("Unknown branch");
        }
        return new FunctionCall(predicate, funcName, params);
    }

    private List<Expression> visitTableElementAccessList(Table_element_access_listContext ctx) {
        Table_indexContext tableIndexContext = ctx.table_index();
        if (tableIndexContext == null) {
            throw new IllegalStateException("Missing table index");
        }
        Table_element_access_listContext elts = ctx.table_element_access_list();
        List<Expression> list = elts == null ? new ArrayList<>() : visitTableElementAccessList(elts);
        list.add(new ConstExpression(tableIndexContext));
        return list;
    }

    private Expression visitColumnRef(Obj_access_refContext ctx) {
        String relationName = ctx.column_ref().getText();
        Expression subRef = null;
        if (ctx.obj_access_ref() != null) {
            subRef = visit(ctx.obj_access_ref());
        } else if (ctx.Star() != null) {
            subRef = new RelationReference(ctx.Star(), ctx.Star().getText(), null);
        } else if (ctx.FIRST() != null) {
            subRef = new FunctionCall(ctx, ctx.FIRST().getText(), new ArrayList<>());
        } else if (ctx.LAST() != null) {
            subRef = new FunctionCall(ctx, ctx.LAST().getText(), new ArrayList<>());
        } else if (ctx.COUNT() != null) {
            subRef = new FunctionCall(ctx, ctx.COUNT().getText(), new ArrayList<>());
        }
        return new RelationReference(ctx, relationName, subRef);
    }

    private List<Expression> visitFunctionAccessReference(Func_access_refContext accessRefContext) {
        List<Expression> functionReferences = new ArrayList<>();
        Obj_access_refContext objReference = accessRefContext.obj_access_ref();
        Table_element_access_listContext eltAccessList = accessRefContext.table_element_access_list();
        if (objReference != null) {
            functionReferences.add(visit(objReference));
        } else if (eltAccessList != null) {
            functionReferences = visitTableElementAccessList(eltAccessList);
        }
        return functionReferences;
    }

    @Override
    public Expression visitAccess_func_expr_count(Access_func_expr_countContext ctx) {
        String functionName = ctx.COUNT().getText();
        String paramsFlag = null;
        List<FunctionParam> params = new ArrayList<>();
        Bit_exprContext bitExpr = ctx.bit_expr();
        TerminalNode start = ctx.Star();
        if (bitExpr != null) {
            params.add(new ExpressionParam(visit(bitExpr)));
        } else if (start != null) {
            params.add(new ExpressionParam(new ConstExpression(start)));
        }
        if (ctx.ALL() != null) {
            paramsFlag = ctx.ALL().getText();
        } else if (ctx.DISTINCT() != null) {
            paramsFlag = ctx.DISTINCT().getText();
        } else if (ctx.UNIQUE() != null) {
            paramsFlag = ctx.UNIQUE().getText();
        }
        FunctionCall fCall = new FunctionCall(ctx, functionName, params);
        fCall.setParamsFlag(paramsFlag);
        return fCall;
    }

    private Expression visitAccessFunctionExpr(Obj_access_refContext ctx) {
        FunctionCall tmp = getFunctionCall(ctx.access_func_expr());
        FunctionCall fCall;
        if (ctx.func_access_ref() == null) {
            fCall = new FunctionCall(ctx, tmp.getFunctionName(), tmp.getParamList());
        } else {
            // 如果存在函数对象访问，先把函数对象访问表达式集合解析出来
            List<Expression> funcAccess = visitFunctionAccessReference(ctx.func_access_ref());
            fCall = new FunctionAccess(ctx, tmp.getFunctionName(), tmp.getParamList(), funcAccess);
        }
        fCall.setParamsFlag(tmp.getParamsFlag());
        return fCall;
    }

    public FunctionCall getFunctionCall(Access_func_exprContext context) {
        String functionName = null;
        List<FunctionParam> params = new ArrayList<>();
        String paramsFlag = null;
        Function_nameContext functionNameCxt = context.function_name();
        // 解析函数调用时的参数传入以及函数名
        Access_func_expr_countContext accessCountFunc = context.access_func_expr_count();
        if (accessCountFunc != null) {
            // 目前访问的是 count 函数
            FunctionCall f = (FunctionCall) visit(accessCountFunc);
            functionName = f.getFunctionName();
            params.addAll(f.getParamList());
            paramsFlag = f.getParamsFlag();
        } else if (functionNameCxt != null) {
            // 存在 function name 节点，函数名使用节点的 text 信息填充
            functionName = functionNameCxt.getText();
            if (context.ALL() != null) {
                paramsFlag = context.ALL().getText();
            } else if (context.DISTINCT() != null) {
                paramsFlag = context.DISTINCT().getText();
            } else if (context.UNIQUE() != null) {
                paramsFlag = context.UNIQUE().getText();
            }
        } else if (context.aggregate_function_keyword() != null) {
            functionName = context.aggregate_function_keyword().getText();
        } else if (context.exists_function_name() != null) {
            functionName = context.exists_function_name().getText();
        }
        if (functionName == null) {
            throw new IllegalStateException("Missing function name");
        }
        if (context.func_param_list() != null) {
            params.addAll(context.func_param_list().func_param().stream()
                    .map(this::visitFunctionParam).collect(Collectors.toList()));
        }
        FunctionCall fCall = new FunctionCall(context, functionName, params);
        fCall.setParamsFlag(paramsFlag);
        return fCall;
    }

    private FunctionParam visitFunctionParam(Func_paramContext paramContext) {
        Bit_exprContext bitExpr = paramContext.bit_expr();
        Func_param_with_assignContext paramWithAssign = paramContext.func_param_with_assign();
        if (bitExpr != null) {
            // 表达式参数
            return new ExpressionParam(visit(bitExpr));
        } else if (paramWithAssign != null) {
            // 函数参数带赋值操作
            String varName = paramWithAssign.var_name().getText();
            Bit_exprContext assignExpr = paramWithAssign.bit_expr();
            Expression varValue;
            if (assignExpr != null) {
                varValue = visit(assignExpr);
            } else {
                varValue = visitBool_pri_in_pl_func(paramWithAssign.bool_pri_in_pl_func());
            }
            return new ParamWithAssign(paramWithAssign, varName, varValue);
        }
        return new ExpressionParam(visit(paramContext.bool_pri_in_pl_func()));
    }

    @Override
    public Expression visitSingle_row_function(Single_row_functionContext ctx) {
        String funcName = null;
        Statement paramsOpt = null;
        List<String> paramFlags = new ArrayList<>();
        List<FunctionParam> params = new ArrayList<>();
        if (ctx.numeric_function() != null) {
            funcName = ctx.numeric_function().MOD().getText();
            params.addAll(ctx.numeric_function().bit_expr().stream()
                    .map(e -> new ExpressionParam(visit(e))).collect(Collectors.toList()));
        } else if (ctx.character_function() != null) {
            Character_functionContext characterFunc = ctx.character_function();
            if (characterFunc.TRANSLATE() != null) {
                funcName = characterFunc.TRANSLATE().getText();
            } else if (characterFunc.TRIM() != null) {
                funcName = characterFunc.TRIM().getText();
            } else if (characterFunc.ASCII() != null) {
                funcName = characterFunc.ASCII().getText();
            }
            if (characterFunc.parameterized_trim() != null) {
                Parameterized_trimContext trim = characterFunc.parameterized_trim();
                params.addAll(
                        trim.bit_expr().stream().map(e -> new ExpressionParam(visit(e))).collect(Collectors.toList()));
                for (int i = 0; i < trim.getChildCount(); i++) {
                    ParseTree p = trim.getChild(i);
                    if (p instanceof TerminalNode) {
                        paramFlags.add(p.getText());
                    }
                }
            } else {
                params.addAll(characterFunc.bit_expr().stream().map(e -> new ExpressionParam(visit(e)))
                        .collect(Collectors.toList()));
            }
            if (characterFunc.translate_charset() != null) {
                paramsOpt = new ConstExpression(characterFunc.translate_charset());
            }
        } else if (ctx.extract_function() != null) {
            funcName = ctx.extract_function().EXTRACT().getText();
            params.add(new ExpressionParam(visit(ctx.extract_function().bit_expr())));
            paramsOpt = new ConstExpression(ctx.extract_function().date_unit_for_extract());
        } else if (ctx.conversion_function() != null) {
            Conversion_functionContext fCtx = ctx.conversion_function();
            funcName = fCtx.CAST().getText();
            params.add(new ExpressionParam(visit(fCtx.bit_expr())));
            paramsOpt = new OracleDataTypeFactory(fCtx.cast_data_type()).generate();
        } else if (ctx.hierarchical_function() != null) {
            funcName = ctx.hierarchical_function().SYS_CONNECT_BY_PATH().getText();
            params.addAll(ctx.hierarchical_function().bit_expr().stream().map(e -> new ExpressionParam(visit(e)))
                    .collect(Collectors.toList()));
        } else if (ctx.environment_id_function() != null) {
            funcName = ctx.environment_id_function().getText();
        }
        if (funcName == null) {
            throw new IllegalStateException("Missing function name");
        }
        FunctionCall fCall = new FunctionCall(ctx, funcName, params);
        if (!paramFlags.isEmpty()) {
            fCall.setParamsFlag(String.join(" ", paramFlags));
        }
        if (paramsOpt != null) {
            fCall.addParamsOption(paramsOpt);
        }
        return fCall;
    }

    @Override
    public Expression visitAggregate_function(Aggregate_functionContext ctx) {
        if (ctx.funcName == null) {
            throw new IllegalStateException("Unknown error, missing function name");
        }
        String funcName = ctx.funcName.getText();
        if (ctx.subFuncName != null) {
            funcName += "." + ctx.subFuncName.getText();
        }
        String paramsFlag = null;
        if (ctx.ALL() != null) {
            paramsFlag = ctx.ALL().getText();
        } else if (ctx.DISTINCT() != null) {
            paramsFlag = ctx.DISTINCT().getText();
        } else if (ctx.UNIQUE() != null) {
            paramsFlag = ctx.UNIQUE().getText();
        }
        List<FunctionParam> params = new ArrayList<>();
        if (ctx.expr_list() != null) {
            params.addAll(ctx.expr_list().bit_expr().stream().map(e -> new ExpressionParam(visit(e)))
                    .collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(ctx.bit_expr())) {
            params.addAll(ctx.bit_expr().stream().map(e -> new ExpressionParam(visit(e))).collect(Collectors.toList()));
        }
        List<Statement> paramsOpts = new ArrayList<>();
        if (ctx.first_or_last() != null) {
            paramsOpts.add(new ConstExpression(ctx.first_or_last()));
        }
        if (ctx.order_by() != null) {
            StatementFactory<OrderBy> factory = new OracleOrderByFactory(ctx.order_by());
            paramsOpts.add(factory.generate());
        }
        FunctionCall fCall = new FunctionCall(ctx, funcName, params);
        fCall.setParamsFlag(paramsFlag);
        paramsOpts.forEach(fCall::addParamsOption);
        return fCall;
    }

    @Override
    public Expression visitSpecial_func_expr(Special_func_exprContext ctx) {
        String funcName;
        List<FunctionParam> params = new ArrayList<>();
        if (ctx.cur_timestamp_func() != null) {
            Cur_timestamp_funcContext cur = ctx.cur_timestamp_func();
            if (cur.SYSDATE() != null) {
                funcName = cur.SYSDATE().getText();
            } else {
                params.add(new ExpressionParam(new ConstExpression(cur.INTNUM())));
                if (cur.CURRENT_TIMESTAMP() != null) {
                    funcName = cur.CURRENT_TIMESTAMP().getText();
                } else {
                    funcName =
                            cur.SYSTIMESTAMP() == null ? cur.LOCALTIMESTAMP().getText() : cur.SYSTIMESTAMP().getText();
                }
            }
        } else if (ctx.INSERT() != null) {
            funcName = ctx.INSERT().getText();
            params.addAll(ctx.bit_expr().stream().map(e -> new ExpressionParam(visit(e))).collect(Collectors.toList()));
        } else if (ctx.CALC_PARTITION_ID() != null || ctx.LEFT() != null) {
            funcName = ctx.LEFT() == null ? ctx.CALC_PARTITION_ID().getText() : ctx.LEFT().getText();
            params.addAll(ctx.bit_expr().stream().map(e -> new ExpressionParam(visit(e))).collect(Collectors.toList()));
        } else if (ctx.POSITION() != null) {
            funcName = ctx.POSITION().getText();
            params.add(new ExpressionParam(
                    new CompoundExpression(ctx, visit(ctx.bit_expr(0)), visit(ctx.bit_expr(1)), Operator.IN)));
        } else if (ctx.DEFAULT() != null || ctx.VALUES() != null) {
            funcName = ctx.VALUES() == null ? ctx.DEFAULT().getText() : ctx.VALUES().getText();
            StatementFactory<ColumnReference> factory = new OracleColumnRefFactory(ctx.column_definition_ref());
            params.add(new ExpressionParam(factory.generate()));
        } else {
            params.add(new ExpressionParam(visit(ctx.bit_expr(0))));
            if (ctx.MONTH() != null) {
                funcName = ctx.MONTH().getText();
            } else {
                String first = ctx.DATE() == null ? ctx.ISNULL().getText() : ctx.DATE().getText();
                String second = ctx.TIME() == null ? ctx.YEAR().getText() : ctx.TIME().getText();
                funcName = first + " " + second;
            }
        }
        return new FunctionCall(ctx, funcName, params);
    }

    @Override
    public Expression visitWindow_function(Window_functionContext ctx) {
        StringBuilder builder = new StringBuilder();
        if (ctx.func_name != null) {
            builder.append(ctx.func_name.getText());
            if (ctx.sub_func_name != null) {
                builder.append(".").append(ctx.sub_func_name.getText());
            }
        } else if (ctx.function_name() != null) {
            builder.append(ctx.function_name().getText());
        }
        if (builder.length() == 0) {
            throw new IllegalStateException("Unknown error, missing function name");
        }
        String funcName = builder.toString();
        List<FunctionParam> params = new ArrayList<>();
        List<Statement> paramsOpts = new ArrayList<>();
        if (ctx.Star() != null) {
            params.add(new ExpressionParam(new ConstExpression(ctx.Star())));
        } else if (CollectionUtils.isNotEmpty(ctx.bit_expr())) {
            params.addAll(ctx.bit_expr().stream().map(e -> new ExpressionParam(visit(e))).collect(Collectors.toList()));
        } else if (ctx.expr_list() != null) {
            params.addAll(ctx.expr_list().bit_expr().stream().map(e -> new ExpressionParam(visit(e)))
                    .collect(Collectors.toList()));
        } else if (ctx.win_fun_first_last_params() != null) {
            Win_fun_first_last_paramsContext win = ctx.win_fun_first_last_params();
            params.add(new ExpressionParam(visit(win.bit_expr())));
            if (win.respect_or_ignore() != null) {
                paramsOpts.add(new ConstExpression(win.respect_or_ignore()));
            }
        } else if (ctx.win_fun_lead_lag_params() != null) {
            Win_fun_lead_lag_paramsContext win = ctx.win_fun_lead_lag_params();
            if (win.respect_or_ignore() != null) {
                paramsOpts.add(new ConstExpression(win.respect_or_ignore()));
            }
            params.add(new ExpressionParam(visit(win.bit_expr())));
            params.addAll(win.expr_list().bit_expr().stream().map(e -> new ExpressionParam(visit(e)))
                    .collect(Collectors.toList()));
        } else if (ctx.func_param_list() != null) {
            params.addAll(ctx.func_param_list().func_param().stream()
                    .map(this::visitFunctionParam).collect(Collectors.toList()));
        }
        if (ctx.first_or_last() != null) {
            paramsOpts.add(new ConstExpression(ctx.first_or_last()));
        }
        if (ctx.respect_or_ignore() != null) {
            paramsOpts.add(new ConstExpression(ctx.respect_or_ignore()));
        }
        if (ctx.order_by() != null) {
            StatementFactory<OrderBy> factory = new OracleOrderByFactory(ctx.order_by());
            paramsOpts.add(factory.generate());
        }
        String paramsFlag = null;
        if (ctx.ALL() != null) {
            paramsFlag = ctx.ALL().getText();
        } else if (ctx.DISTINCT() != null) {
            paramsFlag = ctx.DISTINCT().getText();
        } else if (ctx.UNIQUE() != null) {
            paramsFlag = ctx.UNIQUE().getText();
        }
        StatementFactory<WindowSpec> factory = new OracleWindowSpecFactory(ctx.generalized_window_clause());
        WindowFunction fCall = new WindowFunction(ctx, funcName, params);
        fCall.setWindow(factory.generate());
        fCall.setParamsFlag(paramsFlag);
        paramsOpts.forEach(fCall::addParamsOption);
        return fCall;
    }

}
