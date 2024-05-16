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
import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Column_listContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.ExprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Join_conditionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Join_outerContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Joined_tableContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Natural_join_typeContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Normal_relation_factorContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Order_by_fetch_with_check_optionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Outer_join_typeContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Pivot_aggr_clauseContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Relation_factorContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Select_functionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Table_factorContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Table_referenceContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Table_subqueryContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Tbl_nameContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Transpose_clauseContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Transpose_for_clauseContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Transpose_in_clauseContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Unpivot_column_clauseContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Unpivot_in_clauseContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Use_flashbackContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.Expression.ReferenceOperator;
import com.oceanbase.tools.sqlparser.statement.JoinType;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.expression.FunctionCall;
import com.oceanbase.tools.sqlparser.statement.expression.RelationReference;
import com.oceanbase.tools.sqlparser.statement.select.ExpressionReference;
import com.oceanbase.tools.sqlparser.statement.select.FlashBackType;
import com.oceanbase.tools.sqlparser.statement.select.FlashbackUsage;
import com.oceanbase.tools.sqlparser.statement.select.FromReference;
import com.oceanbase.tools.sqlparser.statement.select.JoinCondition;
import com.oceanbase.tools.sqlparser.statement.select.JoinReference;
import com.oceanbase.tools.sqlparser.statement.select.NameReference;
import com.oceanbase.tools.sqlparser.statement.select.OnJoinCondition;
import com.oceanbase.tools.sqlparser.statement.select.SelectBody;
import com.oceanbase.tools.sqlparser.statement.select.UsingJoinCondition;
import com.oceanbase.tools.sqlparser.statement.select.oracle.Pivot;
import com.oceanbase.tools.sqlparser.statement.select.oracle.Pivot.ExpressionItem;
import com.oceanbase.tools.sqlparser.statement.select.oracle.Pivot.FunctionItem;
import com.oceanbase.tools.sqlparser.statement.select.oracle.UnPivot;
import com.oceanbase.tools.sqlparser.statement.select.oracle.UnPivot.InItem;

import lombok.NonNull;

/**
 * {@link OracleFromReferenceFactory}
 *
 * @author yh263208
 * @date 2022-12-05 20:24
 * @since ODC_release_4.1.0
 * @see StatementFactory
 */
