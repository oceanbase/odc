/*
 * Copyright (c) 2025 OceanBase.
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

import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLCreateMaterializedViewFactory;
import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLCreateMaterializedViewOptsFactory;
import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLMaterializedViewRefreshIntervalFactory;
import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLMaterializedViewRefreshOnClauseFactory;
import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLMaterializedViewRefreshOptsFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.statement.createMaterializedView.CreateMaterializedView;
import com.oceanbase.tools.sqlparser.statement.createMaterializedView.CreateMaterializedViewOpts;
import com.oceanbase.tools.sqlparser.statement.createMaterializedView.MaterializedViewRefreshInterval;
import com.oceanbase.tools.sqlparser.statement.createMaterializedView.MaterializedViewRefreshOnClause;
import com.oceanbase.tools.sqlparser.statement.createMaterializedView.MaterializedViewRefreshOpts;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Assert;
import org.junit.Test;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/18 01:29
 * @since: 4.3.4
 */
public class MySQLCreateMaterializedViewFactoryTest {
    @Test
    public void generate_CreateMaterializedView_generateSucceed() {
        OBParser.Create_mview_stmtContext context = getCreateMaterializedViewContext(
            "CREATE MATERIALIZED VIEW `zijia`.`test_mv_allsyntax` (PRIMARY KEY (prim)) DEFAULT CHARSET = gbk ROW_FORMAT = DYNAMIC COMPRESSION = 'zstd_1.3.8' REPLICA_NUM = 1 BLOCK_SIZE = 16384 USE_BLOOM_FILTER = FALSE ENABLE_MACRO_BLOCK_BLOOM_FILTER = FALSE TABLET_SIZE = 134217728 PCTFREE = 10 PARALLEL 5\n"+
                " partition by hash(prim)\n" +
                "(partition `p0`) WITH COLUMN GROUP(all columns, each column) REFRESH COMPLETE ON DEMAND START WITH sysdate() NEXT sysdate() + INTERVAL 1 DAY ENABLE QUERY REWRITE ENABLE ON QUERY COMPUTATION AS select `zijia`.`test_mv_base`.`col1` AS `prim`,`zijia`.`test_mv_base`.`col2` AS `col2`,`zijia`.`test_mv_base`.`col3` AS `col3`,`zijia`.`test_mv_base`.`col4` AS `col4` from `zijia`.`test_mv_base`");
        MySQLCreateMaterializedViewFactory factory = new MySQLCreateMaterializedViewFactory(context);
        CreateMaterializedView generate = factory.generate();
        MaterializedViewRefreshInterval materializedViewRefreshInterval = new MaterializedViewRefreshInterval("sysdate()", 1L, "DAY");
        MaterializedViewRefreshOpts materializedViewRefreshOpts = new MaterializedViewRefreshOpts("COMPLETE", materializedViewRefreshInterval, new MaterializedViewRefreshOnClause("DEMAND"));
        CreateMaterializedViewOpts createMaterializedViewOpts = new CreateMaterializedViewOpts(true, true, materializedViewRefreshOpts);
        Assert.assertEquals(createMaterializedViewOpts, generate.getCreateMaterializedViewOpts());
        Assert.assertEquals(Integer.valueOf(5), generate.getTableOptions().getParallel());
        Assert.assertEquals("prim", generate.getPartition().getPartitionTargets().get(0).getText());
    }

