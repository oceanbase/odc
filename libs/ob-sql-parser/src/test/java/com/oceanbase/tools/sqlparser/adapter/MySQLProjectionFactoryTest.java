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

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLProjectionFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.ProjectionContext;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.select.Projection;

/**
 * {@link MySQLProjectionFactoryTest}
 *
 * @author yh263208
 * @date 2022-12-12 17:20
 * @since ODC_release_4.1.0
 */
public class MySQLProjectionFactoryTest {

    @Test
    public void generate_starProjection_generateProjectionSucceed() {
        ProjectionContext context = getProjectionContext("select * from tab");
        StatementFactory<Projection> factory = new MySQLProjectionFactory(context);
        Projection actual = factory.generate();

        Projection expect = new Projection();
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnProjection_generateProjectionSucceed() {
        ProjectionContext context = getProjectionContext("select col from tab");
        StatementFactory<Projection> factory = new MySQLProjectionFactory(context);
        Projection actual = factory.generate();

        Projection expect = new Projection(new ColumnReference(null, null, "col"), null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnProjectionWithLabel_generateProjectionSucceed() {
        ProjectionContext context = getProjectionContext("select col label from tab");
        StatementFactory<Projection> factory = new MySQLProjectionFactory(context);
        Projection actual = factory.generate();

        Projection expect = new Projection(new ColumnReference(null, null, "col"), "label");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnProjectionWithAsLabel_generateProjectionSucceed() {
        ProjectionContext context = getProjectionContext("select chz.tab.col as label from tab");
        StatementFactory<Projection> factory = new MySQLProjectionFactory(context);
        Projection actual = factory.generate();

        Projection expect = new Projection(new ColumnReference("chz", "tab", "col"), "label");
        Assert.assertEquals(expect, actual);
    }

    private ProjectionContext getProjectionContext(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.simple_select().select_expr_list().projection(0);
    }
}
