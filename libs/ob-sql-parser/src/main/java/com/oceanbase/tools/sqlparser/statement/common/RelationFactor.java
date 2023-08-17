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
package com.oceanbase.tools.sqlparser.statement.common;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class RelationFactor extends BaseStatement {

    private String userVariable;
    private String schema;
    private final String relation;

    public RelationFactor(@NonNull ParserRuleContext context, @NonNull String relation) {
        super(context);
        this.relation = relation;
    }

    public RelationFactor(@NonNull String relation) {
        this.relation = relation;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (this.schema != null) {
            builder.append(" ").append(this.schema).append(".").append(this.relation);
        } else if (this.relation != null) {
            builder.append(" ").append(this.relation);
        }
        if (this.userVariable != null) {
            builder.append(this.userVariable);
        }
        return builder.length() == 0 ? null : builder.substring(1);
    }
}
