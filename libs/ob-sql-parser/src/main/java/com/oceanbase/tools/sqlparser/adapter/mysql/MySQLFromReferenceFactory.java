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

import java.util.*;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Column_listContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Dot_relation_factorContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.ExprContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Join_conditionContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Joined_tableContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Name_listContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Natural_join_typeContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Normal_relation_factorContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Outer_join_typeContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Relation_factorContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Table_factorContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Table_referenceContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Table_subqueryContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Tbl_nameContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Use_flashbackContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Use_partitionContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.JoinType;
import com.oceanbase.tools.sqlparser.statement.common.BraceBlock;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.select.ExpressionReference;
import com.oceanbase.tools.sqlparser.statement.select.FlashBackType;
import com.oceanbase.tools.sqlparser.statement.select.FlashbackUsage;
import com.oceanbase.tools.sqlparser.statement.select.FromReference;
import com.oceanbase.tools.sqlparser.statement.select.JoinCondition;
import com.oceanbase.tools.sqlparser.statement.select.JoinReference;
import com.oceanbase.tools.sqlparser.statement.select.NameReference;
import com.oceanbase.tools.sqlparser.statement.select.OnJoinCondition;
import com.oceanbase.tools.sqlparser.statement.select.PartitionType;
import com.oceanbase.tools.sqlparser.statement.select.PartitionUsage;
import com.oceanbase.tools.sqlparser.statement.select.SelectBody;
import com.oceanbase.tools.sqlparser.statement.select.UsingJoinCondition;

import lombok.NonNull;

/**
 * {@link MySQLFromReferenceFactory}
 *
 * @author yh263208
 * @date 2022-12-11 21:58
 * @since ODC_release_4.1.0
 */
