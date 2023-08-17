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
import lombok.Setter;

/**
 * {@link ListPartition}
 *
 * @author yh263208
 * @date 2023-05-30 16:55
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ListPartition extends BasePartition {
    private boolean columns;

    public ListPartition(@NonNull ParserRuleContext context,
            @NonNull List<Expression> targets,
            @NonNull List<ListPartitionElement> partitionElements,
            SubPartitionOption subPartitionOption, Integer partitionsNum, boolean columns) {
        super(context, targets, partitionElements, subPartitionOption, partitionsNum);
        this.columns = columns;
    }

    public ListPartition(@NonNull List<Expression> targets,
            @NonNull List<ListPartitionElement> partitionElements,
            SubPartitionOption subPartitionOption, Integer partitionsNum, boolean columns) {
        super(targets, partitionElements, subPartitionOption, partitionsNum);
        this.columns = columns;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("PARTITION BY LIST");
        if (columns) {
            builder.append(" COLUMNS");
        }
        builder.append("(").append(getPartitionTargets().stream()
                .map(Expression::toString).collect(Collectors.joining(",")))
                .append(")");
        if (getSubPartitionOption() != null) {
            builder.append(" ").append(getSubPartitionOption());
        }
        if (getPartitionsNum() != null) {
            builder.append(" PARTITIONS ").append(getPartitionsNum());
        }
        if (CollectionUtils.isNotEmpty(this.getPartitionElements())) {
            builder.append(" (\n\t").append(this.getPartitionElements().stream()
                    .map(PartitionElement::toString).collect(Collectors.joining(",\n\t")))
                    .append("\n)");
        }
        return builder.toString();
    }

}
