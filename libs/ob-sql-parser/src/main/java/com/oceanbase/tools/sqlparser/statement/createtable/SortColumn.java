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
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.select.SortDirection;
import com.oceanbase.tools.sqlparser.statement.select.oracle.SortNullPosition;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link SortColumn}
 *
 * @author yh263208
 * @date 2023-05-24 13:58
 * @since ODC_release_4.2.0
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class SortColumn extends BaseStatement {

    private SortDirection direction;
    private SortNullPosition nullPosition;
    private Integer length;
    private Integer id;
    private final Expression column;

    public SortColumn(@NonNull ParserRuleContext context,
            @NonNull Expression column) {
        super(context);
        this.column = column;
    }

    public SortColumn(@NonNull Expression column) {
        this.column = column;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(this.column.toString());
        if (this.length != null) {
            builder.append("(").append(this.length).append(")");
        }
        if (this.direction != null) {
            builder.append(" ").append(this.direction.name());
        }
        if (this.nullPosition != null) {
            builder.append(" ").append("NULLS ")
                    .append(this.nullPosition == SortNullPosition.LAST ? "LAST" : "FIRST");
        }
        if (this.id != null) {
            builder.append(" ").append("ID ").append(this.id);
        }
        return builder.toString();
    }

}