public class MySQLFromReferenceFactory extends OBParserBaseVisitor<FromReference>
        implements StatementFactory<FromReference> {

    private Table_referenceContext tableReferenceContext;
    private Tbl_nameContext tableNameContext;

    public MySQLFromReferenceFactory(@NonNull Table_referenceContext tableReferenceContext) {
        this.tableReferenceContext = tableReferenceContext;
    }

    public MySQLFromReferenceFactory(@NonNull Tbl_nameContext tableNameContext) {
        this.tableNameContext = tableNameContext;
    }

    @Override
    public FromReference generate() {
        if (this.tableReferenceContext != null) {
            return visit(this.tableReferenceContext);
        }
        return visit(this.tableNameContext);
    }

    @Override
    public FromReference visitTable_reference(Table_referenceContext ctx) {
        if (ctx.table_factor() != null) {
            return visit(ctx.table_factor());
        }
        return visit(ctx.joined_table());
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
            ExpressionReference reference = (ExpressionReference) visit(ctx.table_subquery());
            if (ctx.LATERAL() != null) {
                reference.setLateral(true);
            }
            return reference;
        } else if (ctx.table_reference() != null) {
            FromReference from = visit(ctx.table_reference());
            if (ctx.LeftBrace() == null || ctx.OJ() == null || ctx.RightBrace() == null) {
                return from;
            }
            return new BraceBlock(ctx, ctx.OJ().getText(), from);
        } else if (ctx.json_table_expr() != null) {
            return new ExpressionReference(ctx, new MySQLExpressionFactory()
                    .visitJson_table_expr(ctx.json_table_expr()), alias);
        } else if (ctx.TABLE() != null && ctx.simple_expr() != null) {
            return new ExpressionReference(ctx, new MySQLExpressionFactory().visit(ctx.simple_expr()), alias);
        }
        StatementFactory<SelectBody> factory = new MySQLSelectBodyFactory(ctx.select_with_parens());
        ExpressionReference reference = new ExpressionReference(ctx, factory.generate(), null);
        if (ctx.LATERAL() != null) {
            reference.setLateral(true);
        }
        if (ctx.use_flashback() != null) {
            reference.setFlashbackUsage(visitFlashbackUsage(ctx.use_flashback()));
        }
        return reference;
    }

    @Override
    public FromReference visitJoined_table(Joined_tableContext ctx) {
        JoinType joinType = null;
        if (ctx.inner_join_type() != null) {
            if (ctx.inner_join_type().CROSS() != null) {
                joinType = JoinType.CROSS_JOIN;
            } else if (ctx.inner_join_type().INNER() != null) {
                joinType = JoinType.INNER_JOIN;
            } else if (ctx.inner_join_type().STRAIGHT_JOIN() != null) {
                joinType = JoinType.STRAIGHT_JOIN;
            } else {
                joinType = JoinType.JOIN;
            }
        } else if (ctx.outer_join_type() != null) {
            joinType = getOuterJoinType(ctx.outer_join_type());
        } else if (ctx.natural_join_type() != null) {
            Natural_join_typeContext naturalJoin = ctx.natural_join_type();
            if (naturalJoin.outer_join_type() != null) {
                JoinType outerJ = getOuterJoinType(naturalJoin.outer_join_type());
                joinType = JoinType.valueOf("NATURAL_" + outerJ.name().toUpperCase());
            } else if (naturalJoin.INNER() != null) {
                joinType = JoinType.NATURAL_INNER_JOIN;
            } else {
                joinType = JoinType.NATURAL_JOIN;
            }
        }
        if (joinType == null) {
            throw new IllegalStateException("Missing join type");
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
    public FromReference visitTbl_name(Tbl_nameContext ctx) {
        RelationFactor factor = getRelationFactor(ctx.relation_factor());
        String alias = null;
        if (ctx.relation_name() != null) {
            alias = ctx.relation_name().getText();
        }
        NameReference nameReference = new NameReference(ctx, factor.getSchema(), factor.getRelation(), alias);
        if (ctx.use_partition() != null) {
            nameReference.setPartitionUsage(visitPartitionUsage(ctx.use_partition()));
        }
        if (ctx.use_flashback() != null) {
            nameReference.setFlashbackUsage(visitFlashbackUsage(ctx.use_flashback()));
        }
        nameReference.setUserVariable(factor.getUserVariable());
        return nameReference;
    }

    public static RelationFactor getRelationFactor(Normal_relation_factorContext ctx) {
        RelationFactor relationFactor = new RelationFactor(ctx, getRelation(ctx));
        relationFactor.setSchema(getSchemaName(ctx));
        if (ctx.USER_VARIABLE() != null) {
            relationFactor.setUserVariable(ctx.USER_VARIABLE().getText());
        }
        return relationFactor;
    }

    public static RelationFactor getRelationFactor(Relation_factorContext ctx) {
        RelationFactor relationFactor = new RelationFactor(ctx, getRelation(ctx));
        relationFactor.setSchema(getSchemaName(ctx));
        if (ctx.normal_relation_factor() != null && ctx.normal_relation_factor().USER_VARIABLE() != null) {
            relationFactor.setUserVariable(ctx.normal_relation_factor().USER_VARIABLE().getText());
        }
        return relationFactor;
    }

    public static List<RelationFactor> getRelationFactors(List<Relation_factorContext> ctx) {
        if (CollectionUtils.isEmpty(ctx)) {
            return Collections.emptyList();
        }
        return ctx.stream().map(MySQLFromReferenceFactory::getRelationFactor).collect(Collectors.toList());
    }

    @Override
    public FromReference visitTable_subquery(Table_subqueryContext ctx) {
        StatementFactory<SelectBody> factory = new MySQLSelectBodyFactory(ctx.select_with_parens());
        String alias = ctx.table_subquery_alias().relation_name().getText();
        ExpressionReference reference = new ExpressionReference(ctx, factory.generate(), alias);
        if (ctx.use_flashback() != null) {
            reference.setFlashbackUsage(visitFlashbackUsage(ctx.use_flashback()));
        }
        if (ctx.table_subquery_alias().alias_name_list() != null) {
            reference.setAliasColumns(ctx.table_subquery_alias().alias_name_list()
                    .column_alias_name().stream().map(c -> c.column_name().getText())
                    .collect(Collectors.toList()));
        }
        return reference;
    }

    private static String getRelation(Normal_relation_factorContext c) {
        List<String> names = c.relation_name().stream()
                .map(RuleContext::getText).collect(Collectors.toList());
        if (c.mysql_reserved_keyword() != null) {
            return c.mysql_reserved_keyword().getText();
        } else if (names.size() == 2) {
            return names.get(1);
        } else if (names.size() == 1) {
            return names.get(0);
        }
        return null;
    }

    private static String getRelation(Relation_factorContext context) {
        if (context == null) {
            return null;
        }
        if (context.normal_relation_factor() != null) {
            return getRelation(context.normal_relation_factor());
        }
        Dot_relation_factorContext d = context.dot_relation_factor();
        if (d.relation_name() != null) {
            return d.relation_name().getText();
        } else if (d.mysql_reserved_keyword() != null) {
            return d.mysql_reserved_keyword().getText();
        }
        return null;
    }

    private static String getSchemaName(Relation_factorContext context) {
        if (context == null || context.normal_relation_factor() == null) {
            return null;
        }
        return getSchemaName(context.normal_relation_factor());
    }

    private static String getSchemaName(Normal_relation_factorContext c) {
        List<String> names = c.relation_name().stream()
                .map(RuleContext::getText).collect(Collectors.toList());
        if (c.mysql_reserved_keyword() != null) {
            return names.get(0);
        }
        if (names.size() == 2) {
            return names.get(0);
        }
        return null;
    }

    private FlashbackUsage visitFlashbackUsage(Use_flashbackContext ctx) {
        StatementFactory<Expression> factory = new MySQLExpressionFactory(ctx.bit_expr());
        return new FlashbackUsage(ctx, FlashBackType.AS_OF_SNAPSHOT, factory.generate());
    }

    public static PartitionUsage visitPartitionUsage(Use_partitionContext usePartition) {
        if (usePartition.name_list() != null) {
            List<String> nameList = new ArrayList<>();
            visitNameList(usePartition.name_list(), nameList);
            return new PartitionUsage(usePartition, PartitionType.PARTITION, nameList);
        }
        Map<String, Expression> externalTablePartition = new HashMap<>();
        usePartition.external_table_partitions().external_table_partition()
                .forEach(ctx -> externalTablePartition.put(ctx.relation_name().getText(),
                        new MySQLExpressionFactory().visit(ctx.expr_const())));
        return new PartitionUsage(usePartition, PartitionType.PARTITION, externalTablePartition);
    }

    private static void visitNameList(Name_listContext ctx, List<String> nameList) {
        if (ctx.NAME_OB() != null && ctx.name_list() == null) {
            nameList.add(ctx.NAME_OB().getText());
            return;
        }
        visitNameList(ctx.name_list(), nameList);
        nameList.add(ctx.NAME_OB().getText());
    }

    private JoinType getOuterJoinType(Outer_join_typeContext ctx) {
        List<String> joinList = new ArrayList<>();
        if (ctx.FULL() != null) {
            joinList.add(ctx.FULL().getText());
        } else if (ctx.LEFT() != null) {
            joinList.add(ctx.LEFT().getText());
        } else if (ctx.RIGHT() != null) {
            joinList.add(ctx.RIGHT().getText());
        }
        if (ctx.OUTER() != null) {
            joinList.add(ctx.OUTER().getText());
        }
        joinList.add("JOIN");
        return JoinType.valueOf(String.join("_", joinList).toUpperCase());
    }

    private JoinCondition getFromOnExpr(ExprContext exprContext, ParserRuleContext parent) {
        StatementFactory<Expression> factory = new MySQLExpressionFactory(exprContext);
        return new OnJoinCondition(parent == null ? exprContext : parent, factory.generate());
    }

    private JoinCondition getFromUsingColumnList(Column_listContext columnCtxList, ParserRuleContext parent) {
        List<ColumnReference> columnList = columnCtxList.column_definition_ref().stream().map(child -> {
            StatementFactory<ColumnReference> factory = new MySQLColumnRefFactory(child);
            return factory.generate();
        }).collect(Collectors.toList());
        return new UsingJoinCondition(parent == null ? columnCtxList : parent, columnList);
    }

}
