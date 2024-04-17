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
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Auto_partition_optionContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Auto_range_typeContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Hash_partition_optionContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Key_partition_optionContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.List_partition_optionContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Partition_optionContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Partition_optionsContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Range_partition_optionContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Vertical_column_nameContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.createtable.HashPartition;
import com.oceanbase.tools.sqlparser.statement.createtable.HashPartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.KeyPartition;
import com.oceanbase.tools.sqlparser.statement.createtable.ListPartition;
import com.oceanbase.tools.sqlparser.statement.createtable.ListPartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.Partition;
import com.oceanbase.tools.sqlparser.statement.createtable.RangePartition;
import com.oceanbase.tools.sqlparser.statement.createtable.RangePartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.SubPartitionOption;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;

import lombok.NonNull;

/**
 * {@link MySQLPartitionFactory}
 *
 * @author yh263208
 * @date 2023-05-31 15:58
 * @since ODC_release_4.2.0
 */
public class MySQLPartitionFactory extends OBParserBaseVisitor<Partition> implements StatementFactory<Partition> {

    private final ParserRuleContext parserRuleContext;

    public MySQLPartitionFactory(@NonNull Partition_optionContext partitionOptionContext) {
        this.parserRuleContext = partitionOptionContext;
    }

    public MySQLPartitionFactory(@NonNull Auto_partition_optionContext autoPartitionOptionContext) {
        this.parserRuleContext = autoPartitionOptionContext;
    }

    public MySQLPartitionFactory(@NonNull Hash_partition_optionContext context) {
        this.parserRuleContext = context;
    }

    public MySQLPartitionFactory(@NonNull List_partition_optionContext context) {
        this.parserRuleContext = context;
    }

    public MySQLPartitionFactory(@NonNull Key_partition_optionContext context) {
        this.parserRuleContext = context;
    }

    public MySQLPartitionFactory(@NonNull Range_partition_optionContext context) {
        this.parserRuleContext = context;
    }

    @Override
    public Partition generate() {
        return visit(this.parserRuleContext);
    }

    @Override
    public Partition visitHash_partition_option(Hash_partition_optionContext ctx) {
        List<Expression> targets = Collections.singletonList(new MySQLExpressionFactory(ctx.expr()).generate());
        List<HashPartitionElement> partitionElts = null;
        if (ctx.opt_hash_partition_list() != null) {
            partitionElts = ctx.opt_hash_partition_list().hash_partition_list().hash_partition_element()
                    .stream().map(c -> (HashPartitionElement) new MySQLPartitionElementFactory(c).generate())
                    .collect(Collectors.toList());
        }
        return new HashPartition(ctx, targets, partitionElts, getSubPartitionOption(ctx.partition_options()),
                getPartitionNum(ctx.partition_options()));
    }

    @Override
    public Partition visitList_partition_option(List_partition_optionContext ctx) {
        List<Expression> targets;
        if (ctx.expr() != null) {
            targets = Collections.singletonList(new MySQLExpressionFactory(ctx.expr()).generate());
        } else {
            targets = ctx.column_name_list().column_name().stream()
                    .map(c -> new ColumnReference(c, null, null, c.getText())).collect(Collectors.toList());
        }
        List<ListPartitionElement> partitionElts = ctx.opt_list_partition_list()
                .list_partition_list().list_partition_element().stream()
                .map(c -> (ListPartitionElement) new MySQLPartitionElementFactory(c).generate())
                .collect(Collectors.toList());
        return new ListPartition(ctx, targets, partitionElts, getSubPartitionOption(ctx.partition_options()),
                getPartitionNum(ctx.partition_options()), ctx.COLUMNS() != null);
    }

    @Override
    public Partition visitKey_partition_option(Key_partition_optionContext ctx) {
        List<ColumnReference> targets = null;
        if (ctx.column_name_list() != null) {
            targets = ctx.column_name_list().column_name().stream()
                    .map(c -> new ColumnReference(c, null, null, c.getText())).collect(Collectors.toList());
        }
        List<HashPartitionElement> partitionElts = null;
        if (ctx.opt_hash_partition_list() != null) {
            partitionElts = ctx.opt_hash_partition_list().hash_partition_list().hash_partition_element()
                    .stream().map(c -> (HashPartitionElement) new MySQLPartitionElementFactory(c).generate())
                    .collect(Collectors.toList());
        }
        return new KeyPartition(ctx, targets, partitionElts, getSubPartitionOption(ctx.partition_options()),
                getPartitionNum(ctx.partition_options()));
    }

    @Override
    public Partition visitRange_partition_option(Range_partition_optionContext ctx) {
        List<Expression> targets;
        if (ctx.expr() != null) {
            targets = Collections.singletonList(new MySQLExpressionFactory(ctx.expr()).generate());
        } else {
            targets = ctx.column_name_list().column_name().stream()
                    .map(c -> new ColumnReference(c, null, null, c.getText())).collect(Collectors.toList());
        }
        List<RangePartitionElement> partitionElts = ctx.opt_range_partition_list()
                .range_partition_list().range_partition_element().stream()
                .map(c -> (RangePartitionElement) new MySQLPartitionElementFactory(c).generate())
                .collect(Collectors.toList());
        return new RangePartition(ctx, targets, partitionElts, getSubPartitionOption(ctx.partition_options()),
                getPartitionNum(ctx.partition_options()), ctx.COLUMNS() != null);
    }

    @Override
    public Partition visitAuto_partition_option(Auto_partition_optionContext ctx) {
        Auto_range_typeContext context = ctx.auto_partition_type().auto_range_type();
        List<Expression> targets = null;
        if (context.column_name_list() != null) {
            targets = context.column_name_list().column_name().stream()
                    .map(c -> new ColumnReference(c, null, null, c.getText())).collect(Collectors.toList());
        } else if (context.expr() != null) {
            targets = Collections.singletonList(new MySQLExpressionFactory(context.expr()).generate());
        }
        RangePartition rangePartition = new RangePartition(ctx, targets, null, null, null, context.COLUMNS() != null);
        rangePartition.setAuto(true);
        rangePartition.setPartitionSize(new ConstExpression(ctx.partition_size()));
        return rangePartition;
    }

    private List<ColumnReference> getColumnReferences(Vertical_column_nameContext cxt) {
        List<ColumnReference> list = new ArrayList<>();
        if (cxt.column_name() != null) {
            list.add(new ColumnReference(cxt.column_name(), null, null, cxt.column_name().getText()));
            return list;
        }
        return cxt.column_name_list().column_name().stream()
                .map(c -> new ColumnReference(c, null, null, c.getText())).collect(Collectors.toList());
    }

    private SubPartitionOption getSubPartitionOption(Partition_optionsContext context) {
        if (context == null) {
            return null;
        }
        return context.subpartition_option() == null ? null
                : new MySQLSubPartitionOptionFactory(
                        context.subpartition_option()).generate();
    }

    private Integer getPartitionNum(Partition_optionsContext context) {
        if (context == null) {
            return null;
        }
        return context.partition_num() == null ? null : Integer.valueOf(context.partition_num().INTNUM().getText());
    }

}
