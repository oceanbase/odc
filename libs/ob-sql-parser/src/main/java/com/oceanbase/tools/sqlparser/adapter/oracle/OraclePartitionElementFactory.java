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

import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Compress_optionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Hash_partition_attributes_option_listContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Hash_partition_elementContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.List_partition_elementContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Partition_attributes_option_listContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Range_partition_elementContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Subpartition_listContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.createtable.HashPartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.ListPartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.PartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.PartitionOptions;
import com.oceanbase.tools.sqlparser.statement.createtable.RangePartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.SubPartitionElement;

import lombok.NonNull;

/**
 * {@link OraclePartitionElementFactory}
 *
 * @author yh263208
 * @date 2023-05-31 14:54
 * @since ODC_release_4.2.0
 */
public class OraclePartitionElementFactory extends OBParserBaseVisitor<PartitionElement>
        implements StatementFactory<PartitionElement> {

    private final ParserRuleContext parserRuleContext;

    public OraclePartitionElementFactory(@NonNull Hash_partition_elementContext hashElementContext) {
        this.parserRuleContext = hashElementContext;
    }

    public OraclePartitionElementFactory(@NonNull Range_partition_elementContext rangeElementContext) {
        this.parserRuleContext = rangeElementContext;
    }

    public OraclePartitionElementFactory(@NonNull List_partition_elementContext listElementContext) {
        this.parserRuleContext = listElementContext;
    }

    @Override
    public PartitionElement generate() {
        return visit(this.parserRuleContext);
    }

    @Override
    public PartitionElement visitHash_partition_element(Hash_partition_elementContext ctx) {
        HashPartitionElement element = new HashPartitionElement(ctx,
                OracleFromReferenceFactory.getRelation(ctx.relation_factor()));
        element.setSchema(OracleFromReferenceFactory.getSchemaName(ctx.relation_factor()));
        element.setUserVariable(OracleFromReferenceFactory.getUserVariable(ctx.relation_factor()));
        PartitionOptions options = getPartitionOptions(ctx.hash_partition_attributes_option_list());
        if (ctx.INTNUM() != null && options == null) {
            options = new PartitionOptions(ctx.ID());
            options.setId(Integer.valueOf(ctx.INTNUM().getText()));
        } else if (ctx.INTNUM() != null && options != null) {
            options.setId(Integer.valueOf(ctx.INTNUM().getText()));
        }
        element.setPartitionOptions(options);
        element.setSubPartitionElements(getSubPartitionElements(ctx.subpartition_list()));
        return element;
    }

    @Override
    public PartitionElement visitRange_partition_element(Range_partition_elementContext ctx) {
        List<Expression> rangeExprs = OracleSubPartitionElementFactory.getRangePartitionExprs(
                ctx.range_partition_expr());
        RangePartitionElement element = new RangePartitionElement(ctx,
                OracleFromReferenceFactory.getRelation(ctx.relation_factor()), rangeExprs);
        element.setSchema(OracleFromReferenceFactory.getSchemaName(ctx.relation_factor()));
        element.setUserVariable(OracleFromReferenceFactory.getUserVariable(ctx.relation_factor()));
        PartitionOptions options = getPartitionOptions(ctx.partition_attributes_option_list());
        if (ctx.INTNUM() != null && options == null) {
            options = new PartitionOptions(ctx.ID());
            options.setId(Integer.valueOf(ctx.INTNUM().getText()));
        } else if (ctx.INTNUM() != null && options != null) {
            options.setId(Integer.valueOf(ctx.INTNUM().getText()));
        }
        element.setPartitionOptions(options);
        element.setSubPartitionElements(getSubPartitionElements(ctx.subpartition_list()));
        return element;
    }

    @Override
    public PartitionElement visitList_partition_element(List_partition_elementContext ctx) {
        List<Expression> listExprs = OracleSubPartitionElementFactory
                .getListPartitionExprs(ctx.list_partition_expr());
        ListPartitionElement element = new ListPartitionElement(ctx,
                OracleFromReferenceFactory.getRelation(ctx.relation_factor()), listExprs);
        element.setSchema(OracleFromReferenceFactory.getSchemaName(ctx.relation_factor()));
        element.setUserVariable(OracleFromReferenceFactory.getUserVariable(ctx.relation_factor()));
        PartitionOptions options = getPartitionOptions(ctx.partition_attributes_option_list());
        if (ctx.INTNUM() != null && options == null) {
            options = new PartitionOptions(ctx.ID());
            options.setId(Integer.valueOf(ctx.INTNUM().getText()));
        } else if (ctx.INTNUM() != null && options != null) {
            options.setId(Integer.valueOf(ctx.INTNUM().getText()));
        }
        element.setPartitionOptions(options);
        element.setSubPartitionElements(getSubPartitionElements(ctx.subpartition_list()));
        return element;
    }

    private List<SubPartitionElement> getSubPartitionElements(Subpartition_listContext context) {
        if (context == null) {
            return null;
        }
        if (context.opt_hash_subpartition_list() != null) {
            return context.opt_hash_subpartition_list().hash_subpartition_list().hash_subpartition_element()
                    .stream().map(c -> new OracleSubPartitionElementFactory(c).generate()).collect(Collectors.toList());
        } else if (context.opt_range_subpartition_list() != null) {
            return context.opt_range_subpartition_list().range_subpartition_list().range_subpartition_element()
                    .stream().map(c -> new OracleSubPartitionElementFactory(c).generate()).collect(Collectors.toList());
        }
        return context.opt_list_subpartition_list().list_subpartition_list().list_subpartition_element()
                .stream().map(c -> new OracleSubPartitionElementFactory(c).generate()).collect(Collectors.toList());
    }

    public static PartitionOptions getPartitionOptions(Hash_partition_attributes_option_listContext ctx) {
        if (ctx == null) {
            return null;
        }
        PartitionOptions partitionOptions = new PartitionOptions(ctx);
        if (ctx.TABLESPACE() != null) {
            partitionOptions.setTableSpace(ctx.tablespace().getText());
        }
        if (ctx.compress_option() != null) {
            partitionOptions.merge(getPartitionOptions(ctx.compress_option()));
        }
        return partitionOptions;
    }

    public static PartitionOptions getPartitionOptions(Compress_optionContext ctx) {
        if (ctx == null) {
            return null;
        }
        PartitionOptions partitionOptions = new PartitionOptions(ctx);
        if (ctx.NOCOMPRESS() != null) {
            partitionOptions.setNoCompress(true);
            return partitionOptions;
        }
        CharStream input = ctx.getStart().getInputStream();
        String str = input.getText(Interval.of(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex()));
        int index = str.indexOf(ctx.COMPRESS().getText());
        if (index >= 0) {
            str = str.substring(index + ctx.COMPRESS().getText().length()).trim();
        }
        partitionOptions.setCompress(str);
        return partitionOptions;
    }

    public static PartitionOptions getPartitionOptions(Partition_attributes_option_listContext ctx) {
        if (ctx == null) {
            return null;
        }
        PartitionOptions partitionOptions = new PartitionOptions(ctx);
        if (ctx.physical_attributes_option_list() != null) {
            partitionOptions.merge(OracleSubPartitionElementFactory.getPartitionOptions(
                    ctx.physical_attributes_option_list()));
        }
        if (ctx.compress_option() != null) {
            partitionOptions.merge(getPartitionOptions(ctx.compress_option()));
        }
        return partitionOptions;
    }

}
