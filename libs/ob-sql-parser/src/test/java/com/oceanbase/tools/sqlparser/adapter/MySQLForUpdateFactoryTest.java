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

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLForUpdateFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.For_update_clauseContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Select_stmtContext;
import com.oceanbase.tools.sqlparser.statement.select.ForUpdate;
import com.oceanbase.tools.sqlparser.statement.select.WaitOption;

/**
 * {@link MySQLForUpdateFactoryTest}
 *
 * @author yh263208
 * @date 2022-12-12 16:49
 * @since ODC_release_4.1.0
 */
public class MySQLForUpdateFactoryTest {

    @Test
    public void generate_noColumnListNoWaitOption_generateForUpdateSucceed() {
        For_update_clauseContext context = getForUpdateContext("select 1 from tab for update");
        StatementFactory<ForUpdate> factory = new MySQLForUpdateFactory(context);
        ForUpdate actual = factory.generate();

        ForUpdate expect = new ForUpdate(new ArrayList<>(), null, null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_waitDecimal_generateForUpdateSucceed() {
        For_update_clauseContext context = getForUpdateContext("select 1 from tab for update wait 2E2");
        StatementFactory<ForUpdate> factory = new MySQLForUpdateFactory(context);
        ForUpdate actual = factory.generate();

        ForUpdate expect = new ForUpdate(new ArrayList<>(), WaitOption.WAIT, new BigDecimal("2E2"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_waitIntNum_generateForUpdateSucceed() {
        For_update_clauseContext context = getForUpdateContext("select 1 from tab for update wait 10");
        StatementFactory<ForUpdate> factory = new MySQLForUpdateFactory(context);
        ForUpdate actual = factory.generate();

        ForUpdate expect = new ForUpdate(new ArrayList<>(), WaitOption.WAIT, new BigDecimal("10"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_noWait_generateForUpdateSucceed() {
        For_update_clauseContext context = getForUpdateContext("select 1 from tab for update nowait");
        StatementFactory<ForUpdate> factory = new MySQLForUpdateFactory(context);
        ForUpdate actual = factory.generate();

        ForUpdate expect = new ForUpdate(new ArrayList<>(), WaitOption.NOWAIT, null);
        Assert.assertEquals(expect, actual);
    }

    private For_update_clauseContext getForUpdateContext(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        Select_stmtContext s = parser.select_stmt();
        return s.select_no_parens().for_update_clause();
    }

}
