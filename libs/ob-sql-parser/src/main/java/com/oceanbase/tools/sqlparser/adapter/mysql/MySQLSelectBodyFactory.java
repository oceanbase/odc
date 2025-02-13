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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.ExprContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.From_listContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Groupby_clauseContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Insert_valsContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Named_windowsContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.No_table_selectContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.No_table_select_with_order_and_limitContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Query_expression_option_listContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Select_clause_setContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Select_clause_set_with_order_and_limitContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Select_expr_listContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Select_no_parensContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Select_with_parensContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Select_with_parens_with_order_and_limitContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Set_expression_optionContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Set_typeContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Simple_selectContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Simple_select_with_order_and_limitContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Table_referencesContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Table_values_clauseContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Table_values_clause_with_order_by_and_limitContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.With_clauseContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.common.Window;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;
import com.oceanbase.tools.sqlparser.statement.select.ForUpdate;
import com.oceanbase.tools.sqlparser.statement.select.FromReference;
import com.oceanbase.tools.sqlparser.statement.select.GroupBy;
import com.oceanbase.tools.sqlparser.statement.select.NameReference;
import com.oceanbase.tools.sqlparser.statement.select.OrderBy;
import com.oceanbase.tools.sqlparser.statement.select.Projection;
import com.oceanbase.tools.sqlparser.statement.select.RelatedSelectBody;
import com.oceanbase.tools.sqlparser.statement.select.RelationType;
import com.oceanbase.tools.sqlparser.statement.select.SelectBody;
import com.oceanbase.tools.sqlparser.statement.select.SortKey;
import com.oceanbase.tools.sqlparser.statement.select.WithTable;
import com.oceanbase.tools.sqlparser.statement.select.mysql.Limit;

import lombok.NonNull;

/**
 * {@link MySQLSelectBodyFactory}
 *
 * @author yh263208
 * @date 2022-12-11 23:52
 * @since ODC_release_4.1.0
 * @see StatementFactory
 */
