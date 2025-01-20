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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Alter_table_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTable;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTableAction;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;

import lombok.NonNull;

/**
 * {@link OracleAlterTableFactory}
 *
 * @author yh263208
 * @date 2023-06-14 16:47
 * @since ODC_release_4.2.0
 */
public class OracleAlterTableFactory extends OBParserBaseVisitor<AlterTable> implements StatementFactory<AlterTable> {

    private final ParserRuleContext parserRuleContext;

    public OracleAlterTableFactory(@NonNull Alter_table_stmtContext alterTableStmtContext) {
        this.parserRuleContext = alterTableStmtContext;
    }

    @Override
    public AlterTable generate() {
        return visit(this.parserRuleContext);
    }

    @Override
    public AlterTable visitAlter_table_stmt(Alter_table_stmtContext ctx) {
        AlterTable alterTable;
        RelationFactor relationFactor = OracleFromReferenceFactory.getRelationFactor(ctx.relation_factor());
        if (ctx.alter_table_actions() != null) {
            List<AlterTableAction> actions = ctx.alter_table_actions().alter_table_action().stream()
                    .map(c -> new OracleAlterTableActionFactory(c).generate()).collect(Collectors.toList());
            alterTable = new AlterTable(ctx, relationFactor, actions);
        } else if (ctx.alter_external_table_action() != null) {
            alterTable = new AlterTable(ctx, relationFactor, Collections.singletonList(
                    new OracleAlterTableActionFactory(ctx.alter_external_table_action()).generate()));
        } else {
            alterTable = new AlterTable(ctx, relationFactor, Collections.singletonList(
                    new OracleAlterTableActionFactory(ctx.alter_column_group_action()).generate()));
        }
        if (ctx.EXTERNAL() != null) {
            alterTable.setExternal(true);
        }
        alterTable.setUserVariable(OracleFromReferenceFactory.getUserVariable(ctx.relation_factor()));
        return alterTable;
    }

}
