/*
 * Copyright (c) 2025 OceanBase.
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

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Create_mview_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Create_mview_optsContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Mview_enable_disableContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Mview_refresh_optContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Mv_refresh_on_clauseContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Mv_refresh_methodContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Mv_refresh_modeContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Mv_refresh_intervalContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Mv_start_clauseContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Mv_next_clauseContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.common.ColumnGroupElement;
import com.oceanbase.tools.sqlparser.statement.createMaterializedView.CreateMaterializedView;
import com.oceanbase.tools.sqlparser.statement.createMaterializedView.CreateMaterializedViewOpts;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;
import lombok.NonNull;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/17 20:26
 * @since: 4.3.4
 */
public class MySQLCreateMaterializedViewFactory extends OBParserBaseVisitor<CreateMaterializedView> implements StatementFactory<CreateMaterializedView> {

    private final Create_mview_stmtContext createMViewStmtContext;

    public MySQLCreateMaterializedViewFactory(@NonNull Create_mview_stmtContext createMViewStmtContext) {
        this.createMViewStmtContext = createMViewStmtContext;
    }
    @Override
    public CreateMaterializedView generate() {
        return visit(this.createMViewStmtContext);
    }

    @Override public CreateMaterializedView visitCreate_mview_stmt(Create_mview_stmtContext ctx) {
        CreateMaterializedView createMaterializedView = new CreateMaterializedView();
        if (ctx.table_option_list() != null) {
            createMaterializedView.setTableOptions(new MySQLTableOptionsFactory(ctx.table_option_list()).generate());
        }
        if (ctx.partition_option() != null) {
            createMaterializedView.setPartition(new MySQLPartitionFactory(ctx.partition_option()).generate());
        }
        if (ctx.auto_partition_option() != null) {
            createMaterializedView.setPartition(new MySQLPartitionFactory(ctx.auto_partition_option()).generate());
        }
        if (ctx.with_column_group() != null) {
            List<ColumnGroupElement> columnGroupElements = ctx.with_column_group()
                .column_group_list().column_group_element().stream()
                .map(c -> new MySQLColumnGroupElementFactory(c).generate()).collect(Collectors.toList());
            createMaterializedView.setColumnGroupElements(columnGroupElements);
        }
        if (ctx.create_mview_opts() != null) {
            createMaterializedView.setCreateMaterializedViewOpts(new MySQLCreateMaterializedViewOptsFactory(ctx.create_mview_opts()).generate());
        }
        return createMaterializedView;
    }

}
