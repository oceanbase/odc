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

import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLMaterializedViewRefreshOnClauseFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.statement.creatematerializedview.MaterializedViewRefreshOnClause;

/**
 * @description: Test case for {@link MySQLMaterializedViewRefreshOnClauseFactory}
 * @author: zijia.cj
 * @date: 2025/3/18 19:53
 * @since: 4.3.4
 */
public class MySQLMaterializedViewRefreshOnClauseFactoryTest {
    @Test
    public void generate_onDemand_generateSucceed() {
        OBParser.Mv_refresh_on_clauseContext context = getMaterializedViewRefreshOnClause(
                "ON DEMAND");
        MySQLMaterializedViewRefreshOnClauseFactory factory = new MySQLMaterializedViewRefreshOnClauseFactory(context);
        MaterializedViewRefreshOnClause generate = factory.generate();
        Assert.assertEquals("DEMAND", generate.getRefreshMode());
    }

    private OBParser.Mv_refresh_on_clauseContext getMaterializedViewRefreshOnClause(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.mv_refresh_on_clause();
    }

}
