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
package com.oceanbase.tools.sqlparser.statement.dropindex;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * @Author: Lebie
 * @Date: 2024/8/21 17:17
 * @Description: []
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class DropIndex extends BaseStatement {
    private final String indexName;
    private final String schemaName;
    private RelationFactor relation;

    public DropIndex(@NonNull ParserRuleContext context, @NonNull String indexName, @NonNull RelationFactor relation) {
        super(context);
        this.indexName = indexName;
        this.relation = relation;
        this.schemaName = relation.getSchema();
    }

    public DropIndex(@NonNull ParserRuleContext context, String schemaName, @NonNull String indexName) {
        super(context);
        this.indexName = indexName;
        this.schemaName = schemaName;
    }

    public DropIndex(@NonNull String indexName, @NonNull RelationFactor relation) {
        this.indexName = indexName;
        this.relation = relation;
        this.schemaName = relation.getSchema();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DROP INDEX ");
        if (schemaName != null) {
            sb.append(schemaName).append(".");
        }
        if (indexName != null) {
            sb.append(indexName);
        }
        if (relation != null) {
            sb.append(" ON ").append(relation);
        }
        return sb.toString();
    }
}
