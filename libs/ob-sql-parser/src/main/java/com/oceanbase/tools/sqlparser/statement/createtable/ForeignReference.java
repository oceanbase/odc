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
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link ForeignReference}
 *
 * @author yh263208
 * @date 2023-05-19 19:59
 * @since ODC_release_4.2.0
 * @see BaseStatement
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class ForeignReference extends BaseStatement {

    private OnOption deleteOption;
    private OnOption updateOption;
    private MatchOption matchOption;
    private String userVariable;
    private final List<ColumnReference> columns;
    private final String schema;
    private final String relation;

    public ForeignReference(@NonNull ParserRuleContext context,
            String schema, @NonNull String relation, List<ColumnReference> columns) {
        super(context);
        this.columns = columns;
        this.relation = relation;
        this.schema = schema;
    }

    public ForeignReference(String schema, @NonNull String relation, List<ColumnReference> columns) {
        this.columns = columns;
        this.relation = relation;
        this.schema = schema;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("REFERENCES");
        builder.append(" ").append(this.schema == null ? "" : this.schema)
                .append(this.schema == null ? "" : ".").append(this.relation);
        if (this.userVariable != null) {
            builder.append(this.userVariable);
        }
        if (CollectionUtils.isNotEmpty(this.columns)) {
            builder.append("(")
                    .append(this.columns.stream().map(ColumnReference::toString)
                            .collect(Collectors.joining(",")))
                    .append(")");
        }
        if (this.matchOption != null) {
            builder.append(" MATCH ").append(this.matchOption.name());
        }
        if (this.deleteOption != null) {
            builder.append(" ON DELETE ");
            builder.append(this.deleteOption.name().replace("_", " "));
        }
        if (this.updateOption != null) {
            builder.append(" ON UPDATE ");
            builder.append(this.updateOption.name().replace("_", " "));
        }
        return builder.toString();
    }

    public enum OnOption {
        // on xxx cascade
        CASCADE,
        // on xxx set null
        SET_NULL,
        // on xxx restrict
        RESTRICT,
        // on xxx no action
        NO_ACTION,
        // on xxx set default
        SET_DEFAULT
    }

    public enum MatchOption {
        // match simple
        SIMPLE,
        // match full
        FULL,
        // match partial
        PARTIAL
    }

}
