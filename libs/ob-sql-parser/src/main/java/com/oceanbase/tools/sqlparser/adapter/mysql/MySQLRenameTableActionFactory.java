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
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Rename_table_actionContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.alter.table.RenameTableAction;

import lombok.NonNull;

/**
 * {@link MySQLRenameTableActionFactory}
 *
 * @author yh263208
 * @date 2023-06-15 16:29
 * @since ODC_release_4.2.0
 */
public class MySQLRenameTableActionFactory extends OBParserBaseVisitor<RenameTableAction>
        implements StatementFactory<RenameTableAction> {

    private final Rename_table_actionContext renameTableActionContext;

    public MySQLRenameTableActionFactory(@NonNull Rename_table_actionContext renameTableActionContext) {
        this.renameTableActionContext = renameTableActionContext;
    }

    @Override
    public RenameTableAction generate() {
        return visit(this.renameTableActionContext);
    }

    @Override
    public RenameTableAction visitRename_table_action(Rename_table_actionContext ctx) {
        return new RenameTableAction(ctx,
                MySQLFromReferenceFactory.getRelationFactor(ctx.relation_factor(0)),
                MySQLFromReferenceFactory.getRelationFactor(ctx.relation_factor(1)));
    }

}
