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

import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLMaterializedViewRefreshOptsFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.statement.creatematerializedview.MaterializedViewRefreshInterval;
import com.oceanbase.tools.sqlparser.statement.creatematerializedview.MaterializedViewRefreshOnClause;
import com.oceanbase.tools.sqlparser.statement.creatematerializedview.MaterializedViewRefreshOpts;

/**
 * @description: Test cases for {@link MySQLMaterializedViewRefreshOptsFactory}
 * @author: zijia.cj
 * @date: 2025/3/18 19:47
 * @since: 4.3.4
 */
public class MySQLMaterializedViewRefreshOptsFactoryTest {
    @Test
    public void generate_refreshMethodIsComplete_generateSucceed() {
        OBParser.Mview_refresh_optContext context = getMaterializedViewRefreshOpts(
                "REFRESH COMPLETE ON DEMAND START WITH sysdate() NEXT sysdate() + INTERVAL 1 DAY");
        MySQLMaterializedViewRefreshOptsFactory factory = new MySQLMaterializedViewRefreshOptsFactory(context);
        MaterializedViewRefreshOpts generate = factory.generate();
        MaterializedViewRefreshOpts materializedViewRefreshOpts =
                new MaterializedViewRefreshOpts("COMPLETE", new MaterializedViewRefreshInterval("sysdate()", 1L, "DAY"),
                        new MaterializedViewRefreshOnClause("DEMAND"));
        Assert.assertEquals(materializedViewRefreshOpts, generate);
    }

    @Test
    public void generate_refreshMethodIsForce_generateSucceed() {
        OBParser.Mview_refresh_optContext context1 = getMaterializedViewRefreshOpts(
                "REFRESH FORCE ON DEMAND START WITH sysdate() NEXT sysdate() + INTERVAL 1 DAY");
        MySQLMaterializedViewRefreshOptsFactory factory1 = new MySQLMaterializedViewRefreshOptsFactory(context1);
        MaterializedViewRefreshOpts generate1 = factory1.generate();
        MaterializedViewRefreshOpts materializedViewRefreshOpts1 =
                new MaterializedViewRefreshOpts("FORCE", new MaterializedViewRefreshInterval("sysdate()", 1L, "DAY"),
                        new MaterializedViewRefreshOnClause("DEMAND"));
        Assert.assertEquals(materializedViewRefreshOpts1, generate1);
    }

    @Test
    public void generate_refreshMethodIsFast_generateSucceed() {
        OBParser.Mview_refresh_optContext context2 = getMaterializedViewRefreshOpts(
                "REFRESH FAST ON DEMAND START WITH sysdate() NEXT sysdate() + INTERVAL 1 DAY");
        MySQLMaterializedViewRefreshOptsFactory factory2 = new MySQLMaterializedViewRefreshOptsFactory(context2);
        MaterializedViewRefreshOpts generate2 = factory2.generate();
        MaterializedViewRefreshOpts materializedViewRefreshOpts2 =
                new MaterializedViewRefreshOpts("FAST", new MaterializedViewRefreshInterval("sysdate()", 1L, "DAY"),
                        new MaterializedViewRefreshOnClause("DEMAND"));
        Assert.assertEquals(materializedViewRefreshOpts2, generate2);
    }

    @Test
    public void generate_refreshMethodIsNever_generateSucceed() {
        OBParser.Mview_refresh_optContext context3 = getMaterializedViewRefreshOpts(
                "NEVER REFRESH");
        MySQLMaterializedViewRefreshOptsFactory factory3 = new MySQLMaterializedViewRefreshOptsFactory(context3);
        MaterializedViewRefreshOpts generate3 = factory3.generate();
        MaterializedViewRefreshOpts materializedViewRefreshOpts3 = new MaterializedViewRefreshOpts("NEVER", null, null);
        Assert.assertEquals(materializedViewRefreshOpts3, generate3);
    }

    private OBParser.Mview_refresh_optContext getMaterializedViewRefreshOpts(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.mview_refresh_opt();
    }

}
