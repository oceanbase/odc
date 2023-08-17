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
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.select.oracle.SortNullPosition;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * {@link SortKey}
 *
 * @author yh263208
 * @date 2022-12-06 12:07
 * @since ODC_release_4.1.0
 * @see BaseStatement
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class SortKey extends BaseStatement implements GroupBy {

    private final Expression sort;
    private final SortDirection direction;
    private final SortNullPosition nullPosition;

    public SortKey(@NonNull ParserRuleContext context,
            @NonNull Expression sort, SortDirection direction, SortNullPosition nullPosition) {
        super(context);
        this.sort = sort;
        this.direction = direction;
        this.nullPosition = nullPosition;
    }

    public SortKey(@NonNull ParserRuleContext context,
            @NonNull Expression sort, SortDirection direction) {
        super(context);
        this.nullPosition = null;
        this.sort = sort;
        this.direction = direction;
    }

    public SortKey(@NonNull Expression sort, SortDirection direction) {
        this.nullPosition = null;
        this.sort = sort;
        this.direction = direction;
    }

    public SortKey(@NonNull Expression sort, SortDirection direction, SortNullPosition nullPosition) {
        this.sort = sort;
        this.direction = direction;
        this.nullPosition = nullPosition;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(this.sort.toString());
        if (this.direction != null) {
            builder.append(" ").append(this.direction.name());
        }
        if (this.nullPosition != null) {
            builder.append(" ").append("NULLS ")
                    .append(this.nullPosition == SortNullPosition.LAST ? "LAST" : "FIRST");
        }
        return builder.toString();
    }

}
