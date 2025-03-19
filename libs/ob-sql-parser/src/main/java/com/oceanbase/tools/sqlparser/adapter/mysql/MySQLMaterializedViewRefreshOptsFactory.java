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
import com.oceanbase.tools.sqlparser.statement.creatematerializedview.MaterializedViewRefreshOpts;

import lombok.NonNull;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/18 09:20
 * @since: 4.3.4
 */
public class MySQLMaterializedViewRefreshOptsFactory extends OBParserBaseVisitor<MaterializedViewRefreshOpts>
        implements StatementFactory<MaterializedViewRefreshOpts> {
    private final OBParser.Mview_refresh_optContext mViewRefreshOptContext;

    public MySQLMaterializedViewRefreshOptsFactory(@NonNull OBParser.Mview_refresh_optContext mViewRefreshOptContext) {
        this.mViewRefreshOptContext = mViewRefreshOptContext;
    }

    @Override
    public MaterializedViewRefreshOpts generate() {
        return visit(this.mViewRefreshOptContext);
    }

    @Override
    public MaterializedViewRefreshOpts visitMview_refresh_opt(OBParser.Mview_refresh_optContext ctx) {
        MaterializedViewRefreshOpts materializedViewRefreshOpts = new MaterializedViewRefreshOpts(ctx);
        if (Objects.nonNull(ctx.NEVER())) {
            materializedViewRefreshOpts.setRefreshMethod("NEVER");
        } else if (Objects.nonNull(ctx.REFRESH())) {
            materializedViewRefreshOpts.setRefreshMethod(ctx.mv_refresh_method().getText());
        }
        if (Objects.nonNull(ctx.mv_refresh_on_clause())) {
            materializedViewRefreshOpts.setRefreshOn(
                    new MySQLMaterializedViewRefreshOnClauseFactory(ctx.mv_refresh_on_clause()).generate());
        }
        if (Objects.nonNull(ctx.mv_refresh_interval())) {
            materializedViewRefreshOpts.setRefreshInterval(
                    new MySQLMaterializedViewRefreshIntervalFactory(ctx.mv_refresh_interval()).generate());
        }
        return materializedViewRefreshOpts;
    }
}
