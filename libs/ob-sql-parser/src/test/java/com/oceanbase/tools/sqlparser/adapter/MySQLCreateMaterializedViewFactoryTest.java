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
import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.statement.createMaterializedView.CreateMaterializedView;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Test;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/18 01:29
 * @since: 4.3.4
 */
public class MySQLCreateMaterializedViewFactoryTest {
    @Test
    public void generate_CreateMv1_generateSucceed() {

        OBParser.Create_mview_stmtContext context = getCreateMaterializedViewContext(
            "CREATE MATERIALIZED VIEW `zijia`.`test_mv_allsyntax` (PRIMARY KEY (prim)) DEFAULT CHARSET = gbk ROW_FORMAT = DYNAMIC COMPRESSION = 'zstd_1.3.8' REPLICA_NUM = 1 BLOCK_SIZE = 16384 USE_BLOOM_FILTER = FALSE ENABLE_MACRO_BLOCK_BLOOM_FILTER = FALSE TABLET_SIZE = 134217728 PCTFREE = 10 PARALLEL 5\n"
                +
                " partition by hash(prim)\n" +
                "(partition `p0`) WITH COLUMN GROUP(all columns, each column) REFRESH COMPLETE ON DEMAND START WITH sysdate() NEXT sysdate() + INTERVAL 1 DAY ENABLE QUERY REWRITE ENABLE ON QUERY COMPUTATION AS select `zijia`.`test_mv_base`.`col1` AS `prim`,`zijia`.`test_mv_base`.`col2` AS `col2`,`zijia`.`test_mv_base`.`col3` AS `col3`,`zijia`.`test_mv_base`.`col4` AS `col4` from `zijia`.`test_mv_base`");
        MySQLCreateMaterializedViewFactory factory = new MySQLCreateMaterializedViewFactory(context);
        CreateMaterializedView generate = factory.generate();

        System.out.println("");
    }

    @Test
    public void generate_CreateMv2_generateSucceed() {

        OBParser.Create_mview_stmtContext context = getCreateMaterializedViewContext(
            "CREATE MATERIALIZED VIEW `zijia`.`test_mv_allsyntax` (PRIMARY KEY (prim)) DEFAULT CHARSET = gbk ROW_FORMAT = DYNAMIC COMPRESSION = 'zstd_1.3.8' REPLICA_NUM = 1 BLOCK_SIZE = 16384 USE_BLOOM_FILTER = FALSE ENABLE_MACRO_BLOCK_BLOOM_FILTER = FALSE TABLET_SIZE = 134217728 PCTFREE = 10 PARALLEL 5\n"
                +
                " partition by hash(prim)\n" +
                "(partition `p0`) WITH COLUMN GROUP(all columns, each column) REFRESH COMPLETE ON DEMAND START WITH sysdate() NEXT sysdate() + INTERVAL 1 DAY ENABLE QUERY REWRITE AS select `zijia`.`test_mv_base`.`col1` AS `prim`,`zijia`.`test_mv_base`.`col2` AS `col2`,`zijia`.`test_mv_base`.`col3` AS `col3`,`zijia`.`test_mv_base`.`col4` AS `col4` from `zijia`.`test_mv_base`");
        MySQLCreateMaterializedViewFactory factory = new MySQLCreateMaterializedViewFactory(context);
        CreateMaterializedView generate = factory.generate();

        System.out.println("");
    }

    @Test
    public void generate_CreateMv3_generateSucceed() {

        OBParser.Create_mview_stmtContext context = getCreateMaterializedViewContext(
            "CREATE MATERIALIZED VIEW `zijia`.`test_mv_allsyntax` (PRIMARY KEY (prim)) DEFAULT CHARSET = gbk ROW_FORMAT = DYNAMIC COMPRESSION = 'zstd_1.3.8' REPLICA_NUM = 1 BLOCK_SIZE = 16384 USE_BLOOM_FILTER = FALSE ENABLE_MACRO_BLOCK_BLOOM_FILTER = FALSE TABLET_SIZE = 134217728 PCTFREE = 10 PARALLEL 5\n"
                +
                " partition by hash(prim)\n" +
                "(partition `p0`) WITH COLUMN GROUP(all columns, each column) REFRESH COMPLETE ON DEMAND START WITH sysdate() NEXT sysdate() + INTERVAL 1 DAY ENABLE ON QUERY COMPUTATION AS select `zijia`.`test_mv_base`.`col1` AS `prim`,`zijia`.`test_mv_base`.`col2` AS `col2`,`zijia`.`test_mv_base`.`col3` AS `col3`,`zijia`.`test_mv_base`.`col4` AS `col4` from `zijia`.`test_mv_base`");
        MySQLCreateMaterializedViewFactory factory = new MySQLCreateMaterializedViewFactory(context);
        CreateMaterializedView generate = factory.generate();

        System.out.println("");
    }

