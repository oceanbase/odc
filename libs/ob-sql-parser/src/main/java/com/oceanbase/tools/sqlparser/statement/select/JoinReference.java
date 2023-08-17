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
package com.oceanbase.tools.sqlparser.statement.select;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.JoinType;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * {@link JoinReference}
 *
 * @author yh263208
 * @date 2022-11-24 22:24
 * @since ODC_release_4.1.0
 * @see FromReference
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class JoinReference extends BaseStatement implements FromReference {

    private final FromReference left;
    private final FromReference right;
    private final JoinType type;
    private final JoinCondition condition;

    public JoinReference(@NonNull ParserRuleContext context,
            @NonNull FromReference left,
            @NonNull FromReference right,
            @NonNull JoinType type, JoinCondition condition) {
        super(context);
        this.left = left;
        this.right = right;
        this.type = type;
        this.condition = condition;
    }

    public JoinReference(@NonNull FromReference left,
            @NonNull FromReference right,
            @NonNull JoinType type, JoinCondition condition) {
        this.left = left;
        this.right = right;
        this.type = type;
        this.condition = condition;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.left.toString()).append(" ")
                .append(this.type.name().replace("_", " ")).append(" ")
                .append(this.right.toString());
        if (this.condition == null) {
            return builder.toString();
        }
        return builder.append(" ").append(this.condition.toString()).toString();
    }

}
