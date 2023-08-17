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
import com.oceanbase.tools.sqlparser.statement.select.mysql.Limit;
import com.oceanbase.tools.sqlparser.statement.select.oracle.Fetch;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link Select}
 *
 * @author yh263208
 * @date 2022-12-07 21:01
 * @since ODC_release_4.1.0
 * @see BaseStatement
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class Select extends BaseStatement implements Expression {

    private final SelectBody selectBody;
    private Fetch fetch;
    private OrderBy orderBy;
    private Limit limit;
    private ForUpdate forUpdate;

    public Select(@NonNull ParserRuleContext context, @NonNull SelectBody selectBody) {
        super(context);
        this.selectBody = selectBody;
    }

    public Select(@NonNull SelectBody selectBody) {
        this.selectBody = selectBody;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(this.selectBody.toString());
        if (this.orderBy != null) {
            builder.append(" ").append(this.orderBy.toString());
        }
        if (this.fetch != null) {
            builder.append(" ").append(this.fetch.toString());
        }
        if (this.limit != null) {
            builder.append(" ").append(this.limit.toString());
        }
        if (this.forUpdate != null) {
            builder.append(" ").append(this.forUpdate.toString());
        }
        return builder.toString();
    }

}
