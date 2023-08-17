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

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.Expression;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link SubPartitionOption}
 *
 * @author yh263208
 * @date 2023-05-30 16:01
 * @since ODC_release_4.2.0
 * @see BaseStatement
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class SubPartitionOption extends BaseStatement {

    private Integer subPartitionNum;
    private List<SubPartitionElement> templates;
    /**
     * candidates:
     * 
     * <pre>
     *     1. HASH
     *     2. RANGE
     *     3. LIST
     *     4. KEY
     *     5. RANGE COLUMNS
     *     6. LIST COLUMNS
     * </pre>
     */
    private final String type;
    private final List<Expression> subPartitionTargets;

    public SubPartitionOption(@NonNull ParserRuleContext context,
            @NonNull List<Expression> subPartitionTargets, @NonNull String type) {
        super(context);
        this.type = type;
        this.subPartitionTargets = subPartitionTargets;
    }

    public SubPartitionOption(@NonNull List<Expression> subPartitionTargets,
            @NonNull String type) {
        this.type = type;
        this.subPartitionTargets = subPartitionTargets;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("SUBPARTITION BY ").append(this.type.toUpperCase());
        builder.append("(")
                .append(this.subPartitionTargets.stream().map(Expression::toString)
                        .collect(Collectors.joining(",")))
                .append(")");
        if (CollectionUtils.isNotEmpty(this.templates)) {
            builder.append(" SUBPARTITION TEMPLATE (\n\t").append(this.templates.stream()
                    .map(Object::toString).collect(Collectors.joining(",\n\t")))
                    .append(")");
        } else if (this.subPartitionNum != null) {
            builder.append(" SUBPARTITIONS ").append(this.subPartitionNum);
        }
        return builder.toString();
    }

}
