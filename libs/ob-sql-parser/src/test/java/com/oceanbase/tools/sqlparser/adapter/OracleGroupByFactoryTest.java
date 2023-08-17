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

import com.oceanbase.tools.sqlparser.adapter.oracle.OracleGroupByFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBLexer;
import com.oceanbase.tools.sqlparser.oboracle.OBParser;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Groupby_clauseContext;
import com.oceanbase.tools.sqlparser.statement.expression.ExpressionParam;
import com.oceanbase.tools.sqlparser.statement.expression.FunctionCall;
import com.oceanbase.tools.sqlparser.statement.expression.RelationReference;
import com.oceanbase.tools.sqlparser.statement.select.GroupBy;
import com.oceanbase.tools.sqlparser.statement.select.oracle.GeneralGroupBy;
import com.oceanbase.tools.sqlparser.statement.select.oracle.GroupingSetsGroupBy;

/**
 * {@link OracleGroupByFactoryTest}
 *
 * @author yh263208
 * @date 2022-12-06 22:08
 * @since ODC_release_4.1.0
 */
public class OracleGroupByFactoryTest {

    @Test
    public void generate_exprGroupbyClause_generateExprGroupBySucceed() {
        Groupby_clauseContext context = getGroupByClauseContext("select 1 from dual group by col");
        StatementFactory<GroupBy> factory = new OracleGroupByFactory(context.groupby_element_list().groupby_element(0));
        GroupBy actual = factory.generate();

        GroupBy expect = new GeneralGroupBy(new RelationReference("col", null));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_rollUpGroupbyClause_generateExprGroupBySucceed() {
        Groupby_clauseContext context = getGroupByClauseContext("select 1 from dual group by ROLLUP(col, col1)");
        StatementFactory<GroupBy> factory = new OracleGroupByFactory(context.groupby_element_list().groupby_element(0));
        GroupBy actual = factory.generate();

        ExpressionParam p1 = new ExpressionParam(new RelationReference("col", null));
        ExpressionParam p2 = new ExpressionParam(new RelationReference("col1", null));
        FunctionCall fCall = new FunctionCall("ROLLUP", Arrays.asList(p1, p2));
        GroupBy expect = new GeneralGroupBy(fCall);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_cubeGroupbyClause_generateExprGroupBySucceed() {
        Groupby_clauseContext context = getGroupByClauseContext("select 1 from dual group by CUBE(col, col1)");
        StatementFactory<GroupBy> factory = new OracleGroupByFactory(context.groupby_element_list().groupby_element(0));
        GroupBy actual = factory.generate();

        ExpressionParam p1 = new ExpressionParam(new RelationReference("col", null));
        ExpressionParam p2 = new ExpressionParam(new RelationReference("col1", null));
        FunctionCall fCall = new FunctionCall("CUBE", Arrays.asList(p1, p2));
        GroupBy expect = new GeneralGroupBy(fCall);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_groupingSetsGroupbyClause_generateExprGroupBySucceed() {
        Groupby_clauseContext context =
                getGroupByClauseContext("select 1 from dual group by GROUPING SETS(col, ROLLUP(col), CUBE(col1))");
        StatementFactory<GroupBy> factory = new OracleGroupByFactory(context.groupby_element_list().groupby_element(0));
        GroupBy actual = factory.generate();


        GroupBy g1 = new GeneralGroupBy(new RelationReference("col", null));
        ExpressionParam p1 = new ExpressionParam(new RelationReference("col", null));
        GroupBy g2 = new GeneralGroupBy(new FunctionCall("ROLLUP", Collections.singletonList(p1)));
        ExpressionParam p2 = new ExpressionParam(new RelationReference("col1", null));
        GroupBy g3 = new GeneralGroupBy(new FunctionCall("CUBE", Collections.singletonList(p2)));
        GroupBy expect = new GroupingSetsGroupBy(Arrays.asList(g1, g2, g3));
        Assert.assertEquals(expect, actual);
    }

    private Groupby_clauseContext getGroupByClauseContext(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.simple_select().groupby_clause();
    }
}
