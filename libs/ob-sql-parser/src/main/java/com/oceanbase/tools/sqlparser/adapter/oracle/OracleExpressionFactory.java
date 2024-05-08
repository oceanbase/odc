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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.adapter.oracle.OracleDataTypeFactory.TextTypeOpt;
import com.oceanbase.tools.sqlparser.oboracle.OBParser;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Access_func_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Access_func_expr_countContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Aggregate_functionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Bit_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Bool_priContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Bool_pri_in_pl_funcContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Case_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Character_functionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Collection_predicate_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Column_nameContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Column_refContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Conversion_functionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Cur_timestamp_funcContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Dblink_func_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Delete_xmlContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Dot_notation_fun_sysContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Dot_notation_pathContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Dot_notation_path_obj_access_refContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Entry_opContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Evalname_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.ExprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Func_access_refContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Func_paramContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Func_param_with_assignContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.In_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Insert_child_xmlContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Is_json_constrainContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Js_agg_on_nullContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Js_agg_returning_type_optContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Json_array_contentContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Json_array_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Json_equal_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Json_equal_optionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Json_exists_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Json_exists_response_typeContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Json_mergepatch_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Json_obj_unique_keyContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Json_object_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Json_query_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Json_query_on_optContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Json_table_column_defContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Json_table_column_def_pathContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Json_table_exists_column_defContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Json_table_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Json_table_nested_column_defContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Json_table_on_responseContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Json_table_ordinality_column_defContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Json_table_query_column_defContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Json_table_value_column_defContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Json_value_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Json_value_on_empty_responseContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Json_value_on_error_responseContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Json_value_on_optContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Json_value_on_responseContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Nstring_length_iContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Obj_access_refContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Obj_access_ref_normalContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Opt_js_value_returning_typeContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Opt_json_existContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Opt_json_exists_on_error_on_emptyContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Opt_json_mergepatchContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Opt_json_object_clauseContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Opt_json_table_on_error_on_emptyContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Opt_response_queryContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Opt_response_query_on_empty_errorContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Opt_xml_table_nsContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Parameterized_trimContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.PredicateContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Regular_entry_objContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Relation_nameContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Scalars_optContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Signed_literalContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Simple_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Single_row_functionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Special_func_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.String_length_iContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Table_element_access_listContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Table_indexContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Unary_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Updating_paramsContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Win_fun_first_last_paramsContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Win_fun_lead_lag_paramsContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Window_functionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Wrapper_optsContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Xml_attributes_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Xml_attributes_value_clauseContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Xml_element_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Xml_extract_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Xml_nsContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Xml_sequence_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Xml_table_columnContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Xml_table_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Xml_table_ordinality_column_defContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Xml_table_query_column_defContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Xml_table_value_column_defContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Xml_tagContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Xmlcast_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Xmlparse_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Xmlserialize_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.Expression.ReferenceOperator;
import com.oceanbase.tools.sqlparser.statement.Operator;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.common.DataType;
import com.oceanbase.tools.sqlparser.statement.common.GeneralDataType;
import com.oceanbase.tools.sqlparser.statement.common.oracle.KeepClause;
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
import com.oceanbase.tools.sqlparser.statement.select.OrderBy;
import com.oceanbase.tools.sqlparser.statement.select.SelectBody;
import com.oceanbase.tools.sqlparser.statement.select.oracle.XmlNamespaces;
import com.oceanbase.tools.sqlparser.statement.select.oracle.XmlNamespaces.XmlNamespace;

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

    public OracleExpressionFactory(@NonNull PredicateContext context) {
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
    public Expression visit(ParseTree parseTree) {
        if (parseTree == null) {
            return null;
        }
        return super.visit(parseTree);
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
            SelectBody selectBody = selectFactory.generate();
            if (ctx.MULTISET() == null) {
                return selectBody;
            }
            return new FunctionCall(ctx, ctx.MULTISET().getText(),
                    Collections.singletonList(new ExpressionParam(selectBody)));
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
                FunctionParam p = new ExpressionParam(visit(ctx.bit_expr()));
                return new FunctionCall(ctx, ctx.SET().getText(), Collections.singletonList(p));
            }
            return visit(ctx.bit_expr());
        } else if (ctx.USER_VARIABLE() != null) {
            if (CollectionUtils.isEmpty(ctx.column_ref())) {
                return new ConstExpression(ctx.USER_VARIABLE());
            }
            RelationReference seq = new RelationReference(
                    ctx.column_ref(ctx.column_ref().size() - 2),
                    ctx.column_ref(ctx.column_ref().size() - 2).getText());
            RelationReference column = new RelationReference(
                    ctx.column_ref(ctx.column_ref().size() - 1),
                    ctx.column_ref(ctx.column_ref().size() - 1).getText());
            column.setUserVariable(ctx.USER_VARIABLE().getText());
            seq.reference(column, ReferenceOperator.DOT);
            if (ctx.column_ref().size() < 3) {
                return seq;
            }
            RelationReference ref = new RelationReference(
                    ctx.column_ref(ctx.column_ref().size() - 3),
                    ctx.column_ref(ctx.column_ref().size() - 3).getText());
            ref.reference(seq, ReferenceOperator.DOT);
            return ref;
        } else if (ctx.PLSQL_VARIABLE() != null) {
            return new ConstExpression(ctx.PLSQL_VARIABLE());
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
        } else if (ctx.dblink_func_expr() != null) {
            return visit(ctx.dblink_func_expr());
        }
        return new DefaultExpression(ctx);
    }

    @Override
    public Expression visitDblink_func_expr(Dblink_func_exprContext ctx) {
        RelationReference lastOne = null;
        RelationReference firstOne = null;
        for (int i = 0; i < ctx.column_ref().size() - 1; i++) {
            Column_refContext current = ctx.column_ref(i);
            if (lastOne == null) {
                lastOne = new RelationReference(current, current.getText());
                firstOne = lastOne;
            } else {
                RelationReference target = new RelationReference(current, current.getText());
                lastOne.reference(target, ReferenceOperator.DOT);
                lastOne = target;
            }
        }
        List<FunctionParam> params = new ArrayList<>();
        if (ctx.func_param_list() != null) {
            params.addAll(ctx.func_param_list().func_param().stream()
                    .map(this::visitFunctionParam).collect(Collectors.toList()));
        }
        String funcName = ctx.column_ref(ctx.column_ref().size() - 1).getText();
        FunctionCall functionCall = new FunctionCall(ctx, funcName, params);
        functionCall.setUserVariable(ctx.USER_VARIABLE().getText());
        if (lastOne == null) {
            return functionCall;
        }
        lastOne.reference(functionCall, ReferenceOperator.DOT);
        return firstOne;
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
        } else if (ctx.QUESTIONMARK() != null) {
            ConstExpression ref = new ConstExpression(ctx.QUESTIONMARK());
            visitFunctionAccessReference(ref, ctx.func_access_ref());
            return ref;
        }
        return visit(ctx.dot_notation_fun_sys());
    }

    @Override
    public Expression visitDot_notation_fun_sys(Dot_notation_fun_sysContext ctx) {
        return new FunctionCall(ctx, ctx.dot_notation_fun().func_name.getText(), Collections.emptyList());
    }

    @Override
    public Expression visitXmlparse_expr(Xmlparse_exprContext ctx) {
        FunctionParam p = new ExpressionParam(visit(ctx.xml_text().bit_expr()));
        if (ctx.WELLFORMED() != null) {
            p.addOption(new ConstExpression(ctx.WELLFORMED()));
        }
        FunctionCall functionCall = new FunctionCall(ctx, ctx.XMLPARSE().getText(), Collections.singletonList(p));
        functionCall.addOption(new ConstExpression(ctx.xml_doc_type()));
        return functionCall;
    }

    @Override
    public Expression visitDelete_xml(Delete_xmlContext ctx) {
        List<FunctionParam> params = ctx.bit_expr().stream()
                .map(c -> new ExpressionParam(visit(c))).collect(Collectors.toList());
        return new FunctionCall(ctx, ctx.DELETEXML().getText(), params);
    }

    @Override
    public Expression visitInsert_child_xml(Insert_child_xmlContext ctx) {
        List<FunctionParam> params = ctx.bit_expr().stream()
                .map(c -> new ExpressionParam(visit(c))).collect(Collectors.toList());
        return new FunctionCall(ctx, ctx.INSERTCHILDXML().getText(), params);
    }

    @Override
    public Expression visitXml_sequence_expr(Xml_sequence_exprContext ctx) {
        return new FunctionCall(ctx, ctx.XMLSEQUENCE().getText(),
                Collections.singletonList(new ExpressionParam(visit(ctx.bit_expr()))));
    }

    @Override
    public Expression visitXml_table_expr(Xml_table_exprContext ctx) {
        List<FunctionParam> params = new ArrayList<>();
        if (ctx.opt_xml_table_ns() != null) {
            params.add(new ExpressionParam(visit(ctx.opt_xml_table_ns())));
        }
        if (ctx.opt_xml_table_path() != null) {
            params.add(new ExpressionParam(new ConstExpression(ctx.opt_xml_table_path().complex_string_literal())));
        }
        FunctionCall functionCall = new FunctionCall(ctx, ctx.XMLTABLE().getText(), params);
        if (ctx.opt_xml_passing_clause() != null) {
            functionCall.addOption(visit(ctx.opt_xml_passing_clause().simple_expr()));
        }
        if (ctx.opt_sequence_by_ref() != null) {
            functionCall.addOption(new ConstExpression(ctx.opt_sequence_by_ref()));
        }
        if (ctx.opt_columns_clause() != null) {
            ctx.opt_columns_clause().xml_table_columns_list().xml_table_column().stream()
                    .map(c -> visitXmlTableColumn(c)).forEach(p -> functionCall.addOption(p));
        }
        return functionCall;
    }

    @Override
    public Expression visitOpt_xml_table_ns(Opt_xml_table_nsContext ctx) {
        List<XmlNamespace> list = ctx.xml_ns_list().xml_ns().stream()
                .map(c -> (XmlNamespace) visit(c)).collect(Collectors.toList());
        return new XmlNamespaces(ctx, list);
    }

    @Override
    public Expression visitXml_ns(Xml_nsContext ctx) {
        XmlNamespace xmlNamespace = new XmlNamespace(ctx, ctx.STRING_VALUE().getText());
        if (ctx.DEFAULT() != null) {
            xmlNamespace.setDefaultValue(true);
        }
        if (ctx.xml_identifier() != null) {
            xmlNamespace.setAlias(ctx.xml_identifier().getText());
        }
        return xmlNamespace;
    }

    @Override
    public Expression visitXmlcast_expr(Xmlcast_exprContext ctx) {
        FunctionParam p = new ExpressionParam(visit(ctx.bit_expr()));
        if (ctx.cast_data_type() != null) {
            p.addOption(new OracleDataTypeFactory(ctx.cast_data_type()).generate());
        }
        return new FunctionCall(ctx, ctx.XMLCAST().getText(), Collections.singletonList(p));
    }

    @Override
    public Expression visitXmlserialize_expr(Xmlserialize_exprContext ctx) {
        FunctionParam p = new ExpressionParam(visit(ctx.bit_expr()));
        if (ctx.cast_data_type() != null) {
            p.addOption(new OracleDataTypeFactory(ctx.cast_data_type()).generate());
        }
        FunctionCall functionCall = new FunctionCall(ctx,
                ctx.XMLSERIALIZE().getText(), Collections.singletonList(p));
        functionCall.addOption(new ConstExpression(ctx.xml_doc_type()));
        if (ctx.STRING_VALUE() != null) {
            functionCall.addOption(new ConstExpression(ctx.ENCODING(), ctx.STRING_VALUE()));
        }
        if (ctx.literal() != null) {
            functionCall.addOption(new ConstExpression(ctx.VERSION(), ctx.literal()));
        }
        if (ctx.INDENT() != null) {
            if (ctx.NO() != null) {
                functionCall.addOption(new ConstExpression(ctx.NO(), ctx.INDENT()));
            } else if (ctx.SIZE() != null) {
                Expression left = new ConstExpression(ctx.INDENT(), ctx.SIZE());
                Expression right = new ConstExpression(ctx.signed_int_num().INTNUM());
                if (ctx.signed_int_num().Minus() != null) {
                    right = new CompoundExpression(ctx.signed_int_num(), right, null, Operator.SUB);
                }
                functionCall.addOption(new CompoundExpression(
                        ctx.signed_int_num(), left, right, Operator.EQ));
            } else {
                functionCall.addOption(new ConstExpression(ctx.INDENT()));
            }
        }
        if (ctx.DEFAULTS() != null) {
            if (ctx.HIDE() != null) {
                functionCall.addOption(new ConstExpression(ctx.HIDE(), ctx.DEFAULTS()));
            } else if (ctx.SHOW() != null) {
                functionCall.addOption(new ConstExpression(ctx.SHOW(), ctx.DEFAULTS()));
            }
        }
        return functionCall;
    }

    @Override
    public Expression visitXml_extract_expr(Xml_extract_exprContext ctx) {
        List<FunctionParam> params = ctx.bit_expr().stream()
                .map(c -> new ExpressionParam(visit(c))).collect(Collectors.toList());
        if (ctx.literal() != null) {
            params.add(new ExpressionParam(new ConstExpression(ctx.literal())));
        }
        return new FunctionCall(ctx, ctx.EXTRACT().getText(), params);
    }

    @Override
    public Expression visitXml_element_expr(Xml_element_exprContext ctx) {
        Xml_tagContext tCtx = ctx.xml_tag();
        List<FunctionParam> params = new ArrayList<>();
        FunctionParam xmlTag;
        List<Statement> functionOpts = new ArrayList<>();
        if (tCtx.element_name().column_name() != null) {
            Column_nameContext cCtx = tCtx.element_name().column_name();
            xmlTag = new ExpressionParam(new ColumnReference(cCtx, null, null, cCtx.getText()));
        } else {
            xmlTag = new ExpressionParam(visit(tCtx.element_name().evalname_expr()));
        }
        if (tCtx.element_name().NAME() != null) {
            xmlTag.addOption(new ConstExpression(tCtx.element_name().NAME()));
        } else if (tCtx.element_name().EVALNAME() != null) {
            xmlTag.addOption(new ConstExpression(tCtx.element_name().EVALNAME()));
        }
        if (tCtx.ENTITYESCAPING() != null) {
            functionOpts.add(new ConstExpression(tCtx.ENTITYESCAPING()));
        } else if (tCtx.NOENTITYESCAPING() != null) {
            functionOpts.add(new ConstExpression(tCtx.NOENTITYESCAPING()));
        }
        params.add(xmlTag);
        if (ctx.xml_attributes_expr() != null) {
            Xml_attributes_exprContext xCtx = ctx.xml_attributes_expr();
            List<FunctionParam> xmlAttrsParams = new ArrayList<>();
            fullFillXmlAttrs(xmlAttrsParams, ctx.xml_attributes_expr().xml_attributes_value_clause());
            FunctionCall xmlAttrs = new FunctionCall(xCtx, xCtx.XMLATTRIBUTES().getText(), xmlAttrsParams);
            if (xCtx.ENTITYESCAPING() != null) {
                xmlAttrs.addOption(new ConstExpression(xCtx.ENTITYESCAPING()));
            } else if (xCtx.NOENTITYESCAPING() != null) {
                xmlAttrs.addOption(new ConstExpression(xCtx.NOENTITYESCAPING()));
            }
            if (xCtx.NOSCHEMACHECK() != null) {
                xmlAttrs.addOption(new ConstExpression(xCtx.NOSCHEMACHECK()));
            } else if (xCtx.SCHEMACHECK() != null) {
                xmlAttrs.addOption(new ConstExpression(xCtx.SCHEMACHECK()));
            }
            params.add(new ExpressionParam(xmlAttrs));
        }
        if (ctx.xml_value_clause() != null) {
            params.addAll(ctx.xml_value_clause().xml_value().stream().map(c -> {
                FunctionParam in = new ExpressionParam(visit(c.bit_expr()));
                if (c.column_label() == null) {
                    return in;
                }
                in.addOption(new RelationReference(c.column_label(), c.column_label().getText()));
                return in;
            }).collect(Collectors.toList()));
        }
        FunctionCall fCall = new FunctionCall(ctx, ctx.XMLELEMENT().getText(), params);
        functionOpts.forEach(fCall::addOption);
        return fCall;
    }

    @Override
    public Expression visitEvalname_expr(Evalname_exprContext ctx) {
        if (ctx.simple_expr() != null) {
            return visit(ctx.simple_expr());
        }
        return new CompoundExpression(ctx, visit(ctx.evalname_expr(0)),
                visit(ctx.evalname_expr(1)), Operator.CNNOP);
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
    public Expression visitIs_json_constrain(Is_json_constrainContext ctx) {
        JsonConstraint constraint = new JsonConstraint(ctx);
        if (ctx.strict_opt() != null) {
            constraint.setStrictMode(ctx.strict_opt().LAX() != null ? StrictMode.LAX : StrictMode.STRICT);
        }
        setScalarsMode(constraint, ctx.scalars_opt());
        if (ctx.unique_keys_opt() != null) {
            constraint.setUniqueMode(ctx.unique_keys_opt().WITH() != null ? UniqueMode.WITH_UNIQUE_KEYS
                    : UniqueMode.WITHOUT_UNIQUE_KEYS);
        }
        return constraint;
    }

    @Override
    public Expression visitJson_object_expr(Json_object_exprContext ctx) {
        List<FunctionParam> params = new ArrayList<>();
        Entry_opContext eCtx = ctx.opt_json_object_content().entry_op();
        if (eCtx != null) {
            if (eCtx.Star() != null) {
                params.add(new ExpressionParam(new ConstExpression(eCtx.Star())));
            } else {
                params = eCtx.entry_set().entry_obj().stream().map(c -> {
                    FunctionParam p = new ExpressionParam(visit(c.regular_entry_obj()));
                    if (c.FORMAT() == null) {
                        return p;
                    }
                    p.addOption(new ConstExpression(c.FORMAT(), c.JSON()));
                    return p;
                }).collect(Collectors.toList());
            }
        }
        FunctionCall fCall = new FunctionCall(ctx, "json_object", params);
        if (ctx.opt_json_object_content().opt_json_object_clause() != null) {
            Opt_json_object_clauseContext oCtx = ctx.opt_json_object_content().opt_json_object_clause();
            if (oCtx.js_on_null() != null) {
                JsonOnOption onOption = new JsonOnOption(oCtx.js_on_null());
                if (oCtx.js_on_null().ABSENT() != null) {
                    onOption.setOnNull(new ConstExpression(oCtx.js_on_null().ABSENT()));
                } else {
                    onOption.setOnNull(new NullExpression(oCtx.js_on_null().NULLX(0)));
                }
                fCall.addOption(onOption);
            }
            if (oCtx.json_obj_returning_type() != null) {
                fCall.addOption(new OracleDataTypeFactory(
                        oCtx.json_obj_returning_type().js_return_type()).generate());
            }
            if (oCtx.STRICT() != null || oCtx.json_obj_unique_key() != null) {
                fCall.addOption(getJsonConstraint(oCtx.STRICT(), oCtx.json_obj_unique_key()));
            }
        } else if (ctx.opt_json_object_content().STRICT() != null) {
            fCall.addOption(getJsonConstraint(ctx.opt_json_object_content().STRICT(),
                    ctx.opt_json_object_content().json_obj_unique_key()));
        } else {
            fCall.addOption(getJsonConstraint(null, ctx.opt_json_object_content().json_obj_unique_key()));
        }
        return fCall;
    }

    private JsonConstraint getJsonConstraint(TerminalNode strict, Json_obj_unique_keyContext ctx) {
        JsonConstraint jc;
        if (strict != null && ctx != null) {
            jc = new JsonConstraint(strict, ctx);
            jc.setStrictMode(StrictMode.STRICT);
            jc.setUniqueMode(UniqueMode.WITH_UNIQUE_KEYS);
        } else if (strict != null) {
            jc = new JsonConstraint(strict);
            jc.setStrictMode(StrictMode.STRICT);
        } else {
            jc = new JsonConstraint(ctx);
            jc.setUniqueMode(UniqueMode.WITH_UNIQUE_KEYS);
        }
        return jc;
    }

    @Override
    public Expression visitRegular_entry_obj(Regular_entry_objContext ctx) {
        if (ctx.JSON_OBJECT_VALUE() != null) {
            String[] kvs = ctx.JSON_OBJECT_VALUE().getText().split(":");
            if (kvs.length != 2) {
                return new ConstExpression(ctx.JSON_OBJECT_VALUE());
            }
            Expression key = new ConstExpression(kvs[0].trim());
            Expression value;
            char firstC = kvs[1].charAt(0);
            if (firstC > '0' && firstC < '9') {
                value = new ConstExpression(kvs[1]);
            } else {
                value = new RelationReference(kvs[1], null);
            }
            return new JsonKeyValue(ctx, key, value);
        }
        if (ctx.VALUE() != null) {
            return new JsonKeyValue(ctx,
                    visit(ctx.json_obj_literal_expr(0).bit_expr()),
                    visit(ctx.json_obj_literal_expr(1).bit_expr()));
        }
        Expression key = visit(ctx.json_obj_literal_expr(0).bit_expr());
        if (ctx.Colon() == null) {
            return key;
        }
        Expression value = visit(ctx.json_obj_literal_expr(1).bit_expr());
        return new JsonKeyValue(ctx, key, value);
    }

    @Override
    public Expression visitJson_query_expr(Json_query_exprContext ctx) {
        FunctionParam p1 = new ExpressionParam(visit(ctx.js_doc_expr().bit_expr()));
        if (ctx.js_doc_expr().JSON() != null) {
            p1.addOption(new ConstExpression(ctx.js_doc_expr().FORMAT(), ctx.js_doc_expr().JSON()));
        }
        FunctionParam p2 = new ExpressionParam(new ConstExpression(ctx.js_literal().literal()));
        FunctionCall fCall = new FunctionCall(ctx, ctx.JSON_QUERY().getText(), Arrays.asList(p1, p2));
        if (ctx.js_query_return_type() != null) {
            fCall.addOption(new OracleDataTypeFactory(ctx.js_query_return_type()).generate());
        }
        if (ctx.TRUNCATE() != null) {
            fCall.addOption(new ConstExpression(ctx.TRUNCATE()));
        }
        if (ctx.PRETTY() != null) {
            fCall.addOption(new ConstExpression(ctx.PRETTY()));
        }
        if (ctx.ASCII() != null) {
            fCall.addOption(new ConstExpression(ctx.ASCII()));
        }
        if (ctx.scalars_opt() != null || ctx.wrapper_opts() != null) {
            JsonConstraint constraint = new JsonConstraint(
                    ctx.scalars_opt() == null ? ctx.wrapper_opts() : ctx.scalars_opt());
            setScalarsMode(constraint, ctx.scalars_opt());
            setWrapperMode(constraint, ctx.wrapper_opts());
            fCall.addOption(constraint);
        }
        fCall.addOption(getJsonOnOption(ctx.json_query_on_opt()));
        return fCall;
    }

    @Override
    public Expression visitJson_mergepatch_expr(Json_mergepatch_exprContext ctx) {
        FunctionParam p1 = new ExpressionParam(visit(ctx.bit_expr(0)));
        FunctionParam p2 = new ExpressionParam(visit(ctx.bit_expr(1)));
        FunctionCall fCall = new FunctionCall(ctx, ctx.JSON_MERGEPATCH().getText(), Arrays.asList(p1, p2));
        if (ctx.js_mp_return_clause() != null) {
            fCall.addOption(new OracleDataTypeFactory(ctx.js_mp_return_clause().js_return_type()).generate());
        }
        Opt_json_mergepatchContext oCtx = ctx.opt_json_mergepatch();
        if (oCtx.TRUNCATE() != null) {
            fCall.addOption(new ConstExpression(oCtx.TRUNCATE()));
        }
        if (oCtx.PRETTY() != null) {
            fCall.addOption(new ConstExpression(oCtx.PRETTY()));
        }
        if (oCtx.ASCII() != null) {
            fCall.addOption(new ConstExpression(oCtx.ASCII()));
        }
        if (ctx.json_mergepatch_on_error() != null) {
            JsonOnOption jsonOnOption = new JsonOnOption(ctx.json_mergepatch_on_error());
            if (ctx.json_mergepatch_on_error().NULLX() != null) {
                jsonOnOption.setOnError(new NullExpression(ctx.json_mergepatch_on_error().NULLX()));
            } else {
                jsonOnOption.setOnError(new ConstExpression(ctx.json_mergepatch_on_error().ERROR_P(0)));
            }
            fCall.addOption(jsonOnOption);
        }
        return fCall;
    }

    @Override
    public Expression visitJson_array_expr(Json_array_exprContext ctx) {
        if (ctx.json_array_content() == null) {
            return new FunctionCall(ctx, "json_array", Collections.emptyList());
        }
        Json_array_contentContext jCtx = ctx.json_array_content();
        List<FunctionParam> params = jCtx.js_array_eles().js_array_ele().stream().map(c -> {
            FunctionParam p = new ExpressionParam(visit(c.bit_expr()));
            if (c.JSON() == null) {
                return p;
            }
            p.addOption(new ConstExpression(c.FORMAT(), c.JSON()));
            return p;
        }).collect(Collectors.toList());
        FunctionCall fCall = new FunctionCall(ctx, "json_array", params);
        if (jCtx.json_array_on_null() != null) {
            JsonOnOption jsonOnOption = new JsonOnOption(jCtx.json_array_on_null());
            if (jCtx.json_array_on_null().ABSENT() != null) {
                jsonOnOption.setOnNull(new ConstExpression(jCtx.json_array_on_null().ABSENT()));
            } else {
                jsonOnOption.setOnNull(new NullExpression(jCtx.json_array_on_null().NULLX(0)));
            }
            fCall.addOption(jsonOnOption);
        }
        if (jCtx.js_array_return_clause() != null) {
            fCall.addOption(new OracleDataTypeFactory(jCtx.js_array_return_clause().js_return_type()).generate());
        }
        if (jCtx.STRICT() != null) {
            JsonConstraint jsonConstraint = new JsonConstraint(jCtx.STRICT());
            jsonConstraint.setStrictMode(StrictMode.STRICT);
            fCall.addOption(jsonConstraint);
        }
        return fCall;
    }

    @Override
    public Expression visitJson_value_expr(Json_value_exprContext ctx) {
        FunctionParam p1 = new ExpressionParam(visit(ctx.js_doc_expr().bit_expr()));
        if (ctx.js_doc_expr().JSON() != null) {
            p1.addOption(new ConstExpression(ctx.js_doc_expr().FORMAT(), ctx.js_doc_expr().JSON()));
        }
        FunctionParam p2 = new ExpressionParam(new ConstExpression(ctx.js_literal().literal()));
        FunctionCall fCall = new FunctionCall(ctx, ctx.JSON_VALUE().getText(), Arrays.asList(p1, p2));
        DataType dataType = getDataType(ctx.opt_js_value_returning_type());
        if (dataType != null) {
            fCall.addOption(dataType);
        }
        if (ctx.TRUNCATE() != null) {
            fCall.addOption(new ConstExpression(ctx.TRUNCATE()));
        }
        if (ctx.ASCII() != null) {
            fCall.addOption(new ConstExpression(ctx.ASCII()));
        }
        fCall.addOption(getJsonOnOption(ctx.json_value_on_opt()));
        return fCall;
    }

    @Override
    public Expression visitJson_exists_expr(Json_exists_exprContext ctx) {
        FunctionParam p1 = new ExpressionParam(visit(ctx.js_doc_expr().bit_expr()));
        if (ctx.js_doc_expr().JSON() != null) {
            p1.addOption(new ConstExpression(ctx.js_doc_expr().FORMAT(), ctx.js_doc_expr().JSON()));
        }
        FunctionParam p2 = new ExpressionParam(new ConstExpression(ctx.literal()));
        FunctionCall fCall = new FunctionCall(ctx, ctx.JSON_EXISTS().getText(), Arrays.asList(p1, p2));
        setJsonExistOpt(fCall, ctx.opt_json_exist());
        return fCall;
    }

    @Override
    public Expression visitJson_table_expr(Json_table_exprContext ctx) {
        List<FunctionParam> params = new ArrayList<>();
        FunctionParam p1 = new ExpressionParam(visit(ctx.js_doc_expr().bit_expr()));
        if (ctx.js_doc_expr().FORMAT() != null) {
            p1.addOption(new ConstExpression(ctx.js_doc_expr().FORMAT(), ctx.js_doc_expr().JSON()));
        }
        params.add(p1);
        if (ctx.literal() != null) {
            params.add(new ExpressionParam(new ConstExpression(ctx.literal())));
        }
        FunctionCall fCall = new FunctionCall(ctx, ctx.JSON_TABLE().getText(), params);
        fCall.addOption(getJsonOnOption(ctx.opt_json_table_on_error_on_empty()));
        ctx.json_table_columns_def_opt().json_table_columns_def().json_table_column_def()
                .forEach(c -> fCall.addOption(visitJsonTableColumnDef(c)));
        return fCall;
    }

    @Override
    public Expression visitJson_equal_expr(Json_equal_exprContext ctx) {
        List<FunctionParam> params = new ArrayList<>();
        if (ctx.func_param_list() != null) {
            params.addAll(ctx.func_param_list().func_param().stream()
                    .map(this::visitFunctionParam).collect(Collectors.toList()));
        }
        FunctionCall fCall = new FunctionCall(ctx, ctx.getChild(0).getText(), params);
        if (ctx.json_equal_option() != null) {
            fCall.addOption(getJsonOnOption(ctx.json_equal_option()));
        }
        return fCall;
    }

    @Override
    public Expression visitJson_exists_response_type(Json_exists_response_typeContext ctx) {
        if (ctx.BOOL_VALUE() != null) {
            return new BoolValue(ctx.BOOL_VALUE());
        }
        return new ConstExpression(ctx.ERROR_P());
    }

    @Override
    public Expression visitBool_pri(Bool_priContext boolPri) {
        Bit_exprContext left = boolPri.bit_expr(0);
        Bit_exprContext right = boolPri.bit_expr(1);
        Operator operator = null;
        if (left != null && right == null) {
            // is null or is not null 表达式
            operator = Operator.EQ;
            if (boolPri.not() != null || boolPri.NOT() != null) {
                operator = Operator.NE;
            }
            Expression rightExpr;
            if (boolPri.is_nan_inf_value() != null) {
                rightExpr = new NullExpression(boolPri.is_nan_inf_value());
            } else if (boolPri.NULLX() != null) {
                rightExpr = new NullExpression(boolPri.NULLX());
            } else {
                rightExpr = visit(boolPri.is_json_constrain());
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
            } else if (boolPri.COMP_NSEQ() != null) {
                operator = Operator.NSEQ;
            }
            if (operator == null) {
                throw new IllegalStateException("Missing operator");
            }
            return new CompoundExpression(boolPri, visit(left), visit(right), operator);
        }
        // left == null && right == null
        if (boolPri.predicate() != null) {
            return visit(boolPri.predicate());
        }
        throw new IllegalStateException("Illegal branch");
    }

    @Override
    public Expression visitPredicate(PredicateContext predicate) {
        if (predicate.bool_pri() != null) {
            return new FunctionCall(predicate, predicate.LNNVL().getText(),
                    Collections.singletonList(new ExpressionParam(visitBool_pri(predicate.bool_pri()))));
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

    private Expression visitTableElementAccessList(Expression fCall, Table_element_access_listContext ctx) {
        Table_indexContext tableIndexContext = ctx.table_index();
        if (tableIndexContext == null) {
            throw new IllegalStateException("Missing table index");
        }
        Table_element_access_listContext elts = ctx.table_element_access_list();
        Expression expr = fCall;
        if (elts != null) {
            expr = visitTableElementAccessList(fCall, elts);
        }
        return expr.reference(new OracleExpressionFactory(tableIndexContext.bit_expr()).generate(),
                ReferenceOperator.PAREN);
    }

    private Expression visitColumnRef(Obj_access_refContext ctx) {
        Expression subRef = null;
        ReferenceOperator operator = ReferenceOperator.DOT;
        if (ctx.obj_access_ref() != null) {
            subRef = visit(ctx.obj_access_ref());
        } else if (ctx.Star() != null) {
            subRef = new RelationReference(ctx.Star(), ctx.Star().getText());
        } else if (ctx.FIRST() != null) {
            subRef = new FunctionCall(ctx, ctx.FIRST().getText(), new ArrayList<>());
        } else if (ctx.LAST() != null) {
            subRef = new FunctionCall(ctx, ctx.LAST().getText(), new ArrayList<>());
        } else if (ctx.COUNT() != null) {
            subRef = new FunctionCall(ctx, ctx.COUNT().getText(), new ArrayList<>());
        } else if (ctx.dot_notation_path() != null) {
            subRef = visit(ctx.dot_notation_path());
            operator = ReferenceOperator.BRACKET;
        }
        Expression colRef = new RelationReference(ctx.column_ref(), ctx.column_ref().getText());
        if (subRef != null) {
            colRef.reference(subRef, operator);
        }
        return colRef;
    }

    @Override
    public Expression visitPath_param(OBParser.Path_paramContext ctx) {
        Expression left = new ConstExpression(ctx.INTNUM());
        if (ctx.path_param() == null) {
            return left;
        }
        return new CompoundExpression(ctx, left, visitPath_param(ctx.path_param()), Operator.TO);
    }

    @Override
    public Expression visitDot_notation_path(Dot_notation_pathContext ctx) {
        CollectionExpression exprs = new CollectionExpression(ctx.path_param_array());
        if (ctx.path_param_array().Star() != null) {
            exprs.addExpression(new ConstExpression(ctx.path_param_array().Star()));
        }
        if (ctx.path_param_array().path_param_list() != null) {
            ctx.path_param_array().path_param_list().path_param().forEach(c -> exprs.addExpression(visit(c)));
        }
        Dot_notation_path_obj_access_refContext dCtx = ctx.dot_notation_path_obj_access_ref();
        if (dCtx.obj_access_ref() != null) {
            exprs.reference(visit(dCtx.obj_access_ref()), ReferenceOperator.DOT);
        } else if (dCtx.dot_notation_path() != null) {
            exprs.reference(visit(dCtx.dot_notation_path()), ReferenceOperator.BRACKET);
        }
        return exprs;
    }

    private void visitFunctionAccessReference(Expression fCall, Func_access_refContext ctx) {
        if (ctx.table_element_access_list() != null) {
            visitTableElementAccessList(fCall, ctx.table_element_access_list());
        }
        if (ctx.obj_access_ref() != null) {
            Expression last;
            for (last = fCall; last.getReference() != null; last = last.getReference()) {
            }
            last.reference(visit(ctx.obj_access_ref()), ReferenceOperator.DOT);
        }
    }

    @Override
    public Expression visitAccess_func_expr_count(Access_func_expr_countContext ctx) {
        Bit_exprContext bitExpr = ctx.bit_expr();
        TerminalNode start = ctx.Star();
        List<FunctionParam> params = new ArrayList<>();
        if (bitExpr != null) {
            params.add(new ExpressionParam(visit(bitExpr)));
        } else if (start != null) {
            params.add(new ExpressionParam(new ConstExpression(start)));
        }
        FunctionCall fCall = new FunctionCall(ctx, ctx.COUNT().getText(), params);
        fCall.setKeep(getKeepClause(ctx));
        if (ctx.ALL() != null) {
            fCall.addOption(new ConstExpression(ctx.ALL()));
        } else if (ctx.DISTINCT() != null) {
            fCall.addOption(new ConstExpression(ctx.DISTINCT()));
        } else if (ctx.UNIQUE() != null) {
            fCall.addOption(new ConstExpression(ctx.UNIQUE()));
        }
        return fCall;
    }

    private Expression visitAccessFunctionExpr(Obj_access_refContext ctx) {
        FunctionCall fCall = getFunctionCall(ctx.access_func_expr());
        if (ctx.func_access_ref() == null) {
            return fCall;
        }
        visitFunctionAccessReference(fCall, ctx.func_access_ref());
        return fCall;
    }

    public FunctionCall getFunctionCall(Access_func_exprContext ctx) {
        String functionName = null;
        if (ctx.access_func_expr_count() != null) {
            return (FunctionCall) visit(ctx.access_func_expr_count());
        } else if (ctx.function_name() != null) {
            functionName = ctx.function_name().getText();
        } else if (ctx.aggregate_function_keyword() != null) {
            functionName = ctx.aggregate_function_keyword().getText();
        } else if (ctx.exists_function_name() != null) {
            functionName = ctx.exists_function_name().getText();
        } else if (ctx.NEW() != null && ctx.NAME_OB() != null) {
            functionName = ctx.NEW().getText() + " " + ctx.NAME_OB().getText();
        }
        if (functionName == null) {
            throw new IllegalStateException("Missing function name");
        }
        List<FunctionParam> params = new ArrayList<>();
        if (ctx.func_param_list() != null) {
            params.addAll(ctx.func_param_list().func_param().stream()
                    .map(this::visitFunctionParam).collect(Collectors.toList()));
        }
        FunctionCall fCall = new FunctionCall(ctx, functionName, params);
        if (ctx.ALL() != null) {
            fCall.addOption(new ConstExpression(ctx.ALL()));
        } else if (ctx.DISTINCT() != null) {
            fCall.addOption(new ConstExpression(ctx.DISTINCT()));
        } else if (ctx.UNIQUE() != null) {
            fCall.addOption(new ConstExpression(ctx.UNIQUE()));
        }
        setJsonExistOpt(fCall, ctx.opt_json_exist());
        if (ctx.json_equal_option() != null) {
            fCall.addOption(getJsonOnOption(ctx.json_equal_option()));
        }
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
    public Expression visitObj_access_ref_normal(Obj_access_ref_normalContext ctx) {
        Expression expr;
        if (ctx.pl_var_name() != null) {
            expr = new RelationReference(ctx.pl_var_name(), ctx.pl_var_name().getText());
        } else if (ctx.access_func_expr_count() != null) {
            expr = visit(ctx.access_func_expr_count());
        } else {
            List<FunctionParam> params = new ArrayList<>();
            if (ctx.func_param_list() != null) {
                params.addAll(ctx.func_param_list().func_param().stream()
                        .map(this::visitFunctionParam).collect(Collectors.toList()));
            }
            expr = new FunctionCall(ctx, ctx.getChild(0).getText(), params);
        }
        if (ctx.obj_access_ref_normal() != null) {
            expr.reference(visit(ctx.obj_access_ref_normal()), ReferenceOperator.DOT);
        } else if (ctx.table_element_access_list() != null) {
            visitTableElementAccessList(expr, ctx.table_element_access_list());
        }
        return expr;
    }

    @Override
    public Expression visitSingle_row_function(Single_row_functionContext ctx) {
        String funcName = null;
        List<Statement> functionOpts = new ArrayList<>();
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
                FunctionParam param = new ExpressionParam(visit(trim.bit_expr(0)));
                if (trim.bit_expr(1) != null) {
                    param.addOption(visit(trim.bit_expr(1)));
                }
                params.add(param);
                for (int i = 0; i < trim.getChildCount(); i++) {
                    ParseTree p = trim.getChild(i);
                    if (p instanceof TerminalNode) {
                        functionOpts.add(new ConstExpression((TerminalNode) p));
                    } else {
                        break;
                    }
                }
            } else {
                params.addAll(characterFunc.bit_expr().stream().map(e -> new ExpressionParam(visit(e)))
                        .collect(Collectors.toList()));
                if (params.size() > 0) {
                    params.get(params.size() - 1).addOption(new ConstExpression(characterFunc.translate_charset()));
                }
            }
        } else if (ctx.extract_function() != null) {
            funcName = ctx.extract_function().EXTRACT().getText();
            FunctionParam p = new ExpressionParam(new ConstExpression(ctx.extract_function().date_unit_for_extract()));
            p.addOption(visit(ctx.extract_function().bit_expr()));
            params.add(p);
        } else if (ctx.conversion_function() != null) {
            Conversion_functionContext fCtx = ctx.conversion_function();
            if (fCtx.CAST() != null) {
                funcName = fCtx.CAST().getText();
                FunctionParam functionParam = new ExpressionParam(visit(fCtx.bit_expr()));
                functionParam.addOption(new OracleDataTypeFactory(fCtx.cast_data_type()).generate());
                params.add(functionParam);
            } else {
                funcName = fCtx.TREAT().getText();
                FunctionParam functionParam = new ExpressionParam(visit(fCtx.bit_expr()));
                functionParam.addOption(new OracleDataTypeFactory(fCtx.treat_data_type()).generate());
                params.add(functionParam);
            }
        } else if (ctx.hierarchical_function() != null) {
            funcName = ctx.hierarchical_function().SYS_CONNECT_BY_PATH().getText();
            params.addAll(ctx.hierarchical_function().bit_expr().stream().map(e -> new ExpressionParam(visit(e)))
                    .collect(Collectors.toList()));
        } else if (ctx.environment_id_function() != null) {
            funcName = ctx.environment_id_function().getText();
        } else if (ctx.xml_function() != null) {
            Expression fCall = visit(ctx.xml_function());
            if (ctx.obj_access_ref_normal() != null) {
                fCall.reference(visit(ctx.obj_access_ref_normal()), ReferenceOperator.DOT);
            } else if (ctx.table_element_access_list() != null) {
                visitTableElementAccessList(fCall, ctx.table_element_access_list());
            }
            return fCall;
        } else if (ctx.json_function() != null) {
            return visit(ctx.json_function());
        }
        if (funcName == null) {
            throw new IllegalStateException("Missing function name");
        }
        FunctionCall fCall = new FunctionCall(ctx, funcName, params);
        functionOpts.forEach(fCall::addOption);
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
        List<FunctionParam> params = new ArrayList<>();
        if (ctx.JSON_ARRAYAGG() != null || ctx.JSON_OBJECTAGG() != null) {
            if (ctx.VALUE() != null) {
                Expression key = visit(ctx.bit_expr(0));
                Expression value = visit(ctx.bit_expr(1));
                if (ctx.KEY() != null) {
                    params.add(new ExpressionParam(new JsonKeyValue(
                            ctx.KEY(), ctx.bit_expr(1), key, value)));
                } else {
                    params.add(new ExpressionParam(new JsonKeyValue(
                            ctx.bit_expr(0), ctx.bit_expr(1), key, value)));
                }
            } else {
                params.addAll(
                        ctx.bit_expr().stream().map(c -> new ExpressionParam(visit(c))).collect(Collectors.toList()));
            }
        } else {
            if (ctx.expr_list() != null) {
                params.addAll(ctx.expr_list().bit_expr().stream()
                        .map(e -> new ExpressionParam(visit(e))).collect(Collectors.toList()));
            }
            if (CollectionUtils.isNotEmpty(ctx.bit_expr())) {
                params.addAll(
                        ctx.bit_expr().stream().map(e -> new ExpressionParam(visit(e))).collect(Collectors.toList()));
            }
            if (ctx.simple_expr() != null) {
                params.add(new ExpressionParam(visit(ctx.simple_expr())));
            }
        }
        FunctionCall fCall = new FunctionCall(ctx, funcName, params);
        setFunctionOptions(fCall, ctx);
        fCall.setKeep(getKeepClause(ctx));
        fCall.setWithinGroup(getWithinGroup(ctx));
        return fCall;
    }

    @Override
    public Expression visitSigned_literal(Signed_literalContext ctx) {
        return getExpression(ctx);
    }

    public static Expression getExpression(Signed_literalContext ctx) {
        ConstExpression constExpr;
        if (ctx.literal() != null) {
            constExpr = new ConstExpression(ctx.literal());
        } else {
            constExpr = new ConstExpression(ctx.number_literal());
        }
        Operator operator = null;
        if (ctx.Minus() != null) {
            operator = Operator.SUB;
        } else if (ctx.Plus() != null) {
            operator = Operator.ADD;
        }
        return operator == null ? constExpr : new CompoundExpression(ctx, constExpr, null, operator);
    }

    @Override
    public Expression visitSpecial_func_expr(Special_func_exprContext ctx) {
        String funcName;
        List<FunctionParam> params = new ArrayList<>();
        if (ctx.cur_timestamp_func() != null) {
            Cur_timestamp_funcContext cur = ctx.cur_timestamp_func();
            funcName = cur.getChild(0).getText();
            if (cur.INTNUM() != null) {
                params.add(new ExpressionParam(new ConstExpression(cur.INTNUM())));
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
            funcName = ctx.getChild(0).getText();
            params.add(new ExpressionParam(visit(ctx.bit_expr(0))));
        }
        return new FunctionCall(ctx, funcName, params);
    }

    @Override
    public Expression visitJson_value_on_response(Json_value_on_responseContext ctx) {
        if (ctx.NULLX() != null) {
            return new NullExpression(ctx);
        }
        return new ConstExpression(ctx);
    }

    @Override
    public Expression visitOpt_response_query(Opt_response_queryContext ctx) {
        if (ctx.NULLX() != null) {
            return new NullExpression(ctx);
        }
        return new ConstExpression(ctx);
    }

    @Override
    public Expression visitJson_table_column_def_path(Json_table_column_def_pathContext ctx) {
        if (ctx == null) {
            return null;
        }
        if (ctx.literal() != null) {
            return new ConstExpression(ctx.literal());
        }
        ColumnReference cr = new ColumnReference(ctx.column_name(), null, null, ctx.column_name().getText());
        if (ctx.dot_notation_path() != null) {
            cr.reference(visit(ctx.dot_notation_path()), ReferenceOperator.BRACKET);
        }
        return cr;
    }

    @Override
    public Expression visitOpt_response_query_on_empty_error(Opt_response_query_on_empty_errorContext ctx) {
        if (ctx.opt_response_query() != null) {
            return visit(ctx.opt_response_query());
        }
        return new ConstExpression(ctx);
    }

    @Override
    public Expression visitJson_table_on_response(Json_table_on_responseContext ctx) {
        if (ctx.ERROR_P() != null) {
            return new ConstExpression(ctx.ERROR_P());
        } else if (ctx.NULLX() != null) {
            return new NullExpression(ctx.NULLX());
        }
        return visit(ctx.signed_literal());
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
        List<Statement> functionOpts = new ArrayList<>();
        if (ctx.Star() != null) {
            params.add(new ExpressionParam(new ConstExpression(ctx.Star())));
        } else if (CollectionUtils.isNotEmpty(ctx.bit_expr())) {
            params.addAll(ctx.bit_expr().stream().map(e -> new ExpressionParam(visit(e))).collect(Collectors.toList()));
        } else if (ctx.expr_list() != null) {
            params.addAll(ctx.expr_list().bit_expr().stream().map(e -> new ExpressionParam(visit(e)))
                    .collect(Collectors.toList()));
        } else if (ctx.win_fun_first_last_params() != null) {
            Win_fun_first_last_paramsContext win = ctx.win_fun_first_last_params();
            FunctionParam functionParam = new ExpressionParam(visit(win.bit_expr()));
            if (win.respect_or_ignore() != null) {
                functionOpts.add(new ConstExpression(win.respect_or_ignore(), win.NULLS()));
            }
            params.add(functionParam);
        } else if (ctx.win_fun_lead_lag_params() != null) {
            Win_fun_lead_lag_paramsContext win = ctx.win_fun_lead_lag_params();
            if (win.bit_expr() != null) {
                params.add(new ExpressionParam(visit(win.bit_expr())));
            }
            if (win.expr_list() != null) {
                params.addAll(win.expr_list().bit_expr().stream()
                        .map(e -> new ExpressionParam(visit(e))).collect(Collectors.toList()));
            }
            if (win.respect_or_ignore() != null) {
                functionOpts.add(new ConstExpression(win.respect_or_ignore(), win.NULLS()));
            }
        } else if (ctx.func_param_list() != null) {
            params.addAll(ctx.func_param_list().func_param().stream()
                    .map(this::visitFunctionParam).collect(Collectors.toList()));
        }
        if (ctx.NTH_VALUE() != null) {
            if (ctx.FROM() != null && ctx.first_or_last() != null) {
                functionOpts.add(new ConstExpression(ctx.FROM(), ctx.first_or_last()));
            }
            if (ctx.respect_or_ignore() != null) {
                functionOpts.add(new ConstExpression(ctx.respect_or_ignore(), ctx.NULLS()));
            }
        }
        FunctionCall fCall = new FunctionCall(ctx, funcName, params);
        if (ctx.ALL() != null) {
            fCall.addOption(new ConstExpression(ctx.ALL()));
        } else if (ctx.DISTINCT() != null) {
            fCall.addOption(new ConstExpression(ctx.DISTINCT()));
        } else if (ctx.UNIQUE() != null) {
            fCall.addOption(new ConstExpression(ctx.UNIQUE()));
        }
        fCall.setWithinGroup(getWithinGroup(ctx));
        fCall.setKeep(getKeepClause(ctx));
        fCall.setWindow(new OracleWindowSpecFactory(ctx.generalized_window_clause()).generate());
        functionOpts.forEach(fCall::addOption);
        return fCall;
    }

    private KeepClause getKeepClause(Window_functionContext ctx) {
        if (ctx.KEEP() == null || ctx.DENSE_RANK() == null
                || ctx.first_or_last() == null || ctx.order_by() == null) {
            return null;
        }
        return new KeepClause(ctx.DENSE_RANK(), ctx.order_by(),
                ctx.first_or_last().getText(),
                new OracleOrderByFactory(ctx.order_by()).generate());
    }

    private KeepClause getKeepClause(Aggregate_functionContext ctx) {
        if (ctx.KEEP() == null || ctx.DENSE_RANK() == null
                || ctx.first_or_last() == null || ctx.order_by() == null) {
            return null;
        }
        return new KeepClause(ctx.DENSE_RANK(), ctx.order_by(),
                ctx.first_or_last().getText(),
                new OracleOrderByFactory(ctx.order_by()).generate());
    }

    private OrderBy getWithinGroup(Window_functionContext ctx) {
        if (ctx.WITHIN() == null || ctx.GROUP() == null || ctx.order_by() == null) {
            return null;
        }
        return new OracleOrderByFactory(ctx.order_by()).generate();
    }

    private OrderBy getWithinGroup(Aggregate_functionContext ctx) {
        if (ctx.WITHIN() == null || ctx.GROUP() == null || ctx.order_by() == null) {
            return null;
        }
        return new OracleOrderByFactory(ctx.order_by()).generate();
    }

    private KeepClause getKeepClause(Access_func_expr_countContext ctx) {
        if (ctx.KEEP() == null || ctx.DENSE_RANK() == null
                || ctx.first_or_last() == null || ctx.order_by() == null) {
            return null;
        }
        return new KeepClause(ctx.DENSE_RANK(), ctx.order_by(),
                ctx.first_or_last().getText(),
                new OracleOrderByFactory(ctx.order_by()).generate());
    }

    private void fullFillXmlAttrs(List<FunctionParam> params, Xml_attributes_value_clauseContext ctx) {
        Expression expr = visit(ctx.xml_attributes_value().attributes_name_value().bit_expr());
        FunctionParam param = new ExpressionParam(expr);
        if (ctx.xml_attributes_value().bit_expr() != null) {
            param.addOption(visit(ctx.xml_attributes_value().bit_expr()));
        } else if (ctx.xml_attributes_value().relation_name() != null) {
            Relation_nameContext rCtx = ctx.xml_attributes_value().relation_name();
            param.addOption(new RelationReference(rCtx, rCtx.getText()));
        }
        params.add(param);
        if (ctx.xml_attributes_value_clause() == null) {
            return;
        }
        fullFillXmlAttrs(params, ctx.xml_attributes_value_clause());
    }

    private DataType getDataType(Opt_js_value_returning_typeContext ctx) {
        if (ctx.js_return_default_type() != null) {
            return null;
        }
        if (ctx.js_value_return_type() != null) {
            return new OracleDataTypeFactory(ctx.js_value_return_type()).generate();
        }
        if (ctx.NCHAR() != null || ctx.NVARCHAR2() != null || ctx.CHAR() != null) {
            return OracleDataTypeFactory.getDataType(new TextTypeOpt() {
                @Override
                public ParserRuleContext getCtx() {
                    return ctx;
                }

                @Override
                public String getTypeName() {
                    return ctx.getChild(1).getText();
                }

                @Override
                public String_length_iContext getStringLengthIContext() {
                    return ctx.string_length_i();
                }

                @Override
                public Nstring_length_iContext getNstringLengthIContext() {
                    return ctx.nstring_length_i();
                }

                @Override
                public boolean isBinary() {
                    return ctx.BINARY() != null;
                }
            });
        }
        return new GeneralDataType(ctx, ctx.RAW().getText(), null);
    }

    private void setScalarsMode(JsonConstraint c, Scalars_optContext ctx) {
        if (ctx == null) {
            return;
        }
        c.setScalarsMode(ctx.ALLOW() != null ? ScalarsMode.ALLOW_SCALARS : ScalarsMode.DISALLOW_SCALARS);
    }

    private void setWrapperMode(JsonConstraint c, Wrapper_optsContext ctx) {
        if (ctx == null) {
            return;
        }
        if (ctx.WITH() != null) {
            if (ctx.ARRAY() != null) {
                if (ctx.CONDITIONAL() != null) {
                    c.setWrapperMode(WrapperMode.WITH_CONDITIONAL_ARRAY_WRAPPER);
                } else if (ctx.UNCONDITIONAL() != null) {
                    c.setWrapperMode(WrapperMode.WITH_UNCONDITIONAL_ARRAY_WRAPPER);
                } else {
                    c.setWrapperMode(WrapperMode.WITH_ARRAY_WRAPPER);
                }
            } else {
                if (ctx.CONDITIONAL() != null) {
                    c.setWrapperMode(WrapperMode.WITH_CONDITIONAL_WRAPPER);
                } else if (ctx.UNCONDITIONAL() != null) {
                    c.setWrapperMode(WrapperMode.WITH_UNCONDITIONAL_WRAPPER);
                } else {
                    c.setWrapperMode(WrapperMode.WITH_WRAPPER);
                }
            }
        } else {
            if (ctx.ARRAY() != null) {
                c.setWrapperMode(WrapperMode.WITHOUT_ARRAY_WRAPPER);
            } else {
                c.setWrapperMode(WrapperMode.WITHOUT_WRAPPER);
            }
        }
    }

    private JsonOnOption getJsonOnOption(Json_value_on_optContext ctx) {
        if (ctx == null) {
            return null;
        }
        JsonOnOption jsonOnOption = new JsonOnOption(ctx);
        if (ctx.json_value_on_empty() != null) {
            Json_value_on_empty_responseContext jCtx = ctx.json_value_on_empty()
                    .json_value_on_empty_response();
            if (jCtx.signed_literal() != null) {
                jsonOnOption.setOnEmpty(visit(jCtx.signed_literal()));
            } else if (jCtx.json_value_on_response() != null) {
                jsonOnOption.setOnEmpty(visit(jCtx.json_value_on_response()));
            }
        }
        if (ctx.json_value_on_error() != null) {
            Json_value_on_error_responseContext jCtx = ctx.json_value_on_error()
                    .json_value_on_error_response();
            if (jCtx.signed_literal() != null) {
                jsonOnOption.setOnError(visit(jCtx.signed_literal()));
            } else if (jCtx.json_value_on_response() != null) {
                jsonOnOption.setOnError(visit(jCtx.json_value_on_response()));
            }
        }
        if (ctx.opt_on_mismatchs() != null) {
            jsonOnOption.setOnMismatches(ctx.opt_on_mismatchs().opt_on_mismatch().stream().map(c -> {
                Expression opt;
                if (c.IGNORE() != null) {
                    opt = new ConstExpression(c.IGNORE());
                } else {
                    opt = visit(c.json_value_on_response());
                }
                List<String> types = null;
                if (c.mismatch_type_list() != null) {
                    types = c.mismatch_type_list().mismatch_type().stream().map(i -> {
                        if (i.empty() != null) {
                            return null;
                        }
                        CharStream input = i.getStart().getInputStream();
                        return input.getText(Interval.of(i.getStart().getStartIndex(), i.getStop().getStopIndex()));
                    }).filter(Objects::nonNull).collect(Collectors.toList());
                }
                return new OnMismatch(c, opt, types);
            }).collect(Collectors.toList()));
        }
        return jsonOnOption;
    }

    private JsonOnOption getJsonOnOption(Json_query_on_optContext ctx) {
        if (ctx == null) {
            return null;
        }
        JsonOnOption jsonOnOption = new JsonOnOption(ctx);
        if (ctx.on_empty_query() != null) {
            jsonOnOption.setOnEmpty(visit(ctx.on_empty_query().opt_response_query_on_empty_error()));
        }
        if (ctx.on_error_query() != null) {
            jsonOnOption.setOnError(visit(ctx.on_error_query().opt_response_query_on_empty_error()));
        }
        if (ctx.on_mismatch_query() != null) {
            Expression opt;
            if (ctx.on_mismatch_query().DOT() != null) {
                opt = new ConstExpression(ctx.on_mismatch_query().DOT());
            } else {
                opt = visit(ctx.on_mismatch_query().opt_response_query());
            }
            jsonOnOption.setOnMismatches(Collections.singletonList(
                    new OnMismatch(ctx.on_mismatch_query(), opt, null)));
        }
        return jsonOnOption;
    }

    private JsonOnOption getJsonOnOption(Opt_json_exists_on_error_on_emptyContext ctx) {
        if (ctx == null) {
            return null;
        }
        JsonOnOption jsonOnOption = new JsonOnOption(ctx);
        if (ctx.json_exists_on_error() != null) {
            jsonOnOption.setOnError(visit(ctx.json_exists_on_error().json_exists_response_type()));
        }
        if (ctx.json_exists_on_empty() != null) {
            jsonOnOption.setOnEmpty(visit(ctx.json_exists_on_empty().json_exists_response_type()));
        }
        return jsonOnOption;
    }

    private JsonOnOption getJsonOnOption(Js_agg_on_nullContext ctx) {
        if (ctx == null) {
            return null;
        }
        JsonOnOption jsonOnOption = new JsonOnOption(ctx);
        if (ctx.ABSENT() != null) {
            jsonOnOption.setOnNull(new ConstExpression(ctx.ABSENT()));
        } else {
            jsonOnOption.setOnNull(new NullExpression(ctx.ABSENT()));
        }
        return jsonOnOption;
    }

    private JsonOnOption getJsonOnOption(Opt_json_table_on_error_on_emptyContext ctx) {
        if (ctx == null) {
            return null;
        }
        JsonOnOption jsonOnOption = new JsonOnOption(ctx);
        if (ctx.json_table_on_error() != null) {
            jsonOnOption.setOnError(visit(ctx.json_table_on_error().json_table_on_response()));
        }
        if (ctx.json_table_on_empty() != null) {
            jsonOnOption.setOnEmpty(visit(ctx.json_table_on_empty().json_table_on_response()));
        }
        return jsonOnOption;
    }

    private JsonOnOption getJsonOnOption(Json_equal_optionContext ctx) {
        JsonOnOption jsonOnOption = new JsonOnOption(ctx);
        if (ctx.BOOL_VALUE() != null) {
            jsonOnOption.setOnError(new BoolValue(ctx.BOOL_VALUE()));
        } else {
            jsonOnOption.setOnError(new ConstExpression(ctx.ERROR_P(0)));
        }
        return jsonOnOption;
    }

    private FunctionParam visitJsonTableColumnDef(Json_table_column_defContext ctx) {
        if (ctx.json_table_ordinality_column_def() != null) {
            return visitJsonTableOrdinalityColumnDef(ctx.json_table_ordinality_column_def());
        } else if (ctx.json_table_exists_column_def() != null) {
            return visitJsonTableExistsColumnDef(ctx.json_table_exists_column_def());
        } else if (ctx.json_table_query_column_def() != null) {
            return visitJsonTableQueryColumnDef(ctx.json_table_query_column_def());
        } else if (ctx.json_table_value_column_def() != null) {
            return visitJsonTableValueColumnDef(ctx.json_table_value_column_def());
        }
        return visitJsonTableNestedColumnDef(ctx.json_table_nested_column_def());
    }

    private FunctionParam visitJsonTableOrdinalityColumnDef(Json_table_ordinality_column_defContext ctx) {
        FunctionParam param = new ExpressionParam(new ColumnReference(
                ctx.column_name(), null, null, ctx.column_name().getText()));
        param.addOption(new ConstExpression(ctx.FOR(), ctx.ORDINALITY()));
        return param;
    }

    private FunctionParam visitJsonTableExistsColumnDef(Json_table_exists_column_defContext ctx) {
        FunctionParam param = new ExpressionParam(new ColumnReference(
                ctx.column_name(), null, null, ctx.column_name().getText()));
        param.addOption(new OracleDataTypeFactory(ctx.opt_jt_value_type()).generate());
        if (ctx.TRUNCATE() != null) {
            param.addOption(new ConstExpression(ctx.TRUNCATE()));
        }
        param.addOption(new ConstExpression(ctx.EXISTS()));
        param.addOption(visit(ctx.json_table_column_def_path()));
        param.addOption(getJsonOnOption(ctx.opt_json_exists_on_error_on_empty()));
        return param;
    }

    private FunctionParam visitJsonTableQueryColumnDef(Json_table_query_column_defContext ctx) {
        FunctionParam param = new ExpressionParam(new ColumnReference(
                ctx.column_name(), null, null, ctx.column_name().getText()));
        if (ctx.opt_jt_query_type() != null && ctx.opt_jt_query_type().js_return_type() != null) {
            param.addOption(new OracleDataTypeFactory(ctx.opt_jt_query_type().js_return_type()).generate());
        }
        if (ctx.FORMAT() != null && ctx.JSON() != null) {
            param.addOption(new ConstExpression(ctx.FORMAT(), ctx.JSON()));
        } else if (ctx.JSON() != null) {
            param.addOption(new GeneralDataType(ctx.JSON(), ctx.JSON().getText(), null));
        }
        if (ctx.TRUNCATE() != null) {
            param.addOption(new ConstExpression(ctx.TRUNCATE()));
        }
        if (ctx.scalars_opt() != null || ctx.wrapper_opts() != null) {
            JsonConstraint jsonConstraint;
            if (ctx.scalars_opt() != null && ctx.wrapper_opts() != null) {
                jsonConstraint = new JsonConstraint(ctx.scalars_opt(), ctx.wrapper_opts());
            } else if (ctx.wrapper_opts() != null) {
                jsonConstraint = new JsonConstraint(ctx.wrapper_opts());
            } else {
                jsonConstraint = new JsonConstraint(ctx.scalars_opt());
            }
            setScalarsMode(jsonConstraint, ctx.scalars_opt());
            setWrapperMode(jsonConstraint, ctx.wrapper_opts());
            param.addOption(jsonConstraint);
        }
        param.addOption(visit(ctx.json_table_column_def_path()));
        param.addOption(getJsonOnOption(ctx.json_query_on_opt()));
        return param;
    }

    private FunctionParam visitJsonTableValueColumnDef(Json_table_value_column_defContext ctx) {
        FunctionParam param = new ExpressionParam(new ColumnReference(
                ctx.column_name(), null, null, ctx.column_name().getText()));
        param.addOption(new OracleDataTypeFactory(ctx.opt_jt_value_type()).generate());
        if (ctx.TRUNCATE() != null) {
            param.addOption(new ConstExpression(ctx.TRUNCATE()));
        }
        param.addOption(visit(ctx.json_table_column_def_path()));
        param.addOption(getJsonOnOption(ctx.json_value_on_opt()));
        return param;
    }

    private FunctionParam visitJsonTableNestedColumnDef(Json_table_nested_column_defContext ctx) {
        FunctionParam param = new ExpressionParam(new ConstExpression(ctx.NESTED(), ctx.PATH()));
        param.addOption(new ConstExpression(ctx.literal()));
        ctx.json_table_columns_def().json_table_column_def()
                .forEach(c -> param.addOption(visitJsonTableColumnDef(c)));
        return param;
    }

    private void setJsonExistOpt(@NonNull FunctionCall functionCall, Opt_json_existContext ctx) {
        if (ctx == null) {
            return;
        }
        if (ctx.PASSING() != null) {
            ctx.passing_elements().passing_context().stream()
                    .map(c -> new ExpressionParam(visit(c.bit_expr()), c.sql_var_name().getText()))
                    .forEach(functionCall::addOption);
        }
        functionCall.addOption(getJsonOnOption(ctx.opt_json_exists_on_error_on_empty()));
    }

    private void setFunctionOptions(FunctionCall functionCall, Aggregate_functionContext ctx) {
        if (ctx.ALL() != null) {
            functionCall.addOption(new ConstExpression(ctx.ALL()));
        } else if (ctx.DISTINCT() != null) {
            functionCall.addOption(new ConstExpression(ctx.DISTINCT()));
        } else if (ctx.UNIQUE() != null) {
            functionCall.addOption(new ConstExpression(ctx.UNIQUE()));
        }
        if (ctx.FORMAT() != null && ctx.JSON() != null) {
            functionCall.addOption(new ConstExpression(ctx.FORMAT(), ctx.JSON()));
        }
        if (ctx.WITHIN() == null && ctx.DENSE_RANK() == null && ctx.order_by() != null) {
            functionCall.addOption(new OracleOrderByFactory(ctx.order_by()).generate());
        }
        functionCall.addOption(getJsonOnOption(ctx.js_agg_on_null()));
        if (ctx.js_agg_returning_type_opt() != null) {
            Js_agg_returning_type_optContext jCtx = ctx.js_agg_returning_type_opt();
            if (jCtx.js_return_type() != null) {
                functionCall.addOption(new OracleDataTypeFactory(jCtx.js_return_type()).generate());
            } else {
                functionCall.addOption(new OracleDataTypeFactory(jCtx.js_agg_returning_type()).generate());
            }
        }
        if (ctx.STRICT() != null || ctx.json_obj_unique_key() != null) {
            functionCall.addOption(getJsonConstraint(ctx.STRICT(), ctx.json_obj_unique_key()));
        }
    }

    private FunctionParam visitXmlTableColumn(Xml_table_columnContext ctx) {
        if (ctx.xml_table_ordinality_column_def() != null) {
            return visitXmlTableOrdinalityColumnFef(ctx.xml_table_ordinality_column_def());
        } else if (ctx.xml_table_value_column_def() != null) {
            return visitXmlTableValueColumnDef(ctx.xml_table_value_column_def());
        }
        return visitXmlTableQueryColumnDef(ctx.xml_table_query_column_def());
    }

    private FunctionParam visitXmlTableOrdinalityColumnFef(Xml_table_ordinality_column_defContext ctx) {
        FunctionParam param = new ExpressionParam(new ColumnReference(
                ctx.column_name(), null, null, ctx.column_name().getText()));
        param.addOption(new ConstExpression(ctx.FOR(), ctx.ORDINALITY()));
        return param;
    }

    private FunctionParam visitXmlTableValueColumnDef(Xml_table_value_column_defContext ctx) {
        FunctionParam param = new ExpressionParam(new ColumnReference(
                ctx.column_name(), null, null, ctx.column_name().getText()));
        if (ctx.cast_data_type() != null) {
            param.addOption(new OracleDataTypeFactory(ctx.cast_data_type()).generate());
        }
        if (ctx.opt_xml_table_path() != null) {
            param.addOption(new ConstExpression(ctx.opt_xml_table_path().complex_string_literal()));
        }
        if (ctx.opt_xml_table_default_value() != null) {
            param.addOption(visit(ctx.opt_xml_table_default_value().bit_expr()));
        }
        return param;
    }

    private FunctionParam visitXmlTableQueryColumnDef(Xml_table_query_column_defContext ctx) {
        FunctionParam param = new ExpressionParam(new ColumnReference(
                ctx.column_name(), null, null, ctx.column_name().getText()));
        param.addOption(new ConstExpression(ctx.XMLTYPE()));
        if (ctx.opt_seq_by_ref_with_bracket() != null) {
            param.addOption(new ConstExpression(ctx.opt_seq_by_ref_with_bracket()));
        }
        if (ctx.opt_xml_table_path() != null) {
            param.addOption(new ConstExpression(ctx.opt_xml_table_path().complex_string_literal()));
        }
        if (ctx.opt_xml_table_default_value() != null) {
            param.addOption(visit(ctx.opt_xml_table_default_value().bit_expr()));
        }
        return param;
    }

}
