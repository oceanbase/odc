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

import java.util.Collections;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.sqlparser.adapter.oracle.OracleOrderByFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBLexer;
import com.oceanbase.tools.sqlparser.oboracle.OBParser;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Order_byContext;
import com.oceanbase.tools.sqlparser.statement.expression.RelationReference;
import com.oceanbase.tools.sqlparser.statement.select.OrderBy;
import com.oceanbase.tools.sqlparser.statement.select.SortDirection;
import com.oceanbase.tools.sqlparser.statement.select.SortKey;
import com.oceanbase.tools.sqlparser.statement.select.oracle.SortNullPosition;

/**
 * {@link OracleOrderByFactoryTest}
 *
 * @author yh263208
 * @date 2022-12-06 19:38
 * @since ODC_release_4.1.0
 */
public class OracleOrderByFactoryTest {

    @Test
    public void generate_orderSiblingsBy_generateOrderBySucceed() {
        Order_byContext context = getOrderByContext("select 1 from dual order siblings by col desc");
        StatementFactory<OrderBy> factory = new OracleOrderByFactory(context);
        OrderBy actual = factory.generate();

        SortKey sortKey = new SortKey(new RelationReference("col", null), SortDirection.DESC, null);
        OrderBy expect = new OrderBy(true, Collections.singletonList(sortKey));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_orderByNullsLast_generateOrderBySucceed() {
        Order_byContext context = getOrderByContext("select 1 from dual order by col nulls last");
        StatementFactory<OrderBy> factory = new OracleOrderByFactory(context);
        OrderBy actual = factory.generate();

        SortKey sortKey = new SortKey(new RelationReference("col", null), null, SortNullPosition.LAST);
        OrderBy expect = new OrderBy(false, Collections.singletonList(sortKey));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_orderByNullsFirst_generateOrderBySucceed() {
        Order_byContext context = getOrderByContext("select 1 from dual order by col asc nulls first");
        StatementFactory<OrderBy> factory = new OracleOrderByFactory(context);
        OrderBy actual = factory.generate();

        SortKey sortKey = new SortKey(new RelationReference("col", null), SortDirection.ASC, SortNullPosition.FIRST);
        OrderBy expect = new OrderBy(false, Collections.singletonList(sortKey));
        Assert.assertEquals(expect, actual);
    }

    private Order_byContext getOrderByContext(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.select_stmt().order_by();
    }
}