public class OracleFromReferenceFactory extends OBParserBaseVisitor<FromReference>
        implements StatementFactory<FromReference> {

    private Table_referenceContext tableReferenceContext;
    private Table_factorContext tableFactorContext;

    public OracleFromReferenceFactory(@NonNull Table_referenceContext tableReferenceContext) {
        this.tableReferenceContext = tableReferenceContext;
    }

    public OracleFromReferenceFactory(@NonNull Table_factorContext tableFactorContext) {
        this.tableFactorContext = tableFactorContext;
    }

    @Override
    public FromReference generate() {
        if (this.tableReferenceContext != null) {
            return visit(this.tableReferenceContext);
        }
        return visit(this.tableFactorContext);
    }

    @Override
    public FromReference visitTable_reference(Table_referenceContext ctx) {
        if (ctx.table_factor() != null) {
            return visit(ctx.table_factor());
        }
        return visit(ctx.joined_table());
    }

    @Override
    public FromReference visitJoined_table(Joined_tableContext ctx) {
        JoinType joinType;
        if (ctx.outer_join_type() != null) {
            joinType = getOuterJoinType(ctx.outer_join_type());
        } else if (ctx.INNER() != null) {
            joinType = JoinType.INNER_JOIN;
        } else if (ctx.CROSS() != null) {
            joinType = JoinType.CROSS_JOIN;
        } else if (ctx.natural_join_type() != null) {
            Natural_join_typeContext naturalJoinType = ctx.natural_join_type();
            if (naturalJoinType.INNER() != null) {
                joinType = JoinType.NATURAL_INNER_JOIN;
            } else if (naturalJoinType.outer_join_type() != null) {
                JoinType outerJ = getOuterJoinType(naturalJoinType.outer_join_type());
                joinType = JoinType.valueOf("NATURAL_" + outerJ.name().toUpperCase());
            } else {
                joinType = JoinType.NATURAL_JOIN;
            }
        } else {
            joinType = JoinType.JOIN;
        }
        List<Table_factorContext> tableFactors = ctx.table_factor();
        FromReference left;
        FromReference right;
        if (tableFactors.size() == 2) {
            left = visit(tableFactors.get(0));
            right = visit(tableFactors.get(1));
        } else {
            left = visit(ctx.joined_table());
            right = visit(tableFactors.get(0));
        }
        JoinCondition condition = null;
        Join_conditionContext joinCondition = ctx.join_condition();
        if (joinCondition != null) {
            if (joinCondition.ON() != null) {
                condition = getFromOnExpr(joinCondition.expr(), joinCondition);
            } else {
                condition = getFromUsingColumnList(joinCondition.column_list(), joinCondition);
            }
        } else if (ctx.ON() != null) {
            condition = getFromOnExpr(ctx.expr(), null);
        } else if (ctx.USING() != null) {
            condition = getFromUsingColumnList(ctx.column_list(), null);
        }
        return new JoinReference(ctx, left, right, joinType, condition);
    }

    @Override
    public FromReference visitTable_factor(Table_factorContext ctx) {
        String alias = null;
        if (ctx.relation_name() != null) {
            alias = ctx.relation_name().getText();
        }
        if (ctx.tbl_name() != null) {
            return visit(ctx.tbl_name());
        } else if (ctx.table_subquery() != null) {
            return visit(ctx.table_subquery());
        } else if (ctx.table_reference() != null) {
            return visit(ctx.table_reference());
        } else if (ctx.simple_expr() != null) {
            return new ExpressionReference(ctx,
                    new OracleExpressionFactory(ctx.simple_expr()).generate(), alias);
        } else if (ctx.select_no_parens() != null) {
            return new ExpressionReference(ctx,
                    new OracleSelectBodyFactory(ctx.select_no_parens()).generate(), alias);
        } else if (ctx.select_function() != null) {
            return new ExpressionReference(ctx, visitSelectFunction(ctx.select_function()), alias);
        } else if (ctx.json_table_expr() != null) {
            return new ExpressionReference(ctx, new OracleExpressionFactory()
                    .visitJson_table_expr(ctx.json_table_expr()), alias);
        }
        return new ExpressionReference(ctx, new OracleExpressionFactory()
                .visitXml_table_expr(ctx.xml_table_expr()), alias);
    }

    private Expression visitSelectFunction(Select_functionContext ctx) {
        if (ctx.access_func_expr() != null) {
            return new OracleExpressionFactory().getFunctionCall(ctx.access_func_expr());
        }
        RelationReference ref = new RelationReference(ctx.database_factor(), ctx.database_factor().getText());
        ref.reference(visitSelectFunction(ctx.select_function()), ReferenceOperator.DOT);
        return ref;
    }

    @Override
    public FromReference visitTable_subquery(Table_subqueryContext ctx) {
        String alias = null;
        if (ctx.relation_name() != null) {
            alias = ctx.relation_name().getText();
        }
        if (ctx.select_with_parens() != null) {
            OracleSelectBodyFactory factory = new OracleSelectBodyFactory(ctx.select_with_parens());
            ExpressionReference reference = new ExpressionReference(ctx, factory.generate(), alias);
            if (ctx.use_flashback() != null) {
                reference.setFlashbackUsage(visitFlashbackUsage(ctx.use_flashback()));
            }
            reference.setUnPivot(visitUnPivot(ctx.transpose_clause()));
            reference.setPivot(visitPivot(ctx.transpose_clause()));
            return reference;
        }
        OracleSelectBodyFactory factory = new OracleSelectBodyFactory(ctx.subquery());
        SelectBody select = factory.generate();
        Order_by_fetch_with_check_optionContext oCtx = ctx.order_by_fetch_with_check_option();
        if (oCtx.order_by() != null) {
            select.getLastSelectBody().setOrderBy(new OracleOrderByFactory(oCtx.order_by()).generate());
        }
        if (oCtx.fetch_next_clause() != null) {
            select.getLastSelectBody().setFetch(new OracleFetchFactory(oCtx.fetch_next_clause()).generate());
        }
        if (oCtx.with_check_option() != null) {
            select.getLastSelectBody().setWithCheckOption(true);
        }
        ExpressionReference reference = new ExpressionReference(ctx, select, alias);
        if (ctx.use_flashback() != null) {
            reference.setFlashbackUsage(visitFlashbackUsage(ctx.use_flashback()));
        }
        reference.setUnPivot(visitUnPivot(ctx.transpose_clause()));
        reference.setPivot(visitPivot(ctx.transpose_clause()));
        return reference;
    }

    @Override
    public FromReference visitTbl_name(Tbl_nameContext ctx) {
        if (ctx.dual_table() != null) {
            String alias = ctx.relation_name() == null ? null : ctx.relation_name().getText();
            return new NameReference(ctx, null, ctx.dual_table().DUAL().getText(), alias);
        }
        Relation_factorContext relationFactor = ctx.relation_factor();
        String alias = null;
        if (ctx.relation_name() != null) {
            alias = ctx.relation_name().getText();
        }
        NameReference nameReference =
                new NameReference(ctx, getSchemaName(relationFactor), getRelation(relationFactor), alias);
        if (ctx.use_partition() != null) {
            OraclePartitionUsageFactory factory = new OraclePartitionUsageFactory(ctx.use_partition());
            nameReference.setPartitionUsage(factory.generate());
        }
        if (ctx.use_flashback() != null) {
            nameReference.setFlashbackUsage(visitFlashbackUsage(ctx.use_flashback()));
        }
        nameReference.setPivot(visitPivot(ctx.transpose_clause()));
        nameReference.setUnPivot(visitUnPivot(ctx.transpose_clause()));
        nameReference.setUserVariable(getUserVariable(relationFactor));
        return nameReference;
    }

    public static String getSchemaName(Relation_factorContext relationFactor) {
        if (relationFactor == null || relationFactor.normal_relation_factor() == null) {
            return null;
        }
        return getSchemaName(relationFactor.normal_relation_factor());
    }

    public static String getRelation(Relation_factorContext relationFactor) {
        if (relationFactor == null) {
            return null;
        }
        if (relationFactor.normal_relation_factor() != null) {
            return getRelation(relationFactor.normal_relation_factor());
        } else if (relationFactor.dot_relation_factor() != null) {
            return relationFactor.dot_relation_factor().relation_name().getText();
        }
        return null;
    }

    public static String getUserVariable(Relation_factorContext relationFactor) {
        if (relationFactor == null || relationFactor.normal_relation_factor() == null) {
            return null;
        }
        return getUserVariable(relationFactor.normal_relation_factor());
    }

    public static RelationFactor getRelationFactor(Normal_relation_factorContext ctx) {
        RelationFactor relationFactor = new RelationFactor(ctx, getRelation(ctx));
        relationFactor.setSchema(getSchemaName(ctx));
        relationFactor.setUserVariable(getUserVariable(ctx));
        if (ctx.opt_reverse_link_flag() != null && ctx.opt_reverse_link_flag().Not() != null) {
            relationFactor.setReverseLink(true);
        }
        return relationFactor;
    }

    public static RelationFactor getRelationFactor(Relation_factorContext ctx) {
        RelationFactor relationFactor = new RelationFactor(ctx, getRelation(ctx));
        relationFactor.setSchema(getSchemaName(ctx));
        relationFactor.setUserVariable(getUserVariable(ctx));
        if (ctx.normal_relation_factor() != null
                && ctx.normal_relation_factor().opt_reverse_link_flag() != null
                && ctx.normal_relation_factor().opt_reverse_link_flag().Not() != null) {
            relationFactor.setReverseLink(true);
        }
        return relationFactor;
    }

    public static String getSchemaName(Normal_relation_factorContext ctx) {
        return ctx.database_factor() != null ? ctx.database_factor().relation_name().getText() : null;
    }

    public static String getRelation(Normal_relation_factorContext ctx) {
        return ctx.relation_name().getText();
    }

    public static String getUserVariable(Normal_relation_factorContext ctx) {
        return ctx.USER_VARIABLE() != null ? ctx.USER_VARIABLE().getText() : null;
    }

    private Pivot visitPivot(Transpose_clauseContext ctx) {
        if (ctx == null || ctx.PIVOT() == null) {
            return null;
        }
        Pivot_aggr_clauseContext aggrContext = ctx.pivot_aggr_clause();
        List<FunctionItem> functionItems = aggrContext.pivot_single_aggr_clause().stream().map(c -> {
            OracleExpressionFactory factory = new OracleExpressionFactory();
            Expression fCall;
            if (c.aggregate_function() != null) {
                fCall = factory.visitAggregate_function(c.aggregate_function());
            } else {
                fCall = factory.visitAccess_func_expr_count(c.access_func_expr_count());
            }
            String alias = c.relation_name() == null ? null : c.relation_name().getText();
            return new FunctionItem(c, (FunctionCall) fCall, alias);
        }).collect(Collectors.toList());
        List<ColumnReference> forColumns = new ArrayList<>();
        Transpose_for_clauseContext forClause = ctx.transpose_for_clause();
        if (forClause.column_name() != null) {
            forColumns.add(new ColumnReference(forClause.column_name(),
                    null, null, forClause.column_name().getText()));
        } else {
            forColumns = forClause.column_name_list().column_name().stream()
                    .map(c -> new ColumnReference(c, null, null, c.getText()))
                    .collect(Collectors.toList());
        }
        Transpose_in_clauseContext inClause = ctx.transpose_in_clause();
        List<ExpressionItem> expressionItems = inClause.transpose_in_args().transpose_in_arg().stream().map(c -> {
            String alias = c.relation_name() == null ? null : c.relation_name().getText();
            return new ExpressionItem(c, new OracleExpressionFactory(c.bit_expr()).generate(), alias);
        }).collect(Collectors.toList());
        Pivot pivot = new Pivot(ctx, functionItems, forColumns, expressionItems);
        if (ctx.relation_name() != null) {
            pivot.setAlias(ctx.relation_name().getText());
        }
        return pivot;
    }

    private UnPivot visitUnPivot(Transpose_clauseContext ctx) {
        if (ctx == null || ctx.UNPIVOT() == null) {
            return null;
        }
        boolean includeNulls = ctx.INCLUDE() != null;
        Unpivot_column_clauseContext clauseContext = ctx.unpivot_column_clause();
        List<ColumnReference> unpivotColumns = new ArrayList<>();
        if (clauseContext.column_name() != null) {
            unpivotColumns.add(new ColumnReference(clauseContext.column_name(),
                    null, null, clauseContext.column_name().getText()));
        } else {
            unpivotColumns = clauseContext.column_name_list().column_name().stream()
                    .map(c -> new ColumnReference(c, null, null, c.getText())).collect(Collectors.toList());
        }
        List<ColumnReference> forColumns = new ArrayList<>();
        Transpose_for_clauseContext forClauseContext = ctx.transpose_for_clause();
        if (forClauseContext.column_name() != null) {
            forColumns.add(new ColumnReference(forClauseContext.column_name(),
                    null, null, forClauseContext.column_name().getText()));
        } else {
            forColumns = forClauseContext.column_name_list().column_name().stream()
                    .map(c -> new ColumnReference(c, null, null, c.getText())).collect(Collectors.toList());
        }
        Unpivot_in_clauseContext inClauseContext = ctx.unpivot_in_clause();
        List<InItem> inItems = inClauseContext.unpivot_in_args().unpivot_in_arg().stream().map(c -> {
            List<ColumnReference> columns = new ArrayList<>();
            Unpivot_column_clauseContext c1 = c.unpivot_column_clause();
            if (c1.column_name() != null) {
                columns.add(new ColumnReference(c1.column_name(), null, null, c1.column_name().getText()));
            } else {
                columns = c1.column_name_list().column_name().stream()
                        .map(c2 -> new ColumnReference(c2, null, null, c2.getText())).collect(Collectors.toList());
            }
            Expression as = null;
            if (c.bit_expr() != null) {
                StatementFactory<Expression> factory = new OracleExpressionFactory(c.bit_expr());
                as = factory.generate();
            }
            return new InItem(c, columns, as);
        }).collect(Collectors.toList());
        UnPivot unPivot = new UnPivot(ctx, includeNulls, unpivotColumns, forColumns, inItems);
        if (ctx.relation_name() != null) {
            unPivot.setAlias(ctx.relation_name().getText());
        }
        return unPivot;
    }

    private FlashbackUsage visitFlashbackUsage(Use_flashbackContext ctx) {
        OracleExpressionFactory factory = new OracleExpressionFactory(ctx.bit_expr());
        FlashBackType type = FlashBackType.AS_OF_SCN;
        if (ctx.TIMESTAMP() != null) {
            type = FlashBackType.AS_OF_TIMESTAMP;
        }
        return new FlashbackUsage(ctx, type, factory.generate());
    }

    private JoinCondition getFromOnExpr(ExprContext exprContext, ParserRuleContext parent) {
        StatementFactory<Expression> factory = new OracleExpressionFactory(exprContext);
        return new OnJoinCondition(parent == null ? exprContext : parent, factory.generate());
    }

    private JoinCondition getFromUsingColumnList(Column_listContext columnCtxList, ParserRuleContext parent) {
        List<ColumnReference> columnList = columnCtxList.column_definition_ref().stream().map(child -> {
            OracleColumnRefFactory factory = new OracleColumnRefFactory(child);
            return factory.generate();
        }).collect(Collectors.toList());
        return new UsingJoinCondition(parent == null ? columnCtxList : parent, columnList);
    }

    private JoinType getOuterJoinType(Outer_join_typeContext outerJoinType) {
        Join_outerContext joinOuter = outerJoinType.join_outer();
        boolean outer = joinOuter != null && joinOuter.OUTER() != null;
        if (outerJoinType.FULL() != null) {
            return outer ? JoinType.FULL_OUTER_JOIN : JoinType.FULL_JOIN;
        } else if (outerJoinType.LEFT() != null) {
            return outer ? JoinType.LEFT_OUTER_JOIN : JoinType.LEFT_JOIN;
        } else if (outerJoinType.RIGHT() != null) {
            return outer ? JoinType.RIGHT_OUTER_JOIN : JoinType.RIGHT_JOIN;
        }
        throw new IllegalStateException("Illegal context, " + outerJoinType.getText());
    }

}
