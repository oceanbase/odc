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

import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLCreateMaterializedViewOptsFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.statement.creatematerializedview.CreateMaterializedViewOpts;
import com.oceanbase.tools.sqlparser.statement.creatematerializedview.MaterializedViewRefreshInterval;
import com.oceanbase.tools.sqlparser.statement.creatematerializedview.MaterializedViewRefreshOnClause;
import com.oceanbase.tools.sqlparser.statement.creatematerializedview.MaterializedViewRefreshOpts;

/**
 * @description: Test cases for {@link MySQLCreateMaterializedViewOptsFactory}
 * @author: zijia.cj
 * @date: 2025/3/18 19:43
 * @since: 4.3.4
 */
public class MySQLCreateMaterializedViewOptsFactoryTest {

    @Test
    public void generate_enableRewriteAndEnableCOMPUTATION_throwException() {
        OBParser.Create_mview_optsContext context = getCreateMaterializedViewOpts(
                "REFRESH COMPLETE ON DEMAND START WITH sysdate() NEXT sysdate() + INTERVAL 1 DAY ENABLE QUERY REWRITE ENABLE ON QUERY COMPUTATION");
        MySQLCreateMaterializedViewOptsFactory factory = new MySQLCreateMaterializedViewOptsFactory(context);
        CreateMaterializedViewOpts generate = factory.generate();
        MaterializedViewRefreshOpts materializedViewRefreshOpts =
                new MaterializedViewRefreshOpts("COMPLETE", new MaterializedViewRefreshInterval("sysdate()", 1L, "DAY"),
                        new MaterializedViewRefreshOnClause("DEMAND"));
        CreateMaterializedViewOpts createMaterializedViewOpts =
                new CreateMaterializedViewOpts(true, true, materializedViewRefreshOpts);
        Assert.assertEquals(createMaterializedViewOpts, generate);
    }

    @Test
    public void generate_enableRewriteAndDisableCOMPUTATION_throwException() {
        OBParser.Create_mview_optsContext context = getCreateMaterializedViewOpts(
                "REFRESH COMPLETE ON DEMAND START WITH sysdate() NEXT sysdate() + INTERVAL 1 DAY ENABLE QUERY REWRITE DISABLE ON QUERY COMPUTATION");
        MySQLCreateMaterializedViewOptsFactory factory = new MySQLCreateMaterializedViewOptsFactory(context);
        CreateMaterializedViewOpts generate = factory.generate();
        MaterializedViewRefreshOpts materializedViewRefreshOpts =
                new MaterializedViewRefreshOpts("COMPLETE", new MaterializedViewRefreshInterval("sysdate()", 1L, "DAY"),
                        new MaterializedViewRefreshOnClause("DEMAND"));
        CreateMaterializedViewOpts createMaterializedViewOpts =
                new CreateMaterializedViewOpts(true, false, materializedViewRefreshOpts);
        Assert.assertEquals(createMaterializedViewOpts, generate);
    }

    @Test
    public void generate_disableRewriteAndEnableCOMPUTATION_throwException() {
        OBParser.Create_mview_optsContext context = getCreateMaterializedViewOpts(
                "REFRESH COMPLETE ON DEMAND START WITH sysdate() NEXT sysdate() + INTERVAL 1 DAY DISABLE QUERY REWRITE ENABLE ON QUERY COMPUTATION");
        MySQLCreateMaterializedViewOptsFactory factory = new MySQLCreateMaterializedViewOptsFactory(context);
        CreateMaterializedViewOpts generate = factory.generate();
        MaterializedViewRefreshOpts materializedViewRefreshOpts =
                new MaterializedViewRefreshOpts("COMPLETE", new MaterializedViewRefreshInterval("sysdate()", 1L, "DAY"),
                        new MaterializedViewRefreshOnClause("DEMAND"));
        CreateMaterializedViewOpts createMaterializedViewOpts =
                new CreateMaterializedViewOpts(false, true, materializedViewRefreshOpts);
        Assert.assertEquals(createMaterializedViewOpts, generate);
    }

    @Test
    public void generate_disableRewriteAndDisableCOMPUTATION_throwException() {
        OBParser.Create_mview_optsContext context = getCreateMaterializedViewOpts(
                "REFRESH COMPLETE ON DEMAND START WITH sysdate() NEXT sysdate() + INTERVAL 1 DAY DISABLE QUERY REWRITE DISABLE ON QUERY COMPUTATION");
        MySQLCreateMaterializedViewOptsFactory factory = new MySQLCreateMaterializedViewOptsFactory(context);
        CreateMaterializedViewOpts generate = factory.generate();
        MaterializedViewRefreshOpts materializedViewRefreshOpts =
                new MaterializedViewRefreshOpts("COMPLETE", new MaterializedViewRefreshInterval("sysdate()", 1L, "DAY"),
                        new MaterializedViewRefreshOnClause("DEMAND"));
        CreateMaterializedViewOpts createMaterializedViewOpts =
                new CreateMaterializedViewOpts(false, false, materializedViewRefreshOpts);
        Assert.assertEquals(createMaterializedViewOpts, generate);
    }

    private OBParser.Create_mview_optsContext getCreateMaterializedViewOpts(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.create_mview_opts();
    }

}
