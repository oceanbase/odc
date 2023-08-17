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
import java.util.Objects;
import java.util.stream.Collectors;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Cube_clauseContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Group_by_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Groupby_elementContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Grouping_sets_clauseContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Rollup_clauseContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.select.GroupBy;
import com.oceanbase.tools.sqlparser.statement.select.oracle.CubeGroupBy;
import com.oceanbase.tools.sqlparser.statement.select.oracle.GeneralGroupBy;
import com.oceanbase.tools.sqlparser.statement.select.oracle.GroupingSetsGroupBy;
import com.oceanbase.tools.sqlparser.statement.select.oracle.RollUpGroupBy;

import lombok.NonNull;

/**
 * {@link OracleGroupByFactory}
 *
 * @author yh263208
 * @date 2022-12-06 20:40
 * @since ODC_release_4.1.0
 * @see StatementFactory
 */
public class OracleGroupByFactory extends OBParserBaseVisitor<GroupBy> implements StatementFactory<GroupBy> {

    private final Groupby_elementContext groupbyElementContext;

    public OracleGroupByFactory(@NonNull Groupby_elementContext groupbyElementContext) {
        this.groupbyElementContext = groupbyElementContext;
    }

    @Override
    public GroupBy generate() {
        return visit(this.groupbyElementContext);
    }

    @Override
    public GroupBy visitGroupby_element(Groupby_elementContext ctx) {
        if (ctx.group_by_expr() != null) {
            return visit(ctx.group_by_expr());
        } else if (ctx.rollup_clause() != null) {
            return visit(ctx.rollup_clause());
        } else if (ctx.cube_clause() != null) {
            return visit(ctx.cube_clause());
        } else if (ctx.grouping_sets_clause() != null) {
            return visit(ctx.grouping_sets_clause());
        }
        return new GroupingSetsGroupBy(ctx, new ArrayList<>());
    }

    @Override
    public GroupBy visitGroup_by_expr(Group_by_exprContext ctx) {
        StatementFactory<Expression> factory = new OracleExpressionFactory(ctx.bit_expr());
        return new GeneralGroupBy(ctx, factory.generate());
    }

    @Override
    public GroupBy visitRollup_clause(Rollup_clauseContext ctx) {
        return new RollUpGroupBy(ctx, getExpressions(ctx.group_by_expr_list().group_by_expr()));
    }

    @Override
    public GroupBy visitCube_clause(Cube_clauseContext ctx) {
        return new CubeGroupBy(ctx, getExpressions(ctx.group_by_expr_list().group_by_expr()));
    }

    @Override
    public GroupBy visitGrouping_sets_clause(Grouping_sets_clauseContext ctx) {
        List<GroupBy> groupByList = ctx.grouping_sets_list().grouping_sets().stream().map(child -> {
            if (child.group_by_expr() != null) {
                return visit(child.group_by_expr());
            } else if (child.rollup_clause() != null) {
                return visit(child.rollup_clause());
            } else if (child.cube_clause() != null) {
                return visit(child.cube_clause());
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        return new GroupingSetsGroupBy(ctx, groupByList);
    }

    private List<Expression> getExpressions(List<Group_by_exprContext> groupByExprs) {
        return groupByExprs.stream().map(c -> {
            StatementFactory<Expression> factory = new OracleExpressionFactory(c.bit_expr());
            return factory.generate();
        }).collect(Collectors.toList());
    }

}
