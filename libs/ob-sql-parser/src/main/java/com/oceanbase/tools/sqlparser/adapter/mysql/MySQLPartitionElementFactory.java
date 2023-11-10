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

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Hash_partition_elementContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.List_partition_elementContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Range_partition_elementContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Subpartition_listContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;
import com.oceanbase.tools.sqlparser.statement.createtable.HashPartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.ListPartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.PartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.PartitionOptions;
import com.oceanbase.tools.sqlparser.statement.createtable.RangePartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.SubPartitionElement;

import lombok.NonNull;

/**
 * {@link MySQLPartitionElementFactory}
 *
 * @author yh263208
 * @date 2023-06-01 19:52
 * @since ODC_release_4.2.0
 */
public class MySQLPartitionElementFactory extends OBParserBaseVisitor<PartitionElement>
        implements StatementFactory<PartitionElement> {

    private final ParserRuleContext parserRuleContext;

    public MySQLPartitionElementFactory(@NonNull Hash_partition_elementContext hashPartitionElementContext) {
        this.parserRuleContext = hashPartitionElementContext;
    }

    public MySQLPartitionElementFactory(@NonNull Range_partition_elementContext rangePartitionElementContext) {
        this.parserRuleContext = rangePartitionElementContext;
    }

    public MySQLPartitionElementFactory(@NonNull List_partition_elementContext listPartitionElementContext) {
        this.parserRuleContext = listPartitionElementContext;
    }

    @Override
    public PartitionElement generate() {
        return visit(this.parserRuleContext);
    }

    @Override
    public PartitionElement visitHash_partition_element(Hash_partition_elementContext ctx) {
        RelationFactor factor = MySQLFromReferenceFactory.getRelationFactor(ctx.relation_factor());
        HashPartitionElement element = new HashPartitionElement(ctx, factor.getRelation());
        element.setSchema(factor.getSchema());
        element.setUserVariable(factor.getUserVariable());
        element.setSubPartitionElements(getSubPartitionElements(ctx.subpartition_list()));
        PartitionOptions options =
                MySQLSubPartitionElementFactory.getPartitionOptions(ctx.partition_attributes_option());
        if (ctx.INTNUM() != null && options == null) {
            options = new PartitionOptions(ctx.ID());
            options.setId(Integer.valueOf(ctx.INTNUM().getText()));
        } else if (ctx.INTNUM() != null && options != null) {
            options.setId(Integer.valueOf(ctx.INTNUM().getText()));
        }
        element.setPartitionOptions(options);
        return element;
    }

    @Override
    public PartitionElement visitRange_partition_element(Range_partition_elementContext ctx) {
        RelationFactor factor = MySQLFromReferenceFactory.getRelationFactor(ctx.relation_factor());
        List<Expression> rangeExprs =
                MySQLSubPartitionElementFactory.getRangePartitionExprs(ctx.range_partition_expr());
        RangePartitionElement element = new RangePartitionElement(ctx, factor.getRelation(), rangeExprs);
        element.setSchema(factor.getSchema());
        element.setUserVariable(factor.getUserVariable());
        element.setSubPartitionElements(getSubPartitionElements(ctx.subpartition_list()));
        PartitionOptions options =
                MySQLSubPartitionElementFactory.getPartitionOptions(ctx.partition_attributes_option());
        if (ctx.INTNUM() != null && options == null) {
            options = new PartitionOptions(ctx.ID());
            options.setId(Integer.valueOf(ctx.INTNUM().getText()));
        } else if (ctx.INTNUM() != null && options != null) {
            options.setId(Integer.valueOf(ctx.INTNUM().getText()));
        }
        element.setPartitionOptions(options);
        return element;
    }

    @Override
    public PartitionElement visitList_partition_element(List_partition_elementContext ctx) {
        List<Expression> listExprs = MySQLSubPartitionElementFactory.getListPartitionExprs(ctx.list_partition_expr());
        RelationFactor factor = MySQLFromReferenceFactory.getRelationFactor(ctx.relation_factor());
        ListPartitionElement element = new ListPartitionElement(ctx, factor.getRelation(), listExprs);
        element.setSchema(factor.getSchema());
        element.setUserVariable(factor.getUserVariable());
        element.setSubPartitionElements(getSubPartitionElements(ctx.subpartition_list()));
        PartitionOptions options =
                MySQLSubPartitionElementFactory.getPartitionOptions(ctx.partition_attributes_option());
        if (ctx.INTNUM() != null && options == null) {
            options = new PartitionOptions(ctx.ID());
            options.setId(Integer.valueOf(ctx.INTNUM().getText()));
        } else if (ctx.INTNUM() != null && options != null) {
            options.setId(Integer.valueOf(ctx.INTNUM().getText()));
        }
        element.setPartitionOptions(options);
        return element;
    }

    private List<SubPartitionElement> getSubPartitionElements(Subpartition_listContext context) {
        if (context == null) {
            return null;
        }
        if (context.opt_hash_subpartition_list() != null) {
            return context.opt_hash_subpartition_list().hash_subpartition_list().hash_subpartition_element()
                    .stream().map(c -> new MySQLSubPartitionElementFactory(c).generate()).collect(Collectors.toList());
        } else if (context.opt_range_subpartition_list() != null) {
            return context.opt_range_subpartition_list().range_subpartition_list().range_subpartition_element()
                    .stream().map(c -> new MySQLSubPartitionElementFactory(c).generate()).collect(Collectors.toList());
        }
        return context.opt_list_subpartition_list().list_subpartition_list().list_subpartition_element()
                .stream().map(c -> new MySQLSubPartitionElementFactory(c).generate()).collect(Collectors.toList());
    }

}
