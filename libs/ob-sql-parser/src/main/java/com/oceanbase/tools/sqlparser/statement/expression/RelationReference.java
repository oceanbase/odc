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
import org.antlr.v4.runtime.tree.TerminalNode;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.Expression;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * {@link RelationReference}
 *
 * @author yh263208
 * @date 2022-11-28 16:56
 * @since ODC_release_4.1.0
 * @see BaseStatement
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class RelationReference extends BaseStatement implements Expression {

    private final String relationName;
    private final Expression reference;

    public RelationReference(@NonNull ParserRuleContext ctx,
            @NonNull String relationName, Expression reference) {
        super(ctx);
        this.reference = reference;
        this.relationName = relationName;
    }

    public RelationReference(@NonNull TerminalNode ctx,
            @NonNull String relationName, Expression reference) {
        super(ctx);
        this.reference = reference;
        this.relationName = relationName;
    }

    public RelationReference(@NonNull String relationName, Expression reference) {
        this.reference = reference;
        this.relationName = relationName;
    }

    @Override
    public String toString() {
        if (this.reference == null) {
            return this.relationName;
        }
        return this.relationName + "." + this.reference.toString();
    }

}
