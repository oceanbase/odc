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

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Hash_subpartition_elementContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.List_partition_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.List_subpartition_elementContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Physical_attributes_option_listContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Range_expr_listContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Range_partition_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Range_subpartition_elementContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.Operator;
import com.oceanbase.tools.sqlparser.statement.createtable.PartitionOptions;
import com.oceanbase.tools.sqlparser.statement.createtable.SubHashPartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.SubListPartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.SubPartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.SubRangePartitionElement;
import com.oceanbase.tools.sqlparser.statement.expression.CompoundExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;

import lombok.NonNull;

/**
 * {@link OracleSubPartitionElementFactory}
 *
 * @author yh263208
 * @date 2023-05-31 14:04
 * @since ODC_release_4.2.0
 */
public class OracleSubPartitionElementFactory extends OBParserBaseVisitor<SubPartitionElement>
        implements StatementFactory<SubPartitionElement> {

    private final ParserRuleContext parserRuleContext;

    public OracleSubPartitionElementFactory(@NonNull Hash_subpartition_elementContext hashElementContext) {
        this.parserRuleContext = hashElementContext;
    }

    public OracleSubPartitionElementFactory(@NonNull Range_subpartition_elementContext rangeElementContext) {
        this.parserRuleContext = rangeElementContext;
    }

    public OracleSubPartitionElementFactory(@NonNull List_subpartition_elementContext listElementContext) {
        this.parserRuleContext = listElementContext;
    }

    @Override
    public SubPartitionElement generate() {
        return visit(this.parserRuleContext);
    }

    @Override
    public SubPartitionElement visitHash_subpartition_element(Hash_subpartition_elementContext ctx) {
        SubHashPartitionElement element = new SubHashPartitionElement(ctx,
                OracleFromReferenceFactory.getRelation(ctx.relation_factor()));
        element.setUserVariable(OracleFromReferenceFactory.getUserVariable(ctx.relation_factor()));
        element.setSchema(OracleFromReferenceFactory.getSchemaName(ctx.relation_factor()));
        element.setPartitionOptions(getPartitionOptions(ctx.physical_attributes_option_list()));
        return element;
    }

    @Override
    public SubPartitionElement visitRange_subpartition_element(Range_subpartition_elementContext ctx) {
        List<Expression> rangeExprs = getRangePartitionExprs(ctx.range_partition_expr());
        SubRangePartitionElement element = new SubRangePartitionElement(ctx,
                OracleFromReferenceFactory.getRelation(ctx.relation_factor()), rangeExprs);
        element.setUserVariable(OracleFromReferenceFactory.getUserVariable(ctx.relation_factor()));
        element.setSchema(OracleFromReferenceFactory.getSchemaName(ctx.relation_factor()));
        element.setPartitionOptions(getPartitionOptions(ctx.physical_attributes_option_list()));
        return element;
    }

    @Override
    public SubPartitionElement visitList_subpartition_element(List_subpartition_elementContext ctx) {
        List<Expression> listExprs = getListPartitionExprs(ctx.list_partition_expr());
        SubListPartitionElement element = new SubListPartitionElement(ctx,
                OracleFromReferenceFactory.getRelation(ctx.relation_factor()), listExprs);
        element.setUserVariable(OracleFromReferenceFactory.getUserVariable(ctx.relation_factor()));
        element.setSchema(OracleFromReferenceFactory.getSchemaName(ctx.relation_factor()));
        element.setPartitionOptions(getPartitionOptions(ctx.physical_attributes_option_list()));
        return element;
    }

    public static PartitionOptions getPartitionOptions(Physical_attributes_option_listContext ctx) {
        if (ctx == null || ctx.physical_attributes_option().isEmpty()) {
            return null;
        }
        PartitionOptions partitionOptions = new PartitionOptions(ctx);
        ctx.physical_attributes_option().forEach(c -> {
            Integer num = c.INTNUM() == null ? null : Integer.valueOf(c.INTNUM().getText());
            if (c.PCTFREE() != null) {
                partitionOptions.setPctFree(num);
            } else if (c.PCTUSED() != null) {
                partitionOptions.setPctUsed(num);
            } else if (c.INITRANS() != null) {
                partitionOptions.setIniTrans(num);
            } else if (c.MAXTRANS() != null) {
                partitionOptions.setMaxTrans(num);
            } else if (c.STORAGE() != null) {
                partitionOptions.setStorage(c.storage_options_list().storage_option().stream().map(i -> {
                    CharStream input = i.getStart().getInputStream();
                    return input.getText(Interval.of(i.getStart().getStartIndex(), i.getStop().getStopIndex()));
                }).collect(Collectors.toList()));
            } else if (c.TABLESPACE() != null) {
                partitionOptions.setTableSpace(c.tablespace().getText());
            }
        });
        return partitionOptions;
    }

    public static List<Expression> getListPartitionExprs(List_partition_exprContext context) {
        if (context.DEFAULT() != null) {
            return Collections.singletonList(new ConstExpression(context.DEFAULT()));
        }
        return context.list_expr().bit_expr().stream().map(c -> new OracleExpressionFactory(c).generate())
                .collect(Collectors.toList());
    }

    public static List<Expression> getRangePartitionExprs(Range_partition_exprContext cxt) {
        return getRangePartitionExprs(cxt.range_expr_list());
    }

    public static List<Expression> getRangePartitionExprs(Range_expr_listContext cxt) {
        return cxt.range_expr().stream().map(c -> {
            if (c.access_func_expr() != null) {
                return new OracleExpressionFactory().getFunctionCall(c.access_func_expr());
            } else if (c.MAXVALUE() != null) {
                return new ConstExpression(c);
            }
            Operator operator = null;
            if (c.Plus() != null) {
                operator = Operator.ADD;
            } else if (c.Minus() != null) {
                operator = Operator.SUB;
            }
            if (operator == null) {
                return new ConstExpression(c);
            }
            return new CompoundExpression(c, new ConstExpression(c.literal()), null, operator);
        }).collect(Collectors.toList());
    }

}
