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

package com.oceanbase.tools.sqlparser.statement.common.oracle;

import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.select.Projection;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link Returning}
 *
 * @author yh263208
 * @date 2023-11-08 19:30
 * @since ODC_release_4.2.3
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class Returning extends BaseStatement {

    private boolean bulkCollect;
    private final List<Expression> intoList;
    private final List<Projection> expressionList;

    public Returning(@NonNull ParserRuleContext context,
            @NonNull List<Expression> intoList,
            @NonNull List<Projection> expressionList) {
        super(context);
        this.intoList = intoList;
        this.expressionList = expressionList;
    }

    public Returning(
            @NonNull List<Expression> intoList,
            @NonNull List<Projection> expressionList) {
        this.intoList = intoList;
        this.expressionList = expressionList;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("RETURNING ")
                .append(this.expressionList.stream()
                        .map(Projection::toString)
                        .collect(Collectors.joining(",")));
        if (CollectionUtils.isNotEmpty(this.intoList)) {
            if (this.bulkCollect) {
                builder.append(" BULK COLLECT");
            }
            builder.append(" INTO ").append(this.intoList.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(",")));
        }
        return builder.toString();
    }

}
