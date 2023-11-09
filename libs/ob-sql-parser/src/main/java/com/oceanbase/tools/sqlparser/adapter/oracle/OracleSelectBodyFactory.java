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

import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.ExprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.From_listContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Groupby_clauseContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Query_expression_option_listContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Select_clauseContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Select_clause_setContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Select_clause_set_leftContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Select_clause_set_rightContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Select_expr_listContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Select_no_parensContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Select_with_hierarchical_queryContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Select_with_parensContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Set_expression_optionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Set_typeContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Simple_selectContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.SubqueryContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.With_clauseContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.With_selectContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.select.FromReference;
import com.oceanbase.tools.sqlparser.statement.select.GroupBy;
import com.oceanbase.tools.sqlparser.statement.select.OrderBy;
import com.oceanbase.tools.sqlparser.statement.select.Projection;
import com.oceanbase.tools.sqlparser.statement.select.RelatedSelectBody;
import com.oceanbase.tools.sqlparser.statement.select.RelationType;
import com.oceanbase.tools.sqlparser.statement.select.SelectBody;
import com.oceanbase.tools.sqlparser.statement.select.WithTable;
import com.oceanbase.tools.sqlparser.statement.select.oracle.Fetch;

import lombok.NonNull;

/**
 * {@link OracleSelectBodyFactory}
 *
 * @author yh263208
 * @date 2022-11-25 16:13
 * @since ODC_release_4.1.0
 * @see OBParserBaseVisitor
 */
public class OracleSelectBodyFactory extends OBParserBaseVisitor<SelectBody> implements StatementFactory<SelectBody> {

    private SubqueryContext subqueryContext = null;
    private Select_no_parensContext selectNoParensContext = null;
    private With_selectContext withSelectContext = null;
    private Select_with_parensContext selectWithParensContext = null;

    public OracleSelectBodyFactory(@NonNull With_selectContext withSelectContext) {
        this.withSelectContext = withSelectContext;
    }

    public OracleSelectBodyFactory(@NonNull Select_no_parensContext selectNoParensContext) {
        this.selectNoParensContext = selectNoParensContext;
    }

    public OracleSelectBodyFactory(@NonNull SubqueryContext subqueryContext) {
        this.subqueryContext = subqueryContext;
    }

    public OracleSelectBodyFactory(@NonNull Select_with_parensContext selectWithParensContext) {
        this.selectWithParensContext = selectWithParensContext;
    }

    @Override
    public SelectBody generate() {
        if (this.selectNoParensContext != null) {
            return visit(this.selectNoParensContext);
        } else if (this.subqueryContext != null) {
            return visit(this.subqueryContext);
        } else if (this.selectWithParensContext != null) {
            return visit(this.selectWithParensContext);
        } else if (this.withSelectContext != null) {
            return visit(this.withSelectContext);
        }
        throw new IllegalStateException("No Context available");
    }

    @Override
    public SelectBody visitSubquery(SubqueryContext ctx) {
        if (ctx.select_no_parens() != null) {
            return visit(ctx.select_no_parens());
        } else if (ctx.select_with_parens() != null) {
            return visit(ctx.select_with_parens());
        } else if (ctx.with_select() != null) {
            return visit(ctx.with_select());
        }
        throw new IllegalStateException("No alternative node");
    }

    @Override
    public SelectBody visitSelect_with_parens(Select_with_parensContext ctx) {
        if (ctx.select_with_parens() != null) {
            return visit(ctx.select_with_parens());
        }
        SelectBody select;
        if (ctx.select_no_parens() != null) {
            select = new SelectBody(ctx, visit(ctx.select_no_parens()));
        } else if (ctx.with_select() != null) {
            select = new SelectBody(ctx, visit(ctx.with_select()));
        } else {
            throw new IllegalStateException("No alternative node");
        }
        if (ctx.order_by() != null) {
            StatementFactory<OrderBy> factory = new OracleOrderByFactory(ctx.order_by());
            select.getLastSelectBody().setOrderBy(factory.generate());
        }
        if (ctx.fetch_next_clause() != null) {
            StatementFactory<Fetch> factory = new OracleFetchFactory(ctx.fetch_next_clause());
            select.getLastSelectBody().setFetch(factory.generate());
        }
        return select;
    }

    @Override
    public SelectBody visitSelect_no_parens(Select_no_parensContext ctx) {
        if (ctx.select_clause() != null) {
            return visit(ctx.select_clause());
        }
        return visit(ctx.select_clause_set());
    }

    @Override
    public SelectBody visitWith_select(With_selectContext ctx) {
        SelectBody select;
        if (ctx.select_no_parens() != null) {
            select = new SelectBody(ctx, visit(ctx.select_no_parens()));
        } else {
            select = new SelectBody(ctx, visit(ctx.select_with_parens()));
        }
        With_clauseContext withClause = ctx.with_clause();
        if (withClause.RECURSIVE() != null) {
            select.setRecursive(true);
        }
        if (withClause.common_table_expr() != null) {
            StatementFactory<WithTable> factory = new OracleWithTableFactory(withClause.common_table_expr());
            select.getWith().add(factory.generate());
        } else if (withClause.with_list() != null) {
            withClause.with_list().common_table_expr().forEach(c -> {
                StatementFactory<WithTable> factory = new OracleWithTableFactory(c);
                select.getWith().add(factory.generate());
            });
        }
        return select;
    }

