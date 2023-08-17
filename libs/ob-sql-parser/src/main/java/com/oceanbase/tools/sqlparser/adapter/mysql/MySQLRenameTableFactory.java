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

import java.util.List;
import java.util.stream.Collectors;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Rename_table_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.alter.table.RenameTable;
import com.oceanbase.tools.sqlparser.statement.alter.table.RenameTableAction;

import lombok.NonNull;

/**
 * {@link MySQLRenameTableFactory}
 *
 * @author yh263208
 * @date 2023-06-15 17:03
 * @since ODC_release_4.2.0
 */
public class MySQLRenameTableFactory extends OBParserBaseVisitor<RenameTable> implements StatementFactory<RenameTable> {

    private final Rename_table_stmtContext renameTableStmtContext;

    public MySQLRenameTableFactory(@NonNull Rename_table_stmtContext renameTableStmtContext) {
        this.renameTableStmtContext = renameTableStmtContext;
    }

    @Override
    public RenameTable generate() {
        return visit(this.renameTableStmtContext);
    }

    @Override
    public RenameTable visitRename_table_stmt(Rename_table_stmtContext ctx) {
        List<RenameTableAction> actions = ctx.rename_table_actions().rename_table_action().stream()
                .map(c -> new MySQLRenameTableActionFactory(c).generate()).collect(Collectors.toList());
        return new RenameTable(ctx, actions);
    }

}
