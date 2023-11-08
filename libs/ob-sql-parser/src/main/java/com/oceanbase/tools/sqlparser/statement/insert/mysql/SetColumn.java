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

package com.oceanbase.tools.sqlparser.statement.insert.mysql;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link SetColumn}
 *
 * @author yh263208
 * @date 2023-11-08 20:28
 * @since ODC_release_4.2.3
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class SetColumn extends BaseStatement {

    private final Expression value;
    private final ColumnReference column;

    public SetColumn(@NonNull ParserRuleContext context,
            @NonNull ColumnReference column, @NonNull Expression value) {
        super(context);
        this.value = value;
        this.column = column;
    }

    public SetColumn(@NonNull ColumnReference column, @NonNull Expression value) {
        this.value = value;
        this.column = column;
    }

    @Override
    public String toString() {
        return this.column + "=" + this.value;
    }

}