    @Test
    public void generate_CreateMaterializedViewOpts_generateSucceed() {

        OBParser.Create_mview_optsContext context = getCreateMaterializedViewOpts(
            "REFRESH COMPLETE ON DEMAND START WITH sysdate() NEXT sysdate() + INTERVAL 1 DAY ENABLE QUERY REWRITE ENABLE ON QUERY COMPUTATION");
        MySQLCreateMaterializedViewOptsFactory factory = new MySQLCreateMaterializedViewOptsFactory(context);
        CreateMaterializedViewOpts generate = factory.generate();
        MaterializedViewRefreshOpts materializedViewRefreshOpts = new MaterializedViewRefreshOpts("COMPLETE", new MaterializedViewRefreshInterval("sysdate()", 1L, "DAY"), new MaterializedViewRefreshOnClause("DEMAND"));
        CreateMaterializedViewOpts createMaterializedViewOpts = new CreateMaterializedViewOpts(true, true, materializedViewRefreshOpts);
        Assert.assertEquals(createMaterializedViewOpts,generate);

        OBParser.Create_mview_optsContext context1 = getCreateMaterializedViewOpts(
            "REFRESH COMPLETE ON DEMAND START WITH sysdate() NEXT sysdate() + INTERVAL 1 DAY");
        MySQLCreateMaterializedViewOptsFactory factory1 = new MySQLCreateMaterializedViewOptsFactory(context1);
        CreateMaterializedViewOpts generate1 = factory1.generate();
        MaterializedViewRefreshOpts materializedViewRefreshOpts1 = new MaterializedViewRefreshOpts("COMPLETE", new MaterializedViewRefreshInterval("sysdate()", 1L, "DAY"), new MaterializedViewRefreshOnClause("DEMAND"));
        CreateMaterializedViewOpts createMaterializedViewOpts1 = new CreateMaterializedViewOpts(false, false, materializedViewRefreshOpts1);
        Assert.assertEquals(createMaterializedViewOpts1,generate1);

        OBParser.Create_mview_optsContext context2 = getCreateMaterializedViewOpts(
            "REFRESH COMPLETE ON DEMAND START WITH sysdate() NEXT sysdate() + INTERVAL 1 DAY DISABLE QUERY REWRITE ENABLE ON QUERY COMPUTATION");
        MySQLCreateMaterializedViewOptsFactory factory2 = new MySQLCreateMaterializedViewOptsFactory(context2);
        CreateMaterializedViewOpts generate2 = factory2.generate();
        MaterializedViewRefreshOpts materializedViewRefreshOpts2 = new MaterializedViewRefreshOpts("COMPLETE", new MaterializedViewRefreshInterval("sysdate()", 1L, "DAY"), new MaterializedViewRefreshOnClause("DEMAND"));
        CreateMaterializedViewOpts createMaterializedViewOpts2 = new CreateMaterializedViewOpts(false, true, materializedViewRefreshOpts2);
        Assert.assertEquals(createMaterializedViewOpts2,generate2);

    }

    @Test
    public void generate_MaterializedViewRefreshOpts_generateSucceed() {

        OBParser.Mview_refresh_optContext context = getMaterializedViewRefreshOpts(
            "REFRESH COMPLETE ON DEMAND START WITH sysdate() NEXT sysdate() + INTERVAL 1 DAY");
        MySQLMaterializedViewRefreshOptsFactory factory = new MySQLMaterializedViewRefreshOptsFactory(context);
        MaterializedViewRefreshOpts generate = factory.generate();
        MaterializedViewRefreshOpts materializedViewRefreshOpts = new MaterializedViewRefreshOpts("COMPLETE", new MaterializedViewRefreshInterval("sysdate()", 1L, "DAY"), new MaterializedViewRefreshOnClause("DEMAND"));
        Assert.assertEquals(materializedViewRefreshOpts,generate);

        OBParser.Mview_refresh_optContext context1 = getMaterializedViewRefreshOpts(
            "REFRESH FORCE ON DEMAND START WITH sysdate() NEXT sysdate() + INTERVAL 1 DAY");
        MySQLMaterializedViewRefreshOptsFactory factory1 = new MySQLMaterializedViewRefreshOptsFactory(context1);
        MaterializedViewRefreshOpts generate1 = factory1.generate();
        MaterializedViewRefreshOpts materializedViewRefreshOpts1 = new MaterializedViewRefreshOpts("FORCE", new MaterializedViewRefreshInterval("sysdate()", 1L, "DAY"), new MaterializedViewRefreshOnClause("DEMAND"));
        Assert.assertEquals(materializedViewRefreshOpts1,generate1);

        OBParser.Mview_refresh_optContext context2 = getMaterializedViewRefreshOpts(
            "REFRESH FAST ON DEMAND START WITH sysdate() NEXT sysdate() + INTERVAL 1 DAY");
        MySQLMaterializedViewRefreshOptsFactory factory2 = new MySQLMaterializedViewRefreshOptsFactory(context2);
        MaterializedViewRefreshOpts generate2 = factory2.generate();
        MaterializedViewRefreshOpts materializedViewRefreshOpts2 = new MaterializedViewRefreshOpts("FAST", new MaterializedViewRefreshInterval("sysdate()", 1L, "DAY"), new MaterializedViewRefreshOnClause("DEMAND"));
        Assert.assertEquals(materializedViewRefreshOpts2,generate2);

        OBParser.Mview_refresh_optContext context3 = getMaterializedViewRefreshOpts(
            "NEVER REFRESH");
        MySQLMaterializedViewRefreshOptsFactory factory3 = new MySQLMaterializedViewRefreshOptsFactory(context3);
        MaterializedViewRefreshOpts generate3 = factory3.generate();
        MaterializedViewRefreshOpts materializedViewRefreshOpts3 = new MaterializedViewRefreshOpts("NEVER",null,null);
        Assert.assertEquals(materializedViewRefreshOpts3,generate3);
    }

