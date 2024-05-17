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
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Create_table_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.common.ColumnGroupElement;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;
import com.oceanbase.tools.sqlparser.statement.select.Select;

import lombok.NonNull;

/**
 * {@link OracleCreateTableFactory}
 *
 * @author yh263208
 * @date 2022-12-26 16:22
 * @since ODC_release_4.1.0
 * @see StatementFactory
 */
public class OracleCreateTableFactory extends OBParserBaseVisitor<CreateTable>
        implements StatementFactory<CreateTable> {

    private final Create_table_stmtContext createTableStmtContext;

    public OracleCreateTableFactory(@NonNull Create_table_stmtContext createTableStmtContext) {
        this.createTableStmtContext = createTableStmtContext;
    }

    @Override
    public CreateTable generate() {
        return visit(this.createTableStmtContext);
    }

    @Override
    public CreateTable visitCreate_table_stmt(Create_table_stmtContext ctx) {
        CreateTable createTable = new CreateTable(ctx, OracleFromReferenceFactory.getRelation(ctx.relation_factor()));
        createTable.setSchema(OracleFromReferenceFactory.getSchemaName(ctx.relation_factor()));
        createTable.setUserVariable(OracleFromReferenceFactory.getUserVariable(ctx.relation_factor()));
        if (ctx.special_table_type().GLOBAL() != null && ctx.special_table_type().TEMPORARY() != null) {
            createTable.setGlobal(true);
            createTable.setTemporary(true);
        } else if (ctx.special_table_type().EXTERNAL() != null) {
            createTable.setExternal(true);
        }
        if (ctx.table_element_list() != null) {
            createTable.setTableElements(ctx.table_element_list().table_element().stream()
                    .map(c -> new OracleTableElementFactory(c).generate()).collect(Collectors.toList()));
        } else {
            Select as = new Select(ctx.subquery(), new OracleSelectBodyFactory(ctx.subquery()).generate());
            if (ctx.order_by() != null) {
                as.setOrderBy(new OracleOrderByFactory(ctx.order_by()).generate());
            }
            if (ctx.fetch_next_clause() != null) {
                as.setFetch(new OracleFetchFactory(ctx.fetch_next_clause()).generate());
            }
            createTable.setAs(as);
        }
        if (ctx.on_commit_option() != null) {
            if (ctx.on_commit_option().DELETE() != null) {
                createTable.setCommitOption(ctx.on_commit_option().DELETE().getText());
            } else if (ctx.on_commit_option().PRESERVE() != null) {
                createTable.setCommitOption(ctx.on_commit_option().PRESERVE().getText());
            }
        }
        if (ctx.table_option_list() != null) {
            createTable.setTableOptions(new OracleTableOptionsFactory(ctx.table_option_list()).generate());
        }
        if (ctx.partition_option() != null) {
            createTable.setPartition(new OraclePartitionFactory(ctx.partition_option()).generate());
        } else if (ctx.auto_partition_option() != null) {
            createTable.setPartition(new OraclePartitionFactory(ctx.auto_partition_option()).generate());
        }
        if (ctx.with_column_group() != null) {
            List<ColumnGroupElement> columnGroupElements = ctx.with_column_group()
                    .column_group_list().column_group_element().stream()
                    .map(c -> new OracleColumnGroupElementFactory(c).generate()).collect(Collectors.toList());
            createTable.setColumnGroupElements(columnGroupElements);
        }
        return createTable;
    }

}
