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

import java.util.Objects;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.creatematerializedview.MaterializedViewRefreshInterval;

import lombok.NonNull;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/18 09:59
 * @since: 4.3.4
 */
public class MySQLMaterializedViewRefreshIntervalFactory extends OBParserBaseVisitor<MaterializedViewRefreshInterval>
        implements StatementFactory<MaterializedViewRefreshInterval> {
    private final OBParser.Mv_refresh_intervalContext mvRefreshIntervalContext;

    public MySQLMaterializedViewRefreshIntervalFactory(
            @NonNull OBParser.Mv_refresh_intervalContext mvRefreshIntervalContext) {
        this.mvRefreshIntervalContext = mvRefreshIntervalContext;
    }

    @Override
    public MaterializedViewRefreshInterval generate() {
        return visit(this.mvRefreshIntervalContext);
    }

    @Override
    public MaterializedViewRefreshInterval visitMv_refresh_interval(OBParser.Mv_refresh_intervalContext ctx) {
        MaterializedViewRefreshInterval materializedViewRefreshInterval = new MaterializedViewRefreshInterval(ctx);
        if (Objects.nonNull(ctx.mv_start_clause())) {
            materializedViewRefreshInterval.setStartTime(ctx.mv_start_clause().bit_expr().getText());
        }
        if (Objects.nonNull(ctx.mv_next_clause())) {
            materializedViewRefreshInterval.setInterval(Long.valueOf(ctx.mv_next_clause().bit_expr().expr().getText()));
            materializedViewRefreshInterval.setTimeUnit(ctx.mv_next_clause().bit_expr().date_unit().getText());
        }
        return materializedViewRefreshInterval;
    }

}
