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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Hash_subpartition_elementContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.List_partition_exprContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.List_subpartition_elementContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Partition_attributes_optionContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Range_partition_exprContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Range_subpartition_elementContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.createtable.PartitionOptions;
import com.oceanbase.tools.sqlparser.statement.createtable.SubHashPartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.SubListPartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.SubPartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.SubRangePartitionElement;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;

import lombok.NonNull;

/**
 * {@link MySQLSubPartitionElementFactory}
 *
 * @author yh263208
 * @date 2023-06-01 19:33
 * @since ODC_release_4.2.0
 */
public class MySQLSubPartitionElementFactory extends OBParserBaseVisitor<SubPartitionElement>
        implements StatementFactory<SubPartitionElement> {

    private final ParserRuleContext parserRuleContext;

    public MySQLSubPartitionElementFactory(@NonNull Hash_subpartition_elementContext hashElementContext) {
        this.parserRuleContext = hashElementContext;
    }

    public MySQLSubPartitionElementFactory(@NonNull Range_subpartition_elementContext rangeElementContext) {
        this.parserRuleContext = rangeElementContext;
    }

    public MySQLSubPartitionElementFactory(@NonNull List_subpartition_elementContext listElementContext) {
        this.parserRuleContext = listElementContext;
    }

    @Override
    public SubPartitionElement generate() {
        return visit(this.parserRuleContext);
    }

    @Override
    public SubPartitionElement visitHash_subpartition_element(Hash_subpartition_elementContext ctx) {
        SubHashPartitionElement element = new SubHashPartitionElement(ctx,
                MySQLFromReferenceFactory.getRelation(ctx.relation_factor()));
        element.setSchema(MySQLFromReferenceFactory.getSchemaName(ctx.relation_factor()));
        element.setPartitionOptions(getPartitionOptions(ctx.partition_attributes_option()));
        return element;
    }

    @Override
    public SubPartitionElement visitRange_subpartition_element(Range_subpartition_elementContext ctx) {
        List<Expression> rangeExprs = getRangePartitionExprs(ctx.range_partition_expr());
        SubRangePartitionElement element = new SubRangePartitionElement(ctx,
                MySQLFromReferenceFactory.getRelation(ctx.relation_factor()), rangeExprs);
        element.setSchema(MySQLFromReferenceFactory.getSchemaName(ctx.relation_factor()));
        element.setPartitionOptions(getPartitionOptions(ctx.partition_attributes_option()));
        return element;
    }

    @Override
    public SubPartitionElement visitList_subpartition_element(List_subpartition_elementContext ctx) {
        List<Expression> listExprs = getListPartitionExprs(ctx.list_partition_expr());
        SubListPartitionElement element = new SubListPartitionElement(ctx,
                MySQLFromReferenceFactory.getRelation(ctx.relation_factor()), listExprs);
        element.setSchema(MySQLFromReferenceFactory.getSchemaName(ctx.relation_factor()));
        element.setPartitionOptions(getPartitionOptions(ctx.partition_attributes_option()));
        return element;
    }

    public static PartitionOptions getPartitionOptions(Partition_attributes_optionContext ctx) {
        if (ctx == null) {
            return null;
        }
        PartitionOptions partitionOptions = new PartitionOptions(ctx);
        partitionOptions.setEngine(ctx.INNODB().getText());
        return partitionOptions;
    }

    public static List<Expression> getListPartitionExprs(List_partition_exprContext context) {
        if (context.DEFAULT() != null) {
            return Collections.singletonList(new ConstExpression(context.DEFAULT()));
        }
        return context.list_expr().expr().stream().map(c -> new MySQLExpressionFactory(c).generate())
                .collect(Collectors.toList());
    }

    public static List<Expression> getRangePartitionExprs(Range_partition_exprContext cxt) {
        if (cxt.MAXVALUE() != null) {
            return Collections.singletonList(new ConstExpression(cxt));
        }
        return cxt.range_expr_list().range_expr().stream().map(c -> {
            if (c.MAXVALUE() != null) {
                return new ConstExpression(c);
            }
            return new MySQLExpressionFactory(c.expr()).generate();
        }).collect(Collectors.toList());
    }

}
