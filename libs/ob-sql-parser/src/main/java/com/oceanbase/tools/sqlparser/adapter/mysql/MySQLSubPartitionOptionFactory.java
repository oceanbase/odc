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
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Subpartition_individual_optionContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Subpartition_optionContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Subpartition_template_optionContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.createtable.SubPartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.SubPartitionOption;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;

import lombok.NonNull;

/**
 * {@link MySQLSubPartitionOptionFactory}
 *
 * @author yh263208
 * @date 2023-06-01 20:15
 * @since ODC_release_4.2.0
 */
public class MySQLSubPartitionOptionFactory extends OBParserBaseVisitor<SubPartitionOption>
        implements StatementFactory<SubPartitionOption> {

    private final ParserRuleContext parserRuleContext;

    public MySQLSubPartitionOptionFactory(@NonNull Subpartition_optionContext subpartitionOptionContext) {
        this.parserRuleContext = subpartitionOptionContext;
    }

    @Override
    public SubPartitionOption generate() {
        return visit(this.parserRuleContext);
    }

    @Override
    public SubPartitionOption visitSubpartition_template_option(Subpartition_template_optionContext ctx) {
        List<Expression> subPartitionTargets;
        if (ctx.expr() != null) {
            subPartitionTargets = Collections.singletonList(new MySQLExpressionFactory(ctx.expr()).generate());
        } else {
            subPartitionTargets = ctx.column_name_list().column_name().stream()
                    .map(c -> new ColumnReference(c, null, null, c.getText())).collect(Collectors.toList());
        }
        String type = ctx.COLUMNS() == null ? ctx.getChild(2).getText()
                : ctx.getChild(2).getText().toLowerCase() + " " + ctx.COLUMNS().getText().toLowerCase();
        SubPartitionOption subPartitionOption =
                new SubPartitionOption(ctx, subPartitionTargets, type);
        subPartitionOption.setTemplates(getSubPartitionElements(ctx));
        return subPartitionOption;
    }

    @Override
    public SubPartitionOption visitSubpartition_individual_option(Subpartition_individual_optionContext ctx) {
        List<Expression> targets;
        if (ctx.expr() != null) {
            targets = Collections.singletonList(new MySQLExpressionFactory(ctx.expr()).generate());
        } else {
            targets = ctx.column_name_list().column_name().stream()
                    .map(c -> new ColumnReference(c, null, null, c.getText())).collect(Collectors.toList());
        }
        String type = ctx.COLUMNS() == null ? ctx.getChild(2).getText()
                : ctx.getChild(2).getText().toLowerCase() + " " + ctx.COLUMNS().getText().toLowerCase();
        SubPartitionOption option = new SubPartitionOption(ctx, targets, type);
        if (ctx.INTNUM() != null) {
            option.setSubPartitionNum(Integer.valueOf(ctx.INTNUM().getText()));
        }
        return option;
    }

    private List<SubPartitionElement> getSubPartitionElements(Subpartition_template_optionContext context) {
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
