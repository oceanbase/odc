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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Alter_column_group_optionContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Alter_table_actionsContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Alter_table_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTable;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTableAction;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;

import lombok.NonNull;

/**
 * {@link MySQLAlterTableFactory}
 *
 * @author yh263208
 * @date 2023-06-15 13:57
 * @since ODC_release_4.2.0
 */
public class MySQLAlterTableFactory extends OBParserBaseVisitor<AlterTable> implements StatementFactory<AlterTable> {

    private final ParserRuleContext parserRuleContext;

    public MySQLAlterTableFactory(@NonNull Alter_table_stmtContext context) {
        this.parserRuleContext = context;
    }

    @Override
    public AlterTable generate() {
        return visit(this.parserRuleContext);
    }

    @Override
    public AlterTable visitAlter_table_stmt(Alter_table_stmtContext ctx) {
        RelationFactor factor = MySQLFromReferenceFactory.getRelationFactor(ctx.relation_factor());
        List<AlterTableAction> actions = null;
        if (ctx.alter_table_actions() != null) {
            actions = getAlterTableActions(ctx.alter_table_actions());
        } else if (ctx.alter_column_group_option() != null) {
            actions = getAlterTableActions(ctx.alter_column_group_option());
        }
        AlterTable alterTable = new AlterTable(ctx, factor.getRelation(), actions);
        if (ctx.EXTERNAL() != null) {
            alterTable.setExternal(true);
        }
        alterTable.setSchema(factor.getSchema());
        alterTable.setUserVariable(factor.getUserVariable());
        return alterTable;
    }

    private List<AlterTableAction> getAlterTableActions(Alter_table_actionsContext context) {
        if (context == null) {
            return null;
        }
        List<AlterTableAction> actions = new ArrayList<>();
        if (context.alter_table_action() != null && context.alter_table_actions() == null) {
            actions.add(new MySQLAlterTableActionFactory(context.alter_table_action()).generate());
        } else if (context.alter_table_action() != null && context.alter_table_actions() != null) {
            actions.addAll(getAlterTableActions(context.alter_table_actions()));
            actions.add(new MySQLAlterTableActionFactory(context.alter_table_action()).generate());
        }
        return actions;
    }

    private List<AlterTableAction> getAlterTableActions(Alter_column_group_optionContext context) {
        return Collections.singletonList(new MySQLAlterTableActionFactory(context).generate());
    }

}
