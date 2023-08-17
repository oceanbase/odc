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
package com.oceanbase.tools.sqlparser.statement.select.mysql;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.Expression;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link Limit}
 *
 * @author yh263208
 * @date 2022-12-12 16:11
 * @since ODC_release_4.1.0
 * @see BaseStatement
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class Limit extends BaseStatement {

    private Expression offset;
    private final Expression rowCount;

    public Limit(@NonNull ParserRuleContext context,
            @NonNull Expression rowCount) {
        super(context);
        this.rowCount = rowCount;
    }

    public Limit(@NonNull Expression rowCount) {
        this.rowCount = rowCount;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("LIMIT");
        builder.append(" ").append(this.rowCount.toString());
        if (this.offset == null) {
            return builder.toString();
        }
        return builder.append(" OFFSET ")
                .append(this.offset.toString()).toString();
    }

}
