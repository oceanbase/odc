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
package com.oceanbase.tools.sqlparser.adapter;

import java.util.Arrays;
import java.util.Collections;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLOrderByFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Order_byContext;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.select.OrderBy;
import com.oceanbase.tools.sqlparser.statement.select.SortDirection;
import com.oceanbase.tools.sqlparser.statement.select.SortKey;

/**
 * {@link MySQLOrderByFactoryTest}
 *
 * @author yh263208
 * @date 2022-12-09 16:17
 * @since ODC_release_4.1.0
 */
public class MySQLOrderByFactoryTest {

    @Test
    public void generate_generalOrderByDesc_generateOrderBySucceed() {
        Order_byContext context = getOrderByContext("select col from tab order by col desc;");
        StatementFactory<OrderBy> factory = new MySQLOrderByFactory(context);
        OrderBy actual = factory.generate();

        SortKey sortKey = new SortKey(new ColumnReference(null, null, "col"), SortDirection.DESC);
        OrderBy expect = new OrderBy(Collections.singletonList(sortKey));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_generalOrderByAsc_generateOrderBySucceed() {
        Order_byContext context = getOrderByContext("select col from tab order by col asc;");
        StatementFactory<OrderBy> factory = new MySQLOrderByFactory(context);
        OrderBy actual = factory.generate();

        SortKey sortKey = new SortKey(new ColumnReference(null, null, "col"), SortDirection.ASC);
        OrderBy expect = new OrderBy(Collections.singletonList(sortKey));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_generalOrderByNoDirection_generateOrderBySucceed() {
        Order_byContext context = getOrderByContext("select col from tab order by col;");
        StatementFactory<OrderBy> factory = new MySQLOrderByFactory(context);
        OrderBy actual = factory.generate();

        SortKey sortKey = new SortKey(new ColumnReference(null, null, "col"), null);
        OrderBy expect = new OrderBy(Collections.singletonList(sortKey));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_generalOrderByMultiSortKeys_generateOrderBySucceed() {
        Order_byContext context = getOrderByContext("select col from tab order by chz.tab.col,tab3.col1 desc ;");
        StatementFactory<OrderBy> factory = new MySQLOrderByFactory(context);
        OrderBy actual = factory.generate();

        SortKey s1 = new SortKey(new ColumnReference("chz", "tab", "col"), null);
        SortKey s2 = new SortKey(new ColumnReference(null, "tab3", "col1"), SortDirection.DESC);
        OrderBy expect = new OrderBy(Arrays.asList(s1, s2));
        Assert.assertEquals(expect, actual);
    }

    private Order_byContext getOrderByContext(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.simple_select_with_order_and_limit().order_by();
    }

}
