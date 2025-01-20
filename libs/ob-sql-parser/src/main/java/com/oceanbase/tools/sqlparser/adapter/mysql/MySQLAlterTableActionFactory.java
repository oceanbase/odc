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
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Alter_column_behaviorContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Alter_column_group_optionContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Alter_column_optionContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Alter_constraint_optionContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Alter_index_optionContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Alter_partition_optionContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Alter_table_actionContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Alter_tablegroup_optionContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Modify_partition_infoContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Name_listContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Opt_partition_range_or_listContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTableAction;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTableAction.AlterColumnBehavior;
import com.oceanbase.tools.sqlparser.statement.common.ColumnGroupElement;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnDefinition;
import com.oceanbase.tools.sqlparser.statement.createtable.ConstraintState;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineIndex;
import com.oceanbase.tools.sqlparser.statement.createtable.PartitionElement;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;

import lombok.NonNull;

/**
 * {@link MySQLAlterTableActionFactory}
 *
 * @author yh263208
 * @date 2023-06-15 13:57
 * @since ODC_release_4.2.0
 */
public class MySQLAlterTableActionFactory extends OBParserBaseVisitor<AlterTableAction>
        implements StatementFactory<AlterTableAction> {

    private final ParserRuleContext parserRuleContext;

    public MySQLAlterTableActionFactory(@NonNull Alter_table_actionContext alterTableActionContext) {
        this.parserRuleContext = alterTableActionContext;
    }

    public MySQLAlterTableActionFactory(@NonNull Alter_column_group_optionContext alterTableActionContext) {
        this.parserRuleContext = alterTableActionContext;
    }

    @Override
    public AlterTableAction generate() {
        return visit(this.parserRuleContext);
    }

    @Override
    public AlterTableAction visitAlter_table_action(Alter_table_actionContext ctx) {
        AlterTableAction alterTableAction = new AlterTableAction(ctx);
        if (ctx.table_option_list_space_seperated() != null) {
            alterTableAction.setTableOptions(new MySQLTableOptionsFactory(
                    ctx.table_option_list_space_seperated()).generate());
            return alterTableAction;
        } else if (ctx.RENAME() != null) {
            alterTableAction.setRenameToTable(MySQLFromReferenceFactory.getRelationFactor(ctx.relation_factor()));
            return alterTableAction;
        } else if (ctx.CONVERT() != null && ctx.TO() != null) {
            alterTableAction.setCharset(ctx.charset_name().getText());
            if (ctx.collation() != null) {
                alterTableAction.setCollation(ctx.collation().collation_name().getText());
            }
            return alterTableAction;
        } else if (ctx.REFRESH() != null) {
            alterTableAction.setRefresh(true);
            return alterTableAction;
        }
        return visitChildren(ctx);
    }

    @Override
    public AlterTableAction visitAlter_column_option(Alter_column_optionContext ctx) {
        AlterTableAction alterTableAction = new AlterTableAction(ctx);
        if (ctx.ADD() != null) {
            List<ColumnDefinition> addColumns = new ArrayList<>();
            if (ctx.column_definition() != null) {
                addColumns.add((ColumnDefinition) new MySQLTableElementFactory(ctx.column_definition()).generate());
            } else {
                addColumns = ctx.column_definition_list().column_definition().stream()
                        .map(c -> (ColumnDefinition) new MySQLTableElementFactory(c).generate())
                        .collect(Collectors.toList());
            }
            alterTableAction.setAddColumns(addColumns);
        } else if (ctx.DROP() != null) {
            String option = null;
            if (ctx.CASCADE() != null) {
                option = ctx.CASCADE().getText();
            } else if (ctx.RESTRICT() != null) {
                option = ctx.RESTRICT().getText();
            }
            alterTableAction.setDropColumn(new MySQLColumnRefFactory(ctx.column_definition_ref()).generate(), option);
        } else if (ctx.MODIFY() != null) {
            List<ColumnDefinition> columns = new ArrayList<>();
            columns.add((ColumnDefinition) new MySQLTableElementFactory(ctx.column_definition()).generate());
            alterTableAction.setModifyColumns(columns);
        } else if (ctx.CHANGE() != null) {
            ColumnReference colRef = new MySQLColumnRefFactory(ctx.column_definition_ref()).generate();
            ColumnDefinition colDef =
                    (ColumnDefinition) new MySQLTableElementFactory(ctx.column_definition()).generate();
            alterTableAction.changeColumn(colRef, colDef);
        } else if (ctx.ALTER() != null) {
            ColumnReference colRef = new MySQLColumnRefFactory(ctx.column_definition_ref()).generate();
            Alter_column_behaviorContext aCtx = ctx.alter_column_behavior();
            AlterColumnBehavior behavior = new AlterColumnBehavior(aCtx);
            if (aCtx.signed_literal() != null) {
                behavior.setDefaultValue(MySQLTableElementFactory.getSignedLiteral(aCtx.signed_literal()));
            }
            alterTableAction.alterColumnBehavior(colRef, behavior);
        } else if (ctx.RENAME() != null) {
            ColumnReference ref = new MySQLColumnRefFactory(ctx.column_definition_ref()).generate();
            alterTableAction.renameColumn(ref, ctx.column_name().getText());
        }
        return alterTableAction;
    }

    @Override
    public AlterTableAction visitAlter_tablegroup_option(Alter_tablegroup_optionContext ctx) {
        AlterTableAction alterTableAction = new AlterTableAction(ctx);
        alterTableAction.setDropTableGroup(true);
        return alterTableAction;
    }

    @Override
    public AlterTableAction visitAlter_index_option(Alter_index_optionContext ctx) {
        AlterTableAction alterTableAction = new AlterTableAction(ctx);
        if (ctx.ADD() != null && ctx.out_of_line_index() != null) {
            alterTableAction
                    .setAddIndex((OutOfLineIndex) new MySQLTableElementFactory(ctx.out_of_line_index()).generate());
        } else if (ctx.DROP() != null) {
            alterTableAction.setDropIndexName(ctx.index_name(0).getText());
        } else if (ctx.ALTER() != null && ctx.INDEX() != null) {
            String idxName = ctx.index_name(0).getText();
            if (ctx.visibility_option() != null) {
                alterTableAction.alterIndexVisibility(idxName, ctx.visibility_option().VISIBLE() != null);
            } else if (ctx.parallel_option().NOPARALLEL() != null) {
                alterTableAction.alterIndexNoParallel(idxName);
            } else {
                alterTableAction.alterIndexParallel(idxName,
                        Integer.parseInt(ctx.parallel_option().INTNUM().getText()));
            }
        } else if (ctx.RENAME() != null) {
            alterTableAction.renameIndex(ctx.index_name(0).getText(), ctx.index_name(1).getText());
        }
        return alterTableAction;
    }

    @Override
    public AlterTableAction visitAlter_partition_option(Alter_partition_optionContext ctx) {
        AlterTableAction alterTableAction = new AlterTableAction(ctx);
        if (ctx.DROP() != null) {
            List<String> names = getNames(ctx.drop_partition_name_list().name_list());
            if (ctx.PARTITION() != null) {
                alterTableAction.setDropPartitionNames(names);
            } else if (ctx.SUBPARTITION() != null) {
                alterTableAction.setDropSubPartitionNames(names);
            }
        } else if (ctx.TRUNCATE() != null) {
            List<String> names = getNames(ctx.name_list());
            if (ctx.PARTITION() != null) {
                alterTableAction.setTruncatePartitionNames(names);
            } else if (ctx.SUBPARTITION() != null) {
                alterTableAction.setTruncateSubPartitionNames(names);
            }
        } else if (ctx.ADD() != null && ctx.opt_partition_range_or_list() != null) {
            alterTableAction.setAddPartitionElements(
                    getPartitionElements(ctx.opt_partition_range_or_list()));
        } else if (ctx.modify_partition_info() != null) {
            Modify_partition_infoContext mCtx = ctx.modify_partition_info();
            if (mCtx.hash_partition_option() != null) {
                alterTableAction.setModifyPartition(
                        new MySQLPartitionFactory(mCtx.hash_partition_option()).generate());
            } else if (mCtx.list_partition_option() != null) {
                alterTableAction.setModifyPartition(
                        new MySQLPartitionFactory(mCtx.list_partition_option()).generate());
            } else if (mCtx.range_partition_option() != null) {
                alterTableAction.setModifyPartition(
                        new MySQLPartitionFactory(mCtx.range_partition_option()).generate());
            } else if (mCtx.key_partition_option() != null) {
                alterTableAction.setModifyPartition(
                        new MySQLPartitionFactory(mCtx.key_partition_option()).generate());
            }
        } else if (ctx.REORGANIZE() != null) {
            alterTableAction.reorganizePartition(getNames(ctx.name_list()),
                    getPartitionElements(ctx.opt_partition_range_or_list()));
        } else if (ctx.REMOVE() != null && ctx.PARTITIONING() != null) {
            alterTableAction.setRemovePartitioning(true);
        }
        return alterTableAction;
    }

    @Override
    public AlterTableAction visitAlter_constraint_option(Alter_constraint_optionContext ctx) {
        AlterTableAction action = new AlterTableAction(ctx);
        if (ctx.ADD() != null && ctx.out_of_line_constraint() != null) {
            action.setAddConstraint(
                    (OutOfLineConstraint) new MySQLTableElementFactory(ctx.out_of_line_constraint()).generate());
        } else if (ctx.DROP() != null) {
            if (ctx.PRIMARY() != null) {
                action.setDropPrimaryKey(true);
            } else if (ctx.FOREIGN() != null) {
                action.setDropForeignKeyName(ctx.index_name().getText());
            } else {
                List<String> names = ctx.name_list() != null
                        ? getNames(ctx.name_list())
                        : Collections.singletonList(ctx.constraint_name().getText());
                action.setDropConstraintNames(names);
            }
        } else {
            ConstraintState state = new ConstraintState(ctx.check_state());
            state.setEnforced(ctx.check_state().NOT() == null);
            action.modifyConstraint(ctx.constraint_name().getText(), state);
        }
        return action;
    }

    @Override
    public AlterTableAction visitAlter_column_group_option(Alter_column_group_optionContext ctx) {
        AlterTableAction action = new AlterTableAction(ctx);
        List<ColumnGroupElement> columnGroupElements = ctx.column_group_list().column_group_element()
                .stream().map(c -> new MySQLColumnGroupElementFactory(c).generate()).collect(Collectors.toList());
        if (ctx.ADD() != null) {
            action.setAddColumnGroupElements(columnGroupElements);
        } else {
            action.setDropColumnGroupElements(columnGroupElements);
        }
        return action;
    }

    private List<String> getNames(Name_listContext context) {
        List<String> list = new ArrayList<>();
        if (context.NAME_OB() != null && context.name_list() == null) {
            list.add(context.NAME_OB().getText());
        } else if (context.NAME_OB() != null && context.name_list() != null) {
            list.addAll(getNames(context.name_list()));
            list.add(context.NAME_OB().getText());
        }
        return list;
    }

    private List<PartitionElement> getPartitionElements(Opt_partition_range_or_listContext pCtx) {
        if (pCtx.opt_range_partition_list() != null) {
            return pCtx.opt_range_partition_list().range_partition_list().range_partition_element().stream()
                    .map(c -> new MySQLPartitionElementFactory(c).generate()).collect(Collectors.toList());
        }
        return pCtx.opt_list_partition_list().list_partition_list().list_partition_element().stream()
                .map(c -> new MySQLPartitionElementFactory(c).generate()).collect(Collectors.toList());
    }

}