    @Test
    public void generate_MaterializedViewRefreshInterval_generateSucceed() {

        OBParser.Mv_refresh_intervalContext context1 = getMaterializedViewRefreshInterval(
            "START WITH sysdate() NEXT sysdate() + INTERVAL 1 DAY");
        MySQLMaterializedViewRefreshIntervalFactory factory1 = new MySQLMaterializedViewRefreshIntervalFactory(context1);
        MaterializedViewRefreshInterval generate1 = factory1.generate();
        MaterializedViewRefreshInterval materializedViewRefreshInterval1 = new MaterializedViewRefreshInterval("sysdate()", 1L, "DAY");
        Assert.assertEquals(materializedViewRefreshInterval1,generate1);

        OBParser.Mv_refresh_intervalContext context2 = getMaterializedViewRefreshInterval(
            "START WITH TIMESTAMP '2025-05-01 10:00:00' NEXT TIMESTAMP '2025-05-01 10:00:00' + INTERVAL 10000000 SECOND");
        MySQLMaterializedViewRefreshIntervalFactory factory2 = new MySQLMaterializedViewRefreshIntervalFactory(context2);
        MaterializedViewRefreshInterval generate2 = factory2.generate();
        MaterializedViewRefreshInterval materializedViewRefreshInterval2 = new MaterializedViewRefreshInterval("TIMESTAMP '2025-05-01 10:00:00'", 10000000L, "SECOND");
        Assert.assertEquals(materializedViewRefreshInterval2,generate2);

    }

    @Test
    public void generate_MaterializedViewRefreshOnClause_generateSucceed() {

        OBParser.Mv_refresh_on_clauseContext context = getMaterializedViewRefreshOnClause(
            "ON DEMAND");
        MySQLMaterializedViewRefreshOnClauseFactory factory = new MySQLMaterializedViewRefreshOnClauseFactory(context);
        MaterializedViewRefreshOnClause generate = factory.generate();
        Assert.assertEquals("DEMAND", generate.getRefreshMode());
    }

    private OBParser.Create_mview_stmtContext getCreateMaterializedViewContext(String expr) {
        OBParser parser = getObParser(expr);
        return parser.create_mview_stmt();
    }

    private OBParser.Mv_refresh_on_clauseContext getMaterializedViewRefreshOnClause(String expr) {
        OBParser parser = getObParser(expr);
        return parser.mv_refresh_on_clause();
    }

    private OBParser.Mv_refresh_intervalContext getMaterializedViewRefreshInterval(String expr) {
        OBParser parser = getObParser(expr);
        return parser.mv_refresh_interval();
    }

    private OBParser.Mview_refresh_optContext getMaterializedViewRefreshOpts(String expr) {
        OBParser parser = getObParser(expr);
        return parser.mview_refresh_opt();
    }

    private OBParser.Create_mview_optsContext getCreateMaterializedViewOpts(String expr) {
        OBParser parser = getObParser(expr);
        return parser.create_mview_opts();
    }

    private OBParser getObParser(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser;
    }

}
