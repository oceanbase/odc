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

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.creatematerializedview.MaterializedViewRefreshOnClause;

import lombok.NonNull;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/18 10:11
 * @since: 4.3.4
 */
public class MySQLMaterializedViewRefreshOnClauseFactory extends OBParserBaseVisitor<MaterializedViewRefreshOnClause>
        implements StatementFactory<MaterializedViewRefreshOnClause> {
    private final OBParser.Mv_refresh_on_clauseContext mvRefreshOnClauseContext;

    public MySQLMaterializedViewRefreshOnClauseFactory(
            @NonNull OBParser.Mv_refresh_on_clauseContext mvRefreshOnClauseContext) {
        this.mvRefreshOnClauseContext = mvRefreshOnClauseContext;
    }

    @Override
    public MaterializedViewRefreshOnClause generate() {
        return visit(this.mvRefreshOnClauseContext);
    }

    @Override
    public MaterializedViewRefreshOnClause visitMv_refresh_on_clause(OBParser.Mv_refresh_on_clauseContext ctx) {
        MaterializedViewRefreshOnClause materializedViewRefreshOnClause = new MaterializedViewRefreshOnClause(ctx);
        materializedViewRefreshOnClause.setRefreshMode(ctx.mv_refresh_mode().getText());
        return materializedViewRefreshOnClause;
    }
}
