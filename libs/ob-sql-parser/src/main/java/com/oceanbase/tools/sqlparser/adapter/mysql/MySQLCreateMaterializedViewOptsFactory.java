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
import com.oceanbase.tools.sqlparser.statement.creatematerializedview.CreateMaterializedViewOpts;

import lombok.NonNull;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/18 09:00
 * @since: 4.3.4
 */
public class MySQLCreateMaterializedViewOptsFactory extends OBParserBaseVisitor<CreateMaterializedViewOpts>
        implements StatementFactory<CreateMaterializedViewOpts> {

    private final OBParser.Create_mview_optsContext createMViewOptsContext;

    public MySQLCreateMaterializedViewOptsFactory(@NonNull OBParser.Create_mview_optsContext createMViewOptsContext) {
        this.createMViewOptsContext = createMViewOptsContext;
    }

    @Override
    public CreateMaterializedViewOpts generate() {
        return visit(this.createMViewOptsContext);
    }

    @Override
    public CreateMaterializedViewOpts visitCreate_mview_opts(OBParser.Create_mview_optsContext ctx) {
        CreateMaterializedViewOpts createMaterializedViewOpts = new CreateMaterializedViewOpts(ctx);
        // ctx.mview_refresh_opt()
        if (Objects.nonNull(ctx.REWRITE()) && Objects.nonNull(ctx.COMPUTATION())) {
            if (ctx.REWRITE().getSymbol().getStartIndex() < ctx.COMPUTATION().getSymbol().getStartIndex()) {
                createMaterializedViewOpts.setEnableQueryRewrite(Objects.nonNull(ctx.mview_enable_disable(0).ENABLE()));
                createMaterializedViewOpts
                        .setEnableQueryComputation(Objects.nonNull(ctx.mview_enable_disable(1).ENABLE()));
            } else {
                createMaterializedViewOpts
                        .setEnableQueryComputation(Objects.nonNull(ctx.mview_enable_disable(0).ENABLE()));
                createMaterializedViewOpts.setEnableQueryRewrite(Objects.nonNull(ctx.mview_enable_disable(1).ENABLE()));
            }
        } else {
            if (Objects.nonNull(ctx.REWRITE())) {
                createMaterializedViewOpts.setEnableQueryRewrite(Objects.nonNull(ctx.mview_enable_disable(0).ENABLE()));
            } else if (Objects.nonNull(ctx.COMPUTATION())) {
                createMaterializedViewOpts
                        .setEnableQueryComputation(Objects.nonNull(ctx.mview_enable_disable(0).ENABLE()));
            }
        }
        if (Objects.nonNull(ctx.mview_refresh_opt())) {
            createMaterializedViewOpts.setMaterializedViewRefreshOpts(
                    new MySQLMaterializedViewRefreshOptsFactory(ctx.mview_refresh_opt()).generate());
        }

        return createMaterializedViewOpts;
    }

}
