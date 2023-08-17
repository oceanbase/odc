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

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Order_byContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Sort_listContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.select.OrderBy;
import com.oceanbase.tools.sqlparser.statement.select.SortKey;

import lombok.NonNull;

/**
 * {@link MySQLOrderByFactory}
 *
 * @author yh263208
 * @date 2022-12-09 16:02
 * @since ODC_release_4.1.0
 * @see StatementFactory
 */
public class MySQLOrderByFactory extends OBParserBaseVisitor<OrderBy> implements StatementFactory<OrderBy> {

    private final Order_byContext orderByContext;

    public MySQLOrderByFactory(@NonNull Order_byContext orderByContext) {
        this.orderByContext = orderByContext;
    }

    @Override
    public OrderBy generate() {
        return visit(this.orderByContext);
    }

    @Override
    public OrderBy visitOrder_by(Order_byContext ctx) {
        Sort_listContext sorts = ctx.sort_list();
        List<SortKey> sortKeyList = sorts.sort_key().stream().map(c -> {
            StatementFactory<SortKey> factory = new MySQLSortKeyFactory(c);
            return factory.generate();
        }).collect(Collectors.toList());
        return new OrderBy(ctx, sortKeyList);
    }

}
