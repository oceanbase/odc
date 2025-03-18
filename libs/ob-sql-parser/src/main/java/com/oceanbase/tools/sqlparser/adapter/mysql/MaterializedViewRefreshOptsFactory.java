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
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.createMaterializedView.CreateMaterializedView;
import com.oceanbase.tools.sqlparser.statement.createMaterializedView.CreateMaterializedViewOpts;
import com.oceanbase.tools.sqlparser.statement.createMaterializedView.MaterializedViewRefreshOpts;
import lombok.NonNull;

import java.util.Objects;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/18 01:20
 * @since: 4.3.4
 */
public class MaterializedViewRefreshOptsFactory extends OBParserBaseVisitor<MaterializedViewRefreshOpts> implements StatementFactory<MaterializedViewRefreshOpts> {
    private final OBParser.Mview_refresh_optContext mViewRefreshOptContext;

    public MaterializedViewRefreshOptsFactory(@NonNull OBParser.Mview_refresh_optContext mViewRefreshOptContext) {
        this.mViewRefreshOptContext = mViewRefreshOptContext;
    }
    @Override
    public MaterializedViewRefreshOpts generate() {
        return visit(this.mViewRefreshOptContext);
    }

    @Override public MaterializedViewRefreshOpts visitMview_refresh_opt(OBParser.Mview_refresh_optContext ctx) {
        MaterializedViewRefreshOpts materializedViewRefreshOpts = new MaterializedViewRefreshOpts();
        if(Objects.nonNull(ctx.NEVER())){
            materializedViewRefreshOpts.setRefreshMethod("NEVER");
        }else if(Objects.nonNull(ctx.REFRESH())){
            materializedViewRefreshOpts.setRefreshMethod(ctx.mv_refresh_method().getText());
        }
        if(Objects.nonNull(ctx.mv_refresh_on_clause())){


        }
        if(Objects.nonNull(ctx.mv_refresh_interval())){
            materializedViewRefreshOpts.setRefreshInterval(new MaterializedViewRefreshIntervalFactory(ctx.mv_refresh_interval()).generate());
        }
        return materializedViewRefreshOpts;
    }
}
