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
package com.oceanbase.tools.sqlparser.statement.expression;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * {@link ColumnReference}
 *
 * @author yh263208
 * @date 2022-11-25 14:46
 * @since ODC_release_4.1.0
 * @see BaseStatement
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class ColumnReference extends BaseExpression {

    private final String schema;
    private final String relation;
    private final String column;

    public ColumnReference(@NonNull ParserRuleContext context,
            String schema, String relation, @NonNull String column) {
        super(context);
        this.schema = schema;
        this.relation = relation;
        this.column = column;
    }

    public ColumnReference(String schema, String relation,
            @NonNull String column) {
        this.schema = schema;
        this.relation = relation;
        this.column = column;
    }

    @Override
    public String doToString() {
        StringBuilder builder = new StringBuilder();
        if (this.schema != null) {
            builder.append(this.schema).append(".");
        }
        if (this.relation != null) {
            builder.append(this.relation).append(".");
        }
        return builder.append(this.column).toString();
    }

}
