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

import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLMaterializedViewRefreshIntervalFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.statement.creatematerializedview.MaterializedViewRefreshInterval;

/**
 * @description: Test cases for {@link MySQLMaterializedViewRefreshIntervalFactory}
 * @author: zijia.cj
 * @date: 2025/3/18 19:51
 * @since: 4.3.4
 */
public class MySQLMaterializedViewRefreshIntervalFactoryTest {

    @Test
    public void generate_startNow_generateSucceed() {
        OBParser.Mv_refresh_intervalContext context1 = getMaterializedViewRefreshInterval(
                "START WITH sysdate() NEXT sysdate() + INTERVAL 1 DAY");
        MySQLMaterializedViewRefreshIntervalFactory factory1 =
                new MySQLMaterializedViewRefreshIntervalFactory(context1);
        MaterializedViewRefreshInterval generate1 = factory1.generate();
        MaterializedViewRefreshInterval materializedViewRefreshInterval1 =
                new MaterializedViewRefreshInterval("sysdate()", 1L, "DAY");
        Assert.assertEquals(materializedViewRefreshInterval1, generate1);
    }

    @Test
    public void generate_startTimeStamp_generateSucceed() {
        OBParser.Mv_refresh_intervalContext context2 = getMaterializedViewRefreshInterval(
                "START WITH TIMESTAMP '2025-05-01 10:00:00' NEXT TIMESTAMP '2025-05-01 10:00:00' + INTERVAL 10000000 SECOND");
        MySQLMaterializedViewRefreshIntervalFactory factory2 =
                new MySQLMaterializedViewRefreshIntervalFactory(context2);
        MaterializedViewRefreshInterval generate2 = factory2.generate();
        MaterializedViewRefreshInterval materializedViewRefreshInterval2 =
                new MaterializedViewRefreshInterval("TIMESTAMP '2025-05-01 10:00:00'", 10000000L, "SECOND");
        Assert.assertEquals(materializedViewRefreshInterval2, generate2);
    }

    private OBParser.Mv_refresh_intervalContext getMaterializedViewRefreshInterval(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.mv_refresh_interval();
    }

}
