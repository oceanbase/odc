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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Add_range_or_list_partitionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Add_range_or_list_subpartitionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Alter_column_group_optionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Alter_column_optionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Alter_index_optionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Alter_partition_optionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Alter_table_actionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Alter_tablegroup_optionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Modify_partition_infoContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Name_listContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Opt_alter_compress_optionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Opt_special_partition_listContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Relation_factorContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Special_partition_listContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Split_actionsContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Split_list_partitionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Split_range_partitionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTableAction;
import com.oceanbase.tools.sqlparser.statement.alter.table.PartitionSplitActions;
import com.oceanbase.tools.sqlparser.statement.common.ColumnGroupElement;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnDefinition;
import com.oceanbase.tools.sqlparser.statement.createtable.ConstraintState;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.PartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.PartitionOptions;
import com.oceanbase.tools.sqlparser.statement.createtable.SpecialPartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.SubPartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.TableOptions;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;

import lombok.NonNull;

/**
 * {@link OracleAlterTableActionFactory}
 *
 * @author yh263208
 * @date 2023-06-14 16:47
 * @since ODC_release_4.2.0
 */
public class OracleAlterTableActionFactory extends OBParserBaseVisitor<AlterTableAction>
        implements StatementFactory<AlterTableAction> {

    private final ParserRuleContext parserRuleContext;

    public OracleAlterTableActionFactory(@NonNull Alter_table_actionContext alterTableActionContext) {
        this.parserRuleContext = alterTableActionContext;
    }

    public OracleAlterTableActionFactory(@NonNull Alter_column_group_optionContext alterColumnGroupOptionContext) {
        this.parserRuleContext = alterColumnGroupOptionContext;
    }

    @Override
    public AlterTableAction generate() {
        return visit(this.parserRuleContext);
    }

    @Override
    public AlterTableAction visitAlter_table_action(Alter_table_actionContext ctx) {
        AlterTableAction alterTableAction = new AlterTableAction(ctx);
        if (ctx.table_option_list_space_seperated() != null) {
            StatementFactory<TableOptions> factory = new OracleTableOptionsFactory(
                    ctx.table_option_list_space_seperated());
            alterTableAction.setTableOptions(factory.generate());
            return alterTableAction;
        } else if (ctx.RENAME() != null) {
            alterTableAction.setRenameToTable(getRelationFactor(ctx.relation_factor()));
            return alterTableAction;
        } else if (ctx.REFRESH() != null) {
            alterTableAction.setRefresh(true);
            return alterTableAction;
        } else if (ctx.DROP() != null && ctx.CONSTRAINT() != null) {
            alterTableAction.setDropConstraintNames(Collections.singletonList(ctx.constraint_name().getText()));
            return alterTableAction;
        } else if (ctx.SET() != null && ctx.INTERVAL() != null) {
            if (ctx.bit_expr() != null) {
                alterTableAction.setInterval(new OracleExpressionFactory(ctx.bit_expr()).generate());
            }
            return alterTableAction;
        } else if (ctx.enable_option() != null && ctx.ALL() != null && ctx.TRIGGERS() != null) {
            alterTableAction.setEnableAllTriggers(ctx.enable_option().ENABLE() != null);
            return alterTableAction;
        }
        return visitChildren(ctx);
    }

    @Override
    public AlterTableAction visitOpt_alter_compress_option(Opt_alter_compress_optionContext ctx) {
        AlterTableAction alterTableAction = new AlterTableAction(ctx);
        if (ctx.compress_option().NOCOMPRESS() != null) {
            alterTableAction.setMoveNoCompress(true);
            return alterTableAction;
        }
        CharStream input = ctx.getStart().getInputStream();
        String str = input.getText(Interval.of(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex()));
        int index = str.indexOf(ctx.compress_option().COMPRESS().getText());
        if (index >= 0) {
            str = str.substring(index + ctx.compress_option().COMPRESS().getText().length()).trim();
        }
        alterTableAction.setMoveCompress(str);
        return alterTableAction;
    }

    @Override
    public AlterTableAction visitAlter_column_option(Alter_column_optionContext ctx) {
        AlterTableAction alterTableAction = new AlterTableAction(ctx);
        if (ctx.ADD() != null) {
            List<ColumnDefinition> addColumns = new ArrayList<>();
            if (ctx.column_definition() != null) {
                addColumns.add((ColumnDefinition) new OracleTableElementFactory(ctx.column_definition()).generate());
            } else {
                addColumns = ctx.column_definition_list().column_definition().stream()
                        .map(c -> (ColumnDefinition) new OracleTableElementFactory(c).generate())
                        .collect(Collectors.toList());
            }
            alterTableAction.setAddColumns(addColumns);
        } else if (ctx.DROP() != null) {
            if (ctx.column_definition_ref() != null) {
                String option = null;
                if (ctx.CASCADE() != null) {
                    option = ctx.CASCADE().getText();
                } else if (ctx.RESTRICT() != null) {
                    option = ctx.RESTRICT().getText();
                }
                alterTableAction.setDropColumn(
                        new OracleColumnRefFactory(ctx.column_definition_ref()).generate(), option);
            } else {
                List<ColumnReference> columns = ctx.column_list().column_definition_ref().stream()
                        .map(c -> new OracleColumnRefFactory(c).generate()).collect(Collectors.toList());
                alterTableAction.setDropColumns(columns);
            }
        } else if (ctx.MODIFY() != null) {
            List<ColumnDefinition> columns = new ArrayList<>();
            if (ctx.column_definition_opt_datatype() != null) {
                columns.add((ColumnDefinition) new OracleTableElementFactory(
                        ctx.column_definition_opt_datatype()).generate());
            } else {
                columns = ctx.column_definition_opt_datatype_list().column_definition_opt_datatype().stream().map(
                        c -> (ColumnDefinition) new OracleTableElementFactory(c).generate())
                        .collect(Collectors.toList());
            }
            alterTableAction.setModifyColumns(columns);
        } else if (ctx.RENAME() != null && ctx.COLUMN() != null) {
            ColumnReference ref = new OracleColumnRefFactory(ctx.column_definition_ref()).generate();
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
        AlterTableAction action = new AlterTableAction(ctx);
        if (ctx.ADD() != null && ctx.out_of_line_constraint() != null) {
            action.setAddConstraint(
                    (OutOfLineConstraint) new OracleTableElementFactory(ctx.out_of_line_constraint()).generate());
        } else if (ctx.ALTER() != null && ctx.INDEX() != null) {
            action.alterIndexVisibility(ctx.index_name().getText(), ctx.visibility_option().VISIBLE() != null);
        } else if (ctx.DROP() != null && ctx.PRIMARY() != null && ctx.KEY() != null) {
            action.setDropPrimaryKey(true);
        } else if (ctx.MODIFY() != null && ctx.out_of_line_primary_index() != null) {
            action.setModifyPrimaryKey(
                    (OutOfLineConstraint) new OracleTableElementFactory(ctx.out_of_line_primary_index()).generate());
        } else {
            ConstraintState constraintState = new ConstraintState(ctx);
            if (ctx.RELY() != null || ctx.NORELY() != null) {
                constraintState.setRely(ctx.RELY() != null);
            }
            if (ctx.enable_option() != null) {
                constraintState.setEnable(ctx.enable_option().ENABLE() != null);
            }
            if (ctx.VALIDATE() != null || ctx.NOVALIDATE() != null) {
                constraintState.setValidate(ctx.VALIDATE() != null);
            }
            action.modifyConstraint(ctx.constraint_name().getText(), constraintState);
        }
        return action;
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
            if (ctx.UPDATE() != null && ctx.GLOBAL() != null && ctx.INDEXES() != null) {
                alterTableAction.setUpdateGlobalIndexes(true);
            }
        } else if (ctx.TRUNCATE() != null) {
            List<String> names = getNames(ctx.name_list());
            if (ctx.PARTITION() != null) {
                alterTableAction.setTruncatePartitionNames(names);
            } else if (ctx.SUBPARTITION() != null) {
                alterTableAction.setTruncateSubPartitionNames(names);
            }
            if (ctx.UPDATE() != null && ctx.GLOBAL() != null && ctx.INDEXES() != null) {
                alterTableAction.setUpdateGlobalIndexes(true);
            }
        } else if (ctx.MODIFY() != null && ctx.PARTITION() != null) {
            Add_range_or_list_subpartitionContext pCtx = ctx.add_range_or_list_subpartition();
            List<SubPartitionElement> subElts;
            if (pCtx.range_subpartition_list() != null) {
                subElts = pCtx.range_subpartition_list().range_subpartition_element().stream()
                        .map(c -> new OracleSubPartitionElementFactory(c).generate())
                        .collect(Collectors.toList());
            } else {
                subElts = pCtx.list_subpartition_list().list_subpartition_element().stream()
                        .map(c -> new OracleSubPartitionElementFactory(c).generate())
                        .collect(Collectors.toList());
            }
            alterTableAction.addSubpartitionElements(getRelationFactor(ctx.relation_factor()), subElts);
        } else if (ctx.add_range_or_list_partition() != null) {
            Add_range_or_list_partitionContext pCtx = ctx.add_range_or_list_partition();
            List<PartitionElement> elts;
            if (pCtx.range_partition_list() != null) {
                elts = pCtx.range_partition_list().range_partition_element().stream()
                        .map(c -> new OraclePartitionElementFactory(c).generate())
                        .collect(Collectors.toList());
            } else {
                elts = pCtx.list_partition_list().list_partition_element().stream()
                        .map(c -> new OraclePartitionElementFactory(c).generate())
                        .collect(Collectors.toList());
            }
            alterTableAction.setAddPartitionElements(elts);
        } else if (ctx.SPLIT() != null && ctx.PARTITION() != null) {
            PartitionSplitActions actions = new PartitionSplitActions(ctx.split_actions());
            Split_actionsContext context = ctx.split_actions();
            if (context.list_expr() != null) {
                actions.setListExprs(context.list_expr().bit_expr().stream()
                        .map(c -> new OracleExpressionFactory(c).generate())
                        .collect(Collectors.toList()));
            } else if (context.range_expr_list() != null) {
                actions.setRangeExprs(OracleSubPartitionElementFactory.getRangePartitionExprs(
                        context.range_expr_list()));
            }
            if (context.modify_special_partition() != null
                    && context.modify_special_partition().opt_special_partition_list() != null) {
                Opt_special_partition_listContext oCtx = context
                        .modify_special_partition().opt_special_partition_list();
                actions.setIntos(getSpecialPartitionElement(oCtx.special_partition_list()));
            } else if (context.split_range_partition() != null) {
                Split_range_partitionContext sCtx = context.split_range_partition();
                List<PartitionElement> elts;
                if (sCtx.opt_range_partition_list() != null) {
                    elts = sCtx.opt_range_partition_list().range_partition_list().range_partition_element().stream()
                            .map(c -> new OraclePartitionElementFactory(c).generate()).collect(Collectors.toList());
                } else {
                    elts = sCtx.range_partition_list().range_partition_element().stream()
                            .map(c -> new OraclePartitionElementFactory(c).generate()).collect(Collectors.toList());
                    elts.addAll(getSpecialPartitionElement(sCtx.special_partition_list()));
                }
                actions.setIntos(elts);
            } else if (context.split_list_partition() != null) {
                Split_list_partitionContext sCtx = context.split_list_partition();
                List<PartitionElement> elts;
                if (sCtx.opt_list_partition_list() != null) {
                    elts = sCtx.opt_list_partition_list().list_partition_list().list_partition_element().stream()
                            .map(c -> new OraclePartitionElementFactory(c).generate()).collect(Collectors.toList());
                } else {
                    elts = sCtx.list_partition_list().list_partition_element().stream()
                            .map(c -> new OraclePartitionElementFactory(c).generate()).collect(Collectors.toList());
                    elts.addAll(getSpecialPartitionElement(sCtx.special_partition_list()));
                }
                actions.setIntos(elts);
            }
            alterTableAction.splitPartition(getRelationFactor(ctx.relation_factor()), actions);
        } else if (ctx.RENAME() != null) {
            String from = ctx.relation_name(0).getText();
            String to = ctx.relation_name(1).getText();
            if (ctx.PARTITION() != null) {
                alterTableAction.renamePartition(from, to);
            } else {
                alterTableAction.renameSubPartition(from, to);
            }
        }
        return alterTableAction;
    }

    @Override
    public AlterTableAction visitModify_partition_info(Modify_partition_infoContext ctx) {
        AlterTableAction alterTableAction = new AlterTableAction(ctx);
        if (ctx.hash_partition_option() != null) {
            alterTableAction.setModifyPartition(new OraclePartitionFactory(ctx.hash_partition_option()).generate());
        } else if (ctx.list_partition_option() != null) {
            alterTableAction.setModifyPartition(new OraclePartitionFactory(ctx.list_partition_option()).generate());
        } else if (ctx.range_partition_option() != null) {
            alterTableAction.setModifyPartition(new OraclePartitionFactory(ctx.range_partition_option()).generate());
        }
        return alterTableAction;
    }

    @Override
    public AlterTableAction visitAlter_column_group_option(Alter_column_group_optionContext ctx) {
        AlterTableAction action = new AlterTableAction(ctx);
        List<ColumnGroupElement> columnGroupElements = ctx.column_group_list().column_group_element()
                .stream().map(c -> new OracleColumnGroupElementFactory(c).generate()).collect(Collectors.toList());
        if (ctx.ADD() != null) {
            action.setAddColumnGroupElements(columnGroupElements);
        } else {
            action.setDropColumnGroupElements(columnGroupElements);
        }
        return action;
    }

    private RelationFactor getRelationFactor(Relation_factorContext ctx) {
        RelationFactor relationFactor = new RelationFactor(ctx, OracleFromReferenceFactory.getRelation(ctx));
        relationFactor.setSchema(OracleFromReferenceFactory.getSchemaName(ctx));
        relationFactor.setUserVariable(OracleFromReferenceFactory.getUserVariable(ctx));
        if (ctx.normal_relation_factor() != null
                && ctx.normal_relation_factor().opt_reverse_link_flag() != null
                && ctx.normal_relation_factor().opt_reverse_link_flag().Not() != null) {
            relationFactor.setReverseLink(true);
        }
        return relationFactor;
    }

    private List<String> getNames(Name_listContext context) {
        List<String> list = new ArrayList<>();
        if (context.relation_name() != null && context.name_list() == null) {
            list.add(context.relation_name().getText());
        } else if (context.relation_name() != null && context.name_list() != null) {
            list.addAll(getNames(context.name_list()));
            list.add(context.relation_name().getText());
        }
        return list;
    }

    private List<PartitionElement> getSpecialPartitionElement(Special_partition_listContext ctx) {
        return ctx.special_partition_define().stream().map(c -> {
            SpecialPartitionElement e = new SpecialPartitionElement(c,
                    OracleFromReferenceFactory.getRelation(c.relation_factor()));
            if (c.INTNUM() != null && c.ID() != null) {
                PartitionOptions options = new PartitionOptions(c.ID());
                options.setId(Integer.valueOf(c.INTNUM().getText()));
                e.setPartitionOptions(options);
            }
            e.setSchema(OracleFromReferenceFactory.getSchemaName(c.relation_factor()));
            e.setUserVariable(OracleFromReferenceFactory.getUserVariable(c.relation_factor()));
            return e;
        }).collect(Collectors.toList());
    }

}
