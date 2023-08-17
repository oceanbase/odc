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
package com.oceanbase.tools.sqlparser.adapter.oracle;

import java.util.List;
import java.util.stream.Collectors;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Create_index_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Index_using_algorithmContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.createindex.CreateIndex;
import com.oceanbase.tools.sqlparser.statement.createtable.IndexOptions;
import com.oceanbase.tools.sqlparser.statement.createtable.SortColumn;

import lombok.NonNull;

/**
 * {@link OracleCreateIndexFactory}
 *
 * @author yh263208
 * @date 2023-06-02 15:15
 * @since ODC_release_4.2.0
 */
public class OracleCreateIndexFactory extends OBParserBaseVisitor<CreateIndex>
        implements StatementFactory<CreateIndex> {

    private final Create_index_stmtContext createIndexStmtContext;

    public OracleCreateIndexFactory(@NonNull Create_index_stmtContext createIndexStmtContext) {
        this.createIndexStmtContext = createIndexStmtContext;
    }

    @Override
    public CreateIndex generate() {
        return visit(this.createIndexStmtContext);
    }

    @Override
    public CreateIndex visitCreate_index_stmt(Create_index_stmtContext ctx) {
        List<SortColumn> columns = ctx.sort_column_list().sort_column_key().stream()
                .map(c -> new OracleSortColumnFactory(c).generate()).collect(Collectors.toList());
        CreateIndex index = new CreateIndex(ctx,
                OracleFromReferenceFactory.getRelationFactor(ctx.normal_relation_factor()),
                OracleFromReferenceFactory.getRelationFactor(ctx.relation_factor()), columns);
        if (ctx.opt_index_options() != null) {
            IndexOptions options = new OracleIndexOptionsFactory(ctx.opt_index_options()).generate();
            Index_using_algorithmContext context = ctx.index_using_algorithm();
            if (context != null) {
                if (options.getUsingHash() == null) {
                    options.setUsingHash(OracleTableElementFactory.isUsingHash(context));
                }
                if (options.getUsingBtree() == null) {
                    options.setUsingBtree(OracleTableElementFactory.isUsingBTree(context));
                }
            }
            index.setIndexOptions(options);
        }
        if (ctx.UNIQUE() != null) {
            index.setUnique(true);
        }
        index.setPartition(new OraclePartitionFactory(ctx.opt_partition_option()).generate());
        return index;
    }

}
