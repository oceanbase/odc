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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.sqlparser.adapter.oracle.OracleForUpdateFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBLexer;
import com.oceanbase.tools.sqlparser.oboracle.OBParser;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.For_updateContext;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.select.ForUpdate;
import com.oceanbase.tools.sqlparser.statement.select.WaitOption;

/**
 * {@link OracleForUpdateFactoryTest}
 *
 * @author yh263208
 * @date 2022-12-07 11:03
 * @since ODC_release_4.1.0
 */
public class OracleForUpdateFactoryTest {

    @Test
    public void generate_noColumnListNoWaitOption_generateForUpdateSucceed() {
        For_updateContext context = getForUpdateContext("select 1 from dual for update");
        StatementFactory<ForUpdate> factory = new OracleForUpdateFactory(context);
        ForUpdate actual = factory.generate();

        ForUpdate expect = new ForUpdate(new ArrayList<>(), null, null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_existsColumnListNoWaitOption_generateForUpdateSucceed() {
        For_updateContext context = getForUpdateContext("select 1 from dual for update of tab.col1,tab.col2");
        StatementFactory<ForUpdate> factory = new OracleForUpdateFactory(context);
        ForUpdate actual = factory.generate();

        ColumnReference c1 = new ColumnReference(null, "tab", "col1");
        ColumnReference c2 = new ColumnReference(null, "tab", "col2");
        ForUpdate expect = new ForUpdate(Arrays.asList(c1, c2), null, null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_existsColumnListWaitIntNum_generateForUpdateSucceed() {
        For_updateContext context = getForUpdateContext("select 1 from dual for update of tab.col1,tab.col2 wait 12");
        StatementFactory<ForUpdate> factory = new OracleForUpdateFactory(context);
        ForUpdate actual = factory.generate();

        ColumnReference c1 = new ColumnReference(null, "tab", "col1");
        ColumnReference c2 = new ColumnReference(null, "tab", "col2");
        ForUpdate expect = new ForUpdate(Arrays.asList(c1, c2), WaitOption.WAIT, BigDecimal.valueOf(12));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_existsColumnListWaitDecimal_generateForUpdateSucceed() {
        For_updateContext context = getForUpdateContext("select 1 from dual for update of tab.col1,tab.col2 wait 12.5");
        StatementFactory<ForUpdate> factory = new OracleForUpdateFactory(context);
        ForUpdate actual = factory.generate();

        ColumnReference c1 = new ColumnReference(null, "tab", "col1");
        ColumnReference c2 = new ColumnReference(null, "tab", "col2");
        ForUpdate expect = new ForUpdate(Arrays.asList(c1, c2), WaitOption.WAIT, BigDecimal.valueOf(12.5));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_existsColumnListNoWait_generateForUpdateSucceed() {
        For_updateContext context = getForUpdateContext("select 1 from dual for update of tab.col1,tab.col2 nowait");
        StatementFactory<ForUpdate> factory = new OracleForUpdateFactory(context);
        ForUpdate actual = factory.generate();

        ColumnReference c1 = new ColumnReference(null, "tab", "col1");
        ColumnReference c2 = new ColumnReference(null, "tab", "col2");
        ForUpdate expect = new ForUpdate(Arrays.asList(c1, c2), WaitOption.NOWAIT, null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_existsColumnListSkipLocked_generateForUpdateSucceed() {
        For_updateContext context =
                getForUpdateContext("select 1 from dual for update of tab.col1,tab.col2 skip locked");
        StatementFactory<ForUpdate> factory = new OracleForUpdateFactory(context);
        ForUpdate actual = factory.generate();

        ColumnReference c1 = new ColumnReference(null, "tab", "col1");
        ColumnReference c2 = new ColumnReference(null, "tab", "col2");
        ForUpdate expect = new ForUpdate(Arrays.asList(c1, c2), WaitOption.SKIP_LOCKED, null);
        Assert.assertEquals(expect, actual);
    }

    private For_updateContext getForUpdateContext(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.select_stmt().for_update();
    }

}
