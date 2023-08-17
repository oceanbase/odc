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
import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.tools.sqlparser.statement.Expression;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * {@link RangePartitionElement}
 *
 * @author yh263208
 * @date 2023-05-30 15:24
 * @since ODC_release_4.2.0
 * @see BasePartitionElement
 */
@EqualsAndHashCode(callSuper = true)
public class RangePartitionElement extends BasePartitionElement {
    @Getter
    private final List<Expression> rangeExprs;

    public RangePartitionElement(@NonNull ParserRuleContext context,
            String relation, @NonNull List<Expression> rangeExprs) {
        super(context, relation);
        this.rangeExprs = rangeExprs;
    }

    public RangePartitionElement(String relation, @NonNull List<Expression> rangeExprs) {
        super(relation);
        this.rangeExprs = rangeExprs;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("PARTITION");
        if (getRelationFactor() != null) {
            builder.append(" ").append(getRelationFactor());
        }
        builder.append(" VALUES LESS THAN(").append(this.rangeExprs.stream()
                .map(Object::toString).collect(Collectors.joining(",")))
                .append(")");
        if (getOptions() != null) {
            builder.append(" ").append(getOptions());
        }
        if (CollectionUtils.isNotEmpty(getSubPartitionElements())) {
            builder.append(" (\n\t\t").append(getSubPartitionElements().stream()
                    .map(Object::toString).collect(Collectors.joining(",\n\t\t")))
                    .append("\n\t)");
        }
        return builder.toString();
    }

}
