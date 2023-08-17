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

import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.Expression;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * {@link SubRangePartitionElement}
 *
 * @author yh263208
 * @date 2023-05-30 13:58
 * @since ODC_release_4.2.0
 */
@EqualsAndHashCode(callSuper = true)
public class SubRangePartitionElement extends BaseSubPartitionElement {
    @Getter
    private final List<Expression> rangeExprs;

    public SubRangePartitionElement(@NonNull ParserRuleContext context,
            @NonNull String relation, @NonNull List<Expression> rangeExprs) {
        super(context, relation);
        this.rangeExprs = rangeExprs;
    }

    public SubRangePartitionElement(@NonNull String relation,
            @NonNull List<Expression> rangeExprs) {
        super(relation);
        this.rangeExprs = rangeExprs;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("SUBPARTITION ")
                .append(getRelationFactor())
                .append(" VALUES LESS THAN(").append(this.rangeExprs.stream()
                        .map(Object::toString).collect(Collectors.joining(",")))
                .append(")");
        if (getOptions() != null) {
            builder.append(" ").append(getOptions());
        }
        return builder.toString();
    }

}
