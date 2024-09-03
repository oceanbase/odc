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
package com.oceanbase.odc.service.connection.logicaldatabase.core.rewrite;

import static org.junit.Assert.*;

import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.connection.logicaldatabase.core.model.DataNode;
import com.oceanbase.tools.sqlparser.OBMySQLParser;
import com.oceanbase.tools.sqlparser.SQLParser;
import com.oceanbase.tools.sqlparser.statement.Statement;

public class RelationFactorRewriterTest {

    @Test
    public void testRewrite_MySQL_createTable() {
        String sql = "create table `logical_db_[0-3]`.`logical_tb_[0-3]`(a varchar(10))";
        Set<DataNode> dataNodes = getDataNodes();
        RewriteContext context = new RewriteContext(parse(sql), DialectType.OB_MYSQL, dataNodes);
        RelationFactorRewriter rewriter = new RelationFactorRewriter();
        RewriteResult actual = rewriter.rewrite(context);

        Map<DataNode, String> expectedSqls = new HashMap<>();
        expectedSqls.put(new DataNode("`schema1`", "`table1`"), "create table `schema1`.`table1`(a varchar(10))");
        expectedSqls.put(new DataNode("`schema2`", "`table2`"), "create table `schema2`.`table2`(a varchar(10))");
        RewriteResult expected = new RewriteResult(expectedSqls);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testRewrite_MySQL_alterTable() {
        String sql = "alter table `logical_db_[0-3]`.`logical_tb_[0-3]` add column a varchar(10)";
        Set<DataNode> dataNodes = getDataNodes();
        RewriteContext context = new RewriteContext(parse(sql), DialectType.OB_MYSQL, dataNodes);
        RelationFactorRewriter rewriter = new RelationFactorRewriter();
        RewriteResult actual = rewriter.rewrite(context);

        Map<DataNode, String> expectedSqls = new HashMap<>();
        expectedSqls.put(new DataNode("`schema1`", "`table1`"),
                "alter table `schema1`.`table1` add column a varchar(10)");
        expectedSqls.put(new DataNode("`schema2`", "`table2`"),
                "alter table `schema2`.`table2` add column a varchar(10)");
        RewriteResult expected = new RewriteResult(expectedSqls);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testRewrite_MySQL_dropTable() {
        String sql = "drop table `logical_db_[0-3]`.`logical_tb_[0-3]`";
        Set<DataNode> dataNodes = getDataNodes();
        RewriteContext context = new RewriteContext(parse(sql), DialectType.OB_MYSQL, dataNodes);
        RelationFactorRewriter rewriter = new RelationFactorRewriter();
        RewriteResult actual = rewriter.rewrite(context);

        Map<DataNode, String> expectedSqls = new HashMap<>();
        expectedSqls.put(new DataNode("`schema1`", "`table1`"), "drop table `schema1`.`table1`");
        expectedSqls.put(new DataNode("`schema2`", "`table2`"), "drop table `schema2`.`table2`");
        RewriteResult expected = new RewriteResult(expectedSqls);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testRewrite_MySQL_createIndex() {
        String sql = "create index any_index on `logical_db_[0-3]`.`logical_tb_[0-3]` (a)";
        Set<DataNode> dataNodes = getDataNodes();
        RewriteContext context = new RewriteContext(parse(sql), DialectType.OB_MYSQL, dataNodes);
        RelationFactorRewriter rewriter = new RelationFactorRewriter();
        RewriteResult actual = rewriter.rewrite(context);

        Map<DataNode, String> expectedSqls = new HashMap<>();
        expectedSqls.put(new DataNode("`schema1`", "`table1`"), "create index any_index on `schema1`.`table1` (a)");
        expectedSqls.put(new DataNode("`schema2`", "`table2`"), "create index any_index on `schema2`.`table2` (a)");
        RewriteResult expected = new RewriteResult(expectedSqls);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testRewrite_MySQL_dropIndex() {
        String sql = "drop index any_index on `logical_db_[0-3]`.`logical_tb_[0-3]`";
        Set<DataNode> dataNodes = getDataNodes();
        RewriteContext context = new RewriteContext(parse(sql), DialectType.OB_MYSQL, dataNodes);
        RelationFactorRewriter rewriter = new RelationFactorRewriter();
        RewriteResult actual = rewriter.rewrite(context);

        Map<DataNode, String> expectedSqls = new HashMap<>();
        expectedSqls.put(new DataNode("`schema1`", "`table1`"), "drop index any_index on `schema1`.`table1`");
        expectedSqls.put(new DataNode("`schema2`", "`table2`"), "drop index any_index on `schema2`.`table2`");
        RewriteResult expected = new RewriteResult(expectedSqls);

        Assert.assertEquals(expected, actual);
    }


    private Statement parse(String sql) {
        SQLParser sqlParser = new OBMySQLParser();
        return sqlParser.parse(new StringReader(sql));
    }

    private Set<DataNode> getDataNodes() {
        DataNode node1 = new DataNode();
        node1.setSchemaName("`schema1`");
        node1.setTableName("`table1`");

        DataNode node2 = new DataNode();
        node2.setSchemaName("`schema2`");
        node2.setTableName("`table2`");

        Set<DataNode> set = new HashSet<>();
        set.add(node1);
        set.add(node2);
        return set;
    }
}
