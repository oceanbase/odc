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
package com.oceanbase.tools.sqlparser.statement.createtable;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.common.ColumnGroupElement;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;
import com.oceanbase.tools.sqlparser.statement.select.Select;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link CreateTable}
 *
 * @author yh263208
 * @date 2022-12-26 14:43
 * @since ODC_release_4.1.0
 * @see BaseStatement
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class CreateTable extends BaseStatement {

    private String userVariable;
    private String schema;
    private boolean global;
    private boolean temporary;
    private boolean external;
    private Select as;
    private List<TableElement> tableElements;
    private TableOptions tableOptions;
    private boolean ifNotExists;
    private RelationFactor likeTable;
    // delete or preserve
    private String commitOption;
    private Partition partition;
    private final String tableName;
    private List<ColumnGroupElement> columnGroupElements;

    public CreateTable(@NonNull ParserRuleContext context, @NonNull String tableName) {
        super(context);
        this.tableName = tableName;
    }

    public CreateTable(@NonNull String tableName) {
        this.tableName = tableName;
    }

    public List<ColumnDefinition> getColumnDefinitions() {
        if (CollectionUtils.isEmpty(this.tableElements)) {
            return Collections.emptyList();
        }
        return this.tableElements.stream().filter(t -> t instanceof ColumnDefinition)
                .map(t -> (ColumnDefinition) t).collect(Collectors.toList());
    }

    public List<OutOfLineIndex> getIndexes() {
        if (CollectionUtils.isEmpty(this.tableElements)) {
            return Collections.emptyList();
        }
        return this.tableElements.stream().filter(t -> t instanceof OutOfLineIndex)
                .map(t -> (OutOfLineIndex) t).collect(Collectors.toList());
    }

    public List<OutOfLineConstraint> getConstraints() {
        if (CollectionUtils.isEmpty(this.tableElements)) {
            return Collections.emptyList();
        }
        return this.tableElements.stream().filter(t -> t instanceof OutOfLineConstraint)
                .map(t -> (OutOfLineConstraint) t).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("CREATE");
        if (this.global) {
            builder.append(" GLOBAL");
        }
        if (this.temporary) {
            builder.append(" TEMPORARY");
        }
        if (this.external) {
            builder.append(" EXTERNAL");
        }
        builder.append(" TABLE");
        if (this.ifNotExists) {
            builder.append(" IF NOT EXISTS");
        }
        if (this.schema != null) {
            builder.append(" ").append(this.schema).append(".").append(this.tableName);
        } else {
            builder.append(" ").append(this.tableName);
        }
        if (this.userVariable != null) {
            builder.append(this.userVariable);
        }
        if (this.likeTable != null) {
            builder.append(" LIKE ").append(this.likeTable);
            return builder.toString();
        }
        if (CollectionUtils.isNotEmpty(this.tableElements)) {
            builder.append(" (");
            List<ColumnDefinition> definitions = getColumnDefinitions();
            List<OutOfLineIndex> indexList = getIndexes();
            List<OutOfLineConstraint> constraints = getConstraints();
            if (!definitions.isEmpty()) {
                builder.append("\n\t").append(definitions.stream()
                        .map(ColumnDefinition::toString)
                        .collect(Collectors.joining(",\n\t")));
                if (!indexList.isEmpty() || !constraints.isEmpty()) {
                    builder.append(",");
                }
            }
            if (!indexList.isEmpty()) {
                builder.append("\n\t").append(indexList.stream()
                        .map(OutOfLineIndex::toString)
                        .collect(Collectors.joining(",\n\t")));
                if (!constraints.isEmpty()) {
                    builder.append(",");
                }
            }
            if (!constraints.isEmpty()) {
                builder.append("\n\t").append(constraints.stream()
                        .map(OutOfLineConstraint::toString)
                        .collect(Collectors.joining(",\n\t")));
            }
            builder.append("\n)");
        }
        if (this.tableOptions != null) {
            builder.append(" ").append(this.tableOptions);
        }
        if (this.partition != null) {
            builder.append("\n").append(this.partition).append("\n");
        }
        if (this.commitOption != null) {
            builder.append(" ON COMMIT ").append(this.commitOption.toUpperCase()).append(" ROWS");
        }
        if (this.columnGroupElements != null) {
            builder.append(" WITH COLUMN GROUP(")
                    .append(columnGroupElements.stream()
                            .map(ColumnGroupElement::toString).collect(Collectors.joining(",")))
                    .append(")");
        }
        if (this.as != null) {
            builder.append(" AS ").append(this.as);
        }
        return builder.toString();
    }

}
