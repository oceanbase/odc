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

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Create_table_like_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Create_table_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Special_table_typeContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.common.ColumnGroupElement;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;

import lombok.NonNull;

/**
 * {@link MySQLCreateTableFactory}
 *
 * @author yh263208
 * @date 2022-12-26 15:04
 * @see StatementFactory
 * @since ODC_release_4.1.0
 */
public class MySQLCreateTableFactory extends OBParserBaseVisitor<CreateTable> implements StatementFactory<CreateTable> {

    private final ParserRuleContext parserRuleContext;

    public MySQLCreateTableFactory(@NonNull Create_table_stmtContext createTableStmtContext) {
        this.parserRuleContext = createTableStmtContext;
    }

    public MySQLCreateTableFactory(@NonNull Create_table_like_stmtContext createTableLikeStmtContext) {
        this.parserRuleContext = createTableLikeStmtContext;
    }

    @Override
    public CreateTable generate() {
        return visit(this.parserRuleContext);
    }

    @Override
    public CreateTable visitCreate_table_like_stmt(Create_table_like_stmtContext ctx) {
        RelationFactor factor = MySQLFromReferenceFactory.getRelationFactor(ctx.relation_factor(0));
        CreateTable createTable = new CreateTable(ctx, factor.getRelation());
        if (ctx.special_table_type().TEMPORARY() != null) {
            createTable.setTemporary(true);
        } else if (ctx.special_table_type().EXTERNAL() != null) {
            createTable.setExternal(true);
        }
        if (ctx.IF() != null && ctx.not() != null && ctx.EXISTS() != null) {
            createTable.setIfNotExists(true);
        }
        createTable.setSchema(factor.getSchema());
        createTable.setUserVariable(factor.getUserVariable());
        RelationFactor likeFactor = MySQLFromReferenceFactory.getRelationFactor(ctx.relation_factor(1));
        createTable.setLikeTable(likeFactor);
        return createTable;
    }

    @Override
    public CreateTable visitCreate_table_stmt(Create_table_stmtContext ctx) {
        RelationFactor factor = MySQLFromReferenceFactory.getRelationFactor(ctx.relation_factor());
        CreateTable createTable = new CreateTable(ctx, factor.getRelation());
        if (ctx.special_table_type() != null) {
            Special_table_typeContext specialTableTypeContext = ctx.special_table_type();
            if (specialTableTypeContext.EXTERNAL() != null) {
                createTable.setExternal(true);
            } else if (specialTableTypeContext.TEMPORARY() != null) {
                createTable.setTemporary(true);
            }
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
        if (ctx.partition_option() != null) {
            createTable.setPartition(new MySQLPartitionFactory(ctx.partition_option()).generate());
        }
        if (ctx.auto_partition_option() != null) {
            createTable.setPartition(new MySQLPartitionFactory(ctx.auto_partition_option()).generate());
        }
        if (ctx.with_column_group() != null) {
            List<ColumnGroupElement> columnGroupElements = ctx.with_column_group()
                    .column_group_list().column_group_element().stream()
                    .map(c -> new MySQLColumnGroupElementFactory(c).generate()).collect(Collectors.toList());
            createTable.setColumnGroupElements(columnGroupElements);
        }
        return createTable;
    }

}
