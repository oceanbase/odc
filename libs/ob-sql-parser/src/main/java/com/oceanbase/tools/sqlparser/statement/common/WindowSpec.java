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
package com.oceanbase.tools.sqlparser.statement.common;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.select.OrderBy;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link WindowSpec}
 *
 * @author yh263208
 * @date 2022-12-11 16:56
 * @since ODC_release_4.1.0
 * @see BaseStatement
 */
@Setter
@Getter
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class WindowSpec extends BaseStatement {

    private OrderBy orderBy;
    private List<Expression> partitionBy = new ArrayList<>();
    private String name;
    private WindowBody body;

    public WindowSpec(@NonNull ParserRuleContext context) {
        super(context);
    }

    public WindowSpec(@NonNull ParserRuleContext context, @NonNull WindowSpec other) {
        super(context);
        this.orderBy = other.orderBy;
        this.partitionBy = other.partitionBy;
        this.name = other.name;
        this.body = other.body;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (this.name != null) {
            builder.append(this.name);
        }
        if (CollectionUtils.isNotEmpty(this.partitionBy)) {
            String exprList = this.partitionBy.stream()
                    .map(Object::toString).collect(Collectors.joining(","));
            builder.append(" PARTITION BY ")
                    .append(exprList);
        }
        if (this.orderBy != null) {
            builder.append(" ").append(this.orderBy.toString());
        }
        if (this.body != null) {
            builder.append(" ").append(this.body.toString());
        }
        if (builder.length() != 0 && builder.toString().startsWith(" ")) {
            return builder.substring(1);
        }
        return builder.toString();
    }

}
