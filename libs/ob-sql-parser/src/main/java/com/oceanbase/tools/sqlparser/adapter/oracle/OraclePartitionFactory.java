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
import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Auto_partition_optionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Auto_range_typeContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Column_partition_optionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Hash_partition_optionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.List_partition_optionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Opt_partition_optionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Range_partition_optionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Subpartition_optionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Vertical_column_nameContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnPartition;
import com.oceanbase.tools.sqlparser.statement.createtable.HashPartition;
import com.oceanbase.tools.sqlparser.statement.createtable.HashPartitionElement;
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
 * {@link OraclePartitionFactory}
 *
 * @author yh263208
 * @date 2023-05-31 15:58
 * @since ODC_release_4.2.0
 */
public class OraclePartitionFactory extends OBParserBaseVisitor<Partition> implements StatementFactory<Partition> {

    private final ParserRuleContext parserRuleContext;

    public OraclePartitionFactory(@NonNull Opt_partition_optionContext optPartitionOptionContext) {
        this.parserRuleContext = optPartitionOptionContext;
    }

    public OraclePartitionFactory(@NonNull Hash_partition_optionContext ctx) {
        this.parserRuleContext = ctx;
    }

    public OraclePartitionFactory(@NonNull Range_partition_optionContext ctx) {
        this.parserRuleContext = ctx;
    }

    public OraclePartitionFactory(@NonNull List_partition_optionContext ctx) {
        this.parserRuleContext = ctx;
    }

    @Override
    public Partition generate() {
        return visit(this.parserRuleContext);
    }

    @Override
    public Partition visitHash_partition_option(Hash_partition_optionContext ctx) {
        List<Expression> targets = ctx.column_name_list().column_name().stream()
                .map(c -> new ColumnReference(c, null, null, c.getText())).collect(Collectors.toList());
        List<HashPartitionElement> partitionElts = null;
        if (ctx.hash_partition_list() != null) {
            partitionElts = ctx.hash_partition_list().hash_partition_element()
                    .stream().map(c -> (HashPartitionElement) new OraclePartitionElementFactory(c).generate())
                    .collect(Collectors.toList());
        }
        Integer num = null;
        if (ctx.INTNUM() != null) {
            num = Integer.valueOf(ctx.INTNUM().getText());
        }
        HashPartition hashPartition = new HashPartition(ctx, targets, partitionElts,
                getSubPartitionOption(ctx.subpartition_option()), num);
        if (ctx.hash_partition_attributes_option_list() != null) {
            hashPartition.setPartitionOptions(OraclePartitionElementFactory
                    .getPartitionOptions(ctx.hash_partition_attributes_option_list()));
        }
        return hashPartition;
    }

    @Override
    public Partition visitRange_partition_option(Range_partition_optionContext ctx) {
        List<Expression> targets = ctx.column_name_list().column_name().stream()
                .map(c -> new ColumnReference(c, null, null, c.getText())).collect(Collectors.toList());
        List<RangePartitionElement> partitionElts = ctx.opt_range_partition_list()
                .range_partition_list().range_partition_element().stream()
                .map(c -> (RangePartitionElement) new OraclePartitionElementFactory(c).generate())
                .collect(Collectors.toList());
        RangePartition partition = new RangePartition(ctx, targets, partitionElts,
                getSubPartitionOption(ctx.subpartition_option()), null, false);
        if (ctx.interval_option() != null) {
            partition.setInterval(new OracleExpressionFactory(ctx.interval_option().bit_expr()).generate());
        }
        return partition;
    }

    @Override
    public Partition visitAuto_partition_option(Auto_partition_optionContext ctx) {
        List<Expression> targets = null;
        Auto_range_typeContext context = ctx.auto_partition_type().auto_range_type();
        if (context.column_name_list() != null) {
            targets = context.column_name_list().column_name().stream()
                    .map(c -> new ColumnReference(c, null, null, c.getText())).collect(Collectors.toList());
        }
        RangePartition rangePartition = new RangePartition(ctx, targets, null, null, null, false);
        rangePartition.setAuto(true);
        rangePartition.setPartitionSize(new ConstExpression(ctx.partition_size()));
        return rangePartition;
    }

    @Override
    public Partition visitList_partition_option(List_partition_optionContext ctx) {
        List<Expression> targets = ctx.column_name_list().column_name().stream()
                .map(c -> new ColumnReference(c, null, null, c.getText())).collect(Collectors.toList());
        List<ListPartitionElement> partitionElts = ctx.opt_list_partition_list()
                .list_partition_list().list_partition_element().stream()
                .map(c -> (ListPartitionElement) new OraclePartitionElementFactory(c).generate())
                .collect(Collectors.toList());
        return new ListPartition(ctx, targets, partitionElts, getSubPartitionOption(ctx.subpartition_option()), null,
                false);
    }

    @Override
    public Partition visitColumn_partition_option(Column_partition_optionContext ctx) {
        List<ColumnReference> targets = getColumnReferences(ctx.vertical_column_name());
        if (ctx.aux_column_list() != null) {
            targets.addAll(ctx.aux_column_list().vertical_column_name().stream()
                    .flatMap(c -> getColumnReferences(c).stream()).collect(Collectors.toList()));
        }
        return new ColumnPartition(ctx, targets);
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

    private SubPartitionOption getSubPartitionOption(Subpartition_optionContext context) {
        return context == null ? null : new OracleSubPartitionOptionFactory(context).generate();
    }

}