    @Override
    public SelectBody visitSelect_clause_set(Select_clause_setContext ctx) {
        SelectBody left;
        SelectBody right = visit(ctx.select_clause_set_right());
        if (ctx.select_clause_set() != null) {
            left = new SelectBody(ctx, visit(ctx.select_clause_set()));
        } else if (ctx.select_clause_set_left() != null) {
            left = new SelectBody(ctx, visit(ctx.select_clause_set_left()));
        } else {
            throw new IllegalStateException("Node is not found");
        }
        Set_typeContext type = ctx.set_type();
        if (type == null) {
            throw new IllegalStateException("Missing set top node");
        }
        RelationType relationType;
        if (type.set_type_other() != null) {
            relationType = RelationType.valueOf(type.set_type_other().getText());
        } else {
            Set_expression_optionContext o = type.set_expression_option();
            relationType = RelationType.UNION;
            if (o != null && o.ALL() != null) {
                relationType = RelationType.UNION_ALL;
            }
        }
        left.getLastSelectBody().setRelatedSelect(new RelatedSelectBody(right, relationType));
        return left;
    }

    @Override
    public SelectBody visitSelect_clause_set_right(Select_clause_set_rightContext ctx) {
        if (ctx.simple_select() != null) {
            return visit(ctx.simple_select());
        } else if (ctx.select_with_hierarchical_query() != null) {
            return visit(ctx.select_with_hierarchical_query());
        }
        return visit(ctx.select_with_parens());
    }

    @Override
    public SelectBody visitSelect_clause_set_left(Select_clause_set_leftContext ctx) {
        return visit(ctx.select_clause_set_right());
    }

    @Override
    public SelectBody visitSelect_clause(Select_clauseContext ctx) {
        if (ctx.simple_select() != null) {
            return visit(ctx.simple_select());
        }
        return visit(ctx.select_with_hierarchical_query());
    }

    @Override
    public SelectBody visitSelect_with_hierarchical_query(Select_with_hierarchical_queryContext ctx) {
        SelectBody select = visitSelect(new SelectContext() {
            @Override
            public ParserRuleContext getTarget() {
                return ctx;
            }

            @Override
            public Select_expr_listContext projectionList() {
                return ctx.select_expr_list();
            }

            @Override
            public From_listContext fromList() {
                return ctx.from_list();
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
        });
        if (ctx.start_with() != null) {
            StatementFactory<Expression> factory = new OracleExpressionFactory(ctx.start_with().expr());
            select.setStartWith(factory.generate());
        }
        if (ctx.connect_by() != null) {
            StatementFactory<Expression> factory = new OracleExpressionFactory(ctx.connect_by().expr());
            select.setConnectBy(factory.generate());
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
            public From_listContext fromList() {
                return ctx.from_list();
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
        });
    }

    private SelectBody visitSelect(SelectContext ctx) {
        List<Projection> selectItems = visitProjectionList(ctx.projectionList());
        List<FromReference> froms = visitFromList(ctx.fromList());
        SelectBody select = new SelectBody(ctx.getTarget(), selectItems, froms);
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
            StatementFactory<Expression> factory = new OracleExpressionFactory(where);
            select.setWhere(factory.generate());
        }
        if (having != null) {
            StatementFactory<Expression> factory = new OracleExpressionFactory(having);
            select.setHaving(factory.generate());
        }
        if (ctx.groupClause() != null) {
            select.setGroupBy(visitGroupByClause(ctx.groupClause()));
        }
        if (ctx.queryOptionList() != null) {
            select.setQueryOptions(getQueryExpr(ctx.queryOptionList()));
        }
        return select;
    }

    private String getQueryExpr(Query_expression_option_listContext context) {
        return context.query_expression_option().stream().map(RuleContext::getText).collect(Collectors.joining(" "));
    }

    private List<Projection> visitProjectionList(Select_expr_listContext context) {
        return context.projection().stream().map(child -> {
            OracleProjectionFactory factory = new OracleProjectionFactory(child);
            return factory.generate();
        }).collect(Collectors.toList());
    }

    private List<FromReference> visitFromList(From_listContext context) {
        return context.table_references().table_reference().stream().map(c -> {
            StatementFactory<FromReference> factory = new OracleFromReferenceFactory(c);
            return factory.generate();
        }).collect(Collectors.toList());
    }

    private List<GroupBy> visitGroupByClause(Groupby_clauseContext context) {
        return context.groupby_element_list().groupby_element().stream().map(c -> {
            StatementFactory<GroupBy> factory = new OracleGroupByFactory(c);
            return factory.generate();
        }).collect(Collectors.toList());
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

        From_listContext fromList();

        Groupby_clauseContext groupClause();

        TerminalNode whereNode();

        TerminalNode havingNode();

        List<ExprContext> exprList();

        Query_expression_option_listContext queryOptionList();
    }

}
