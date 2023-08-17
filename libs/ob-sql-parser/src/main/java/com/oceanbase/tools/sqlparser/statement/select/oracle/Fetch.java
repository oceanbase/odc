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
package com.oceanbase.tools.sqlparser.statement.select.oracle;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.Expression;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * {@link Fetch}
 *
 * @author yh263208
 * @date 2022-12-06 10:59
 * @since ODC_release_4.1.0
 * @see BaseStatement
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class Fetch extends BaseStatement {

    private final Expression fetch;
    private final Expression offset;
    private final FetchType type;
    private final FetchAddition addition;
    private final FetchDirection direction;

    public Fetch(@NonNull ParserRuleContext context, Expression fetch,
            FetchDirection direction, FetchType type, FetchAddition addition, Expression offset) {
        super(context);
        this.type = type;
        this.addition = addition;
        this.fetch = fetch;
        this.direction = direction;
        this.offset = offset;
    }

    public Fetch(Expression fetch, FetchDirection direction,
            FetchType type, FetchAddition addition, Expression offset) {
        this.type = type;
        this.addition = addition;
        this.fetch = fetch;
        this.direction = direction;
        this.offset = offset;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (this.offset != null) {
            builder.append("OFFSET ")
                    .append(this.offset.toString()).append(" ROWS ");
        }
        builder.append("FETCH");
        if (this.direction != null) {
            builder.append(" ").append(this.direction == FetchDirection.FIRST ? "FIRST" : "NEXT");
        }
        if (this.fetch != null) {
            builder.append(" ").append(this.fetch.toString());
        }
        if (this.type != null && this.type == FetchType.PERCENT) {
            builder.append(" ").append("PERCENT");
        }
        builder.append(" ROWS");
        if (this.addition != null) {
            builder.append(" ").append(this.addition == FetchAddition.ONLY ? "ONLY" : "WITH TIES");
        }
        return builder.toString();
    }

}
