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

import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.common.ColumnGroupElement;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link OutOfLineConstraint}
 *
 * @author yh263208
 * @date 2023-05-24 17:37
 * @since ODC_release_4.2.0
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class OutOfLineConstraint extends BaseStatement implements TableElement {

    private String indexName;
    private String constraintName;
    private boolean uniqueKey;
    private boolean primaryKey;
    private final ConstraintState state;
    private final List<SortColumn> columns;
    private List<ColumnGroupElement> columnGroupElements;

    public OutOfLineConstraint(@NonNull ParserRuleContext context,
            ConstraintState state, @NonNull List<SortColumn> columns) {
        super(context);
        this.state = state;
        this.columns = columns;
    }

    public OutOfLineConstraint(@NonNull ParserRuleContext context,
            @NonNull OutOfLineConstraint target) {
        super(context);
        this.indexName = target.indexName;
        this.state = target.state;
        this.uniqueKey = target.uniqueKey;
        this.primaryKey = target.primaryKey;
        this.columns = target.columns;
        this.constraintName = target.constraintName;
    }

    public OutOfLineConstraint(ConstraintState state, @NonNull List<SortColumn> columns) {
        this.state = state;
        this.columns = columns;
    }

    @Override
    public String toString() {
        StringBuilder builder;
        if (this.constraintName != null) {
            builder = new StringBuilder(" CONSTRAINT");
            builder.append(" ").append(this.constraintName);
        } else {
            builder = new StringBuilder();
        }
        if (this.primaryKey) {
            builder.append(" PRIMARY KEY");
        } else if (this.uniqueKey) {
            builder.append(" UNIQUE");
        }
        if (this.indexName != null) {
            builder.append(" ").append(this.indexName);
        }
        if (CollectionUtils.isNotEmpty(this.columns)) {
            builder.append("(")
                    .append(getColumns().stream().map(SortColumn::toString)
                            .collect(Collectors.joining(",")))
                    .append(")");
        }
        if (this.state != null) {
            builder.append(" ").append(this.state);
        }
        if (this.columnGroupElements != null) {
            builder.append(" WITH COLUMN GROUP(")
                    .append(columnGroupElements.stream()
                            .map(ColumnGroupElement::toString).collect(Collectors.joining(",")))
                    .append(")");
        }
        return builder.length() == 0 ? "" : builder.substring(1);
    }

}