public class MySQLSelectBodyFactory
        extends OBParserBaseVisitor<SelectBody> implements StatementFactory<SelectBody> {

    private Select_no_parensContext selectNoParensContext = null;
    private Select_with_parensContext selectWithParensContext = null;

    public MySQLSelectBodyFactory(@NonNull Select_no_parensContext selectNoParensContext) {
        this.selectNoParensContext = selectNoParensContext;
    }

    public MySQLSelectBodyFactory(@NonNull Select_with_parensContext selectWithParensContext) {
        this.selectWithParensContext = selectWithParensContext;
    }

    @Override
    public SelectBody generate() {
        if (this.selectWithParensContext != null) {
            return visit(this.selectWithParensContext);
        }
        return visit(this.selectNoParensContext);
    }

    @Override
    public SelectBody visitSelect_with_parens(Select_with_parensContext ctx) {
        SelectBody select;
        if (ctx.select_no_parens() != null) {
            select = new SelectBody(ctx, visit(ctx.select_no_parens()));
        } else {
            select = new SelectBody(ctx, visit(ctx.select_with_parens()));
        }
        if (ctx.with_clause() != null) {
            With_clauseContext w = ctx.with_clause();
            if (w.RECURSIVE() != null) {
                select.setRecursive(true);
            }
            select.getWith().addAll(w.with_list().common_table_expr().stream().map(c -> {
                StatementFactory<WithTable> factory = new MySQLWithTableFactory(c);
                return factory.generate();
            }).collect(Collectors.toList()));
        }
        return select;
    }

    @Override
    public SelectBody visitSelect_no_parens(Select_no_parensContext ctx) {
        SelectBody select;
        if (ctx.select_clause() != null) {
            select = new SelectBody(ctx, visit(ctx.select_clause()));
        } else if (ctx.select_clause_set() != null) {
            select = new SelectBody(ctx, visit(ctx.select_clause_set()));
        } else {
            select = new SelectBody(ctx, visit(ctx.select_clause_set_with_order_and_limit()));
        }
        if (ctx.for_update_clause() != null) {
            StatementFactory<ForUpdate> factory = new MySQLForUpdateFactory(ctx.for_update_clause());
            select.getLastSelectBody().setForUpdate(factory.generate());
        }
        if (ctx.opt_lock_in_share_mode() != null) {
            select.getLastSelectBody().setLockInShareMode(true);
        }
        return select;
    }

    @Override
    public SelectBody visitTable_values_clause(Table_values_clauseContext ctx) {
        return new SelectBody(ctx, ctx.values_row_list().row_value().stream()
                .map(c -> getValueRows(c.insert_vals())).collect(Collectors.toList()));
    }

    @Override
    public SelectBody visitTable_values_clause_with_order_by_and_limit(
            Table_values_clause_with_order_by_and_limitContext ctx) {
        SelectBody selectBody = new SelectBody(ctx, visit(ctx.table_values_clause()));
        if (ctx.order_by() != null) {
            selectBody.setOrderBy(new MySQLOrderByFactory(ctx.order_by()).generate());
        }
        if (ctx.limit_clause() != null) {
            selectBody.setLimit(new MySQLLimitFactory(ctx.limit_clause()).generate());
        }
        return selectBody;
    }

    @Override
    public SelectBody visitSelect_with_parens_with_order_and_limit(Select_with_parens_with_order_and_limitContext ctx) {
        SelectBody select = new SelectBody(ctx, visit(ctx.select_with_parens()));
        if (ctx.order_by() != null) {
            StatementFactory<OrderBy> factory = new MySQLOrderByFactory(ctx.order_by());
            select.getLastSelectBody().setOrderBy(factory.generate());
        }
        if (ctx.limit_clause() != null) {
            StatementFactory<Limit> factory = new MySQLLimitFactory(ctx.limit_clause());
            select.getLastSelectBody().setLimit(factory.generate());
        }
        return select;
    }

    @Override
    public SelectBody visitSelect_clause_set_with_order_and_limit(Select_clause_set_with_order_and_limitContext ctx) {
        SelectBody select = new SelectBody(ctx, visit(ctx.select_clause_set()));
        if (ctx.order_by() != null) {
            StatementFactory<OrderBy> factory = new MySQLOrderByFactory(ctx.order_by());
            select.getLastSelectBody().setOrderBy(factory.generate());
        }
        if (ctx.limit_clause() != null) {
            StatementFactory<Limit> factory = new MySQLLimitFactory(ctx.limit_clause());
            select.getLastSelectBody().setLimit(factory.generate());
        }
        return select;
    }

    @Override
    public SelectBody visitSelect_clause_set(Select_clause_setContext ctx) {
        SelectBody left = null;
        if (ctx.select_clause_set() != null) {
            left = new SelectBody(ctx, visit(ctx.select_clause_set()));
            if (ctx.order_by() != null) {
                StatementFactory<OrderBy> factory = new MySQLOrderByFactory(ctx.order_by());
                left.getLastSelectBody().setOrderBy(factory.generate());
            }
            if (ctx.limit_clause() != null) {
                StatementFactory<Limit> factory = new MySQLLimitFactory(ctx.limit_clause());
                left.getLastSelectBody().setLimit(factory.generate());
            }
        } else if (ctx.select_clause_set_left() != null) {
            left = new SelectBody(ctx, visit(ctx.select_clause_set_left()));
        }
        if (left == null) {
            throw new IllegalStateException("Missing left clause");
        }
        RelationType type = getRelationType(ctx.set_type());
        SelectBody right = visit(ctx.select_clause_set_right());
        left.getLastSelectBody().setRelatedSelect(new RelatedSelectBody(right, type));
        return left;
    }

    private RelationType getRelationType(Set_typeContext setType) {
        if (setType.set_type_other() != null) {
            return RelationType.valueOf(setType.set_type_other().getText().toUpperCase());
        }
        Set_expression_optionContext op = setType.set_expression_option();
        if (op != null) {
            if (op.ALL() != null) {
                return RelationType.UNION_ALL;
            } else if (op.DISTINCT() != null) {
                return RelationType.UNION_DISTINCT;
            } else if (op.UNIQUE() != null) {
                return RelationType.UNION_UNIQUE;
            }
        }
        return RelationType.UNION;
    }

    @Override
    public SelectBody visitSimple_select_with_order_and_limit(Simple_select_with_order_and_limitContext ctx) {
        SelectBody select = new SelectBody(ctx, visit(ctx.simple_select()));
        if (ctx.order_by() != null) {
            select.setOrderBy(new MySQLOrderByFactory(ctx.order_by()).generate());
        }
        if (ctx.opt_approx() != null) {
            select.setApproximate(true);
        }
        if (ctx.limit_clause() != null) {
            select.setLimit(new MySQLLimitFactory(ctx.limit_clause()).generate());
        }
        return select;
    }

    @Override
    public SelectBody visitSimple_select(Simple_selectContext ctx) {
        return visitSelect(new SelectContext() {
            @Override
            public ParserRuleContext getTarget() {
                return ctx;
            }

            @Override
            public Select_expr_listContext projectionList() {
                return ctx.select_expr_list();
            }

            @Override
            public List<FromReference> fromList() {
                return visitFromList(ctx.from_list());
            }

            @Override
            public Groupby_clauseContext groupClause() {
                return ctx.groupby_clause();
            }

            @Override
            public TerminalNode whereNode() {
                return ctx.WHERE();
            }

            @Override
            public TerminalNode havingNode() {
                return ctx.HAVING();
            }

            @Override
            public List<ExprContext> exprList() {
                return ctx.expr();
            }

            @Override
            public Query_expression_option_listContext queryOptionList() {
                return ctx.query_expression_option_list();
            }

            @Override
            public Named_windowsContext namedWindows() {
                return ctx.named_windows();
            }
        });
    }

    @Override
    public SelectBody visitNo_table_select_with_order_and_limit(No_table_select_with_order_and_limitContext ctx) {
        SelectBody select = new SelectBody(ctx, visit(ctx.no_table_select()));
        if (ctx.order_by() != null) {
            StatementFactory<OrderBy> factory = new MySQLOrderByFactory(ctx.order_by());
            select.setOrderBy(factory.generate());
        }
        if (ctx.limit_clause() != null) {
            StatementFactory<Limit> factory = new MySQLLimitFactory(ctx.limit_clause());
            select.setLimit(factory.generate());
        }
        return select;
    }

    @Override
    public SelectBody visitNo_table_select(No_table_selectContext ctx) {
        return visitSelect(new SelectContext() {
            @Override
            public ParserRuleContext getTarget() {
                return ctx;
            }

            @Override
            public Select_expr_listContext projectionList() {
                return ctx.select_expr_list();
            }

            @Override
            public List<FromReference> fromList() {
                if (ctx.DUAL() == null) {
                    return new ArrayList<>();
                }
                FromReference ref = new NameReference(ctx.DUAL(), null, ctx.DUAL().getText(), null);
                return Collections.singletonList(ref);
            }

            @Override
            public Groupby_clauseContext groupClause() {
                return ctx.groupby_clause();
            }

            @Override
            public TerminalNode whereNode() {
                return ctx.WHERE();
            }

            @Override
            public TerminalNode havingNode() {
                return ctx.HAVING();
            }

            @Override
            public List<ExprContext> exprList() {
                return ctx.expr();
            }

            @Override
            public Query_expression_option_listContext queryOptionList() {
                return ctx.query_expression_option_list();
            }

            @Override
            public Named_windowsContext namedWindows() {
                return ctx.named_windows();
            }
        });
    }

    private SelectBody visitSelect(SelectContext ctx) {
        List<Projection> selectItems = visitProjectionList(ctx.projectionList());
        SelectBody select = new SelectBody(ctx.getTarget(), selectItems, ctx.fromList());
        ExprContext where = null;
        ExprContext having = null;
        if (ctx.whereNode() != null && ctx.havingNode() != null) {
            // 既有 where 子句，也有 having 子句，此时第一个 expr 是 where 的，第二个是 having 的
            where = ctx.exprList().get(0);
            if (where == null) {
                throw new IllegalStateException("Missing where clause");
            }
            having = ctx.exprList().get(1);
            if (having == null) {
                throw new IllegalStateException("Missing having clause");
            }
        } else if (ctx.whereNode() == null && ctx.havingNode() != null) {
            having = ctx.exprList().get(0);
        } else if (ctx.havingNode() == null && ctx.whereNode() != null) {
            where = ctx.exprList().get(0);
        }
        if (where != null) {
            StatementFactory<Expression> factory = new MySQLExpressionFactory(where);
            select.setWhere(factory.generate());
        }
        if (having != null) {
            StatementFactory<Expression> factory = new MySQLExpressionFactory(having);
            select.setHaving(factory.generate());
        }
        if (ctx.groupClause() != null) {
            if (ctx.groupClause().ROLLUP() != null) {
                select.setWithRollUp(true);
            }
            select.setGroupBy(visitGroupByClause(ctx.groupClause()));
        }
        if (ctx.queryOptionList() != null) {
            select.setQueryOptions(getQueryExpr(ctx.queryOptionList()));
        }
        if (ctx.namedWindows() != null) {
            select.setWindows(visitNamedWindows(ctx.namedWindows()));
        }
        return select;
    }

    private List<Projection> visitProjectionList(Select_expr_listContext context) {
        return context.projection().stream().map(child -> {
            StatementFactory<Projection> factory = new MySQLProjectionFactory(child);
            return factory.generate();
        }).collect(Collectors.toList());
    }

    public static List<FromReference> visitFromList(From_listContext context) {
        return visitFromList(context.table_references());
    }

    public static List<FromReference> visitFromList(Table_referencesContext context) {
        List<FromReference> fromRefs = context.table_reference().stream().map(c -> {
            StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(c);
            return factory.generate();
        }).collect(Collectors.toList());
        fromRefs.addAll(context.table_references_paren().stream().flatMap(t -> {
            Stream<FromReference> s2 = t.table_reference().stream().map(r -> {
                StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(r);
                return factory.generate();
            });
            if (t.table_references_paren() == null) {
                return s2;
            }
            Stream<FromReference> s1 = t.table_references_paren().table_reference().stream().map(r -> {
                StatementFactory<FromReference> factory = new MySQLFromReferenceFactory(r);
                return factory.generate();
            });
            return Stream.concat(s1, s2);
        }).collect(Collectors.toList()));
        return fromRefs;
    }

    private List<GroupBy> visitGroupByClause(Groupby_clauseContext context) {
        return context.sort_list_for_group_by().sort_key_for_group_by().stream().map(c -> {
            StatementFactory<SortKey> factory = new MySQLSortKeyFactory(c);
            return factory.generate();
        }).collect(Collectors.toList());
    }

    private List<Window> visitNamedWindows(Named_windowsContext context) {
        return context.named_window().stream().map(c -> {
            StatementFactory<Window> factory = new MySQLWindowFactory(c);
            return factory.generate();
        }).collect(Collectors.toList());
    }

    private String getQueryExpr(Query_expression_option_listContext context) {
        return context.query_expression_option().stream().map(RuleContext::getText).collect(Collectors.joining(" "));
    }

    private List<Expression> getValueRows(Insert_valsContext ctx) {
        Expression expr = null;
        if (ctx.expr_or_default() != null) {
            if (ctx.expr_or_default().expr() == null) {
                expr = new ConstExpression(ctx.expr_or_default().DEFAULT());
            } else {
                expr = new MySQLExpressionFactory(ctx.expr_or_default().expr()).generate();
            }
        }
        if (ctx.empty() != null || expr == null) {
            return new ArrayList<>();
        }
        List<Expression> exprs;
        if (ctx.insert_vals() == null) {
            exprs = new ArrayList<>();
        } else {
            exprs = getValueRows(ctx.insert_vals());
        }
        exprs.add(expr);
        return exprs;
    }

    /**
     * {@link SelectContext}
     *
     * @author yh263208
     * @date 2022-12-07 18:21
     * @since ODC_release_4.1.0
     */
    interface SelectContext {

        ParserRuleContext getTarget();

        Select_expr_listContext projectionList();

        List<FromReference> fromList();

        Groupby_clauseContext groupClause();

        TerminalNode whereNode();

        TerminalNode havingNode();

        List<ExprContext> exprList();

        Query_expression_option_listContext queryOptionList();

        Named_windowsContext namedWindows();
    }

}
