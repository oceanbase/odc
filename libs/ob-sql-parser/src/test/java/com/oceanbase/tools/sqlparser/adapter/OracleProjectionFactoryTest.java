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

import com.oceanbase.tools.sqlparser.adapter.oracle.OracleProjectionFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBLexer;
import com.oceanbase.tools.sqlparser.oboracle.OBParser;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.ProjectionContext;
import com.oceanbase.tools.sqlparser.statement.expression.RelationReference;
import com.oceanbase.tools.sqlparser.statement.select.Projection;

/**
 * {@link OracleProjectionFactoryTest}
 *
 * @author yh263208
 * @date 2022-12-06 19:16
 * @since ODC_release_4.1.0
 */
public class OracleProjectionFactoryTest {

    @Test
    public void generate_starProjection_generateProjectionSucceed() {
        ProjectionContext context = getProjectionContext("select * from dual");
        StatementFactory<Projection> factory = new OracleProjectionFactory(context);
        Projection actual = factory.generate();

        Projection expect = new Projection();
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnProjection_generateProjectionSucceed() {
        ProjectionContext context = getProjectionContext("select col from dual");
        StatementFactory<Projection> factory = new OracleProjectionFactory(context);
        Projection actual = factory.generate();

        Projection expect = new Projection(new RelationReference("col", null), null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnProjectionWithLabel_generateProjectionSucceed() {
        ProjectionContext context = getProjectionContext("select col label from dual");
        StatementFactory<Projection> factory = new OracleProjectionFactory(context);
        Projection actual = factory.generate();

        Projection expect = new Projection(new RelationReference("col", null), "label");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnProjectionWithAsLabel_generateProjectionSucceed() {
        ProjectionContext context = getProjectionContext("select col as label from dual");
        StatementFactory<Projection> factory = new OracleProjectionFactory(context);
        Projection actual = factory.generate();

        Projection expect = new Projection(new RelationReference("col", null), "label");
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
