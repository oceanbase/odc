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

import java.util.stream.Collectors;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Create_table_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;

import lombok.NonNull;

/**
 * {@link MySQLCreateTableFactory}
 *
 * @author yh263208
 * @date 2022-12-26 15:04
 * @since ODC_release_4.1.0
 * @see StatementFactory
 */
public class MySQLCreateTableFactory extends OBParserBaseVisitor<CreateTable> implements StatementFactory<CreateTable> {

    private final Create_table_stmtContext createTableStmtContext;

    public MySQLCreateTableFactory(@NonNull Create_table_stmtContext createTableStmtContext) {
        this.createTableStmtContext = createTableStmtContext;
    }

    @Override
    public CreateTable generate() {
        return visit(this.createTableStmtContext);
    }

    @Override
    public CreateTable visitCreate_table_stmt(Create_table_stmtContext ctx) {
        RelationFactor factor = MySQLFromReferenceFactory.getRelationFactor(ctx.relation_factor());
        CreateTable createTable = new CreateTable(ctx, factor.getRelation());
        if (ctx.temporary_option().TEMPORARY() != null) {
            createTable.setTemporary(true);
        } else if (ctx.temporary_option().EXTERNAL() != null) {
            createTable.setExternal(true);
        }
        if (ctx.IF() != null && ctx.not() != null && ctx.EXISTS() != null) {
            createTable.setIfNotExists(true);
        }
        createTable.setSchema(factor.getSchema());
        createTable.setUserVariable(factor.getUserVariable());
        if (ctx.table_element_list() != null) {
            createTable.setTableElements(ctx.table_element_list().table_element().stream()
                    .map(c -> new MySQLTableElementFactory(c).generate()).collect(Collectors.toList()));
        } else {
            createTable.setAs(new MySQLSelectFactory(ctx.select_stmt()).generate());
        }
        if (ctx.table_option_list() != null) {
            createTable.setTableOptions(new MySQLTableOptionsFactory(ctx.table_option_list()).generate());
        }
        if (ctx.opt_partition_option() != null) {
            createTable.setPartition(new MySQLPartitionFactory(ctx.opt_partition_option()).generate());
        }
        return createTable;
    }

}
