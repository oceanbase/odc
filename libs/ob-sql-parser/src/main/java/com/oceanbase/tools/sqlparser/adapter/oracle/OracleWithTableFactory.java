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

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBParser;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Alias_name_listContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Common_table_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.select.OrderBy;
import com.oceanbase.tools.sqlparser.statement.select.SelectBody;
import com.oceanbase.tools.sqlparser.statement.select.SortKey;
import com.oceanbase.tools.sqlparser.statement.select.WithTable;
import com.oceanbase.tools.sqlparser.statement.select.oracle.Fetch;
import com.oceanbase.tools.sqlparser.statement.select.oracle.SearchMode;
import com.oceanbase.tools.sqlparser.statement.select.oracle.SetValue;

import lombok.NonNull;

/**
 * {@link OracleWithTableFactory}
 *
 * @author yh263208
 * @date 2022-12-07 17:16
 * @since ODC_release_4.1.0
 * @see StatementFactory
 */
public class OracleWithTableFactory extends OBParserBaseVisitor<WithTable> implements StatementFactory<WithTable> {

    private final Common_table_exprContext commonTableExprContext;

    public OracleWithTableFactory(@NonNull Common_table_exprContext commonTableExprContext) {
        this.commonTableExprContext = commonTableExprContext;
    }

    @Override
    public WithTable generate() {
        return visit(this.commonTableExprContext);
    }

    @Override
    public WithTable visitCommon_table_expr(OBParser.Common_table_exprContext ctx) {
        String relationName = ctx.relation_name().getText();
        SelectBody select;
        if (ctx.select_no_parens() != null) {
            StatementFactory<SelectBody> factory = new OracleSelectBodyFactory(ctx.select_no_parens());
            select = factory.generate();
        } else if (ctx.with_select() != null) {
            StatementFactory<SelectBody> factory = new OracleSelectBodyFactory(ctx.with_select());
            select = factory.generate();
        } else if (ctx.select_with_parens() != null) {
            StatementFactory<SelectBody> factory = new OracleSelectBodyFactory(ctx.select_with_parens());
            select = factory.generate();
        } else {
            if (ctx.subquery() == null) {
                throw new IllegalStateException("Missing sub query");
            }
            StatementFactory<SelectBody> factory = new OracleSelectBodyFactory(ctx.subquery());
            select = factory.generate();
            StatementFactory<OrderBy> orderByFactory = new OracleOrderByFactory(ctx.order_by());
            select.setOrderBy(orderByFactory.generate());
            if (ctx.fetch_next_clause() != null) {
                StatementFactory<Fetch> fetchFactory = new OracleFetchFactory(ctx.fetch_next_clause());
                select.setFetch(fetchFactory.generate());
            }
        }
        WithTable withTable = new WithTable(ctx, relationName, select);
        List<Alias_name_listContext> aliasNameList = ctx.alias_name_list();
        if (CollectionUtils.isNotEmpty(aliasNameList)) {
            if (aliasNameList.size() == 1 && ctx.CYCLE() == null) {
                withTable.setAliasList(visitAliasNames(aliasNameList.get(0)));
            } else if (aliasNameList.size() != 1) {
                withTable.setAliasList(visitAliasNames(aliasNameList.get(0)));
                withTable.setCycleAliasList(visitAliasNames(aliasNameList.get(1)));
            } else {
                withTable.setCycleAliasList(visitAliasNames(aliasNameList.get(0)));
            }
        }
        if (ctx.SEARCH() != null) {
            SearchMode mode = ctx.BREADTH() != null ? SearchMode.BREADTH_FIRST : SearchMode.DEPTH_FIRST;
            withTable.setSearchMode(mode);
            if (ctx.sort_list() != null) {
                List<SortKey> sortKeys = ctx.sort_list().sort_key().stream().map(c -> {
                    StatementFactory<SortKey> factory = new OracleSortKeyFactory(c);
                    return factory.generate();
                }).collect(Collectors.toList());
                withTable.setSearchSortKeyList(sortKeys);
            }
            if (ctx.search_set_value() != null) {
                SetValue setValue = new SetValue(ctx.search_set_value().var_name().getText(), null, null);
                withTable.setSearchValueSet(setValue);
            }
        }
        if (ctx.CYCLE() != null) {
            String name = ctx.var_name().getText();
            String value = ctx.STRING_VALUE(0) == null ? null : ctx.STRING_VALUE(0).getText();
            String defaultValue = ctx.STRING_VALUE(1) == null ? null : ctx.STRING_VALUE(1).getText();
            withTable.setCycleValueSet(new SetValue(name, value, defaultValue));
        }
        return withTable;
    }

    private List<String> visitAliasNames(Alias_name_listContext ctx) {
        return ctx.column_alias_name().stream().map(c -> c.column_name().getText()).collect(Collectors.toList());
    }

}
