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

import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLLimitFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Limit_clauseContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Select_no_parensContext;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;
import com.oceanbase.tools.sqlparser.statement.select.mysql.Limit;

/**
 * {@link MySQLLimitFactoyTest}
 *
 * @author yh263208
 * @date 2022-12-12 16:24
 * @since ODC_release_4.1.0
 */
public class MySQLLimitFactoyTest {

    @Test
    public void generate_limitIntNum_generateFetchSucceed() {
        Limit_clauseContext context = getFetchNextClauseContext("select 1 from tab limit 20");
        StatementFactory<Limit> factory = new MySQLLimitFactory(context);
        Limit actual = factory.generate();

        Limit expect = new Limit(new ConstExpression("20"));
        Assert.assertEquals(actual, expect);
    }

    @Test
    public void generate_questionMark_generateFetchSucceed() {
        Limit_clauseContext context = getFetchNextClauseContext("select 1 from tab limit :23");
        StatementFactory<Limit> factory = new MySQLLimitFactory(context);
        Limit actual = factory.generate();

        Limit expect = new Limit(new ConstExpression(":23"));
        Assert.assertEquals(actual, expect);
    }

    @Test
    public void generate_columnRef_generateFetchSucceed() {
        Limit_clauseContext context = getFetchNextClauseContext("select 1 from tab limit tab.col");
        StatementFactory<Limit> factory = new MySQLLimitFactory(context);
        Limit actual = factory.generate();

        Limit expect = new Limit(new ColumnReference(null, "tab", "col"));
        Assert.assertEquals(actual, expect);
    }

    @Test
    public void generate_withOffset_generateFetchSucceed() {
        Limit_clauseContext context = getFetchNextClauseContext("select 1 from tab limit tab.col offset 34");
        StatementFactory<Limit> factory = new MySQLLimitFactory(context);
        Limit actual = factory.generate();

        Limit expect = new Limit(new ColumnReference(null, "tab", "col"));
        expect.setOffset(new ConstExpression("34"));
        Assert.assertEquals(actual, expect);
    }

    private Limit_clauseContext getFetchNextClauseContext(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        Select_no_parensContext context = parser.select_stmt().select_no_parens();
        return context.select_clause().simple_select_with_order_and_limit().limit_clause();
    }

}
