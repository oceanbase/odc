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

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Setter
@EqualsAndHashCode(callSuper = false)
abstract class BaseSubPartitionElement extends BaseStatement implements SubPartitionElement {
    @Getter
    private String userVariable;
    private String schema;
    private PartitionOptions partitionOptions;
    private final String relation;

    public BaseSubPartitionElement(@NonNull ParserRuleContext context, String relation) {
        super(context);
        this.relation = relation;
    }

    public BaseSubPartitionElement(String relation) {
        this.relation = relation;
    }

    @Override
    public String getSchema() {
        return this.schema;
    }

    @Override
    public String getRelation() {
        return this.relation;
    }

    @Override
    public PartitionOptions getOptions() {
        return this.partitionOptions;
    }

    public String getRelationFactor() {
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
