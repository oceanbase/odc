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

import com.oceanbase.tools.sqlparser.adapter.oracle.OracleFetchFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBLexer;
import com.oceanbase.tools.sqlparser.oboracle.OBParser;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Fetch_next_clauseContext;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;
import com.oceanbase.tools.sqlparser.statement.select.oracle.Fetch;
import com.oceanbase.tools.sqlparser.statement.select.oracle.FetchAddition;
import com.oceanbase.tools.sqlparser.statement.select.oracle.FetchDirection;
import com.oceanbase.tools.sqlparser.statement.select.oracle.FetchType;

/**
 * {@link OracleFetchFactoryTest}
 *
 * @author yh263208
 * @date 2022-12-06 19:58
 * @since ODC_release_4.1.0
 */
public class OracleFetchFactoryTest {

    @Test
    public void generate_fetchNextCountOnly_generateFetchSucceed() {
        Fetch_next_clauseContext context =
                getFetchNextClauseContext("select 1 from dual offset 1 row fetch next 20 rows only");
        StatementFactory<Fetch> factory = new OracleFetchFactory(context.fetch_next());
        Fetch actual = factory.generate();

        Fetch expect =
                new Fetch(new ConstExpression("20"), FetchDirection.NEXT, FetchType.COUNT, FetchAddition.ONLY, null);
        Assert.assertEquals(actual, expect);
    }

    @Test
    public void generate_fetchNextCountWithTies_generateFetchSucceed() {
        Fetch_next_clauseContext context =
                getFetchNextClauseContext("select 1 from dual offset 1 row fetch next 20 rows with ties");
        StatementFactory<Fetch> factory = new OracleFetchFactory(context.fetch_next());
        Fetch actual = factory.generate();

        Fetch expect = new Fetch(new ConstExpression("20"), FetchDirection.NEXT, FetchType.COUNT,
                FetchAddition.WITH_TIES, null);
        Assert.assertEquals(actual, expect);
    }

    @Test
    public void generate_fetchFirstCountOnly_generateFetchSucceed() {
        Fetch_next_clauseContext context =
                getFetchNextClauseContext("select 1 from dual offset 1 row fetch first 20 rows only");
        StatementFactory<Fetch> factory = new OracleFetchFactory(context.fetch_next());
        Fetch actual = factory.generate();

        Fetch expect =
                new Fetch(new ConstExpression("20"), FetchDirection.FIRST, FetchType.COUNT, FetchAddition.ONLY, null);
        Assert.assertEquals(actual, expect);
    }

    @Test
    public void generate_fetchFirstCountWithTies_generateFetchSucceed() {
        Fetch_next_clauseContext context =
                getFetchNextClauseContext("select 1 from dual offset 1 row fetch first 20 rows with ties");
        StatementFactory<Fetch> factory = new OracleFetchFactory(context.fetch_next());
        Fetch actual = factory.generate();

        Fetch expect = new Fetch(new ConstExpression("20"), FetchDirection.FIRST, FetchType.COUNT,
                FetchAddition.WITH_TIES, null);
        Assert.assertEquals(actual, expect);
    }

    @Test
    public void generate_fetchFirstCountWithoutExpr_generateFetchSucceed() {
        Fetch_next_clauseContext context =
                getFetchNextClauseContext("select 1 from dual offset 1 row fetch first rows with ties");
        StatementFactory<Fetch> factory = new OracleFetchFactory(context.fetch_next());
        Fetch actual = factory.generate();

        Fetch expect = new Fetch(null, FetchDirection.FIRST, FetchType.COUNT, FetchAddition.WITH_TIES, null);
        Assert.assertEquals(actual, expect);
    }

    @Test
    public void generate_fetchNextPercentOnly_generateFetchSucceed() {
        Fetch_next_clauseContext context =
                getFetchNextClauseContext("select 1 from dual offset 1 row fetch next 20 percent rows only");
        StatementFactory<Fetch> factory = new OracleFetchFactory(context.fetch_next());
        Fetch actual = factory.generate();

        Fetch expect =
                new Fetch(new ConstExpression("20"), FetchDirection.NEXT, FetchType.PERCENT, FetchAddition.ONLY, null);
        Assert.assertEquals(actual, expect);
    }

    @Test
    public void generate_fetchNextPercentWithTies_generateFetchSucceed() {
        Fetch_next_clauseContext context =
                getFetchNextClauseContext("select 1 from dual offset 1 row fetch next 20 percent rows with ties");
        StatementFactory<Fetch> factory = new OracleFetchFactory(context.fetch_next());
        Fetch actual = factory.generate();

        Fetch expect = new Fetch(new ConstExpression("20"), FetchDirection.NEXT, FetchType.PERCENT,
                FetchAddition.WITH_TIES, null);
        Assert.assertEquals(actual, expect);
    }

    @Test
    public void generate_fetchFirstPercentOnly_generateFetchSucceed() {
        Fetch_next_clauseContext context =
                getFetchNextClauseContext("select 1 from dual offset 1 row fetch first 20 percent rows only");
        StatementFactory<Fetch> factory = new OracleFetchFactory(context.fetch_next());
        Fetch actual = factory.generate();

        Fetch expect =
                new Fetch(new ConstExpression("20"), FetchDirection.FIRST, FetchType.PERCENT, FetchAddition.ONLY, null);
        Assert.assertEquals(actual, expect);
    }

    @Test
    public void generate_fetchFirstPercentWithTies_generateFetchSucceed() {
        Fetch_next_clauseContext context =
                getFetchNextClauseContext("select 1 from dual offset 1 row fetch first 20 percent rows with ties");
        StatementFactory<Fetch> factory = new OracleFetchFactory(context.fetch_next());
        Fetch actual = factory.generate();

        Fetch expect = new Fetch(new ConstExpression("20"), FetchDirection.FIRST, FetchType.PERCENT,
                FetchAddition.WITH_TIES, null);
        Assert.assertEquals(actual, expect);
    }

    @Test
    public void generate_fetchClauseFirstPercentWithTies_generateFetchSucceed() {
        Fetch_next_clauseContext context =
                getFetchNextClauseContext("select 1 from dual offset 1 row fetch first 20 percent rows with ties");
        StatementFactory<Fetch> factory = new OracleFetchFactory(context);
        Fetch actual = factory.generate();

        Fetch expect =
                new Fetch(new ConstExpression("20"), FetchDirection.FIRST, FetchType.PERCENT, FetchAddition.WITH_TIES,
                        new ConstExpression("1"));
        Assert.assertEquals(actual, expect);
    }

    private Fetch_next_clauseContext getFetchNextClauseContext(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.select_stmt().fetch_next_clause();
    }

}
