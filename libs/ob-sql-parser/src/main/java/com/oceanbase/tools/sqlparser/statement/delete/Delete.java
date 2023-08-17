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
package com.oceanbase.tools.sqlparser.statement.delete;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.select.FromReference;
import com.oceanbase.tools.sqlparser.statement.select.OrderBy;
import com.oceanbase.tools.sqlparser.statement.select.mysql.Limit;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link Delete}
 *
 * @author yh263208
 * @date 2022-12-20 17:05
 * @since ODC_release_4.1.0
 * @see BaseStatement
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class Delete extends BaseStatement {

    private boolean cursor;
    private Expression where;
    private final FromReference singleDelete;
    private final MultiDelete multiDelete;
    private OrderBy orderBy;
    private Limit limit;

    public Delete(@NonNull ParserRuleContext context, @NonNull FromReference singleDelete) {
        super(context);
        this.singleDelete = singleDelete;
        this.multiDelete = null;
    }

    public Delete(@NonNull ParserRuleContext context, @NonNull MultiDelete multiDelete) {
        super(context);
        this.multiDelete = multiDelete;
        this.singleDelete = null;

    }

    public Delete(@NonNull FromReference singleDelete) {
        this.singleDelete = singleDelete;
        this.multiDelete = null;
    }

    public Delete(@NonNull MultiDelete multiDelete) {
        this.multiDelete = multiDelete;
        this.singleDelete = null;

    }

    @Override
    public String toString() {
        return this.getText();
    }

}