    @Test
    public void generate_CreateMv4_generateSucceed() {

        OBParser.Create_mview_stmtContext context = getCreateMaterializedViewContext(
            "CREATE MATERIALIZED VIEW `zijia`.`test_mv_allsyntax` (PRIMARY KEY (prim)) DEFAULT CHARSET = gbk ROW_FORMAT = DYNAMIC COMPRESSION = 'zstd_1.3.8' REPLICA_NUM = 1 BLOCK_SIZE = 16384 USE_BLOOM_FILTER = FALSE ENABLE_MACRO_BLOCK_BLOOM_FILTER = FALSE TABLET_SIZE = 134217728 PCTFREE = 10 PARALLEL 5\n"
                +
                " partition by hash(prim)\n" +
                "(partition `p0`) WITH COLUMN GROUP(all columns, each column) REFRESH COMPLETE ON DEMAND START WITH sysdate() NEXT sysdate() + INTERVAL 1 DAY DISABLE QUERY REWRITE\n" +
                "    DISABLE ON QUERY COMPUTATION AS select `zijia`.`test_mv_base`.`col1` AS `prim`,`zijia`.`test_mv_base`.`col2` AS `col2`,`zijia`.`test_mv_base`.`col3` AS `col3`,`zijia`.`test_mv_base`.`col4` AS `col4` from `zijia`.`test_mv_base`");
        MySQLCreateMaterializedViewFactory factory = new MySQLCreateMaterializedViewFactory(context);
        CreateMaterializedView generate = factory.generate();

        System.out.println("");
    }

    @Test
    public void generate_CreateMv5_generateSucceed() {

        OBParser.Create_mview_stmtContext context = getCreateMaterializedViewContext(
            "CREATE MATERIALIZED VIEW `zijia`.`test_mv_allsyntax` (PRIMARY KEY (prim)) DEFAULT CHARSET = gbk ROW_FORMAT = DYNAMIC COMPRESSION = 'zstd_1.3.8' REPLICA_NUM = 1 BLOCK_SIZE = 16384 USE_BLOOM_FILTER = FALSE ENABLE_MACRO_BLOCK_BLOOM_FILTER = FALSE TABLET_SIZE = 134217728 PCTFREE = 10 PARALLEL 5\n"
                +
                " partition by hash(prim)\n" +
                "(partition `p0`) WITH COLUMN GROUP(all columns, each column) REFRESH COMPLETE ON DEMAND START WITH sysdate() NEXT sysdate() + INTERVAL 1 DAY \n" +
                "    DISABLE ON QUERY COMPUTATION  DISABLE QUERY REWRITE AS select `zijia`.`test_mv_base`.`col1` AS `prim`,`zijia`.`test_mv_base`.`col2` AS `col2`,`zijia`.`test_mv_base`.`col3` AS `col3`,`zijia`.`test_mv_base`.`col4` AS `col4` from `zijia`.`test_mv_base`");
        MySQLCreateMaterializedViewFactory factory = new MySQLCreateMaterializedViewFactory(context);
        CreateMaterializedView generate = factory.generate();

        System.out.println("");
    }

    @Test
    public void generate_CreateNeverRefreshMview_generateSucceed() {

        OBParser.Create_mview_stmtContext context = getCreateMaterializedViewContext(
            "CREATE MATERIALIZED VIEW `test_mv_never` NEVER REFRESH  AS\n" +
                "SELECT * FROM test_mv_base;  ");
        MySQLCreateMaterializedViewFactory factory = new MySQLCreateMaterializedViewFactory(context);
        CreateMaterializedView generate = factory.generate();

        System.out.println("");
    }


    private OBParser.Create_mview_stmtContext getCreateMaterializedViewContext(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.create_mview_stmt();
    }

}
