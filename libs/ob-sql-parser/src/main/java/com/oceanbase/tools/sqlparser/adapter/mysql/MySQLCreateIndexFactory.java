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
package com.oceanbase.tools.sqlparser.adapter.mysql;

import java.util.List;
import java.util.stream.Collectors;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Create_index_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Index_using_algorithmContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.common.ColumnGroupElement;
import com.oceanbase.tools.sqlparser.statement.createindex.CreateIndex;
import com.oceanbase.tools.sqlparser.statement.createtable.IndexOptions;
import com.oceanbase.tools.sqlparser.statement.createtable.SortColumn;

import lombok.NonNull;

/**
 * {@link MySQLCreateIndexFactory}
 *
 * @author yh263208
 * @date 2023-06-02 16:16
 * @since ODC_release_4.2.0
 */
public class MySQLCreateIndexFactory extends OBParserBaseVisitor<CreateIndex> implements StatementFactory<CreateIndex> {

    private final Create_index_stmtContext createIndexStmtContext;

    public MySQLCreateIndexFactory(@NonNull Create_index_stmtContext createIndexStmtContext) {
        this.createIndexStmtContext = createIndexStmtContext;
    }

    @Override
    public CreateIndex generate() {
        return visit(this.createIndexStmtContext);
    }

    @Override
    public CreateIndex visitCreate_index_stmt(Create_index_stmtContext ctx) {
        List<SortColumn> columns = ctx.sort_column_list().sort_column_key().stream()
                .map(c -> new MySQLSortColumnFactory(c).generate()).collect(Collectors.toList());
        CreateIndex index = new CreateIndex(ctx,
                MySQLFromReferenceFactory.getRelationFactor(ctx.normal_relation_factor()),
                MySQLFromReferenceFactory.getRelationFactor(ctx.relation_factor()), columns);
        if (ctx.opt_index_options() != null) {
            IndexOptions options = new MySQLIndexOptionsFactory(ctx.opt_index_options()).generate();
            Index_using_algorithmContext context = ctx.index_using_algorithm();
            if (context != null) {
                if (options.getUsingHash() == null) {
                    options.setUsingHash(MySQLTableElementFactory.isUsingHash(context));
                }
                if (options.getUsingBtree() == null) {
                    options.setUsingBtree(MySQLTableElementFactory.isUsingBTree(context));
                }
            }
            index.setIndexOptions(options);
        }
        if (ctx.UNIQUE() != null) {
            index.setUnique(true);
        } else if (ctx.FULLTEXT() != null) {
            index.setFullText(true);
        } else if (ctx.SPATIAL() != null) {
            index.setSpatial(true);
        }
        if (ctx.IF() != null && ctx.not() != null && ctx.EXISTS() != null) {
            index.setIfNotExists(true);
        }
        if (ctx.partition_option() != null) {
            index.setPartition(new MySQLPartitionFactory(ctx.partition_option()).generate());
        } else if (ctx.auto_partition_option() != null) {
            index.setPartition(new MySQLPartitionFactory(ctx.auto_partition_option()).generate());
        }
        if (ctx.with_column_group() != null) {
            List<ColumnGroupElement> columnGroupElements = ctx.with_column_group()
                    .column_group_list().column_group_element().stream()
                    .map(c -> new MySQLColumnGroupElementFactory(c).generate()).collect(Collectors.toList());
            index.setColumnGroupElements(columnGroupElements);
        }
        return index;
    }

}
