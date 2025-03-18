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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLCreateMaterializedViewFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.statement.common.ColumnGroupElement;
import com.oceanbase.tools.sqlparser.statement.creatematerializedview.CreateMaterializedView;
import com.oceanbase.tools.sqlparser.statement.creatematerializedview.CreateMaterializedViewOpts;
import com.oceanbase.tools.sqlparser.statement.creatematerializedview.MaterializedViewRefreshInterval;
import com.oceanbase.tools.sqlparser.statement.creatematerializedview.MaterializedViewRefreshOnClause;
import com.oceanbase.tools.sqlparser.statement.creatematerializedview.MaterializedViewRefreshOpts;
import com.oceanbase.tools.sqlparser.statement.createtable.HashPartition;
import com.oceanbase.tools.sqlparser.statement.createtable.TableOptions;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;

/**
 * @description: Test cases for {@link MySQLCreateMaterializedViewFactory}
 * @author: zijia.cj
 * @date: 2025/3/18 09:29
 * @since: 4.3.4
 */
public class MySQLCreateMaterializedViewFactoryTest {
    @Test
    public void generate_allSyntax_generateSucceed() {
        OBParser.Create_mview_stmtContext context = getCreateMaterializedViewContext(
                "CREATE MATERIALIZED VIEW `zijia`.`test_mv_allsyntax` (PRIMARY KEY (prim)) " +
                        "DEFAULT CHARSET = gbk ROW_FORMAT = DYNAMIC COMPRESSION = 'zstd_1.3.8' REPLICA_NUM = 1 BLOCK_SIZE = 16384 USE_BLOOM_FILTER = FALSE ENABLE_MACRO_BLOCK_BLOOM_FILTER = FALSE TABLET_SIZE = 134217728 PCTFREE = 10 PARALLEL 5\n"
                        +
                        " partition by hash(prim)\n" +
                        " WITH COLUMN GROUP(all columns, each column) REFRESH COMPLETE ON DEMAND START WITH sysdate() NEXT sysdate() + INTERVAL 1 DAY ENABLE QUERY REWRITE ENABLE ON QUERY COMPUTATION "
                        +
                        "AS select `zijia`.`test_mv_base`.`col1` AS `prim`,`zijia`.`test_mv_base`.`col2` AS `col2`,`zijia`.`test_mv_base`.`col3` AS `col3`,`zijia`.`test_mv_base`.`col4` AS `col4` from `zijia`.`test_mv_base`");
        MySQLCreateMaterializedViewFactory factory = new MySQLCreateMaterializedViewFactory(context);
        CreateMaterializedView generate = factory.generate();

        TableOptions tableOptions = new TableOptions();
        tableOptions.setParallel(5);
        tableOptions.setBlockSize(16384);
        tableOptions.setReplicaNum(1);
        tableOptions.setUseBloomFilter(false);
        tableOptions.setTabletSize(134217728);
        tableOptions.setPctFree(10);
        tableOptions.setCompression("'zstd_1.3.8'");
        tableOptions.setRowFormat("DYNAMIC");
        tableOptions.setCharset("gbk");
        tableOptions.setEnableMacroBlockBloomFilter(false);

        HashPartition hashPartition = new HashPartition(Collections.singletonList(
                new ColumnReference(null, null, "prim")), null, null, null);

        List<ColumnGroupElement> columnGroupElements = new ArrayList<>();
        columnGroupElements.add(new ColumnGroupElement(true, false));
        columnGroupElements.add(new ColumnGroupElement(false, true));

        MaterializedViewRefreshInterval materializedViewRefreshInterval =
                new MaterializedViewRefreshInterval("sysdate()", 1L, "DAY");
        MaterializedViewRefreshOpts materializedViewRefreshOpts = new MaterializedViewRefreshOpts("COMPLETE",
                materializedViewRefreshInterval, new MaterializedViewRefreshOnClause("DEMAND"));
        CreateMaterializedViewOpts createMaterializedViewOpts =
                new CreateMaterializedViewOpts(true, true, materializedViewRefreshOpts);

        CreateMaterializedView createMaterializedView = new CreateMaterializedView();
        createMaterializedView.setTableOptions(tableOptions);
        createMaterializedView.setPartition(hashPartition);
        createMaterializedView.setColumnGroupElements(columnGroupElements);
        createMaterializedView.setCreateMaterializedViewOpts(createMaterializedViewOpts);

        Assert.assertEquals(createMaterializedView, generate);
    }

    private OBParser.Create_mview_stmtContext getCreateMaterializedViewContext(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.create_mview_stmt();
    }

}
