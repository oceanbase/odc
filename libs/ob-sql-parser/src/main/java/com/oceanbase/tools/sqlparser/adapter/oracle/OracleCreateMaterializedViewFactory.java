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

import org.antlr.v4.runtime.RuleContext;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Create_mview_optsContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Create_mview_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Mv_refresh_intervalContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Mview_refresh_optContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.common.ColumnGroupElement;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;
import com.oceanbase.tools.sqlparser.statement.createmview.CreateMaterializedView;
import com.oceanbase.tools.sqlparser.statement.createmview.MaterializedViewOptions;
import com.oceanbase.tools.sqlparser.statement.createmview.MaterializedViewRefreshOption;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineConstraint;
import com.oceanbase.tools.sqlparser.statement.select.Select;

import lombok.NonNull;

/**
 * {@link OracleCreateMaterializedViewFactory}
 *
 * @author yh263208
 * @date 2025-03-20 00:31
 * @since: 4.3.4
 */
public class OracleCreateMaterializedViewFactory extends OBParserBaseVisitor<CreateMaterializedView>
        implements StatementFactory<CreateMaterializedView> {

    private final Create_mview_stmtContext createMViewStmtContext;

    public OracleCreateMaterializedViewFactory(@NonNull Create_mview_stmtContext createMViewStmtContext) {
        this.createMViewStmtContext = createMViewStmtContext;
    }

    @Override
    public CreateMaterializedView generate() {
        return visit(this.createMViewStmtContext);
    }

    @Override
    public CreateMaterializedView visitCreate_mview_stmt(Create_mview_stmtContext ctx) {
        RelationFactor viewName = OracleFromReferenceFactory.getRelationFactor(ctx.view_name().relation_factor());
        Select asSelect = new OracleSelectFactory(ctx.view_subquery()).generate();
        CreateMaterializedView createMView = new CreateMaterializedView(ctx, viewName, asSelect);
        if (ctx.mv_column_list() != null) {
            if (ctx.mv_column_list().column_name_list() != null) {
                createMView.setColumns(ctx.mv_column_list().column_name_list().column_name()
                    .stream().map(RuleContext::getText).collect(Collectors.toList()));
            }
            if (ctx.mv_column_list().out_of_line_primary_index() != null) {
                createMView.setPrimaryKey((OutOfLineConstraint) new OracleTableElementFactory(
                        ctx.mv_column_list().out_of_line_primary_index()).generate());
            }
        }
        if (ctx.table_option_list() != null) {
            createMView.setTableOptions(new OracleTableOptionsFactory(ctx.table_option_list()).generate());
        }
        if (ctx.partition_option() != null) {
            createMView.setPartition(new OraclePartitionFactory(ctx.partition_option()).generate());
        } else if (ctx.auto_partition_option() != null) {
            createMView.setPartition(new OraclePartitionFactory(ctx.auto_partition_option()).generate());
        }
        if (ctx.with_column_group() != null) {
            List<ColumnGroupElement> columnGroupElements = ctx.with_column_group()
                    .column_group_list().column_group_element().stream()
                    .map(c -> new OracleColumnGroupElementFactory(c).generate()).collect(Collectors.toList());
            createMView.setColumnGroupElements(columnGroupElements);
        }
        if (ctx.create_mview_opts() != null) {
            createMView.setViewOptions(visitCreateMViewOpts(ctx.create_mview_opts()));
        }
        if (ctx.view_with_opt().with_check_option() != null) {
            List<String> list = new ArrayList<>();
            for (int i = 0; i < ctx.view_with_opt().with_check_option().getChildCount(); i++) {
                list.add(ctx.view_with_opt().with_check_option().getChild(i).getText().toUpperCase());
            }
            createMView.setWithOption(String.join(" ", list));
        } else if (ctx.view_with_opt().WITH() != null
                && ctx.view_with_opt().READ() != null
                && ctx.view_with_opt().ONLY() != null) {
            createMView.setWithOption("WITH READ ONLY");
        }
        return createMView;
    }

    private MaterializedViewOptions visitCreateMViewOpts(Create_mview_optsContext ctx) {
        MaterializedViewOptions mViewOptions = new MaterializedViewOptions(
                ctx, visitMViewRefreshOpt(ctx.mview_refresh_opt()));
        if (ctx.on_query_computation_clause() != null) {
            if (ctx.on_query_computation_clause().DISABLE() != null) {
                mViewOptions.setEnableQueryComputation(false);
            } else if (ctx.on_query_computation_clause().ENABLE() != null) {
                mViewOptions.setEnableQueryComputation(true);
            }
        }
        if (ctx.query_rewrite_clause() != null) {
            if (ctx.query_rewrite_clause().DISABLE() != null) {
                mViewOptions.setEnableQueryWrite(false);
            } else if (ctx.query_rewrite_clause().ENABLE() != null) {
                mViewOptions.setEnableQueryWrite(true);
            }
        }
        return mViewOptions;
    }

    private MaterializedViewRefreshOption visitMViewRefreshOpt(Mview_refresh_optContext ctx) {
        boolean neverRefresh = ctx.NEVER() != null;
        String refreshMode = null;
        if (ctx.mv_refresh_method() != null) {
            refreshMode = ctx.mv_refresh_method().getText();
        }
        MaterializedViewRefreshOption option = new MaterializedViewRefreshOption(ctx, neverRefresh, refreshMode);
        if (ctx.mv_refresh_on_clause().mv_refresh_mode() != null) {
            option.setRefreshMode(ctx.mv_refresh_on_clause().mv_refresh_mode().getText());
        }
        Mv_refresh_intervalContext intervalCtx = ctx.mv_refresh_interval();
        if (intervalCtx.mv_start_clause().bit_expr() != null) {
            option.setStartWith(new OracleExpressionFactory(intervalCtx.mv_start_clause().bit_expr()).generate());
        }
        if (intervalCtx.mv_next_clause().bit_expr() != null) {
            option.setNext(new OracleExpressionFactory(intervalCtx.mv_next_clause().bit_expr()).generate());
        }
        return option;
    }

}
