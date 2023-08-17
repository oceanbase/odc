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

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Column_definition_refContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Column_refContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Mysql_reserved_keywordContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Relation_nameContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;

import lombok.NonNull;

/**
 * {@link MySQLColumnRefFactory}
 *
 * @author yh263208
 * @date 2022-12-08 19:45
 * @since ODC_release_4.1.0
 * @see StatementFactory
 */
public class MySQLColumnRefFactory extends OBParserBaseVisitor<ColumnReference>
        implements StatementFactory<ColumnReference> {

    private final Column_refContext columnRefContext;
    private final Column_definition_refContext columnDefinitionRefContext;

    public MySQLColumnRefFactory(@NonNull Column_refContext columnRefContext) {
        this.columnRefContext = columnRefContext;
        this.columnDefinitionRefContext = null;
    }

    public MySQLColumnRefFactory(@NonNull Column_definition_refContext columnDefinitionRefContext) {
        this.columnDefinitionRefContext = columnDefinitionRefContext;
        this.columnRefContext = null;
    }

    @Override
    public ColumnReference generate() {
        if (this.columnDefinitionRefContext != null) {
            return visit(this.columnDefinitionRefContext);
        }
        return visit(this.columnRefContext);
    }

    @Override
    public ColumnReference visitColumn_ref(Column_refContext ctx) {
        if (CollectionUtils.isEmpty(ctx.Dot()) && ctx.column_name() != null) {
            return new ColumnReference(ctx, null, null, ctx.column_name().getText());
        }
        String schema = null;
        String relation;
        String column;
        LinkedList<Relation_nameContext> relationNames = new LinkedList<>(ctx.relation_name());
        LinkedList<Mysql_reserved_keywordContext> keywords = new LinkedList<>(ctx.mysql_reserved_keyword());
        if (ctx.Star() != null) {
            column = ctx.Star().getText();
        } else if (ctx.column_name() != null) {
            column = ctx.column_name().getText();
        } else {
            if (CollectionUtils.isEmpty(keywords)) {
                throw new IllegalStateException("No column name found");
            }
            if (keywords.size() == 1) {
                column = keywords.get(0).getText();
                keywords.removeFirst();
            } else {
                column = keywords.get(1).getText();
                keywords.removeLast();
            }
        }
        if (CollectionUtils.isNotEmpty(keywords)) {
            relation = keywords.get(0).getText();
        } else {
            if (CollectionUtils.isEmpty(relationNames)) {
                throw new IllegalStateException("No relation name found");
            }
            if (relationNames.size() == 1) {
                relation = relationNames.get(0).getText();
                relationNames.removeFirst();
            } else {
                relation = relationNames.get(1).getText();
                relationNames.removeLast();
            }
        }
        if (CollectionUtils.isNotEmpty(relationNames)) {
            schema = relationNames.get(0).getText();
        }
        return new ColumnReference(ctx, schema, relation, column);
    }

    @Override
    public ColumnReference visitColumn_definition_ref(Column_definition_refContext ctx) {
        List<Relation_nameContext> relations = ctx.relation_name();
        int size = relations.size();
        String relation = (--size) < 0 ? null : relations.get(size).getText();
        String schema = (--size) < 0 ? null : relations.get(size).getText();
        return new ColumnReference(ctx, schema, relation, ctx.column_name().getText());
    }

}
